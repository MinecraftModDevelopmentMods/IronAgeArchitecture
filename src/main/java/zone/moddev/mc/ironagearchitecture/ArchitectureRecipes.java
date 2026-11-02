package zone.moddev.mc.ironagearchitecture;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;
import zone.moddev.mc.ironagearchitecture.block.FrameBlock;
import zone.moddev.mc.ironagearchitecture.block.FrameFamily;
import zone.moddev.mc.ironagearchitecture.block.RoofBlock;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Vanilla-only survival recipes, with explicit wood and finish identities. */
public final class ArchitectureRecipes {
    private ArchitectureRecipes() { }
    public static void register() {
        if (ArchitectureConfig.cob) {
            GameRegistry.addShapelessRecipe(new ItemStack(ArchitectureBlocks.COB.get(0),8),
                    Blocks.DIRT,Blocks.DIRT,Blocks.DIRT,Blocks.DIRT,Items.CLAY_BALL,Items.CLAY_BALL,Items.CLAY_BALL,Items.CLAY_BALL,Items.WHEAT);
            GameRegistry.addShapelessRecipe(new ItemStack(ArchitectureBlocks.COB.get(1)),ArchitectureBlocks.COB.get(0),new ItemStack(Items.DYE,1,15));
            for (int finish = 0; finish < 2; finish++) {
                Block cob = ArchitectureBlocks.COB.get(finish);
                GameRegistry.addRecipe(new ItemStack(ArchitectureBlocks.SLABS.get(finish),6),"CCC",'C',cob);
                GameRegistry.addRecipe(new ItemStack(ArchitectureBlocks.STAIRS.get(finish),4),"C  ","CC ","CCC",'C',cob);
            }
        }
        if (ArchitectureConfig.frames) for (int wood = 0; wood < 6; wood++) {
            FrameBlock frame = ArchitectureBlocks.FRAMES.get(wood);
            for (int finish = 0; finish < 2; finish++) {
                GameRegistry.addRecipe(new ItemStack(frame,4,finish * 2)," P ","PCP"," P ",
                        'P',new ItemStack(Blocks.PLANKS,1,wood),'C',ArchitectureBlocks.COB.get(finish));
                GameRegistry.addShapelessRecipe(new ItemStack(frame,1,finish * 2 + 1),
                        new ItemStack(frame,1,finish * 2),Items.STICK,Items.STICK);
            }
            FrameBlock rail=ArchitectureBlocks.findWall(frame.wood,FrameFamily.RAIL);
            FrameBlock stud=ArchitectureBlocks.findWall(frame.wood,FrameFamily.STUD);
            FrameBlock brace=ArchitectureBlocks.findWall(frame.wood,FrameFamily.BRACE);
            FrameBlock terminal=ArchitectureBlocks.findWall(frame.wood,FrameFamily.TERMINAL);
            for(int finish=0;finish<2;finish++) {
                int meta=finish*2;
                ItemStack post=new ItemStack(frame,1,meta);
                GameRegistry.addRecipe(new ItemStack(rail,2,meta),"PP",'P',post);
                cycle(rail,meta);
                GameRegistry.addShapelessRecipe(new ItemStack(stud,1,meta),post.copy());
                GameRegistry.addShapelessRecipe(new ItemStack(stud,1,meta+1),new ItemStack(stud,1,meta),Items.STICK);
                GameRegistry.addShapelessRecipe(new ItemStack(brace,1,meta),post.copy(),Items.STICK);
                cycle(brace,meta);
                GameRegistry.addRecipe(new ItemStack(terminal,3,meta),"P","P","P",'P',post.copy());
                cycle(terminal,meta);
            }
            for(FrameBlock wall:ArchitectureBlocks.WALLS) if(frame.wood.equals(wall.wood)) for(int pattern=0;pattern<2;pattern++)
                GameRegistry.addShapelessRecipe(new ItemStack(wall,1,2+pattern),new ItemStack(wall,1,pattern),new ItemStack(Items.DYE,1,15));
        }
        if(ArchitectureConfig.frames) {
            for(int finish=0;finish<2;finish++) GameRegistry.addShapelessRecipe(new ItemStack(ArchitectureBlocks.DAUB,1,finish),ArchitectureBlocks.COB.get(finish));
            GameRegistry.addShapelessRecipe(new ItemStack(ArchitectureBlocks.DAUB,1,1),new ItemStack(ArchitectureBlocks.DAUB,1,0),new ItemStack(Items.DYE,1,15));
            registerJetties();
        }
        for (RoofBlock roof : ArchitectureBlocks.ROOFS) {
            if (roof.isRidge()) continue;
            boolean thatch = roof.getRegistryName().getResourcePath().startsWith("thatch");
            Object material = thatch ? Items.WHEAT : Blocks.STONE;
            GameRegistry.addRecipe(new ItemStack(roof,6),"MMM","SSS",'M',material,'S',Items.STICK);
            Block ridge = Block.getBlockFromName(IronAgeArchitecture.MODID + ":" + (thatch ? "thatch_ridge" : "slate_ridge"));
            GameRegistry.addRecipe(new ItemStack(ridge,3),"RR",'R',roof);
        }
    }
    private static void cycle(FrameBlock block,int meta) {
        GameRegistry.addShapelessRecipe(new ItemStack(block,1,meta+1),new ItemStack(block,1,meta));
        GameRegistry.addShapelessRecipe(new ItemStack(block,1,meta),new ItemStack(block,1,meta+1));
    }
    private static void registerJetties() {
        for(FrameBlock full:ArchitectureBlocks.WALLS) {
            PartialWallBlock thin=ArchitectureBlocks.findPartial(full.wood,full.family,WallProfile.THIN);
            PartialWallBlock shortWall=ArchitectureBlocks.findPartial(full.wood,full.family,WallProfile.SHORT_LEFT);
            for(int meta=0;meta<full.variants();meta++) {
                int cut=full.family==FrameFamily.TERMINAL?full.finish(meta):meta;
                GameRegistry.addRecipe(new ItemStack(thin,4,cut),"W","W",'W',new ItemStack(full,1,meta));
            }
            for(int meta=0;meta<thin.variants();meta++) {
                GameRegistry.addRecipe(new ItemStack(shortWall,4,meta),"W","W",'W',new ItemStack(thin,1,meta));
                // Distinct horizontal recombination never shadows vertical cutting.
                GameRegistry.addRecipe(new ItemStack(thin,1,meta),"WW",'W',new ItemStack(shortWall,1,meta));
            }
            for(PartialWallBlock partial:new PartialWallBlock[]{thin,shortWall}) {
                for(int p=0;p<(partial.variants()==2?1:2);p++) GameRegistry.addShapelessRecipe(
                    new ItemStack(partial,1,p+(partial.variants()==2?1:2)),new ItemStack(partial,1,p),new ItemStack(Items.DYE,1,15));
                if(partial.variants()==4) for(int finish=0;finish<2;finish++) cycle(partial,finish*2);
            }
        }
        for(int wood=0;wood<6;wood++) {
            String timber=ArchitectureBlocks.WOODS[wood];
            PartialWallBlock end=ArchitectureBlocks.findPartial(timber,FrameFamily.TERMINAL,WallProfile.SHORT_LEFT);
            for(WallProfile profile:new WallProfile[]{WallProfile.CORNER_QUARTER,WallProfile.CORNER_HALF_LEFT,WallProfile.CORNER_L}) {
                PartialWallBlock corner=ArchitectureBlocks.findPartial(timber,FrameFamily.TERMINAL,profile);
                for(int finish=0;finish<2;finish++) {
                    ItemStack ingredient=new ItemStack(end,1,finish);
                    if(profile==WallProfile.CORNER_QUARTER) GameRegistry.addShapelessRecipe(new ItemStack(corner,1,finish*2),ingredient);
                    else if(profile==WallProfile.CORNER_HALF_LEFT) GameRegistry.addRecipe(new ItemStack(corner,1,finish*2),"S","S",'S',ingredient);
                    else GameRegistry.addRecipe(new ItemStack(corner,1,finish*2),"SS","S ",'S',ingredient);
                    cycle(corner,finish*2);
                }
                for(int pattern=0;pattern<2;pattern++) GameRegistry.addShapelessRecipe(new ItemStack(corner,1,2+pattern),new ItemStack(corner,1,pattern),new ItemStack(Items.DYE,1,15));
            }
            JettyFloorBlock full=ArchitectureBlocks.findFloor(timber,FloorShape.FULL,null),half=ArchitectureBlocks.findFloor(timber,FloorShape.HALF,null),
                quarter=ArchitectureBlocks.findFloor(timber,FloorShape.QUARTER,null),ell=ArchitectureBlocks.findFloor(timber,FloorShape.L,null);
            GameRegistry.addRecipe(new ItemStack(full,6),"PPP","SSS",'P',new ItemStack(Blocks.PLANKS,1,wood),'S',Items.STICK);
            GameRegistry.addRecipe(new ItemStack(half,4),"F","F",'F',full);
            GameRegistry.addRecipe(new ItemStack(quarter,4),"F","F",'F',half);
            GameRegistry.addRecipe(new ItemStack(full),"FF",'F',half);
            GameRegistry.addRecipe(new ItemStack(half),"FF",'F',quarter);
            GameRegistry.addRecipe(new ItemStack(ell),"QQ","Q ",'Q',quarter);
            GameRegistry.addShapelessRecipe(new ItemStack(quarter,3),ell);
            JettyBracketItem bracket=ArchitectureBlocks.findBracket(timber,true),small=ArchitectureBlocks.findBracket(timber,false);
            GameRegistry.addRecipe(new ItemStack(bracket,2),"P ","PS",'P',new ItemStack(Blocks.PLANKS,1,wood),'S',Items.STICK);
            GameRegistry.addShapelessRecipe(new ItemStack(small,2),bracket);
            GameRegistry.addRecipe(new ItemStack(bracket),"BB",'B',small);
        }
    }
}
