package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.*;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Cell descriptors live with their chunks. Only small construction drawings use saved data. */
public final class RoofData extends WorldSavedData {
    private static final String NAME="ironagearchitecture_roof_plans_v1",CELLS="ironagearchitecture_roof_cells_v1";
    private static final Map<World,RoofData> CLIENT=new WeakHashMap<>();
    private final Map<Chunk,Map<Long,RoofCell>> serverCells=Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Long,Map<Long,RoofCell>> clientCells=new ConcurrentHashMap<>();
    public final Map<UUID,RoofPlan> plans=new LinkedHashMap<>();
    private final Map<UUID,Set<String>> removedWindows=new HashMap<>();
    public boolean removed(UUID plan,String id){return removedWindows.getOrDefault(plan,Collections.emptySet()).contains(id);}
    public void removeWindow(UUID plan,String id){removedWindows.computeIfAbsent(plan,k->new HashSet<>()).add(id);markDirty();}
    public void restoreWindow(UUID plan,String id){Set<String> ids=removedWindows.get(plan);if(ids!=null&&ids.remove(id)){if(ids.isEmpty())removedWindows.remove(plan);markDirty();}}
    public RoofData(){super(NAME);}public RoofData(String name){super(name);}
    public static RoofData get(World w){if(w.isRemote)return CLIENT.computeIfAbsent(w,k->new RoofData());RoofData d=(RoofData)w.getPerWorldStorage().getOrLoadData(RoofData.class,NAME);if(d==null){d=new RoofData();w.getPerWorldStorage().setData(NAME,d);}return d;}
    private static long key(int x,int z){return RoofPlan.column(x,z);}
    public Map<Long,RoofCell> chunk(World w,int x,int z){if(w.isRemote)return clientCells.getOrDefault(key(x,z),Collections.emptyMap());if(!w.isBlockLoaded(new BlockPos(x*16,0,z*16)))return Collections.emptyMap();return serverCells.getOrDefault(w.getChunkFromChunkCoords(x,z),Collections.emptyMap());}
    public RoofCell at(World w,BlockPos p){return chunk(w,p.getX()>>4,p.getZ()>>4).get(p.toLong());}
    public Map<Long,RoofCell> owned(World w,RoofPlan plan){Map<Long,RoofCell> result=new TreeMap<>();for(int x=(plan.minX-6)>>4;x<=(plan.maxX+6)>>4;x++)for(int z=(plan.minZ-6)>>4;z<=(plan.maxZ+6)>>4;z++)for(Map.Entry<Long,RoofCell> e:chunk(w,x,z).entrySet())if(e.getValue().plan.equals(plan.id))result.put(e.getKey(),e.getValue());return result;}
    /** Physical parts keep the built footprint while a different drawing is being edited. */
    public RoofPlan interaction(UUID id,BlockPos at){RoofPlan built=plans.get(id);if(built!=null&&at.getX()>=built.minX-6&&at.getX()<=built.maxX+6&&at.getZ()>=built.minZ-6&&at.getZ()<=built.maxZ+6)return built;return drawing(id);}
    public RoofPlan drawing(UUID id){RoofPlan p=drafts.get(id);return p==null?plans.get(id):p;}
    public final Map<UUID,RoofPlan> drafts=new LinkedHashMap<>();
    public void draft(RoofPlan p){drafts.put(p.id,p.settingsCopy());markDirty();}
    public void put(World w,BlockPos p,RoofCell cell){if(!w.isBlockLoaded(p))throw new IllegalStateException("Roof chunk is unloaded");Map<Long,RoofCell> map;if(w.isRemote)map=clientCells.computeIfAbsent(key(p.getX()>>4,p.getZ()>>4),k->new ConcurrentHashMap<>());else{Chunk c=w.getChunkFromBlockCoords(p);map=serverCells.computeIfAbsent(c,k->new ConcurrentHashMap<>());c.setChunkModified();}if(cell==null)map.remove(p.toLong());else{RoofMeshes.prepare(cell);map.put(p.toLong(),cell);}if(!w.isRemote)RoofNetwork.changed(w,p,cell);}
    public void snapshot(int x,int z,boolean replace,Map<Long,RoofCell> cells){long k=key(x,z);Map<Long,RoofCell> map=clientCells.computeIfAbsent(k,q->new ConcurrentHashMap<>());if(replace)map.clear();for(Map.Entry<Long,RoofCell> e:cells.entrySet()){if(e.getValue()==null)map.remove(e.getKey());else{RoofMeshes.prepare(e.getValue());map.put(e.getKey(),e.getValue());}}}
    public void forget(int x,int z){clientCells.remove(key(x,z));}
    public void savePlan(RoofPlan p){plans.put(p.id,p.settingsCopy());markDirty();}
    public RoofPlan find(BlockPos floor){for(RoofPlan p:plans.values())if(p.contains(floor))return RoofPlanner.solve(p.copy());return null;}
    @Override public void readFromNBT(NBTTagCompound n){plans.clear();drafts.clear();removedWindows.clear();NBTTagList removed=n.getTagList("removedWindows",10);for(int i=0;i<removed.tagCount();i++){NBTTagCompound a=removed.getCompoundTagAt(i);removedWindows.computeIfAbsent(a.getUniqueId("plan"),k->new HashSet<>()).add(a.getString("id"));}for(boolean draft:new boolean[]{false,true}){NBTTagList list=n.getTagList(draft?"drafts":"plans",10);for(int i=0;i<list.tagCount();i++){RoofPlan p=RoofPlan.read(list.getCompoundTagAt(i));p.cells.clear();(draft?drafts:plans).put(p.id,p);}}setDirty(false);}
    @Override public NBTTagCompound writeToNBT(NBTTagCompound n){for(boolean draft:new boolean[]{false,true}){NBTTagList list=new NBTTagList();for(RoofPlan p:(draft?drafts:plans).values()){NBTTagCompound a=p.write(new NBTTagCompound());a.removeTag("cells");list.appendTag(a);}n.setTag(draft?"drafts":"plans",list);}NBTTagList removed=new NBTTagList();for(Map.Entry<UUID,Set<String>> e:removedWindows.entrySet())for(String id:e.getValue()){NBTTagCompound a=new NBTTagCompound();a.setUniqueId("plan",e.getKey());a.setString("id",id);removed.appendTag(a);}n.setTag("removedWindows",removed);n.setInteger("version",2);return n;}
    public static final class Events {
        @SubscribeEvent public void load(ChunkDataEvent.Load e){Map<Long,RoofCell> cells=new ConcurrentHashMap<>();NBTTagList list=e.getData().getTagList(CELLS,10);if(list.tagCount()>65536)throw new IllegalStateException("Invalid roof chunk");for(int i=0;i<list.tagCount();i++){NBTTagCompound n=list.getCompoundTagAt(i);BlockPos p=BlockPos.fromLong(n.getLong("pos"));if((p.getX()>>4)!=e.getChunk().xPosition||(p.getZ()>>4)!=e.getChunk().zPosition)throw new IllegalStateException("Roof cell in wrong chunk");RoofCell cell=RoofCell.read(n);cells.put(p.toLong(),cell);}if(!cells.isEmpty())get(e.getWorld()).serverCells.put(e.getChunk(),cells);}
        @SubscribeEvent public void loaded(ChunkEvent.Load e){RoofWindowController.loaded(e.getWorld(),e.getChunk().xPosition,e.getChunk().zPosition);}
        @SubscribeEvent public void save(ChunkDataEvent.Save e){Map<Long,RoofCell> cells=get(e.getWorld()).serverCells.get(e.getChunk());if(cells==null){e.getData().removeTag(CELLS);return;}NBTTagList list=new NBTTagList();for(Map.Entry<Long,RoofCell> entry:cells.entrySet()){NBTTagCompound n=entry.getValue().write();n.setLong("pos",entry.getKey());list.appendTag(n);}e.getData().setTag(CELLS,list);}
        @SubscribeEvent public void watch(ChunkWatchEvent.Watch e){World w=e.getPlayer().world;RoofNetwork.snapshot(e.getPlayer(),e.getChunk().chunkXPos,e.getChunk().chunkZPos,get(w).chunk(w,e.getChunk().chunkXPos,e.getChunk().chunkZPos));}
        @SubscribeEvent public void unwatch(ChunkWatchEvent.UnWatch e){RoofNetwork.unwatch(e.getPlayer(),e.getChunk().chunkXPos,e.getChunk().chunkZPos);}
        @SubscribeEvent public void unload(ChunkEvent.Unload e){if(e.getWorld().isRemote)get(e.getWorld()).forget(e.getChunk().xPosition,e.getChunk().zPosition);}
        @SubscribeEvent public void worldUnload(WorldEvent.Unload e){CLIENT.remove(e.getWorld());}
    }
}
