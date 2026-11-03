package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Check actual generated bay endpoints, including the reversed local axis of rear-half walls. */
final class TimberJoinProbe {
    static void preview(World world) {
        int bays=0;
        for(int house:new int[]{0,40,62}) for(int floor=0;floor<(house==0?2:3);floor++) {
            double offset=floor==0?0:floor==1?.5:house==62?1.5:1;
            BlockPos low=new BlockPos(house+5,65+floor*5,(int)Math.floor(-offset)),high=low.east().up();
            if(!endpoint(world,low,house+6,66+floor*5)||!endpoint(world,high,house+6,66+floor*5))
                throw new IllegalStateException("Preview brace bay does not meet: "+low+" "+world.getBlockState(low)+" / "+high+" "+world.getBlockState(high));
            for(BlockPos at:new BlockPos[]{low.up(),low.east()}) if(TimberCorners.derive(world.getBlockState(at),world,at)==0)
                throw new IllegalStateException("Point-only brace join lacks a corner continuation at "+at);
            bays++;
        }
        verifyCorners(world);
        System.out.println("IAA_TIMBER_JOIN_PREVIEW_PASS continuousBraceBays="+bays+" cornerCases=96");
    }
    private static void verifyCorners(World world) {
        BlockPos at=new BlockPos(15,110,-64);
        for(int wood=0;wood<6;wood++) for(boolean thin:new boolean[]{false,true}) for(EnumFacing facing:EnumFacing.HORIZONTALS) for(int slope=0;slope<2;slope++) {
            FrameBlock host=thin?ArchitectureBlocks.findPartial(null,FrameFamily.INFILL,WallProfile.THIN):ArchitectureBlocks.DAUB;
            FrameBlock brace=thin?ArchitectureBlocks.findPartial(ArchitectureBlocks.WOODS[wood],FrameFamily.BRACE,WallProfile.THIN):ArchitectureBlocks.findWall(ArchitectureBlocks.WOODS[wood],FrameFamily.BRACE);
            IBlockState plain=host.getDefaultState().withProperty(FrameBlock.FACING,facing),beam=brace.getDefaultState().withProperty(FrameBlock.FACING,facing).withProperty(FrameBlock.PANEL,slope);
            BlockPos h=at.offset(facing.rotateY()),v=slope==0?at.down():at.up();
            for(EnumFacing side:EnumFacing.values()) world.setBlockToAir(at.offset(side));
            world.setBlockState(at,plain,2);world.setBlockState(h,beam,2);world.setBlockState(v,beam,2);
            int expected=(wood+1)<<((slope==0?1:3)*3),actual=TimberCorners.derive(plain,world,at);
            if(actual!=expected) throw new IllegalStateException("Missing/incorrect diagonal corner "+thin+" "+facing+" "+slope+" expected="+expected+" actual="+actual);
            java.util.List<WallGeometry.Part> parts=host.parts(plain,host.connections(plain,world,at),actual);int patches=0;
            for(WallGeometry.Part part:parts) if(part.timber!=null) {patches++;if(!part.timber.equals(ArchitectureBlocks.WOODS[wood])) throw new IllegalStateException("Wrong infill corner timber");}
            if(patches!=2) throw new IllegalStateException("Both elevations need a corner continuation");
            world.setBlockToAir(h);if(TimberCorners.derive(plain,world,at)!=0) throw new IllegalStateException("One brace alone produced a corner");
            world.setBlockState(h,beam,2);world.setBlockState(v,beam.withProperty(FrameBlock.PANEL,1-slope),2);
            if(TimberCorners.derive(plain,world,at)!=0) throw new IllegalStateException("Incompatible slopes produced a corner");
            FrameBlock other=thin?ArchitectureBlocks.findPartial(ArchitectureBlocks.WOODS[(wood+1)%6],FrameFamily.BRACE,WallProfile.THIN):ArchitectureBlocks.findWall(ArchitectureBlocks.WOODS[(wood+1)%6],FrameFamily.BRACE);
            world.setBlockState(v,other.getDefaultState().withProperty(FrameBlock.FACING,facing).withProperty(FrameBlock.PANEL,slope),2);
            if(TimberCorners.derive(plain,world,at)!=0) throw new IllegalStateException("Mixed woods produced a corner");
            world.setBlockState(v,beam.withProperty(FrameBlock.FACING,facing.getOpposite()),2);
            if(TimberCorners.derive(plain,world,at)!=0) throw new IllegalStateException("Offset/opposite brace produced a corner");
            world.setBlockToAir(at);for(EnumFacing side:EnumFacing.values()) world.setBlockToAir(at.offset(side));
        }
    }
    private static boolean endpoint(World world,BlockPos pos,double x,double y) {
        IBlockState state=world.getBlockState(pos);if(!(state.getBlock() instanceof FrameBlock)) return false;
        FrameBlock block=(FrameBlock)state.getBlock();if(block.family!=FrameFamily.BRACE) return false;
        int faces=0;
        for(WallGeometry.Part part:block.parts(state,block.connections(state,world,pos))) {
            if(part.grain!=WallGeometry.Grain.RISING&&part.grain!=WallGeometry.Grain.FALLING) continue;
            boolean found=false;
            for(double[] p:part.outline) {
                double[] point=part.transform(p[0],p[1],part.front,state.getValue(FrameBlock.FACING));
                if(Math.abs(pos.getX()+point[0]/16-x)<1e-7&&Math.abs(pos.getY()+point[1]/16-y)<1e-7) found=true;
            }
            if(found) faces++;
        }
        return faces>=2;
    }
}
