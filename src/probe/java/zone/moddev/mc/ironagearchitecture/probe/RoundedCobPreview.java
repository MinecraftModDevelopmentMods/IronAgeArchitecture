package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Three player-buildable layouts; the roof pieces remain the ordinary existing kit. */
final class RoundedCobPreview {
    static final BlockPos SPAWN=new BlockPos(-39,10,-14);
    static final List<BlockPos> ROOF=new ArrayList<>();
    static final Map<BlockPos,IBlockState> ROOF_STATES=new HashMap<>();
    private RoundedCobPreview() { }
    static void build(WorldServer world) {
        for(int x=-77;x<=9;x++)for(int z=-25;z<=29;z++) {
            world.setBlockState(new BlockPos(x,8,z),Blocks.DIRT.getDefaultState(),2);
            world.setBlockState(new BlockPos(x,9,z),(z<0?Blocks.GRAVEL:Blocks.GRASS).getDefaultState(),2);
        }
        Set<Long> rectangle=new HashSet<>();for(int x=0;x<9;x++)for(int z=0;z<7;z++)rectangle.add(SmartCobGeometry.key(x,z));
        house(world,-69,0,rectangle,1,4,1,4,new int[]{1,6},false);
        Set<Long> oval=new HashSet<>();for(int x=0;x<15;x++)for(int z=0;z<11;z++)if(Math.pow((x+.5-7.5)/7.5,2)+Math.pow((z+.5-5.5)/5.5,2)<=1)oval.add(SmartCobGeometry.key(x,z));
        house(world,-47,0,oval,2,5,0,7,new int[]{4,10},true);
        Set<Long> lShape=new HashSet<>();for(int x=0;x<15;x++)for(int z=0;z<13;z++)
            if(x<11&&z<9||x<5&&z<13||Math.pow(x+.5-10,2)+Math.pow(z+.5-6,2)<=3.5*3.5)lShape.add(SmartCobGeometry.key(x,z));
        house(world,-23,0,lShape,2,8,1,5,new int[]{2,8},true);
        // Same-size ordinary and smart limewashed corners make the geometry difference clear.
        for(int i=0;i<5;i++)for(int y=10;y<13;y++) {
            world.setBlockState(new BlockPos(-68+i,y,-12),ArchitectureBlocks.COB.get(1).getDefaultState(),2);
            world.setBlockState(new BlockPos(-68,y,-12+i),ArchitectureBlocks.COB.get(1).getDefaultState(),2);
            world.setBlockState(new BlockPos(-58+i,y,-12),ArchitectureBlocks.SMART_COB.getStateFromMeta(1),2);
            world.setBlockState(new BlockPos(-58,y,-12+i),ArchitectureBlocks.SMART_COB.getStateFromMeta(1),2);
        }
        for(int i=0;i<30&&SmartCobEvents.pending(world);i++)SmartCobEvents.flush(world);
        if(!(world.getBlockState(new BlockPos(-65,10,0)).getBlock() instanceof BlockDoor))throw new IllegalStateException("Preview cottage door is missing");
        if(SmartCobData.get(world).profile(new BlockPos(-61,11,0)).area().contains(.92,.08))throw new IllegalStateException("Preview front corner did not soften");
    }
    private static void house(WorldServer world,int sx,int sz,Set<Long> footprint,int thickness,int height,int finish,int door,int[] windows,boolean glass) {
        Set<Long> walls=new HashSet<>();int width=0,depth=0;
        for(long cell:footprint) {
            int x=SmartCobGeometry.x(cell),z=SmartCobGeometry.z(cell);width=Math.max(width,x+1);depth=Math.max(depth,z+1);
            boolean edge=false;for(int dx=-thickness;dx<=thickness;dx++)for(int dz=-thickness;dz<=thickness;dz++)
                if(Math.abs(dx)+Math.abs(dz)<=thickness&&!footprint.contains(SmartCobGeometry.key(x+dx,z+dz)))edge=true;
            if(edge)walls.add(cell);
            world.setBlockState(new BlockPos(sx+x,9,sz+z),Blocks.STONEBRICK.getDefaultState(),2);
            world.setBlockState(new BlockPos(sx+x,10,sz+z),Blocks.PLANKS.getStateFromMeta(1),2);
            if(height>6&&!edge)world.setBlockState(new BlockPos(sx+x,14,sz+z),Blocks.PLANKS.getStateFromMeta(1),2);
        }
        for(long cell:walls)for(int y=10;y<10+height;y++)world.setBlockState(new BlockPos(sx+SmartCobGeometry.x(cell),y,sz+SmartCobGeometry.z(cell)),ArchitectureBlocks.SMART_COB.getStateFromMeta(finish),2);
        for(int z=0;z<=thickness;z++)for(int y=10;y<12;y++)world.setBlockToAir(new BlockPos(sx+door,y,sz+z));
        IBlockState doorState=Blocks.SPRUCE_DOOR.getDefaultState().withProperty(BlockDoor.FACING,EnumFacing.NORTH);
        world.setBlockState(new BlockPos(sx+door,10,sz),doorState.withProperty(BlockDoor.HALF,BlockDoor.EnumDoorHalf.LOWER),2);
        world.setBlockState(new BlockPos(sx+door,11,sz),doorState.withProperty(BlockDoor.HALF,BlockDoor.EnumDoorHalf.UPPER),2);
        for(int x:windows)for(int y:new int[]{11,12,15,16}) {
            if(y>=10+height-1)continue;
            for(int z=0;z<=thickness;z++)world.setBlockToAir(new BlockPos(sx+x,y,sz+z));
            IBlockState window=Blocks.GLASS_PANE.getDefaultState();
            if(!glass&&ArchitectureConfig.windows)window=ArchitectureBlocks.findWindow("spruce",finish,false,true,false).getDefaultState().withProperty(WindowBlock.FACING,EnumFacing.NORTH);
            world.setBlockState(new BlockPos(sx+x,y,sz),window,2);
        }
        if(height>6) {
            for(int y=10;y<=15;y++){BlockPos at=new BlockPos(sx+thickness,y,sz+depth-3);world.setBlockState(at.west(),Blocks.PLANKS.getDefaultState(),2);world.setBlockState(at,Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING,EnumFacing.EAST),2);}
        }
        roof(world,sx,sz,width,depth,10+height);
    }
    private static void roof(WorldServer world,int sx,int sz,int width,int depth,int base) {
        RoofBlock slope=(RoofBlock)Block.getBlockFromName("ironagearchitecture:thatch_roof");
        RoofBlock ridge=(RoofBlock)Block.getBlockFromName("ironagearchitecture:thatch_ridge");
        if(slope==null||ridge==null)return;
        if((width&1)==0)width++;
        for(int x=-1;x<=width;x++)for(int z=-1;z<=depth;z++) {
            int dx=Math.min(x,width-1-x),dz=Math.min(z,depth-1-z),rise=Math.min(dx,dz);boolean apex=dx==width/2&&dz>=dx||dz==depth/2&&dx>=dz;
            RoofBlock piece=apex?ridge:slope;EnumFacing facing=apex?(width>=depth?EnumFacing.EAST:EnumFacing.NORTH):dx<dz?(x<width/2?EnumFacing.EAST:EnumFacing.WEST):(z<depth/2?EnumFacing.SOUTH:EnumFacing.NORTH);
            BlockPos at=new BlockPos(sx+x,base+rise,sz+z);IBlockState state=piece.getDefaultState().withProperty(RoofBlock.FACING,facing);
            world.setBlockState(at,state,2);ROOF.add(at);ROOF_STATES.put(at,state);
        }
    }
    static void roofs(WorldServer world,boolean visible){for(BlockPos at:ROOF)world.setBlockState(at,visible?ROOF_STATES.get(at):Blocks.AIR.getDefaultState(),2);}
}
