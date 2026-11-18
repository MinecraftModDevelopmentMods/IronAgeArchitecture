package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;

/** A bounded, saved construction drawing. All dimensions are on a half-block grid. */
public final class RoofPlan {
    public static final int MAX_CELLS=1024;
    public static final double[] PITCHES={.5,1,2};
    public UUID id=UUID.randomUUID();
    public int minX,maxX,minZ,maxZ,floorY,pitch=1,base, eave=1,gable=1,wood;
    public boolean alongX;
    public final Set<Long> openings=new TreeSet<>();
    public final Map<Long,RoofCell> cells=new LinkedHashMap<>();
    public String error="";
    public static long column(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
    public int width(){return maxX-minX+1;}
    public int length(){return maxZ-minZ+1;}
    public boolean contains(BlockPos p){return p.getX()>=minX&&p.getX()<=maxX&&p.getZ()>=minZ&&p.getZ()<=maxZ&&p.getY()==floorY;}
    public boolean valid(){return error.isEmpty()&&!cells.isEmpty();}
    public int timber(){int n=0;for(RoofCell c:cells.values())if(c.timber())n++;return n;}
    public RoofPlan settingsCopy(){RoofPlan p=new RoofPlan();p.id=id;p.minX=minX;p.maxX=maxX;p.minZ=minZ;p.maxZ=maxZ;p.floorY=floorY;p.pitch=pitch;p.base=base;p.eave=eave;p.gable=gable;p.wood=wood;p.alongX=alongX;p.openings.addAll(openings);p.error=error;return p;}
    public RoofPlan copy(){RoofPlan p=settingsCopy();p.cells.putAll(cells);return p;}
    public NBTTagCompound write(NBTTagCompound n){n.setUniqueId("id",id);n.setIntArray("settings",new int[]{minX,maxX,minZ,maxZ,floorY,pitch,base,eave,gable,wood,alongX?1:0});NBTTagList holes=new NBTTagList();for(long h:openings)holes.appendTag(new NBTTagLong(h));n.setTag("holes",holes);NBTTagList parts=new NBTTagList();for(Map.Entry<Long,RoofCell> e:cells.entrySet()){NBTTagCompound p=e.getValue().write();p.setLong("pos",e.getKey());parts.appendTag(p);}n.setTag("cells",parts);n.setString("error",error);return n;}
    public static RoofPlan read(NBTTagCompound n){RoofPlan p=new RoofPlan();if(n.hasUniqueId("id"))p.id=n.getUniqueId("id");int[] s=n.getIntArray("settings");if(s.length!=11)throw new IllegalArgumentException("Incomplete roof drawing");p.minX=s[0];p.maxX=s[1];p.minZ=s[2];p.maxZ=s[3];p.floorY=s[4];p.pitch=s[5];p.base=s[6];p.eave=s[7];p.gable=s[8];p.wood=s[9];p.alongX=s[10]!=0;NBTTagList h=n.getTagList("holes",4);if(h.tagCount()>1024)throw new IllegalArgumentException("Too many roof openings");for(int i=0;i<h.tagCount();i++)p.openings.add(((NBTTagLong)h.get(i)).getLong());NBTTagList c=n.getTagList("cells",10);if(c.tagCount()>MAX_CELLS)throw new IllegalArgumentException("Roof drawing too large");for(int i=0;i<c.tagCount();i++){NBTTagCompound a=c.getCompoundTagAt(i);p.cells.put(a.getLong("pos"),RoofCell.read(a));}p.error=n.getString("error");return p;}
}
