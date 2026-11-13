package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/** Saved framing, indexed by chunk. Reads from render workers never touch the world. */
public final class SmartWallData extends WorldSavedData {
    private static final String NAME="ironagearchitecture_smart_walls_v1";
    private final Map<Long,Map<Long,Integer>> chunks=new ConcurrentHashMap<>();

    public SmartWallData() { super(NAME); }
    public SmartWallData(String name) { super(name); }

    public static SmartWallData get(World world) {
        SmartWallData data=(SmartWallData)world.getPerWorldStorage().getOrLoadData(SmartWallData.class,NAME);
        if(data==null) {data=new SmartWallData();world.getPerWorldStorage().setData(NAME,data);}
        return data;
    }

    public static long chunkKey(int x,int z) { return ((long)x<<32)|(z&0xffffffffL); }
    private static long chunkKey(BlockPos pos) { return chunkKey(pos.getX()>>4,pos.getZ()>>4); }

    public int code(BlockPos pos) { return chunk(pos.getX()>>4,pos.getZ()>>4).getOrDefault(pos.toLong(),0); }
    public Map<Long,Integer> chunk(int x,int z) {
        Map<Long,Integer> values=chunks.get(chunkKey(x,z));
        return values==null?Collections.emptyMap():values;
    }
    public void clearChunk(int x,int z) { chunks.remove(chunkKey(x,z)); }

    public boolean put(BlockPos pos,int code) {
        long key=chunkKey(pos), at=pos.toLong();
        if(code==0) {
            Map<Long,Integer> values=chunks.get(key);
            if(values==null||values.remove(at)==null) return false;
        } else {
            Map<Long,Integer> values=chunks.computeIfAbsent(key,k->new ConcurrentHashMap<>());
            Integer old=values.put(at,code);
            if(old!=null&&old==code) return false;
        }
        markDirty();
        return true;
    }

    @Override public void readFromNBT(NBTTagCompound root) {
        chunks.clear();
        NBTTagList list=root.getTagList("cells",10);
        for(int i=0;i<list.tagCount();i++) {
            NBTTagCompound entry=list.getCompoundTagAt(i);
            BlockPos pos=BlockPos.fromLong(entry.getLong("p"));
            if(pos.getY()>=0&&pos.getY()<256) put(pos,entry.getInteger("c"));
        }
        setDirty(false);
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound root) {
        NBTTagList list=new NBTTagList();
        for(Map<Long,Integer> chunk:chunks.values()) for(Map.Entry<Long,Integer> cell:chunk.entrySet()) {
            NBTTagCompound entry=new NBTTagCompound();
            entry.setLong("p",cell.getKey());entry.setInteger("c",cell.getValue());list.appendTag(entry);
        }
        root.setTag("cells",list);
        return root;
    }
}
