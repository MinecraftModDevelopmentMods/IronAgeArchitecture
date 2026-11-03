package zone.moddev.mc.ironagearchitecture;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Shared starters, lossless shaping and finish-preserving conversions. */
public final class ArchitectureRecipes {
    private ArchitectureRecipes() { }
    private static void shaped(ItemStack output,Object... inputs) {if(EraCatalog.canCraft(output)) GameRegistry.addRecipe(output,inputs);}
    private static void shapeless(ItemStack output,Object... inputs) {if(EraCatalog.canCraft(output)) GameRegistry.addShapelessRecipe(output,inputs);}
    private static ItemStack stack(Block block,int count,int meta) {return new ItemStack(block,count,meta);}
    private static ItemStack count(ItemStack stack,int count) {ItemStack result=stack.copy();result.stackSize=count;return result;}
    public static void register() {
        if(ArchitectureConfig.cob) registerCob();
        if(ArchitectureConfig.frames) {
            registerWalls();registerWallCuts();registerCorners();registerFloors();
            for(Block block:ArchitectureBlocks.VISIBLE) if(block instanceof FrameBlock&&!(block instanceof JettyFloorBlock)) {
                FrameBlock wall=(FrameBlock)block;
                int patterns=wall.variants()/2;
                for(int pattern=0;pattern<patterns;pattern++) wash(stack(wall,1,pattern),stack(wall,1,pattern+patterns));
                if(patterns==2) for(int finish=0;finish<2;finish++) flip(wall,finish*2);
            }
        }
        if(ArchitectureConfig.windows) registerWindows();
        for(RoofBlock roof:ArchitectureBlocks.ROOFS) if(!roof.isRidge()) {
            boolean thatch=roof.getRegistryName().getResourcePath().startsWith("thatch");
            shaped(new ItemStack(roof,6),"MMM","SSS",'M',thatch?Items.WHEAT:Blocks.STONE,'S',Items.STICK);
            Block ridge=Block.getBlockFromName(IronAgeArchitecture.MODID+":"+(thatch?"thatch_ridge":"slate_ridge"));
            cut(new ItemStack(roof),new ItemStack(ridge));join(new ItemStack(ridge),new ItemStack(roof));
        }
    }
    private static void registerCob() {
        shapeless(new ItemStack(ArchitectureBlocks.COB.get(0),8),Blocks.DIRT,Blocks.DIRT,Blocks.DIRT,Blocks.DIRT,
            Items.CLAY_BALL,Items.CLAY_BALL,Items.CLAY_BALL,Items.CLAY_BALL,Items.WHEAT);
        for(int finish=0;finish<2;finish++) {
            Block cob=ArchitectureBlocks.COB.get(finish),slab=ArchitectureBlocks.SLABS.get(finish),stairs=ArchitectureBlocks.STAIRS.get(finish);
            shaped(new ItemStack(slab,6),"CCC",'C',cob);
            shaped(new ItemStack(stairs,8),"C  ","CC ","CCC",'C',cob);
            join(new ItemStack(slab),new ItemStack(cob));
            shaped(new ItemStack(cob,3),"SS","SS",'S',stairs);
        }
        wash(new ItemStack(ArchitectureBlocks.COB.get(0)),new ItemStack(ArchitectureBlocks.COB.get(1)));
        wash(new ItemStack(ArchitectureBlocks.SLABS.get(0)),new ItemStack(ArchitectureBlocks.SLABS.get(1)));
        wash(new ItemStack(ArchitectureBlocks.STAIRS.get(0)),new ItemStack(ArchitectureBlocks.STAIRS.get(1)));
    }
    private static void registerWindows() {
        shaped(new ItemStack(ArchitectureBlocks.LEADED_PANE,8,0),"GGG","GIG","GGG",'G',Blocks.GLASS_PANE,'I',Items.IRON_INGOT);
        for(int pattern=0;pattern<2;pattern++) shapeless(new ItemStack(ArchitectureBlocks.LEADED_PANE,1,1-pattern),new ItemStack(ArchitectureBlocks.LEADED_PANE,1,pattern));
        for(WindowBlock window:ArchitectureBlocks.WINDOWS) if(!window.right&&!window.back) {
            FrameBlock panel=window.thin?ArchitectureBlocks.findPartial(window.wood,FrameFamily.POST,WallProfile.THIN):ArchitectureBlocks.findWall(window.wood,FrameFamily.POST);
            shapeless(new ItemStack(window),stack(panel,1,window.finish*2),new ItemStack(ArchitectureBlocks.LEADED_PANE,1,window.diamond?0:1));
            shapeless(new ItemStack(ArchitectureBlocks.findWindow(window.wood,window.finish,window.thin,!window.diamond,false)),new ItemStack(window));
            if(window.finish==0) wash(new ItemStack(window),new ItemStack(ArchitectureBlocks.findWindow(window.wood,1,window.thin,window.diamond,false)));
        }
    }
    private static void registerWalls() {
        for(int finish=0;finish<2;finish++) {
            shapeless(stack(ArchitectureBlocks.DAUB,1,finish),ArchitectureBlocks.COB.get(finish));
            shapeless(new ItemStack(ArchitectureBlocks.COB.get(finish)),stack(ArchitectureBlocks.DAUB,1,finish));
        }
        for(int wood=0;wood<6;wood++) for(int finish=0;finish<2;finish++) {
            String timber=ArchitectureBlocks.WOODS[wood];
            shaped(stack(ArchitectureBlocks.FRAMES.get(wood),4,finish*2)," P ","PDP"," P ",
                'P',new ItemStack(Blocks.PLANKS,1,wood),'D',stack(ArchitectureBlocks.DAUB,1,finish));
            // The same pattern rules apply to full walls, thin walls and short pieces.
            for(WallProfile profile:new WallProfile[]{null,WallProfile.THIN,WallProfile.SHORT_LEFT}) {
                FrameBlock post=wall(timber,FrameFamily.POST,profile);
                ItemStack base=stack(post,1,finish*2);
                for(FrameFamily family:new FrameFamily[]{FrameFamily.RAIL,FrameFamily.STUD,FrameFamily.BRACE,FrameFamily.TERMINAL}) {
                    FrameBlock target=wall(timber,family,profile);
                    int first=target.variants()==2?finish:finish*2;
                    ItemStack output=stack(target,3,first);
                    switch(family) {
                        case RAIL:shaped(output,"PPP",'P',base);break;
                        case STUD:shaped(output,"P","P","P",'P',base);break;
                        case BRACE:shaped(output,"P  "," P ","  P",'P',base);break;
                        case TERMINAL:shaped(output,"PP","P ",'P',base);break;
                        default:throw new IllegalArgumentException("Not a wall pattern");
                    }
                    // A row of three patterned pieces returns three base posts of the same size.
                    for(int pattern=0;pattern<target.variants()/2;pattern++)
                        shaped(count(base,3),"WWW",'W',stack(target,1,first+pattern));
                }
            }
        }
    }
    private static FrameBlock wall(String wood,FrameFamily family,WallProfile profile) {
        return profile==null?ArchitectureBlocks.findWall(wood,family):ArchitectureBlocks.findPartial(wood,family,profile);
    }
    private static void registerWallCuts() {
        for(FrameBlock full:ArchitectureBlocks.WALLS) {
            PartialWallBlock thin=ArchitectureBlocks.findPartial(full.wood,full.family,WallProfile.THIN);
            PartialWallBlock shortWall=ArchitectureBlocks.findPartial(full.wood,full.family,WallProfile.SHORT_LEFT);
            for(int meta=0;meta<full.variants();meta++)
                cut(stack(full,1,meta),stack(thin,1,full.family==FrameFamily.TERMINAL?full.finish(meta):meta));
            for(int meta=0;meta<thin.variants();meta++) {
                cut(stack(thin,1,meta),stack(shortWall,1,meta));
                join(stack(shortWall,1,meta),stack(thin,1,meta));
                // A thin terminal is an end post; the corner pattern remains a one-item swap.
                int restored=full.family==FrameFamily.TERMINAL?meta*2+1:meta;
                join(stack(thin,1,meta),stack(full,1,restored));
            }
        }
    }
    private static void registerCorners() {
        for(String timber:ArchitectureBlocks.WOODS) {
            PartialWallBlock end=ArchitectureBlocks.findPartial(timber,FrameFamily.TERMINAL,WallProfile.SHORT_LEFT);
            for(WallProfile profile:new WallProfile[]{WallProfile.CORNER_QUARTER,WallProfile.CORNER_HALF_LEFT,WallProfile.CORNER_L}) {
                PartialWallBlock corner=ArchitectureBlocks.findPartial(timber,FrameFamily.TERMINAL,profile);
                for(int finish=0;finish<2;finish++) {
                    ItemStack ingredient=stack(end,1,finish),output=stack(corner,1,finish*2);
                    if(profile==WallProfile.CORNER_QUARTER) shapeless(output,ingredient);
                    else if(profile==WallProfile.CORNER_HALF_LEFT) shaped(output,"E","E",'E',ingredient);
                    else shaped(output,"EE","E ",'E',ingredient);
                    for(int pattern=0;pattern<2;pattern++)
                        shaped(stack(end,2*profile.quarters(),finish),"CC",'C',stack(corner,1,finish*2+pattern));
                }
            }
        }
    }
    private static void registerFloors() {
        for(int wood=0;wood<6;wood++) {
            String timber=ArchitectureBlocks.WOODS[wood];
            ItemStack full=new ItemStack(ArchitectureBlocks.findFloor(timber,FloorShape.FULL,null));
            ItemStack half=new ItemStack(ArchitectureBlocks.findFloor(timber,FloorShape.HALF,null));
            ItemStack quarter=new ItemStack(ArchitectureBlocks.findFloor(timber,FloorShape.QUARTER,null));
            ItemStack ell=new ItemStack(ArchitectureBlocks.findFloor(timber,FloorShape.L,null));
            shaped(count(full,6),"PPP","SSS",'P',new ItemStack(Blocks.PLANKS,1,wood),'S',Items.STICK);
            cut(full,half);cut(half,quarter);join(half,full);join(quarter,half);
            shaped(ell,"QQ","Q ",'Q',quarter);shapeless(count(quarter,3),ell);
            ItemStack bracket=new ItemStack(ArchitectureBlocks.findBracket(timber,true)),small=new ItemStack(ArchitectureBlocks.findBracket(timber,false));
            shapeless(count(bracket,2),full,Items.STICK);
            shapeless(count(small,2),bracket);join(small,bracket);
        }
    }
    private static void cut(ItemStack large,ItemStack small) {shaped(count(small,4),"X","X",'X',large);}
    private static void join(ItemStack small,ItemStack large) {shaped(large,"XX",'X',small);}
    private static void flip(FrameBlock block,int first) {
        shapeless(stack(block,1,first+1),stack(block,1,first));shapeless(stack(block,1,first),stack(block,1,first+1));
    }
    private static void wash(ItemStack raw,ItemStack washed) {
        ItemStack lime=new ItemStack(Items.DYE,1,15);
        shapeless(washed,raw,lime);
        shaped(count(washed,8),"RRR","RLR","RRR",'R',raw,'L',lime);
    }
}
