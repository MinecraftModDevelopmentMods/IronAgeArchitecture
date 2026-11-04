package zone.moddev.mc.ironagearchitecture.block;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Fired clay wall panels with the same occupied half as a thin timber wall. */
public final class TileHangingBlock extends BlockHorizontal {
    private final boolean thin;

    public TileHangingBlock(boolean thin) {
        super(Material.ROCK);
        this.thin = thin;
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        setHardness(2F);
        setResistance(6F);
        setSoundType(SoundType.STONE);
        setHarvestLevel("pickaxe", 0);
        if (thin) {
            setLightOpacity(0);
            useNeighborBrightness = true;
        }
    }

    public boolean isThin() { return thin; }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }
    @Override public boolean isOpaqueCube(IBlockState state) { return !thin; }
    @Override public boolean isFullCube(IBlockState state) { return !thin; }
    @Override public int damageDropped(IBlockState state) { return 0; }
    @Override public int getMetaFromState(IBlockState state) { return thin ? state.getValue(FACING).getHorizontalIndex() : 0; }
    @Override public IBlockState getStateFromMeta(int meta) {
        return thin ? getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3)) : getDefaultState();
    }
    @Override public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return thin ? state.withProperty(FACING, rotation.rotate(state.getValue(FACING))) : state;
    }
    @Override public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return thin ? state.withProperty(FACING, mirror.mirror(state.getValue(FACING))) : state;
    }
    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side,
            float x, float y, float z, int meta, EntityLivingBase placer) {
        if (!thin) return getDefaultState();
        EnumFacing facing = side.getOpposite();
        if (side.getAxis().isVertical()) {
            facing = placer.getHorizontalFacing().getAxis() == EnumFacing.Axis.X
                ? (x < .5F ? EnumFacing.WEST : EnumFacing.EAST)
                : (z < .5F ? EnumFacing.NORTH : EnumFacing.SOUTH);
        }
        if (!placer.isSneaking()) {
            EnumFacing inherited = WallRuns.straightHalfFacing(world.getBlockState(pos.offset(side.getOpposite())));
            if (inherited != null) facing = inherited;
        }
        return getDefaultState().withProperty(FACING, facing);
    }

    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (!thin) return FULL_BLOCK_AABB;
        EnumFacing facing = state.getValue(FACING);
        double[] a = WallGeometry.rotate(0, 0, 0, facing);
        double[] b = WallGeometry.rotate(16, 16, 8, facing);
        return new AxisAlignedBB(Math.min(a[0], b[0]) / 16, 0, Math.min(a[2], b[2]) / 16,
            Math.max(a[0], b[0]) / 16, 1, Math.max(a[2], b[2]) / 16);
    }

    @Override public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return !thin || side == state.getValue(FACING);
    }
}
