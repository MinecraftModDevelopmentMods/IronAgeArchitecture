package zone.moddev.mc.ironagearchitecture.block;

import net.minecraft.util.IStringSerializable;

/** A small, fixed roof kit. These are derived states, not additional block IDs. */
public enum ThatchForm implements IStringSerializable {
    FLAT, SLOPE, EAVE, HIP, EAVE_HIP, VALLEY, RIDGE, RIDGE_END, VALLEY_RUN, VALLEY_END, CAP, BASIN, SOLID;
    @Override public String getName(){return name().toLowerCase(java.util.Locale.ROOT);}
}
