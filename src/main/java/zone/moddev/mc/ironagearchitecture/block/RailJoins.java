package zone.moddev.mc.ironagearchitecture.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;

/** Complete a source-to-anchor run without recursively asking neighbouring blocks for their joins. */
final class RailJoins {
    private RailJoins(){ }
    static int bridge(IBlockState state,IBlockAccess world,BlockPos pos,int mask) {
        FrameBlock owner=(FrameBlock)state.getBlock();if(owner.family==FrameFamily.INFILL||owner.wood==null) return mask;
        int sources=mask;
        for(int height=0;height<2;height++) for(EnumFacing direction:EnumFacing.HORIZONTALS) {
            int bit=WallGeometry.bit(direction)<<(height==0?4:8),opposite=WallGeometry.bit(direction.getOpposite())<<(height==0?4:8);
            if((sources&bit)==0&&(sources&opposite)!=0&&anchor(world,state,pos,direction,height,owner.wood)) mask|=bit;
        }
        return mask;
    }
    private static IBlockState state(IBlockAccess world,BlockPos at){return world instanceof World&&!((World)world).isBlockLoaded(at)?Blocks.AIR.getDefaultState():world.getBlockState(at);}
    private static boolean touches(IBlockState a,BlockPos pa,IBlockState b,BlockPos pb,EnumFacing direction) {
        for(WallRuns.Run x:WallRuns.of(a,pa)) for(WallRuns.Run y:WallRuns.of(b,pb))
            if(x.tangent.getAxis()==direction.getAxis()&&x.touches(y,direction)) return true;
        return false;
    }
    private static boolean anchor(IBlockAccess world,IBlockState previous,BlockPos pos,EnumFacing direction,int height,String wood) {
        BlockPos before=pos;
        for(int distance=1;distance<=16;distance++) {
            BlockPos at=pos.offset(direction,distance);IBlockState next=state(world,at);
            if(!(next.getBlock() instanceof FrameBlock)||next.getBlock() instanceof JettyFloorBlock) return false;
            FrameBlock block=(FrameBlock)next.getBlock();
            if(!wood.equals(block.wood)||block.family==FrameFamily.INFILL||!touches(previous,before,next,at,direction)) return false;
            double bottom=height==0?0:13,top=bottom+3;
            for(WallGeometry.Part part:block.parts(next,0)) {
                if(part.grain==WallGeometry.Grain.DAUB) continue;
                double min=16,max=0;for(double[] p:part.outline){min=Math.min(min,p[1]);max=Math.max(max,p[1]);}
                if(Math.min(max,top)-Math.max(min,bottom)>1e-6) return true;
            }
            previous=next;before=at;
        }
        return false;
    }
}
