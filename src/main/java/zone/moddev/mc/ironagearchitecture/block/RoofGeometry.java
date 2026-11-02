package zone.moddev.mc.ironagearchitecture.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

/** Pure shared height fields for smooth baked surfaces and 1/16-step collision. */
public final class RoofGeometry {
    public static final int STEPS = 16;
    private RoofGeometry() { }
    public static double height(RoofShape shape, boolean ridge, double x, double z) {
        if (ridge) return Math.min(x, 1 - x);
        double main = 1 - z;
        switch (shape) {
            case INNER_LEFT: return Math.max(main, 1 - x);
            case INNER_RIGHT: return Math.max(main, x);
            case OUTER_LEFT: return Math.min(main, 1 - x);
            case OUTER_RIGHT: return Math.min(main, x);
            default: return main;
        }
    }
    public static double[] rotate(double x, double y, double z, EnumFacing facing) {
        switch (facing) {
            case EAST: return new double[]{1 - z, y, x};
            case SOUTH: return new double[]{1 - x, y, 1 - z};
            case WEST: return new double[]{z, y, 1 - x};
            default: return new double[]{x, y, z};
        }
    }
    public static List<AxisAlignedBB> boxes(RoofShape shape, boolean ridge, EnumFacing facing) {
        List<AxisAlignedBB> result = new ArrayList<>();
        for (int z = 0; z < STEPS; z++) {
            int start = 0;
            while (start < STEPS) {
                double top = cellHeight(shape, ridge, start, z);
                int end = start + 1;
                while (end < STEPS && Math.abs(cellHeight(shape, ridge, end, z) - top) < 1e-8) end++;
                if (top > 0) {
                    double[] a = rotate(start / 16.0, 0, z / 16.0, facing);
                    double[] b = rotate(end / 16.0, top, (z + 1) / 16.0, facing);
                    result.add(new AxisAlignedBB(Math.min(a[0], b[0]), 0, Math.min(a[2], b[2]),
                            Math.max(a[0], b[0]), top, Math.max(a[2], b[2])));
                }
                start = end;
            }
        }
        return result;
    }
    private static double cellHeight(RoofShape shape, boolean ridge, int x, int z) {
        double max = 0;
        for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++)
            max = Math.max(max, height(shape, ridge, (x + dx) / 16.0, (z + dz) / 16.0));
        return max;
    }
    /** Unit-square top triangles; the ridge and corner seams are explicit mesh edges. */
    public static List<double[][]> top(RoofShape shape, boolean ridge) {
        List<double[][]> faces = new ArrayList<>();
        if (ridge) {
            faces.add(new double[][]{{0,0,0},{0,0,1},{.5,.5,1},{.5,.5,0}});
            faces.add(new double[][]{{.5,.5,0},{.5,.5,1},{1,0,1},{1,0,0}});
        } else {
            double[] a = point(shape,0,0), b = point(shape,0,1), c = point(shape,1,1), d = point(shape,1,0);
            if (shape == RoofShape.INNER_RIGHT || shape == RoofShape.OUTER_RIGHT) {
                faces.add(new double[][]{a,b,d,d}); faces.add(new double[][]{b,c,d,d});
            } else {
                faces.add(new double[][]{a,b,c,c}); faces.add(new double[][]{a,c,d,d});
            }
        }
        return faces;
    }
    private static double[] point(RoofShape shape, double x, double z) { return new double[]{x,height(shape,false,x,z),z}; }
}
