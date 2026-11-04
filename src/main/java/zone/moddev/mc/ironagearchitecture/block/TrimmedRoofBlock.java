package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Straight clay edge pieces. The ridge end faces outwards and the verge has two hands. */
public class TrimmedRoofBlock extends RoofBlock {
    public enum Kind { FASCIA, VERGE, RIDGE_END }

    public static final PropertyBool RIGHT = PropertyBool.create("right");
    public final Kind kind;

    public TrimmedRoofBlock(Kind kind) {
        super(kind == Kind.RIDGE_END, "clay_tile");
        this.kind = kind;
        if (getDefaultState().getPropertyKeys().contains(RIGHT)) setDefaultState(getDefaultState().withProperty(RIGHT, false));
    }

    @Override protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, RIGHT);
    }

    @Override public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) { return state; }
    @Override public RoofShape geometryShape(IBlockState state) { return RoofShape.STRAIGHT; }

    @Override public IBlockState getStateFromMeta(int meta) {
        return super.getStateFromMeta(meta).withProperty(RIGHT, kind == Kind.VERGE && (meta & 4) != 0);
    }

    @Override public int getMetaFromState(IBlockState state) {
        return super.getMetaFromState(state) | (kind == Kind.VERGE && state.getValue(RIGHT) ? 4 : 0);
    }

    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side,
            float x, float y, float z, int meta, EntityLivingBase player) {
        EnumFacing facing = player.getHorizontalFacing();
        if (kind == Kind.RIDGE_END) facing = facing.getOpposite();
        EnumFacing inverse = facing.getAxis() == EnumFacing.Axis.X ? facing.getOpposite() : facing;
        boolean right = kind == Kind.VERGE && RoofGeometry.rotate(x, y, z, inverse)[0] >= .5;
        return getDefaultState().withProperty(FACING, facing).withProperty(RIGHT, right);
    }

    @Override public IBlockState withMirror(IBlockState state, Mirror mirror) {
        IBlockState mirrored = super.withMirror(state, mirror);
        return kind == Kind.VERGE && mirror != Mirror.NONE ? mirrored.cycleProperty(RIGHT) : mirrored;
    }

    @Override public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) { return false; }

    @Override public List<AxisAlignedBB> collision(IBlockState state) {
        return RoofTrimGeometry.boxes(kind, state.getValue(RIGHT), state.getValue(FACING));
    }
}
