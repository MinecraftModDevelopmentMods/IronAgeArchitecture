package zone.moddev.mc.ironagearchitecture.block;

import net.minecraft.util.IStringSerializable;

/** Corner names follow the vanilla stair convention. */
public enum RoofShape implements IStringSerializable {
    STRAIGHT, INNER_LEFT, INNER_RIGHT, OUTER_LEFT, OUTER_RIGHT;
    @Override public String getName() { return name().toLowerCase(java.util.Locale.ROOT); }
}
