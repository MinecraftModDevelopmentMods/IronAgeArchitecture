package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Measure timber contact across the shared block face, rather than trusting connection bits. */
final class HorizontalRailProbe {
    static void verify(World world) {
        BlockPos base=new BlockPos(15,105,-80);int links=0;
        for(String wood:ArchitectureBlocks.WOODS) for(int finish=0;finish<2;finish++) for(EnumFacing facing:EnumFacing.HORIZONTALS)
            for(WallProfile profile:new WallProfile[]{null,WallProfile.CORNER_L,WallProfile.CORNER_HALF_LEFT,WallProfile.CORNER_HALF_RIGHT,WallProfile.CORNER_QUARTER})
                for(boolean window:new boolean[]{false,true}) for(int height=0;height<2;height++) {
            if(window&&!ArchitectureConfig.windows) continue;
            EnumFacing right=facing.rotateY();BlockPos middle=base.offset(right),end=base.offset(right,2);
            clear(world,base);clear(world,middle);clear(world,end);
            FrameBlock corner=profile==null?ArchitectureBlocks.findWall(wood,FrameFamily.TERMINAL):ArchitectureBlocks.findPartial(wood,FrameFamily.TERMINAL,profile);
            EnumFacing plane=profile!=null&&profile.z0==8?facing.getOpposite():facing;
            FrameBlock post=profile==null?ArchitectureBlocks.findWall(wood,FrameFamily.POST):ArchitectureBlocks.findPartial(wood,FrameFamily.POST,WallProfile.THIN);
            IBlockState cs=corner.getDefaultState().withProperty(FrameBlock.FACING,facing).withProperty(FrameBlock.PANEL,finish*2),ps=post.getDefaultState().withProperty(FrameBlock.FACING,plane).withProperty(FrameBlock.PANEL,finish*2);
            IBlockState source;
            if(window) source=ArchitectureBlocks.findWindow(wood,finish,profile!=null,true,false,profile!=null&&profile.z0==8).getDefaultState().withProperty(WindowBlock.FACING,facing);
            else {FrameBlock rail=profile==null?ArchitectureBlocks.findWall(wood,FrameFamily.RAIL):ArchitectureBlocks.findPartial(wood,FrameFamily.RAIL,WallProfile.THIN);source=rail.getDefaultState().withProperty(FrameBlock.FACING,plane).withProperty(FrameBlock.PANEL,finish*2+height);}
            world.setBlockState(base,cs,2);world.setBlockState(middle,ps,2);world.setBlockState(end,source,2);
            if(!touch(world,base,middle,right,height)||!touch(world,middle,end,right,height)) throw new IllegalStateException("Horizontal rail gap: "+wood+" "+finish+" "+facing+" "+profile+" window="+window+" height="+height+" cornerMask="+corner.connections(cs,world,base)+" postMask="+post.connections(ps,world,middle));
            world.setBlockToAir(end);
            if((corner.connections(cs,world,base)&0xff0)!=0||(post.connections(ps,world,middle)&0xff0)!=0) throw new IllegalStateException("Rail remained after source removal");
            links++;
        }
        clear(world,base);for(EnumFacing side:EnumFacing.HORIZONTALS) for(int d=1;d<=3;d++) clear(world,base.offset(side,d));
        System.out.println("IAA_HORIZONTAL_RAIL_PASS linkedRuns="+links+" measuredFaces="+(links*2));
    }
    private static List<AxisAlignedBB> timber(World world,BlockPos at,int height) {
        IBlockState state=world.getBlockState(at);List<AxisAlignedBB> result=new ArrayList<>();
        double y0=at.getY()+(height==0?0:13.0/16),y1=at.getY()+(height==0?3.0/16:1);
        if(state.getBlock() instanceof FrameBlock) {
            FrameBlock block=(FrameBlock)state.getBlock();for(WallGeometry.Part part:block.parts(state,block.connections(state,world,at),TimberCorners.derive(state,world,at))) if(part.grain!=WallGeometry.Grain.DAUB) result.add(part.bounds(state.getValue(FrameBlock.FACING)).offset(at));
        } else if(state.getBlock() instanceof WindowBlock) {
            WindowBlock block=(WindowBlock)state.getBlock();result.addAll(block.boxes(state,world,at,false));for(int i=0;i<result.size();i++) result.set(i,result.get(i).offset(at));
        }
        result.removeIf(b->Math.min(b.maxY,y1)-Math.max(b.minY,y0)<1e-6);return result;
    }
    private static boolean touch(World world,BlockPos a,BlockPos b,EnumFacing direction,int height) {
        for(AxisAlignedBB x:timber(world,a,height)) for(AxisAlignedBB y:timber(world,b,height)) {
            boolean alongX=direction.getAxis()==EnumFacing.Axis.X,positive=direction.getAxisDirection()==EnumFacing.AxisDirection.POSITIVE;
            double edge=alongX?(positive?x.maxX:x.minX):(positive?x.maxZ:x.minZ),other=alongX?(positive?y.minX:y.maxX):(positive?y.minZ:y.maxZ);
            double overlap=alongX?Math.min(x.maxZ,y.maxZ)-Math.max(x.minZ,y.minZ):Math.min(x.maxX,y.maxX)-Math.max(x.minX,y.minX);
            if(Math.abs(edge-other)<1e-6&&overlap>1e-6&&Math.min(x.maxY,y.maxY)-Math.max(x.minY,y.minY)>1e-6) return true;
        }
        return false;
    }
    private static void clear(World world,BlockPos pos){world.setBlockToAir(pos);world.setBlockToAir(pos.up());world.setBlockToAir(pos.down());}
}
