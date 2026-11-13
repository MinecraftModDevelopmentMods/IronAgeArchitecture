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
    private static final Map<World,LinkedHashSet<BlockPos>> DIRTY=new WeakHashMap<>();
    private static final Map<World,Map<Long,Set<EntityPlayerMP>>> WATCHERS=new WeakHashMap<>();

    public static void changed(World world,BlockPos pos) {
        if(world.isRemote) return;
        SmartWallData data=SmartWallData.get(world);
        LinkedHashSet<BlockPos> queue=DIRTY.computeIfAbsent(world,w->new LinkedHashSet<>());
        if(world.getBlockState(pos).getBlock() instanceof SmartWallBlock) {
            if(data.code(pos)==0) data.put(pos,SmartWallGeometry.FALLBACK);
            queue.add(pos.toImmutable());
        } else if(data.put(pos,0)) send(world,pos,Collections.singletonMap(pos.toLong(),0),false);
        for(int x=-2;x<=2;x++) for(int y=-2;y<=2;y++) for(int z=-2;z<=2;z++) {
            BlockPos at=pos.add(x,y,z);
            if(data.code(at)!=0||(Math.abs(x)<=1&&Math.abs(y)<=1&&Math.abs(z)<=1&&world.isBlockLoaded(at)
                &&world.getBlockState(at).getBlock() instanceof SmartWallBlock)) queue.add(at);
        }
    }

    public static boolean pending(World world){return DIRTY.containsKey(world)&&!DIRTY.get(world).isEmpty();}
    public static int flush(World world) {
        LinkedHashSet<BlockPos> queue=DIRTY.get(world);
        if(queue==null||queue.isEmpty()) return 0;
        SmartWallData data=SmartWallData.get(world);
        Set<String> visited=new HashSet<>();
        Map<Long,Map<Long,Integer>> changed=new HashMap<>();
        int work=0, seeds=0;
        while(!queue.isEmpty()&&work<2048&&seeds++<512) {
            Iterator<BlockPos> iterator=queue.iterator();BlockPos seed=iterator.next();iterator.remove();
            if(!world.isBlockLoaded(seed)||!(world.getBlockState(seed).getBlock() instanceof SmartWallBlock)) continue;
            for(EnumFacing side:EnumFacing.HORIZONTALS) {
                if(visited.contains(seed.toLong()+"/"+side)) continue;
                SmartWallPlanner.Plan plan=SmartWallPlanner.survey(world,seed,side);
                if(plan==null) continue;
                work+=plan.cells.size();
                for(Map.Entry<BlockPos,Integer> entry:plan.cells.entrySet()) {
                    BlockPos at=entry.getKey();
                    visited.add(at.toLong()+"/"+side);
                    int shift=side.getHorizontalIndex()*8;
                    int next=(data.code(at)&~(255<<shift))|(entry.getValue()<<shift);
                    if(data.put(at,next)) changed.computeIfAbsent(SmartWallData.chunkKey(at.getX()>>4,at.getZ()>>4),k->new HashMap<>()).put(at.toLong(),next);
                }
            }
        }
        for(Map.Entry<Long,Map<Long,Integer>> entry:changed.entrySet()) {
            int x=(int)(entry.getKey()>>32),z=(int)(long)entry.getKey();
            send(world,new BlockPos(x*16,0,z*16),entry.getValue(),false);
        }
        return work;
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
        world.addScheduledTask(()->{
            if(!world.isBlockLoaded(new BlockPos(chunk.xPosition*16,64,chunk.zPosition*16))) return;
            for(ExtendedBlockStorage section:chunk.getBlockStorageArray()) if(section!=null&&!section.isEmpty())
                for(int x=0;x<16;x++) for(int z=0;z<16;z++) for(int y=0;y<16;y++)
                    if(section.get(x,y,z).getBlock() instanceof SmartWallBlock)
                        changed(world,new BlockPos(chunk.xPosition*16+x,section.getYLocation()+y,chunk.zPosition*16+z));
            SmartWallData data=SmartWallData.get(world);
            for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++)
                for(long pos:data.chunk(chunk.xPosition+dx,chunk.zPosition+dz).keySet())
                    DIRTY.computeIfAbsent(world,w->new LinkedHashSet<>()).add(BlockPos.fromLong(pos));
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
    @SubscribeEvent public void unloaded(WorldEvent.Unload event){DIRTY.remove(event.getWorld());WATCHERS.remove(event.getWorld());}

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
