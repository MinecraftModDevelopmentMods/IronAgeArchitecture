package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import java.security.MessageDigest;
import java.util.concurrent.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Snapshot ordinary roof states and saved wall descriptors; hashing never reads a world. */
final class MPFingerprint {
    static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"MP verification hashes");t.setDaemon(true);return t;});
    static final class Snapshot {final SortedMap<Long,Integer> roof=new TreeMap<>();final SortedMap<Long,SmartCobProfile> cob=new TreeMap<>();final SortedMap<Long,Integer> timber=new TreeMap<>();final SortedMap<Long,int[]> frames=new TreeMap<>();}
    static Snapshot capture(World world,int x,int z){Snapshot s=new Snapshot();for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){int cx=x+dx,cz=z+dz;if(!world.isBlockLoaded(new BlockPos(cx*16,64,cz*16)))continue;Chunk chunk=world.getChunkFromChunkCoords(cx,cz);for(ExtendedBlockStorage section:chunk.getBlockStorageArray())if(section!=null&&!section.isEmpty())for(int px=0;px<16;px++)for(int py=0;py<16;py++)for(int pz=0;pz<16;pz++){IBlockState state=section.get(px,py,pz);if(state.getBlock() instanceof zone.moddev.mc.ironagearchitecture.roof.RoofFrameBlock){BlockPos p=new BlockPos(cx*16+px,section.getYLocation()+py,cz*16+pz);zone.moddev.mc.ironagearchitecture.roof.RoofCell c=zone.moddev.mc.ironagearchitecture.roof.RoofData.get(world).at(world,p);if(c==null)s.frames.put(p.toLong(),new int[]{-1});else{int[] values=new int[c.timber.length+c.skin.length+3];values[0]=state.getValue(zone.moddev.mc.ironagearchitecture.roof.RoofFrameBlock.WOOD);values[1]=zone.moddev.mc.ironagearchitecture.roof.RoofFrameBlock.cover(state);values[2]=c.timber.length;System.arraycopy(c.timber,0,values,3,c.timber.length);System.arraycopy(c.skin,0,values,3+c.timber.length,c.skin.length);s.frames.put(p.toLong(),values);}}else if(SmartThatchBlock.owner(state)){BlockPos p=new BlockPos(cx*16+px,section.getYLocation()+py,cz*16+pz);s.roof.put(p.toLong(),ThatchShape.at(world,p,state).code);}else if(SmartThatchBlock.active(state)){BlockPos p=new BlockPos(cx*16+px,section.getYLocation()+py,cz*16+pz),above=SmartThatchBlock.parent(world,p);s.roof.put(p.toLong(),above==null?-1:65536+ThatchShape.at(world,above,world.getBlockState(above)).code);}}s.cob.putAll(SmartCobData.get(world).chunk(cx,cz));s.timber.putAll(SmartWallData.get(world).chunk(cx,cz));}return s;}
    static final class Result {final String hash;final int roof,cob,timber;Result(String h,Snapshot s){hash=h;roof=s.roof.size();cob=s.cob.size();timber=s.timber.size();}}
    static Future<Result> submit(Snapshot snapshot){return WORKER.submit(()->hash(snapshot));}
    private static void number(MessageDigest digest,long n){for(int i=7;i>=0;i--)digest.update((byte)(n>>>(i*8)));}
    private static Result hash(Snapshot s)throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");number(digest,s.roof.size());for(Map.Entry<Long,Integer> e:s.roof.entrySet()){number(digest,e.getKey());number(digest,e.getValue());}number(digest,s.cob.size());for(Map.Entry<Long,SmartCobProfile> e:s.cob.entrySet()){number(digest,e.getKey());int[] v=e.getValue().encode();number(digest,v.length);for(int n:v)number(digest,n);}number(digest,s.timber.size());for(Map.Entry<Long,Integer> e:s.timber.entrySet()){number(digest,e.getKey());number(digest,e.getValue());}number(digest,s.frames.size());for(Map.Entry<Long,int[]> e:s.frames.entrySet()){number(digest,e.getKey());number(digest,e.getValue().length);for(int v:e.getValue())number(digest,v);}StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format("%02x",b&255));return new Result(hex.toString(),s);}
}
