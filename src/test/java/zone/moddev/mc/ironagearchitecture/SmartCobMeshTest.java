package zone.moddev.mc.ironagearchitecture;

import java.awt.geom.Area;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import net.minecraft.util.math.*;
import zone.moddev.mc.ironagearchitecture.block.*;

public class SmartCobMeshTest {
    @Test public void squareFallbackIsClosedAndHasOutwardWinding() {
        SmartCobView view=new SmartCobView(SmartCobProfile.SQUARE,null,null,false,false);
        assertEquals(1,volume(SmartCobMesh.faces(view)),1e-7);
    }
    @Test public void curvedFacesAndRaySelectionUseTheSameContour() {
        SmartCobGeometry.Section section=SmartCobGeometry.solve(SmartCobGeometryTest.ring(8,6,1),new Area());
        for(SmartCobProfile profile:section.profiles.values()) {
            SmartCobView view=new SmartCobView(profile,null,null,false,false);
            for(SmartCobMesh.Face face:SmartCobMesh.faces(view)) {
                if(face.normals[0][1]==0) {
                    double length=Math.hypot(face.points[3][0]-face.points[0][0],face.points[3][2]-face.points[0][2]);
                    if(length>.05)assertEquals("Contour texture must not collapse into horizontal stripes",length,Math.abs(face.uv[3][0]-face.uv[0][0]),length*.15+.001);
                }
                for(double[] p:face.points)for(double coordinate:p)assertTrue(coordinate>=-1e-8&&coordinate<=1.00000001);
                for(double[] n:face.normals)assertEquals(1,Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]),.004);
                double[] a=face.points[0],b=face.points[1],c=face.points[2],n=face.normals[0];
                double x=(b[1]-a[1])*(c[2]-a[2])-(b[2]-a[2])*(c[1]-a[1]),y=(b[2]-a[2])*(c[0]-a[0])-(b[0]-a[0])*(c[2]-a[2]),z=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
                assertTrue("Inward cob face",x*n[0]+y*n[1]+z*n[2]>=-1e-8);
            }
            Area shape=profile.area();
            for(double x=.031;x<1;x+=.11)for(double z=.031;z<1;z+=.11) {
                RayTraceResult hit=SmartCobMesh.trace(view,BlockPos.ORIGIN,new Vec3d(x,2,z),new Vec3d(x,-1,z));
                assertEquals(shape.contains(x,z),hit!=null);
            }
        }
    }
    @Test public void stackedCoursesHideTheirSharedCaps() {
        SmartCobProfile profile=SmartCobProfile.SQUARE;
        assertEquals(4,SmartCobMesh.faces(new SmartCobView(profile,profile,profile,false,false)).size());
    }
    @Test public void everyOutsideBoundaryCanBeTargetedInItsOwningCell() {
        SmartCobGeometry.Section section=SmartCobGeometry.solve(SmartCobGeometryTest.ring(11,9,2),new Area());
        for(List<double[]> loop:SmartCobGeometry.loops(section.shape))for(int i=0;i<loop.size();i++) {
            double[] a=loop.get(i),b=loop.get((i+1)%loop.size());double length=Math.hypot(b[0]-a[0],b[1]-a[1]);if(length<.001)continue;
            double x=(a[0]+b[0])/2,z=(a[1]+b[1])/2,nx=(b[1]-a[1])/length,nz=-(b[0]-a[0])/length;
            int cx=(int)Math.floor(x-nx*.0001),cz=(int)Math.floor(z-nz*.0001);SmartCobProfile profile=section.profiles.get(SmartCobGeometry.key(cx,cz));
            assertNotNull(profile);SmartCobView view=new SmartCobView(profile,null,null,false,false);
            assertNotNull("Missing outside skin",SmartCobMesh.trace(view,new BlockPos(cx,0,cz),new Vec3d(x+nx*.05,.5,z+nz*.05),new Vec3d(x-nx*.05,.5,z-nz*.05)));
        }
    }
    private static double volume(List<SmartCobMesh.Face> faces) {
        double total=0;
        for(SmartCobMesh.Face face:faces)for(int i=1;i<3;i++) {
            double[] a=face.points[0],b=face.points[i],c=face.points[i+1];
            total+=(a[0]*(b[1]*c[2]-b[2]*c[1])+a[1]*(b[2]*c[0]-b[0]*c[2])+a[2]*(b[0]*c[1]-b[1]*c[0]))/6;
        }
        return total;
    }
}
