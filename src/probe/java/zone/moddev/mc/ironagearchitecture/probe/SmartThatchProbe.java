package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.*;
import net.minecraft.item.*;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Finite roof states and ordinary block interaction; no profile drains or roof packets. */
final class SmartThatchProbe {
    static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Simple thatch: "+why);}
    static void flush(WorldServer world){ProbeShapes.flush(world);}
    static void verify(WorldServer world,boolean reload){
        if(!ArchitectureConfig.thatch){require(ArchitectureBlocks.SMART_THATCH==null,"disabled registration");return;}
        SmartThatchBlock block=ArchitectureBlocks.SMART_THATCH,part=ArchitectureBlocks.SMART_THATCH_PART;BlockPos origin=new BlockPos(15,110,256);
        if(reload){require(SmartThatchBlock.owner(world.getBlockState(origin.add(3,3,3))),"saved material");require(shape(world,origin.add(3,3,3))==ThatchShape.of(ThatchForm.RIDGE,EnumFacing.NORTH),"saved roof derives its ridge immediately");unusedProfiles(world);System.out.println("IAA_THATCH_RELOAD_PASS finiteStates=true noRoofData=true");return;}
        for(int x=-1;x<=3;x++)for(int z=-1;z<=3;z++)world.getChunkFromChunkCoords((origin.getX()>>4)+x,(origin.getZ()>>4)+z);
        for(int m=0;m<16;m++){require(block.getMetaFromState(block.getStateFromMeta(m))==(m&3),"facing metadata round trip");require(part.getMetaFromState(part.getStateFromMeta(m))==(m&8),"underside marker metadata");}
        for(int x=0;x<7;x++)for(int z=0;z<7;z++)world.setBlockState(origin.add(x,Math.min(x,6-x),z),block.getDefaultState(),3);
        require(shape(world,origin.add(3,3,3))==ThatchShape.of(ThatchForm.RIDGE,EnumFacing.NORTH),"ridge from opposing lower courses");require(shape(world,origin.add(1,1,3))==ThatchShape.of(ThatchForm.SLOPE,EnumFacing.EAST),"straight slope");
        BlockPos hip=origin.east(16);for(int x=0;x<7;x++)for(int z=0;z<7;z++)world.setBlockState(hip.add(x,Math.min(Math.min(x,6-x),Math.min(z,6-z)),z),block.getDefaultState(),2);
        require(shape(world,hip).profile!=ThatchShape.FLAT_PROFILE,"diagonal corner");require(shape(world,hip.add(3,3,3))==ThatchShape.of(ThatchForm.CAP,EnumFacing.NORTH),"hip cap");
        BlockPos valley=origin.east(32);for(int x=0;x<7;x++)for(int z=0;z<3;z++)world.setBlockState(valley.add(x,Math.abs(x-3),z),block.getDefaultState(),2);require(shape(world,valley.add(3,0,1))==ThatchShape.of(ThatchForm.VALLEY_RUN,EnumFacing.NORTH),"valley between raised runs");
        BlockPos edit=origin.add(1,1,3);ThatchShape before=shape(world,edit);world.setBlockState(edit.north().up(),block.getDefaultState(),2);ThatchShape after=shape(world,edit);require(before!=after,"neighbour changes are available in the same tick");world.setBlockToAir(edit.north().up());
        BlockPos far=new BlockPos(160000,100,160000);require(!world.isBlockLoaded(far),"unloaded control");ThatchShape.at(world,far,block.getDefaultState());require(!world.isBlockLoaded(far),"selection does not force chunks");
        FakePlayer player=FakePlayerFactory.getMinecraft(world);player.setPosition(0,100,240);player.capabilities.isCreativeMode=false;
        BlockPos placed=origin.add(0,20,16);world.setBlockState(placed.down(),Blocks.STONE.getDefaultState(),2);ItemStack stack=new ItemStack(block,3);player.setHeldItem(EnumHand.MAIN_HAND,stack);
        EntityArmorStand entity=new EntityArmorStand(world,placed.getX()+.5,placed.getY(),placed.getZ()+.5);entity.setEntityBoundingBox(new AxisAlignedBB(placed.getX()+.45,placed.getY()+.1,placed.getZ()+.45,placed.getX()+.55,placed.getY()+.2,placed.getZ()+.55));require(world.spawnEntity(entity),"entity fixture spawned");entity.setEntityBoundingBox(new AxisAlignedBB(placed.getX()+.45,placed.getY()+.1,placed.getZ()+.45,placed.getX()+.55,placed.getY()+.2,placed.getZ()+.55));require(entity.canBeCollidedWith()&&!world.getEntitiesWithinAABB(net.minecraft.entity.Entity.class,new AxisAlignedBB(placed)).isEmpty(),"entity fixture occupies target cell");
        stack.getItem().onItemUse(stack,player,world,placed.down(),EnumHand.MAIN_HAND,EnumFacing.UP,.5F,1,.5F);require(!SmartThatchBlock.owner(world.getBlockState(placed))&&stack.stackSize==3,"occupied geometry rejects placement without consuming material");
        entity.setEntityBoundingBox(new AxisAlignedBB(placed.getX()+.45,placed.getY()+.6,placed.getZ()+.45,placed.getX()+.55,placed.getY()+.8,placed.getZ()+.55));
        require(stack.getItem().onItemUse(stack,player,world,placed.down(),EnumHand.MAIN_HAND,EnumFacing.UP,.5F,1,.5F)==EnumActionResult.SUCCESS&&stack.stackSize==2,"clear space above the slab permits placement");entity.setDead();world.removeEntityDangerously(entity);
        require(world.getTileEntity(placed)==null,"no tile entity");List<ItemStack> drops=block.getDrops(world,placed,world.getBlockState(placed),0);require(drops.size()==1&&drops.get(0).stackSize==1&&drops.get(0).getMetadata()==0,"one canonical material drop");
        BlockPos old=far.east(32);world.getChunkFromBlockCoords(old);world.setBlockState(old,part.getDefaultState(),2);world.setBlockState(old.east(),block.getDefaultState(),2);world.setBlockState(old.west(),Blocks.STONE.getDefaultState(),2);require(part.isReplaceable(world,old)&&part.isAir(part.getDefaultState(),world,old),"retired helpers are inert and replaceable");flush(world);require(world.getBlockState(old).getBlock()==Blocks.AIR&&SmartThatchBlock.owner(world.getBlockState(old.east()))&&world.getBlockState(old.west()).getBlock()==Blocks.STONE,"cleanup removes only legacy helpers");
        undersides(world,player,origin.east(96));unusedProfiles(world);System.out.println("IAA_SMART_THATCH_PASS finiteStates=224 metadata=4 immediate=true localUndersides=true roofPackets=0 geometryData=0 legacyCleanup=true");
    }

    static void undersides(WorldServer world,FakePlayer player,BlockPos start){
        if(player.connection==null)player.connection=new net.minecraft.network.NetHandlerPlayServer(world.getMinecraftServer(),new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.SERVERBOUND),player);
        for(EnumFacing facing:EnumFacing.HORIZONTALS){BlockPos p=start.add(facing.getHorizontalIndex()*12,0,0),low=p.offset(facing.getOpposite()).down(),high=p.offset(facing).up();world.getChunkFromBlockCoords(p);SmartThatchBlock b=ArchitectureBlocks.SMART_THATCH,part=ArchitectureBlocks.SMART_THATCH_PART;world.setBlockState(low,b.getDefaultState(),3);world.setBlockState(high,b.getDefaultState(),3);world.setBlockState(p,b.getDefaultState(),3);BlockPos under=p.down();require(SmartThatchBlock.active(world.getBlockState(under)),"local underside created "+facing);require(part.getMetaFromState(world.getBlockState(under))==8,"underside metadata marker");require(part.getPickBlock(world.getBlockState(under),null,world,under,player).getItem()==Item.getItemFromBlock(b),"picking underside returns the roof item");require(part.getDrops(world,under,world.getBlockState(under),0).isEmpty(),"underside has no independent drops");
            double[] point=RoofGeometry.rotate(.37,0,.8,facing);Vec3d a=new Vec3d(p.getX()+point[0],p.getY()-1,p.getZ()+point[2]),z=new Vec3d(a.xCoord,p.getY()+1,a.zCoord);net.minecraft.util.math.RayTraceResult hit=world.rayTraceBlocks(a,z,false,false,false);require(hit!=null&&hit.getBlockPos().equals(under),"vanilla ray targets the underside "+facing);
            boolean drops=world.getGameRules().getBoolean("doTileDrops");world.getGameRules().setOrCreateGameRule("doTileDrops","true");player.interactionManager.setGameType(net.minecraft.world.GameType.SURVIVAL);player.setHeldItem(EnumHand.MAIN_HAND,new ItemStack(net.minecraft.init.Items.IRON_AXE));int before=items(world,p);require(player.interactionManager.tryHarvestBlock(under),"survival mining through underside");require(!SmartThatchBlock.owner(world.getBlockState(p))&&!SmartThatchBlock.active(world.getBlockState(under))&&items(world,p)==before+1,"one drop and both cells removed");
            world.setBlockState(p,b.getDefaultState(),3);world.setBlockState(under,Blocks.STONE.getDefaultState(),3);require(SmartThatchBlock.owner(world.getBlockState(p))&&world.getBlockState(under).getBlock()==Blocks.STONE,"placing support preserves the roof");world.setBlockToAir(under);require(SmartThatchBlock.active(world.getBlockState(under)),"removing support refits underside");before=items(world,p);part.onBlockExploded(world,under,new net.minecraft.world.Explosion(world,null,p.getX(),p.getY(),p.getZ(),1,false,true));require(!SmartThatchBlock.owner(world.getBlockState(p))&&items(world,p)==before+1,"exploding underside drops its owner once");world.setBlockState(p,b.getDefaultState(),3);world.setBlockState(under,Blocks.FIRE.getDefaultState(),3);require(!SmartThatchBlock.owner(world.getBlockState(p))&&world.getBlockState(under).getBlock()==Blocks.FIRE,"burning underside removes roof without deleting the fire");world.setBlockToAir(under);double[] q=RoofGeometry.rotate(.37,0,.8,facing);EntityArmorStand blocker=new EntityArmorStand(world,p.getX()+q[0],p.getY()-.1,p.getZ()+q[2]);require(world.spawnEntity(blocker),"underside blocker spawned");blocker.setEntityBoundingBox(new AxisAlignedBB(p.getX()+q[0]-.03,p.getY()-.15,p.getZ()+q[2]-.03,p.getX()+q[0]+.03,p.getY()-.05,p.getZ()+q[2]+.03));world.setBlockState(p,b.getDefaultState(),3);require(!SmartThatchBlock.active(world.getBlockState(under)),"entity prevents underside expansion");blocker.setDead();world.removeEntityDangerously(blocker);b.updateTick(world,p,world.getBlockState(p),world.rand);require(SmartThatchBlock.active(world.getBlockState(under)),"deferred underside fits when entity clears");world.setBlockToAir(p);world.getGameRules().setOrCreateGameRule("doTileDrops",Boolean.toString(drops));
        }System.out.println("IAA_THATCH_UNDERSIDE_PASS facings=4 ray=true pick=true survivalDrops=true replacement=true fire=true explosion=true entityClearance=true");
    }
    static int items(WorldServer world,BlockPos p){int count=0;for(EntityItem item:world.getEntitiesWithinAABB(EntityItem.class,new AxisAlignedBB(p).expand(2,2,2)))if(item.getEntityItem()!=null&&item.getEntityItem().getItem()==Item.getItemFromBlock(ArchitectureBlocks.SMART_THATCH))count+=item.getEntityItem().stackSize;return count;}
    static ThatchShape shape(WorldServer world,BlockPos pos){return ThatchShape.at(world,pos,world.getBlockState(pos));}
    static void unusedProfiles(WorldServer world){Map<?,?> loaded=ReflectionHelper.getPrivateValue(MapStorage.class,world.getPerWorldStorage(),"loadedDataMap","field_75749_b");for(Object key:loaded.keySet())require(!key.toString().startsWith("ironagearchitecture_smart_thatch"),"old roof profile data must not load");for(String name:new String[]{"zone.moddev.mc.ironagearchitecture.SmartThatchNetwork","zone.moddev.mc.ironagearchitecture.block.SmartThatchGeometry","zone.moddev.mc.ironagearchitecture.block.SmartThatchData"})try{Class.forName(name);throw new IllegalStateException("Rounded roof engine is still present: "+name);}catch(ClassNotFoundException expected){}}
}
