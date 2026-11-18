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
    /** Absent in older saves: those retain the original solver until deliberately revised. */
    public int version=2,revision,seat,wallInset,frame,clearance=5,drop=1,edge=1,mansard;
    public final int[] overhang={2,2,1,1};
    public final List<RoofFeature> features=new ArrayList<>();
    public final Set<Long> openings=new TreeSet<>();
    public final Map<Long,RoofCell> cells=new LinkedHashMap<>();
    public String error="";
    public static long column(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
    public int width(){return maxX-minX+1;}
    public int length(){return maxZ-minZ+1;}
    public boolean contains(BlockPos p){return p.getX()>=minX&&p.getX()<=maxX&&p.getZ()>=minZ&&p.getZ()<=maxZ&&p.getY()==floorY;}
    public boolean valid(){return error.isEmpty()&&!cells.isEmpty();}
    public int timber(){int n=0;for(RoofCell c:cells.values())if(c.timber())n++;return n;}
    public RoofPlan settingsCopy(){RoofPlan p=new RoofPlan();p.id=id;p.minX=minX;p.maxX=maxX;p.minZ=minZ;p.maxZ=maxZ;p.floorY=floorY;p.pitch=pitch;p.base=base;p.eave=eave;p.gable=gable;p.wood=wood;p.alongX=alongX;p.version=version;p.revision=revision;p.seat=seat;p.wallInset=wallInset;p.frame=frame;p.clearance=clearance;p.drop=drop;p.edge=edge;p.mansard=mansard;System.arraycopy(overhang,0,p.overhang,0,4);for(RoofFeature f:features)p.features.add(f.copy());p.openings.addAll(openings);p.error=error;return p;}
    public RoofPlan copy(){RoofPlan p=settingsCopy();p.cells.putAll(cells);return p;}
    public NBTTagCompound write(NBTTagCompound n){n.setUniqueId("id",id);n.setIntArray("settings",new int[]{minX,maxX,minZ,maxZ,floorY,pitch,base,eave,gable,wood,alongX?1:0});n.setInteger("version",version);n.setInteger("revision",revision);n.setIntArray("design",new int[]{seat,wallInset,frame,clearance,drop,edge,mansard});n.setIntArray("overhang",overhang);NBTTagList fs=new NBTTagList();for(RoofFeature f:features)fs.appendTag(f.write());n.setTag("features",fs);NBTTagList holes=new NBTTagList();for(long h:openings)holes.appendTag(new NBTTagLong(h));n.setTag("holes",holes);NBTTagList parts=new NBTTagList();for(Map.Entry<Long,RoofCell> e:cells.entrySet()){NBTTagCompound p=e.getValue().write();p.setLong("pos",e.getKey());parts.appendTag(p);}n.setTag("cells",parts);n.setString("error",error);return n;}
    public static RoofPlan read(NBTTagCompound n){RoofPlan p=new RoofPlan();if(n.hasUniqueId("id"))p.id=n.getUniqueId("id");int[] s=n.getIntArray("settings");if(s.length!=11)throw new IllegalArgumentException("Incomplete roof drawing");p.minX=s[0];p.maxX=s[1];p.minZ=s[2];p.maxZ=s[3];p.floorY=s[4];p.pitch=s[5];p.base=s[6];p.eave=s[7];p.gable=s[8];p.wood=s[9];p.alongX=s[10]!=0;p.version=n.hasKey("version")?n.getInteger("version"):1;p.revision=n.getInteger("revision");if(p.version<1||p.version>2)throw new IllegalArgumentException("Unknown roof drawing version");if(p.version==2){int[] d=n.getIntArray("design"),o=n.getIntArray("overhang");if(d.length!=7||o.length!=4)throw new IllegalArgumentException("Incomplete roof controls");p.seat=d[0];p.wallInset=d[1];p.frame=d[2];p.clearance=d[3];p.drop=d[4];p.edge=d[5];p.mansard=d[6];System.arraycopy(o,0,p.overhang,0,4);NBTTagList fs=n.getTagList("features",10);if(fs.tagCount()>64)throw new IllegalArgumentException("Too many roof features");Set<Integer> ids=new HashSet<>();for(int i=0;i<fs.tagCount();i++){RoofFeature f=RoofFeature.read(fs.getCompoundTagAt(i));if(!ids.add(f.id))throw new IllegalArgumentException("Duplicate roof feature");p.features.add(f);}}NBTTagList h=n.getTagList("holes",4);if(h.tagCount()>1024)throw new IllegalArgumentException("Too many roof openings");for(int i=0;i<h.tagCount();i++)p.openings.add(((NBTTagLong)h.get(i)).getLong());NBTTagList c=n.getTagList("cells",10);if(c.tagCount()>MAX_CELLS)throw new IllegalArgumentException("Roof drawing too large");for(int i=0;i<c.tagCount();i++){NBTTagCompound a=c.getCompoundTagAt(i);p.cells.put(a.getLong("pos"),RoofCell.read(a));}p.error=n.getString("error");return p;}
}
