package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;

/** Immutable references to reusable local prisms, not a saved mesh. Coordinates are thirty-seconds of a block. */
public final class RoofCell {
    public final UUID plan;
    public final BlockPos owner;
    public final int[] timber,skin;
    public RoofCell(UUID plan,BlockPos owner,int[] timber,int[] skin){this.plan=plan;this.owner=owner;this.timber=timber.clone();this.skin=skin.clone();}
    public boolean timber(){return timber.length>0;}
    public boolean coverable(){return skin.length>0;}
    public NBTTagCompound write(){NBTTagCompound n=new NBTTagCompound();n.setUniqueId("plan",plan);n.setLong("owner",owner.toLong());n.setIntArray("timber",timber);n.setIntArray("skin",skin);return n;}
    public static RoofCell read(NBTTagCompound n){int[] a=n.getIntArray("timber"),b=n.getIntArray("skin");if(a.length>512||b.length>256||a.length%8!=0||b.length%8!=0)throw new IllegalArgumentException("Invalid roof cell");return new RoofCell(n.getUniqueId("plan"),BlockPos.fromLong(n.getLong("owner")),a,b);}
    @Override public boolean equals(Object o){if(!(o instanceof RoofCell))return false;RoofCell c=(RoofCell)o;return plan.equals(c.plan)&&owner.equals(c.owner)&&Arrays.equals(timber,c.timber)&&Arrays.equals(skin,c.skin);}
    @Override public int hashCode(){return Objects.hash(plan,owner,Arrays.hashCode(timber),Arrays.hashCode(skin));}
}
