package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.nbt.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Read/rebuild/save a disposable copy of the player's actual house without changing materials. */
final class SmartReplayProbe {
    final WorldServer world;final MinecraftServer server;final Map<BlockPos,IBlockState> material=new HashMap<>();final List<Double> times=new ArrayList<>();int cob,thatch,timber,quiet;long before;final long deadline=System.nanoTime()+120000000000L;
    SmartReplayProbe(WorldServer world,MinecraftServer server){this.world=world;this.server=server;}
    static void start(WorldServer world,MinecraftServer server)throws Exception {
        SmartReplayProbe probe=new SmartReplayProbe(world,server);NBTTagCompound root;
        try(InputStream in=new FileInputStream(System.getProperty("iaa.replayLevel"))){root=CompressedStreamTools.readCompressed(in).getCompoundTag("Data");}
        NBTTagList pos=root.getCompoundTag("Player").getTagList("Pos",6);int cx=(pos.tagCount()>0?(int)Math.floor(pos.getDoubleAt(0)):root.getInteger("SpawnX"))>>4,cz=(pos.tagCount()>0?(int)Math.floor(pos.getDoubleAt(2)):root.getInteger("SpawnZ"))>>4;
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){Chunk chunk=world.getChunkFromChunkCoords(cx+dx,cz+dz);for(ExtendedBlockStorage section:chunk.getBlockStorageArray())if(section!=null&&!section.isEmpty())for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++){IBlockState state=section.get(x,y,z);BlockPos at=new BlockPos(chunk.xPosition*16+x,section.getYLocation()+y,chunk.zPosition*16+z);if(state.getBlock() instanceof SmartCobBlock){probe.cob++;probe.material.put(at,state);SmartCobEvents.changed(world,at);}else if(SmartThatchBlock.owner(state)){probe.thatch++;probe.material.put(at,state);SmartThatchEvents.changed(world,at);}else if(state.getBlock() instanceof SmartWallBlock){probe.timber++;probe.material.put(at,state);SmartWallEvents.changed(world,at);}}}
        if(probe.cob==0)throw new IllegalStateException("Replay did not find the player's cob house");FMLCommonHandler.instance().bus().register(probe);System.out.println("IAA_REPLAY_START cob="+probe.cob+" thatch="+probe.thatch+" timber="+probe.timber);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)public void before(TickEvent.WorldTickEvent e){if(e.world==world&&e.phase==TickEvent.Phase.START)before=System.nanoTime();}
    @SubscribeEvent(priority=EventPriority.LOWEST)public void after(TickEvent.WorldTickEvent e){if(e.world!=world||e.phase!=TickEvent.Phase.END)return;times.add((System.nanoTime()-before)/1e6);boolean pending=SmartWallEvents.pending(world)||SmartCobEvents.pending(world)||SmartThatchEvents.pending(world);quiet=pending?0:quiet+1;if(quiet<40&&System.nanoTime()<deadline)return;boolean preserved=true;for(Map.Entry<BlockPos,IBlockState> entry:material.entrySet())preserved&=world.getBlockState(entry.getKey()).equals(entry.getValue());if(pending||!preserved)throw new IllegalStateException("Replay failed to settle or changed saved materials");Collections.sort(times);long save=System.nanoTime();server.saveAllWorlds(false);double saveMs=(System.nanoTime()-save)/1e6;String json="{\"passed\":true,\"cob\":"+cob+",\"thatch\":"+thatch+",\"timber\":"+timber+",\"ownerStatesPreserved\":true,\"p95Ms\":"+times.get((times.size()-1)*95/100)+",\"maxMs\":"+times.get(times.size()-1)+",\"saveMs\":"+saveMs+",\"runtimeSha256\":\""+System.getProperty("iaa.runtimeSha256")+"\"}";try{Files.write(Paths.get(System.getProperty("iaa.receipt")),json.getBytes(StandardCharsets.UTF_8));}catch(Exception failure){throw new RuntimeException(failure);}System.out.println("IAA_REPLAY_PASS "+json);server.initiateShutdown();}
}
