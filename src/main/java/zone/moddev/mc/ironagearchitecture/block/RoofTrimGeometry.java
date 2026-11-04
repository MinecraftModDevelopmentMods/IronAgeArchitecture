package zone.moddev.mc.ironagearchitecture.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import zone.moddev.mc.ironagearchitecture.block.TrimmedRoofBlock.Kind;

/** Shared section dimensions for the tile surface, its thin overhang and the timber edging. */
public final class RoofTrimGeometry {
    private static final double DEPTH = 3.0 / 16;
    private static final double EAVE = 1 - DEPTH;

    private RoofTrimGeometry() { }

    public static final class Section {
        public final double x0, x1, z0, z1;
        public final boolean thin;

        private Section(double x0, double x1, double z0, double z1, boolean thin) {
            this.x0 = x0;
            this.x1 = x1;
            this.z0 = z0;
            this.z1 = z1;
            this.thin = thin;
        }
    }

    public static Section[] sections(Kind kind, boolean right) {
        switch (kind) {
            case FASCIA: return new Section[]{new Section(0, 1, 0, EAVE, false)};
            case VERGE: return new Section[]{new Section(0, .5, 0, 1, !right), new Section(.5, 1, 0, 1, right)};
            case RIDGE_END: return new Section[]{
                new Section(0, .5, 0, .5, true), new Section(.5, 1, 0, .5, true),
                new Section(0, .5, .5, 1, false), new Section(.5, 1, .5, 1, false)};
            default: throw new AssertionError(kind);
        }
    }

    public static double height(Kind kind, double x, double z) {
        return RoofGeometry.height(RoofShape.STRAIGHT, kind == Kind.RIDGE_END, x, z);
    }

    public static double underside(Section section, double height) {
        return section.thin ? height - DEPTH : 0;
    }

    public static List<AxisAlignedBB> boxes(Kind kind, boolean right, EnumFacing facing) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        double step = 1.0 / RoofGeometry.STEPS;
        for (Section section : sections(kind, right)) {
            boolean ridge = kind == Kind.RIDGE_END;
            double start = ridge ? section.x0 : section.z0;
            double end = ridge ? section.x1 : section.z1;
            for (double at = start; at < end; at += step) {
                double x0 = ridge ? at : section.x0;
                double x1 = ridge ? at + step : section.x1;
                double z0 = ridge ? section.z0 : at;
                double z1 = ridge ? section.z1 : at + step;
                double first = height(kind, x0, z0);
                double last = height(kind, x1, z1);
                double low = Math.max(0, underside(section, Math.min(first, last)));
                double high = Math.max(first, last);
                double[] a = RoofGeometry.rotate(x0, low, z0, facing);
                double[] b = RoofGeometry.rotate(x1, high, z1, facing);
                boxes.add(new AxisAlignedBB(Math.min(a[0], b[0]), low, Math.min(a[2], b[2]),
                    Math.max(a[0], b[0]), high, Math.max(a[2], b[2])));
            }
        }
        return boxes;
    }
}
