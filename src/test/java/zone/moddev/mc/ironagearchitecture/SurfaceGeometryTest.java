package zone.moddev.mc.ironagearchitecture;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import net.minecraft.util.EnumFacing;
import zone.moddev.mc.ironagearchitecture.block.*;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.*;

public class SurfaceGeometryTest {
    private static double area(List<Part> parts) {
        double total=0;for(SurfaceGeometry.Face face:SurfaceGeometry.faces(parts,EnumFacing.NORTH)) total+=SurfaceGeometry.area(face.points);return total;
    }
    @Test public void identicalBoxesHaveOneOutsideSkin() {
        List<Part> parts=new ArrayList<>();
        WallGeometry.box(parts,0,0,0,16,16,16,Grain.DAUB);
        WallGeometry.box(parts,0,0,0,16,16,16,Grain.VERTICAL);
        assertEquals(6*256,area(parts),1e-4);
    }
    @Test public void adjacentBoxesHideTheirInternalFaces() {
        List<Part> parts=new ArrayList<>();
        WallGeometry.box(parts,0,0,0,8,16,16,Grain.EAST_WEST);
        WallGeometry.box(parts,8,0,0,16,16,16,Grain.NORTH_SOUTH);
        assertEquals(6*256,area(parts),1e-4);
    }
    @Test public void overlappingRailsAndPostsDoNotDoubleTheirTopSurface() {
        List<Part> parts=new ArrayList<>();
        WallGeometry.box(parts,6,0,0,10,16,8,Grain.VERTICAL);
        WallGeometry.box(parts,0,13,0,16,16,2,Grain.EAST_WEST);
        double top=0;
        for(SurfaceGeometry.Face face:SurfaceGeometry.faces(parts,EnumFacing.NORTH)) {
            boolean upper=true;for(double[] p:face.points) if(Math.abs(p[1]-16)>1e-6) upper=false;
            if(upper) top+=SurfaceGeometry.area(face.points);
        }
        assertEquals(4*8+16*2-4*2,top,1e-4);
    }
    @Test public void separatedBoxesKeepBothSkins() {
        List<Part> parts=new ArrayList<>();WallGeometry.box(parts,0,0,0,4,4,4,Grain.DAUB);WallGeometry.box(parts,8,8,8,12,12,12,Grain.DAUB);
        assertEquals(2*6*16,area(parts),1e-4);
    }
    @Test public void clippingPreservesBoundsAndVisibleTimberInAllFacings() {
        for(EnumFacing facing:EnumFacing.HORIZONTALS) {
            List<Part> parts=PartialWallGeometry.parts(WallProfile.CORNER_L,FrameFamily.TERMINAL,0,facing,0xff0);
            List<SurfaceGeometry.Face> faces=SurfaceGeometry.faces(parts,facing);assertFalse(faces.isEmpty());
            boolean wood=false;
            for(SurfaceGeometry.Face face:faces) {
                if(face.part.grain!=Grain.DAUB) wood=true;
                for(double[] p:face.points) {
                    double[] q=face.part.transform(p[0],p[1],p[2],facing);
                    for(double coordinate:q) assertTrue(coordinate>=-1e-5&&coordinate<=16.00001);
                }
            }
            assertTrue(wood);
        }
    }
    @Test public void inverseTransformHandlesHorizontalAndTurnedParts() {
        Part part=new Part(new double[][]{{0,0},{16,0},{16,16},{0,16}},8,10,Grain.RISING,EnumFacing.WEST,true);
        for(EnumFacing facing:EnumFacing.HORIZONTALS) {
            double[] p=part.transform(3,5,9,facing),q=part.inverse(p[0],p[1],p[2],facing);
            assertArrayEquals(new double[]{3,5,9},q,1e-9);
        }
    }
}
