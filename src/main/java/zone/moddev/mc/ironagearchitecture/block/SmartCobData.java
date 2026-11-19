package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;

/** Separate versioned cob profiles; no timber descriptors or block metadata are repurposed. */
public final class SmartCobData extends WorldSavedData {
    private static final String NAME="ironagearchitecture_smart_cob_v1";
    private final Map<Long,Map<Long,SmartCobProfile>> chunks=new ConcurrentHashMap<>();
    private SmartShapePages pages;
    public SmartCobData(){super(NAME);}
    public SmartCobData(String name){super(name);}
    public static SmartCobData get(World world) {
        SmartCobData data=(SmartCobData)world.getPerWorldStorage().getOrLoadData(SmartCobData.class,NAME);
        if(data==null) {data=new SmartCobData();world.getPerWorldStorage().setData(NAME,data);}
        if(!world.isRemote&&data.pages==null)data.attach(world);return data;
    }
    public SmartCobProfile profile(BlockPos pos){return chunk(pos.getX()>>4,pos.getZ()>>4).getOrDefault(pos.toLong(),SmartCobProfile.SQUARE);}
    public boolean contains(BlockPos pos){return chunk(pos.getX()>>4,pos.getZ()>>4).containsKey(pos.toLong());}
    public Map<Long,SmartCobProfile> chunk(int x,int z){return chunks.getOrDefault(SmartWallData.chunkKey(x,z),Collections.emptyMap());}
    public void clearChunk(int x,int z){chunks.remove(SmartWallData.chunkKey(x,z));}
    public boolean put(BlockPos pos,SmartCobProfile profile) {
        long key=SmartWallData.chunkKey(pos.getX()>>4,pos.getZ()>>4);
        if(profile==null) {Map<Long,SmartCobProfile> cells=chunks.get(key);if(cells==null||cells.remove(pos.toLong())==null) return false;}
        else {Map<Long,SmartCobProfile> cells=chunks.computeIfAbsent(key,k->new ConcurrentHashMap<>());if(profile.equals(cells.get(pos.toLong())))return false;cells.put(pos.toLong(),profile);}
        if(pages==null)markDirty();else pages.dirty(SmartShapePages.key(pos.getX()>>4,pos.getZ()>>4));return true;
    }
    @Override public boolean isDirty(){return pages==null&&super.isDirty();}
    private void attach(World world){SmartShapePages opened=new SmartShapePages(world,NAME+"_page",this::page);
        for(NBTTagCompound patch:opened.loaded()){long key=patch.getLong("page");int x=(int)(key>>32),z=(int)key;for(int dx=0;dx<2;dx++)for(int dz=0;dz<2;dz++)chunks.remove(SmartWallData.chunkKey(x*2+dx,z*2+dz));SmartCobData local=new SmartCobData();local.readFromNBT(patch);chunks.putAll(local.chunks);}
        pages=opened;opened.restored();setDirty(false);
    }
    private NBTTagCompound page(long key){SmartCobData local=new SmartCobData();int x=(int)(key>>32),z=(int)key;for(int dx=0;dx<2;dx++)for(int dz=0;dz<2;dz++){long chunk=SmartWallData.chunkKey(x*2+dx,z*2+dz);Map<Long,SmartCobProfile> values=chunks.get(chunk);if(values!=null)local.chunks.put(chunk,values);}return local.writeToNBT(new NBTTagCompound());}
    @Override public void readFromNBT(NBTTagCompound root) {
        chunks.clear();java.util.List<SmartCobProfile> palette=new java.util.ArrayList<>();NBTTagList dictionary=root.getTagList("profiles",10);
        for(int i=0;i<dictionary.tagCount();i++)palette.add(new SmartCobProfile(dictionary.getCompoundTagAt(i).getIntArray("v")));
        java.util.Map<SmartCobProfile,SmartCobProfile> shared=new java.util.HashMap<>();
        NBTTagList cells=root.getTagList("cells",10);
        for(int i=0;i<cells.tagCount();i++) {NBTTagCompound entry=cells.getCompoundTagAt(i);BlockPos pos=BlockPos.fromLong(entry.getLong("p"));if(pos.getY()<0||pos.getY()>=256)continue;
            try{SmartCobProfile profile=entry.hasKey("t")?palette.get(entry.getInteger("t")):new SmartCobProfile(entry.getIntArray("v"));SmartCobProfile existing=shared.get(profile);if(existing==null){shared.put(profile,profile);existing=profile;}put(pos,existing);}catch(IllegalArgumentException|IndexOutOfBoundsException invalid){put(pos,SmartCobProfile.SQUARE);}
        }
        setDirty(false);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound root) {
        NBTTagList cells=new NBTTagList(),profiles=new NBTTagList();java.util.Map<SmartCobProfile,Integer> ids=new java.util.HashMap<>();
        for(Map<Long,SmartCobProfile> chunk:chunks.values())for(Map.Entry<Long,SmartCobProfile> cell:chunk.entrySet()){Integer id=ids.get(cell.getValue());if(id==null){id=ids.size();ids.put(cell.getValue(),id);NBTTagCompound profile=new NBTTagCompound();profile.setIntArray("v",cell.getValue().encode());profiles.appendTag(profile);}NBTTagCompound entry=new NBTTagCompound();entry.setLong("p",cell.getKey());entry.setInteger("t",id);cells.appendTag(entry);}
        root.setInteger("format",2);root.setTag("profiles",profiles);root.setTag("cells",cells);return root;
    }
}
