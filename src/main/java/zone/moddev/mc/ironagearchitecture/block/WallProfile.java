package zone.moddev.mc.ironagearchitecture.block;

/** Saved, block-owned half-grid footprints. Coordinates are local pixels. */
public enum WallProfile {
    THIN(0,16,0,8,false), SHORT_LEFT(0,8,0,8,false), SHORT_RIGHT(8,16,0,8,false),
    CORNER_L(0,16,0,16,true), CORNER_HALF_LEFT(0,16,8,16,true),
    CORNER_HALF_RIGHT(8,16,0,16,true), CORNER_QUARTER(8,16,8,16,true);
    public final int x0,x1,z0,z1;
    public final boolean corner;
    WallProfile(int x0,int x1,int z0,int z1,boolean corner) {
        this.x0=x0;this.x1=x1;this.z0=z0;this.z1=z1;this.corner=corner;
    }
    public String key() {return name().toLowerCase(java.util.Locale.ROOT);}
    public boolean contains(double x,double z) {
        return x>=x0&&x<=x1&&z>=z0&&z<=z1&&(this!=CORNER_L||x<=8||z<=8);
    }
    public int quarters() {return this==CORNER_L?3:(x1-x0)*(z1-z0)/64;}
}
