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
    public static List<FrameBlock> meshes() {
        List<FrameBlock> result=new ArrayList<>(WALLS);result.addAll(PARTIALS);result.addAll(FLOORS);return result;
    }
    public static FrameBlock DAUB;
    public static final List<RoofBlock> ROOFS = new ArrayList<>();
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
            item(full, new ItemBlock(full));
            COB.add(full);
            CobSlab half = block(new CobSlab(false), finish + "_slab");
            CobSlab doubled = block(new CobSlab(true), finish + "_double_slab");
            half.setHalf(half); doubled.setHalf(half);
            item(half, new ItemSlab(half, half, doubled));
            SLABS.add(half);
            CobStairs stairs = block(new CobStairs(full.getDefaultState()), finish + "_stairs");
            item(stairs, new ItemBlock(stairs));
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
        }
        if (ArchitectureConfig.thatch) roofFamily("thatch", true);
        if (ArchitectureConfig.slate) roofFamily("slate", false);
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
            RoofBlock roof = block(ridge ? new RidgeBlock(flammable) : new RoofBlock(false, flammable), name + (ridge ? "_ridge" : "_roof"));
            item(roof, new ItemBlock(roof));
            ROOFS.add(roof);
            if (flammable) Blocks.FIRE.setFireInfo(roof, 60, 20);
        }
    }
    private static final class CobBlock extends Block {
        CobBlock() { super(Material.GROUND); setHardness(1.2F); setResistance(4F); setSoundType(SoundType.GROUND); setHarvestLevel("shovel", 0); }
    }
}
