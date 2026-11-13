package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.*;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;

final class SmartCobProbe {
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Smart cob: "+message);}
    static void verify(WorldServer world,boolean reload) {
        if(!ArchitectureConfig.smartCob){require(ArchitectureBlocks.SMART_COB==null,"disabled registration");return;}
        SmartCobBlock block=ArchitectureBlocks.SMART_COB;int ids=0;for(Block b:ArchitectureBlocks.REGISTERED)if(b instanceof SmartCobBlock)ids++;require(ids==1,"one block ID");
        for(int meta=0;meta<16;meta++)require(block.getMetaFromState(block.getStateFromMeta(meta))==(meta&1),"finish-only metadata");
        BlockPos origin=new BlockPos(15,90,192);SmartCobData data=SmartCobData.get(world);
        if(reload) {require(world.getBlockState(origin.up()).getBlock()==block&&data.contains(origin.up())&&!data.profile(origin.up()).equals(SmartCobProfile.SQUARE),"saved curved corner");System.out.println("IAA_COB_RELOAD_PASS savedProfile=true");return;}
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)world.getChunkFromChunkCoords((origin.getX()>>4)+x,(origin.getZ()>>4)+z);
        for(int x=0;x<8;x++)for(int z=0;z<6;z++)if(x==0||x==7||z==0||z==5)for(int y=0;y<4;y++)world.setBlockState(origin.add(x,y,z),block.getStateFromMeta((x+y)&1),2);
        flush(world);SmartCobProfile before=data.profile(origin.up());require(!before.equals(SmartCobProfile.SQUARE),"rounded corner");
        require(!before.area().contains(.01,.01),"removed corner is air");
        require(block.collisionRayTrace(world.getBlockState(origin.up()),world,origin.up(),new Vec3d(15.01,94,192.01),new Vec3d(15.01,89,192.01))==null,"no invisible square collision target");
        require(block.collisionRayTrace(world.getBlockState(origin.up()),world,origin.up(),new Vec3d(15.5,91.5,190),new Vec3d(15.5,91.5,194))!=null,"curved wall targeting");
        require(world.getTileEntity(origin.up())==null,"no wall tile entity");
        world.setBlockState(origin.up(),block.getStateFromMeta(1-world.getBlockState(origin.up()).getValue(SmartCobBlock.FINISH)),3);flush(world);require(before.equals(data.profile(origin.up())),"finish changes preserve shape");
        NBTTagCompound nbt=data.writeToNBT(new NBTTagCompound());SmartCobData restored=new SmartCobData();restored.readFromNBT(nbt);require(before.equals(restored.profile(origin.up())),"NBT contour round trip");
        SmartCobNetwork.Packet packet=new SmartCobNetwork.Packet(0,origin.getX()>>4,origin.getZ()>>4,true,Collections.singletonMap(origin.up().toLong(),before));io.netty.buffer.ByteBuf bytes=io.netty.buffer.Unpooled.buffer();
        try {packet.toBytes(bytes);SmartCobNetwork.Packet decoded=new SmartCobNetwork.Packet();decoded.fromBytes(bytes);require(decoded.cells.get(origin.up().toLong()).equals(before),"network contour round trip");}finally{bytes.release();}
        Map<Long,SmartCobProfile> dense=new HashMap<>();for(int i=0;i<3000;i++)dense.put(new BlockPos(i&15,i>>8,(i>>4)&15).toLong(),SmartCobProfile.SQUARE);
        List<SmartCobNetwork.Packet> batches=SmartCobNetwork.packets(0,0,0,true,dense);int received=0;
        require(batches.size()>1&&batches.get(0).replace,"dense fallback chunks split their snapshots");
        for(int i=0;i<batches.size();i++) {io.netty.buffer.ByteBuf buffer=io.netty.buffer.Unpooled.buffer();try{batches.get(i).toBytes(buffer);SmartCobNetwork.Packet decoded=new SmartCobNetwork.Packet();decoded.fromBytes(buffer);received+=decoded.cells.size();require(i==0||!decoded.replace,"only the first snapshot replaces a chunk");}finally{buffer.release();}}
        require(received==dense.size(),"split snapshots preserve every fallback profile");
        BlockPos unknown=new BlockPos(160000,100,160000);require(!world.isBlockLoaded(unknown)&&SmartCobPlanner.survey(world,unknown)==null&&!world.isBlockLoaded(unknown),"unknown chunks stay unloaded");
        FakePlayer player=FakePlayerFactory.getMinecraft(world);player.setPosition(10,90,180);
        for(int finish=0;finish<2;finish++) {
            IBlockState state=block.getStateFromMeta(finish);List<ItemStack> drops=block.getDrops(world,origin,state,0);
            require(drops.size()==1&&drops.get(0).stackSize==1&&drops.get(0).getMetadata()==finish,"canonical intact drops");
            require(block.getPickBlock(state,null,world,origin,player).getMetadata()==finish,"pick finish");
        }
        BlockPos at=origin.north(5);ItemStack held=new ItemStack(block,2,0);net.minecraft.entity.item.EntityArmorStand entity=new net.minecraft.entity.item.EntityArmorStand(world,at.getX()+.5,at.getY(),at.getZ()+.5);world.spawnEntity(entity);
        require(!((SmartCobItem)held.getItem()).placeBlockAt(held,player,world,at,EnumFacing.UP,.5F,1,.5F,block.getDefaultState()),"placement against an entity");world.removeEntityDangerously(entity);
        world.setBlockState(at,block.getDefaultState(),2);flush(world);world.setBlockToAir(at);flush(world);require(!data.contains(at),"removed owner clears its profile");
        // An edit may fill a previously rounded corner, but cannot grow into a player/entity.
        world.setBlockState(at,block.getDefaultState(),2);flush(world);SmartCobProfile rounded=data.profile(at);
        net.minecraft.entity.item.EntityArmorStand inCorner=new net.minecraft.entity.item.EntityArmorStand(world,at.getX()+.01,at.getY(),at.getZ()+.01);
        inCorner.setEntityBoundingBox(new AxisAlignedBB(at.getX()+.07,at.getY()+.1,at.getZ()+.07,at.getX()+.09,at.getY()+.9,at.getZ()+.09));world.spawnEntity(inCorner);
        world.setBlockState(at.west(),Blocks.STONE.getDefaultState(),3);world.setBlockState(at.north(),Blocks.STONE.getDefaultState(),3);
        require(SmartCobEvents.pending(world),"foreign neighbour edit queues a survey");flush(world);
        require(rounded.equals(data.profile(at)),"an expanding contour waits for entities");inCorner.setDead();world.removeEntityDangerously(inCorner);
        world.getWorldInfo().setWorldTotalTime(world.getTotalWorldTime()+11);
        new SmartCobEvents().tick(new net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent(net.minecraftforge.fml.relauncher.Side.SERVER,net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END,world));flush(world);
        require(data.profile(at).area().contains(.08,.08),"blocked expansion retries when clear");
        world.setBlockToAir(at);world.setBlockToAir(at.west());world.setBlockToAir(at.north());flush(world);
        BlockPos oversized=new BlockPos(48,110,208);
        for(int x=2;x<=6;x++)for(int z=12;z<=14;z++)world.getChunkFromChunkCoords(x,z);
        for(int x=0;x<49;x++)world.setBlockState(oversized.east(x),block.getDefaultState(),2);
        data.put(oversized,rounded);flush(world);
        require(SmartCobPlanner.survey(world,oversized)==null&&data.profile(oversized).equals(rounded),"oversized section retains its saved contour");
        require(data.profile(oversized.east()).equals(SmartCobProfile.SQUARE),"new oversized cells keep a closed square fallback");
        for(int x=0;x<49;x++)world.setBlockToAir(oversized.east(x));flush(world);
        for(int y=0;y<33;y++)world.setBlockState(oversized.up(y),block.getDefaultState(),2);
        require(SmartCobPlanner.survey(world,oversized)==null,"vertical survey bound");
        for(int y=0;y<33;y++)world.setBlockToAir(oversized.up(y));flush(world);
        require(!SmartCobEvents.pending(world),"idle survey queue drains");
        if(ArchitectureConfig.windows) {
            BlockPos opening=origin.add(3,1,0);WindowBlock window=ArchitectureBlocks.findWindow("oak",1,false,true,false);
            world.setBlockState(opening,window.getDefaultState().withProperty(WindowBlock.FACING,EnumFacing.NORTH),3);
            world.setBlockState(opening.up(),window.getDefaultState().withProperty(WindowBlock.FACING,EnumFacing.NORTH),3);flush(world);
            require(WindowController.toggle(world,opening,null)&&world.getBlockState(opening.up()).getValue(WindowBlock.OPEN),"window opens beside cob reveals");
            require(WindowController.toggle(world,opening,null)&&!world.getBlockState(opening).getValue(WindowBlock.OPEN),"window closes beside cob reveals");flush(world);
        }
        System.out.println("IAA_SMART_COB_PASS oneId=true metadata=16 mixedFinish=true collision=true ray=true chunkBorder=true saved=true packets=true entityRetry=true bounds=true noIdleWork=true");
    }
    private static void flush(WorldServer world){for(int i=0;i<30&&SmartCobEvents.pending(world);i++)SmartCobEvents.flush(world);require(!SmartCobEvents.pending(world),"survey queue did not drain");}
}
