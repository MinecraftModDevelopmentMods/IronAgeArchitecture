package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.world.WorldServer;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Explicit test drain. Gameplay and performance probes use ordinary world ticks. */
final class ProbeShapes {
    static void flush(WorldServer world){long limit=System.nanoTime()+120000000000L;while(SmartChunkWork.pending(world)||SmartWallEvents.pending(world)||SmartCobEvents.pending(world)||SmartThatchEvents.pending(world)){SmartChunkWork.flush(world);SmartWallEvents.flush(world);SmartCobEvents.flush(world);SmartThatchEvents.flush(world);if(System.nanoTime()>limit)throw new IllegalStateException("Smart shape jobs failed to settle");java.util.concurrent.locks.LockSupport.parkNanos(100000L);}}
}
