package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;

/** Persist only unfinished work. Loading a completed city does not rebuild it. */
public final class SmartShapeDirtyData extends WorldSavedData {
    private static final String NAME="ironagearchitecture_shape_work_v1";
    public static final int TIMBER=0,COB=1,THATCH=2;
    private final Map<Integer,Map<Long,Map<Long,Integer>>> chunks=new HashMap<>();
    public SmartShapeDirtyData(){super(NAME);}public SmartShapeDirtyData(String name){super(name);}
    public static SmartShapeDirtyData get(World world){SmartShapeDirtyData data=(SmartShapeDirtyData)world.getPerWorldStorage().getOrLoadData(SmartShapeDirtyData.class,NAME);if(data==null){data=new SmartShapeDirtyData();world.getPerWorldStorage().setData(NAME,data);}return data;}
    public void add(int family,BlockPos pos,int mask){Map<Long,Integer> cells=chunks.computeIfAbsent(family,k->new HashMap<>()).computeIfAbsent(SmartWallData.chunkKey(pos.getX()>>4,pos.getZ()>>4),k->new HashMap<>());int before=cells.getOrDefault(pos.toLong(),0),next=before|mask;if(before!=next){cells.put(pos.toLong(),next);markDirty();}}
    public void clear(int family,BlockPos pos,int mask){Map<Long,Integer> cells=chunk(family,pos.getX()>>4,pos.getZ()>>4);int before=cells.getOrDefault(pos.toLong(),0),next=before&~mask;if(before==next)return;if(next==0)cells.remove(pos.toLong());else cells.put(pos.toLong(),next);if(cells.isEmpty()){Map<Long,Map<Long,Integer>> familyChunks=chunks.get(family);familyChunks.remove(SmartWallData.chunkKey(pos.getX()>>4,pos.getZ()>>4));if(familyChunks.isEmpty())chunks.remove(family);}markDirty();}
    public Map<Long,Integer> chunk(int family,int x,int z){return chunks.getOrDefault(family,Collections.emptyMap()).getOrDefault(SmartWallData.chunkKey(x,z),Collections.emptyMap());}
    @Override public void readFromNBT(NBTTagCompound root){chunks.clear();NBTTagList list=root.getTagList("pending",10);for(int i=0;i<list.tagCount();i++){NBTTagCompound tag=list.getCompoundTagAt(i);int family=tag.getInteger("family"),mask=tag.getInteger("mask");BlockPos p=BlockPos.fromLong(tag.getLong("p"));if(family>=0&&family<=COB&&mask>0&&mask<=15&&p.getY()>=0&&p.getY()<256)add(family,p,mask);}setDirty(false);}
    @Override public NBTTagCompound writeToNBT(NBTTagCompound root){NBTTagList list=new NBTTagList();for(Map.Entry<Integer,Map<Long,Map<Long,Integer>>> family:chunks.entrySet())for(Map<Long,Integer> chunk:family.getValue().values())for(Map.Entry<Long,Integer> cell:chunk.entrySet()){NBTTagCompound tag=new NBTTagCompound();tag.setInteger("family",family.getKey());tag.setLong("p",cell.getKey());tag.setInteger("mask",cell.getValue());list.appendTag(tag);}root.setTag("pending",list);return root;}
}
