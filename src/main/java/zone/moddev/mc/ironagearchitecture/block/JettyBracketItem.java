package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;

/** Explicit half/full-reach bracket, attached to a floor without a multipart tile. */
public final class JettyBracketItem extends Item {
    public final String wood;
    public final boolean fullReach;
    public JettyBracketItem(String wood,boolean fullReach) {this.wood=wood;this.fullReach=fullReach;}
    @Override public EnumActionResult onItemUse(ItemStack stack,EntityPlayer player,World world,BlockPos pos,EnumHand hand,EnumFacing face,float x,float y,float z) {
        IBlockState old=world.getBlockState(pos);
        if(!(old.getBlock() instanceof JettyFloorBlock)||face!=EnumFacing.DOWN||!player.canPlayerEdit(pos,face,stack)) return EnumActionResult.FAIL;
        JettyFloorBlock floor=(JettyFloorBlock)old.getBlock();
        if(floor.bracket!=null||!wood.equals(floor.wood)) return EnumActionResult.FAIL;
        double[] local=WallGeometry.rotate(x*16,0,z*16,old.getValue(JettyFloorBlock.FACING)==EnumFacing.EAST?EnumFacing.WEST:
            old.getValue(JettyFloorBlock.FACING)==EnumFacing.WEST?EnumFacing.EAST:old.getValue(JettyFloorBlock.FACING));
        EnumFacing selected=null;double best=Double.POSITIVE_INFINITY;
        for(EnumFacing candidate:EnumFacing.HORIZONTALS) {
            double[] anchor=JettyFloorGeometry.bracketAnchor(floor.shape,candidate,fullReach);
            if(anchor==null) continue;
            EnumFacing turn=candidate==EnumFacing.NORTH?EnumFacing.EAST:candidate==EnumFacing.SOUTH?EnumFacing.WEST:candidate==EnumFacing.WEST?EnumFacing.NORTH:EnumFacing.SOUTH;
            double[] point=WallGeometry.rotate(anchor[0],0,anchor[1],turn);
            double distance=(point[0]-local[0])*(point[0]-local[0])+(point[2]-local[2])*(point[2]-local[2]);
            if(distance<best) {selected=candidate;best=distance;}
        }
        if(selected==null) return EnumActionResult.FAIL;
        // Do not attach through a neighbouring deck's hidden perimeter edge.
        int joins=floor.connections(old,world,pos);boolean exposed=false;
        for(int q=0;q<4;q++) if((floor.shape.mask&(1<<q))!=0&&JettyFloorGeometry.exposed(floor.shape,q,selected,joins)) exposed=true;
        if(!exposed) return EnumActionResult.FAIL;
        JettyFloorBlock combined=ArchitectureBlocks.findFloor(wood,floor.shape,selected);
        IBlockState next=combined.getDefaultState().withProperty(JettyFloorBlock.FACING,old.getValue(JettyFloorBlock.FACING))
            .withProperty(JettyFloorBlock.PANEL,(old.getValue(JettyFloorBlock.PANEL)&1)|(fullReach?2:0));
        // Check the newly occupied lower half against entities before committing the change.
        for(WallGeometry.Part part:combined.parts(next,joins)) if(part.bounds(next.getValue(JettyFloorBlock.FACING)).minY<.5
            &&!world.checkNoEntityCollision(part.bounds(next.getValue(JettyFloorBlock.FACING)).offset(pos))) return EnumActionResult.FAIL;
        if(!world.isRemote) {
            world.setBlockState(pos,next,3);
            if(!player.capabilities.isCreativeMode) stack.stackSize--;
        }
        return EnumActionResult.SUCCESS;
    }
    @Override public void addInformation(ItemStack stack,EntityPlayer player,List<String> tooltip,boolean advanced) {tooltip.add("Use beneath a matching jetty floor edge.");}
}
