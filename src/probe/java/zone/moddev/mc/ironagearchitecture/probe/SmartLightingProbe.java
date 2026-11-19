package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.world.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import zone.moddev.mc.ironagearchitecture.block.SmartLighting;

/** Restore a temporary client light change and check both a new bake and live access. */
final class SmartLightingProbe {
    static void verify(World world,BlockPos center){
        BlockPos changed=center.west();int before=world.getLightFor(EnumSkyBlock.BLOCK,changed);
        try {
            ChunkCache old=new ChunkCache(world,center.add(-2,-2,-2),center.add(2,2,2),0);
            if(SmartLighting.packed(old,center)!=reference(old,center,false)||SmartLighting.cob(old,center)!=reference(old,center,true))throw new IllegalStateException("Initial mesh lighting differs");
            world.setLightFor(EnumSkyBlock.BLOCK,changed,15);
            ChunkCache fresh=new ChunkCache(world,center.add(-2,-2,-2),center.add(2,2,2),0);
            if(SmartLighting.packed(fresh,center)!=reference(fresh,center,false)||SmartLighting.cob(fresh,center)!=reference(fresh,center,true)||SmartLighting.packed(world,center)!=reference(world,center,false))throw new IllegalStateException("Lighting cache survived a new render context");
        } finally {world.setLightFor(EnumSkyBlock.BLOCK,changed,before);}
        if(SmartLighting.packed(world,center)!=reference(world,center,false))throw new IllegalStateException("Live lighting remained cached");
        System.out.println("IAA_LIGHT_CACHE_PASS live=true newBake=true restored=true");
    }
    private static int reference(IBlockAccess access,BlockPos center,boolean own){int light=own?access.getCombinedLight(center,0):0,sky=(light>>16)&255,block=light&255;for(EnumFacing side:EnumFacing.values()){int n=access.getCombinedLight(center.offset(side),0);sky=Math.max(sky,(n>>16)&255);block=Math.max(block,n&255);}return sky<<16|block;}
}
