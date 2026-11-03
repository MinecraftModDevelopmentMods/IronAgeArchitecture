package zone.moddev.mc.ironagearchitecture.block;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Only nonempty border sections need cross-chunk ownership repair; never polls or force-loads. */
public final class WindowEvents {
    @SubscribeEvent public void tick(net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent event) {
        if(event.phase==net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END&&!event.world.isRemote&&event.world.getTotalWorldTime()%10==0) WindowController.retryEntities(event.world);
    }
    @SubscribeEvent public void loaded(ChunkEvent.Load event) {
        if(!(event.getWorld() instanceof WorldServer)||zone.moddev.mc.ironagearchitecture.ArchitectureBlocks.WINDOW_LEAF==null) return;
        WorldServer world=(WorldServer)event.getWorld();Chunk chunk=event.getChunk();
        world.addScheduledTask(()->{
            if(!world.isBlockLoaded(new BlockPos(chunk.xPosition*16,64,chunk.zPosition*16))) return;
            scan(world,chunk);
            for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) {
                if(dx==0&&dz==0) continue;
                int x=chunk.xPosition+dx,z=chunk.zPosition+dz;BlockPos pos=new BlockPos(x*16,64,z*16);
                if(world.isBlockLoaded(pos)) scan(world,world.getChunkFromBlockCoords(pos));
            }
        });
    }
    private void scan(WorldServer world,Chunk chunk) {
        ExtendedBlockStorage[] sections=chunk.getBlockStorageArray();
        for(int s=0;s<sections.length;s++) if(sections[s]!=null&&!sections[s].isEmpty()) for(int y=0;y<16;y++) for(int edge=0;edge<16;edge++) {
            for(int inset=0;inset<2;inset++) {
                repair(world,new BlockPos(chunk.xPosition*16+inset, s*16+y,chunk.zPosition*16+edge));
                repair(world,new BlockPos(chunk.xPosition*16+15-inset,s*16+y,chunk.zPosition*16+edge));
                repair(world,new BlockPos(chunk.xPosition*16+edge,s*16+y,chunk.zPosition*16+inset));
                repair(world,new BlockPos(chunk.xPosition*16+edge,s*16+y,chunk.zPosition*16+15-inset));
            }
        }
    }
    private void repair(WorldServer world,BlockPos pos){Block block=world.getBlockState(pos).getBlock();if(block instanceof WindowBlock) WindowController.neighbor(world,pos);else if(block instanceof WindowLeafBlock) WindowController.repair(world,pos);}
}
