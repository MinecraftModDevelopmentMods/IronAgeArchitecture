package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import zone.moddev.mc.ironagearchitecture.block.*;

/** A copy of the completed server benchmark, dressed with a level road for native views. */
final class CityClientScene {
    static void prepare(WorldServer world) {
        int houses=Integer.getInteger("iaa.performanceHouses",64),across=(int)Math.ceil(Math.sqrt(houses));
        for(int x=24;x<32+across*16;x++)for(int z=24;z<32+across*16;z++)world.setBlockState(new BlockPos(x,69,z),Blocks.STONEBRICK.getDefaultState(),0);
        int walls=0,roofs=0;
        for(int i=0;i<houses;i++){int sx=32+(i%across)*16,sz=32+(i/across)*16;for(int x=-1;x<=9;x++)for(int z=-1;z<=7;z++)for(int y=70;y<82;y++){BlockPos at=new BlockPos(sx+x,y,sz+z);if(world.getBlockState(at).getBlock() instanceof SmartCobBlock)walls++;if(SmartThatchBlock.owner(world.getBlockState(at)))roofs++;}}
        if(walls!=houses*136||roofs!=houses*99)throw new IllegalStateException("City source is incomplete: "+walls+" / "+roofs);
        ProbeShapes.flush(world);world.setWorldTime(1000);world.getGameRules().setOrCreateGameRule("doDaylightCycle","false");world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("doFireTick","false");world.setSpawnPoint(new BlockPos(32+across*8,84,32+across*8));
        System.out.println("IAA_CITY_READY houses="+houses+" cob="+walls+" thatch="+roofs);
    }
}
