package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Player-facing recipe contracts checked in initialized, packaged Forge. */
final class RecipeProbe {
    private static void require(boolean ok,String message) {if(!ok) throw new IllegalStateException(message);}
    private static boolean ours(ItemStack stack) {return stack!=null&&"ironagearchitecture".equals(stack.getItem().getRegistryName().getResourceDomain());}
    private static String key(ItemStack stack) {return stack.getItem().getRegistryName()+"@"+stack.getMetadata();}
    private static InventoryCrafting grid() {return new InventoryCrafting(new Container(){@Override public boolean canInteractWith(EntityPlayer player){return true;}},3,3);}
    private static List<ItemStack> inputs(IRecipe recipe) {
        List<ItemStack> result=new ArrayList<>();
        if(recipe instanceof ShapedRecipes) {for(ItemStack input:((ShapedRecipes)recipe).recipeItems) if(input!=null) result.add(input);}
        else if(recipe instanceof ShapelessRecipes) result.addAll(((ShapelessRecipes)recipe).recipeItems);
        else throw new IllegalStateException("Unexpected recipe "+recipe);
        return result;
    }
    private static void crafts(World world,ItemStack expected,ItemStack... slots) {
        InventoryCrafting grid=grid();for(int i=0;i<slots.length;i++) if(slots[i]!=null) grid.setInventorySlotContents(i,slots[i].copy());
        ItemStack actual=CraftingManager.getInstance().findMatchingRecipe(grid,world);
        require(ItemStack.areItemStacksEqual(expected,actual),"Crafting contract expected "+expected+" got "+actual);
    }
    static void verify(World world) {
        if(ArchitectureConfig.frames) for(int wood=0;wood<6;wood++) for(int finish=0;finish<2;finish++) {
            ItemStack plank=new ItemStack(Blocks.PLANKS,1,wood),infill=new ItemStack(ArchitectureBlocks.DAUB,1,finish);
            crafts(world,new ItemStack(ArchitectureBlocks.FRAMES.get(wood),4,finish*2),null,plank,null,plank,infill,plank,null,plank,null);
            ItemStack other=new ItemStack(Blocks.PLANKS,1,(wood+1)%6);
            crafts(world,null,null,other,null,plank,infill,plank,null,plank,null);
        }
        if(ArchitectureConfig.windows) for(WindowBlock window:ArchitectureBlocks.WINDOWS) if(!window.right&&!window.back) {
            ItemStack glazed=new ItemStack(window);
            crafts(world,null,glazed,null,null,glazed); // Full/thin wall cutting must not multiply glazing.
            crafts(world,null,glazed,glazed); // Rejoining glazed frames is deliberately unavailable too.
            crafts(world,null,glazed,new ItemStack(ArchitectureBlocks.LEADED_PANE,1,window.diamond?0:1));
            FrameBlock panel=window.thin?ArchitectureBlocks.findPartial(window.wood,FrameFamily.POST,WallProfile.THIN):ArchitectureBlocks.findWall(window.wood,FrameFamily.POST);
            crafts(world,null,new ItemStack(panel,1,window.finish*2+1),new ItemStack(ArchitectureBlocks.LEADED_PANE,1,window.diamond?0:1));
            if(window.finish==0) {
                String otherWood=window.wood.equals("oak")?"spruce":"oak";
                for(WindowBlock other:new WindowBlock[]{ArchitectureBlocks.findWindow(otherWood,0,window.thin,window.diamond,false),ArchitectureBlocks.findWindow(window.wood,1,window.thin,window.diamond,false),ArchitectureBlocks.findWindow(window.wood,0,!window.thin,window.diamond,false),ArchitectureBlocks.findWindow(window.wood,0,window.thin,!window.diamond,false)})
                    crafts(world,null,new ItemStack(other),glazed,glazed,glazed,new ItemStack(Items.DYE,1,15),glazed,glazed,glazed,glazed);
            }
        }
        int grids=0,pure=0;
        for(IRecipe recipe:CraftingManager.getInstance().getRecipeList()) if(ours(recipe.getRecipeOutput())) {
            List<ItemStack> ingredients=inputs(recipe);
            boolean conversion=true;Map<String,Integer> before=new TreeMap<>();
            for(ItemStack input:ingredients) {
                if(!ours(input)) {conversion=false;break;}
                resources(before,input,1);
            }
            if(conversion) {
                ItemStack output=recipe.getRecipeOutput();Map<String,Integer> after=new TreeMap<>();resources(after,output,output.stackSize);
                require(before.equals(after),"Material changes in conversion "+output+": "+before+" -> "+after);pure++;
            }
            if(recipe instanceof ShapedRecipes) {
                ShapedRecipes shaped=(ShapedRecipes)recipe;
                for(int y=0;y<=3-shaped.recipeHeight;y++) for(int x=0;x<=3-shaped.recipeWidth;x++) for(boolean mirror:new boolean[]{false,true}) {
                    InventoryCrafting grid=grid();
                    for(int row=0;row<shaped.recipeHeight;row++) for(int col=0;col<shaped.recipeWidth;col++) {
                        ItemStack input=shaped.recipeItems[(mirror?shaped.recipeWidth-1-col:col)+row*shaped.recipeWidth];
                        if(input!=null) grid.setInventorySlotContents(x+col+(y+row)*3,input.copy());
                    }
                    require(ItemStack.areItemStacksEqual(recipe.getRecipeOutput(),CraftingManager.getInstance().findMatchingRecipe(grid,world)),"Shifted/mirrored recipe shadowed "+recipe.getRecipeOutput());grids++;
                }
            } else for(int multiplier:new int[]{1,5}) {
                InventoryCrafting grid=grid();for(int i=0;i<ingredients.size();i++) grid.setInventorySlotContents(i*multiplier%9,ingredients.get(i).copy());
                require(ItemStack.areItemStacksEqual(recipe.getRecipeOutput(),CraftingManager.getInstance().findMatchingRecipe(grid,world)),"Reordered shapeless recipe shadowed "+recipe.getRecipeOutput());grids++;
            }
        }
        int reachable=verifyReachability();
        System.out.println("IAA_RECIPE_PASS grids="+grids+" conservedConversions="+pure+" reachableVariants="+reachable);
    }
    static int verifyReachability() {
        Set<String> reachable=new HashSet<>();boolean progress=true;
        while(progress) {
            progress=false;
            for(IRecipe recipe:CraftingManager.getInstance().getRecipeList()) if(ours(recipe.getRecipeOutput())) {
                boolean available=true;for(ItemStack input:inputs(recipe)) if(ours(input)&&!available(reachable,input)) {available=false;break;}
                if(available&&reachable.add(key(recipe.getRecipeOutput()))) progress=true;
            }
        }
        int count=0;
        for(ItemStack stack:ArchitectureBlocks.visibleStacks()) {boolean allowed=EraCatalog.canCraft(stack);require(reachable.contains(key(stack))==allowed,"Unreachable or excluded item "+stack);if(allowed) count++;}
        return count;
    }
    private static boolean available(Set<String> reachable,ItemStack input) {
        if(input.getMetadata()!=net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE) return reachable.contains(key(input));
        String prefix=input.getItem().getRegistryName()+"@";
        for(String candidate:reachable) if(candidate.startsWith(prefix)) return true;
        return false;
    }
    private static void add(Map<String,Integer> quantities,String material,int units) {quantities.put(material,quantities.getOrDefault(material,0)+units);}
    private static void resources(Map<String,Integer> quantities,ItemStack stack,int count) {
        if(stack.getItem() instanceof LeadedPaneItem) {add(quantities,"leaded-glazing",count);return;}
        Block block=Block.getBlockFromItem(stack.getItem());
        if(block instanceof WindowBlock) {WindowBlock window=(WindowBlock)block;add(quantities,"frame:"+window.wood+":"+window.finish,(window.thin?2:4)*count);add(quantities,"leaded-glazing",count);return;}
        if (block instanceof FireplaceBlock && ((FireplaceBlock)block).kind == FireplaceBlock.Kind.COLLAR)
            add(quantities, "roof:clay_tile", 4 * count);
        add(quantities,material(stack),units(stack)*count);
    }
    private static String material(ItemStack stack) {
        Item item=stack.getItem();
        if(item instanceof JettyBracketItem) return "floor:"+((JettyBracketItem)item).wood;
        Block block=Block.getBlockFromItem(item);
        if(block instanceof JettyFloorBlock) return "floor:"+((JettyFloorBlock)block).wood;
        if(block instanceof FrameBlock) {FrameBlock wall=(FrameBlock)block;return wall.family==FrameFamily.INFILL?"daub:"+wall.finish(stack.getMetadata()):"frame:"+wall.wood+":"+wall.finish(stack.getMetadata());}
        if(item instanceof GutterItem || block instanceof DownpipeBlock) return "iron_drain";
        if(block instanceof FireplaceBlock) return "fireplace:" + ((FireplaceBlock)block).masonry.key;
        if(block instanceof RoofBlock) return "roof:"+((RoofBlock)block).materialKey;
        if(block instanceof TileHangingBlock) return "roof:clay_tile";
        for(int finish=0;finish<ArchitectureBlocks.COB.size();finish++) if(ArchitectureBlocks.COB.get(finish)==block||ArchitectureBlocks.SLABS.get(finish)==block||ArchitectureBlocks.STAIRS.get(finish)==block) return "daub:"+finish;
        throw new IllegalStateException("Unknown recipe material "+stack);
    }
    private static int units(ItemStack stack) {
        Item item=stack.getItem();if(item instanceof JettyBracketItem) return ((JettyBracketItem)item).fullReach?2:1;
        Block block=Block.getBlockFromItem(item);
        if(block instanceof JettyFloorBlock) return Integer.bitCount(((JettyFloorBlock)block).shape.mask);
        if(block instanceof PartialWallBlock) {WallProfile p=((PartialWallBlock)block).profile;return p.corner?p.quarters():(p==WallProfile.THIN?2:1);}
        if(ArchitectureBlocks.SLABS.contains(block)) return 2;
        if(ArchitectureBlocks.STAIRS.contains(block)) return 3;
        if(block instanceof RoofBlock) return ((RoofBlock)block).isRidge()?2:4;
        if(block instanceof TileHangingBlock) return ((TileHangingBlock)block).isThin()?2:4;
        return 4;
    }
}
