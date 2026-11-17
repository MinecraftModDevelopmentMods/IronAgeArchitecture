package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Move a roof's anchor across a page boundary and retain deletion tombstones. */
final class SmartPersistenceProbe {
    static void verify(WorldServer world,boolean reload){
        if(!ArchitectureConfig.smartCob||!ArchitectureConfig.thatch)return;
        BlockPos roof=new BlockPos(-1,130,1025),cob=new BlockPos(-1,140,1030);
        for(int x=-2;x<=1;x++)for(int z=63;z<=65;z++)world.getChunkFromChunkCoords(x,z);
        SmartCobData walls=SmartCobData.get(world);
        if(!reload){
            world.setBlockState(roof,ArchitectureBlocks.SMART_THATCH.getDefaultState(),2);world.setBlockState(roof.east(),ArchitectureBlocks.SMART_THATCH.getDefaultState(),2);
            world.setBlockState(cob,ArchitectureBlocks.SMART_COB.getDefaultState(),2);world.setBlockState(cob.east(),ArchitectureBlocks.SMART_COB.getDefaultState(),2);ProbeShapes.flush(world);
            world.getPerWorldStorage().saveAllData();
            world.setBlockToAir(roof);world.setBlockToAir(cob);ProbeShapes.flush(world);
            world.getPerWorldStorage().saveAllData();
        }
        require(!SmartThatchBlock.owner(world.getBlockState(roof))&&SmartThatchBlock.owner(world.getBlockState(roof.east())),"roof block deletion survives reload without profiles");
        require(!walls.contains(cob)&&walls.contains(cob.east()),"removed cob does not return from the old page");
        require(ThatchShape.at(world,roof.east(),world.getBlockState(roof.east())).profile==ThatchShape.FLAT_PROFILE,"surviving roof is immediately available");
        WorldServer nether=world.getMinecraftServer().worldServerForDimension(-1);
        require(nether!=null&&!SmartCobData.get(nether).contains(cob.east())&&!SmartThatchBlock.owner(nether.getBlockState(roof.east())),"dimension pages stay separate");
        System.out.println("IAA_PERSISTENCE_PASS reload="+reload+" movedAnchor=true tombstones=true dimensions=true");
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Shape persistence: "+message);}
}
