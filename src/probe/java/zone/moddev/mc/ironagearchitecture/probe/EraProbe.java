package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.inventory.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.FrameBlock;

/** Runs against Forge's actual crafting manager and parsed config files. */
final class EraProbe {
    private static void require(boolean condition,String message) {if(!condition) throw new IllegalStateException(message);}
    static int expectedRecipes() {
        return ArchitectureConfig.survivalEras.allowed.contains(Era.LATE_MEDIEVAL_TUDOR)?(ArchitectureConfig.windows?2010:1863)+(ArchitectureConfig.clayTiles?19:0):
            ArchitectureConfig.survivalEras.allowed.contains(Era.IRON_AGE)?36:0;
    }
    static int expectedVariants() { return (ArchitectureConfig.windows ? 510 : 460) + (ArchitectureConfig.clayTiles ? 15 : 0); }
    private static boolean ours(IRecipe recipe) {
        ItemStack output=recipe.getRecipeOutput();
        return output!=null&&"ironagearchitecture".equals(output.getItem().getRegistryName().getResourceDomain());
    }
    private static InventoryCrafting grid(IRecipe recipe) {
        InventoryCrafting grid=new InventoryCrafting(new Container(){@Override public boolean canInteractWith(EntityPlayer player){return true;}},3,3);
        if(recipe instanceof ShapedRecipes) {
            ShapedRecipes r=(ShapedRecipes)recipe;
            for(int y=0;y<r.recipeHeight;y++) for(int x=0;x<r.recipeWidth;x++) {
                ItemStack input=r.recipeItems[x+y*r.recipeWidth];if(input!=null) grid.setInventorySlotContents(x+y*3,input.copy());
            }
        } else if(recipe instanceof ShapelessRecipes) {
            List<ItemStack> inputs=((ShapelessRecipes)recipe).recipeItems;
            for(int i=0;i<inputs.size();i++) grid.setInventorySlotContents(i,inputs.get(i).copy());
        } else throw new IllegalStateException("Unexpected recipe "+recipe);
        return grid;
    }
    static void verify(World world) throws Exception {
        List<IRecipe> recipes=CraftingManager.getInstance().getRecipeList();
        List<IRecipe> original=new ArrayList<>(recipes);
        EraSelection selected=ArchitectureConfig.survivalEras;
        boolean cob=ArchitectureConfig.cob,frames=ArchitectureConfig.frames,thatch=ArchitectureConfig.thatch,slate=ArchitectureConfig.slate,windows=ArchitectureConfig.windows;
        boolean clayTiles = ArchitectureConfig.clayTiles;
        String[][] cases={{"all"},{"iron_age"},{"late_medieval_tudor"},{"iron_age","late_medieval_tudor"},{},{"typo"},{"iron_age","typo"},{" IRON_AGE ","iron_age"}};
        try {
            recipes.removeIf(EraProbe::ours);ArchitectureConfig.survivalEras=EraSelection.parse("all");ArchitectureRecipes.register();
            List<IRecipe> baseline=new ArrayList<>();for(IRecipe recipe:recipes) if(ours(recipe)) baseline.add(recipe);
            require(baseline.size()==expectedRecipes(),"Era baseline "+baseline.size());
            Files.createDirectories(Paths.get("era-matrix"));
            for(int c=0;c<cases.length;c++) {
                Path file=Paths.get("era-matrix",c+".cfg");
                String text="blocks {\n B:leadedWindows="+windows+"\n B:clayTiles="+clayTiles+"\n}\nrecipes {\n S:survivalEras <\n"+String.join("\n",cases[c])+"\n >\n}\n";
                Files.write(file,text.getBytes(StandardCharsets.UTF_8));ArchitectureConfig.load(file.toFile());
                recipes.removeIf(EraProbe::ours);ArchitectureRecipes.register();
                int count=0;for(IRecipe recipe:recipes) if(ours(recipe)) {require(EraCatalog.canCraft(recipe.getRecipeOutput()),"Prohibited registered output");count++;}
                require(count==expectedRecipes(),"Era recipe count case "+c+": "+count);
                for(IRecipe recipe:baseline) {
                    ItemStack expected=recipe.getRecipeOutput(),actual=CraftingManager.getInstance().findMatchingRecipe(grid(recipe),world);
                    if(EraCatalog.canCraft(expected)) require(actual!=null&&ItemStack.areItemStacksEqual(actual,expected),"Allowed recipe unavailable case "+c+" "+expected);
                    else require(actual==null||!"ironagearchitecture".equals(actual.getItem().getRegistryName().getResourceDomain())||EraCatalog.canCraft(actual),"Restricted output crafted case "+c);
                }
                int iron=0,late=0;
                for(ItemStack stack:ArchitectureBlocks.visibleStacks()) {
                    if(EraCatalog.eras(stack.getItem()).contains(Era.IRON_AGE)) iron++;
                    if(EraCatalog.eras(stack.getItem()).contains(Era.LATE_MEDIEVAL_TUDOR)) late++;
                }
                require(iron==14&&late==expectedVariants(),"Creative palette changed with recipe filter");
                int expected=ArchitectureConfig.survivalEras.allowed.contains(Era.LATE_MEDIEVAL_TUDOR)?expectedVariants():
                    ArchitectureConfig.survivalEras.allowed.contains(Era.IRON_AGE)?14:0;
                require(RecipeProbe.verifyReachability()==expected,"Era selection leaves unreachable content case "+c);
            }
            System.out.println("IAA_ERA_MATRIX_PASS cases="+cases.length+" sharedVariants=14 lateVariants="+expectedVariants());
        } finally {
            recipes.clear();recipes.addAll(original);ArchitectureConfig.survivalEras=selected;
            ArchitectureConfig.cob=cob;ArchitectureConfig.frames=frames;ArchitectureConfig.thatch=thatch;ArchitectureConfig.slate=slate;ArchitectureConfig.windows=windows;
            ArchitectureConfig.clayTiles = clayTiles;
        }
    }
}
