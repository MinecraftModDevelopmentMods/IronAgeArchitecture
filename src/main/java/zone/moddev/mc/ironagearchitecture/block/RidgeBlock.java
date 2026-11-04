package zone.moddev.mc.ironagearchitecture.block;

import java.util.Locale;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** Half-height 45-degree apex joins full-rise slopes placed one row below. */
public final class RidgeBlock extends RoofBlock {
    public enum End implements IStringSerializable { SINGLE, START, MIDDLE, END;
        @Override public String getName() { return name().toLowerCase(Locale.ROOT); }
    }
    public static final PropertyEnum<End> ENDS = PropertyEnum.create("ends",End.class);
    public RidgeBlock(String materialKey) { super(true,materialKey); setDefaultState(getDefaultState().withProperty(ENDS,End.SINGLE)); }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this,FACING,ENDS); }
    private boolean connects(IBlockAccess world, BlockPos pos, EnumFacing facing) {
        IBlockState other = world.getBlockState(pos);
        if (!(other.getBlock() instanceof RoofBlock)) return false;
        RoofBlock roof = (RoofBlock) other.getBlock();
        EnumFacing direction = other.getValue(FACING);
        return roof.isRidge() && materialKey.equals(roof.materialKey)
            && (roof instanceof TrimmedRoofBlock ? direction == facing : direction.getAxis() == facing.getAxis());
    }
    @Override public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        EnumFacing facing = state.getValue(FACING);
        boolean north = connects(world,pos.offset(facing),facing);
        boolean south = connects(world,pos.offset(facing.getOpposite()),facing.getOpposite());
        return state.withProperty(ENDS,north ? (south ? End.MIDDLE : End.END) : (south ? End.START : End.SINGLE));
    }
    @Override public RoofShape geometryShape(IBlockState state) { return RoofShape.STRAIGHT; }
    @Override public boolean capNorth(IBlockState state) { return state.getValue(ENDS) == End.SINGLE || state.getValue(ENDS) == End.START; }
    @Override public boolean capSouth(IBlockState state) { return state.getValue(ENDS) == End.SINGLE || state.getValue(ENDS) == End.END; }
}
