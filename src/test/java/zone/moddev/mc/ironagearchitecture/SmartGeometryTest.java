package zone.moddev.mc.ironagearchitecture;

import org.junit.Test;
import static org.junit.Assert.*;
import net.minecraft.util.EnumFacing;
import zone.moddev.mc.ironagearchitecture.block.*;

public class SmartGeometryTest {
    @Test public void joinedHalfPanelsFillTheSharedPlasterBoundary() {
        int coverage=3<<(EnumFacing.EAST.getHorizontalIndex()*4);
        boolean meets=false;
        for(WallGeometry.Part part:SmartWallGeometry.parts(3,SmartWallGeometry.FALLBACK,coverage))
            if(part.grain==WallGeometry.Grain.DAUB&&part.bounds(EnumFacing.NORTH).maxX==1) meets=true;
        assertTrue(meets);
    }
    @Test public void allPhysicalProfilesKeepMeshesInsideTheirCell() {
        for(int mask=1;mask<=15;mask++) for(int code:new int[]{SmartWallGeometry.FALLBACK,0xffffffff,0x80808080}) {
            for(WallGeometry.Part part:SmartWallGeometry.parts(mask,code,0)) {
                net.minecraft.util.math.AxisAlignedBB bounds=part.bounds(EnumFacing.NORTH);
                assertTrue(bounds.minX>=0&&bounds.minY>=0&&bounds.minZ>=0);
                assertTrue(bounds.maxX<=1&&bounds.maxY<=1&&bounds.maxZ<=1);
            }
        }
    }
}
