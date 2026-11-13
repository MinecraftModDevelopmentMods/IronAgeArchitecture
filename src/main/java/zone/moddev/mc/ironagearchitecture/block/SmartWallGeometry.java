package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.math.AxisAlignedBB;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.Grain;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.Part;

/** Fixed half-grid occupancy and face-local timbers share the ordinary clipping path. */
public final class SmartWallGeometry {
    public static final int FALLBACK=0x9b9b9b9b;
    private SmartWallGeometry() { }

    public static int mask(SmartWallBlock block,EnumFacing facing) {
        return mask(block.profile,facing);
    }
    public static int mask(int profile,EnumFacing facing) {
        int source=profile==0?15:profile==1?3:7, result=0;
        for(int q=0;q<4;q++) if((source&(1<<q))!=0) {
            double[] p=WallGeometry.rotate(q%2*8+4,0,q/2*8+4,facing);
            result|=1<<((p[0]<8?0:1)+(p[2]<8?0:2));
        }
        return result;
    }

    public static List<AxisAlignedBB> boxes(int mask) {
        List<AxisAlignedBB> result=new ArrayList<>();
        for(int q=0;q<4;q++) if((mask&(1<<q))!=0)
            result.add(new AxisAlignedBB(q%2*.5,0,q/2*.5,q%2*.5+.5,1,q/2*.5+.5));
        return result;
    }

    public static int face(int code,EnumFacing side) {return code>>>(side.getHorizontalIndex()*8)&255;}

    public static List<Part> parts(int mask,int code,int coverage) {
        List<Part> result=new ArrayList<>();
        for(int q=0;q<4;q++) if((mask&(1<<q))!=0) {
            int x=q%2,z=q/2;
            double x0=x*8,x1=x0+8,z0=z*8,z1=z0+8;
            if(!occupied(mask,x-1,z)&&!covered(coverage,EnumFacing.WEST,x,z)) x0+=.75;
            if(!occupied(mask,x+1,z)&&!covered(coverage,EnumFacing.EAST,x,z)) x1-=.75;
            if(!occupied(mask,x,z-1)&&!covered(coverage,EnumFacing.NORTH,x,z)) z0+=.75;
            if(!occupied(mask,x,z+1)&&!covered(coverage,EnumFacing.SOUTH,x,z)) z1-=.75;
            WallGeometry.box(result,x0,0,z0,x1,16,z1,Grain.DAUB);
        }
        for(EnumFacing side:EnumFacing.HORIZONTALS) {
            int pattern=face(code,side);
            if(pattern==0) continue;
            for(double[] strip:strips(mask,side,coverage)) {
                double low=strip[0],high=strip[1],depth=strip[2];
                if((pattern&SmartFacade.LEFT)!=0||low>0) beam(result,low,0,low+1.5,16,depth,side,Grain.VERTICAL);
                if((pattern&SmartFacade.RIGHT)!=0||high<16) beam(result,high-1.5,0,high,16,depth,side,Grain.VERTICAL);
                if((pattern&SmartFacade.CENTRE)!=0) beam(result,Math.max(low,7.2),0,Math.min(high,8.8),16,depth,side,Grain.VERTICAL);
                if((pattern&SmartFacade.BOTTOM)!=0) beam(result,low,0,high,1.5,depth,side,Grain.EAST_WEST);
                if((pattern&SmartFacade.TOP)!=0) beam(result,low,14.5,high,16,depth,side,Grain.EAST_WEST);
                if((pattern&SmartFacade.RISE)!=0) brace(result,low,high,depth,side,true);
                if((pattern&SmartFacade.FALL)!=0) brace(result,low,high,depth,side,false);
            }
        }
        return result;
    }

    public static int coverage(IBlockAccess world,BlockPos pos) {
        int result=0;
        for(EnumFacing side:EnumFacing.HORIZONTALS) {
            BlockPos next=pos.offset(side);
            if(world instanceof net.minecraft.world.World&&!((net.minecraft.world.World)world).isBlockLoaded(next)) continue;
            result|=footprint(world.getBlockState(next))<<(side.getHorizontalIndex()*4);
        }
        return result;
    }

    private static int footprint(IBlockState state) {
        if(state.getBlock() instanceof SmartWallBlock) return ((SmartWallBlock)state.getBlock()).mask(state);
        if(state.getBlock() instanceof PartialWallBlock) {
            PartialWallBlock wall=(PartialWallBlock)state.getBlock();
            int result=0;
            EnumFacing facing=state.getValue(FrameBlock.FACING);
            for(int q=0;q<4;q++) {
                double[] point=WallGeometry.rotate(q%2*8+4,0,q/2*8+4,facing==EnumFacing.EAST?EnumFacing.WEST:facing==EnumFacing.WEST?EnumFacing.EAST:facing);
                if(wall.profile.contains(point[0],point[2])) result|=1<<q;
            }
            return result;
        }
        EnumFacing half=WallRuns.straightHalfFacing(state);
        if(half!=null) return half==EnumFacing.NORTH?3:half==EnumFacing.SOUTH?12:half==EnumFacing.WEST?5:10;
        if(state.getBlock() instanceof WindowBlock||state.isFullCube()) return 15;
        return 0;
    }

    private static boolean covered(int coverage,EnumFacing side,int x,int z) {
        int nextX=x+side.getFrontOffsetX(),nextZ=z+side.getFrontOffsetZ();
        if(nextX>=0&&nextX<2&&nextZ>=0&&nextZ<2) return false;
        int neighbour=(coverage>>>(side.getHorizontalIndex()*4))&15;
        return (neighbour&(1<<(Math.floorMod(nextX,2)+Math.floorMod(nextZ,2)*2)))!=0;
    }

    private static List<double[]> strips(int mask,EnumFacing side,int coverage) {
        List<double[]> result=new ArrayList<>();
        EnumFacing inverse=side==EnumFacing.EAST?EnumFacing.WEST:side==EnumFacing.WEST?EnumFacing.EAST:side;
        for(int q=0;q<4;q++) if((mask&(1<<q))!=0) {
            int x=q%2,z=q/2;
            if(occupied(mask,x+side.getFrontOffsetX(),z+side.getFrontOffsetZ())||covered(coverage,side,x,z)) continue;
            double[] a=WallGeometry.rotate(x*8,0,z*8,inverse);
            double[] b=WallGeometry.rotate(x*8+8,0,z*8+8,inverse);
            double low=Math.min(a[0],b[0]),high=Math.max(a[0],b[0]),depth=Math.min(a[2],b[2]);
            double[] merged=null;
            for(double[] candidate:result) if(candidate[2]==depth&&(candidate[1]==low||candidate[0]==high)) merged=candidate;
            if(merged==null) result.add(new double[]{low,high,depth});
            else {merged[0]=Math.min(merged[0],low);merged[1]=Math.max(merged[1],high);}
        }
        return result;
    }
    private static boolean occupied(int mask,int x,int z) {return x>=0&&x<2&&z>=0&&z<2&&(mask&(1<<(x+z*2)))!=0;}
    private static void beam(List<Part> list,double x0,double y0,double x1,double y1,double depth,EnumFacing side,Grain grain) {
        if(x1>x0) list.add(new Part(new double[][]{{x0,y0},{x1,y0},{x1,y1},{x0,y1}},depth,depth+1.25,grain,side));
    }
    private static void brace(List<Part> list,double low,double high,double depth,EnumFacing side,boolean rising) {
        double w=1.15;
        double[][] outline={{0,0},{w,0},{16,16-w},{16,16},{16-w,16},{0,w}};
        if(!rising) {
            double[][] reflected=new double[outline.length][];
            for(int i=0;i<outline.length;i++) reflected[outline.length-i-1]=new double[]{16-outline[i][0],outline[i][1]};
            outline=reflected;
        }
        outline=WallGeometry.clip(WallGeometry.clip(outline,low,true),high,false);
        if(outline.length>=3) list.add(new Part(outline,depth+(rising?.25:.375),depth+(rising?1.15:1.1),rising?Grain.RISING:Grain.FALLING,side));
    }
}
