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
    public SmartCobData(){super(NAME);}
    public SmartCobData(String name){super(name);}
    public static SmartCobData get(World world) {
        SmartCobData data=(SmartCobData)world.getPerWorldStorage().getOrLoadData(SmartCobData.class,NAME);
        if(data==null) {data=new SmartCobData();world.getPerWorldStorage().setData(NAME,data);}return data;
    }
    public SmartCobProfile profile(BlockPos pos){return chunk(pos.getX()>>4,pos.getZ()>>4).getOrDefault(pos.toLong(),SmartCobProfile.SQUARE);}
    public boolean contains(BlockPos pos){return chunk(pos.getX()>>4,pos.getZ()>>4).containsKey(pos.toLong());}
    public Map<Long,SmartCobProfile> chunk(int x,int z){return chunks.getOrDefault(SmartWallData.chunkKey(x,z),Collections.emptyMap());}
    public void clearChunk(int x,int z){chunks.remove(SmartWallData.chunkKey(x,z));}
    public boolean put(BlockPos pos,SmartCobProfile profile) {
        long key=SmartWallData.chunkKey(pos.getX()>>4,pos.getZ()>>4);
        if(profile==null) {Map<Long,SmartCobProfile> cells=chunks.get(key);if(cells==null||cells.remove(pos.toLong())==null) return false;}
        else {SmartCobProfile old=chunks.computeIfAbsent(key,k->new ConcurrentHashMap<>()).put(pos.toLong(),profile);if(profile.equals(old)) return false;}
        markDirty();return true;
    }
    @Override public void readFromNBT(NBTTagCompound root) {
        chunks.clear();NBTTagList cells=root.getTagList("cells",10);
        for(int i=0;i<cells.tagCount();i++) {
            NBTTagCompound entry=cells.getCompoundTagAt(i);BlockPos pos=BlockPos.fromLong(entry.getLong("p"));
            if(pos.getY()<0||pos.getY()>255) continue;
            try {put(pos,new SmartCobProfile(entry.getIntArray("v")));} catch(IllegalArgumentException invalid) {put(pos,SmartCobProfile.SQUARE);}
        }
        setDirty(false);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound root) {
        NBTTagList cells=new NBTTagList();
        for(Map<Long,SmartCobProfile> chunk:chunks.values()) for(Map.Entry<Long,SmartCobProfile> cell:chunk.entrySet()) {
            NBTTagCompound entry=new NBTTagCompound();entry.setLong("p",cell.getKey());entry.setIntArray("v",cell.getValue().encode());cells.appendTag(entry);
        }
        root.setTag("cells",cells);return root;
    }
}
