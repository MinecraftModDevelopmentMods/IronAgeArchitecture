package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.block.FrameBlock;
import zone.moddev.mc.ironagearchitecture.block.FrameFamily;
import zone.moddev.mc.ironagearchitecture.block.RoofBlock;

/** Two elevations and a corner lab; uses existing vanilla masonry/window blocks only. */
final class DaubPreview {
    void build(WorldServer world) {
        for(int x=-10;x<=64;x++) for(int z=-12;z<=32;z++) {
            world.setBlockState(new BlockPos(x,63,z),Blocks.GRASS.getDefaultState(),2);
            for(int y=64;y<=80;y++) world.setBlockToAir(new BlockPos(x,y,z));
        }
        house(world,0,"dark_oak",true);
        house(world,18,"spruce",false);
        // The same L-shaped corner in four rotations, including cap/rail intersections.
        for(EnumFacing facing:EnumFacing.HORIZONTALS) {
            int ox=38+facing.getHorizontalIndex()*6;
            BlockPos origin=new BlockPos(ox,65,3);
            for(int y=0;y<3;y++) put(world,origin.up(y),"dark_oak",FrameFamily.TERMINAL,2,facing);
            for(int distance=1;distance<=3;distance++) for(int y=0;y<3;y++) {
                FrameFamily family=y==1?FrameFamily.INFILL:FrameFamily.RAIL;
                int panel=y==1?1:y==0?2:3;
                put(world,origin.offset(facing.rotateY(),distance).up(y),"dark_oak",family,panel,facing);
                put(world,origin.offset(facing.getOpposite(),distance).up(y),"dark_oak",family,panel,facing.rotateYCCW());
            }
        }
        // Raw/washed module catalog with a small gap between each material.
        for(int wood=0;wood<6;wood++) {
            int index=0;
            for(FrameFamily family:FrameFamily.values()) {
                if(family==FrameFamily.INFILL) continue;
                for(int pattern=0;pattern<2;pattern++) for(int y=0;y<3;y++)
                    put(world,new BlockPos(wood*4,65+y,17+index*2+pattern),ArchitectureBlocks.WOODS[wood],family,2+pattern,EnumFacing.WEST);
                index++;
            }
        }
    }
    private void put(WorldServer world,BlockPos pos,String wood,FrameFamily family,int panel,EnumFacing facing) {
        FrameBlock block=family==FrameFamily.INFILL?ArchitectureBlocks.DAUB:ArchitectureBlocks.findWall(wood,family);
        world.setBlockState(pos,block.getDefaultState().withProperty(FrameBlock.PANEL,panel).withProperty(BlockHorizontal.FACING,facing),2);
    }
    private void house(WorldServer world,int ox,String wood,boolean broadBays) {
        int width=9,depth=8;
        for(int x=0;x<=width;x++) for(int z=0;z<=depth;z++) {
            world.setBlockState(new BlockPos(ox+x,64,z),Blocks.STONEBRICK.getDefaultState(),2);
            if(x!=0&&x!=width&&z!=0&&z!=depth) continue;
            for(int y=65;y<=67;y++) {
                IBlockState state=Blocks.STONEBRICK.getDefaultState();
                if(z==0&&(x==2||x==7)&&y==66) state=Blocks.GLASS.getDefaultState();
                if(z==0&&(x==4||x==5)&&y<=66) state=Blocks.AIR.getDefaultState();
                world.setBlockState(new BlockPos(ox+x,y,z),state,2);
            }
            for(int y=68;y<=71;y++) {
                boolean corner=(x==0||x==width)&&(z==0||z==depth);
                EnumFacing facing=z==0?EnumFacing.NORTH:z==depth?EnumFacing.SOUTH:x==0?EnumFacing.WEST:EnumFacing.EAST;
                if(corner) facing=x==0?(z==0?EnumFacing.NORTH:EnumFacing.WEST):(z==0?EnumFacing.EAST:EnumFacing.SOUTH);
                int along=z==0||z==depth?x:z;
                FrameFamily family=corner?FrameFamily.TERMINAL:along%3==0?FrameFamily.POST:y==68||y==71?FrameFamily.RAIL:
                        broadBays?FrameFamily.INFILL:FrameFamily.STUD;
                int panel=family==FrameFamily.INFILL?1:family==FrameFamily.RAIL?(y==68?2:3):family==FrameFamily.STUD?3:2;
                put(world,new BlockPos(ox+x,y,z),wood,family,panel,facing);
            }
        }
        // Two deliberate single diagonals continue across 2x2 bays, away from the floor rails.
        if(broadBays) {
            put(world,new BlockPos(ox+1,69,0),wood,FrameFamily.BRACE,2,EnumFacing.NORTH);
            put(world,new BlockPos(ox+2,70,0),wood,FrameFamily.BRACE,2,EnumFacing.NORTH);
            put(world,new BlockPos(ox+8,69,0),wood,FrameFamily.BRACE,3,EnumFacing.NORTH);
            put(world,new BlockPos(ox+7,70,0),wood,FrameFamily.BRACE,3,EnumFacing.NORTH);
            // A broad two-block window demonstrates surrounding posts and chosen rails.
            for(int x=4;x<=5;x++) world.setBlockState(new BlockPos(ox+x,69,0),Blocks.GLASS.getDefaultState(),2);
        }
        RoofBlock roof=ArchitectureBlocks.ROOFS.stream().filter(b->b.getRegistryName().getResourcePath().equals("slate_roof")).findFirst().get();
        // Even-width building: two full-rise slopes meet at their common high edge.
        for(int z=-1;z<=depth+1;z++) for(int x=-1;x<=width+1;x++) {
            int rise=Math.min(x+1,width+1-x);
            EnumFacing facing=x<=4?EnumFacing.EAST:EnumFacing.WEST;
            world.setBlockState(new BlockPos(ox+x,72+rise,z),roof.getDefaultState().withProperty(BlockHorizontal.FACING,facing),2);
        }
        // Existing infill fills the gables; studs and braces give their pattern a larger scale.
        for(int z:new int[]{0,depth}) for(int x=0;x<=width;x++) {
            int rise=Math.min(x+1,width+1-x);
            for(int y=72;y<72+rise;y++) put(world,new BlockPos(ox+x,y,z),wood,x==3||x==6?FrameFamily.POST:FrameFamily.INFILL,x==3||x==6?2:1,z==0?EnumFacing.NORTH:EnumFacing.SOUTH);
        }
    }
}
