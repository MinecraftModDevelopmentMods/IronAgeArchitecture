package zone.moddev.mc.ironagearchitecture;

import org.junit.Test;
import static org.junit.Assert.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import zone.moddev.mc.ironagearchitecture.block.FrameFamily;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry;

public class WallGeometryTest {
    @Test public void plainInfillContainsNoAutomaticTimber() {
        for(WallGeometry.Part part:WallGeometry.parts(FrameFamily.INFILL,0,EnumFacing.NORTH,(1<<28)-1))
            assertEquals(WallGeometry.Grain.DAUB,part.grain);
    }
    @Test public void isolatedPostDoesNotAddBordersOrRails() {
        int timber=0;
        for(WallGeometry.Part part:WallGeometry.parts(FrameFamily.POST,0,EnumFacing.NORTH,0)) if(part.grain!=WallGeometry.Grain.DAUB) {
            AxisAlignedBB box=part.bounds(EnumFacing.NORTH);
            assertEquals(3.0/16,box.maxX-box.minX,1e-9);
            assertEquals(1,box.maxY-box.minY,1e-9);timber++;
        }
        assertEquals(1,timber);
    }
    @Test public void cornerHasOneOuterPostSharedByBothFaces() {
        int outer=0;
        for(WallGeometry.Part part:WallGeometry.parts(FrameFamily.TERMINAL,0,EnumFacing.NORTH,0)) if(part.grain==WallGeometry.Grain.VERTICAL) {
            AxisAlignedBB box=part.bounds(EnumFacing.NORTH);
            if(box.minX==0&&box.minZ==0) {
                assertEquals(3.0/16,box.maxX,1e-9);assertEquals(3.0/16,box.maxZ,1e-9);outer++;
            }
        }
        assertEquals(1,outer);
    }
    @Test public void railsHaveSingleConsistentThicknessAtChosenHeight() {
        for(int panel=0;panel<2;panel++) {
            int rails=0;
            for(WallGeometry.Part part:WallGeometry.parts(FrameFamily.RAIL,panel,EnumFacing.NORTH,0)) if(part.grain==WallGeometry.Grain.EAST_WEST) {
                AxisAlignedBB box=part.bounds(EnumFacing.NORTH);
                assertEquals(3.0/16,box.maxY-box.minY,1e-9);
                assertEquals(panel==0?0:13.0/16,box.minY,1e-9);rails++;
            }
            assertEquals(2,rails); // One continuous rail on each elevation, with no per-block borders.
        }
    }
    @Test public void everyModuleAndJoinStaysInsideItsCellInEveryFacing() {
        for(FrameFamily family:FrameFamily.values()) for(int panel=0;panel<4;panel++)
            for(EnumFacing facing:EnumFacing.HORIZONTALS) for(int mask:new int[]{0,15,255,4095,(1<<28)-1})
                for(WallGeometry.Part part:WallGeometry.parts(family,panel,facing,mask)) {
                    AxisAlignedBB box=part.bounds(facing);
                    assertTrue(box.minX>=-1e-9&&box.minY>=0&&box.minZ>=-1e-9);
                    assertTrue(box.maxX<=1+1e-9&&box.maxY<=1&&box.maxZ<=1+1e-9);
                    assertTrue(box.maxX>box.minX&&box.maxY>box.minY&&box.maxZ>box.minZ);
                }
    }
    @Test public void studsBridgeIntoSillAndHeadRails() {
        for(int panel=0;panel<2;panel++) {
            int mask=panel==0?3<<16:3<<12;
            int studs=0;
            for(WallGeometry.Part part:WallGeometry.parts(FrameFamily.RAIL,panel,EnumFacing.NORTH,mask)) if(part.grain==WallGeometry.Grain.VERTICAL) {
                AxisAlignedBB box=part.bounds(EnumFacing.NORTH);
                assertEquals(panel==0?3.0/16:0,box.minY,1e-9);
                assertEquals(panel==0?1:13.0/16,box.maxY,1e-9);studs++;
            }
            assertEquals(2,studs);
        }
    }
    @Test public void braceAttachmentsTerminateAtPrincipalPostCentre() {
        int count=0;
        for(WallGeometry.Part part:WallGeometry.parts(FrameFamily.POST,0,EnumFacing.NORTH,1<<20)) if(part.grain==WallGeometry.Grain.RISING) {
            AxisAlignedBB box=part.bounds(EnumFacing.NORTH);assertEquals(.5,box.minX,1e-9);assertEquals(1,box.maxX,1e-9);count++;
        }
        assertEquals(2,count);
    }
    @Test public void diagonalEndpointsMeetAcrossTwoBlockBays() {
        for(boolean rising:new boolean[]{true,false}) {
            WallGeometry.Part brace=null;
            for(WallGeometry.Part part:WallGeometry.parts(FrameFamily.BRACE,rising?0:1,EnumFacing.NORTH,0))
                if(part.grain==(rising?WallGeometry.Grain.RISING:WallGeometry.Grain.FALLING)) brace=part;
            assertNotNull(brace);
            boolean low=false,high=false;
            for(double[] p:brace.outline) {
                if(p[0]==(rising?0:16)&&p[1]==0) low=true;
                if(p[0]==(rising?16:0)&&p[1]==16) high=true;
            }
            assertTrue(low&&high);
        }
    }
}
