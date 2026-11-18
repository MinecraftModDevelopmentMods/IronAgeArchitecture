package zone.moddev.mc.ironagearchitecture.roof;

import net.minecraft.nbt.NBTTagCompound;

/** A small editable feature, in the roof's cross-slope (u) and ridge (v) coordinates. */
public final class RoofFeature {
    public static final int WINDOW=0,GABLE=1,SHED=2,VERTICAL_WINDOW=3,SOLID_PANEL=4;
    public int id,type,u,v,width=3,height=1,lift,wood,finish=1,wall=3,cover=1;
    public boolean diamond=true;
    public int y=-1,parent=-1,facing;
    public RoofFeature copy(){return read(write());}
    public NBTTagCompound write(){NBTTagCompound n=new NBTTagCompound();n.setIntArray("f",new int[]{id,type,u,v,width,height,lift,wood,finish,wall,cover});n.setBoolean("diamond",diamond);n.setInteger("y",y);n.setInteger("parent",parent);n.setInteger("face",facing);return n;}
    public static RoofFeature read(NBTTagCompound n){int[] a=n.getIntArray("f");if(a.length!=11)throw new IllegalArgumentException("Incomplete roof feature");RoofFeature f=new RoofFeature();f.id=a[0];f.type=a[1];f.u=a[2];f.v=a[3];f.width=a[4];f.height=a[5];f.lift=a[6];f.wood=a[7];f.finish=a[8];f.wall=a[9];f.cover=a[10];f.diamond=n.getBoolean("diamond");f.y=n.hasKey("y")?n.getInteger("y"):-1;f.parent=n.hasKey("parent")?n.getInteger("parent"):-1;f.facing=n.getInteger("face");if(f.id<0||f.type<0||f.type>4||f.width<1||f.width>30||f.height<1||f.height>2||f.lift<0||f.lift>4||f.wood<0||f.wood>5||f.finish<0||f.finish>1||f.wall<0||f.wall>4||f.cover<1||f.cover>3||f.y< -1||f.y>255||f.facing<0||f.facing>3)throw new IllegalArgumentException("Invalid roof feature");return f;}
}
