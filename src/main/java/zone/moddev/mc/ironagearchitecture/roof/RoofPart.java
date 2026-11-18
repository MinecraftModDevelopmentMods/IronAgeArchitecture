package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/** A paid logical part can have clipped fragments in several cells. Identity is never a mesh key. */
public final class RoofPart {
    public static final int TIMBER=0,COVER=1,WALL=2,FASCIA=3,SOFFIT=4,WINDOW=5,GUTTER=6;
    public final String id;public final BlockPos root;public final int kind,material;public final int[] data;public final EnumFacing facing;
    public RoofPart(String id,BlockPos root,int kind,int material,int[] data){this(id,root,kind,material,data,EnumFacing.NORTH);}
    public RoofPart(String id,BlockPos root,int kind,int material,int[] data,EnumFacing facing){this.id=id;this.root=root;this.kind=kind;this.material=material;this.data=data.clone();this.facing=facing;}
    public RoofPart withMaterial(int m){return new RoofPart(id,root,kind,m,data,facing);}
    public NBTTagCompound write(){NBTTagCompound n=new NBTTagCompound();n.setString("id",id);n.setLong("root",root.toLong());n.setInteger("kind",kind);n.setInteger("material",material);n.setIntArray("data",data);n.setByte("facing",(byte)facing.getIndex());return n;}
    public static RoofPart read(NBTTagCompound n){String id=n.getString("id");int kind=n.getInteger("kind"),m=n.getInteger("material");int[] a=n.getIntArray("data");if(id.length()>100||kind<0||kind>6||m<0||m>255||a.length>2048||a.length%8!=0)throw new IllegalArgumentException("Invalid roof part");return new RoofPart(id,BlockPos.fromLong(n.getLong("root")),kind,m,a,EnumFacing.getFront(n.getByte("facing")));}
    public int geometryHash(){return Objects.hash(kind,material,facing,Arrays.hashCode(data));}
    public boolean geometryEquals(RoofPart p){return kind==p.kind&&material==p.material&&facing==p.facing&&Arrays.equals(data,p.data);}
    @Override public boolean equals(Object o){if(!(o instanceof RoofPart))return false;RoofPart p=(RoofPart)o;return id.equals(p.id)&&root.equals(p.root)&&geometryEquals(p);}
    @Override public int hashCode(){return Objects.hash(id,root,geometryHash());}
}
