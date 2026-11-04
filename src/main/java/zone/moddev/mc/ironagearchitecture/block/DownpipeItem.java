package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DownpipeItem extends EraItemBlock {
    private final DownpipeBlock pipe;

    public DownpipeItem(DownpipeBlock pipe) {
        super(pipe);
        this.pipe = pipe;
        setHasSubtypes(pipe.isOffset());
    }

    @Override public int getMetadata(int damage) { return pipe.isOffset() ? damage & 3 : 0; }
    @Override public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName(stack) + (pipe.isOffset() ? "." + getMetadata(stack.getMetadata()) : "");
    }
    @Override public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> result) {
        for (int i = 0; i < (pipe.isOffset() ? 4 : 1); i++) result.add(new ItemStack(item, 1, i));
    }
    @Override public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos,
            EnumFacing side, float x, float y, float z, IBlockState state) {
        for (AxisAlignedBB box : pipe.boxes(state)) if (!world.checkNoEntityCollision(box.offset(pos))) return false;
        return super.placeBlockAt(stack, player, world, pos, side, x, y, z, state);
    }
    @Override public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        if (pipe.isShoe()) {
            hint(tooltip, "pipe.shoe");
            return;
        }
        hint(tooltip, pipe.isOffset() ? "pipe.offset." + getMetadata(stack.getMetadata()) : "pipe.place");
        hint(tooltip, pipe.isOffset() ? "pipe.cycle" : "pipe.craft");
    }
}
