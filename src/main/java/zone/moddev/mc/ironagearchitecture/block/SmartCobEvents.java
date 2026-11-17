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
import zone.moddev.mc.ironagearchitecture.SmartCobNetwork;

/** Coalesced server work, bounded per tick. Idle walls are read directly from their saved layout. */
public final class SmartCobEvents {
    private static final Map<World,LinkedHashSet<BlockPos>> DIRTY=new WeakHashMap<>();
    private static final Map<World,Map<Long,Set<EntityPlayerMP>>> WATCHERS=new WeakHashMap<>();
    private static final Map<World,Map<BlockPos,Long>> RETRIES=new WeakHashMap<>();

    private static final Map<World,LinkedHashSet<BlockPos>> CHANGES=new WeakHashMap<>(),DEFERRED=new WeakHashMap<>();
    private static final Map<World,Work> ACTIVE=new WeakHashMap<>();
    private static final Map<World,Map<BlockPos,SmartCobPlanner.Snapshot>> LAYOUTS=new WeakHashMap<>();
    private static final class Work {
        final SmartCobPlanner.Snapshot snapshot;final java.util.concurrent.Future<Map<BlockPos,SmartCobProfile>> result;boolean stale;
        Work(SmartCobPlanner.Snapshot snapshot,java.util.concurrent.Future<Map<BlockPos,SmartCobProfile>> result){this.snapshot=snapshot;this.result=result;}
    }
    public static void changed(World world,BlockPos pos) {
        if(world.isRemote||!zone.moddev.mc.ironagearchitecture.ArchitectureConfig.smartCob||SmartThatchEvents.applying(world))return;
        SmartCobData data=SmartCobData.get(world);boolean relevant=world.isBlockLoaded(pos)&&world.getBlockState(pos).getBlock() instanceof SmartCobBlock;
        if(relevant&&!data.contains(pos))data.put(pos,SmartCobProfile.SQUARE);
        for(int x=-1;x<=1&&!relevant;x++)for(int z=-1;z<=1&&!relevant;z++)relevant=!data.chunk((pos.getX()>>4)+x,(pos.getZ()>>4)+z).isEmpty();
        if(relevant){CHANGES.computeIfAbsent(world,w->new LinkedHashSet<>()).add(pos.toImmutable());SmartShapeDirtyData.get(world).add(SmartShapeDirtyData.COB,pos,1);}
    }
    public static boolean pending(World world){return ACTIVE.containsKey(world)||!CHANGES.getOrDefault(world,new LinkedHashSet<>()).isEmpty()||!DIRTY.getOrDefault(world,new LinkedHashSet<>()).isEmpty();}
    public static int flush(World world) {
        long deadline=System.nanoTime()+2000000L;SmartCobData data=SmartCobData.get(world);LinkedHashSet<BlockPos> queue=DIRTY.computeIfAbsent(world,w->new LinkedHashSet<>()),edits=CHANGES.get(world);Work active=ACTIVE.get(world);int work=0;
        while(edits!=null&&!edits.isEmpty()&&System.nanoTime()<deadline) {
            Iterator<BlockPos> i=edits.iterator();BlockPos pos=i.next();i.remove();
            if(world.isBlockLoaded(pos)&&!(world.getBlockState(pos).getBlock() instanceof SmartCobBlock)){Map<BlockPos,SmartCobPlanner.Snapshot> known=LAYOUTS.get(world);if(known!=null)known.remove(pos);if(data.put(pos,null))send(world,pos,Collections.singletonMap(pos.toLong(),null),false);}
            if(!data.contains(pos))SmartShapeDirtyData.get(world).clear(SmartShapeDirtyData.COB,pos,1);
            for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++){BlockPos at=pos.add(x,y,z);if(active!=null&&active.snapshot.members.contains(at))active.stale=true;if(data.contains(at)){queue.add(at);SmartShapeDirtyData.get(world).add(SmartShapeDirtyData.COB,at,1);}}
        }
        if(edits!=null&&!edits.isEmpty())return work;
        if(active!=null) {
            if(!active.result.isDone())return work;
            ACTIVE.remove(world);Map<BlockPos,SmartCobProfile> plan=SmartShapeWorker.completed(active.result);
            if(active.stale||plan==null){queue.addAll(active.snapshot.members);return work;}
            for(BlockPos at:active.snapshot.members)if(!world.isBlockLoaded(at)){DEFERRED.computeIfAbsent(world,w->new LinkedHashSet<>()).addAll(active.snapshot.members);return work;}
            if(obstructed(world,data,plan)){RETRIES.computeIfAbsent(world,w->new HashMap<>()).put(active.snapshot.members.iterator().next(),world.getTotalWorldTime()+10);return work;}
            Map<Long,Map<Long,SmartCobProfile>> changes=new HashMap<>();
            for(Map.Entry<BlockPos,SmartCobProfile> entry:plan.entrySet())if(data.put(entry.getKey(),entry.getValue())){BlockPos at=entry.getKey();changes.computeIfAbsent(SmartWallData.chunkKey(at.getX()>>4,at.getZ()>>4),k->new HashMap<>()).put(at.toLong(),entry.getValue());SmartThatchEvents.changed(world,at);work++;}
            for(BlockPos at:active.snapshot.members)SmartShapeDirtyData.get(world).clear(SmartShapeDirtyData.COB,at,1);
            Map<BlockPos,SmartCobPlanner.Snapshot> layouts=LAYOUTS.computeIfAbsent(world,w->new HashMap<>());Set<SmartCobPlanner.Snapshot> previous=new HashSet<>();for(BlockPos at:active.snapshot.members){SmartCobPlanner.Snapshot old=layouts.get(at);if(old!=null)previous.add(old);}for(SmartCobPlanner.Snapshot old:previous)for(BlockPos at:old.members)layouts.remove(at);for(BlockPos at:active.snapshot.members)layouts.put(at,active.snapshot);
            for(Map.Entry<Long,Map<Long,SmartCobProfile>> entry:changes.entrySet())send(world,new BlockPos((int)(entry.getKey()>>32)*16,0,(int)(long)entry.getKey()*16),entry.getValue(),false);
        }
        while(!queue.isEmpty()&&System.nanoTime()<deadline) {
            Iterator<BlockPos> i=queue.iterator();BlockPos seed=i.next();i.remove();if(!world.isBlockLoaded(seed)||!(world.getBlockState(seed).getBlock() instanceof SmartCobBlock))continue;
            SmartCobPlanner.Snapshot snapshot=SmartCobPlanner.capture(world,seed);if(snapshot==null){DEFERRED.computeIfAbsent(world,w->new LinkedHashSet<>()).add(seed);continue;}
            if(!data.profile(seed).equals(SmartCobProfile.SQUARE)&&snapshot.sameLayout(LAYOUTS.getOrDefault(world,Collections.emptyMap()).get(seed))){queue.removeAll(snapshot.members);for(BlockPos at:snapshot.members)SmartShapeDirtyData.get(world).clear(SmartShapeDirtyData.COB,at,1);continue;}
            queue.removeAll(snapshot.members);java.util.concurrent.Future<Map<BlockPos,SmartCobProfile>> result=SmartShapeWorker.submit(()->SmartCobPlanner.solve(snapshot));
            if(result==null){queue.addAll(snapshot.members);return work;}
            ACTIVE.put(world,new Work(snapshot,result));return work;
        }return work;
    }
    private static boolean obstructed(World world,SmartCobData data,Map<BlockPos,SmartCobProfile> plan) {
        for(Map.Entry<BlockPos,SmartCobProfile> entry:plan.entrySet()) {
            if(entry.getValue().equals(data.profile(entry.getKey())))continue;java.awt.geom.Area added=entry.getValue().area();added.subtract(data.profile(entry.getKey()).area());if(added.isEmpty())continue;
            BlockPos pos=entry.getKey();
            for(net.minecraft.entity.Entity entity:world.getEntitiesWithinAABB(net.minecraft.entity.Entity.class,new AxisAlignedBB(pos))) {
                if(entity.isDead||entity.noClip||!entity.canBeCollidedWith())continue;
                AxisAlignedBB box=entity.getEntityBoundingBox();
                if(added.intersects(box.minX-pos.getX(),box.minZ-pos.getZ(),box.maxX-box.minX,box.maxZ-box.minZ))return true;
            }
        }
        return false;
    }

    private static void send(World world,BlockPos at,Map<Long,SmartCobProfile> values,boolean replace) {
        int x=at.getX()>>4,z=at.getZ()>>4;
        Set<EntityPlayerMP> players=WATCHERS.getOrDefault(world,Collections.emptyMap()).get(SmartWallData.chunkKey(x,z));
        if(players!=null) for(EntityPlayerMP player:new ArrayList<>(players))
            SmartCobNetwork.send(player,world.provider.getDimension(),x,z,replace,values);
    }

    @SubscribeEvent public void loaded(WorldEvent.Load event) {
        if(event.getWorld().isRemote) return;
        event.getWorld().addEventListener(new Listener());
    }
    @SubscribeEvent public void tick(TickEvent.WorldTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||event.world.isRemote)return;
        Map<BlockPos,Long> retries=RETRIES.get(event.world);
        if(retries!=null)for(Iterator<Map.Entry<BlockPos,Long>> i=retries.entrySet().iterator();i.hasNext();) {
            Map.Entry<BlockPos,Long> next=i.next();if(next.getValue()<=event.world.getTotalWorldTime()) {DIRTY.computeIfAbsent(event.world,w->new LinkedHashSet<>()).add(next.getKey());i.remove();}
        }
        flush(event.world);
    }
    @SubscribeEvent public void chunkLoaded(ChunkEvent.Load event) {
        if(!(event.getWorld() instanceof WorldServer)) return;
        WorldServer world=(WorldServer)event.getWorld();Chunk chunk=event.getChunk();
        SmartChunkWork.enqueue(world,chunk,"cob",()->{
            if(!world.isBlockLoaded(new BlockPos(chunk.xPosition*16,64,chunk.zPosition*16))) return;
            for(ExtendedBlockStorage section:chunk.getBlockStorageArray()) if(section!=null&&!section.isEmpty())
                for(int x=0;x<16;x++) for(int z=0;z<16;z++) for(int y=0;y<16;y++)
                    if(section.get(x,y,z).getBlock() instanceof SmartCobBlock)
                        {BlockPos at=new BlockPos(chunk.xPosition*16+x,section.getYLocation()+y,chunk.zPosition*16+z);if(!SmartCobData.get(world).contains(at)||SmartCobData.get(world).profile(at).equals(SmartCobProfile.SQUARE))changed(world,at);}
            for(long pos:new HashSet<>(SmartShapeDirtyData.get(world).chunk(SmartShapeDirtyData.COB,chunk.xPosition,chunk.zPosition).keySet()))changed(world,BlockPos.fromLong(pos));
            LinkedHashSet<BlockPos> deferred=DEFERRED.get(world);
            if(deferred!=null)for(Iterator<BlockPos> i=deferred.iterator();i.hasNext();){BlockPos at=i.next();if(Math.abs((at.getX()>>4)-chunk.xPosition)<=4&&Math.abs((at.getZ()>>4)-chunk.zPosition)<=4){DIRTY.computeIfAbsent(world,w->new LinkedHashSet<>()).add(at);i.remove();}}
        });
    }
    @SubscribeEvent public void watch(ChunkWatchEvent.Watch event) {
        World world=event.getPlayer().world;
        int x=event.getChunk().chunkXPos,z=event.getChunk().chunkZPos;
        WATCHERS.computeIfAbsent(world,w->new HashMap<>()).computeIfAbsent(SmartWallData.chunkKey(x,z),k->new HashSet<>()).add(event.getPlayer());
        SmartCobNetwork.send(event.getPlayer(),world.provider.getDimension(),x,z,true,SmartCobData.get(world).chunk(x,z));
    }
    @SubscribeEvent public void unwatch(ChunkWatchEvent.UnWatch event) {
        SmartCobNetwork.unwatch(event.getPlayer(),event.getChunk().chunkXPos,event.getChunk().chunkZPos);
        Map<Long,Set<EntityPlayerMP>> chunks=WATCHERS.get(event.getPlayer().world);
        if(chunks==null) return;
        long key=SmartWallData.chunkKey(event.getChunk().chunkXPos,event.getChunk().chunkZPos);
        Set<EntityPlayerMP> players=chunks.get(key);
        if(players!=null){players.remove(event.getPlayer());if(players.isEmpty())chunks.remove(key);}
    }
    @SubscribeEvent public void unloaded(WorldEvent.Unload event){DIRTY.remove(event.getWorld());WATCHERS.remove(event.getWorld());RETRIES.remove(event.getWorld());CHANGES.remove(event.getWorld());DEFERRED.remove(event.getWorld());LAYOUTS.remove(event.getWorld());Work active=ACTIVE.remove(event.getWorld());if(active!=null)active.result.cancel(true);}

    private static final class Listener implements IWorldEventListener {
        public void notifyBlockUpdate(World w,BlockPos p,IBlockState old,IBlockState next,int flags){if(old==next||old.getBlock() instanceof SmartCobBlock&&next.getBlock() instanceof SmartCobBlock)return;if((SmartThatchBlock.roof(old)||old.getBlock().isAir(old,w,p))&&(SmartThatchBlock.roof(next)||next.getBlock().isAir(next,w,p)))return;changed(w,p);}
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
