package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;

/** Immutable references to reusable local prisms, not a saved mesh. Coordinates are thirty-seconds of a block. */
public final class RoofCell {
    public final UUID plan;
    public final BlockPos owner;
    public final int[] timber,skin;
    public final boolean designed;
    public final List<RoofPart> parts;
    public final List<RoofWindow> windows;
    public RoofCell(UUID plan,BlockPos owner,int[] timber,int[] skin){this.plan=plan;this.owner=owner;this.timber=timber.clone();this.skin=skin.clone();designed=false;parts=Collections.emptyList();windows=Collections.emptyList();}
    public RoofCell(UUID plan,BlockPos owner,List<RoofPart> parts,List<RoofWindow> windows){this.plan=plan;this.owner=owner;List<RoofPart> ps=new ArrayList<>(parts);ps.sort(Comparator.comparing(a->a.id));List<RoofWindow> ws=new ArrayList<>(windows);ws.sort(Comparator.comparing(a->a.id));this.parts=Collections.unmodifiableList(ps);this.windows=Collections.unmodifiableList(ws);designed=true;List<Integer> a=new ArrayList<>(),b=new ArrayList<>();for(RoofPart p:ps){List<Integer> list=p.kind==RoofPart.COVER?b:p.kind==RoofPart.TIMBER||p.kind==RoofPart.FASCIA||p.kind==RoofPart.SOFFIT?a:null;if(list!=null)for(int i:p.data)list.add(i);}timber=ints(a);skin=ints(b);}
    public int estimatedBytes(){int n=160;if(!designed)return n+4*(timber.length+skin.length);for(RoofPart p:parts)n+=128+p.id.length()*3+p.data.length*4;for(RoofWindow w:windows)n+=224+w.id.length()*3;return n;}
    private static int[] ints(List<Integer> a){int[] b=new int[a.size()];for(int i=0;i<b.length;i++)b[i]=a.get(i);return b;}
    public boolean empty(){return designed&&parts.isEmpty()&&windows.isEmpty();}
    public boolean timber(){return timber.length>0;}
    public boolean coverable(){return skin.length>0;}
    public NBTTagCompound write(){NBTTagCompound n=new NBTTagCompound();n.setUniqueId("plan",plan);n.setLong("owner",owner.toLong());if(designed){n.setInteger("version",2);net.minecraft.nbt.NBTTagList ps=new net.minecraft.nbt.NBTTagList(),ws=new net.minecraft.nbt.NBTTagList();for(RoofPart p:parts)ps.appendTag(p.write());for(RoofWindow w:windows)ws.appendTag(w.write());n.setTag("parts",ps);n.setTag("windows",ws);}else{n.setIntArray("timber",timber);n.setIntArray("skin",skin);}return n;}
    public static RoofCell read(NBTTagCompound n){if(n.getInteger("version")==2){net.minecraft.nbt.NBTTagList ps=n.getTagList("parts",10),ws=n.getTagList("windows",10);if(ps.tagCount()>48||ws.tagCount()>8)throw new IllegalArgumentException("Too many parts in roof cell");List<RoofPart> parts=new ArrayList<>();List<RoofWindow> windows=new ArrayList<>();for(int i=0;i<ps.tagCount();i++)parts.add(RoofPart.read(ps.getCompoundTagAt(i)));for(int i=0;i<ws.tagCount();i++)windows.add(RoofWindow.read(ws.getCompoundTagAt(i)));return new RoofCell(n.getUniqueId("plan"),BlockPos.fromLong(n.getLong("owner")),parts,windows);}int[] a=n.getIntArray("timber"),b=n.getIntArray("skin");if(a.length>512||b.length>256||a.length%8!=0||b.length%8!=0)throw new IllegalArgumentException("Invalid roof cell");return new RoofCell(n.getUniqueId("plan"),BlockPos.fromLong(n.getLong("owner")),a,b);}
    @Override public boolean equals(Object o){if(!(o instanceof RoofCell))return false;RoofCell c=(RoofCell)o;return plan.equals(c.plan)&&owner.equals(c.owner)&&designed==c.designed&&parts.equals(c.parts)&&windows.equals(c.windows)&&Arrays.equals(timber,c.timber)&&Arrays.equals(skin,c.skin);}
    @Override public int hashCode(){return Objects.hash(plan,owner,designed,parts,windows,Arrays.hashCode(timber),Arrays.hashCode(skin));}
}
