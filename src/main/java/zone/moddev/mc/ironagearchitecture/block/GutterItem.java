package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.Era;
import zone.moddev.mc.ironagearchitecture.EraTabs;

public final class GutterItem extends Item {
    public final boolean outlet;

    public GutterItem(boolean outlet) {
        this.outlet = outlet;
        setCreativeTab(EraTabs.tab(Era.LATE_MEDIEVAL_TUDOR));
    }

    @Override public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos,
            EnumHand hand, EnumFacing side, float x, float y, float z) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof FasciaBlock) || side != state.getValue(FasciaBlock.FACING).getOpposite())
            return EnumActionResult.PASS;
        if (state.getValue(FasciaBlock.FITTING) != 0 || stack.stackSize <= 0 || !world.isBlockModifiable(player, pos)
                || !player.canPlayerEdit(pos, side, stack)) return EnumActionResult.FAIL;
        for (AxisAlignedBB box : DrainGeometry.gutterBoxes(outlet, 3, state.getValue(FasciaBlock.FACING)))
            if (!world.checkNoEntityCollision(box.offset(pos))) return EnumActionResult.FAIL;
        if (!world.isRemote) {
            if (!world.setBlockState(pos, state.withProperty(FasciaBlock.FITTING, outlet ? 2 : 1), 3)) return EnumActionResult.FAIL;
            FasciaBlock.playFit(world, player, pos);
            if (!player.capabilities.isCreativeMode) stack.stackSize--;
        }
        return EnumActionResult.SUCCESS;
    }

    @Override public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        EraItemBlock.hint(tooltip, "gutter.fit");
        EraItemBlock.hint(tooltip, outlet ? "gutter.outlet" : "gutter.craft");
    }
}
