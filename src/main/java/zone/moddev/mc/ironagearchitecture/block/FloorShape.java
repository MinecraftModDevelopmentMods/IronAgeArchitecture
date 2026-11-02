package zone.moddev.mc.ironagearchitecture.block;

import java.util.Locale;
import net.minecraft.util.EnumFacing;

/** One bit per 8x8 quarter: north-west, north-east, south-west, south-east. */
public enum FloorShape {
    FULL(15), HALF(3), QUARTER(1), L(7);
    public final int mask;
    FloorShape(int mask) {this.mask=mask;}
    public String key() {return name().toLowerCase(Locale.ROOT);}
    public boolean contains(double x,double z) {
        return x>=0&&x<16&&z>=0&&z<16&&(mask&(1<<((x<8?0:1)+(z<8?0:2))))!=0;
    }
    public int rotatedMask(EnumFacing facing) {
        int result=0;
        for(int q=0;q<4;q++) if((mask&(1<<q))!=0) {
            double[] point=WallGeometry.rotate((q%2)*8+4,0,(q/2)*8+4,facing);
            result|=1<<((point[0]<8?0:1)+(point[2]<8?0:2));
        }
        return result;
    }
}
