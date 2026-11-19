package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Disposable player-buildable rough roofs over the three released cob layouts. */
final class SmartThatchPreview {
    static final BlockPos SPAWN=RoundedCobPreview.SPAWN;
    static final Set<BlockPos> MATERIAL=new HashSet<>();
    private SmartThatchPreview() { }
    static void roof(WorldServer world,RoundedCobPreview.House house) {
        Set<Long> footprint=new HashSet<>(house.footprint);
        for(long key:house.footprint)for(int[] d:new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}})footprint.add(SmartCobGeometry.key(SmartCobGeometry.x(key)+d[0],SmartCobGeometry.z(key)+d[1]));
        for(long key:footprint) {
            int x=SmartCobGeometry.x(key),z=SmartCobGeometry.z(key),distance=0;
            outer:for(int radius=1;radius<=24;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)if(Math.max(Math.abs(dx),Math.abs(dz))==radius&&!footprint.contains(SmartCobGeometry.key(x+dx,z+dz))){distance=radius-1;break outer;}
            boolean gable=house.x==-69;int rise=gable?Math.min(z+1,7-z):distance;BlockPos at=new BlockPos(house.x+x,9+house.height+rise,house.z+z);if(world.isAirBlock(at)||world.getBlockState(at).getBlock()==ArchitectureBlocks.SMART_THATCH_PART){world.setBlockState(at,ArchitectureBlocks.SMART_THATCH.getDefaultState(),2);MATERIAL.add(at);}
        }
        if(house.x==-69)for(int x:new int[]{0,8})for(int z=0;z<7;z++)for(int y=10+house.height;y<10+house.height+Math.min(z,6-z);y++)world.setBlockState(new BlockPos(house.x+x,y,house.z+z),ArchitectureBlocks.SMART_COB.getStateFromMeta(1),2);
    }
    static void build(WorldServer world) {
        MATERIAL.clear();for(int x=-90;x<=20;x++)for(int z=-55;z<=40;z++)for(int y=4;y<=9;y++)world.setBlockState(new BlockPos(x,y,z),(y==9?(z<0?Blocks.GRAVEL:Blocks.GRASS):Blocks.DIRT).getDefaultState(),2);
        RoundedCobPreview.skipRoofs=true;try{RoundedCobPreview.build(world);}finally{RoundedCobPreview.skipRoofs=false;}
        // Keep the floor level with the existing door thresholds for walking inside.
        for(RoundedCobPreview.House house:RoundedCobPreview.HOUSES)for(long cell:house.footprint){BlockPos at=new BlockPos(house.x+SmartCobGeometry.x(cell),10,house.z+SmartCobGeometry.z(cell));if(world.getBlockState(at).getBlock()==Blocks.PLANKS){world.setBlockToAir(at);world.setBlockState(at.down(),Blocks.PLANKS.getStateFromMeta(1),2);}}
        for(RoundedCobPreview.House house:RoundedCobPreview.HOUSES)roof(world,house);
        if(ArchitectureConfig.fireplaces) {
            FireplaceBlock fire=ArchitectureBlocks.fireplace(FireplaceBlock.Masonry.BRICK,FireplaceBlock.Kind.FIREBOX),flue=ArchitectureBlocks.fireplace(FireplaceBlock.Masonry.BRICK,FireplaceBlock.Kind.FLUE),cap=ArchitectureBlocks.fireplace(FireplaceBlock.Masonry.BRICK,FireplaceBlock.Kind.POT_CAP);
            int collarY=0;for(BlockPos p:MATERIAL)if(p.getX()==-20&&p.getZ()==9)collarY=Math.max(collarY,p.getY());
            BlockPos base=new BlockPos(-20,10,9);world.setBlockState(base,fire.getDefaultState().withProperty(FireplaceBlock.LIT,true),2);for(int y=11;y<25;y++){BlockPos at=new BlockPos(-20,y,9);IBlockState state=flue.getDefaultState();if(y==collarY&&ArchitectureConfig.clayTiles)state=ArchitectureBlocks.fireplace(FireplaceBlock.Masonry.BRICK,FireplaceBlock.Kind.COLLAR).getDefaultState().withProperty(FireplaceBlock.FACING,EnumFacing.WEST);world.setBlockState(at,state,2);MATERIAL.remove(at);}world.setBlockState(new BlockPos(-20,25,9),cap.getDefaultState(),2);
            if(FireplaceDraft.smokeOutlet(world,new BlockPos(-20,25,9),world.getBlockState(new BlockPos(-20,25,9)))==null)throw new IllegalStateException("Preview chimney has no draft");
        }
        Block old=Block.getBlockFromName("ironagearchitecture:thatch_roof"),ridge=Block.getBlockFromName("ironagearchitecture:thatch_ridge");
        if(old instanceof RoofBlock&&ridge instanceof RoofBlock) {
            for(int x=-84;x<=-80;x++)for(int z=-12;z<=-8;z++){world.setBlockState(new BlockPos(x,9,z),Blocks.STONEBRICK.getDefaultState(),2);for(int y=10;y<13;y++)if(x==-84||x==-80||z==-12||z==-8)world.setBlockState(new BlockPos(x,y,z),ArchitectureBlocks.COB.get(1).getDefaultState(),2);}
            for(int x=-1;x<=5;x++)for(int z=-1;z<=5;z++){int dx=Math.min(x,4-x),dz=Math.min(z,4-z),rise=Math.min(dx,dz);boolean top=dx==2&&dz>=2||dz==2&&dx>=2;RoofBlock piece=(RoofBlock)(top?ridge:old);EnumFacing facing=top?EnumFacing.EAST:dx<dz?(x<2?EnumFacing.EAST:EnumFacing.WEST):(z<2?EnumFacing.SOUTH:EnumFacing.NORTH);world.setBlockState(new BlockPos(-84+x,13+rise,-12+z),piece.getDefaultState().withProperty(RoofBlock.FACING,facing),2);}
        }
        ProbeShapes.flush(world);SmartThatchProbe.flush(world);
        for(BlockPos owner:MATERIAL)if(!SmartThatchBlock.owner(world.getBlockState(owner))||((SmartThatchBlock)world.getBlockState(owner).getBlock()).mesh(world,owner).faces.isEmpty())throw new IllegalStateException("Preview roof missing at "+owner);
        int helpers=0;for(BlockPos at:MATERIAL)if(SmartThatchBlock.active(world.getBlockState(at.down())))helpers++;
        for(BlockPos entrance:new BlockPos[]{new BlockPos(-65,10,0),new BlockPos(-40,10,0),new BlockPos(-18,10,0)}) {
            ((net.minecraft.block.BlockDoor)Blocks.SPRUCE_DOOR).toggleDoor(world,entrance,true);
            for(int step=0;step<5;step++){double z=entrance.getZ()-.5+step*.75;net.minecraft.util.math.AxisAlignedBB body=new net.minecraft.util.math.AxisAlignedBB(entrance.getX()+.2,10,z-.3,entrance.getX()+.8,11.8,z+.3);if(!world.getCollisionBoxes(null,body).isEmpty())throw new IllegalStateException("Preview entrance blocks a normal player at "+entrance+" step "+step);}
            ((net.minecraft.block.BlockDoor)Blocks.SPRUCE_DOOR).toggleDoor(world,entrance,false);
        }
        if(ArchitectureConfig.windows)for(int x:new int[]{-68,-63}){BlockPos at=new BlockPos(x,11,0);if(world.getBlockState(at).getBlock() instanceof WindowBlock){if(!WindowController.toggle(world,at,null)||!world.getBlockState(at).getValue(WindowBlock.OPEN)||!WindowController.toggle(world,at,null))throw new IllegalStateException("Casement failed below shaped eaves");}}
        System.out.println("IAA_THATCH_PREVIEW_READY material="+MATERIAL.size()+" helpers="+helpers);
    }
}
