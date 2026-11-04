package zone.moddev.mc.ironagearchitecture.block;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.block.FireplaceBlock.Kind;

/** A stack can share masonry, but every outlet reads only its own vertical channel. */
public final class FireplaceDraft {
    public static final int MAX_DEPTH = 32;

    private FireplaceDraft() { }

    @Nullable
    public static Vec3d smokeOutlet(World world, BlockPos pos, IBlockState state) {
        if (!(state.getBlock() instanceof FireplaceBlock)) return null;
        Kind kind = ((FireplaceBlock)state.getBlock()).kind;
        Vec3d outlet;
        if (kind == Kind.FLUE || kind == Kind.POT_CAP) {
            if (!clear(world, pos.up())) return null;
            outlet = new Vec3d(.5, 1.05, .5);
        } else if (kind == Kind.SLAB_CAP) {
            EnumFacing front = state.getValue(FireplaceBlock.FACING);
            EnumFacing open = null;
            for (EnumFacing side : new EnumFacing[]{front, front.rotateY(), front.getOpposite(), front.rotateYCCW()}) {
                if (clear(world, pos.offset(side))) {
                    open = side;
                    break;
                }
            }
            if (open == null) return null;
            outlet = new Vec3d(.5 + open.getFrontOffsetX() * .55, .7, .5 + open.getFrontOffsetZ() * .55);
        } else return null;

        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos(pos);
        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            below.setPos(pos.getX(), pos.getY() - depth, pos.getZ());
            if (below.getY() < 0 || !world.isBlockLoaded(below)) return null;
            IBlockState source = world.getBlockState(below);
            if (!(source.getBlock() instanceof FireplaceBlock)) return null;
            Kind part = ((FireplaceBlock)source.getBlock()).kind;
            if (!part.flue()) return part == Kind.FIREBOX && source.getValue(FireplaceBlock.LIT) ? outlet : null;
        }
        return null;
    }

    private static boolean clear(World world, BlockPos pos) {
        return world.isBlockLoaded(pos) && world.isAirBlock(pos);
    }
}
