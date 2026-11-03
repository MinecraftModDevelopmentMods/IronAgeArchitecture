package zone.moddev.mc.ironagearchitecture;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import zone.moddev.mc.ironagearchitecture.block.*;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.Part;

public class TimberCornerTest {
    @Test public void cornerContinuationsKeepTheirWidthWoodAndCellBounds() {
        for(boolean thin:new boolean[]{false,true}) for(int corner=0;corner<4;corner++) for(int wood=0;wood<6;wood++) {
            List<Part> parts=TimberCorners.add(Collections.emptyList(),thin,(wood+1)<<(corner*3));assertEquals(2,parts.size());
            for(Part part:parts) {
                assertEquals(ArchitectureBlocks.WOODS[wood],part.timber);
                double[][] polygon=new double[3][];for(int i=0;i<3;i++) polygon[i]=new double[]{part.outline[i][0],part.outline[i][1],0};
                assertEquals(1.25*1.25/2,SurfaceGeometry.area(polygon),1e-8);
                for(EnumFacing facing:EnumFacing.HORIZONTALS) {AxisAlignedBB b=part.bounds(facing);assertTrue(b.minX>=0&&b.minY>=0&&b.minZ>=0&&b.maxX<=1&&b.maxY<=1&&b.maxZ<=1);}
            }
        }
    }
    @Test public void continuationFacesHaveOutwardWinding() {
        for(int corner=0;corner<4;corner++) for(Part part:TimberCorners.add(Collections.emptyList(),true,1<<(corner*3))) {
            double x=0,y=0;for(double[] p:part.outline){x+=p[0]/3;y+=p[1]/3;}
            double[] centre={x,y,(part.front+part.back)/2};
            for(SurfaceGeometry.Face face:SurfaceGeometry.faces(Collections.singletonList(part),EnumFacing.NORTH)) {
                double[] a=face.points[0],b=face.points[1],c=face.points[2];
                double nx=(b[1]-a[1])*(c[2]-a[2])-(b[2]-a[2])*(c[1]-a[1]),ny=(b[2]-a[2])*(c[0]-a[0])-(b[0]-a[0])*(c[2]-a[2]),nz=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
                assertTrue(nx*(a[0]-centre[0])+ny*(a[1]-centre[1])+nz*(a[2]-centre[2])>1e-8);
            }
        }
    }
    @Test public void unconnectedInfillKeepsItsOriginalGeometry() {
        List<Part> plain=WallGeometry.parts(FrameFamily.INFILL,0,EnumFacing.NORTH,0);
        assertSame(plain,TimberCorners.add(plain,true,0));
    }
}
