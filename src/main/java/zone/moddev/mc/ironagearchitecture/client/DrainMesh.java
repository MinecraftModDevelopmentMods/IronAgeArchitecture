package zone.moddev.mc.ironagearchitecture.client;

/** Compatibility adapter: the same hollow profiles also supply server-side roof targeting. */
final class DrainMesh {
    interface Face extends zone.moddev.mc.ironagearchitecture.block.DrainMesh.Face {}
    static void gutter(boolean outlet,int ends,Face face){zone.moddev.mc.ironagearchitecture.block.DrainMesh.gutter(outlet,ends,face);}
    static void pipe(double[][] path,Face face){zone.moddev.mc.ironagearchitecture.block.DrainMesh.pipe(path,face);}
}
