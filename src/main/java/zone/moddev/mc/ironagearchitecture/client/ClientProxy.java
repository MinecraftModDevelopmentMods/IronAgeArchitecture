package zone.moddev.mc.ironagearchitecture.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.CommonProxy;
import zone.moddev.mc.ironagearchitecture.block.DownpipeBlock;
import zone.moddev.mc.ironagearchitecture.block.GutterItem;
import zone.moddev.mc.ironagearchitecture.block.FireplaceBlock;
import zone.moddev.mc.ironagearchitecture.block.FrameBlock;
import zone.moddev.mc.ironagearchitecture.block.RoofBlock;
import zone.moddev.mc.ironagearchitecture.block.JettyBracketItem;
import zone.moddev.mc.ironagearchitecture.block.WindowBlock;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraft.util.ResourceLocation;

/** Models are registered before baking and never referenced by common classes. */
public final class ClientProxy extends CommonProxy {
    @Override public zone.moddev.mc.ironagearchitecture.block.SmartCobProfile cobProfile(net.minecraft.world.IBlockAccess access,net.minecraft.util.math.BlockPos pos) {
        if(access instanceof net.minecraft.world.World)return super.cobProfile(access,pos);
        net.minecraft.world.World world=Minecraft.getMinecraft().world;
        return world==null?zone.moddev.mc.ironagearchitecture.block.SmartCobProfile.SQUARE:zone.moddev.mc.ironagearchitecture.block.SmartCobData.get(world).profile(pos);
    }
    @Override public void cobSnapshot(zone.moddev.mc.ironagearchitecture.SmartCobNetwork.Packet packet) {
        Minecraft.getMinecraft().addScheduledTask(()->{
            net.minecraft.world.World world=Minecraft.getMinecraft().world;if(world==null||world.provider.getDimension()!=packet.dimension)return;
            zone.moddev.mc.ironagearchitecture.block.SmartCobData data=zone.moddev.mc.ironagearchitecture.block.SmartCobData.get(world);
            if(packet.replace)data.clearChunk(packet.x,packet.z);
            for(java.util.Map.Entry<Long,zone.moddev.mc.ironagearchitecture.block.SmartCobProfile> entry:packet.cells.entrySet())data.put(net.minecraft.util.math.BlockPos.fromLong(entry.getKey()),entry.getValue());
            world.markBlockRangeForRenderUpdate(packet.x*16-1,0,packet.z*16-1,packet.x*16+16,255,packet.z*16+16);
        });
    }
    private zone.moddev.mc.ironagearchitecture.BuildingMode localBuildingMode;
    @SubscribeEvent public void disconnected(net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(()->applyBuildingMode(localBuildingMode));
    }
    private void applyBuildingMode(zone.moddev.mc.ironagearchitecture.BuildingMode mode) {
        if(mode==zone.moddev.mc.ironagearchitecture.ArchitectureConfig.buildingMode) return;
        zone.moddev.mc.ironagearchitecture.ArchitectureConfig.buildingMode=mode;
        net.minecraft.item.crafting.CraftingManager.getInstance().getRecipeList().removeIf(recipe->{
            net.minecraft.item.ItemStack output=recipe.getRecipeOutput();
            return output!=null&&"ironagearchitecture".equals(output.getItem().getRegistryName().getResourceDomain());
        });
        zone.moddev.mc.ironagearchitecture.ArchitectureRecipes.register();
    }
    @SubscribeEvent public void smartChunkUnloaded(net.minecraftforge.event.world.ChunkEvent.Unload event) {
        if(event.getWorld().isRemote) {
            zone.moddev.mc.ironagearchitecture.block.SmartWallData.get(event.getWorld()).clearChunk(event.getChunk().xPosition,event.getChunk().zPosition);
            zone.moddev.mc.ironagearchitecture.block.SmartCobData.get(event.getWorld()).clearChunk(event.getChunk().xPosition,event.getChunk().zPosition);
        }
    }
    @Override public int smartLayout(net.minecraft.world.IBlockAccess access,net.minecraft.util.math.BlockPos pos) {
        if(access instanceof net.minecraft.world.World) return super.smartLayout(access,pos);
        net.minecraft.world.World world=Minecraft.getMinecraft().world;
        return world==null?0:zone.moddev.mc.ironagearchitecture.block.SmartWallData.get(world).code(pos);
    }
    @Override public void smartSnapshot(zone.moddev.mc.ironagearchitecture.SmartWallNetwork.Packet packet) {
        Minecraft.getMinecraft().addScheduledTask(()->{
            net.minecraft.world.World world=Minecraft.getMinecraft().world;
            if(world==null||world.provider.getDimension()!=packet.dimension) return;
            applyBuildingMode(zone.moddev.mc.ironagearchitecture.BuildingMode.values()[packet.mode]);
            zone.moddev.mc.ironagearchitecture.block.SmartWallData data=zone.moddev.mc.ironagearchitecture.block.SmartWallData.get(world);
            if(packet.replace) data.clearChunk(packet.x,packet.z);
            for(java.util.Map.Entry<Long,Integer> entry:packet.cells.entrySet()) {
                net.minecraft.util.math.BlockPos at=net.minecraft.util.math.BlockPos.fromLong(entry.getKey());
                if((at.getX()>>4)==packet.x&&(at.getZ()>>4)==packet.z) data.put(at,entry.getValue());
            }
            world.markBlockRangeForRenderUpdate(packet.x*16-16,0,packet.z*16-16,packet.x*16+31,255,packet.z*16+31);
        });
    }
    @Override public void preInit() {
        localBuildingMode=zone.moddev.mc.ironagearchitecture.ArchitectureConfig.buildingMode;
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(this);
        super.preInit();
        MinecraftForge.EVENT_BUS.register(this);
        for (Block block : ArchitectureBlocks.VISIBLE) {
            int count = ArchitectureBlocks.variants(block);
            for (int meta = 0; meta < count; meta++) {
                String name = block.getRegistryName().toString() + (block instanceof FrameBlock || block instanceof zone.moddev.mc.ironagearchitecture.block.SmartCobBlock || block == ArchitectureBlocks.DOWNPIPE_OFFSET ? "_" + meta : "");
                ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block),meta,new ModelResourceLocation(name,"inventory"));
            }
        }
        if (ArchitectureBlocks.SOFFIT != null)
            ModelLoader.setCustomModelResourceLocation(ArchitectureBlocks.SOFFIT, 0,
                new ModelResourceLocation(ArchitectureBlocks.SOFFIT.getRegistryName(), "inventory"));
        if (ArchitectureBlocks.GUTTER != null) for (GutterItem item : gutterItems())
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        for(JettyBracketItem item:ArchitectureBlocks.BRACKETS) ModelLoader.setCustomModelResourceLocation(item,0,new ModelResourceLocation(item.getRegistryName(),"inventory"));
        if(ArchitectureBlocks.LEADED_PANE!=null) for(int meta=0;meta<2;meta++) ModelLoader.setCustomModelResourceLocation(ArchitectureBlocks.LEADED_PANE,meta,new ModelResourceLocation("ironagearchitecture:leaded_pane_"+meta,"inventory"));
    }
    @SubscribeEvent public void bake(ModelBakeEvent event) {
        if(ArchitectureBlocks.SMART_COB!=null) for(int finish=0;finish<2;finish++) {
            ModelResourceLocation key=new ModelResourceLocation(ArchitectureBlocks.SMART_COB.getRegistryName(),"finish="+finish);
            event.getModelRegistry().putObject(key,new SmartCobBakedModel(event.getModelRegistry().getObject(key),finish));
            key=new ModelResourceLocation(ArchitectureBlocks.SMART_COB.getRegistryName()+"_"+finish,"inventory");
            event.getModelRegistry().putObject(key,new SmartCobBakedModel(event.getModelRegistry().getObject(key),finish));
        }
        for(WindowBlock window:ArchitectureBlocks.WINDOWS) {
            java.util.Map<Integer,WindowBakedModel> shared=new java.util.HashMap<>();
            for(IBlockState state:window.getBlockState().getValidStates()) {ModelResourceLocation key=new ModelResourceLocation(window.getRegistryName(),variant(state));int shape=window.getMetaFromState(state)&7;WindowBakedModel model=shared.get(shape);if(model==null){model=new WindowBakedModel(event.getModelRegistry().getObject(key),state.withProperty(WindowBlock.POWERED,false));shared.put(shape,model);}event.getModelRegistry().putObject(key,model);}
            if(!window.right&&!window.back) {ModelResourceLocation key=new ModelResourceLocation(window.getRegistryName(),"inventory");event.getModelRegistry().putObject(key,new WindowBakedModel(event.getModelRegistry().getObject(key),window.getDefaultState()));}
        }
        if(ArchitectureBlocks.WINDOW_LEAF!=null) {
            for(IBlockState state:ArchitectureBlocks.WINDOW_LEAF.getBlockState().getValidStates()) {ModelResourceLocation key=new ModelResourceLocation(ArchitectureBlocks.WINDOW_LEAF.getRegistryName(),variant(state));event.getModelRegistry().putObject(key,new WindowBakedModel(event.getModelRegistry().getObject(key),(IBlockState)null));}
            for(int meta=0;meta<2;meta++) {ModelResourceLocation key=new ModelResourceLocation("ironagearchitecture:leaded_pane_"+meta,"inventory");event.getModelRegistry().putObject(key,new WindowBakedModel(event.getModelRegistry().getObject(key),meta==0));}
        }
        for(FrameBlock block:ArchitectureBlocks.meshes()) {
            for(IBlockState state:block.getBlockState().getValidStates()) {
                ModelResourceLocation key=new ModelResourceLocation(block.getRegistryName(),variant(state));
                event.getModelRegistry().putObject(key,new WallBakedModel(event.getModelRegistry().getObject(key),block,state));
            }
            for(int meta=0;ArchitectureBlocks.VISIBLE.contains(block)&&meta<block.variants();meta++) {
                ModelResourceLocation key=new ModelResourceLocation(block.getRegistryName()+"_"+meta,"inventory");
                IBlockState state=block.getDefaultState().withProperty(FrameBlock.PANEL,meta);
                event.getModelRegistry().putObject(key,new WallBakedModel(event.getModelRegistry().getObject(key),block,state));
            }
        }
        if (ArchitectureBlocks.GUTTER != null) {
            for (GutterItem item : gutterItems()) {
                ModelResourceLocation key = new ModelResourceLocation(item.getRegistryName(), "inventory");
                event.getModelRegistry().putObject(key, RoofBakedModel.gutter(event.getModelRegistry().getObject(key), item.outlet));
            }
            for (DownpipeBlock pipe : new DownpipeBlock[]{ArchitectureBlocks.DOWNPIPE, ArchitectureBlocks.DOWNPIPE_OFFSET, ArchitectureBlocks.DOWNPIPE_SHOE}) {
                for (IBlockState state : pipe.getBlockState().getValidStates()) {
                    ModelResourceLocation key = new ModelResourceLocation(pipe.getRegistryName(), variant(state));
                    event.getModelRegistry().putObject(key, RoofBakedModel.pipe(event.getModelRegistry().getObject(key), pipe, state));
                }
                for (int i = 0; i < ArchitectureBlocks.variants(pipe); i++) {
                    String name = pipe.getRegistryName().toString() + (pipe.isOffset() ? "_" + i : "");
                    ModelResourceLocation key = new ModelResourceLocation(name, "inventory");
                    IBlockState state = pipe.getDefaultState().withProperty(DownpipeBlock.PROFILE, i);
                    event.getModelRegistry().putObject(key, RoofBakedModel.pipe(event.getModelRegistry().getObject(key), pipe, state));
                }
            }
        }
        for (FireplaceBlock block : ArchitectureBlocks.FIREPLACES) {
            TextureAtlasSprite masonry = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(block.masonry.texture);
            for (IBlockState state : block.getBlockState().getValidStates()) {
                ModelResourceLocation key = new ModelResourceLocation(block.getRegistryName(), variant(state));
                event.getModelRegistry().putObject(key,
                    new FireplaceBakedModel(event.getModelRegistry().getObject(key), block, state, masonry));
            }
            ModelResourceLocation inventory = new ModelResourceLocation(block.getRegistryName(), "inventory");
            event.getModelRegistry().putObject(inventory,
                new FireplaceBakedModel(event.getModelRegistry().getObject(inventory), block, block.getDefaultState(), masonry));
        }
        for (RoofBlock block : ArchitectureBlocks.ROOFS) {
            String texture = "ironagearchitecture:blocks/" + block.textureKey();
            for (IBlockState state : block.getBlockState().getValidStates()) {
                ModelResourceLocation key = new ModelResourceLocation(block.getRegistryName(),variant(state));
                IBakedModel base = event.getModelRegistry().getObject(key);
                event.getModelRegistry().putObject(key,new RoofBakedModel(base,block,state,
                        Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(texture)));
            }
            if (!ArchitectureBlocks.VISIBLE.contains(block)) continue;
            ModelResourceLocation inventory = new ModelResourceLocation(block.getRegistryName(),"inventory");
            event.getModelRegistry().putObject(inventory,new RoofBakedModel(event.getModelRegistry().getObject(inventory),block,
                    block.getDefaultState(),Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(texture)));
        }
    }
    private static GutterItem[] gutterItems() {
        return new GutterItem[]{ArchitectureBlocks.GUTTER, ArchitectureBlocks.GUTTER_OUTLET};
    }

    @SubscribeEvent public void stitch(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(new ResourceLocation("minecraft:blocks/glass_silver"));event.getMap().registerSprite(new ResourceLocation("minecraft:blocks/iron_block"));
        event.getMap().registerSprite(new ResourceLocation("minecraft:blocks/planks_big_oak"));
        if (!ArchitectureBlocks.FIREPLACES.isEmpty()) for (String material : new String[]{
                "brick", "stonebrick", "hardened_clay_stained_orange", "stone_slab_top", "magma", "fire_layer_0"})
            event.getMap().registerSprite(new ResourceLocation("minecraft:blocks/" + material));
        for(String wood:ArchitectureBlocks.WOODS) {
            event.getMap().registerSprite(new ResourceLocation("ironagearchitecture:blocks/beam_"+wood));
            event.getMap().registerSprite(new ResourceLocation("minecraft:blocks/log_"+(wood.equals("dark_oak")?"big_oak":wood)+"_top"));
        }
    }
    @SubscribeEvent public void loadWorld(WorldEvent.Load event) {
        if(event.getWorld().isRemote) {
            zone.moddev.mc.ironagearchitecture.block.SmartWallData.get(event.getWorld());
            event.getWorld().addEventListener(new WallRenderListener());
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String variant(IBlockState state) {
        List<IProperty<?>> properties = new ArrayList<>(state.getPropertyKeys());
        properties.sort(Comparator.comparing(IProperty::getName));
        List<String> values = new ArrayList<>();
        for (IProperty property : properties) values.add(property.getName() + "=" + property.getName(state.getValue(property)));
        return String.join(",",values);
    }
}
