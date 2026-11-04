package zone.moddev.mc.ironagearchitecture.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

/** Dimensions are in pixels, with +Z pointing out from the wall. */
public final class DrainGeometry {
    public static final double OUTER = 14.5, HALF = 6.5, WALL = 1.5;
    public static final double RADIUS = .88, COLLAR = 1.15;
    public static final int FACETS = 8;

    public enum Offset {
        EAVE(new double[][]{{16, OUTER}, {13, OUTER}, {3, WALL}, {0, WALL}}, EnumFacing.UP, EnumFacing.DOWN),
        HALF_BLOCK(new double[][]{{16, OUTER}, {12, OUTER}, {4, HALF}, {0, HALF}}, EnumFacing.UP, EnumFacing.DOWN),
        JETTY_OUT(new double[][]{{16, WALL}, {10, WALL}, {8, .9}, {8, 0}}, EnumFacing.UP, EnumFacing.NORTH),
        JETTY_IN(new double[][]{{8, 16}, {8, 14.5}, {2, WALL}, {0, WALL}}, EnumFacing.SOUTH, EnumFacing.DOWN);

        public final double[][] path;
        public final EnumFacing inlet, outlet;

        Offset(double[][] path, EnumFacing inlet, EnumFacing outlet) {
            this.path = path;
            this.inlet = inlet;
            this.outlet = outlet;
        }
    }

    private DrainGeometry() { }

    public static double[][] straight(int lane) {
        double depth = laneDepth(lane);
        return new double[][]{{16, depth}, {0, depth}};
    }

    public static double[][] shoe(int lane) {
        double depth = laneDepth(lane);
        // The outside lane has very little room left before the edge of the cell.
        double mouth = Math.min(depth + 5.5, 15.1);
        double heel = lane == 2 ? 3.5 : 7.25;
        return new double[][]{{16, depth}, {heel, depth}, {1.75, mouth}};
    }

    private static double laneDepth(int lane) {
        return lane == 0 ? WALL : lane == 1 ? HALF : OUTER;
    }

    public static EnumFacing rotateSide(EnumFacing side, EnumFacing facing) {
        if (side.getAxis().isVertical()) return side;
        return side == EnumFacing.NORTH ? facing : facing.getOpposite();
    }

    public static double[] gutterPoint(int index, double radius) {
        double angle = Math.PI * index / FACETS;
        return new double[]{2.7 - radius * Math.sin(angle), OUTER + radius * Math.cos(angle)};
    }

    public static boolean outletOpening(int facet) { return facet >= 2 && facet <= 5; }

    public static double[][] ring(double[][] path, int index, double radius) {
        double[] prev = path[Math.max(0, index - 1)];
        double[] next = path[Math.min(path.length - 1, index + 1)];
        double dy = next[0] - prev[0], dz = next[1] - prev[1];
        double length = Math.hypot(dy, dz);
        double[][] points = new double[FACETS][];
        for (int i = 0; i < FACETS; i++) {
            double angle = Math.PI * 2 * i / FACETS;
            double radial = radius * Math.sin(angle);
            points[i] = new double[]{8 + radius * Math.cos(angle), path[index][0] + radial * dz / length,
                path[index][1] - radial * dy / length};
        }
        return points;
    }

    public static List<AxisAlignedBB> gutterBoxes(boolean outlet, int ends, EnumFacing facing) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        for (int i = 0; i < FACETS; i++) {
            double[] a = gutterPoint(i, 1.5), b = gutterPoint(i+1, 1.5);
            double[] c = gutterPoint(i, 1.13), d = gutterPoint(i+1, 1.13);
            double lowY = min(a,b,c,d,0), lowZ = min(a,b,c,d,1);
            double highY = max(a,b,c,d,0), highZ = max(a,b,c,d,1);
            if (outlet && outletOpening(i)) {
                boxes.add(box(0, lowY, lowZ, 7.2, highY, highZ, facing));
                boxes.add(box(8.8, lowY, lowZ, 16, highY, highZ, facing));
            } else boxes.add(box(0, lowY, lowZ, 16, highY, highZ, facing));
            double capY = Math.min(c[0], d[0]), capZ0 = Math.min(c[1], d[1]), capZ1 = Math.max(c[1], d[1]);
            if ((ends & 1) != 0) boxes.add(box(0, capY, capZ0, .22, 2.7, capZ1, facing));
            if ((ends & 2) != 0) boxes.add(box(15.78, capY, capZ0, 16, 2.7, capZ1, facing));
        }
        if (outlet) boxes.add(box(8-COLLAR, 0, OUTER-COLLAR, 8+COLLAR, 1.36, OUTER+COLLAR, facing));
        return boxes;
    }

    private static double min(double[] a, double[] b, double[] c, double[] d, int index) {
        return Math.min(Math.min(a[index], b[index]), Math.min(c[index], d[index]));
    }

    private static double max(double[] a, double[] b, double[] c, double[] d, int index) {
        return Math.max(Math.max(a[index], b[index]), Math.max(c[index], d[index]));
    }

    public static List<AxisAlignedBB> pipeBoxes(double[][] path, EnumFacing facing) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        for (int i = 0; i < path.length - 1; i++) {
            double[] start = path[i], end = path[i+1];
            int steps = Math.max(1, (int)Math.ceil(Math.abs(end[1] - start[1])));
            for (int step = 0; step < steps; step++) {
                double a = step / (double)steps, b = (step + 1) / (double)steps;
                double y0 = start[0] + (end[0] - start[0]) * a, y1 = start[0] + (end[0] - start[0]) * b;
                double z0 = start[1] + (end[1] - start[1]) * a, z1 = start[1] + (end[1] - start[1]) * b;
                double rise = start[1] == end[1] ? 0 : RADIUS;
                boxes.add(box(8-RADIUS, Math.min(y0,y1)-rise, Math.min(z0,z1)-RADIUS,
                    8+RADIUS, Math.max(y0,y1)+rise, Math.max(z0,z1)+RADIUS, facing));
            }
        }
        if (path[0][0] == 16) boxes.add(box(8-COLLAR, 14.5, path[0][1]-COLLAR,
            8+COLLAR, 15.3, path[0][1]+COLLAR, facing));
        return boxes;
    }

    private static double clip(double value) { return Math.max(0, Math.min(16, value)) / 16; }

    private static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1, double z1, EnumFacing facing) {
        double[] a = RoofGeometry.rotate(clip(x0), clip(y0), clip(z0), facing);
        double[] b = RoofGeometry.rotate(clip(x1), clip(y1), clip(z1), facing);
        return new AxisAlignedBB(Math.min(a[0], b[0]), a[1], Math.min(a[2], b[2]),
            Math.max(a[0], b[0]), b[1], Math.max(a[2], b[2]));
    }

    public static Vec3d point(double y, double z, EnumFacing facing) {
        double[] p = RoofGeometry.rotate(.5, y / 16, z / 16, facing);
        return new Vec3d(p[0], p[1], p[2]);
    }
}
