package zone.moddev.mc.ironagearchitecture.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.CommonProxy;
import zone.moddev.mc.ironagearchitecture.block.FrameBlock;
import zone.moddev.mc.ironagearchitecture.block.RoofBlock;
import zone.moddev.mc.ironagearchitecture.block.JettyBracketItem;
import zone.moddev.mc.ironagearchitecture.block.WindowBlock;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraft.util.ResourceLocation;

/** Models are registered before baking and never referenced by common classes. */
public final class ClientProxy extends CommonProxy {
    @Override public void preInit() {
        super.preInit();
        MinecraftForge.EVENT_BUS.register(this);
        for (Block block : ArchitectureBlocks.VISIBLE) {
            int count = block instanceof FrameBlock ? ((FrameBlock)block).variants() : 1;
            for (int meta = 0; meta < count; meta++) {
                String name = block.getRegistryName().toString() + (block instanceof FrameBlock ? "_" + meta : "");
                ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block),meta,new ModelResourceLocation(name,"inventory"));
            }
        }
        for(JettyBracketItem item:ArchitectureBlocks.BRACKETS) ModelLoader.setCustomModelResourceLocation(item,0,new ModelResourceLocation(item.getRegistryName(),"inventory"));
        if(ArchitectureBlocks.LEADED_PANE!=null) for(int meta=0;meta<2;meta++) ModelLoader.setCustomModelResourceLocation(ArchitectureBlocks.LEADED_PANE,meta,new ModelResourceLocation("ironagearchitecture:leaded_pane_"+meta,"inventory"));
    }
    @SubscribeEvent public void bake(ModelBakeEvent event) {
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
        for (RoofBlock block : ArchitectureBlocks.ROOFS) {
            String texture = "ironagearchitecture:blocks/" + (block.getRegistryName().getResourcePath().startsWith("thatch") ? "thatch" : "slate");
            for (IBlockState state : block.getBlockState().getValidStates()) {
                ModelResourceLocation key = new ModelResourceLocation(block.getRegistryName(),variant(state));
                IBakedModel base = event.getModelRegistry().getObject(key);
                event.getModelRegistry().putObject(key,new RoofBakedModel(base,block,state,
                        Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(texture)));
            }
            ModelResourceLocation inventory = new ModelResourceLocation(block.getRegistryName(),"inventory");
            event.getModelRegistry().putObject(inventory,new RoofBakedModel(event.getModelRegistry().getObject(inventory),block,
                    block.getDefaultState(),Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(texture)));
        }
    }
    @SubscribeEvent public void stitch(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(new ResourceLocation("minecraft:blocks/glass_silver"));event.getMap().registerSprite(new ResourceLocation("minecraft:blocks/iron_block"));
        for(String wood:ArchitectureBlocks.WOODS) {
            event.getMap().registerSprite(new ResourceLocation("ironagearchitecture:blocks/beam_"+wood));
            event.getMap().registerSprite(new ResourceLocation("minecraft:blocks/log_"+(wood.equals("dark_oak")?"big_oak":wood)+"_top"));
        }
    }
    @SubscribeEvent public void loadWorld(WorldEvent.Load event) {
        if(event.getWorld().isRemote) event.getWorld().addEventListener(new WallRenderListener());
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
