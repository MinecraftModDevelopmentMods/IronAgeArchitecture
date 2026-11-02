package zone.moddev.mc.ironagearchitecture;

import org.junit.Test;
import static org.junit.Assert.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import zone.moddev.mc.ironagearchitecture.block.RoofGeometry;
import zone.moddev.mc.ironagearchitecture.block.RoofShape;

public class RoofGeometryTest {
    @Test public void roofProfilesAreContinuousAtCornerSeams() {
        for (int i=0;i<=32;i++) {
            double t=i/32.0;
            for (RoofShape shape:RoofShape.values()) {
                double x=shape.name().endsWith("RIGHT")?1-t:t;
                assertEquals(1-t,RoofGeometry.height(shape,false,x,t),1e-9);
            }
        }
    }
    @Test public void valleyAndHipOwnTheCorrectHalf() {
        assertEquals(.8,RoofGeometry.height(RoofShape.INNER_LEFT,false,.2,.7),1e-9);
        assertEquals(.3,RoofGeometry.height(RoofShape.OUTER_LEFT,false,.2,.7),1e-9);
        assertEquals(.8,RoofGeometry.height(RoofShape.INNER_RIGHT,false,.8,.7),1e-9);
    }
    @Test public void ridgeHasHalfHeightApexAndFortyFiveDegreeSides() {
        assertEquals(0,RoofGeometry.height(RoofShape.STRAIGHT,true,0,.5),0);
        assertEquals(.5,RoofGeometry.height(RoofShape.STRAIGHT,true,.5,.5),0);
        assertEquals(.25,RoofGeometry.height(RoofShape.STRAIGHT,true,.75,.5),0);
    }
    @Test public void allCollisionBoxesStayInsideTheCellAndCoverSurface() {
        for (RoofShape shape:RoofShape.values()) for (EnumFacing facing:EnumFacing.HORIZONTALS) for (boolean ridge:new boolean[]{false,true}) {
            for (AxisAlignedBB box:RoofGeometry.boxes(shape,ridge,facing)) {
                assertTrue(box.minX>=0&&box.maxX<=1&&box.minZ>=0&&box.maxZ<=1);
                assertTrue(box.minY==0&&box.maxY>0&&box.maxY<=(ridge?.5:1));
            }
            for (int x=0;x<16;x++) for(int z=0;z<16;z++) {
                double px=(x+.5)/16, pz=(z+.5)/16;
                double[] p=RoofGeometry.rotate(px,0,pz,facing);
                double top=0;
                for(AxisAlignedBB box:RoofGeometry.boxes(shape,ridge,facing))
                    if(p[0]>=box.minX&&p[0]<=box.maxX&&p[2]>=box.minZ&&p[2]<=box.maxZ) top=Math.max(top,box.maxY);
                double visual=RoofGeometry.height(shape,ridge,px,pz);
                assertTrue(top>=visual-1e-9&&top-visual<=1.0/16+1e-9);
            }
        }
    }
    @Test public void rotationsPreserveHeightAndReturnToStart() {
        double[] point={.2,.7,.8};
        for(int i=0;i<4;i++) point=RoofGeometry.rotate(point[0],point[1],point[2],EnumFacing.EAST);
        assertArrayEquals(new double[]{.2,.7,.8},point,1e-9);
    }
}
