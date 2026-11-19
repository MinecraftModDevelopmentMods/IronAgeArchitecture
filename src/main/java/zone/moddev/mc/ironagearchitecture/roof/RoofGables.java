package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Two bounded wall-course samples, taken when a drawing changes, never while rendering. */
public final class RoofGables {
    public static final String[] NAMES={"Open","Match walls","Cob","Limewashed cob","Daub","Limewashed daub","Hanging tiles"};
    private RoofGables(){}
    public static int material(RoofPlan p,int end){return p.gableFill[end]==0?-1:p.gableFill[end]==1?p.gableMaterial[end]:p.gableFill[end]-2;}
    public static int wood(RoofPlan p,int end){return p.gableFill[end]==1?p.gableWood[end]:p.wood;}
    public static boolean part(RoofPart p){return p.id.startsWith("gable:");}
    public static void detect(World world,RoofPlan p){
        for(int end=0;end<2;end++){
            int[] votes=new int[30];int first=(p.alongX?p.minZ:p.minX)-p.wallInset,last=(p.alongX?p.maxZ:p.maxX)+p.wallInset;
            int v=end==0?(p.alongX?p.minX:p.minZ)-p.wallInset:(p.alongX?p.maxX:p.maxZ)+p.wallInset;
            for(int u=first;u<=last;u++){
                BlockPos at=p.alongX?new BlockPos(v,p.floorY-(p.wallInset==1?0:1),u):new BlockPos(u,p.floorY-(p.wallInset==1?0:1),v);
                if(!world.isBlockLoaded(at))continue;int style=style(world.getBlockState(at),p.wood);if(style>=0)votes[style]++;
            }
            int best=-1;for(int i=0;i<votes.length;i++)if(votes[i]>0&&(best<0||votes[i]>votes[best]))best=i;
            Block fallback=Block.getBlockFromName("ironagearchitecture:limewashed_cob");
            p.gableMaterial[end]=best<0?(fallback==null||fallback==Blocks.AIR?-1:1):best/6;
            p.gableWood[end]=best<0?p.wood:best%6;
        }
    }
    private static int style(IBlockState s,int wood){
        Block b=s.getBlock();int finish;
        if(b instanceof SmartCobBlock)return s.getValue(SmartCobBlock.FINISH)*6+wood;
        if(b instanceof FrameBlock){FrameBlock f=(FrameBlock)b;finish=f.finish(s.getValue(FrameBlock.PANEL));int w=Arrays.asList(ArchitectureBlocks.WOODS).indexOf(f.wood);return (2+finish)*6+(w<0?wood:w);}
        if(b instanceof WindowBlock){WindowBlock w=(WindowBlock)b;return (2+w.finish)*6+Math.max(0,Arrays.asList(ArchitectureBlocks.WOODS).indexOf(w.wood));}
        String name=b.getRegistryName()==null?"":b.getRegistryName().toString();
        if(name.equals("ironagearchitecture:wattle_and_daub"))return (2+(b.getMetaFromState(s)&1))*6+wood;
        if(name.equals("ironagearchitecture:cob"))return wood;
        if(name.equals("ironagearchitecture:limewashed_cob"))return 6+wood;
        if(name.startsWith("ironagearchitecture:clay_tile_hanging"))return 24+wood;
        return -1;
    }
    /** Existing wall blocks and windows remain independent of the paid roof assembly. */
    public static void preserveExisting(World world,RoofPlan p){
        for(Map.Entry<Long,RoofCell> e:new ArrayList<>(p.cells.entrySet())){
            BlockPos at=BlockPos.fromLong(e.getKey());if(!world.isBlockLoaded(at))continue;
            IBlockState state=world.getBlockState(at);Block b=state.getBlock();
            if(RoofDesignInteraction.host(state)||!(state.isFullCube()||b instanceof SmartCobBlock||b instanceof FrameBlock||b instanceof WindowBlock))continue;
            RoofCell c=e.getValue();List<RoofPart> kept=new ArrayList<>();for(RoofPart part:c.parts)if(!part(part))kept.add(part);
            if(kept.size()==c.parts.size())continue;
            RoofCell next=new RoofCell(c.plan,c.owner,kept,c.windows);if(next.empty())p.cells.remove(e.getKey());else p.cells.put(e.getKey(),next);
        }
    }
}
