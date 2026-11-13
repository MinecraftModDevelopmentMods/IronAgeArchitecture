package zone.moddev.mc.ironagearchitecture;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import zone.moddev.mc.ironagearchitecture.block.*;

public class SmartCobGeometryTest {
    public static Set<Long> ring(int width,int depth,int thick) {
        Set<Long> cells=new HashSet<>();for(int x=0;x<width;x++) for(int z=0;z<depth;z++)
            if(x<thick||z<thick||x>=width-thick||z>=depth-thick) cells.add(SmartCobGeometry.key(x,z));return cells;
    }
    @Test public void joinedWallsAreRoundedAtBuildingCornersNotEveryBlock() throws Exception {
        Set<Long> cells=ring(9,7,1);SmartCobGeometry.Section section=SmartCobGeometry.solve(cells,new Area());
        assertFalse("Outer corner must be cut back",section.shape.contains(.01,.01));
        assertTrue("Straight centre stays filled",section.shape.contains(4.5,.5));
        assertFalse("Room stays clear",section.shape.contains(4.5,3.5));
        assertTrue(SmartCobGeometry.thickEnough(section.shape));
        BufferedImage report=new BufferedImage(900,520,BufferedImage.TYPE_INT_RGB);Graphics2D g=report.createGraphics();
        g.setColor(Color.WHITE);g.fillRect(0,0,900,520);g.translate(35,35);g.scale(58,58);
        g.setColor(new Color(235,225,208));g.fill(SmartCobGeometry.footprint(cells));g.setColor(new Color(174,127,77));g.fill(section.shape);
        g.setStroke(new BasicStroke(.012f));g.setColor(new Color(0,0,0,70));for(int x=0;x<=9;x++) g.draw(new Line2D.Double(x,0,x,7));for(int z=0;z<=7;z++) g.draw(new Line2D.Double(0,z,9,z));
        g.dispose();java.io.File file=new java.io.File("build/reports/rounded-cob/contours.png");file.getParentFile().mkdirs();javax.imageio.ImageIO.write(report,"png",file);
    }
    @Test public void everyClippedProfileStaysInsideItsOwnerAndReassemblesTheSection() {
        for(Set<Long> cells:Arrays.asList(ring(9,7,1),ring(10,8,2),new HashSet<>(Arrays.asList(SmartCobGeometry.key(0,0))))) {
            SmartCobGeometry.Section section=SmartCobGeometry.solve(cells,new Area());Area combined=new Area();
            for(Map.Entry<Long,SmartCobProfile> entry:section.profiles.entrySet()) {
                SmartCobProfile p=entry.getValue();assertEquals(p,new SmartCobProfile(p.encode()));
                for(java.util.List<double[]> loop:p.loops()) for(double[] v:loop) assertTrue(v[0]>=0&&v[0]<=1&&v[1]>=0&&v[1]<=1);
                Area a=p.area();a.transform(AffineTransform.getTranslateInstance(SmartCobGeometry.x(entry.getKey()),SmartCobGeometry.z(entry.getKey())));combined.add(a);
                for(net.minecraft.util.math.AxisAlignedBB box:p.boxes()) assertTrue(box.minX>=0&&box.minZ>=0&&box.maxX<=1&&box.maxZ<=1);
            }
            // Quantisation is below 1/4096; compare occupancy away from the contour itself.
            for(double x=.04;x<11;x+=.13) for(double z=.04;z<9;z+=.13)
                if(!section.shape.intersects(x-.001,z-.001,.002,.002)||section.shape.contains(x-.001,z-.001,.002,.002))
                    assertEquals(section.shape.contains(x,z),combined.contains(x,z));
        }
    }
    @Test public void contactsStaySquareAndSteppedRunsNeverGrowIntoAir() {
        Set<Long> cells=ring(9,7,1);cells.remove(SmartCobGeometry.key(4,0));
        Area contacts=new Area(new Rectangle2D.Double(3,0,1,1));
        SmartCobGeometry.Section section=SmartCobGeometry.solve(cells,contacts);
        assertEquals("Contact patches must not become detached pillars",1,SmartCobGeometry.components(section.shape));
        assertTrue(section.shape.contains(3.01,.01));assertFalse(section.shape.contains(4.5,.5));
        Set<Long> steps=new HashSet<>();for(int i=0;i<6;i++) {steps.add(SmartCobGeometry.key(i,i));steps.add(SmartCobGeometry.key(i+1,i));}
        SmartCobGeometry.Section stair=SmartCobGeometry.solve(steps,new Area());Area added=new Area(stair.shape);added.subtract(SmartCobGeometry.footprint(steps));assertTrue(added.isEmpty());
        assertTrue(SmartCobGeometry.thickEnough(stair.shape));
    }
    @Test public void openingsDoNotRefitTheOutsideOfOtherCourses() {
        Set<Long> whole=ring(9,7,1),open=new HashSet<>(whole);open.remove(SmartCobGeometry.key(4,0));
        SmartCobGeometry.Section base=SmartCobGeometry.solve(whole,new Area()),course=SmartCobGeometry.slice(base,open);
        assertEquals(base.profiles.get(SmartCobGeometry.key(0,0)),course.profiles.get(SmartCobGeometry.key(0,0)));
        assertFalse(course.shape.contains(4.5,.5));
    }
    @Test public void frameContactsMustNotCancelRemoteBuildingCorners() throws Exception {
        Set<Long> cells=ring(9,7,1);Area contacts=new Area();
        for(int window:new int[]{1,4,6}) {
            contacts.add(new Area(new Rectangle2D.Double(window-.5,0,.5,1)));
            contacts.add(new Area(new Rectangle2D.Double(window+1,0,.5,1)));
        }
        for(long cell:cells)for(int[] d:new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
            int x=SmartCobGeometry.x(cell),z=SmartCobGeometry.z(cell),nx=x+d[0],nz=z+d[1];
            if(nx>=1&&nx<8&&nz>=1&&nz<6)contacts.add(new Area(new Rectangle2D.Double(x+(d[0]>0?.5:0),z+(d[1]>0?.5:0),d[0]==0?1:.5,d[1]==0?1:.5)));
        }
        SmartCobGeometry.Section section=SmartCobGeometry.solve(cells,contacts);
        assertFalse("A window on the front must not square off the rear corner",section.shape.contains(.08,6.92));
        assertFalse("A neighbouring window must leave room for a softened front corner",section.shape.contains(8.92,.08));
    }
    @Test public void steppedOvalHasContinuousDiagonalRunsInsteadOfRoundedStairTeeth() throws Exception {
        Set<Long> footprint=new HashSet<>(),walls=new HashSet<>();
        for(int x=0;x<15;x++)for(int z=0;z<11;z++)if(Math.pow((x+.5-7.5)/7.5,2)+Math.pow((z+.5-5.5)/5.5,2)<=1)footprint.add(SmartCobGeometry.key(x,z));
        Area contacts=new Area();
        for(long cell:footprint) {
            int x=SmartCobGeometry.x(cell),z=SmartCobGeometry.z(cell);boolean edge=false;
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(Math.abs(dx)+Math.abs(dz)<=2&&!footprint.contains(SmartCobGeometry.key(x+dx,z+dz)))edge=true;
            if(edge)walls.add(cell);
        }
        for(long cell:walls)for(int[] d:new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
            int x=SmartCobGeometry.x(cell),z=SmartCobGeometry.z(cell);long next=SmartCobGeometry.key(x+d[0],z+d[1]);
            if(footprint.contains(next)&&!walls.contains(next))contacts.add(new Area(new Rectangle2D.Double(x+(d[0]>0?.5:0),z+(d[1]>0?.5:0),d[0]==0?1:.5,d[1]==0?1:.5)));
        }
        SmartCobGeometry.Section section=SmartCobGeometry.solve(walls,contacts);
        assertFalse("Stepped outer shoulder must be replaced by a flowing inset",section.shape.contains(11.85,1.15));
        Area outside=new Area(section.shape);outside.subtract(SmartCobGeometry.footprint(walls));assertTrue(outside.isEmpty());
        BufferedImage report=new BufferedImage(900,680,BufferedImage.TYPE_INT_RGB);Graphics2D g=report.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,900,680);g.translate(25,25);g.scale(55,55);
        g.setColor(new Color(235,225,208));g.fill(SmartCobGeometry.footprint(walls));g.setColor(new Color(174,127,77));g.fill(section.shape);g.dispose();
        java.io.File file=new java.io.File("build/reports/rounded-cob/oval-contours.png");file.getParentFile().mkdirs();javax.imageio.ImageIO.write(report,"png",file);
    }
}
