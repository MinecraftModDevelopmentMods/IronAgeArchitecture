package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.Era;
import zone.moddev.mc.ironagearchitecture.EraTabs;

public final class SoffitItem extends Item {
    public SoffitItem() {
        setCreativeTab(EraTabs.tab(Era.LATE_MEDIEVAL_TUDOR));
    }

    @Override public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos,
            EnumHand hand, EnumFacing side, float x, float y, float z) {
        IBlockState state = world.getBlockState(pos);
        if (side != EnumFacing.DOWN || !(state.getBlock() instanceof FasciaBlock)
                || !FasciaBlock.onUnderside(state, x, y, z)) return EnumActionResult.PASS;
        if (((FasciaBlock)state.getBlock()).lined || stack.stackSize <= 0 || !world.isBlockModifiable(player, pos)
                || !player.canPlayerEdit(pos, side, stack)) return EnumActionResult.FAIL;
        if (!world.isRemote) {
            if (!world.setBlockState(pos, FasciaBlock.withLining(state, true), 3)) return EnumActionResult.FAIL;
            FasciaBlock.playLining(world, player, pos);
            if (!player.capabilities.isCreativeMode) stack.stackSize--;
        }
        return EnumActionResult.SUCCESS;
    }

    @Override public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        EraItemBlock.hint(tooltip, "soffit.fit");
        EraItemBlock.hint(tooltip, "soffit.craft");
    }
}
