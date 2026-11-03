package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.block.*;

/** The first and third houses are glazed; the second is retained for comparison. */
final class WindowPreview {
    static void build(World world) {
        for(int x=-2;x<=42;x++) for(int z=21;z<=32;z++) world.setBlockState(new BlockPos(x,63,z),(z==25?net.minecraft.init.Blocks.COBBLESTONE:net.minecraft.init.Blocks.GRASS).getDefaultState(),2);
        house(world,0,"dark_oak",new double[]{0,.5},1);
        house(world,40,"dark_oak",new double[]{0,.5,1},1);
        house(world,62,"oak",new double[]{0,.5,1.5},0);
        for(int wood=0;wood<6;wood++) for(int pattern=0;pattern<2;pattern++) {
            int x=wood*6;for(int y=0;y<2;y++) {
                WindowBlock block=ArchitectureBlocks.findWindow(ArchitectureBlocks.WOODS[wood],1,true,pattern==0,pattern==1);
                world.setBlockState(new BlockPos(x+pattern*2,64+y,23),block.getDefaultState(),2);
            }
        }
        BlockPos border=new BlockPos(15,64,27);world.getChunkFromBlockCoords(border.east());
        for(int y=0;y<2;y++) {
            world.setBlockState(border.up(y),ArchitectureBlocks.findWindow("oak",0,false,true,false).getDefaultState().withProperty(WindowBlock.FACING,EnumFacing.EAST),2);
            world.setBlockState(border.south(2).up(y),ArchitectureBlocks.findWindow("oak",1,true,false,true).getDefaultState().withProperty(WindowBlock.FACING,EnumFacing.EAST),2);
        }
        if(!WindowController.set(world,WindowController.column(world,border),true,null,null,false)) throw new IllegalStateException("Border preview could not open");
    }
    private static void house(World world,int x,String wood,double[] offsets,int finish) {
        for(int storey=0;storey<offsets.length;storey++) {
            double front=-offsets[storey];int z=(int)Math.floor(front),base=64+storey*5;
            for(int column=0;column<2;column++) for(int y=0;y<2;y++) {
                boolean back=front-z>=.5;WindowBlock window=ArchitectureBlocks.findWindow(wood,finish,true,storey%2==0,column==1,back);
                world.setBlockState(new BlockPos(x+2+column,base+1+y,z),window.getDefaultState().withProperty(WindowBlock.FACING,EnumFacing.NORTH),2);
            }
            if(storey>0) WindowController.set(world,WindowController.column(world,new BlockPos(x+3,base+1,z)),true,null,null,false);
        }
    }
}
