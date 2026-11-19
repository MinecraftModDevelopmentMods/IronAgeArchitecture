package zone.moddev.mc.ironagearchitecture;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;
import zone.moddev.mc.ironagearchitecture.block.*;

public class SmartPerformanceTest {
    @Test public void oldExpandedCobMigratesToASharedPalette(){SmartCobData source=new SmartCobData();BlockPos p=new BlockPos(0,70,0);source.put(p,SmartCobProfile.SQUARE);source.put(p.east(),new SmartCobProfile(SmartCobProfile.SQUARE.encode()));NBTTagCompound saved=source.writeToNBT(new NBTTagCompound());assertEquals(1,saved.getTagList("profiles",10).tagCount());SmartCobData loaded=new SmartCobData();loaded.readFromNBT(saved);assertSame(loaded.profile(p),loaded.profile(p.east()));assertEquals(source.profile(p),loaded.profile(p));}

    @Test public void aFullCacheEvictsColdEntriesWithoutDroppingItsWorkingSet(){MeshCache<Integer,List<Integer>> cache=new MeshCache<>(8,List::size);for(int i=0;i<8;i++)cache.put(i,Collections.singletonList(i));cache.get(0);cache.put(8,Collections.singletonList(8));assertEquals(8,cache.size());assertNotNull(cache.get(0));assertNull(cache.get(1));assertNotNull(cache.get(7));}

    @Test public void unfinishedFacesSurviveReloadIndependently(){SmartShapeDirtyData data=new SmartShapeDirtyData();BlockPos p=new BlockPos(-1,90,16);data.add(SmartShapeDirtyData.TIMBER,p,15);data.add(SmartShapeDirtyData.COB,p,1);data.clear(SmartShapeDirtyData.TIMBER,p,1);SmartShapeDirtyData loaded=new SmartShapeDirtyData();loaded.readFromNBT(data.writeToNBT(new NBTTagCompound()));assertEquals(Integer.valueOf(14),loaded.chunk(SmartShapeDirtyData.TIMBER,-1,1).get(p.toLong()));assertEquals(Integer.valueOf(1),loaded.chunk(SmartShapeDirtyData.COB,-1,1).get(p.toLong()));loaded.clear(SmartShapeDirtyData.COB,p,1);assertTrue(loaded.chunk(SmartShapeDirtyData.COB,-1,1).isEmpty());assertFalse(loaded.chunk(SmartShapeDirtyData.TIMBER,-1,1).isEmpty());}

    @Test public void longRoofsYieldToOtherBuildingsWithoutUsingTheCallerThread() throws Exception {
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);AtomicBoolean otherRan=new AtomicBoolean(),interleaved=new AtomicBoolean();
        Future<Integer> roof=SmartShapeWorker.submitSteps(()->new SmartShapeWorker.Steps<Integer>(){int n;public boolean advance(){if(n==0){assertTrue(Thread.currentThread().getName().startsWith("Architecture shape worker"));started.countDown();try{release.await();}catch(InterruptedException stop){throw new RuntimeException(stop);}}long until=System.nanoTime()+100000;while(System.nanoTime()<until){}if(otherRan.get())interleaved.set(true);return ++n==500;}public Integer result(){return n;}});
        try{assertTrue(started.await(5,TimeUnit.SECONDS));Future<Boolean> other=SmartShapeWorker.submit(()->{otherRan.set(true);return true;});release.countDown();assertTrue(other.get(5,TimeUnit.SECONDS));assertEquals(Integer.valueOf(500),roof.get(5,TimeUnit.SECONDS));assertTrue(interleaved.get());}finally{release.countDown();roof.cancel(true);}
    }

}
