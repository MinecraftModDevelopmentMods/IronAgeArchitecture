package zone.moddev.mc.ironagearchitecture.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import zone.moddev.mc.ironagearchitecture.block.FireplaceBlock.Kind;

/** Model and collision dimensions in sixteenths of a block. */
public final class FireplaceGeometry {
    public static final EnumFacing[] JOIN_SIDES = {EnumFacing.WEST, EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.SOUTH};
    public enum Surface { MASONRY, GRATE, POT, STONE, COALS }

    private FireplaceGeometry() { }

    public static final class Part {
        public final double x0, y0, z0, x1, y1, z1;
        public final Surface surface;

        Part(double x0, double y0, double z0, double x1, double y1, double z1, Surface surface) {
            this.x0 = x0; this.y0 = y0; this.z0 = z0; this.x1 = x1; this.y1 = y1; this.z1 = z1;
            this.surface = surface;
        }
    }

    public static List<Part> parts(Kind kind, int joins, boolean lit) {
        List<Part> result = new ArrayList<>();
        double x0 = (joins & 1) != 0 ? 0 : 2, x1 = (joins & 2) != 0 ? 16 : 14;
        double z0 = (joins & 4) != 0 ? 0 : 2, z1 = (joins & 8) != 0 ? 16 : 14;
        switch (kind) {
            case HEARTH:
            case FIREBOX:
                add(result, (joins & 1) != 0 ? 0 : 1, 0, 1, (joins & 2) != 0 ? 16 : 15, 3, 15, Surface.MASONRY);
                add(result, (joins & 1) != 0 ? 0 : .5, 3, .5, (joins & 2) != 0 ? 16 : 15.5, 4.5, 15.5, Surface.MASONRY);
                if (kind == Kind.FIREBOX) {
                    add(result, 4, 4.5, 4, 12, 4.85, 12, Surface.GRATE);
                    for (int i = 5; i <= 11; i += 2)
                        add(result, i, 4.85, 4, i + .6, 5.2, 12, Surface.GRATE);
                    add(result, 5.5, 5.2, 5.5, 10.5, 6.15, 10.5, lit ? Surface.COALS : Surface.GRATE);
                }
                break;
            case JAMB:
                add(result, 2, 0, 2, 14, 2, 11, Surface.MASONRY);
                add(result, 3, 2, 3, 13, 14, 10, Surface.MASONRY);
                add(result, 2, 14, 2, 14, 16, 11, Surface.MASONRY);
                break;
            case LINTEL:
                add(result, 0, 0, 3, 16, 4, 16, Surface.MASONRY);
                add(result, 0, 4, 2, 16, 6, 16, Surface.MASONRY);
                add(result, 0, 6, 3, 16, 16, 16, Surface.MASONRY);
                break;
            case FLUE:
            case COLLAR:
                ring(result, x0, z0, x1, z1, 0, 16, Surface.MASONRY);
                break;
            case POT_CAP:
                ring(result, 1, 1, 15, 15, 0, 2, Surface.MASONRY);
                ring(result, 4.5, 4.5, 11.5, 11.5, 2, 14, Surface.POT);
                ring(result, 3.75, 3.75, 12.25, 12.25, 14, 16, Surface.POT);
                break;
            case SLAB_CAP:
                ring(result, 1, 1, 15, 15, 0, 3, Surface.MASONRY);
                for (int x : new int[]{3, 11}) for (int z : new int[]{3, 11})
                    add(result, x, 3, z, x+2, 12, z+2, Surface.MASONRY);
                add(result, .5, 12, .5, 15.5, 15, 15.5, Surface.STONE);
                break;
            default: throw new AssertionError(kind);
        }
        return result;
    }

    private static void ring(List<Part> parts, double x0, double z0, double x1, double z1,
            double bottom, double top, Surface surface) {
        add(parts, x0, bottom, z0, 6, top, z1, surface);
        add(parts, 10, bottom, z0, x1, top, z1, surface);
        add(parts, 6, bottom, z0, 10, top, 6, surface);
        add(parts, 6, bottom, 10, 10, top, z1, surface);
    }

    private static void add(List<Part> parts, double x0, double y0, double z0, double x1, double y1, double z1, Surface surface) {
        parts.add(new Part(x0, y0, z0, x1, y1, z1, surface));
    }

    public static List<Part> collarStrips(int joins) {
        double x0 = (joins & 1) != 0 ? 0 : 2, x1 = (joins & 2) != 0 ? 16 : 14;
        double z0 = (joins & 4) != 0 ? 0 : 2, z1 = (joins & 8) != 0 ? 16 : 14;
        List<Part> strips = new ArrayList<>(4);
        if (x0 > 0) add(strips, 0, 0, 0, x0, 16, 16, Surface.MASONRY);
        if (x1 < 16) add(strips, x1, 0, 0, 16, 16, 16, Surface.MASONRY);
        if (z0 > 0) add(strips, x0, 0, 0, x1, 16, z0, Surface.MASONRY);
        if (z1 < 16) add(strips, x0, 0, z1, x1, 16, 16, Surface.MASONRY);
        return strips;
    }

    public static List<AxisAlignedBB> collision(Kind kind, int joins, EnumFacing facing) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        for (Part part : parts(kind, joins, false))
            boxes.add(rotateBox(part.x0, part.y0, part.z0, part.x1, part.y1, part.z1, facing));
        if (kind == Kind.COLLAR) for (Part strip : collarStrips(joins)) {
            for (double z = strip.z0; z < strip.z1; z++) {
                double high = 16 * RoofGeometry.height(RoofShape.STRAIGHT, false, strip.x0/16, z/16);
                boxes.add(rotateBox(strip.x0, 0, z, strip.x1, high, z+1, facing));
            }
        }
        return boxes;
    }

    private static AxisAlignedBB rotateBox(double x0, double y0, double z0, double x1, double y1, double z1, EnumFacing facing) {
        double[] a = RoofGeometry.rotate(x0/16,y0/16,z0/16,facing);
        double[] b = RoofGeometry.rotate(x1/16,y1/16,z1/16,facing);
        return new AxisAlignedBB(Math.min(a[0],b[0]),a[1],Math.min(a[2],b[2]),Math.max(a[0],b[0]),b[1],Math.max(a[2],b[2]));
    }

    public static EnumFacing rotateSide(EnumFacing side, EnumFacing facing) {
        int turns = facing == EnumFacing.NORTH ? 0 : facing == EnumFacing.EAST ? 1 : facing == EnumFacing.SOUTH ? 2 : 3;
        for (int i = 0; i < turns; i++) side = side.rotateY();
        return side;
    }
}
