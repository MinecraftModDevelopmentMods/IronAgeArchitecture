package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.*;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;

final class SmartWallProbe {
    private static void require(boolean ok,String message){if(!ok) throw new IllegalStateException("Smart walls: "+message);}
    static void verify(WorldServer world,boolean reload) {
        if(!ArchitectureConfig.frames) {require(ArchitectureBlocks.SMART_WALLS.isEmpty(),"disabled registration");return;}
        require(ArchitectureBlocks.SMART_WALLS.size()==(ArchitectureConfig.retainLegacyWallIds?18:12),"registered profiles");
        int recipes=0;
        for(net.minecraft.item.crafting.IRecipe recipe:net.minecraft.item.crafting.CraftingManager.getInstance().getRecipeList()) {
            ItemStack output=recipe.getRecipeOutput();
            if(output!=null&&"ironagearchitecture".equals(output.getItem().getRegistryName().getResourceDomain())) {
                require(ArchitectureBlocks.available(output.getItem()),"mode excludes manual or automatic recipe");
                recipes++;
            }
        }
        require(recipes==EraProbe.expectedRecipes(),"mode recipe total "+recipes+" expected "+EraProbe.expectedRecipes());
        RecipeProbe.verifyReachability();
        for(SmartWallBlock wall:ArchitectureBlocks.SMART_WALLS) for(int meta=0;meta<16;meta++)
            for(Mirror mirror:new Mirror[]{Mirror.FRONT_BACK,Mirror.LEFT_RIGHT}) {
                IBlockState state=wall.getStateFromMeta(meta);
                int original=wall.mask(state),expected=0;
                for(int q=0;q<4;q++) if((original&(1<<q))!=0) {
                    int x=q%2,z=q/2;
                    if(mirror==Mirror.FRONT_BACK) x=1-x;else z=1-z;
                    expected|=1<<(x+z*2);
                }
                require(wall.mask(wall.withMirror(state,mirror))==expected,"mirrored occupancy");
            }
        BlockPos origin=new BlockPos(44,100,80);
        SmartWallData data=SmartWallData.get(world);
        if(reload) {
            require(data.code(origin)!=0,"saved framing");
            System.out.println("IAA_SMART_RELOAD_PASS savedLayout=true");return;
        }
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) world.getChunkFromBlockCoords(origin.add(x*16,0,z*16));
        SmartWallBlock full=ArchitectureBlocks.findSmart("oak",0);
        for(int x=0;x<11;x++) for(int y=0;y<4;y++)
            world.setBlockState(origin.add(x,y,0),full.getDefaultState(),2);
        for(int y=1;y<=2;y++) world.setBlockState(origin.add(3,y,0),opening(),2);
        for(int i=0;i<100&&SmartWallEvents.pending(world);i++) SmartWallEvents.flush(world);
        BlockPos unloaded=new BlockPos(160000,100,160000);
        require(!world.isBlockLoaded(unloaded)&&SmartWallPlanner.survey(world,unloaded,EnumFacing.NORTH)==null&&!world.isBlockLoaded(unloaded),
            "unknown chunk remains unloaded");
        int before=data.code(origin.add(4,1,0)), braces=0;
        for(int x=0;x<11;x++) for(int y=0;y<4;y++) {
            int code=SmartWallGeometry.face(data.code(origin.add(x,y,0)),EnumFacing.NORTH);
            if((code&(SmartFacade.RISE|SmartFacade.FALL))!=0) braces++;
        }
        require(braces>0,"planned facade braces, origin="+Integer.toHexString(data.code(origin))+", pending="+SmartWallEvents.pending(world)+", survey="+(SmartWallPlanner.survey(world,origin,EnumFacing.NORTH)!=null));
        world.setBlockState(origin.add(6,1,0),opening(),3);
        for(int i=0;i<100&&SmartWallEvents.pending(world);i++) SmartWallEvents.flush(world);
        require(data.code(origin.add(4,1,0))!=before||((SmartWallGeometry.face(data.code(origin.add(5,1,0)),EnumFacing.NORTH)&SmartFacade.RIGHT)!=0),
            "opening edit reframes neighbouring bay");
        NBTTagCompound saved=data.writeToNBT(new NBTTagCompound());
        SmartWallData restored=new SmartWallData();restored.readFromNBT(saved);
        require(restored.code(origin)==data.code(origin),"NBT layout round trip");
        SmartWallNetwork.Packet packet=new SmartWallNetwork.Packet();
        packet.dimension=0;packet.x=origin.getX()>>4;packet.z=origin.getZ()>>4;packet.replace=true;
        packet.cells=Collections.singletonMap(origin.toLong(),data.code(origin));
        io.netty.buffer.ByteBuf bytes=io.netty.buffer.Unpooled.buffer();
        try {
            packet.toBytes(bytes);
            SmartWallNetwork.Packet decoded=new SmartWallNetwork.Packet();decoded.fromBytes(bytes);
            require(decoded.cells.equals(packet.cells)&&decoded.mode==ArchitectureConfig.buildingMode.ordinal(),"layout packet and mode");
        } finally {bytes.release();}
        FakePlayer player=FakePlayerFactory.getMinecraft(world);
        player.setPosition(35,100,75);player.setSneaking(true);
        try {
            SmartWallBlock thin=ArchitectureBlocks.findSmart("oak",1);
            BlockPos corner=origin.north(4);
            IBlockState first=thin.getDefaultState().withProperty(FrameBlock.FACING,EnumFacing.NORTH);
            world.setBlockState(corner,first,3);
            ItemStack held=new ItemStack(thin,2,0);
            EnumActionResult result=held.getItem().onItemUse(held,player,world,corner,EnumHand.MAIN_HAND,EnumFacing.WEST,.2F,.5F,.5F);
            require(result==EnumActionResult.SUCCESS,"thin corner placement");
            IBlockState placed=world.getBlockState(corner);
            require(placed.getBlock() instanceof SmartWallBlock&&((SmartWallBlock)placed.getBlock()).profile(placed)==2,"joined L footprint");
            List<ItemStack> drops=placed.getBlock().getDrops(world,corner,placed,0);
            require(drops.size()==1&&drops.get(0).stackSize==2&&drops.get(0).getItem()==Item.getItemFromBlock(thin),"corner recovers two panels");
            for(EnumFacing facing:EnumFacing.HORIZONTALS) {
                IBlockState state=thin.getDefaultState().withProperty(FrameBlock.FACING,facing);
                int occupied=thin.mask(state);
                require(Integer.bitCount(occupied)==2,"thin facing");
                double volume=0;for(AxisAlignedBB box:SmartWallGeometry.boxes(occupied)) volume+=(box.maxX-box.minX)*(box.maxZ-box.minZ);
                require(Math.abs(volume-.5)<1e-8,"fixed thin collision");
                world.setBlockState(corner,state,2);
                JettyFloorBlock floor=ArchitectureBlocks.findFloor("oak",FloorShape.FULL,null);
                IBlockState deck=floor.getDefaultState();
                world.setBlockState(corner.up(),deck,2);
                require((floor.connections(deck,world,corner.up())>>>16)==occupied,"jetty support follows occupied half");
                for(int q=0;q<4;q++) {
                    double cx=q%2*.5+.25,cz=q/2*.5+.25;
                    Vec3d start=new Vec3d(corner.getX()+cx,corner.getY()-.1,corner.getZ()+cz);
                    Vec3d end=new Vec3d(corner.getX()+cx,corner.getY()+1.1,corner.getZ()+cz);
                    boolean hit=thin.collisionRayTrace(state,world,corner,start,end)!=null;
                    require(hit==((occupied&(1<<q))!=0),"thin occupied/empty targeting");
                }
                world.setBlockToAir(corner.up());
            }
            require(world.getTileEntity(corner)==null,"no per-wall tile entity");
        } finally {player.setSneaking(false);}
        ItemStack silkTool=new ItemStack(net.minecraft.init.Items.DIAMOND_AXE);
        silkTool.addEnchantment(net.minecraft.init.Enchantments.SILK_TOUCH,1);
        BlockPos mined=origin.north(8);
        for(SmartWallBlock wall:ArchitectureBlocks.SMART_WALLS) if(wall.profile==0)
            for(EnumFacing facing:EnumFacing.HORIZONTALS) for(int finish=0;finish<2;finish++) {
                IBlockState state=wall.getDefaultState().withProperty(FrameBlock.FACING,facing).withProperty(FrameBlock.PANEL,finish);
                world.setBlockState(mined,state,2);world.setBlockToAir(mined);
                wall.harvestBlock(world,player,mined,state,null,silkTool);
                List<net.minecraft.entity.item.EntityItem> drops=world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,new AxisAlignedBB(mined).expand(1,1,1));
                drops.removeIf(drop->drop.isDead);
                require(drops.size()==1&&drops.get(0).getEntityItem().getItem()==Item.getItemFromBlock(wall)
                    &&drops.get(0).getEntityItem().getMetadata()==finish,"silk harvest keeps wood and finish "+wall+" "+facing+" "+finish+" drops="+drops.size());
                for(net.minecraft.entity.item.EntityItem drop:drops) world.removeEntity(drop);
            }
        // The normal server/client path uses exactly the persisted descriptor format.
        System.out.println("IAA_SMART_WALL_PASS braces="+braces+" chunkBorder=true edit=true save=true corners=true facings=4 mode="+ArchitectureConfig.buildingMode);
    }
    private static IBlockState opening() {
        return ArchitectureConfig.windows?ArchitectureBlocks.findWindow("oak",0,false,true,false).getDefaultState():Blocks.AIR.getDefaultState();
    }
}
