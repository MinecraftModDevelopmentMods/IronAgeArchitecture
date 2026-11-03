package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraftforge.common.property.IUnlistedProperty;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.*;

/** Tiny in-cell continuations prevent diagonally adjoining beams from pinching to a point. */
public final class TimberCorners {
    public static final IUnlistedProperty<Integer> PROPERTY=new IUnlistedProperty<Integer>() {
        @Override public String getName(){return "timber_corners";}
        @Override public boolean isValid(Integer value){return value!=null&&value>=0&&value<4096;}
        @Override public Class<Integer> getType(){return Integer.class;}
        @Override public String valueToString(Integer value){return value.toString();}
    };
    private TimberCorners(){ }
    private static IBlockState state(IBlockAccess world,BlockPos pos){return world instanceof World&&!((World)world).isBlockLoaded(pos)?Blocks.AIR.getDefaultState():world.getBlockState(pos);}
    private static boolean samePlane(IBlockState a,BlockPos pa,IBlockState b,BlockPos pb) {
        for(WallRuns.Run x:WallRuns.of(a,pa)) for(WallRuns.Run y:WallRuns.of(b,pb))
            if(x.tangent.getAxis()==y.tangent.getAxis()&&Math.abs(x.depth0-y.depth0)<1e-6&&Math.abs(x.depth1-y.depth1)<1e-6) return true;
        return false;
    }
    private static String endpoint(IBlockState state,BlockPos pos,double[] corner,EnumFacing.Axis normal) {
        if(!(state.getBlock() instanceof FrameBlock)||state.getBlock() instanceof JettyFloorBlock) return null;
        FrameBlock block=(FrameBlock)state.getBlock();
        if(block.family!=FrameFamily.BRACE&&!(block.family==FrameFamily.POST&&(state.getValue(FrameBlock.PANEL)&1)!=0)) return null;
        for(Part part:block.parts(state,0)) if(part.grain==Grain.RISING||part.grain==Grain.FALLING) for(double[] vertex:part.outline) {
            double[] p=part.transform(vertex[0],vertex[1],part.front,state.getValue(FrameBlock.FACING));
            double dx=Math.abs(pos.getX()*16+p[0]-corner[0]),dz=Math.abs(pos.getZ()*16+p[2]-corner[2]);
            if(Math.abs(pos.getY()*16+p[1]-corner[1])<1e-6&&(normal==EnumFacing.Axis.X?dz<1e-6&&dx<=1.25:dx<1e-6&&dz<=1.25)) return block.wood;
        }
        return null;
    }
    public static int derive(IBlockState owner,IBlockAccess world,BlockPos pos) {
        FrameBlock block=(FrameBlock)owner.getBlock();if(block instanceof JettyFloorBlock) return 0;
        if(block instanceof PartialWallBlock&&((PartialWallBlock)block).profile.corner) return 0;
        if(!(block instanceof PartialWallBlock)&&block.family==FrameFamily.TERMINAL&&(owner.getValue(FrameBlock.PANEL)&1)==0) return 0;
        EnumFacing facing=owner.getValue(FrameBlock.FACING);int result=0;
        for(int corner=0;corner<4;corner++) {
            boolean right=(corner&1)!=0,top=(corner&2)!=0;
            if(block instanceof PartialWallBlock) {WallProfile p=((PartialWallBlock)block).profile;int x=right?16:0;if(x<p.x0||x>p.x1) continue;}
            BlockPos hp=pos.offset(right?facing.rotateY():facing.rotateYCCW()),vp=top?pos.up():pos.down();
            IBlockState h=state(world,hp),v=state(world,vp);
            if(!samePlane(owner,pos,h,hp)||!samePlane(owner,pos,v,vp)) continue;
            // Check the endpoints on either elevation; the wood is the same on both faces.
            double[] local=WallGeometry.rotate(right?16:0,top?16:0,0,facing);
            double[] point={pos.getX()*16+local[0],pos.getY()*16+local[1],pos.getZ()*16+local[2]};
            String wood=endpoint(h,hp,point,facing.getAxis()),other=endpoint(v,vp,point,facing.getAxis());
            if(wood==null||!wood.equals(other)||block.wood!=null&&!block.wood.equals(wood)) continue;
            for(int w=0;w<ArchitectureBlocks.WOODS.length;w++) if(ArchitectureBlocks.WOODS[w].equals(wood)) result|=(w+1)<<(corner*3);
        }
        return result;
    }
    public static List<Part> add(List<Part> base,boolean thin,int encoded) {
        if(encoded==0) return base;List<Part> result=new ArrayList<>(base);double depth=thin?8:16,w=1.25;
        for(int corner=0;corner<4;corner++) {
            int wood=(encoded>>(corner*3)&7)-1;if(wood<0||wood>=ArchitectureBlocks.WOODS.length) continue;
            boolean right=(corner&1)!=0,top=(corner&2)!=0;
            double x=right?16:0,y=top?16:0;
            double[][] triangle={{x,y},{x+(right?-w:w),y},{x,y+(top?-w:w)}};
            // Reflecting just one axis reverses winding.
            if(right!=top) {double[] swap=triangle[1];triangle[1]=triangle[2];triangle[2]=swap;}
            Grain grain=right!=top?Grain.RISING:Grain.FALLING;
            result.add(new Part(triangle,.375,1.25,grain,EnumFacing.NORTH,false,ArchitectureBlocks.WOODS[wood]));
            result.add(new Part(triangle,depth-1.25,depth-.375,grain,EnumFacing.NORTH,false,ArchitectureBlocks.WOODS[wood]));
        }
        return Collections.unmodifiableList(result);
    }
}
