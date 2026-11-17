package zone.moddev.mc.ironagearchitecture.block;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;

/** A render rebuild owns its ChunkCache. Live world lighting is never cached. */
public final class SmartLighting {
    private static final EnumFacing[] SIDES=EnumFacing.values();
    private static final class Cache {
        WeakReference<IBlockAccess> access=new WeakReference<>(null);
        final Map<Long,Integer> values=new HashMap<>();
    }
    private static final ThreadLocal<Cache> LOCAL=ThreadLocal.withInitial(Cache::new);
    private SmartLighting() { }
    public static int cob(IBlockAccess access,BlockPos pos){int near=packed(access,pos),own=access.getCombinedLight(pos,0);return Math.max((near>>16)&255,(own>>16)&255)<<16|Math.max(near&255,own&255);}
    public static int packed(IBlockAccess access,BlockPos pos){
        Cache cache=null;
        if(access instanceof ChunkCache){cache=LOCAL.get();if(cache.access.get()!=access){cache.access=new WeakReference<>(access);cache.values.clear();}Integer light=cache.values.get(pos.toLong());if(light!=null)return light;}
        int sky=0,block=0;BlockPos.MutableBlockPos sample=new BlockPos.MutableBlockPos();
        for(EnumFacing side:SIDES){sample.setPos(pos.getX()+side.getFrontOffsetX(),pos.getY()+side.getFrontOffsetY(),pos.getZ()+side.getFrontOffsetZ());int light=access.getCombinedLight(sample,0);sky=Math.max(sky,(light>>16)&255);block=Math.max(block,light&255);}
        int result=sky<<16|block;if(cache!=null){if(cache.values.size()>=8192)cache.values.clear();cache.values.put(pos.toLong(),result);}return result;
    }
}
