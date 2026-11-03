package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.*;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.block.*;

final class WindowProbe {
    private static void require(boolean ok,String message){if(!ok) throw new IllegalStateException(message);}
    static void verify(WorldServer world,boolean reload) {
        int metadata=0,operations=0;BlockPos pos=new BlockPos(15,90,-48);
        // A border arena needs the diagonal chunks too: power queries deliberately never load them.
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) world.getChunkFromChunkCoords((pos.getX()>>4)+x,(pos.getZ()>>4)+z);
        FakePlayer player=FakePlayerFactory.getMinecraft(world);player.capabilities.isCreativeMode=false;player.setHeldItem(EnumHand.MAIN_HAND,null);player.setPosition(8,90,-60);
        for(WindowBlock block:ArchitectureBlocks.WINDOWS) {
            require(Item.getItemFromBlock(block.canonical())!=null,"Missing canonical window item");
            if(block.right||block.back) require(Item.getItemFromBlock(block)==null,"Hidden window has an item");
            for(int meta=0;meta<16;meta++) {
                IBlockState state=block.getStateFromMeta(meta);require(block.getMetaFromState(state)==meta,"Window metadata "+state);
                for(Mirror mirror:Mirror.values()) {IBlockState once=block.withMirror(state,mirror);require(once.getBlock().withMirror(once,mirror).equals(state),"Window mirror "+state);}
                IBlockState rotated=state;for(int i=0;i<4;i++) rotated=block.withRotation(rotated,Rotation.CLOCKWISE_90);require(rotated.equals(state),"Window rotation");metadata++;
            }
            for(EnumFacing facing:EnumFacing.HORIZONTALS) {
                clear(world,pos);IBlockState state=block.getDefaultState().withProperty(WindowBlock.FACING,facing);
                world.setBlockState(pos,state,2);world.setBlockState(pos.up(),state,2);
                Vec3d start=point(pos,8,8,-16,facing),end=point(pos,8,8,32,facing);
                require(block.collisionRayTrace(state,world,pos,start,end)!=null,"Closed pane targeting "+state);
                BlockPos leaf=pos.offset(facing);world.setBlockState(leaf.up(),Blocks.STONE.getDefaultState(),3);
                require(!WindowController.toggle(world,pos,null)&&!world.getBlockState(pos).getValue(WindowBlock.OPEN)&&!world.getBlockState(pos.up()).getValue(WindowBlock.OPEN),"Partial opening on obstruction");
                world.setBlockToAir(leaf.up());
                AxisAlignedBB swing=WindowGeometry.sweep(block.right,facing,block.back).offset(pos);
                EntityArmorStand entity=new EntityArmorStand(world,(swing.minX+swing.maxX)/2,pos.getY(),(swing.minZ+swing.maxZ)/2);world.spawnEntity(entity);
                require(!WindowController.toggle(world,pos,null),"Opened through an entity");world.removeEntityDangerously(entity);
                require(WindowController.toggle(world,pos,null)&&world.getBlockState(pos.up()).getValue(WindowBlock.OPEN),"Column did not open");
                require(WindowController.owns(world,leaf,pos)&&WindowController.owns(world,leaf.up(),pos.up()),"Missing outside cells");
                IBlockState opened=world.getBlockState(pos);
                require(block.collisionRayTrace(opened,world,pos,start,end)==null,"Open aperture still targets a pane");
                List<AxisAlignedBB> proxyBoxes=WindowGeometry.boxes(block.thin,block.right,true,facing,WindowGeometry.UP,true,block.back);
                AxisAlignedBB proxyBox=proxyBoxes.get(proxyBoxes.size()-1).offset(leaf);double cx=(proxyBox.minX+proxyBox.maxX)/2,cy=(proxyBox.minY+proxyBox.maxY)/2,cz=(proxyBox.minZ+proxyBox.maxZ)/2;
                Vec3d a=facing.getAxis()==EnumFacing.Axis.Z?new Vec3d(cx-1,cy,cz):new Vec3d(cx,cy,cz-1),b=facing.getAxis()==EnumFacing.Axis.Z?new Vec3d(cx+1,cy,cz):new Vec3d(cx,cy,cz+1);
                require(ArchitectureBlocks.WINDOW_LEAF.collisionRayTrace(world.getBlockState(leaf),world,leaf,a,b)!=null,"Outside leaf cannot be targeted");
                for(double z=-.8;z<1.8;z+=.2) {
                    Vec3d centre=point(pos,8,1.5,z*16,facing);
                    AxisAlignedBB body=new AxisAlignedBB(centre.xCoord-.3,centre.yCoord,centre.zCoord-.3,centre.xCoord+.3,centre.yCoord+1.8,centre.zCoord+.3);
                    require(world.getCollisionBoxes(player,body).isEmpty(),"Player cannot fit through open column "+state);
                }
                require(ArchitectureBlocks.WINDOW_LEAF.getDrops(world,leaf,world.getBlockState(leaf),0).isEmpty(),"Independent helper drops");
                require(ArchitectureBlocks.WINDOW_LEAF.getPickBlock(world.getBlockState(leaf),null,world,leaf,player).getItem()==Item.getItemFromBlock(block.canonical()),"Helper pick identity");
                require(WindowController.toggle(world,pos.up(),null)&&world.isAirBlock(leaf)&&world.isAirBlock(leaf.up()),"Column did not close");
                world.setBlockState(pos.down(),Blocks.REDSTONE_BLOCK.getDefaultState(),3);
                require(world.getBlockState(pos).getValue(WindowBlock.OPEN)&&world.getBlockState(pos.up()).getValue(WindowBlock.POWERED),"Power at lower section "+state+" actual="+world.getBlockState(pos)+" power="+world.isBlockPowered(pos));
                require(WindowController.toggle(world,pos,null)&&!world.getBlockState(pos).getValue(WindowBlock.OPEN),"Manual powered override");
                world.setBlockState(pos.offset(facing.getOpposite()),Blocks.STONE.getDefaultState(),3);
                require(!world.getBlockState(pos).getValue(WindowBlock.OPEN),"Unchanged power erased manual override");
                world.setBlockToAir(pos.offset(facing.getOpposite()));world.setBlockToAir(pos.down());
                world.setBlockState(leaf,Blocks.STONE.getDefaultState(),3);world.setBlockState(pos.up(2),Blocks.REDSTONE_BLOCK.getDefaultState(),3);
                require(!world.getBlockState(pos).getValue(WindowBlock.OPEN),"Blocked redstone opening");world.setBlockToAir(leaf);
                require(world.getBlockState(pos).getValue(WindowBlock.OPEN)&&world.getBlockState(pos).getValue(WindowBlock.POWERED),"Power did not retry after clearing obstruction");
                world.setBlockToAir(pos.up(2));require(!world.getBlockState(pos).getValue(WindowBlock.OPEN),"Falling power did not close");
                WindowController.toggle(world,pos,null);world.setBlockState(leaf,Blocks.STONE.getDefaultState(),3);
                require(!world.getBlockState(pos).getValue(WindowBlock.OPEN)&&world.getBlockState(leaf).getBlock()==Blocks.STONE,"Forced replacement deleted a foreign block");
                clear(world,pos);operations++;
            }
        }
        // Mining the outside portion has exactly the owner's ordinary/creative drop behaviour.
        WindowBlock sample=ArchitectureBlocks.findWindow("oak",1,true,true,true,true);
        for(boolean creative:new boolean[]{false,true}) {
            clear(world,pos);removeDrops(world,pos);player.capabilities.isCreativeMode=creative;
            world.setBlockState(pos,sample.getDefaultState(),2);WindowController.toggle(world,pos,null);BlockPos leaf=pos.north();IBlockState proxy=world.getBlockState(leaf);
            require(ArchitectureBlocks.WINDOW_LEAF.removedByPlayer(proxy,world,leaf,player,true),"Could not mine open leaf");
            ArchitectureBlocks.WINDOW_LEAF.harvestBlock(world,player,leaf,proxy,null,null);
            List<EntityItem> drops=world.getEntitiesWithinAABB(EntityItem.class,new AxisAlignedBB(pos.add(-2,-1,-2),pos.add(3,3,3)));
            require(drops.size()==(creative?0:1),"Duplicate/lost helper harvest drops "+drops.size());
            require(world.isAirBlock(pos)&&world.isAirBlock(leaf),"Harvest left a window/helper");removeDrops(world,pos);
        }
        player.capabilities.isCreativeMode=false;
        placement(world,pos,player);
        clear(world,pos);world.setBlockState(pos,sample.getDefaultState(),2);
        AxisAlignedBB swept=WindowGeometry.sweep(sample.right,EnumFacing.NORTH,sample.back).offset(pos);
        EntityArmorStand blocker=new EntityArmorStand(world,(swept.minX+swept.maxX)/2,pos.getY(),(swept.minZ+swept.maxZ)/2);world.spawnEntity(blocker);
        world.setBlockState(pos.down(),Blocks.REDSTONE_BLOCK.getDefaultState(),3);
        require(!world.getBlockState(pos).getValue(WindowBlock.OPEN),"Powered window opened through entity");
        world.removeEntityDangerously(blocker);WindowController.retryEntities(world);
        require(world.getBlockState(pos).getValue(WindowBlock.OPEN),"Entity-blocked power edge never retried");
        clear(world,pos);world.setBlockState(pos,sample.getDefaultState(),2);
        blocker=new EntityArmorStand(world,(swept.minX+swept.maxX)/2,pos.getY(),(swept.minZ+swept.maxZ)/2);world.spawnEntity(blocker);
        world.setBlockState(pos.down(),Blocks.REDSTONE_BLOCK.getDefaultState(),3);world.removeEntityDangerously(blocker);
        require(WindowController.toggle(world,pos,null)&&WindowController.toggle(world,pos,null),"Manual override of a pending power edge");
        WindowController.retryEntities(world);require(!world.getBlockState(pos).getValue(WindowBlock.OPEN),"Pending retry erased a manual override");
        clear(world,pos);removeDrops(world,pos);
        world.setBlockState(pos,sample.getDefaultState(),2);WindowController.toggle(world,pos,null);
        ArchitectureBlocks.WINDOW_LEAF.onBlockExploded(world,pos.north(),new net.minecraft.world.Explosion(world,null,pos.getX(),pos.getY(),pos.getZ(),1,false,true));
        require(world.isAirBlock(pos)&&world.isAirBlock(pos.north()),"Explosion left owner/helper");
        require(world.getEntitiesWithinAABB(EntityItem.class,new AxisAlignedBB(pos.add(-2,-1,-2),pos.add(3,3,3))).size()==1,"Explosion duplicated/lost window");removeDrops(world,pos);
        // Reconcile links on either side without replacing unrelated blocks.
        world.setBlockState(pos.north(),ArchitectureBlocks.WINDOW_LEAF.getDefaultState().withProperty(WindowLeafBlock.FACING,EnumFacing.SOUTH),2);
        WindowController.repair(world,pos.north());require(world.isAirBlock(pos.north()),"Orphan helper remained");
        world.setBlockState(pos,sample.getDefaultState(),2);WindowController.toggle(world,pos,null);
        world.setBlockState(pos.north(),net.minecraft.init.Blocks.AIR.getDefaultState(),2); // break callback closes the owner
        world.setBlockState(pos,world.getBlockState(pos).withProperty(WindowBlock.OPEN,true),2);
        WindowController.repair(world,pos);require(WindowController.owns(world,pos.north(),pos),"Missing helper was not repaired");clear(world,pos);
        // Save all visible choices, both hinge directions and both thin half-cell placements.
        int saved=0;
        for(WindowBlock block:ArchitectureBlocks.WINDOWS) {
            BlockPos at=new BlockPos(-112+saved*3,75,160);EnumFacing facing=EnumFacing.getHorizontal(saved&3);
            boolean open=(saved&1)!=0,power=(saved&2)!=0;IBlockState expected=block.getDefaultState().withProperty(WindowBlock.FACING,facing).withProperty(WindowBlock.OPEN,open).withProperty(WindowBlock.POWERED,power);
            if(reload) {require(world.getBlockState(at).equals(expected)&&world.getBlockState(at.up()).equals(expected),"Window reload "+saved);require(WindowController.owns(world,at.offset(facing),at)==open,"Helper reload "+saved);}
            else {
                world.setBlockState(at,block.getDefaultState().withProperty(WindowBlock.FACING,facing),2);world.setBlockState(at.up(),block.getDefaultState().withProperty(WindowBlock.FACING,facing),2);
                if(power) world.setBlockState(at.down(),Blocks.REDSTONE_BLOCK.getDefaultState(),3);
                require(WindowController.set(world,WindowController.column(world,at),open,power,null,false),"Saved window setup "+saved);
            }
            saved++;
        }
        System.out.println("IAA_WINDOW_PROBE_PASS metadata="+metadata+" directionalColumns="+operations+" savedColumns="+saved+" reload="+reload);
    }
    private static Vec3d point(BlockPos pos,double x,double y,double z,EnumFacing facing){double[] p=WallGeometry.rotate(x,y,z,facing);return new Vec3d(pos.getX()+p[0]/16,pos.getY()+p[1]/16,pos.getZ()+p[2]/16);}
    private static void placement(WorldServer world,BlockPos pos,FakePlayer player) {
        for(boolean thin:new boolean[]{false,true}) for(EnumFacing facing:EnumFacing.HORIZONTALS) for(boolean right:new boolean[]{false,true}) for(boolean rear:new boolean[]{false,true}) {
            if(!thin&&rear) continue;
            clear(world,pos);player.setSneaking(false);player.rotationYaw=facing.getOpposite().getHorizontalIndex()*90;
            WindowBlock block=ArchitectureBlocks.findWindow("oak",0,thin,true,false);ItemStack stack=new ItemStack(block,4);player.setHeldItem(EnumHand.MAIN_HAND,stack);
            double[] hit=WallGeometry.rotate(right?12:4,16,rear?12:4,facing);
            require(stack.getItem().onItemUse(stack,player,world,pos,EnumHand.MAIN_HAND,EnumFacing.UP,(float)hit[0]/16,1,(float)hit[2]/16)==EnumActionResult.SUCCESS,"Initial placement");
            IBlockState placed=world.getBlockState(pos);WindowBlock actual=(WindowBlock)placed.getBlock();
            require(placed.getValue(WindowBlock.FACING)==facing&&actual.right==right&&actual.back==rear&&stack.stackSize==3,"Placement facing/hinge "+facing+" "+placed);
            require(!actual.onBlockActivated(world,pos,placed,player,EnumHand.MAIN_HAND,stack,EnumFacing.UP,.5F,1,.5F),"Window item toggles clicked window");
            require(stack.getItem().onItemUse(stack,player,world,pos,EnumHand.MAIN_HAND,EnumFacing.UP,.5F,1,.5F)==EnumActionResult.SUCCESS,"Stack placement");
            require(world.getBlockState(pos.up()).getBlock()==actual&&world.getBlockState(pos.up()).getValue(WindowBlock.FACING)==facing,"Stack lost alignment");
            player.setHeldItem(EnumHand.MAIN_HAND,null);WindowController.toggle(world,pos,null);
            BlockPos adjacent=pos.offset(facing.rotateY());world.setBlockState(adjacent,placed,2);
            require(!world.getBlockState(adjacent).getValue(WindowBlock.OPEN),"Adjacent column opened together");
            WindowController.toggle(world,adjacent,null);WindowController.toggle(world,pos,null);
            require(world.getBlockState(adjacent).getValue(WindowBlock.OPEN),"Adjacent column closed together");world.setBlockToAir(adjacent);
            // A missing middle section must not silently merge incompatible open states.
            world.setBlockToAir(pos.up());world.setBlockState(pos.up(2),placed,2);WindowController.toggle(world,pos.up(2),null);
            ItemStack bridge=new ItemStack(block);player.setHeldItem(EnumHand.MAIN_HAND,bridge);
            require(bridge.getItem().onItemUse(bridge,player,world,pos,EnumHand.MAIN_HAND,EnumFacing.UP,.5F,1,.5F)==EnumActionResult.FAIL&&bridge.stackSize==1&&world.isAirBlock(pos.up()),"Open/closed bridge was accepted");
            clear(world,pos);player.setHeldItem(EnumHand.MAIN_HAND,null);
        }
        for(EnumFacing facing:EnumFacing.HORIZONTALS) for(boolean interior:new boolean[]{false,true}) {
            clear(world,pos);PartialWallBlock wall=ArchitectureBlocks.findPartial("oak",FrameFamily.POST,WallProfile.THIN);
            IBlockState support=wall.getDefaultState().withProperty(FrameBlock.FACING,facing);world.setBlockState(pos.down(),support,2);
            Vec3d standing=point(pos,8,0,interior?48:-32,facing);player.setPosition(standing.xCoord,standing.yCoord,standing.zCoord);player.rotationYaw=facing.getOpposite().getHorizontalIndex()*90;
            ItemStack stack=new ItemStack(ArchitectureBlocks.findWindow("oak",0,true,true,false));
            require(stack.getItem().onItemUse(stack,player,world,pos.down(),EnumHand.MAIN_HAND,EnumFacing.UP,.5F,1,.5F)==EnumActionResult.SUCCESS,"Thin wall placement");
            IBlockState result=world.getBlockState(pos);WindowBlock actual=(WindowBlock)result.getBlock();
            require(result.getValue(WindowBlock.FACING)==(interior?facing.getOpposite():facing)&&actual.back==interior,"Thin wall outward facing "+result);
            require(WallRuns.of(result,pos).get(0).aligned(WallRuns.of(support,pos.down()).get(0)),"Thin window moved off the wall plane");
            player.setSneaking(true);player.rotationYaw=facing.getHorizontalIndex()*90;
            ItemStack override=new ItemStack(ArchitectureBlocks.findWindow("oak",0,false,true,false));
            require(override.getItem().onItemUse(override,player,world,pos,EnumHand.MAIN_HAND,EnumFacing.UP,.5F,1,.5F)==EnumActionResult.SUCCESS,"Sneak placement");
            require(world.getBlockState(pos.up()).getValue(WindowBlock.FACING)==facing.getOpposite(),"Sneaking did not override alignment");
            player.setSneaking(false);clear(world,pos);
        }
        player.setPosition(8,90,-60);
        player.setSneaking(false);
    }
    private static void removeDrops(WorldServer world,BlockPos pos){for(EntityItem item:world.getEntitiesWithinAABB(EntityItem.class,new AxisAlignedBB(pos.add(-2,-1,-2),pos.add(3,3,3)))) world.removeEntityDangerously(item);}
    private static void clear(WorldServer world,BlockPos pos){for(int y=-1;y<=3;y++) {world.setBlockToAir(pos.up(y));for(EnumFacing facing:EnumFacing.HORIZONTALS) world.setBlockToAir(pos.up(y).offset(facing));}}
}
