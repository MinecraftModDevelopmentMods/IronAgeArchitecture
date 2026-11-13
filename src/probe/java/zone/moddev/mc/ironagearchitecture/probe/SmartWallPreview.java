package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;

/** The same smart item builds a cottage, half-depth jetty and full projecting townhouse. */
final class SmartWallPreview {
    static final BlockPos SPAWN=new BlockPos(-6,10,-8);
    private SmartWallPreview() { }

    static void build(WorldServer world) {
        for(int x=-35;x<=18;x++) for(int z=-36;z<=18;z++) {
            for(int y=3;y<9;y++) world.setBlockState(new BlockPos(x,y,z),Blocks.DIRT.getDefaultState(),2);
            world.setBlockState(new BlockPos(x,9,z),(z<0?Blocks.COBBLESTONE:Blocks.GRASS).getDefaultState(),2);
        }
        house(world,-25,7,7,3,0,"oak",0);
        annex(world);
        house(world,-15,9,9,4,1,"spruce",1);
        house(world,-3,11,9,4,2,"dark_oak",1);
        if(Boolean.getBoolean("iaa.surfacePreview")) surfaceSamples(world);
        for(int i=0;i<100&&SmartWallEvents.pending(world);i++) SmartWallEvents.flush(world);
    }

    private static void surfaceSamples(WorldServer world) {
        // Separate suspended samples expose the otherwise buried brace end caps.
        FrameBlock[] samples={ArchitectureBlocks.findWall("oak",FrameFamily.BRACE),
            ArchitectureBlocks.findWall("spruce",FrameFamily.POST),
            ArchitectureBlocks.findPartial("birch",FrameFamily.BRACE,WallProfile.THIN),
            ArchitectureBlocks.findPartial("dark_oak",FrameFamily.BRACE,WallProfile.SHORT_LEFT)};
        for(int i=0;i<samples.length;i++)
            world.setBlockState(new BlockPos(-24+i*3,12,14),samples[i].getDefaultState()
                .withProperty(FrameBlock.FACING,EnumFacing.NORTH).withProperty(FrameBlock.PANEL,i==1||i==2?3:0),2);
    }

    private static void house(WorldServer world,int start,int width,int depth,int height,int jetty,String wood,int finish) {
        int base=10, floor=base+height, upper=floor+1, roof=upper+height, front=jetty==0?0:-1;
        IBlockState deck=ArchitectureBlocks.findFloor(wood,FloorShape.FULL,null).getDefaultState();
        for(int x=0;x<width;x++) for(int z=0;z<depth;z++) {
            world.setBlockState(new BlockPos(start+x,base-1,z),Blocks.STONEBRICK.getDefaultState(),2);
            world.setBlockState(new BlockPos(start+x,floor,z),deck,2);
        }
        if(jetty!=0) for(int x=0;x<width;x++) {
            IBlockState edge=ArchitectureBlocks.findFloor(wood,jetty==1?FloorShape.HALF:FloorShape.FULL,null)
                .getDefaultState().withProperty(FrameBlock.FACING,jetty==1?EnumFacing.SOUTH:EnumFacing.NORTH);
            world.setBlockState(new BlockPos(start+x,floor,-1),edge,2);
        }
        for(int storey=0;storey<2;storey++) {
            int y0=storey==0?base:upper, z0=storey==0?0:front;
            boolean thin=storey==1&&jetty==1;
            for(int x=0;x<width;x++) for(int z=z0;z<depth;z++) {
                if(x!=0&&x!=width-1&&z!=z0&&z!=depth-1) continue;
                EnumFacing facing=z==z0?EnumFacing.NORTH:z==depth-1?EnumFacing.SOUTH:x==0?EnumFacing.WEST:EnumFacing.EAST;
                for(int y=0;y<height;y++) {
                    BlockPos at=new BlockPos(start+x,y0+y,z);
                    int profile=thin?1:0;
                    EnumFacing plane=thin?facing.getOpposite():facing;
                    if(thin&&(x==0||x==width-1)&&(z==z0||z==depth-1)) {
                        int mask=(x==0?10:5)|(z==z0?12:3);
                        placeCorner(world,at,wood,finish,mask);
                    } else world.setBlockState(at,ArchitectureBlocks.findSmart(wood,profile).getDefaultState()
                        .withProperty(FrameBlock.FACING,plane).withProperty(FrameBlock.PANEL,finish),2);
                }
            }
            int door=width/2;
            if(storey==0) for(int y=0;y<2;y++)
                world.setBlockState(new BlockPos(start+door,base+y,0),Blocks.SPRUCE_DOOR.getStateFromMeta(y==0?3:8),2);
            int[] windows=storey==1&&jetty==2?new int[]{2,7}:new int[]{1,width-3};
            for(int x:windows) for(int dx=0;dx<(width>=9?2:1);dx++) for(int y=1;y<Math.min(3,height-1);y++) {
                WindowBlock window=ArchitectureBlocks.findWindow(wood,finish,thin,true,dx!=0,thin);
                world.setBlockState(new BlockPos(start+x+dx,y0+y,z0),window.getDefaultState().withProperty(WindowBlock.FACING,EnumFacing.NORTH),2);
            }
            // A side window exercises the return and its independent facade.
            for(int y=1;y<Math.min(3,height-1);y++) {
                WindowBlock window=ArchitectureBlocks.findWindow(wood,finish,thin,false,false,thin);
                world.setBlockState(new BlockPos(start+width-1,y0+y,depth/2),window.getDefaultState().withProperty(WindowBlock.FACING,EnumFacing.EAST),2);
            }
        }
        if(jetty==2&&ArchitectureConfig.buildingMode.manual()) for(int y=upper;y<upper+height;y++)
            world.setBlockState(new BlockPos(start+5,y,-1),ArchitectureBlocks.findWall(wood,FrameFamily.POST)
                .getDefaultState().withProperty(FrameBlock.PANEL,finish*2).withProperty(FrameBlock.FACING,EnumFacing.NORTH),2);
        for(int x=0;x<width;x++) {
            int rise=Math.min(x,width-1-x);
            for(int y=0;y<rise;y++) for(int z:new int[]{front,depth-1})
                world.setBlockState(new BlockPos(start+x,roof+y,z),ArchitectureBlocks.findSmart(wood,0).getDefaultState()
                    .withProperty(FrameBlock.PANEL,finish),2);
        }
        RoofBlock slope=(RoofBlock)Block.getBlockFromName("ironagearchitecture:clay_tile_roof");
        RoofBlock ridge=(RoofBlock)Block.getBlockFromName("ironagearchitecture:clay_tile_ridge");
        for(int x=-1;x<=width;x++) for(int z=front-1;z<=depth;z++) {
            int rise=Math.min(x,width-1-x);
            boolean apex=x==width/2;
            EnumFacing facing=apex?EnumFacing.NORTH:x<width/2?EnumFacing.EAST:EnumFacing.WEST;
            RoofBlock piece=apex?ridge:slope;
            if(x==-1||x==width) piece=ArchitectureBlocks.CLAY_FASCIA;
            IBlockState state=piece.getDefaultState().withProperty(RoofBlock.FACING,facing);
            world.setBlockState(new BlockPos(start+x,roof+rise,z),state,2);
        }
        // A ladder gives ordinary players access to both floors.
        for(int y=base;y<=upper;y++) {
            BlockPos at=new BlockPos(start+1,y,depth-2);
            world.setBlockState(at.west(),Blocks.STONEBRICK.getDefaultState(),2);
            world.setBlockState(at,Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING,EnumFacing.EAST),2);
        }
    }

    private static void annex(WorldServer world) {
        IBlockState wall=ArchitectureBlocks.findSmart("oak",0).getDefaultState();
        RoofBlock slope=(RoofBlock)Block.getBlockFromName("ironagearchitecture:clay_tile_roof");
        for(int x=-18;x<=-17;x++) for(int z=4;z<=6;z++) {
            world.setBlockState(new BlockPos(x,9,z),Blocks.STONEBRICK.getDefaultState(),2);
            int top=-x-3;
            if(z==4||z==6||x==-17)
                for(int y=10;y<top;y++) world.setBlockState(new BlockPos(x,y,z),wall,2);
        }
        // A connected wall cell at the internal turn keeps the two framing runs in contact.
        for(int y=10;y<15;y++) world.setBlockState(new BlockPos(-19,y,4),wall,2);
        // Open the original rear side wall into the attached room.
        for(int y=10;y<13;y++) world.setBlockToAir(new BlockPos(-19,y,5));
        for(int x=-18;x<=-16;x++) for(int z=3;z<=7;z++) {
            RoofBlock piece=x==-16?ArchitectureBlocks.CLAY_FASCIA:slope;
            world.setBlockState(new BlockPos(x,-x-3,z),piece.getDefaultState().withProperty(RoofBlock.FACING,EnumFacing.WEST),2);
        }
    }

    private static void placeCorner(WorldServer world,BlockPos at,String wood,int finish,int mask) {
        SmartWallBlock corner=ArchitectureBlocks.findSmart(wood,2);
        for(EnumFacing facing:EnumFacing.HORIZONTALS) if(SmartWallGeometry.mask(2,facing)==mask) {
            world.setBlockState(at,corner.state(2,facing,finish),2);
            return;
        }
        throw new IllegalStateException("Missing smart corner orientation");
    }
}
