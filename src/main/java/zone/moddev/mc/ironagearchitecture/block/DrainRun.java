package zone.moddev.mc.ironagearchitecture.block;

import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class DrainRun {
    private DrainRun() { }

    static final class Port {
        final EnumFacing side;
        final EnumFacing facing;
        final double y, depth;

        Port(EnumFacing side, EnumFacing facing, double y, double depth) {
            this.side = DrainGeometry.rotateSide(side, facing);
            this.facing = facing;
            this.y = y;
            this.depth = depth;
        }

        int lane() {
            if (depth == DrainGeometry.WALL) return 0;
            if (depth == DrainGeometry.HALF) return 1;
            if (depth == DrainGeometry.OUTER) return 2;
            return -1;
        }

        boolean matches(Port other) {
            if (facing != other.facing || side != other.side.getOpposite()) return false;
            return side.getAxis().isVertical() ? depth == other.depth : y == other.y;
        }
    }

    static Port port(IBlockState state, boolean output) {
        if (state.getBlock() instanceof FasciaBlock) {
            return output && state.getValue(FasciaBlock.FITTING) == 2
                ? new Port(EnumFacing.DOWN, state.getValue(BlockHorizontal.FACING), 0, DrainGeometry.OUTER) : null;
        }
        if (!(state.getBlock() instanceof DownpipeBlock)) return null;
        DownpipeBlock pipe = (DownpipeBlock)state.getBlock();
        if (output && pipe.isShoe()) return null;
        double[][] path = pipe.path(state);
        double[] end = path[output ? path.length - 1 : 0];
        EnumFacing side = output ? EnumFacing.DOWN : EnumFacing.UP;
        if (pipe.isOffset()) {
            DrainGeometry.Offset offset = DrainGeometry.Offset.values()[state.getValue(DownpipeBlock.PROFILE)];
            side = output ? offset.outlet : offset.inlet;
        }
        return new Port(side, state.getValue(BlockHorizontal.FACING), end[0], end[1]);
    }

    public static boolean canDrip(IBlockState state, World world, BlockPos pos) {
        if (!world.isRaining()) return false;
        BlockPos inlet = exposedInletForDrip(state, world, pos);
        return inlet != null && world.isRainingAt(inlet.up());
    }

    @Nullable
    public static BlockPos exposedInletForDrip(IBlockState state, World world, BlockPos pos) {
        if (dripPoint(state, world, pos) == null) return null;
        for (int count = 0; count < 32; count++) {
            if (state.getBlock() instanceof FasciaBlock)
                return world.isBlockLoaded(pos.up()) && world.canSeeSky(pos.up()) ? pos : null;
            Port inlet = port(state, false);
            if (inlet == null) return null;
            pos = pos.offset(inlet.side);
            if (!world.isBlockLoaded(pos)) return null;
            state = world.getBlockState(pos);
            Port previous = port(state, true);
            if (previous == null || !inlet.matches(previous)) return null;
        }
        return null;
    }

    @Nullable
    private static Vec3d dripPoint(IBlockState state, World world, BlockPos pos) {
        if (state.getBlock() instanceof DownpipeBlock && ((DownpipeBlock)state.getBlock()).isShoe()) {
            double[][] path = ((DownpipeBlock)state.getBlock()).path(state);
            double[] mouth = path[path.length - 1];
            return DrainGeometry.point(mouth[0] - .65, mouth[1] + .3, state.getValue(BlockHorizontal.FACING));
        }
        Port end = port(state, true);
        if (end == null || end.side != EnumFacing.DOWN || !world.isBlockLoaded(pos.down()) || !world.isAirBlock(pos.down()))
            return null;
        return DrainGeometry.point(-.48, end.depth, end.facing);
    }

    @SideOnly(Side.CLIENT)
    static void showDrip(IBlockState state, World world, BlockPos pos) {
        if (!canDrip(state, world, pos)) return;
        Vec3d point = dripPoint(state, world, pos);
        if (point == null) return;
        world.spawnParticle(EnumParticleTypes.DRIP_WATER, pos.getX()+point.xCoord, pos.getY()+point.yCoord,
            pos.getZ()+point.zCoord, 0, 0, 0);
    }
}
