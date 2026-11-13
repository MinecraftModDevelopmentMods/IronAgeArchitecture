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

    public static void changed(World world,BlockPos pos) {
        if(world.isRemote||!zone.moddev.mc.ironagearchitecture.ArchitectureConfig.smartCob) return;
        SmartCobData data=SmartCobData.get(world);
        LinkedHashSet<BlockPos> queue=DIRTY.computeIfAbsent(world,w->new LinkedHashSet<>());
        if(world.getBlockState(pos).getBlock() instanceof SmartCobBlock) {
            if(!data.contains(pos)) data.put(pos,SmartCobProfile.SQUARE);
            queue.add(pos.toImmutable());
        } else if(data.put(pos,null)) send(world,pos,Collections.singletonMap(pos.toLong(),null),false);
        for(int x=-2;x<=2;x++) for(int y=-2;y<=2;y++) for(int z=-2;z<=2;z++) {
            BlockPos at=pos.add(x,y,z);
            if(data.contains(at)||(Math.abs(x)<=1&&Math.abs(y)<=1&&Math.abs(z)<=1&&world.isBlockLoaded(at)
                &&world.getBlockState(at).getBlock() instanceof SmartCobBlock)) queue.add(at);
        }
    }

    public static boolean pending(World world){return DIRTY.containsKey(world)&&!DIRTY.get(world).isEmpty();}
    public static int flush(World world) {
        LinkedHashSet<BlockPos> queue=DIRTY.get(world);
        if(queue==null||queue.isEmpty()) return 0;
        SmartCobData data=SmartCobData.get(world);
        Set<BlockPos> visited=new HashSet<>();
        Map<Long,Map<Long,SmartCobProfile>> changed=new HashMap<>();
        int work=0,seeds=0;
        while(!queue.isEmpty()&&work<2048&&seeds++<128) {
            Iterator<BlockPos> iterator=queue.iterator();BlockPos seed=iterator.next();iterator.remove();
            if(visited.contains(seed)||!world.isBlockLoaded(seed)||!(world.getBlockState(seed).getBlock() instanceof SmartCobBlock)) continue;
            Map<BlockPos,SmartCobProfile> plan=SmartCobPlanner.survey(world,seed);
            if(plan==null) continue;
            work+=plan.size();visited.addAll(plan.keySet());queue.removeAll(plan.keySet());
            if(obstructed(world,data,plan)) {RETRIES.computeIfAbsent(world,w->new HashMap<>()).put(seed,world.getTotalWorldTime()+10);continue;}
            for(Map.Entry<BlockPos,SmartCobProfile> entry:plan.entrySet()) {
                BlockPos at=entry.getKey();
                if(data.put(at,entry.getValue())) changed.computeIfAbsent(SmartWallData.chunkKey(at.getX()>>4,at.getZ()>>4),k->new HashMap<>()).put(at.toLong(),entry.getValue());
            }
        }
        for(Map.Entry<Long,Map<Long,SmartCobProfile>> entry:changed.entrySet()) {
            int x=(int)(entry.getKey()>>32),z=(int)(long)entry.getKey();send(world,new BlockPos(x*16,0,z*16),entry.getValue(),false);
        }
        return work;
    }
    private static boolean obstructed(World world,SmartCobData data,Map<BlockPos,SmartCobProfile> plan) {
        for(Map.Entry<BlockPos,SmartCobProfile> entry:plan.entrySet()) {
            java.awt.geom.Area added=entry.getValue().area();added.subtract(data.profile(entry.getKey()).area());if(added.isEmpty())continue;
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
        world.addScheduledTask(()->{
            if(!world.isBlockLoaded(new BlockPos(chunk.xPosition*16,64,chunk.zPosition*16))) return;
            for(ExtendedBlockStorage section:chunk.getBlockStorageArray()) if(section!=null&&!section.isEmpty())
                for(int x=0;x<16;x++) for(int z=0;z<16;z++) for(int y=0;y<16;y++)
                    if(section.get(x,y,z).getBlock() instanceof SmartCobBlock)
                        changed(world,new BlockPos(chunk.xPosition*16+x,section.getYLocation()+y,chunk.zPosition*16+z));
            SmartCobData data=SmartCobData.get(world);
            for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++)
                for(long pos:data.chunk(chunk.xPosition+dx,chunk.zPosition+dz).keySet())
                    DIRTY.computeIfAbsent(world,w->new LinkedHashSet<>()).add(BlockPos.fromLong(pos));
        });
    }
    @SubscribeEvent public void watch(ChunkWatchEvent.Watch event) {
        World world=event.getPlayer().world;
        int x=event.getChunk().chunkXPos,z=event.getChunk().chunkZPos;
        WATCHERS.computeIfAbsent(world,w->new HashMap<>()).computeIfAbsent(SmartWallData.chunkKey(x,z),k->new HashSet<>()).add(event.getPlayer());
        SmartCobNetwork.send(event.getPlayer(),world.provider.getDimension(),x,z,true,SmartCobData.get(world).chunk(x,z));
    }
    @SubscribeEvent public void unwatch(ChunkWatchEvent.UnWatch event) {
        Map<Long,Set<EntityPlayerMP>> chunks=WATCHERS.get(event.getPlayer().world);
        if(chunks==null) return;
        long key=SmartWallData.chunkKey(event.getChunk().chunkXPos,event.getChunk().chunkZPos);
        Set<EntityPlayerMP> players=chunks.get(key);
        if(players!=null){players.remove(event.getPlayer());if(players.isEmpty())chunks.remove(key);}
    }
    @SubscribeEvent public void unloaded(WorldEvent.Unload event){DIRTY.remove(event.getWorld());WATCHERS.remove(event.getWorld());RETRIES.remove(event.getWorld());}

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
