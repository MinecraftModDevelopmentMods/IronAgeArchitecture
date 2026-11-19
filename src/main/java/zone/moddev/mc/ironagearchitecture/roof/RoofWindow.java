package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/** Immutable window slice. Relative offsets permit reuse at unrelated houses. */
public final class RoofWindow {
    public final String id;public final BlockPos owner;public final int dx,dy,dz,y32,slope32,style,joins,rise32;public final EnumFacing facing;public final boolean vertical,right,open,powered;
    public RoofWindow(String id,BlockPos owner,int dx,int dy,int dz,int y32,int slope32,int style,int joins,EnumFacing facing,boolean vertical,boolean right,boolean open,boolean powered){this(id,owner,dx,dy,dz,y32,slope32,style,joins,facing,vertical,right,open,powered,vertical?0:16);}
    private RoofWindow(String id,BlockPos owner,int dx,int dy,int dz,int y32,int slope32,int style,int joins,EnumFacing facing,boolean vertical,boolean right,boolean open,boolean powered,int rise){this.rise32=rise;this.id=id;this.owner=owner;this.dx=dx;this.dy=dy;this.dz=dz;this.y32=y32;this.slope32=slope32;this.style=style;this.joins=joins;this.facing=facing;this.vertical=vertical;this.right=right;this.open=open;this.powered=powered;}
    public RoofWindow slice(BlockPos cell){return new RoofWindow(id,owner,owner.getX()-cell.getX(),owner.getY()-cell.getY(),owner.getZ()-cell.getZ(),y32,slope32,style,joins,facing,vertical,right,open,powered,rise32);}
    public RoofWindow state(boolean o,boolean p){return new RoofWindow(id,owner,dx,dy,dz,y32,slope32,style,joins,facing,vertical,right,o,p,rise32);}
    public RoofWindow rise(int value){return new RoofWindow(id,owner,dx,dy,dz,y32,slope32,style,joins,facing,vertical,right,open,powered,value);}
    public NBTTagCompound write(){NBTTagCompound n=new NBTTagCompound();n.setString("id",id);n.setLong("owner",owner.toLong());n.setInteger("rise",rise32);n.setIntArray("w",new int[]{dx,dy,dz,y32,slope32,style,joins,facing.getHorizontalIndex(),vertical?1:0,right?1:0,open?1:0,powered?1:0});return n;}
    public static RoofWindow read(NBTTagCompound n){int[] a=n.getIntArray("w");if(a.length!=12||Math.abs(a[0])>4||Math.abs(a[1])>6||Math.abs(a[2])>4||a[3]<-64||a[3]>64||a[4]<0||a[4]>64||a[5]<0||a[5]>23||a[6]<0||a[6]>15)throw new IllegalArgumentException("Invalid roof window");return new RoofWindow(n.getString("id"),BlockPos.fromLong(n.getLong("owner")),a[0],a[1],a[2],a[3],a[4],a[5],a[6],EnumFacing.getHorizontal(a[7]),a[8]!=0,a[9]!=0,a[10]!=0,a[11]!=0,Math.max(0,Math.min(16,n.getInteger("rise"))));}
    public int geometryHash(){return Objects.hash(dx,dy,dz,y32,slope32,style,joins,facing,vertical,right,open,rise32);}
    public boolean geometryEquals(RoofWindow w){return dx==w.dx&&dy==w.dy&&dz==w.dz&&y32==w.y32&&slope32==w.slope32&&style==w.style&&joins==w.joins&&facing==w.facing&&vertical==w.vertical&&right==w.right&&open==w.open&&rise32==w.rise32;}
    @Override public boolean equals(Object o){if(!(o instanceof RoofWindow))return false;RoofWindow w=(RoofWindow)o;return id.equals(w.id)&&owner.equals(w.owner)&&powered==w.powered&&geometryEquals(w);}
    @Override public int hashCode(){return Objects.hash(id,owner,powered,geometryHash());}
}
