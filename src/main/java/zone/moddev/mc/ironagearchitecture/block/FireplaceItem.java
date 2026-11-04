package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import java.util.Locale;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class FireplaceItem extends EraItemBlock {
    private final FireplaceBlock fireplace;

    public FireplaceItem(FireplaceBlock block) {
        super(block);
        this.fireplace = block;
    }

    @Override public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos,
            EnumFacing side, float x, float y, float z, IBlockState state) {
        for (AxisAlignedBB box : fireplace.boxes(state, world, pos))
            if (!world.checkNoEntityCollision(box.offset(pos))) return false;
        return super.placeBlockAt(stack, player, world, pos, side, x, y, z, state);
    }

    @Override public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        hint(tooltip, "fireplace." + fireplace.kind.name().toLowerCase(Locale.ROOT));
    }
}
