package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Real world ticks, continuous placement and a cold/idle city. Never shipped to players. */
final class SmartPerformanceProbe {
    private final WorldServer world;private final MinecraftServer server;
    private final List<BlockPos> walls=new ArrayList<>(),roofs=new ArrayList<>();
    private final List<Double> timings=new ArrayList<>();private final List<String> rows=new ArrayList<>();
    private final int houses=Integer.getInteger("iaa.performanceHouses",1),rate=Integer.getInteger("iaa.performanceRate",4);
    private int warm=30,phase,index,settle,ticks;private long tickStart,phaseStart=System.nanoTime();
    private boolean finished;private final long deadline=System.nanoTime()+600000000000L;
    private SmartPerformanceProbe(WorldServer world,MinecraftServer server){this.world=world;this.server=server;}
    static void start(WorldServer world,MinecraftServer server) {
        SmartPerformanceProbe probe=new SmartPerformanceProbe(world,server);
        int across=(int)Math.ceil(Math.sqrt(probe.houses));
        // A dedicated server has no real player here. Keep the complete fixture in
        // its spawn area so chunk unloading is not mistaken for geometry latency.
        world.setSpawnPoint(new BlockPos(32+across*8,70,32+across*8));
        for(int i=0;i<probe.houses;i++) {
            int sx=32+(i%across)*16,sz=32+(i/across)*16;
            for(int x=-2;x<=10;x++)for(int z=-2;z<=8;z++)world.getChunkFromChunkCoords((sx+x)>>4,(sz+z)>>4);
            for(int y=0;y<5;y++)for(int x=0;x<9;x++)for(int z=0;z<7;z++)if(x==0||x==8||z==0||z==6)if(!(z==0&&(x==3||x==6)&&(y==1||y==2)))probe.walls.add(new BlockPos(sx+x,70+y,sz+z));
            for(int x=-1;x<=9;x++)for(int z=-1;z<=7;z++){int d=Math.min(Math.min(x+1,9-x),Math.min(z+1,7-z));probe.roofs.add(new BlockPos(sx+x,75+Math.max(0,d-1),sz+z));}
        }
        FMLCommonHandler.instance().bus().register(probe);
        System.out.println("IAA_PERF_START houses="+probe.houses+" walls="+probe.walls.size()+" roofs="+probe.roofs.size()+" blocksPerTick="+probe.rate);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void before(TickEvent.WorldTickEvent event) {
        if(event.world!=world||event.phase!=TickEvent.Phase.START||finished)return;
        tickStart=System.nanoTime();if(warm>0){warm--;return;}
        if(phase==0||phase==1) {
            List<BlockPos> cells=phase==0?walls:roofs;IBlockState state=phase==0?ArchitectureBlocks.SMART_COB.getDefaultState():ArchitectureBlocks.SMART_THATCH.getDefaultState();
            for(int n=0;n<rate&&index<cells.size();n++)world.setBlockState(cells.get(index++),state,3);
        } else if(phase==2&&ticks%5==0&&ticks<100) {
            BlockPos at=walls.get(0).north();if(world.isAirBlock(at))world.setBlockState(at,ArchitectureBlocks.SMART_COB.getDefaultState(),3);else world.setBlockToAir(at);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void after(TickEvent.WorldTickEvent event) {
        if(event.world!=world||event.phase!=TickEvent.Phase.END||finished||warm>0)return;
        timings.add((System.nanoTime()-tickStart)/1e6);ticks++;
        if(System.nanoTime()>deadline){finish(false,"wall-clock timeout");return;}
        boolean pending=SmartCobEvents.pending(world)||SmartThatchEvents.pending(world)||SmartWallEvents.pending(world);
        if(ticks%200==0)diagnostic();
        boolean placed=phase==0?index==walls.size():phase==1?index==roofs.size():ticks>=140;
        if(placed&&!pending)settle++;else settle=0;
        if(settle<10)return;
        Collections.sort(timings);double total=0;for(double t:timings)total+=t;
        String row="{\"stage\":\""+new String[]{"cob","thatch","city_edit_idle"}[phase]+"\",\"ticks\":"+ticks+",\"meanMs\":"+(total/timings.size())+",\"p95Ms\":"+timings.get((timings.size()-1)*95/100)+",\"p99Ms\":"+timings.get((timings.size()-1)*99/100)+",\"maxMs\":"+timings.get(timings.size()-1)+",\"elapsedSeconds\":"+((System.nanoTime()-phaseStart)/1e9)+"}";
        rows.add(row);System.out.println("IAA_PERF_STAGE "+row);phase++;index=settle=ticks=0;timings.clear();phaseStart=System.nanoTime();if(phase==3)finish(true,"");
    }
    private void finish(boolean passed,String reason) {
        finished=true;long saveStart=System.nanoTime();server.saveAllWorlds(false);double saveMs=(System.nanoTime()-saveStart)/1e6;String result="{\"passed\":"+passed+",\"houses\":"+houses+",\"blocksPerTick\":"+rate+",\"saveMs\":"+saveMs+",\"heapUsedMiB\":"+((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576.0)+",\"runtimeSha256\":\""+System.getProperty("iaa.runtimeSha256")+"\",\"reason\":\""+reason+"\",\"stages\":["+String.join(",",rows)+"]}";
        try{Files.write(Paths.get(System.getProperty("iaa.receipt","performance.json")),result.getBytes(StandardCharsets.UTF_8));}catch(Exception failure){throw new RuntimeException(failure);}
        System.out.println("IAA_PERF_RESULT "+result);server.initiateShutdown();
    }
    private void diagnostic(){StringBuilder out=new StringBuilder("IAA_PERF_QUEUE phase="+phase+" tick="+ticks+" nano="+System.nanoTime());try{for(Class<?> type:new Class<?>[]{SmartCobEvents.class,SmartThatchEvents.class,SmartWallEvents.class}){out.append(' ').append(type.getSimpleName()).append(':');for(String name:new String[]{"DIRTY","CHANGES","ACTIVE","RETRY"}){java.lang.reflect.Field f;try{f=type.getDeclaredField(name);}catch(NoSuchFieldException missing){continue;}f.setAccessible(true);Object value=((Map<?,?>)f.get(null)).get(world);if(value instanceof Collection)out.append(name).append('=').append(((Collection<?>)value).size()).append(',');else if(value!=null&&name.equals("ACTIVE"))for(java.lang.reflect.Field d:value.getClass().getDeclaredFields()){d.setAccessible(true);Object v=d.get(value);if(v instanceof java.util.concurrent.Future)out.append(d.getName()).append("Done=").append(((java.util.concurrent.Future<?>)v).isDone()).append(',');else if(v instanceof Boolean||v instanceof Number)out.append(d.getName()).append('=').append(v).append(',');}}}}catch(Exception e){throw new RuntimeException(e);}System.out.println(out);}
}
