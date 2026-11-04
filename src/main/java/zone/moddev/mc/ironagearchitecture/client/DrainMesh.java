package zone.moddev.mc.ironagearchitecture.client;

import zone.moddev.mc.ironagearchitecture.block.DrainGeometry;

/** Hollow iron profiles, shared by the fascia mesh and the inventory fittings. */
final class DrainMesh {
    interface Face { void add(double[] a, double[] b, double[] c, double[] d); }

    private DrainMesh() { }

    static void gutter(boolean outlet, int ends, Face face) {
        for (int i = 0; i < DrainGeometry.FACETS; i++) {
            double[] a = DrainGeometry.gutterPoint(i, 1.5), b = DrainGeometry.gutterPoint(i+1, 1.5);
            double[] c = DrainGeometry.gutterPoint(i, 1.13), d = DrainGeometry.gutterPoint(i+1, 1.13);
            if (outlet && DrainGeometry.outletOpening(i)) {
                trough(face, 0, 7.2, a, b, c, d);
                trough(face, 8.8, 16, a, b, c, d);
                face.add(gutterPoint(7.2,b), gutterPoint(7.2,a), gutterPoint(7.2,c), gutterPoint(7.2,d));
                face.add(gutterPoint(8.8,a), gutterPoint(8.8,b), gutterPoint(8.8,d), gutterPoint(8.8,c));
            } else trough(face, 0, 16, a, b, c, d);
            if ((ends & 1) != 0) face.add(gutterPoint(0,a), gutterPoint(0,b), gutterPoint(0,d), gutterPoint(0,c));
            if ((ends & 2) != 0) face.add(gutterPoint(16,b), gutterPoint(16,a), gutterPoint(16,c), gutterPoint(16,d));
        }
        for (int i : new int[]{0, DrainGeometry.FACETS}) {
            double[] a = DrainGeometry.gutterPoint(i, 1.5), b = DrainGeometry.gutterPoint(i, 1.13);
            if (i == 0) face.add(gutterPoint(0,a), gutterPoint(16,a), gutterPoint(16,b), gutterPoint(0,b));
            else face.add(gutterPoint(16,a), gutterPoint(0,a), gutterPoint(0,b), gutterPoint(16,b));
        }
        if ((ends & 1) != 0) cap(0, .22, face);
        if ((ends & 2) != 0) cap(16, 15.78, face);
        if (outlet) tube(new double[][]{{1.54, DrainGeometry.OUTER}, {0, DrainGeometry.OUTER}},
            DrainGeometry.COLLAR, false, face);
    }

    private static void trough(Face face, double start, double end, double[] a, double[] b, double[] c, double[] d) {
        face.add(gutterPoint(start,a), gutterPoint(start,b), gutterPoint(end,b), gutterPoint(end,a));
        face.add(gutterPoint(start,d), gutterPoint(start,c), gutterPoint(end,c), gutterPoint(end,d));
    }

    private static void cap(double outer, double inner, Face face) {
        double[] center = {2.7, DrainGeometry.OUTER};
        for (int i = 0; i < DrainGeometry.FACETS; i++) {
            double[] a = DrainGeometry.gutterPoint(i, 1.13), b = DrainGeometry.gutterPoint(i+1, 1.13);
            if (outer == 0) {
                face.add(gutterPoint(outer,a), gutterPoint(outer,center), gutterPoint(outer,b), gutterPoint(outer,b));
                face.add(gutterPoint(inner,b), gutterPoint(inner,center), gutterPoint(inner,a), gutterPoint(inner,a));
            } else {
                face.add(gutterPoint(outer,b), gutterPoint(outer,center), gutterPoint(outer,a), gutterPoint(outer,a));
                face.add(gutterPoint(inner,a), gutterPoint(inner,center), gutterPoint(inner,b), gutterPoint(inner,b));
            }
        }
        double[] a = DrainGeometry.gutterPoint(0,1.13), b = DrainGeometry.gutterPoint(DrainGeometry.FACETS,1.13);
        face.add(gutterPoint(Math.min(outer,inner),a), gutterPoint(Math.max(outer,inner),a),
            gutterPoint(Math.max(outer,inner),b), gutterPoint(Math.min(outer,inner),b));
    }

    private static double[] gutterPoint(double x, double[] point) { return new double[]{x, point[0], point[1]}; }

    static void pipe(double[][] path, Face face) {
        tube(path, DrainGeometry.RADIUS, true, face);
        if (path[0][0] == 16) {
            double z = path[0][1];
            tube(new double[][]{{15.3,z}, {14.5,z}}, DrainGeometry.COLLAR, false, face);
        }
    }

    private static void tube(double[][] path, double radius, boolean hollow, Face face) {
        double[][] outside = DrainGeometry.ring(path, 0, radius);
        double[][] inside = DrainGeometry.ring(path, 0, .61);
        if (hollow) rim(outside, inside, true, face);
        for (int i = 1; i < path.length; i++) {
            double[][] next = DrainGeometry.ring(path, i, radius);
            double[][] nextInside = DrainGeometry.ring(path, i, .61);
            for (int j = 0; j < outside.length; j++) {
                int k = (j + 1) % outside.length;
                face.add(outside[j], outside[k], next[k], next[j]);
                if (hollow) face.add(inside[k], inside[j], nextInside[j], nextInside[k]);
            }
            outside = next;
            inside = nextInside;
        }
        rim(outside, inside, false, face);
        if (!hollow) rim(DrainGeometry.ring(path, 0, radius), DrainGeometry.ring(path, 0, .61), true, face);
    }

    private static void rim(double[][] outside, double[][] inside, boolean top, Face face) {
        for (int j = 0; j < outside.length; j++) {
            int k = (j + 1) % outside.length;
            if (top) face.add(outside[k], outside[j], inside[j], inside[k]);
            else face.add(outside[j], outside[k], inside[k], inside[j]);
        }
    }
}
