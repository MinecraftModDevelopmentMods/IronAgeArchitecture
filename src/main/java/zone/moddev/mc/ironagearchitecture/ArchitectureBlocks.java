package zone.moddev.mc.ironagearchitecture;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import zone.moddev.mc.ironagearchitecture.block.CobSlab;
import zone.moddev.mc.ironagearchitecture.block.CobStairs;
import zone.moddev.mc.ironagearchitecture.block.FrameBlock;
import zone.moddev.mc.ironagearchitecture.block.FrameItem;
import zone.moddev.mc.ironagearchitecture.block.FrameFamily;
import zone.moddev.mc.ironagearchitecture.block.RoofBlock;
import zone.moddev.mc.ironagearchitecture.block.RidgeBlock;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Explicit small catalog; no dependency on Furniture registrations. */
public final class ArchitectureBlocks {
    public static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
    public static final String[] FINISHES = {"cob", "limewashed_cob"};
    public static final List<Block> VISIBLE = new ArrayList<>();
    public static final List<Block> COB = new ArrayList<>();
    public static final List<CobSlab> SLABS = new ArrayList<>();
    public static final List<CobStairs> STAIRS = new ArrayList<>();
    public static final List<FrameBlock> FRAMES = new ArrayList<>();
    public static final List<FrameBlock> WALLS = new ArrayList<>();
    public static final List<PartialWallBlock> PARTIALS = new ArrayList<>();
    public static final List<JettyFloorBlock> FLOORS = new ArrayList<>();
    public static final List<JettyBracketItem> BRACKETS = new ArrayList<>();
    public static final List<WindowBlock> WINDOWS = new ArrayList<>();
    public static WindowLeafBlock WINDOW_LEAF;
    public static LeadedPaneItem LEADED_PANE;
    public static GutterItem GUTTER, GUTTER_OUTLET;
    public static SoffitItem SOFFIT;
    public static DownpipeBlock DOWNPIPE, DOWNPIPE_OFFSET, DOWNPIPE_SHOE;

    public static int variants(Block block) {
        if (block instanceof FrameBlock) return ((FrameBlock)block).variants();
        return block == DOWNPIPE_OFFSET ? 4 : 1;
    }
    public static List<ItemStack> visibleStacks() {
        List<ItemStack> result=new ArrayList<>();
        for(Block block:VISIBLE) for(int meta=0;meta<variants(block);meta++) result.add(new ItemStack(block,1,meta));
        for(JettyBracketItem item:BRACKETS) result.add(new ItemStack(item));
        if(LEADED_PANE!=null) {result.add(new ItemStack(LEADED_PANE,1,0));result.add(new ItemStack(LEADED_PANE,1,1));}
        if (GUTTER != null) {
            result.add(new ItemStack(GUTTER));
            result.add(new ItemStack(GUTTER_OUTLET));
            result.add(new ItemStack(SOFFIT));
        }
        return result;
    }
    public static List<FrameBlock> meshes() {
        List<FrameBlock> result=new ArrayList<>(WALLS);result.addAll(PARTIALS);result.addAll(FLOORS);return result;
    }
    public static FrameBlock DAUB;
    public static final List<RoofBlock> ROOFS = new ArrayList<>();
    public static TileHangingBlock CLAY_HANGING, CLAY_HANGING_THIN;
    public static FasciaBlock CLAY_FASCIA, CLAY_FASCIA_LINED;
    public static TrimmedRoofBlock CLAY_VERGE, CLAY_RIDGE_END;
    private ArchitectureBlocks() { }
    public static <T extends Block> T block(T block, String name) {
        block.setRegistryName(new ResourceLocation(IronAgeArchitecture.MODID, name));
        block.setUnlocalizedName(IronAgeArchitecture.MODID + "." + name);
        GameRegistry.register(block);
        return block;
    }
    private static void item(Block block, ItemBlock item) {
        item.setRegistryName(block.getRegistryName());
        GameRegistry.register(item);
        block.setCreativeTab(IronAgeArchitecture.TAB);
        VISIBLE.add(block);
    }
    public static void register() {
        if (ArchitectureConfig.cob) for (String finish : FINISHES) {
            Block full = block(new CobBlock(), finish);
            item(full, new EraItemBlock(full));
            COB.add(full);
            CobSlab half = block(new CobSlab(false), finish + "_slab");
            CobSlab doubled = block(new CobSlab(true), finish + "_double_slab");
            half.setHalf(half); doubled.setHalf(half);
            item(half, new EraItemSlab(half, half, doubled));
            SLABS.add(half);
            CobStairs stairs = block(new CobStairs(full.getDefaultState()), finish + "_stairs");
            item(stairs, new EraItemBlock(stairs));
            STAIRS.add(stairs);
        }
        if (ArchitectureConfig.frames) {
            DAUB=wall(null,FrameFamily.INFILL);
            for (String wood : WOODS) for(FrameFamily family:FrameFamily.values()) {
                if(family==FrameFamily.INFILL) continue;
                FrameBlock frame=wall(wood,family);
                if(family==FrameFamily.POST) FRAMES.add(frame);
            }
            registerJetties();
            if(ArchitectureConfig.windows) registerWindows();
        }
        if (ArchitectureConfig.thatch) roofFamily("thatch", true);
        if (ArchitectureConfig.slate) roofFamily("slate", false);
        if (ArchitectureConfig.clayTiles) {
            roofFamily("clay_tile", false);
            CLAY_HANGING = block(new TileHangingBlock(false), "clay_tile_hanging");
            CLAY_HANGING_THIN = block(new TileHangingBlock(true), "clay_tile_hanging_thin");
            item(CLAY_HANGING, new TileHangingItem(CLAY_HANGING));
            item(CLAY_HANGING_THIN, new TileHangingItem(CLAY_HANGING_THIN));
            CLAY_FASCIA = block(new FasciaBlock(false), "clay_tile_fascia");
            item(CLAY_FASCIA, new EraItemBlock(CLAY_FASCIA));
            ROOFS.add(CLAY_FASCIA);
            CLAY_VERGE = roofTrim("clay_tile_verge", TrimmedRoofBlock.Kind.VERGE);
            CLAY_RIDGE_END = roofTrim("clay_tile_ridge_end", TrimmedRoofBlock.Kind.RIDGE_END);
            GUTTER = gutter("iron_gutter", false);
            GUTTER_OUTLET = gutter("iron_gutter_outlet", true);
            DOWNPIPE = block(new DownpipeBlock(DownpipeBlock.Kind.STRAIGHT), "iron_downpipe");
            item(DOWNPIPE, new DownpipeItem(DOWNPIPE));
            DOWNPIPE_OFFSET = block(new DownpipeBlock(DownpipeBlock.Kind.OFFSET), "iron_downpipe_offset");
            item(DOWNPIPE_OFFSET, new DownpipeItem(DOWNPIPE_OFFSET));
            DOWNPIPE_SHOE = block(new DownpipeBlock(DownpipeBlock.Kind.SHOE), "iron_downpipe_shoe");
            item(DOWNPIPE_SHOE, new DownpipeItem(DOWNPIPE_SHOE));
            // The lined fascia keeps the same orientation and gutter metadata as the bare one.
            CLAY_FASCIA_LINED = block(new FasciaBlock(true), "clay_tile_fascia_lined");
            ROOFS.add(CLAY_FASCIA_LINED);
            SOFFIT = new SoffitItem();
            SOFFIT.setRegistryName(new ResourceLocation(IronAgeArchitecture.MODID, "dark_oak_soffit"));
            SOFFIT.setUnlocalizedName(IronAgeArchitecture.MODID + ".dark_oak_soffit");
            GameRegistry.register(SOFFIT);
        }
        EraCatalog.assignPrimaryTabs();
    }
    private static GutterItem gutter(String name, boolean outlet) {
        GutterItem item = new GutterItem(outlet);
        item.setRegistryName(new ResourceLocation(IronAgeArchitecture.MODID, name));
        item.setUnlocalizedName(IronAgeArchitecture.MODID + "." + name);
        return GameRegistry.register(item);
    }
    private static void registerWindows() {
        WINDOW_LEAF=block(new WindowLeafBlock(),"open_window_leaf");
        LEADED_PANE=new LeadedPaneItem();LEADED_PANE.setRegistryName(new ResourceLocation(IronAgeArchitecture.MODID,"leaded_pane"));
        LEADED_PANE.setUnlocalizedName(IronAgeArchitecture.MODID+".leaded_pane");LEADED_PANE.setCreativeTab(EraTabs.tab(Era.LATE_MEDIEVAL_TUDOR));GameRegistry.register(LEADED_PANE);
        for(String wood:WOODS) for(int finish=0;finish<2;finish++) for(boolean thin:new boolean[]{false,true}) for(boolean diamond:new boolean[]{true,false}) for(boolean right:new boolean[]{false,true}) for(boolean back:new boolean[]{false,true}) {
            if(back&&!thin) continue;
            String name="leaded_window_"+(thin?"thin_":"full_")+FINISHES[finish]+"_"+(diamond?"diamond_":"rectangle_")+wood+(right?"_right":"")+(back?"_back":"");
            WindowBlock window=block(new WindowBlock(wood,finish,thin,diamond,right,back),name);WINDOWS.add(window);
            if(!right&&!back) item(window,new WindowItem(window));Blocks.FIRE.setFireInfo(window,5,20);
        }
    }
    public static WindowBlock findWindow(String wood,int finish,boolean thin,boolean diamond,boolean right) {
        return findWindow(wood,finish,thin,diamond,right,false);
    }
    public static WindowBlock findWindow(String wood,int finish,boolean thin,boolean diamond,boolean right,boolean back) {
        for(WindowBlock block:WINDOWS) if(block.wood.equals(wood)&&block.finish==finish&&block.thin==thin&&block.diamond==diamond&&block.right==right&&block.back==back) return block;
        throw new IllegalArgumentException("Unregistered window "+wood+" "+finish+" "+thin+" "+diamond+" "+right);
    }
    private static FrameBlock wall(String wood,FrameFamily family) {
        FrameBlock frame=block(new FrameBlock(wood,family),family.registryPrefix+(wood==null?"":"_"+wood));
        item(frame,new FrameItem(frame));WALLS.add(frame);
        if(wood!=null) Blocks.FIRE.setFireInfo(frame,5,20);
        return frame;
    }
    public static FrameBlock findWall(String wood,FrameFamily family) {
        for(FrameBlock block:WALLS) if(block.family==family&&java.util.Objects.equals(block.wood,wood)) return block;
        throw new IllegalArgumentException("Unregistered wall family "+wood+" "+family);
    }
    private static void registerJetties() {
        for(WallProfile profile:WallProfile.values()) {
            if(!profile.corner) partial(null,FrameFamily.INFILL,profile);
            for(String wood:WOODS) for(FrameFamily family:FrameFamily.values()) {
                if(family==FrameFamily.INFILL||profile.corner&&family!=FrameFamily.TERMINAL) continue;
                partial(wood,family,profile);
            }
        }
        for(String wood:WOODS) {
            for(FloorShape shape:FloorShape.values()) {
                floor(wood,shape,null);
                for(EnumFacing bracket:EnumFacing.HORIZONTALS) floor(wood,shape,bracket);
            }
            for(boolean full:new boolean[]{false,true}) {
                JettyBracketItem bracket=new JettyBracketItem(wood,full);
                String name="jetty_bracket_"+(full?"full_":"half_")+wood;
                bracket.setRegistryName(new ResourceLocation(IronAgeArchitecture.MODID,name));
                bracket.setUnlocalizedName(IronAgeArchitecture.MODID+"."+name);bracket.setCreativeTab(IronAgeArchitecture.TAB);
                GameRegistry.register(bracket);BRACKETS.add(bracket);
            }
        }
    }
    private static void partial(String wood,FrameFamily family,WallProfile profile) {
        String name="partial_"+profile.key()+"_"+family.registryPrefix+(wood==null?"":"_"+wood);
        PartialWallBlock wall=block(new PartialWallBlock(wood,family,profile),name);PARTIALS.add(wall);
        if(profile!=WallProfile.SHORT_RIGHT&&profile!=WallProfile.CORNER_HALF_RIGHT) item(wall,new FrameItem(wall));
        if(wood!=null) Blocks.FIRE.setFireInfo(wall,5,20);
    }
    private static void floor(String wood,FloorShape shape,EnumFacing bracket) {
        String name="jetty_floor_"+shape.key()+"_"+wood+(bracket==null?"":"_bracket_"+bracket.getName());
        JettyFloorBlock floor=block(new JettyFloorBlock(wood,shape,bracket),name);FLOORS.add(floor);
        if(bracket==null) item(floor,new FrameItem(floor));
        Blocks.FIRE.setFireInfo(floor,5,20);
    }
    public static PartialWallBlock findPartial(String wood,FrameFamily family,WallProfile profile) {
        for(PartialWallBlock block:PARTIALS) if(block.profile==profile&&block.family==family&&java.util.Objects.equals(wood,block.wood)) return block;
        throw new IllegalArgumentException("Unregistered partial wall "+wood+" "+family+" "+profile);
    }
    public static JettyFloorBlock findFloor(String wood,FloorShape shape,EnumFacing bracket) {
        for(JettyFloorBlock block:FLOORS) if(block.wood.equals(wood)&&block.shape==shape&&block.bracket==bracket) return block;
        throw new IllegalArgumentException("Unregistered jetty floor");
    }
    public static JettyBracketItem findBracket(String wood,boolean full) {
        for(JettyBracketItem item:BRACKETS) if(item.wood.equals(wood)&&item.fullReach==full) return item;
        throw new IllegalArgumentException("Unregistered bracket");
    }
    private static void roofFamily(String name, boolean flammable) {
        for (boolean ridge : new boolean[]{false, true}) {
            RoofBlock roof = block(ridge ? new RidgeBlock(name) : new RoofBlock(false, name), name + (ridge ? "_ridge" : "_roof"));
            item(roof, new EraItemBlock(roof));
            ROOFS.add(roof);
            if (flammable) Blocks.FIRE.setFireInfo(roof, 60, 20);
        }
    }
    private static TrimmedRoofBlock roofTrim(String name, TrimmedRoofBlock.Kind kind) {
        TrimmedRoofBlock trim = block(new TrimmedRoofBlock(kind), name);
        item(trim, new EraItemBlock(trim));
        ROOFS.add(trim);
        return trim;
    }
    private static final class CobBlock extends Block {
        CobBlock() { super(Material.GROUND); setHardness(1.2F); setResistance(4F); setSoundType(SoundType.GROUND); setHarvestLevel("shovel", 0); }
    }
}
