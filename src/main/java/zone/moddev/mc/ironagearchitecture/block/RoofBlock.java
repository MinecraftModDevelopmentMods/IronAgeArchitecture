package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Saves facing only. Matching neighbours own corners using the vanilla stair guard. */
public class RoofBlock extends BlockHorizontal {
    public static final PropertyEnum<RoofShape> SHAPE = PropertyEnum.create("shape", RoofShape.class);
    private final boolean ridge;
    public RoofBlock(boolean ridge, boolean thatch) {
        super(thatch ? Material.GRASS : Material.ROCK);
        this.ridge = ridge;
        setHardness(thatch ? .8F : 2F); setResistance(thatch ? 1F : 6F);
        setSoundType(thatch ? SoundType.PLANT : SoundType.STONE);
        setHarvestLevel(thatch ? "axe" : "pickaxe", 0);
        IBlockState state = blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH);
        setDefaultState(ridge ? state : state.withProperty(SHAPE, RoofShape.STRAIGHT));
        setLightOpacity(0);
    }
    public boolean isRidge() { return ridge; }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING, SHAPE); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3)); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getHorizontalIndex(); }
    @Override public int damageDropped(IBlockState state) { return 0; }
    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float x, float y, float z, int meta, EntityLivingBase player) {
        return getDefaultState().withProperty(FACING, player.getHorizontalFacing());
    }
    private boolean matches(IBlockState state) { return state.getBlock() == this; }
    private boolean different(IBlockAccess world, BlockPos pos, EnumFacing direction, IBlockState state) {
        IBlockState other = world.getBlockState(pos.offset(direction));
        return !matches(other) || other.getValue(FACING) != state.getValue(FACING);
    }
    @Override public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        EnumFacing facing = state.getValue(FACING);
        IBlockState front = world.getBlockState(pos.offset(facing));
        if (matches(front)) {
            EnumFacing turn = front.getValue(FACING);
            if (turn.getAxis() != facing.getAxis() && different(world, pos, turn.getOpposite(), state))
                return state.withProperty(SHAPE, turn == facing.rotateYCCW() ? RoofShape.OUTER_LEFT : RoofShape.OUTER_RIGHT);
        }
        IBlockState back = world.getBlockState(pos.offset(facing.getOpposite()));
        if (matches(back)) {
            EnumFacing turn = back.getValue(FACING);
            if (turn.getAxis() != facing.getAxis() && different(world, pos, turn, state))
                return state.withProperty(SHAPE, turn == facing.rotateYCCW() ? RoofShape.INNER_LEFT : RoofShape.INNER_RIGHT);
        }
        return state.withProperty(SHAPE, RoofShape.STRAIGHT);
    }
    public RoofShape geometryShape(IBlockState state) { return state.getValue(SHAPE); }
    public boolean capNorth(IBlockState state) { return true; }
    public boolean capSouth(IBlockState state) { return true; }
    public List<AxisAlignedBB> collision(IBlockState state) { return RoofGeometry.boxes(geometryShape(state), ridge, state.getValue(FACING)); }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) { return side == EnumFacing.DOWN; }
    @Nullable @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state, World world, BlockPos pos) { return NULL_AABB; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) { return new AxisAlignedBB(0,0,0,1,ridge ? .5 : 1,1); }
    @Override public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB mask, List<AxisAlignedBB> boxes, @Nullable Entity entity) {
        for (AxisAlignedBB box : collision(getActualState(state,world,pos))) addCollisionBoxToList(pos,mask,boxes,box);
    }
    @Nullable @Override public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        RayTraceResult closest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (AxisAlignedBB box : collision(getActualState(state,world,pos))) {
            RayTraceResult hit = rayTrace(pos,start,end,box);
            if (hit != null && start.squareDistanceTo(hit.hitVec) < distance) {
                closest = hit; distance = start.squareDistanceTo(hit.hitVec);
            }
        }
        return closest;
    }
    @Override public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) { return true; }
    @Override public IBlockState withRotation(IBlockState state, Rotation rotation) { return state.withProperty(FACING,rotation.rotate(state.getValue(FACING))); }
    @Override public IBlockState withMirror(IBlockState state, Mirror mirror) { return withRotation(state,mirror.toRotation(state.getValue(FACING))); }
}
