package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;

public final class WindowItem extends EraItemBlock {
    public WindowItem(WindowBlock block){super(block);}
    @Override public EnumActionResult onItemUse(ItemStack stack,EntityPlayer player,World world,BlockPos pos,EnumHand hand,EnumFacing side,float x,float y,float z) {
        if(!world.getBlockState(pos).getBlock().isReplaceable(world,pos)) pos=pos.offset(side);
        if(stack.stackSize==0||!world.getBlockState(pos).getBlock().isReplaceable(world,pos)||!player.canPlayerEdit(pos,side,stack)) return EnumActionResult.FAIL;
        WindowBlock base=(WindowBlock)block;EnumFacing facing=player.getHorizontalFacing().getOpposite();
        IBlockState previous=world.getBlockState(pos.offset(side.getOpposite()));Boolean right=null;Boolean back=null;
        if(!player.isSneaking()) {
            if(previous.getBlock() instanceof WindowBlock&&((WindowBlock)previous.getBlock()).thin==base.thin) {facing=previous.getValue(WindowBlock.FACING);right=((WindowBlock)previous.getBlock()).right;back=((WindowBlock)previous.getBlock()).back;}
            else if(base.thin&&WallRuns.straightHalfFacing(previous)!=null) {
                EnumFacing plane=WallRuns.straightHalfFacing(previous);BlockPos clicked=pos.offset(side.getOpposite());
                double[] centre=WallGeometry.rotate(8,0,4,plane);
                facing=plane.getAxis()==EnumFacing.Axis.Z?(player.posZ<clicked.getZ()+centre[2]/16?EnumFacing.NORTH:EnumFacing.SOUTH):(player.posX<clicked.getX()+centre[0]/16?EnumFacing.WEST:EnumFacing.EAST);
                EnumFacing inverse=facing==EnumFacing.EAST?EnumFacing.WEST:facing==EnumFacing.WEST?EnumFacing.EAST:facing;
                back=WallGeometry.rotate(centre[0],0,centre[2],inverse)[2]>=8;
            }
            for(BlockPos next:new BlockPos[]{pos.down(),pos.up()}) {
                IBlockState neighbor=world.getBlockState(next);
                if(neighbor.getBlock() instanceof WindowBlock) {WindowBlock other=(WindowBlock)neighbor.getBlock();if(base.wood.equals(other.wood)&&base.finish==other.finish&&base.thin==other.thin&&base.diamond==other.diamond){facing=neighbor.getValue(WindowBlock.FACING);right=other.right;back=other.back;break;}}
            }
        }
        if(right==null) {EnumFacing inverse=facing==EnumFacing.EAST?EnumFacing.WEST:facing==EnumFacing.WEST?EnumFacing.EAST:facing;right=WallGeometry.rotate(x*16,0,z*16,inverse)[0]>=8;}
        if(back==null) {EnumFacing inverse=facing==EnumFacing.EAST?EnumFacing.WEST:facing==EnumFacing.WEST?EnumFacing.EAST:facing;back=base.thin&&WallGeometry.rotate(x*16,0,z*16,inverse)[2]>=8;}
        WindowBlock placed=ArchitectureBlocks.findWindow(base.wood,base.finish,base.thin,base.diamond,right,back);
        if(world.isRemote) return EnumActionResult.SUCCESS;
        if(!WindowController.place(world,pos,placed.getDefaultState().withProperty(WindowBlock.FACING,facing),player)) return EnumActionResult.FAIL;
        world.playSound(null,pos,net.minecraft.init.SoundEvents.BLOCK_WOOD_PLACE,SoundCategory.BLOCKS,1,.8F);
        if(!player.capabilities.isCreativeMode) stack.stackSize--;return EnumActionResult.SUCCESS;
    }
    @Override public void addInformation(ItemStack stack,EntityPlayer player,List<String> tooltip,boolean advanced) {
        hint(tooltip,"window.use");hint(tooltip,"window.place");hint(tooltip,"window.glaze");if(((WindowBlock)block).finish==0) hint(tooltip,"wash");
    }
}
