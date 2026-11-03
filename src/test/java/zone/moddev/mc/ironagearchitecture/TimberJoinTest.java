package zone.moddev.mc.ironagearchitecture;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import net.minecraft.util.EnumFacing;
import zone.moddev.mc.ironagearchitecture.block.*;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.*;

public class TimberJoinTest {
    private List<Part> parts(FrameFamily family,int pattern,WallProfile profile,int mask) {
        return profile==null?WallGeometry.parts(family,pattern,EnumFacing.NORTH,mask):PartialWallGeometry.parts(profile,family,pattern,EnumFacing.NORTH,mask);
    }
    private double edge(List<Part> parts,boolean left) {
        double result=left?Double.NEGATIVE_INFINITY:Double.POSITIVE_INFINITY;
        for(Part part:parts) if(part.grain==Grain.EAST_WEST) for(double[] p:part.outline) result=left?Math.max(result,p[0]):Math.min(result,p[0]);
        return result;
    }
    @Test public void oneSidedRailsStopAtStudsRatherThanTheEmptyEdgeOfThePanel() {
        for(WallProfile profile:new WallProfile[]{null,WallProfile.THIN,WallProfile.SHORT_LEFT,WallProfile.SHORT_RIGHT}) for(int pattern=0;pattern<2;pattern++) for(boolean left:new boolean[]{false,true}) {
            if(profile==WallProfile.SHORT_LEFT&&!left||profile==WallProfile.SHORT_RIGHT&&left) continue;
            double shift=profile==null||profile==WallProfile.THIN?0:profile==WallProfile.SHORT_LEFT?-4:4;
            double expected=(pattern==0?(left?9:7):(left?11.5:4.5))+shift;
            int mask=WallGeometry.bit(left?EnumFacing.WEST:EnumFacing.EAST)<<4;
            assertEquals("profile="+profile+" pattern="+pattern+" left="+left,expected,edge(parts(FrameFamily.STUD,pattern,profile,mask),left),1e-8);
        }
    }
    @Test public void oneSidedRailsFinishOnTheDiagonalAtTheirOwnHeight() {
        for(int pattern=0;pattern<2;pattern++) for(boolean left:new boolean[]{false,true}) for(boolean top:new boolean[]{false,true}) {
            boolean near=(pattern==0)!=top;
            double expected=left?(near?4.25:16):(near?0:11.75);
            int mask=WallGeometry.bit(left?EnumFacing.WEST:EnumFacing.EAST)<<(top?8:4);
            assertEquals(expected,edge(parts(FrameFamily.BRACE,pattern,WallProfile.THIN,mask),left),1e-8);
        }
    }
    @Test public void completeRailRunsAndDeliberateRailPanelsKeepTheirWidth() {
        for(FrameFamily family:new FrameFamily[]{FrameFamily.POST,FrameFamily.STUD,FrameFamily.BRACE,FrameFamily.TERMINAL}) {
            List<Part> both=parts(family,1,WallProfile.THIN,(WallGeometry.bit(EnumFacing.WEST)|WallGeometry.bit(EnumFacing.EAST))<<4);
            assertEquals(16,edge(both,true),1e-8);assertEquals(0,edge(both,false),1e-8);
        }
        assertEquals(16,edge(parts(FrameFamily.RAIL,0,WallProfile.THIN,0),true),1e-8);
        assertEquals(0,edge(parts(FrameFamily.RAIL,0,WallProfile.THIN,0),false),1e-8);
    }
}
