package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import zone.moddev.mc.ironagearchitecture.SmartWallNetwork;

/** Coalesced server work, bounded per tick. Idle walls are read directly from their saved layout. */
public final class SmartWallEvents {
    private static final Map<World,LinkedHashSet<FaceSeed>> DIRTY=new WeakHashMap<>();
    private static final Map<World,LinkedHashSet<BlockPos>> CHANGES=new WeakHashMap<>(),DEFERRED=new WeakHashMap<>();
    private static final Map<World,Work> ACTIVE=new WeakHashMap<>();
    private static final Map<World,Map<Long,Set<EntityPlayerMP>>> WATCHERS=new WeakHashMap<>();

    private static final class FaceSeed {
        final BlockPos pos;final EnumFacing side;
        FaceSeed(BlockPos pos,EnumFacing side){this.pos=pos;this.side=side;}
        @Override public int hashCode(){return 31*pos.hashCode()+side.ordinal();}
        @Override public boolean equals(Object value){return value instanceof FaceSeed&&pos.equals(((FaceSeed)value).pos)&&side==((FaceSeed)value).side;}
    }
    private static final class Work {
        final SmartWallPlanner.Snapshot input;final java.util.concurrent.Future<SmartWallPlanner.Plan> result;boolean stale;
        Work(SmartWallPlanner.Snapshot input,java.util.concurrent.Future<SmartWallPlanner.Plan> result){this.input=input;this.result=result;}
    }
    public static void changed(World world,BlockPos pos) {
        if(world.isRemote)return;SmartWallData data=SmartWallData.get(world);boolean relevant=world.isBlockLoaded(pos)&&world.getBlockState(pos).getBlock() instanceof SmartWallBlock;
        if(relevant&&data.code(pos)==0)data.put(pos,SmartWallGeometry.FALLBACK);
        for(int x=-1;x<=1&&!relevant;x++)for(int z=-1;z<=1&&!relevant;z++)relevant=!data.chunk((pos.getX()>>4)+x,(pos.getZ()>>4)+z).isEmpty();
        if(relevant){CHANGES.computeIfAbsent(world,w->new LinkedHashSet<>()).add(pos.toImmutable());SmartShapeDirtyData.get(world).add(SmartShapeDirtyData.TIMBER,pos,15);}
    }
    public static boolean pending(World world){return ACTIVE.containsKey(world)||!CHANGES.getOrDefault(world,new LinkedHashSet<>()).isEmpty()||!DIRTY.getOrDefault(world,new LinkedHashSet<>()).isEmpty();}
    public static int flush(World world) {
        long deadline=System.nanoTime()+1500000L;SmartWallData data=SmartWallData.get(world);LinkedHashSet<FaceSeed> queue=DIRTY.computeIfAbsent(world,w->new LinkedHashSet<>());LinkedHashSet<BlockPos> edits=CHANGES.get(world);Work active=ACTIVE.get(world);
        while(edits!=null&&!edits.isEmpty()&&System.nanoTime()<deadline){Iterator<BlockPos> i=edits.iterator();BlockPos pos=i.next();i.remove();if(world.isBlockLoaded(pos)&&!(world.getBlockState(pos).getBlock() instanceof SmartWallBlock)&&data.put(pos,0))send(world,pos,Collections.singletonMap(pos.toLong(),0),false);if(data.code(pos)==0)SmartShapeDirtyData.get(world).clear(SmartShapeDirtyData.TIMBER,pos,15);
            for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++){BlockPos at=pos.add(x,y,z);if(active!=null&&active.input.members.contains(at))active.stale=true;if(data.code(at)!=0){SmartShapeDirtyData.get(world).add(SmartShapeDirtyData.TIMBER,at,15);for(EnumFacing side:EnumFacing.HORIZONTALS)queue.add(new FaceSeed(at,side));}}}
        if(edits!=null&&!edits.isEmpty())return 0;
        int changed=0;
        if(active!=null){if(!active.result.isDone())return 0;ACTIVE.remove(world);SmartWallPlanner.Plan plan=SmartShapeWorker.completed(active.result);if(active.stale||plan==null){for(BlockPos p:active.input.members)queue.add(new FaceSeed(p,active.input.normal));return 0;}
            for(BlockPos p:active.input.members)if(!world.isBlockLoaded(p)){DEFERRED.computeIfAbsent(world,w->new LinkedHashSet<>()).addAll(active.input.members);return 0;}
            Map<Long,Map<Long,Integer>> updates=new HashMap<>();int shift=plan.normal.getHorizontalIndex()*8;
            for(Map.Entry<BlockPos,Integer> e:plan.cells.entrySet()){BlockPos at=e.getKey();int next=(data.code(at)&~(255<<shift))|(e.getValue()<<shift);SmartShapeDirtyData.get(world).clear(SmartShapeDirtyData.TIMBER,at,1<<plan.normal.getHorizontalIndex());if(data.put(at,next)){updates.computeIfAbsent(SmartWallData.chunkKey(at.getX()>>4,at.getZ()>>4),k->new HashMap<>()).put(at.toLong(),next);changed++;}}
            for(Map.Entry<Long,Map<Long,Integer>> e:updates.entrySet())send(world,new BlockPos((int)(e.getKey()>>32)*16,0,(int)(long)e.getKey()*16),e.getValue(),false);
        }
        while(!queue.isEmpty()&&System.nanoTime()<deadline){Iterator<FaceSeed> i=queue.iterator();FaceSeed seed=i.next();i.remove();if(!world.isBlockLoaded(seed.pos)||!(world.getBlockState(seed.pos).getBlock() instanceof SmartWallBlock))continue;
            SmartWallPlanner.Snapshot input=SmartWallPlanner.capture(world,seed.pos,seed.side);if(input==null){DEFERRED.computeIfAbsent(world,w->new LinkedHashSet<>()).add(seed.pos);continue;}for(BlockPos p:input.members)queue.remove(new FaceSeed(p,seed.side));java.util.concurrent.Future<SmartWallPlanner.Plan> result=SmartShapeWorker.submit(()->SmartWallPlanner.solve(input));if(result==null){for(BlockPos p:input.members)queue.add(new FaceSeed(p,seed.side));return changed;}ACTIVE.put(world,new Work(input,result));return changed;
        }return changed;
    }
    private static void send(World world,BlockPos at,Map<Long,Integer> values,boolean replace) {
        int x=at.getX()>>4,z=at.getZ()>>4;
        Set<EntityPlayerMP> players=WATCHERS.getOrDefault(world,Collections.emptyMap()).get(SmartWallData.chunkKey(x,z));
        if(players!=null) for(EntityPlayerMP player:new ArrayList<>(players))
            SmartWallNetwork.send(player,world.provider.getDimension(),x,z,replace,values);
    }

    @SubscribeEvent public void loaded(WorldEvent.Load event) {
        if(event.getWorld().isRemote) return;
        event.getWorld().addEventListener(new Listener());
    }
    @SubscribeEvent public void tick(TickEvent.WorldTickEvent event) {
        if(event.phase==TickEvent.Phase.END&&!event.world.isRemote) flush(event.world);
    }
    @SubscribeEvent public void chunkLoaded(ChunkEvent.Load event) {
        if(!(event.getWorld() instanceof WorldServer)) return;
        WorldServer world=(WorldServer)event.getWorld();Chunk chunk=event.getChunk();
        SmartChunkWork.enqueue(world,chunk,"timber",()->{
            if(!world.isBlockLoaded(new BlockPos(chunk.xPosition*16,64,chunk.zPosition*16))) return;
            for(ExtendedBlockStorage section:chunk.getBlockStorageArray()) if(section!=null&&!section.isEmpty())
                for(int x=0;x<16;x++) for(int z=0;z<16;z++) for(int y=0;y<16;y++)
                    if(section.get(x,y,z).getBlock() instanceof SmartWallBlock)
                        {BlockPos at=new BlockPos(chunk.xPosition*16+x,section.getYLocation()+y,chunk.zPosition*16+z);if(SmartWallData.get(world).code(at)==0)changed(world,at);}
            for(long pos:new HashSet<>(SmartShapeDirtyData.get(world).chunk(SmartShapeDirtyData.TIMBER,chunk.xPosition,chunk.zPosition).keySet()))changed(world,BlockPos.fromLong(pos));
            LinkedHashSet<BlockPos> deferred=DEFERRED.get(world);if(deferred!=null)for(Iterator<BlockPos> i=deferred.iterator();i.hasNext();){BlockPos at=i.next();if(Math.abs((at.getX()>>4)-chunk.xPosition)<=4&&Math.abs((at.getZ()>>4)-chunk.zPosition)<=4){changed(world,at);i.remove();}}
        });
    }
    @SubscribeEvent public void watch(ChunkWatchEvent.Watch event) {
        World world=event.getPlayer().world;
        int x=event.getChunk().chunkXPos,z=event.getChunk().chunkZPos;
        WATCHERS.computeIfAbsent(world,w->new HashMap<>()).computeIfAbsent(SmartWallData.chunkKey(x,z),k->new HashSet<>()).add(event.getPlayer());
        SmartWallNetwork.send(event.getPlayer(),world.provider.getDimension(),x,z,true,SmartWallData.get(world).chunk(x,z));
    }
    @SubscribeEvent public void unwatch(ChunkWatchEvent.UnWatch event) {
        Map<Long,Set<EntityPlayerMP>> chunks=WATCHERS.get(event.getPlayer().world);
        if(chunks==null) return;
        long key=SmartWallData.chunkKey(event.getChunk().chunkXPos,event.getChunk().chunkZPos);
        Set<EntityPlayerMP> players=chunks.get(key);
        if(players!=null){players.remove(event.getPlayer());if(players.isEmpty())chunks.remove(key);}
    }
    @SubscribeEvent public void unloaded(WorldEvent.Unload event){DIRTY.remove(event.getWorld());WATCHERS.remove(event.getWorld());CHANGES.remove(event.getWorld());DEFERRED.remove(event.getWorld());Work active=ACTIVE.remove(event.getWorld());if(active!=null)active.result.cancel(true);}

    private static final class Listener implements IWorldEventListener {
        public void notifyBlockUpdate(World w,BlockPos p,IBlockState old,IBlockState next,int flags){changed(w,p);}
        public void notifyLightSet(BlockPos pos){}
        public void markBlockRangeForRenderUpdate(int a,int b,int c,int d,int e,int f){}
        public void playSoundToAllNearExcept(net.minecraft.entity.player.EntityPlayer p,net.minecraft.util.SoundEvent s,net.minecraft.util.SoundCategory c,double x,double y,double z,float v,float pitch){}
        public void playRecord(net.minecraft.util.SoundEvent sound,BlockPos pos){}
        public void spawnParticle(int id,boolean ignore,double x,double y,double z,double vx,double vy,double vz,int...data){}
        public void onEntityAdded(net.minecraft.entity.Entity entity){}
        public void onEntityRemoved(net.minecraft.entity.Entity entity){}
        public void broadcastSound(int id,BlockPos pos,int data){}
        public void playEvent(net.minecraft.entity.player.EntityPlayer p,int type,BlockPos pos,int data){}
        public void sendBlockBreakProgress(int id,BlockPos pos,int progress){}
    }
}
