package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class TileHangingItem extends EraItemBlock {
    public TileHangingItem(TileHangingBlock block) { super(block); }

    @Override public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world,
            BlockPos pos, EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (!((TileHangingBlock) block).isThin()) return super.onItemUse(stack, player, world, pos, hand, side, x, y, z);
        Block clicked = world.getBlockState(pos).getBlock();
        if (!clicked.isReplaceable(world, pos)) pos = pos.offset(side);
        if (stack.stackSize == 0 || !player.canPlayerEdit(pos, side, stack)
                || !world.canBlockBePlaced(block, pos, true, side, null, stack)) return EnumActionResult.FAIL;
        IBlockState state = block.getStateForPlacement(world, pos, side, x, y, z, 0, player, stack);
        if (!world.checkNoEntityCollision(state.getBoundingBox(world, pos).offset(pos))
                || !placeBlockAt(stack, player, world, pos, side, x, y, z, state)) return EnumActionResult.FAIL;
        SoundType sound = block.getSoundType(state, world, pos, player);
        world.playSound(player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * .8F);
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
        return EnumActionResult.SUCCESS;
    }

    @Override public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        hint(tooltip, "cut");
        hint(tooltip, ((TileHangingBlock) block).isThin() ? "clay.place" : "clay.swap");
    }
}
