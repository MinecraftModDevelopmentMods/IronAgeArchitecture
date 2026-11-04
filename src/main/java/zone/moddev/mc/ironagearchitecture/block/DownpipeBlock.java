package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class DownpipeBlock extends BlockHorizontal {
    public static final PropertyInteger PROFILE = PropertyInteger.create("profile", 0, 3);
    public enum Kind { STRAIGHT, OFFSET, SHOE }

    private final Kind kind;

    public DownpipeBlock(Kind kind) {
        // The shoe discharges inside its own cell. Vanilla drips expire inside solid materials.
        super(kind == Kind.SHOE ? Material.CIRCUITS : Material.IRON);
        this.kind = kind;
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(PROFILE, 0));
        setHardness(1.5F);
        setSoundType(SoundType.METAL);
        setLightOpacity(0);
    }

    public boolean isOffset() { return kind == Kind.OFFSET; }
    public boolean isShoe() { return kind == Kind.SHOE; }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING, PROFILE); }
    @Override public IBlockState getStateFromMeta(int meta) {
        int profile = meta >> 2 & 3;
        return getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
            .withProperty(PROFILE, !isOffset() && profile == 3 ? 0 : profile);
    }
    @Override public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | state.getValue(PROFILE) << 2;
    }
    @Override public int damageDropped(IBlockState state) { return isOffset() ? state.getValue(PROFILE) : 0; }
    @Override public ItemStack getPickBlock(IBlockState state, RayTraceResult hit, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(this, 1, damageDropped(state));
    }
    @Override public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withProperty(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float x, float y,
            float z, int meta, EntityLivingBase placer) {
        int profile = isOffset() ? meta & 3 : 0;
        EnumFacing facing = placer.getHorizontalFacing();
        BlockPos clicked = pos.offset(side.getOpposite());
        IBlockState against = world.getBlockState(clicked);
        if (!placer.isSneaking()) {
            for (boolean upstream : new boolean[]{true, false}) {
                DrainRun.Port port = DrainRun.port(against, upstream);
                if (port == null || port.side != side) continue;
                EnumFacing aligned = against.getValue(FACING);
                int candidate = isOffset() ? profile : port.lane();
                if (candidate < 0) continue;
                IBlockState chosen = state(aligned, candidate);
                DrainRun.Port own = DrainRun.port(chosen, !upstream);
                if (own != null && port.matches(own)) return chosen;
            }
            if (!isOffset() && !(against.getBlock() instanceof DownpipeBlock) && side.getAxis().isHorizontal())
                facing = side.getOpposite();
        } else if (!isOffset()) {
            EnumFacing inverse = facing.getAxis() == EnumFacing.Axis.X ? facing.getOpposite() : facing;
            double depth = RoofGeometry.rotate(x, y, z, inverse)[2];
            profile = depth < .25 ? 0 : depth < .69 ? 1 : 2;
        }
        return state(facing, profile);
    }

    public IBlockState state(EnumFacing facing, int profile) {
        return getDefaultState().withProperty(FACING, facing).withProperty(PROFILE, profile);
    }

    public double[][] path(IBlockState state) {
        int profile = state.getValue(PROFILE);
        if (isOffset()) return DrainGeometry.Offset.values()[profile].path;
        return isShoe() ? DrainGeometry.shoe(profile) : DrainGeometry.straight(profile);
    }

    public List<AxisAlignedBB> boxes(IBlockState state) {
        return DrainGeometry.pipeBoxes(path(state), state.getValue(FACING));
    }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) { return false; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return WindowBlock.union(boxes(state));
    }
    @Nullable @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state, World world, BlockPos pos) {
        return NULL_AABB;
    }
    @Override public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
            AxisAlignedBB mask, List<AxisAlignedBB> result, @Nullable Entity entity) {
        for (AxisAlignedBB box : boxes(state)) addCollisionBoxToList(pos, mask, result, box);
    }
    @Nullable @Override public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d from, Vec3d to) {
        RayTraceResult nearest = null;
        for (AxisAlignedBB box : boxes(state)) {
            RayTraceResult hit = rayTrace(pos, from, to, box);
            if (hit != null && (nearest == null || from.squareDistanceTo(hit.hitVec) < from.squareDistanceTo(nearest.hitVec))) nearest = hit;
        }
        return nearest;
    }
    @SideOnly(Side.CLIENT)
    @Override public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        DrainRun.showDrip(state, world, pos);
    }
}
