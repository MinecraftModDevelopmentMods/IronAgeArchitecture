package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;

/** Repeatable plots above the existing city; all edits go through real player packets. */
final class MPPlan {
    static final int FLOOR=100;
    static final int[][] DISTRICTS={{11,11},{2,2},{20,2},{2,19},{20,19},{11,2},{11,19},{2,11},{20,11},{6,6}};
    static BlockPos plot(int id,boolean spread){if(spread){int[] p=DISTRICTS[id];return new BlockPos(32+p[0]*16,FLOOR,32+p[1]*16);}return new BlockPos(32+9*16+(id%5)*8,FLOOR,32+11*16+(id/5)*8);}
    static BlockPos destination(int id,boolean spread){return plot(spread?(id+3)%10:1,true);}
    static Vec3d position(BlockPos plot,boolean deck,int id){if(deck)return new Vec3d(plot.getX()+6.5,FLOOR+1,plot.getZ()+6.5);return new Vec3d((plot.getX()>>4)*16-.6,71,(plot.getZ()>>4)*16+2.5+(id%3)*.3);}
    static final class Op {final BlockPos at;final boolean remove,roof;Op(BlockPos p,boolean remove,boolean roof){at=p;this.remove=remove;this.roof=roof;}}
    static Block wall(int id){return id%3==2?ArchitectureBlocks.findSmart("oak",0):ArchitectureBlocks.SMART_COB;}
    static int finish(int id){return id%3==1?1:0;}
    static List<Op> steps(BlockPos plot){List<Op> out=new ArrayList<>();for(int y=1;y<=3;y++)for(int z=1;z<=5;z++)for(int x=1;x<=5;x++)if(x==1||x==5||z==1||z==5)if(!(x==3&&z==1&&y<3))out.add(new Op(plot.add(x,y,z),false,false));
        // Start on the supported perimeter, then fill in from its existing roof neighbours.
        for(int z=1;z<=5;z++)for(int x=1;x<=5;x++)if(x==1||x==5||z==1||z==5)out.add(new Op(plot.add(x,4,z),false,true));for(int z=2;z<=4;z++)for(int x=2;x<=4;x++)out.add(new Op(plot.add(x,4,z),false,true));
        List<Op> holes=new ArrayList<>();for(int[] p:new int[][]{{1,2,2},{1,2,3},{1,2,4},{5,2,2},{5,2,4}})holes.add(new Op(plot.add(p[0],p[1],p[2]),true,false));for(int[] p:new int[][]{{2,4,2},{3,4,2},{4,4,2},{2,4,3},{4,4,3}})holes.add(new Op(plot.add(p[0],p[1],p[2]),true,true));out.addAll(holes);for(Op op:holes)out.add(new Op(op.at,false,op.roof));return out;}
    static Map<BlockPos,Block> finalBlocks(int id,BlockPos plot){Map<BlockPos,Block> out=new HashMap<>();for(Op op:steps(plot)){if(op.remove)out.remove(op.at);else out.put(op.at,op.roof?ArchitectureBlocks.SMART_THATCH:wall(id));}return out;}
}
