package zone.moddev.mc.ironagearchitecture;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import zone.moddev.mc.ironagearchitecture.block.*;

public class WindowGeometryTest {
    private double area(List<WindowGeometry.Face> faces){double a=0;for(WindowGeometry.Face f:faces) a+=SurfaceGeometry.area(f.vertices);return a;}
    @Test public void movingAndSplittingPreserveTheSurface() {
        for(boolean thin:new boolean[]{false,true}) for(boolean diamond:new boolean[]{false,true}) for(boolean right:new boolean[]{false,true}) for(EnumFacing facing:EnumFacing.HORIZONTALS) for(int joins:new int[]{0,3,15}) {
            double closed=area(WindowGeometry.faces(thin,diamond,right,false,facing,joins,false));
            double open=area(WindowGeometry.faces(thin,diamond,right,true,facing,joins,false))+area(WindowGeometry.faces(thin,diamond,right,true,facing,joins,true));
            assertEquals(closed,open,1e-4);
        }
    }
    @Test public void everyCellMeshAndCollisionBoxStaysInsideItsCell() {
        for(boolean open:new boolean[]{false,true}) for(boolean right:new boolean[]{false,true}) for(EnumFacing facing:EnumFacing.HORIZONTALS) for(boolean proxy:new boolean[]{false,true}) {
            for(WindowGeometry.Face face:WindowGeometry.faces(true,true,right,open,facing,3,proxy)) for(double[] p:face.vertices) {
                for(int i=0;i<3;i++) assertTrue(Arrays.toString(p),p[i]>=-1e-6&&p[i]<=16.000001);
                assertTrue(p[3]>=-1e-6&&p[3]<=16.000001);assertTrue(p[4]>=-1e-6&&p[4]<=16.000001);
            }
            for(AxisAlignedBB b:WindowGeometry.boxes(true,right,open,facing,3,proxy)) assertTrue(b.minX>=0&&b.minY>=0&&b.minZ>=0&&b.maxX<=1&&b.maxY<=1&&b.maxZ<=1);
        }
    }
    @Test public void openCasementGivesAPlayerRoomThroughTwoStackedSections() {
        for(boolean thin:new boolean[]{false,true}) for(boolean right:new boolean[]{false,true}) for(double z=-.8;z<1.5;z+=.1) {
            AxisAlignedBB player=new AxisAlignedBB(.2,1.5/16,z-.3,.8,1.5/16+1.8,z+.3);
            for(int y=0;y<2;y++) for(boolean proxy:new boolean[]{false,true}) for(AxisAlignedBB box:WindowGeometry.boxes(thin,right,true,EnumFacing.NORTH,y==0?WindowGeometry.UP:WindowGeometry.DOWN,proxy))
                assertFalse(box.offset(0,y,proxy?-1:0).intersectsWith(player));
        }
    }
    @Test public void closedGlassBlocksTheCentreAndOnlyOpenLeavesUseOutsideCells() {
        AxisAlignedBB path=new AxisAlignedBB(.4,.4,-.5,.6,.6,.5);boolean hit=false;
        for(AxisAlignedBB b:WindowGeometry.boxes(false,false,false,EnumFacing.NORTH,0,false)) hit|=b.intersectsWith(path);
        assertTrue(hit);assertTrue(WindowGeometry.boxes(false,false,false,EnumFacing.NORTH,0,true).isEmpty());
        assertFalse(WindowGeometry.boxes(false,false,true,EnumFacing.NORTH,0,true).isEmpty());
    }
    @Test public void rearHalfPlacementsKeepTheirPlaneAndStillOpenOutwards() {
        for(boolean right:new boolean[]{false,true}) for(EnumFacing facing:EnumFacing.HORIZONTALS) {
            List<WindowGeometry.Face> closed=WindowGeometry.faces(true,true,right,false,facing,0,false,true);
            List<WindowGeometry.Face> opened=WindowGeometry.faces(true,true,right,true,facing,0,false,true);
            List<WindowGeometry.Face> outside=WindowGeometry.faces(true,true,right,true,facing,0,true,true);
            assertFalse(outside.isEmpty());assertEquals(area(closed),area(opened)+area(outside),1e-4);
            for(WindowGeometry.Face face:closed) for(double[] vertex:face.vertices) {
                EnumFacing inverse=facing==EnumFacing.EAST?EnumFacing.WEST:facing==EnumFacing.WEST?EnumFacing.EAST:facing;
                assertTrue(WallGeometry.rotate(vertex[0],vertex[1],vertex[2],inverse)[2]>=8-1e-6);
            }
        }
    }
}
