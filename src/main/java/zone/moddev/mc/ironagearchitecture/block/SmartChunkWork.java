package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.*;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** One shared budget for loaded-border checks; saved shapes remain available meanwhile. */
public final class SmartChunkWork {
    private static final Map<World,LinkedHashMap<Chunk,LinkedHashMap<String,Runnable>>> QUEUED=new WeakHashMap<>();
    public static void enqueue(WorldServer world,Chunk chunk,String family,Runnable validation){
        world.addScheduledTask(()->QUEUED.computeIfAbsent(world,w->new LinkedHashMap<>()).computeIfAbsent(chunk,c->new LinkedHashMap<>()).put(family,validation));
    }
    public static boolean pending(World world){Map<Chunk,LinkedHashMap<String,Runnable>> queue=QUEUED.get(world);return queue!=null&&!queue.isEmpty();}
    public static void flush(WorldServer world){LinkedHashMap<Chunk,LinkedHashMap<String,Runnable>> queue=QUEUED.get(world);if(queue==null)return;long deadline=System.nanoTime()+2000000L;
        while(!queue.isEmpty()&&System.nanoTime()<deadline){Map.Entry<Chunk,LinkedHashMap<String,Runnable>> first=queue.entrySet().iterator().next();Chunk chunk=first.getKey();
            if(!world.isBlockLoaded(new BlockPos(chunk.xPosition*16,64,chunk.zPosition*16))||world.getChunkFromChunkCoords(chunk.xPosition,chunk.zPosition)!=chunk){queue.remove(chunk);continue;}
            Iterator<Runnable> jobs=first.getValue().values().iterator();Runnable job=jobs.next();jobs.remove();if(first.getValue().isEmpty())queue.remove(chunk);job.run();
        }
        if(queue.isEmpty())QUEUED.remove(world);
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public void tick(TickEvent.WorldTickEvent event){if(event.phase==TickEvent.Phase.END&&event.world instanceof WorldServer)flush((WorldServer)event.world);}
    @SubscribeEvent public void unloaded(ChunkEvent.Unload event){Map<Chunk,LinkedHashMap<String,Runnable>> queue=QUEUED.get(event.getWorld());if(queue!=null)queue.remove(event.getChunk());}
    @SubscribeEvent public void unloaded(WorldEvent.Unload event){QUEUED.remove(event.getWorld());}
}
