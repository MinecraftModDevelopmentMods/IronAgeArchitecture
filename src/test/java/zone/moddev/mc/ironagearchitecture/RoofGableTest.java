package zone.moddev.mc.ironagearchitecture;

import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.*;
import org.junit.Test;
import static org.junit.Assert.*;
import zone.moddev.mc.ironagearchitecture.roof.*;

public class RoofGableTest {
    private RoofPlan plan(boolean axis,int pitch){RoofPlan p=new RoofPlan();p.minX=0;p.maxX=7;p.minZ=0;p.maxZ=8;p.floorY=64;p.alongX=axis;p.pitch=pitch;return p;}
    private Vec3d point(RoofPlan p,double u,double y,double v){return p.alongX?new Vec3d(v,y,u):new Vec3d(u,y,v);}
    private void closed(RoofPlan p){
        RoofPlanner.solve(p);assertTrue(p.error,p.valid());
        double first=(p.alongX?p.minZ:p.minX)-p.wallInset,last=(p.alongX?p.maxZ:p.maxX)+1+p.wallInset;
        double start=(p.alongX?p.minX:p.minZ)-p.wallInset,end=(p.alongX?p.maxX:p.maxZ)+1+p.wallInset;
        for(int side=0;side<2;side++)for(double u=first+.125;u<last;u+=.25){
            double surface=RoofDesignPlanner.surface(p,u);
            for(double y:new double[]{p.floorY+1+.125,surface-.015625}){
                if(y>surface)continue;
                double v=side==0?start:end-1;Vec3d a=point(p,u,y,side==0?start-.25:end+.25),b=point(p,u,y,v+.5);
                BlockPos cell=new BlockPos(point(p,u,y,v+.5));RoofCell c=p.cells.get(cell.toLong());assertNotNull("missing gable cell "+cell,c);
                RayTraceResult hit=RoofMeshes.get(c,0).trace(cell,a,b);assertNotNull("gap at "+a+" -> "+b,hit);
                RoofPart touched=c.parts.get((Integer)hit.hitInfo);assertTrue(RoofGables.part(touched)||touched.kind==RoofPart.TIMBER);
            }
        }
    }
    @Test public void bothEndsCloseUpToEveryPitchAndMansardProfile(){for(boolean axis:new boolean[]{false,true})for(int pitch=0;pitch<3;pitch++)for(int mansard=0;mansard<4;mansard++){RoofPlan p=plan(axis,pitch);p.mansard=mansard;closed(p);}}
    @Test public void insetAndRaisedRoofsKeepTheirGablesContinuous(){for(int inset=0;inset<2;inset++)for(int base:new int[]{0,3,8}){RoofPlan p=plan(true,1);p.wallInset=inset;p.base=base;closed(p);}}
    @Test public void daubFramingFitsTheRoofWithNoGapAtItsPosts(){for(int wood=0;wood<6;wood++){RoofPlan p=plan(false,1);p.wood=wood;p.gableFill[0]=p.gableFill[1]=5;closed(p);boolean timber=false;for(RoofCell c:p.cells.values())for(RoofPart part:c.parts)if(RoofGables.part(part)&&part.kind==RoofPart.TIMBER){timber=true;assertEquals(wood,part.material);}assertTrue(timber);}}
    @Test public void endsCanHaveDifferentMaterialsOrStayOpen(){RoofPlan p=plan(false,1);p.gableFill[0]=0;p.gableFill[1]=6;RoofPlanner.solve(p);assertTrue(p.error,p.valid());for(RoofCell c:p.cells.values())for(RoofPart part:c.parts)if(RoofGables.part(part)){assertTrue(part.id.startsWith("gable:1:"));assertEquals(RoofPart.WALL,part.kind);assertEquals(4,part.material);}}
    @Test public void oldDrawingsDoNotAcquireInfillUntilSelected(){RoofPlan p=plan(true,1);Arrays.fill(p.gableFill,0);RoofPlanner.solve(p);NBTTagCompound n=p.write(new NBTTagCompound());n.removeTag("infill");RoofPlan restored=RoofPlan.read(n);assertArrayEquals(new int[]{0,0},restored.gableFill);assertEquals(p.cells,RoofPlanner.solve(restored).cells);}
    @Test public void settingsMaterialsAndProfilesSurviveReload(){RoofPlan p=plan(false,1);p.gableFill[0]=1;p.gableMaterial[0]=3;p.gableWood[0]=5;p.gableFill[1]=2;RoofPlanner.solve(p);RoofPlan copy=RoofPlan.read(p.write(new NBTTagCompound()));assertArrayEquals(p.gableFill,copy.gableFill);assertArrayEquals(p.gableMaterial,copy.gableMaterial);assertArrayEquals(p.gableWood,copy.gableWood);assertEquals(p.cells,RoofPlanner.solve(copy).cells);}
    @Test public void panelsStayInTheirWallCourseDespiteOverhangs(){RoofPlan p=plan(true,2);Arrays.fill(p.overhang,4);RoofPlanner.solve(p);assertTrue(p.error,p.valid());Set<String> paid=new HashSet<>();for(Map.Entry<Long,RoofCell> e:p.cells.entrySet())for(RoofPart part:e.getValue().parts)if(RoofGables.part(part)){BlockPos at=BlockPos.fromLong(e.getKey());assertTrue(at.getX()==0||at.getX()==7);assertTrue(at.getZ()>=0&&at.getZ()<=8);assertEquals(at,part.root);assertTrue(paid.add(part.id));RoofCell single=new RoofCell(p.id,at,Collections.singletonList(part),Collections.emptyList());for(RoofMeshes.Face f:RoofMeshes.get(single,0).faces)for(double[] v:f.vertices)for(int i=0;i<3;i++)assertTrue(v[i]>=-1e-8&&v[i]<=1+1e-8);}assertFalse(paid.isEmpty());}
    @Test public void cobPlasterCoversTheEndCollars(){RoofPlan p=plan(false,1);p.frame=1;RoofPlanner.solve(p);double y=p.floorY+1+p.clearance*.5+.0625;BlockPos at=new BlockPos(3,y,0);RoofCell c=p.cells.get(at.toLong());RayTraceResult hit=RoofMeshes.get(c,0).trace(at,new Vec3d(3.5,y,-1),new Vec3d(3.5,y,.5));assertNotNull(hit);assertEquals(RoofPart.WALL,c.parts.get((Integer)hit.hitInfo).kind);}
    @Test public void invalidControlsAreRejected(){RoofPlan p=plan(false,1);p.gableFill[0]=7;assertFalse(RoofPlanner.solve(p).valid());}
}
