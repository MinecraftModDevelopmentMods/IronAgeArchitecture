package zone.moddev.mc.ironagearchitecture.probe;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Save before any geometry flush, then resume from the player JAR's persisted work list. */
final class PendingShapesProbe {
    static final BlockPos ORIGIN=new BlockPos(15,100,400);final WorldServer world;final MinecraftServer server;int quiet,ticks;
    PendingShapesProbe(WorldServer world,MinecraftServer server){this.world=world;this.server=server;}
    static void start(WorldServer world,MinecraftServer server,boolean reload)throws Exception {
        if(reload){FMLCommonHandler.instance().bus().register(new PendingShapesProbe(world,server));return;}
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)world.getChunkFromChunkCoords((ORIGIN.getX()>>4)+x,(ORIGIN.getZ()>>4)+z);
        for(int x=0;x<4;x++)for(int z=0;z<4;z++){if(x==0||x==3||z==0||z==3)for(int y=0;y<3;y++)world.setBlockState(ORIGIN.add(x,y,z),ArchitectureBlocks.SMART_COB.getDefaultState(),3);world.setBlockState(ORIGIN.add(x,3+Math.min(x,3-x),z),ArchitectureBlocks.SMART_THATCH.getDefaultState(),3);}
        for(int x=0;x<5;x++)for(int y=0;y<4;y++)world.setBlockState(ORIGIN.add(8+x,y,0),ArchitectureBlocks.findSmart("oak",0).getDefaultState(),3);
        if(SmartThatchEvents.pending(world))throw new IllegalStateException("Simple roofs must not queue shaping");
        world.setSpawnPoint(ORIGIN);server.saveAllWorlds(false);write(server,"{\"passed\":true,\"savedWithUnfinishedWork\":true,\"runtimeSha256\":\""+System.getProperty("iaa.runtimeSha256")+"\"}");
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)public void tick(TickEvent.WorldTickEvent e)throws Exception {
        if(e.world!=world||e.phase!=TickEvent.Phase.END)return;ticks++;boolean pending=SmartChunkWork.pending(world)||SmartCobEvents.pending(world)||SmartThatchEvents.pending(world)||SmartWallEvents.pending(world);quiet=pending?0:quiet+1;if(ticks>1200)throw new IllegalStateException("Saved shape work did not finish on reload");if(quiet<30)return;
        if(SmartCobData.get(world).profile(ORIGIN.up()).equals(SmartCobProfile.SQUARE))throw new IllegalStateException("Unfinished cob did not resume");BlockPos roof=ORIGIN.add(1,4,1);if(!SmartThatchBlock.owner(world.getBlockState(roof)))throw new IllegalStateException("Saved simple roof is missing");
        for(int x=0;x<5;x++)for(int y=0;y<4;y++){BlockPos at=ORIGIN.add(8+x,y,0);if(SmartWallData.get(world).code(at)==0||SmartShapeDirtyData.get(world).chunk(SmartShapeDirtyData.TIMBER,at.getX()>>4,at.getZ()>>4).containsKey(at.toLong()))throw new IllegalStateException("Timber faces did not finish");}
        server.saveAllWorlds(false);write(server,"{\"passed\":true,\"unfinishedWorkResumed\":true,\"runtimeSha256\":\""+System.getProperty("iaa.runtimeSha256")+"\"}");
    }
    static void write(MinecraftServer server,String json)throws Exception {Files.write(Paths.get(System.getProperty("iaa.receipt")),json.getBytes(StandardCharsets.UTF_8));System.out.println("IAA_PENDING_PASS "+json);server.initiateShutdown();}
}
