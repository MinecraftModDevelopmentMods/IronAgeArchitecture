package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;

/** Ordinary edits repair at most one local neighbourhood. Undersides always belong to the cell above. */
public final class SmartThatchEvents {
    private static final String CLEANUP="ironagearchitecture_thatch_skin";
    private static final Set<Chunk> CLEANED=Collections.newSetFromMap(new WeakHashMap<Chunk,Boolean>());
    private static final ThreadLocal<Set<World>> BUSY=ThreadLocal.withInitial(()->Collections.newSetFromMap(new IdentityHashMap<World,Boolean>()));
    public static boolean applying(World world){return BUSY.get().contains(world);}
    public static boolean pending(World world){return false;}
    public static boolean retrying(World world){return false;}
    public static int flush(World world){return 0;}
    public static void changed(World world,BlockPos pos){world.markBlockRangeForRenderUpdate(pos.add(-1,-2,-1),pos.add(1,1,1));}
    public static void edited(World world,BlockPos pos){changed(world,pos);if(applying(world)||ArchitectureBlocks.SMART_THATCH_PART==null)return;BUSY.get().add(world);try{for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){BlockPos at=pos.add(dx,dy,dz);if(!world.isBlockLoaded(at))continue;IBlockState s=world.getBlockState(at);if(SmartThatchBlock.owner(s))fit(world,at,s);else if(s.getBlock()==ArchitectureBlocks.SMART_THATCH_PART&&(!SmartThatchBlock.active(s)||SmartThatchBlock.parent(world,at)==null))world.setBlockToAir(at);}}finally{BUSY.get().remove(world);}}
    public static void removeBelow(World world,BlockPos owner){BlockPos below=owner.down();if(world.getBlockState(below).getBlock()!=ArchitectureBlocks.SMART_THATCH_PART)return;boolean busy=BUSY.get().add(world);try{world.setBlockToAir(below);}finally{if(busy)BUSY.get().remove(world);}}
    private static void fit(World world,BlockPos owner,IBlockState state){if(owner.getY()<=0)return;BlockPos below=owner.down();IBlockState old=world.getBlockState(below);boolean part=old.getBlock()==ArchitectureBlocks.SMART_THATCH_PART;ThatchGeometry.Mesh mesh=ThatchGeometry.underside(ThatchShape.at(world,owner,state));if(mesh==ThatchGeometry.EMPTY){if(part)world.setBlockToAir(below);return;}if(!part&&old.getBlock()!=Blocks.AIR)return;if(part&&SmartThatchBlock.active(old))return;for(net.minecraft.entity.Entity entity:world.getEntitiesWithinAABB(net.minecraft.entity.Entity.class,mesh.bounds.offset(below))){if(entity.isDead||entity.noClip||!entity.canBeCollidedWith())continue;for(net.minecraft.util.math.AxisAlignedBB box:mesh.boxes)if(box.offset(below).intersectsWith(entity.getEntityBoundingBox())){if(!world.isRemote)world.scheduleUpdate(owner,state.getBlock(),10);return;}}world.setBlockState(below,ArchitectureBlocks.SMART_THATCH_PART.getStateFromMeta(8),2);}
    @SubscribeEvent public void read(ChunkDataEvent.Load event){if(event.getData().getCompoundTag("Level").getBoolean(CLEANUP))CLEANED.add(event.getChunk());}
    @SubscribeEvent public void write(ChunkDataEvent.Save event){if(CLEANED.contains(event.getChunk()))event.getData().getCompoundTag("Level").setBoolean(CLEANUP,true);}
    @SubscribeEvent public void chunkLoaded(ChunkEvent.Load event){
        if(!(event.getWorld() instanceof WorldServer)||ArchitectureBlocks.SMART_THATCH_PART==null)return;WorldServer world=(WorldServer)event.getWorld();Chunk chunk=event.getChunk();
        queueBorders(world,chunk);if(CLEANED.contains(chunk))return;
        SmartChunkWork.enqueue(world,chunk,"thatch-skin",()->{BUSY.get().add(world);try{List<BlockPos> owners=new ArrayList<>();for(ExtendedBlockStorage section:chunk.getBlockStorageArray())if(section!=null)for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++){IBlockState state=section.get(x,y,z);BlockPos at=null;if(SmartThatchBlock.owner(state)){at=new BlockPos(chunk.xPosition*16+x,section.getYLocation()+y,chunk.zPosition*16+z);owners.add(at);}else if(state.getBlock()==ArchitectureBlocks.SMART_THATCH_PART){at=new BlockPos(chunk.xPosition*16+x,section.getYLocation()+y,chunk.zPosition*16+z);if(!SmartThatchBlock.active(state)||SmartThatchBlock.parent(world,at)==null)world.setBlockToAir(at);}}for(BlockPos at:owners)fit(world,at,world.getBlockState(at));CLEANED.add(chunk);}finally{BUSY.get().remove(world);}});
    }

    private static void queueBorders(WorldServer world,Chunk centre){for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){BlockPos at=new BlockPos((centre.xPosition+dx)*16,64,(centre.zPosition+dz)*16);if(!world.isBlockLoaded(at))continue;Chunk chunk=world.getChunkFromBlockCoords(at);SmartChunkWork.enqueue(world,chunk,"thatch-border",()->{BUSY.get().add(world);try{for(ExtendedBlockStorage section:chunk.getBlockStorageArray())if(section!=null&&!section.isEmpty())for(int x=0;x<16;x++)for(int z=0;z<16;z++){if(x!=0&&x!=15&&z!=0&&z!=15)continue;for(int y=0;y<16;y++){IBlockState state=section.get(x,y,z);if(SmartThatchBlock.owner(state))fit(world,new BlockPos(chunk.xPosition*16+x,section.getYLocation()+y,chunk.zPosition*16+z),state);}}}finally{BUSY.get().remove(world);}});}}
}
