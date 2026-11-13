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
    @Test public void diagonalBraceEndsReplaceTheDaubSurfaceTheyOccupy() {
        for(EnumFacing facing:EnumFacing.HORIZONTALS) for(int panel=0;panel<4;panel++) {
            List<Part> parts=WallGeometry.parts(FrameFamily.BRACE,panel,facing,0);
            double bottom=0;
            for(SurfaceGeometry.Face face:SurfaceGeometry.faces(parts,facing)) {
                boolean onBottom=true;
                for(double[] point:face.points)
                    if(Math.abs(face.part.transform(point[0],point[1],point[2],facing)[1])>1e-6) onBottom=false;
                if(onBottom) bottom+=SurfaceGeometry.area(face.points);
            }
            double inset=(panel&1)==0?.25:.375;
            double expected=16*(16-2*WallGeometry.RECESS)+2*1.25*(WallGeometry.RECESS-inset);
            assertEquals("Duplicate wood/daub skin at "+facing+" panel "+panel,expected,bottom,1e-4);
        }
    }
    @Test public void identicalSlopingPrismsHaveOneOutsideSkin() {
        double[][] outline={{0,0},{2,0},{16,14},{16,16},{14,16},{0,2}};
        for(EnumFacing facing:EnumFacing.HORIZONTALS) for(boolean horizontal:new boolean[]{false,true}) {
            Part timber=new Part(outline,.25,1.25,Grain.RISING,facing,horizontal);
            Part daub=new Part(outline,.25,1.25,Grain.DAUB,facing,horizontal);
            assertEquals(area(Collections.singletonList(timber)),area(Arrays.asList(daub,timber)),1e-4);
        }
    }
    @Test public void manualAndSmartWallsHaveNoCoplanarTimberDaubOverlap() {
        for(EnumFacing facing:EnumFacing.HORIZONTALS) for(FrameFamily family:FrameFamily.values()) for(int panel=0;panel<4;panel++) {
            noTimberDaubOverlap(WallGeometry.parts(family,panel,facing,0xfff),facing);
            for(WallProfile profile:WallProfile.values())
                noTimberDaubOverlap(PartialWallGeometry.parts(profile,family,panel,facing,0xfff),facing);
        }
        for(int mask=1;mask<=15;mask++) for(int code:new int[]{SmartWallGeometry.FALLBACK,0xffffffff,0x80808080})
            noTimberDaubOverlap(SmartWallGeometry.parts(mask,code,0),EnumFacing.NORTH);
    }
    /** Independently intersect the finished polygons in 2D, after their world transforms. */
    private static void noTimberDaubOverlap(List<Part> parts,EnumFacing facing) {
        List<SurfaceGeometry.Face> faces=SurfaceGeometry.faces(parts,facing);
        for(SurfaceGeometry.Face wood:faces) {
            if(wood.part.grain==Grain.DAUB) continue;
            double[][] a=worldPoints(wood,facing);
            double[] n=new double[3];
            for(int i=0;i<a.length;i++) for(int axis=0;axis<3;axis++)
                n[axis]+=a[i][(axis+1)%3]*a[(i+1)%a.length][(axis+2)%3]-a[i][(axis+2)%3]*a[(i+1)%a.length][(axis+1)%3];
            double length=Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);
            for(int axis=0;axis<3;axis++) n[axis]/=length;
            int drop=Math.abs(n[0])>Math.abs(n[1])?0:1;if(Math.abs(n[2])>Math.abs(n[drop])) drop=2;
            for(SurfaceGeometry.Face daub:faces) {
                if(daub.part.grain!=Grain.DAUB) continue;
                double[][] b=worldPoints(daub,facing);boolean coplanar=true;
                for(double[] p:b) if(Math.abs(n[0]*(p[0]-a[0][0])+n[1]*(p[1]-a[0][1])+n[2]*(p[2]-a[0][2]))>1e-6) coplanar=false;
                if(!coplanar) continue;
                java.awt.geom.Area overlap=project(a,drop);overlap.intersect(project(b,drop));
                java.awt.geom.PathIterator path=overlap.getPathIterator(null);double[] point=new double[6];
                double firstX=0,firstY=0,lastX=0,lastY=0,total=0;
                while(!path.isDone()) {
                    int kind=path.currentSegment(point);
                    if(kind==java.awt.geom.PathIterator.SEG_MOVETO) {firstX=lastX=point[0];firstY=lastY=point[1];}
                    else if(kind==java.awt.geom.PathIterator.SEG_LINETO) {total+=lastX*point[1]-lastY*point[0];lastX=point[0];lastY=point[1];}
                    else if(kind==java.awt.geom.PathIterator.SEG_CLOSE) total+=lastX*firstY-lastY*firstX;
                    path.next();
                }
                assertEquals("Overlapping plaster and "+wood.part.grain+" at "+facing,0,Math.abs(total)/2,1e-5);
            }
        }
    }
    private static double[][] worldPoints(SurfaceGeometry.Face face,EnumFacing facing) {
        double[][] result=new double[face.points.length][];
        for(int i=0;i<result.length;i++) result[i]=face.part.transform(face.points[i][0],face.points[i][1],face.points[i][2],facing);
        return result;
    }
    private static java.awt.geom.Area project(double[][] points,int drop) {
        java.awt.geom.Path2D.Double path=new java.awt.geom.Path2D.Double();
        for(int i=0;i<points.length;i++) {
            double x=points[i][(drop+1)%3],y=points[i][(drop+2)%3];
            if(i==0) path.moveTo(x,y);else path.lineTo(x,y);
        }
        path.closePath();return new java.awt.geom.Area(path);
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
