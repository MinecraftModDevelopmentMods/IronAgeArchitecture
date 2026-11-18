package zone.moddev.mc.ironagearchitecture;

import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.*;
import org.junit.Test;
import static org.junit.Assert.*;
import zone.moddev.mc.ironagearchitecture.roof.*;

public class RoofPlanTest {
    private static RoofPlan plan(int pitch,boolean axis,int width,int length){RoofPlan p=new RoofPlan();p.minX=-16;p.maxX=p.minX+width-1;p.minZ=-9;p.maxZ=p.minZ+length-1;p.floorY=64;p.pitch=pitch;p.alongX=axis;return RoofPlanner.solve(p);}
    @Test public void everyPitchAndRidgeDirectionMakesAUsablePlan(){for(int pitch=0;pitch<3;pitch++)for(boolean axis:new boolean[]{false,true})for(int width:new int[]{7,8}){RoofPlan p=plan(pitch,axis,width,11);assertTrue(p.error,p.valid());assertTrue(p.timber()>0);assertTrue(p.cells.size()<=1024);}}
    @Test public void cellsAndCoveringsStayInsideTheirOwnVoxel(){for(int pitch=0;pitch<3;pitch++)for(boolean axis:new boolean[]{false,true}){RoofPlan p=plan(pitch,axis,8,10);assertTrue(p.error,p.valid());for(RoofCell c:p.cells.values())for(int cover=0;cover<4;cover++){RoofMeshes.Mesh m=RoofMeshes.get(c,cover);for(RoofMeshes.Face f:m.faces){double n=0;for(double a:f.normal)n+=a*a;assertEquals(1,n,1e-7);for(double[] v:f.vertices)for(double a:v)assertTrue("vertex "+a,a>=-1e-8&&a<=1+1e-8);}for(AxisAlignedBB b:m.boxes)assertTrue(b.minX>=0&&b.minY>=0&&b.minZ>=0&&b.maxX<=1&&b.maxY<=1&&b.maxZ<=1);}}}
    @Test public void repeatedHousesShareMeshObjects(){RoofPlan a=plan(1,false,8,11),b=plan(1,false,8,11);assertNotEquals(a.id,b.id);Iterator<RoofCell> x=a.cells.values().iterator(),y=b.cells.values().iterator();while(x.hasNext())assertSame(RoofMeshes.get(x.next(),1),RoofMeshes.get(y.next(),1));}
    @Test public void floorHolesAreSeparateFromRoofOpenings(){RoofPlan p=plan(1,false,8,11);int before=p.cells.size();p.openings.add(RoofPlan.column(-13,-5));RoofPlanner.solve(p);assertTrue(p.error,p.valid());for(long key:p.cells.keySet()){BlockPos at=BlockPos.fromLong(key);assertFalse(at.getX()==-13&&at.getZ()==-5);}assertTrue(p.cells.size()<before);}
    @Test public void savedDrawingIsExactAfterReload(){RoofPlan p=plan(2,true,7,9);p.wood=5;RoofPlan q=RoofPlan.read(p.write(new NBTTagCompound()));assertEquals(p.cells,q.cells);assertEquals(p.id,q.id);assertEquals(p.wood,q.wood);assertEquals(p.cells,RoofPlanner.solve(q).cells);}
    @Test public void everyCoveringSectionHasATimberOwner(){for(int pitch=0;pitch<3;pitch++){RoofPlan p=plan(pitch,false,8,11);assertTrue(p.error,p.valid());for(Map.Entry<Long,RoofCell> e:p.cells.entrySet()){RoofCell c=e.getValue();RoofCell owner=p.cells.get(c.owner.toLong());assertNotNull(owner);assertTrue(owner.timber());assertEquals(BlockPos.fromLong(e.getKey()).getX(),c.owner.getX());assertEquals(BlockPos.fromLong(e.getKey()).getZ(),c.owner.getZ());}}}
    @Test public void invalidAndOversizedPlansProduceNoBuildableDrawing(){RoofPlan p=plan(2,false,32,32);assertFalse(p.valid());assertFalse(p.error.isEmpty());p=plan(1,false,3,8);assertFalse(p.valid());p=plan(1,false,33,8);assertFalse(p.valid());}
    @Test public void raisedBaseAddsSupportingTimber(){RoofPlan p=plan(1,false,8,11);p.base=4;RoofPlanner.solve(p);assertTrue(p.error,p.valid());boolean post=false;for(Map.Entry<Long,RoofCell> e:p.cells.entrySet())if(BlockPos.fromLong(e.getKey()).getY()==65&&e.getValue().timber())post=true;assertTrue(post);}
    @Test public void targetingHitsTheActualSurface(){RoofPlan p=plan(1,false,8,11);assertTrue(p.error,p.valid());boolean hit=false;for(Map.Entry<Long,RoofCell> e:p.cells.entrySet()){BlockPos at=BlockPos.fromLong(e.getKey());RayTraceResult r=RoofMeshes.get(e.getValue(),1).trace(at,new Vec3d(at.getX()+.37,at.getY()+2,at.getZ()+.43),new Vec3d(at.getX()+.37,at.getY()-1,at.getZ()+.43));if(r!=null){hit=true;assertTrue(r.hitVec.yCoord>=at.getY()-1e-8&&r.hitVec.yCoord<=at.getY()+1+1e-8);}}assertTrue(hit);}
    @Test public void fiveHundredDrawingsDoNotCreateFiveHundredMeshKits(){long start=System.nanoTime();for(int i=0;i<500;i++){RoofPlan p=plan(i%3,i%2==0,8,11);assertTrue(p.error,p.valid());for(RoofCell c:p.cells.values())RoofMeshes.get(c,1);}System.out.println("ROOF_500_DRAWINGS_MS="+(System.nanoTime()-start)/1e6+" SHARED_KIT="+RoofMeshes.cached());assertTrue(RoofMeshes.cached()<4096);}
}
