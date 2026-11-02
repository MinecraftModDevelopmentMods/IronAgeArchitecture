package zone.moddev.mc.ironagearchitecture;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import zone.moddev.mc.ironagearchitecture.block.*;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.*;

public class JettyGeometryTest {
    @Test public void allWallPartsStayInsideTheirSavedFootprint() {
        for(WallProfile p:WallProfile.values()) for(FrameFamily f:FrameFamily.values()) {
            if(p.corner&&f!=FrameFamily.TERMINAL) continue;
            for(int panel=0;panel<4;panel++) for(int mask:new int[]{0,0x0fffffff}) {
                List<Part> parts=PartialWallGeometry.parts(p,f,panel,EnumFacing.NORTH,mask);
                assertFalse(parts.isEmpty());
                for(Part part:parts) {
                    AxisAlignedBB b=part.bounds(EnumFacing.NORTH);
                    assertTrue(p+" "+f+" "+b,b.minX>=0&&b.minY>=0&&b.minZ>=0&&b.maxX<=1&&b.maxY<=1&&b.maxZ<=1);
                    for(double[] vertex:part.outline) for(double depth:new double[]{part.front,part.back}) {
                        double[] q=part.transform(vertex[0],vertex[1],depth,EnumFacing.NORTH);
                        assertTrue(p+" vertex "+Arrays.toString(q),p.contains(q[0],q[2]));
                    }
                }
            }
        }
    }
    @Test public void shortBracesRetainFortyFiveDegreePhase() {
        for(WallProfile p:new WallProfile[]{WallProfile.SHORT_LEFT,WallProfile.SHORT_RIGHT}) {
            List<Part> parts=PartialWallGeometry.parts(p,FrameFamily.BRACE,0,EnumFacing.NORTH,0);
            for(Part part:parts) if(part.grain==Grain.RISING) for(double[] v:part.outline) assertTrue(Math.abs(v[0]-v[1])<=1.25001);
        }
    }
    @Test public void thinPostRetainsThreePixelWidthAndHalfBlockDepth() {
        for(Part part:PartialWallGeometry.parts(WallProfile.THIN,FrameFamily.POST,0,EnumFacing.NORTH,0)) if(part.grain==Grain.VERTICAL) {
            AxisAlignedBB b=part.bounds(EnumFacing.NORTH);assertEquals(3/16.0,b.maxX-b.minX,1e-9);assertEquals(.5,b.maxZ-b.minZ,1e-9);return;
        }
        fail("post missing");
    }
    @Test public void floorBoardsCoverEveryOccupiedQuarterAtTheSameHeight() {
        for(FloorShape shape:FloorShape.values()) for(boolean axis:new boolean[]{false,true}) {
            int boards=0;
            for(Part part:JettyFloorGeometry.parts(shape,axis,0,null,false)) if(part.grain==Grain.BOARDS_X||part.grain==Grain.BOARDS_Z) {
                boards++;AxisAlignedBB b=part.bounds(EnumFacing.NORTH);assertEquals(1,b.maxY,0);assertEquals(14/16.0,b.minY,0);
            }
            assertEquals(Integer.bitCount(shape.mask),boards);
        }
    }
    @Test public void coveredEdgesDoNotGeneratePerimeterBeams() {
        assertTrue(JettyFloorGeometry.exposed(FloorShape.HALF,0,EnumFacing.NORTH,0));
        assertFalse(JettyFloorGeometry.exposed(FloorShape.HALF,0,EnumFacing.EAST,0));
        assertFalse(JettyFloorGeometry.exposed(FloorShape.HALF,0,EnumFacing.NORTH,1<<EnumFacing.NORTH.getHorizontalIndex()));
    }
    @Test public void floorAndBracketBoundsRotateInsideTheirCells() {
        for(FloorShape shape:FloorShape.values()) for(boolean full:new boolean[]{false,true}) for(EnumFacing side:EnumFacing.HORIZONTALS) {
            if(JettyFloorGeometry.bracketAnchor(shape,side,full)==null) continue;
            for(Part part:JettyFloorGeometry.parts(shape,false,0,side,full)) for(EnumFacing facing:EnumFacing.HORIZONTALS) {
                AxisAlignedBB b=part.bounds(facing);
                assertTrue(shape+" "+side+" "+b,b.minX>=-1e-9&&b.minY>=-1e-9&&b.minZ>=-1e-9&&b.maxX<=1.000001&&b.maxY<=1.000001&&b.maxZ<=1.000001);
            }
        }
    }
    @Test public void quarterDeckCannotCarryAWholeBlockBracket() {
        for(EnumFacing side:EnumFacing.HORIZONTALS) {
            assertNull(JettyFloorGeometry.bracketAnchor(FloorShape.QUARTER,side,true));
            assertNotNull(JettyFloorGeometry.bracketAnchor(FloorShape.QUARTER,side,false));
        }
    }
    @Test public void floorRotationsPreserveArea() {
        for(FloorShape shape:FloorShape.values()) for(EnumFacing facing:EnumFacing.HORIZONTALS)
            assertEquals(Integer.bitCount(shape.mask),Integer.bitCount(shape.rotatedMask(facing)));
    }
    @Test public void diagonalCornerTiesReachAllFourOutsideCorners() {
        for(double x:new double[]{0,16}) for(double z:new double[]{0,16}) {
            boolean found=false;
            for(Part part:JettyFloorGeometry.parts(FloorShape.FULL,false,0,null,false)) if(part.horizontal)
                for(double[] point:part.outline) if(point[0]==x&&point[1]==z) found=true;
            assertTrue("Corner tie missing at "+x+", "+z,found);
        }
    }
}
