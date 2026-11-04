package zone.moddev.mc.ironagearchitecture.probe;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.ArchitectureConfig;
import zone.moddev.mc.ironagearchitecture.block.FrameBlock;
import zone.moddev.mc.ironagearchitecture.block.RoofBlock;
import zone.moddev.mc.ironagearchitecture.block.RoofShape;
import zone.moddev.mc.ironagearchitecture.block.RoofGeometry;
import zone.moddev.mc.ironagearchitecture.block.FrameFamily;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry;
import zone.moddev.mc.ironagearchitecture.block.*;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;

/** Disposable packaged-runtime verification mod. Never part of the player jar. */
@Mod(modid="ironagearchitectureprobe",name="Architecture Verification",version="1",acceptableRemoteVersions="*",
        dependencies="required-after:ironagearchitecture@[0.1.10.110021]")
public final class ArchitectureProbe {
    @SidedProxy(clientSide="zone.moddev.mc.ironagearchitecture.probe.ClientProbe",serverSide="zone.moddev.mc.ironagearchitecture.probe.ProbeProxy")
    public static ProbeProxy proxy;
    @Mod.EventHandler public void init(FMLInitializationEvent event) { proxy.init(); }
    private static void require(boolean condition,String message) { if(!condition) throw new IllegalStateException(message); }
    @Mod.EventHandler public void started(FMLServerStartedEvent event) throws Exception {
        if(!Boolean.getBoolean("iaa.probe")) return;
        MinecraftServer server=FMLCommonHandler.instance().getMinecraftServerInstance();
        verifyConfigMatrix();
        WorldServer world=server.worldServerForDimension(0);
        if(Boolean.getBoolean("iaa.eraMatrix")) EraProbe.verify(world);
        if(Boolean.getBoolean("iaa.recipeMatrix")) RecipeProbe.verify(world);
        if(Boolean.getBoolean("iaa.legacy")) verifyLegacy(world);
        if(Boolean.getBoolean("iaa.legacyDaub")) verifyLegacyDaub(world);
        int variants=0, recipes=0, states=0;
        boolean reload=Boolean.getBoolean("iaa.reload");
        boolean clayClient = Boolean.getBoolean("iaa.clientProbe") && ArchitectureConfig.clayTiles && ArchitectureConfig.frames;
        variants=ArchitectureBlocks.visibleStacks().size();
        List<Block> savedBlocks=new ArrayList<>(ArchitectureBlocks.VISIBLE);
        savedBlocks.removeIf(block->block instanceof WindowBlock);
        for(FrameBlock block:ArchitectureBlocks.meshes()) if(!savedBlocks.contains(block)) savedBlocks.add(block);
        if(Boolean.getBoolean("iaa.legacyJetties")) {
            int index=0;
            for(Block block:savedBlocks) {
                if (ClayTileProbe.isClay(block)) continue;
                for(int meta=0;meta<16;meta++) {
                    require(world.getBlockState(new BlockPos(100+index,70,100)).equals(block.getStateFromMeta(meta)),"0.1.2 saved state "+index);
                    index++;
                }
            }
            require(index==4448,"0.1.2 legacy matrix incomplete");
        }
        for(Block block:savedBlocks) {
            int count=block instanceof FrameBlock?((FrameBlock)block).variants():1;
            for(int meta=0;meta<16;meta++) {
                IBlockState state=block.getStateFromMeta(meta);
                require(block.getStateFromMeta(block.getMetaFromState(state)).equals(state),"Metadata "+block+" "+meta);
                BlockPos pos=new BlockPos(100+states,70,100);
                if(reload) require(world.getBlockState(pos).equals(state),"Reload "+pos);
                else world.setBlockState(pos,state,2);
                List<ItemStack> drops=block.getDrops(world,pos,state,0);
                Item expected=block instanceof PartialWallBlock?Item.getItemFromBlock(((PartialWallBlock)block).canonical()):
                    block instanceof JettyFloorBlock?Item.getItemFromBlock(((JettyFloorBlock)block).canonical()):Item.getItemFromBlock(block);
                require(drops.size()==(block instanceof JettyFloorBlock&&((JettyFloorBlock)block).bracket!=null?2:1)&&drops.get(0).getItem()==expected,"Drop item "+block);
                require(drops.get(0).getMetadata()==(block instanceof FrameBlock?block.damageDropped(state):0),"Drop identity "+block);
                states++;
            }
        }
        require(variants==Integer.getInteger("iaa.expectedVariants",EraProbe.expectedVariants()),"Visible variants "+variants);
        for(IRecipe recipe:CraftingManager.getInstance().getRecipeList()) {
            ItemStack output=recipe.getRecipeOutput();
            if(output==null||!"ironagearchitecture".equals(output.getItem().getRegistryName().getResourceDomain())) continue;
            InventoryCrafting grid=new InventoryCrafting(new Container(){@Override public boolean canInteractWith(EntityPlayer p){return true;}},3,3);
            if(recipe instanceof ShapedRecipes) {
                ShapedRecipes shaped=(ShapedRecipes)recipe;
                for(int y=0;y<shaped.recipeHeight;y++) for(int x=0;x<shaped.recipeWidth;x++) {
                    ItemStack ingredient=shaped.recipeItems[x+y*shaped.recipeWidth];
                    if(ingredient!=null) grid.setInventorySlotContents(x+y*3,ingredient.copy());
                }
            } else if(recipe instanceof ShapelessRecipes) {
                List<ItemStack> inputs=((ShapelessRecipes)recipe).recipeItems;
                for(int i=0;i<inputs.size();i++) grid.setInventorySlotContents(i,inputs.get(i).copy());
            } else throw new IllegalStateException("Unexpected recipe type "+recipe);
            ItemStack actual=CraftingManager.getInstance().findMatchingRecipe(grid,world);
            require(actual!=null&&ItemStack.areItemStacksEqual(actual,output),"Recipe shadowed "+output);
            recipes++;
        }
        if(variants>=460) {
            require(recipes==EraProbe.expectedRecipes(),"Recipe catalog changed "+recipes);
            verifyRoofs(world);
            verifyWalls(world);
            verifyPlacementAndWalking(world);
            verifyJetties(world);
            verifyWallHeads(world);
            HorizontalRailProbe.verify(world);
            if(ArchitectureConfig.windows) WindowProbe.verify(world,reload);
            if (!reload && !clayClient) {
                new JettyPreview().build(world);
                if (ArchitectureConfig.windows) WindowPreview.build(world);
                TimberJoinProbe.preview(world);
            }
            for(int finish=0;finish<2;finish++) {
                Block doubled=Block.getBlockFromName("ironagearchitecture:"+ArchitectureBlocks.FINISHES[finish]+"_double_slab");
                List<ItemStack> drops=doubled.getDrops(world,new BlockPos(90,70,100),doubled.getDefaultState(),0);
                int total=0;for(ItemStack stack:drops) {total+=stack.stackSize;require(stack.getItem()==Item.getItemFromBlock(ArchitectureBlocks.SLABS.get(finish)),"Slab drop");}
                require(total==2,"Double slab quantity");
            }
        }
        ClayTileProbe.verify(world);
        if (!reload && clayClient) RyeTilePreview.build(world);
        if (Boolean.getBoolean("iaa.clayOnly")) {
            require(variants == 7 && recipes == 10 && RecipeProbe.verifyReachability() == 7, "Standalone clay tiles");
        }
        world.setWorldTime(1000);world.getGameRules().setOrCreateGameRule("doDaylightCycle","false");
        world.getGameRules().setOrCreateGameRule("doFireTick","false");world.getGameRules().setOrCreateGameRule("doMobSpawning","false");
        world.getGameRules().setOrCreateGameRule("spawnRadius","0");
        world.setSpawnPoint(clayClient ? RyeTilePreview.SPAWN : new BlockPos(7,64,-9));
        server.saveAllWorlds(false);
        String result="{\"passed\":true,\"variants\":"+variants+",\"recipes\":"+recipes+",\"states\":"+states+",\"reload\":"+reload
                +",\"legacy\":"+Boolean.getBoolean("iaa.legacy")+",\"legacyDaub\":"+Boolean.getBoolean("iaa.legacyDaub")
                +",\"legacyJetties\":"+Boolean.getBoolean("iaa.legacyJetties")+",\"eras\":"+new com.google.gson.Gson().toJson(zone.moddev.mc.ironagearchitecture.ArchitectureConfig.survivalEras.allowed)
                +",\"eraMatrix\":"+Boolean.getBoolean("iaa.eraMatrix")+",\"recipeMatrix\":"+Boolean.getBoolean("iaa.recipeMatrix")+",\"runtimeSha256\":\""+System.getProperty("iaa.runtimeSha256","development")+"\"}\n";
        Files.write(Paths.get(System.getProperty("iaa.receipt","architecture-probe.json")),result.getBytes(StandardCharsets.UTF_8));
        System.out.println("IAA_PROBE_PASS "+result.trim());
        if(!Boolean.getBoolean("iaa.keepRunning")) server.initiateShutdown();
    }
    private void verifyConfigMatrix() throws Exception {
        boolean cob=ArchitectureConfig.cob,frames=ArchitectureConfig.frames,thatch=ArchitectureConfig.thatch,slate=ArchitectureConfig.slate,windows=ArchitectureConfig.windows;
        boolean clayTiles = ArchitectureConfig.clayTiles;
        zone.moddev.mc.ironagearchitecture.EraSelection eras=ArchitectureConfig.survivalEras;
        java.nio.file.Path directory=Paths.get("config-matrix");Files.createDirectories(directory);
        try {
            for(int mask=0;mask<64;mask++) {
                java.nio.file.Path file=directory.resolve(mask+".cfg");
                String text="blocks {\n B:cob="+((mask&1)!=0)+"\n B:timberFrames="+((mask&2)!=0)
                        +"\n B:thatch="+((mask&4)!=0)+"\n B:slate="+((mask&8)!=0)+"\n B:leadedWindows="+((mask&16)!=0)
                        +"\n B:clayTiles="+((mask&32)!=0)+"\n}\n";
                Files.write(file,text.getBytes(StandardCharsets.UTF_8));ArchitectureConfig.load(file.toFile());
                require(ArchitectureConfig.cob==((mask&1)!=0)&&ArchitectureConfig.frames==((mask&3)==3)
                        &&ArchitectureConfig.thatch==((mask&4)!=0)&&ArchitectureConfig.slate==((mask&8)!=0)&&ArchitectureConfig.windows==((mask&19)==19)
                        &&ArchitectureConfig.clayTiles==((mask&32)!=0),"Config mask "+mask);
            }
        } finally {ArchitectureConfig.cob=cob;ArchitectureConfig.frames=frames;ArchitectureConfig.thatch=thatch;ArchitectureConfig.slate=slate;ArchitectureConfig.windows=windows;ArchitectureConfig.clayTiles=clayTiles;ArchitectureConfig.survivalEras=eras;}
    }
    private void verifyWallHeads(WorldServer world) {
        BlockPos pos=new BlockPos(95,90,95);
        for(String wood:ArchitectureBlocks.WOODS) for(EnumFacing facing:EnumFacing.HORIZONTALS) {
            JettyFloorBlock floor=ArchitectureBlocks.findFloor(wood,FloorShape.FULL,null);
            IBlockState deck=floor.getDefaultState().withProperty(FrameBlock.FACING,facing);
            world.setBlockState(pos,deck,2);
            for(PartialWallBlock wall:ArchitectureBlocks.PARTIALS) if(wood.equals(wall.wood)) {
                IBlockState below=wall.getDefaultState().withProperty(FrameBlock.FACING,facing);
                world.setBlockState(pos.down(),below,2);
                int joins=floor.connections(deck,world,pos);
                for(int q=0;q<4;q++) {
                    double x=q%2*8+4,z=q/2*8+4;
                    require(((joins>>(16+q))&1)==(wall.profile.contains(x,z)?1:0),"Wall-head footprint "+wall+" "+facing);
                    double[] point=WallGeometry.rotate(x,4,z,facing);boolean filled=false;
                    for(WallGeometry.Part part:floor.parts(deck,joins)) if(part.bounds(facing).isVecInside(new net.minecraft.util.math.Vec3d(point[0]/16,point[1]/16,point[2]/16))) filled=true;
                    require(filled==wall.profile.contains(x,z),"Wall-head opening/cantilever air "+wall+" "+q);
                }
            }
            world.setBlockToAir(pos.down());
            require((floor.connections(deck,world,pos)>>16)==0,"Stale wall-head beam");
            world.setBlockState(pos.down(),Blocks.STONE.getDefaultState(),2);
            require((floor.connections(deck,world,pos)>>16)==15,"Solid lower-wall head");
            world.setBlockToAir(pos);world.setBlockToAir(pos.down());
        }
    }
    private void verifyPlacementAndWalking(WorldServer world) {
        FakePlayer player=FakePlayerFactory.getMinecraft(world);
        // Server players have zero step height; emulate the client's normal walking step.
        player.stepHeight=.6F;
        BlockPos pos=new BlockPos(70,80,70);
        for(FrameBlock frame:ArchitectureBlocks.WALLS) for(int panel=0;panel<frame.variants();panel++) for(EnumFacing facing:EnumFacing.HORIZONTALS) {
            player.rotationYaw=facing.getHorizontalIndex()*90;
            IBlockState placed=frame.getStateForPlacement(world,pos,EnumFacing.UP,.5F,.5F,.5F,panel,player);
            require(placed.getValue(FrameBlock.PANEL)==panel&&placed.getValue(BlockHorizontal.FACING)==player.getHorizontalFacing().getOpposite(),"Frame placement");
        }
        for(int finish=0;finish<2;finish++) {
            Block half=ArchitectureBlocks.SLABS.get(finish);
            world.setBlockState(pos,half.getDefaultState(),2);
            ItemStack stack=new ItemStack(half);
            player.setPosition(70.5,82,72);
            EnumActionResult result=stack.getItem().onItemUse(stack,player,world,pos,EnumHand.MAIN_HAND,EnumFacing.UP,.5F,1,.5F);
            require(result==EnumActionResult.SUCCESS&&world.getBlockState(pos).getBlock()!=half&&world.getBlockState(pos).isFullCube(),"Slab merging");
        }
        world.setBlockToAir(pos);
        for(int x=-3;x<=3;x++) for(int z=-3;z<=3;z++) world.setBlockState(pos.add(x,-1,z),Blocks.STONE.getDefaultState(),2);
        for(RoofBlock roof:ArchitectureBlocks.ROOFS) {
            if(roof.isRidge() || roof instanceof TrimmedRoofBlock) continue;
            for(EnumFacing facing:EnumFacing.HORIZONTALS) {
                world.setBlockState(pos,roof.getDefaultState().withProperty(BlockHorizontal.FACING,facing),2);
                double[] start=RoofGeometry.rotate(.5,0,1.6,facing);
                player.setPosition(pos.getX()+start[0],pos.getY(),pos.getZ()+start[2]);
                for(int step=0;step<56;step++) {
                    player.onGround=true;
                    player.move(facing.getFrontOffsetX()*.025,-.08,facing.getFrontOffsetZ()*.025);
                }
                require(player.posY>pos.getY()+.75&&player.posY<=pos.getY()+1.01,"Walk slope "+facing+" height "+player.posY
                        +" x="+player.posX+" z="+player.posZ+" step="+player.stepHeight+" noclip="+player.noClip
                        +" collision="+world.getCollisionBoxes(player,new net.minecraft.util.math.AxisAlignedBB(pos,pos.add(1,1,1))).size());
            }
        }
        world.setBlockToAir(pos);
    }
    private void verifyLegacy(WorldServer world) {
        for(int wood=0;wood<6;wood++) for(int meta=0;meta<16;meta++) {
            IBlockState state=world.getBlockState(new BlockPos(100+(6+wood)*16+meta,70,100));
            require(state.getBlock()==ArchitectureBlocks.FRAMES.get(wood),"Old timber block ID "+wood);
            require(state.getValue(FrameBlock.PANEL)==(meta>>2)&&state.getValue(BlockHorizontal.FACING)==EnumFacing.getHorizontal(meta&3),"Old timber variant "+meta);
        }
    }
    private void verifyLegacyDaub(WorldServer world) {
        List<Block> blocks=new ArrayList<>();
        for(int finish=0;finish<2;finish++) {blocks.add(ArchitectureBlocks.COB.get(finish));blocks.add(ArchitectureBlocks.SLABS.get(finish));blocks.add(ArchitectureBlocks.STAIRS.get(finish));}
        blocks.addAll(ArchitectureBlocks.WALLS);
        for (RoofBlock roof : ArchitectureBlocks.ROOFS) if (!ClayTileProbe.isClay(roof)) blocks.add(roof);
        int index=0;
        for(Block block:blocks) for(int meta=0;meta<16;meta++) {
            require(world.getBlockState(new BlockPos(100+index,70,100)).equals(block.getStateFromMeta(meta)),"0.1.1 saved wall/roof state "+index);
            index++;
        }
        require(index==656,"0.1.1 legacy matrix incomplete");
    }
    private void verifyJetties(WorldServer world) {
        BlockPos pos=new BlockPos(64,88,64);
        for(FrameBlock block:ArchitectureBlocks.meshes()) if(block instanceof PartialWallBlock||block instanceof JettyFloorBlock)
            for(int meta=0;meta<16;meta++) {
                IBlockState state=block.getStateFromMeta(meta);
                require(!state.isFullCube()&&!state.isOpaqueCube(),"Partial block classification "+state);
                for(Mirror mirror:Mirror.values()) {
                    IBlockState once=block.withMirror(state,mirror);
                    require(once.getBlock().withMirror(once,mirror).equals(state),"Partial double mirror "+state+" "+mirror+" -> "+once);
                }
                IBlockState rotated=state;
                for(int i=0;i<4;i++) rotated=rotated.getBlock().withRotation(rotated,Rotation.CLOCKWISE_90);
                require(rotated.equals(state),"Partial rotation "+state);
                for(WallGeometry.Part part:block.parts(state,0)) {
                    net.minecraft.util.math.AxisAlignedBB bounds=part.bounds(state.getValue(FrameBlock.FACING));
                    require(bounds.minX>=0&&bounds.minY>=0&&bounds.minZ>=0&&bounds.maxX<=1&&bounds.maxY<=1&&bounds.maxZ<=1,"Partial bounds "+state+" "+bounds);
                }
            }
        for(String wood:ArchitectureBlocks.WOODS) for(EnumFacing facing:EnumFacing.HORIZONTALS) {
            PartialWallBlock post=ArchitectureBlocks.findPartial(wood,FrameFamily.POST,WallProfile.THIN),rail=ArchitectureBlocks.findPartial(wood,FrameFamily.RAIL,WallProfile.THIN);
            IBlockState postState=post.getDefaultState().withProperty(FrameBlock.FACING,facing),railState=rail.getDefaultState().withProperty(FrameBlock.FACING,facing);
            EnumFacing right=facing.rotateY();
            world.setBlockState(pos,postState,2);world.setBlockState(pos.offset(right),railState,2);
            require((post.connections(postState,world,pos)&(WallGeometry.bit(right)<<4))!=0,"Thin post/rail join");
            world.setBlockState(pos.offset(right),railState.withProperty(FrameBlock.FACING,facing.getOpposite()),2);
            require((post.connections(postState,world,pos)&(WallGeometry.bit(right)<<4))==0,"Offset parallel wall falsely joined");
            world.setBlockToAir(pos.offset(right));
            require(post.connections(postState,world,pos)==0,"Thin stale join");
        }
        FakePlayer player=FakePlayerFactory.getMinecraft(world);player.setPosition(65,94,69);
        player.capabilities.isCreativeMode=false;
        BlockPos placement=new BlockPos(80,89,80);
        for(PartialWallBlock block:ArchitectureBlocks.PARTIALS) if(ArchitectureBlocks.VISIBLE.contains(block))
            for(int panel=0;panel<block.variants();panel++) for(EnumFacing side:EnumFacing.values()) for(float hit:new float[]{.25F,.75F}) {
                world.setBlockToAir(placement);player.setSneaking(false);
                player.rotationYaw=side.getHorizontalIndex()*90;
                ItemStack stack=new ItemStack(block,1,panel);
                IBlockState expected=block.getStateForPlacement(world,placement,side,hit,hit,hit,panel,player);
                EnumActionResult result=stack.getItem().onItemUse(stack,player,world,placement,EnumHand.MAIN_HAND,side,hit,hit,hit);
                require(result==EnumActionResult.SUCCESS&&stack.stackSize==0&&world.getBlockState(placement).equals(expected),"Partial item placement "+block+" "+side);
            }
        world.setBlockState(placement,Blocks.STONE.getDefaultState(),2);world.setBlockState(placement.up(),Blocks.STONE.getDefaultState(),2);
        ItemStack blocked=new ItemStack(ArchitectureBlocks.findPartial("oak",FrameFamily.POST,WallProfile.THIN));
        require(blocked.getItem().onItemUse(blocked,player,world,placement,EnumHand.MAIN_HAND,EnumFacing.UP,.25F,1,.25F)==EnumActionResult.FAIL&&blocked.stackSize==1,"Occupied wall placement consumed item");
        world.setBlockToAir(placement);world.setBlockToAir(placement.up());
        PartialWallBlock thin=ArchitectureBlocks.findPartial("oak",FrameFamily.POST,WallProfile.THIN);
        world.setBlockState(placement.down(),thin.getDefaultState().withProperty(FrameBlock.FACING,EnumFacing.WEST),2);
        player.rotationYaw=0;player.setSneaking(false);
        require(thin.getStateForPlacement(world,placement,EnumFacing.UP,.25F,1,.25F,0,player).getValue(FrameBlock.FACING)==EnumFacing.WEST,"Continuation lost its wall plane");
        player.setSneaking(true);
        require(thin.getStateForPlacement(world,placement,EnumFacing.UP,.25F,1,.25F,0,player).getValue(FrameBlock.FACING)==EnumFacing.NORTH,"Sneaking did not bypass plane inheritance");
        player.setSneaking(false);world.setBlockToAir(placement.down());
        world.setBlockState(pos,thin.getDefaultState(),2);
        require(thin.collisionRayTrace(thin.getDefaultState(),world,pos,new net.minecraft.util.math.Vec3d(64.5,90,64.8),new net.minecraft.util.math.Vec3d(64.5,87,64.8))==null,"Empty half blocked targeting");
        require(thin.collisionRayTrace(thin.getDefaultState(),world,pos,new net.minecraft.util.math.Vec3d(64.5,90,64.2),new net.minecraft.util.math.Vec3d(64.5,87,64.2))!=null,"Filled half missing targeting");
        world.setBlockToAir(pos);
        for(String wood:ArchitectureBlocks.WOODS) {
            JettyFloorBlock full=ArchitectureBlocks.findFloor(wood,FloorShape.FULL,null),quarter=ArchitectureBlocks.findFloor(wood,FloorShape.QUARTER,null);
            world.setBlockState(pos,full.getDefaultState(),2);world.setBlockState(pos.east(),full.getDefaultState(),2);
            require(!JettyFloorGeometry.exposed(FloorShape.FULL,1,EnumFacing.EAST,full.connections(full.getDefaultState(),world,pos)),"Deck internal perimeter");
            world.setBlockToAir(pos.east());
            require(JettyFloorGeometry.exposed(FloorShape.FULL,1,EnumFacing.EAST,full.connections(full.getDefaultState(),world,pos)),"Deck removed perimeter");
            ItemStack bracket=new ItemStack(ArchitectureBlocks.findBracket(wood,true));
            EnumActionResult used=bracket.getItem().onItemUse(bracket,player,world,pos,EnumHand.MAIN_HAND,EnumFacing.DOWN,.5F,.5F,0);
            require(used==EnumActionResult.SUCCESS&&((JettyFloorBlock)world.getBlockState(pos).getBlock()).bracket!=null&&bracket.stackSize==0,"Bracket placement/consumption");
            List<ItemStack> drops=world.getBlockState(pos).getBlock().getDrops(world,pos,world.getBlockState(pos),0);
            require(drops.size()==2&&drops.get(1).getItem()==ArchitectureBlocks.findBracket(wood,true),"Bracket drops");
            world.setBlockState(pos,quarter.getDefaultState(),2);bracket=new ItemStack(ArchitectureBlocks.findBracket(wood,true));
            require(bracket.getItem().onItemUse(bracket,player,world,pos,EnumHand.MAIN_HAND,EnumFacing.DOWN,0,0,0)==EnumActionResult.FAIL&&bracket.stackSize==1,"Invalid bracket consumed item");
            // An actual server entity stands on the thin boards and crosses a floor seam.
            world.setBlockState(pos,full.getDefaultState(),2);world.setBlockState(pos.east(),full.getDefaultState(),2);
            net.minecraft.util.math.AxisAlignedBB feet=new net.minecraft.util.math.AxisAlignedBB(64.2,88.9,64.2,64.8,89.1,64.8);
            require(!world.getCollisionBoxes(player,feet).isEmpty(),"Deck walkable top");
            require(world.getCollisionBoxes(player,new net.minecraft.util.math.AxisAlignedBB(64.2,88.05,64.2,64.8,88.4,64.8)).isEmpty(),"Invisible full-cube collision below deck");
            player.setPosition(64.5,89,64.5);player.move(.8,-.1,0);
            require(Math.abs(player.posY-89)<1e-6&&Math.abs(player.posX-65.3)<1e-6,"Walking across floor seam");
            player.setPosition(65,94,69);
            world.setBlockToAir(pos);world.setBlockToAir(pos.east());
        }
    }
    private void verifyWalls(WorldServer world) {
        BlockPos pos=new BlockPos(64,82,64);
        for(FrameBlock wall:ArchitectureBlocks.WALLS) for(int panel=0;panel<wall.variants();panel++) for(EnumFacing facing:EnumFacing.HORIZONTALS) {
            IBlockState state=wall.getDefaultState().withProperty(FrameBlock.PANEL,panel).withProperty(BlockHorizontal.FACING,facing);
            require(state.isFullCube()&&state.getLightOpacity()==255,"Filled wall lighting/opacity "+state);
            for(Mirror mirror:Mirror.values()) require(wall.withMirror(wall.withMirror(state,mirror),mirror).equals(state),"Wall double mirror "+state+" "+mirror);
            IBlockState rotated=state;
            for(int i=0;i<4;i++) rotated=wall.withRotation(rotated,Rotation.CLOCKWISE_90);
            require(rotated.equals(state),"Wall rotation "+state);
        }
        for(String wood:ArchitectureBlocks.WOODS) for(EnumFacing facing:EnumFacing.HORIZONTALS) {
            for(EnumFacing direction:EnumFacing.HORIZONTALS) for(int d=1;d<=4;d++) world.setBlockToAir(pos.offset(direction,d));
            FrameBlock post=ArchitectureBlocks.findWall(wood,FrameFamily.POST);
            FrameBlock rail=ArchitectureBlocks.findWall(wood,FrameFamily.RAIL);
            FrameBlock corner=ArchitectureBlocks.findWall(wood,FrameFamily.TERMINAL);
            IBlockState postState=post.getDefaultState().withProperty(BlockHorizontal.FACING,facing);
            world.setBlockState(pos,postState,2);
            EnumFacing right=facing.rotateY();
            world.setBlockState(pos.offset(right),rail.getDefaultState().withProperty(BlockHorizontal.FACING,facing),2);
            require((post.connections(postState,world,pos)&(WallGeometry.bit(right)<<4))!=0,"Post/sill connection");
            world.setBlockToAir(pos.offset(right));
            require((post.connections(postState,world,pos)&0xFF0)==0,"Stale removed rail");
            world.setBlockState(pos,corner.getDefaultState().withProperty(BlockHorizontal.FACING,facing),2);
            world.setBlockState(pos.offset(right),rail.getDefaultState().withProperty(FrameBlock.PANEL,3).withProperty(BlockHorizontal.FACING,facing),2);
            world.setBlockState(pos.offset(facing.getOpposite()),rail.getDefaultState().withProperty(FrameBlock.PANEL,3).withProperty(BlockHorizontal.FACING,facing.rotateYCCW()),2);
            int mask=corner.connections(world.getBlockState(pos),world,pos);
            require((mask&(WallGeometry.bit(right)<<8))!=0&&(mask&(WallGeometry.bit(facing.getOpposite())<<8))!=0,"Corner rail wrap");
            for(EnumFacing direction:EnumFacing.HORIZONTALS) world.setBlockToAir(pos.offset(direction));
            // Bounded joins pass through structural intersections and stop at plain infill.
            world.setBlockState(pos,postState,2);
            world.setBlockState(pos.offset(right),postState,2);
            world.setBlockState(pos.offset(right,2),rail.getDefaultState().withProperty(BlockHorizontal.FACING,facing),2);
            require((post.connections(postState,world,pos)&(WallGeometry.bit(right)<<4))!=0,"Rail through post");
            world.setBlockState(pos.offset(right),ArchitectureBlocks.DAUB.getDefaultState(),2);
            require((post.connections(postState,world,pos)&0xFF0)==0,"Infill must stop automatic rail");
            for(EnumFacing direction:EnumFacing.HORIZONTALS) for(int d=1;d<=4;d++) world.setBlockToAir(pos.offset(direction,d));
            FrameBlock stud=ArchitectureBlocks.findWall(wood,FrameFamily.STUD);
            IBlockState railState=rail.getDefaultState().withProperty(BlockHorizontal.FACING,facing);
            world.setBlockState(pos,railState,2);
            world.setBlockState(pos.up(),stud.getDefaultState().withProperty(FrameBlock.PANEL,3).withProperty(BlockHorizontal.FACING,facing),2);
            require(((rail.connections(railState,world,pos)>>16)&15)==3,"Stud must reach sill rail");
            world.setBlockToAir(pos.up());
            world.setBlockState(pos,postState,2);
            FrameBlock brace=ArchitectureBlocks.findWall(wood,FrameFamily.BRACE);
            world.setBlockState(pos.offset(right).up(),brace.getDefaultState().withProperty(BlockHorizontal.FACING,facing),2);
            require((post.connections(postState,world,pos)&(1<<20))!=0,"Brace must reach principal post");
            world.setBlockToAir(pos.offset(right).up());
        }
    }
    private void verifyRoofs(WorldServer world) {
        BlockPos pos=new BlockPos(90,70,90);
        for(RoofBlock roof:ArchitectureBlocks.ROOFS) {
            if(roof.isRidge() || roof instanceof TrimmedRoofBlock) continue;
            for(EnumFacing facing:EnumFacing.HORIZONTALS) {
                for(EnumFacing turn:new EnumFacing[]{facing.rotateY(),facing.rotateYCCW()}) {
                    for(EnumFacing side:EnumFacing.HORIZONTALS) world.setBlockToAir(pos.offset(side));
                    IBlockState state=roof.getDefaultState().withProperty(BlockHorizontal.FACING,facing);
                    world.setBlockState(pos,state,2);
                    world.setBlockState(pos.offset(facing),roof.getDefaultState().withProperty(BlockHorizontal.FACING,turn),2);
                    require(roof.getActualState(state,world,pos).getValue(RoofBlock.SHAPE).name().startsWith("OUTER"),"Hip");
                    world.setBlockToAir(pos.offset(facing));
                    require(roof.getActualState(state,world,pos).getValue(RoofBlock.SHAPE)==RoofShape.STRAIGHT,"Neighbour removal");
                    world.setBlockState(pos.offset(facing.getOpposite()),roof.getDefaultState().withProperty(BlockHorizontal.FACING,turn),2);
                    require(roof.getActualState(state,world,pos).getValue(RoofBlock.SHAPE).name().startsWith("INNER"),"Valley");
                }
            }
        }
    }
    private void demonstration(WorldServer world) {
        for(int x=-6;x<=46;x++) for(int z=-8;z<=24;z++) {
            world.setBlockState(new BlockPos(x,63,z),Blocks.GRASS.getDefaultState(),2);
            for(int y=64;y<=76;y++) world.setBlockToAir(new BlockPos(x,y,z));
        }
        cottage(world,0,"thatch",ArchitectureBlocks.FRAMES.get(1),3);
        cottage(world,16,"slate",ArchitectureBlocks.FRAMES.get(0),2);
        for(int wood=0;wood<6;wood++) for(int panel=0;panel<4;panel++)
            world.setBlockState(new BlockPos(wood*3,65,17+panel),ArchitectureBlocks.FRAMES.get(wood).getDefaultState().withProperty(FrameBlock.PANEL,panel),2);
        for(RoofBlock roof:ArchitectureBlocks.ROOFS) for(EnumFacing facing:EnumFacing.HORIZONTALS)
            world.setBlockState(new BlockPos(28+facing.getHorizontalIndex()*3,65,roof.isRidge()?18:14),roof.getDefaultState().withProperty(BlockHorizontal.FACING,facing),2);
        RoofBlock testRoof=ArchitectureBlocks.ROOFS.get(0);
        for(int i=0;i<4;i++) {
            BlockPos pos=new BlockPos(31+i*4,65,4);
            world.setBlockState(pos,testRoof.getDefaultState(),2);
            world.setBlockState(pos.offset(i<2?EnumFacing.NORTH:EnumFacing.SOUTH),
                    testRoof.getDefaultState().withProperty(BlockHorizontal.FACING,i%2==0?EnumFacing.WEST:EnumFacing.EAST),2);
        }
    }
    private void cottage(WorldServer world,int ox,String material,FrameBlock frame,int panel) {
        Block cob=ArchitectureBlocks.COB.get(panel/2);
        for(int x=0;x<=8;x++) for(int z=0;z<=8;z++) {
            world.setBlockState(new BlockPos(ox+x,64,z),Blocks.PLANKS.getDefaultState(),2);
            if(x==0||x==8||z==0||z==8) for(int y=65;y<=68;y++) {
                EnumFacing facing=x==0?EnumFacing.WEST:x==8?EnumFacing.EAST:z==0?EnumFacing.NORTH:EnumFacing.SOUTH;
                IBlockState wall=(y==65?cob.getDefaultState():frame.getDefaultState().withProperty(FrameBlock.PANEL,panel).withProperty(BlockHorizontal.FACING,facing));
                if(y==66&&(x==2||x==6)&&(z==0||z==8)) wall=Blocks.GLASS.getDefaultState();
                if(z==0&&x==4&&(y==65||y==66)) wall=Blocks.AIR.getDefaultState();
                world.setBlockState(new BlockPos(ox+x,y,z),wall,2);
            }
        }
        RoofBlock roof=(RoofBlock)Block.getBlockFromName("ironagearchitecture:"+material+"_roof");
        RoofBlock ridge=(RoofBlock)Block.getBlockFromName("ironagearchitecture:"+material+"_ridge");
        for(int z=-1;z<=9;z++) {
            for(int x=-1;x<=9;x++) {
                if(x==4) world.setBlockState(new BlockPos(ox+x,74,z),ridge.getDefaultState(),2);
                else world.setBlockState(new BlockPos(ox+x,69+Math.min(x+1,9-x),z),roof.getDefaultState().withProperty(BlockHorizontal.FACING,x<4?EnumFacing.EAST:EnumFacing.WEST),2);
            }
        }
        // Close the gables below each slope without changing the visible roof profile.
        for(int z:new int[]{0,8}) for(int x=0;x<=8;x++) for(int y=69;y<69+Math.min(x+1,9-x);y++)
            world.setBlockState(new BlockPos(ox+x,y,z),cob.getDefaultState(),2);
    }
}
