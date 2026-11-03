package zone.moddev.mc.ironagearchitecture;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import net.minecraft.util.EnumFacing;
import zone.moddev.mc.ironagearchitecture.block.*;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.*;

public class HorizontalRailTest {
    private int count(List<Part> parts,Grain grain){int count=0;for(Part part:parts) if(part.grain==grain) count++;return count;}
    @Test public void cornerRailsFollowOnlyTheirConnectedLeg() {
        for(WallProfile profile:WallProfile.values()) if(profile.corner) for(EnumFacing facing:EnumFacing.HORIZONTALS) for(int height=0;height<2;height++) {
            int shift=height==0?4:8;
            List<Part> alongX=PartialWallGeometry.parts(profile,FrameFamily.TERMINAL,0,facing,WallGeometry.bit(facing.rotateY())<<shift);
            assertEquals(2,count(alongX,Grain.EAST_WEST));assertEquals(0,count(alongX,Grain.NORTH_SOUTH));
            List<Part> alongZ=PartialWallGeometry.parts(profile,FrameFamily.TERMINAL,0,facing,WallGeometry.bit(facing.getOpposite())<<shift);
            assertEquals(0,count(alongZ,Grain.EAST_WEST));assertEquals(2,count(alongZ,Grain.NORTH_SOUTH));
        }
    }
    @Test public void incomingRailsCanFinishOnTheCornerPostWithoutGrowingAnUnusedReturn() {
        for(EnumFacing facing:EnumFacing.HORIZONTALS) {
            int incoming=(WallGeometry.bit(facing)|WallGeometry.bit(facing.rotateYCCW()))<<4;
            List<Part> parts=PartialWallGeometry.parts(WallProfile.CORNER_L,FrameFamily.TERMINAL,0,facing,incoming);
            assertEquals(0,count(parts,Grain.EAST_WEST));assertEquals(0,count(parts,Grain.NORTH_SOUTH));
        }
    }
    @Test public void rearCornerRailsStartAtTheInsidePostRatherThanShowingThroughTheOtherWall() {
        for(WallProfile profile:WallProfile.values()) if(profile.corner) for(int pattern=0;pattern<2;pattern++) {
            List<Part> parts=PartialWallGeometry.parts(profile,FrameFamily.TERMINAL,pattern,EnumFacing.NORTH,WallGeometry.bit(EnumFacing.EAST)<<4);
            double start=Double.POSITIVE_INFINITY;
            for(Part part:parts) if(part.grain==Grain.EAST_WEST&&part.front>profile.z0+4) for(double[] point:part.outline) start=Math.min(start,point[0]);
            assertEquals(profile.x0+8-(pattern==0?1.5:3),start,1e-8);
            List<Part> across=PartialWallGeometry.parts(profile,FrameFamily.TERMINAL,pattern,EnumFacing.NORTH,WallGeometry.bit(EnumFacing.SOUTH)<<4);
            double z=16;for(Part part:across) if(part.grain==Grain.NORTH_SOUTH&&part.outline[0][0]>profile.x0+4) z=Math.min(z,part.front);
            assertEquals(profile.z0+8-(pattern==0?1.5:3),z,1e-8);
        }
        List<Part> full=WallGeometry.parts(FrameFamily.TERMINAL,0,EnumFacing.NORTH,WallGeometry.bit(EnumFacing.EAST)<<4);
        double start=16;for(Part part:full) if(part.grain==Grain.EAST_WEST&&part.front>8) for(double[] point:part.outline) start=Math.min(start,point[0]);
        assertEquals(13,start,1e-8);
    }
}
