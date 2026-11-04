package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class FireplaceBlock extends BlockHorizontal {
    public enum Masonry {
        BRICK("brick", "minecraft:blocks/brick"), STONE("stone", "minecraft:blocks/stonebrick");

        public final String key, texture;
        Masonry(String key, String texture) {
            this.key = key;
            this.texture = texture;
        }
    }

    public enum Kind {
        HEARTH, FIREBOX, JAMB, LINTEL, FLUE, COLLAR, POT_CAP, SLAB_CAP;

        public boolean hearth() { return this == HEARTH || this == FIREBOX; }
        public boolean flue() { return this == FLUE || this == COLLAR; }
    }

    public static final PropertyBool LIT = PropertyBool.create("lit");
    public static final IUnlistedProperty<Integer> JOINS = new IUnlistedProperty<Integer>() {
        public String getName() { return "fireplace_joins"; }
        public boolean isValid(Integer value) { return value != null && value >= 0 && value <= 15; }
        public Class<Integer> getType() { return Integer.class; }
        public String valueToString(Integer value) { return value.toString(); }
    };

    public final Masonry masonry;
    public final Kind kind;

    public FireplaceBlock(Masonry masonry, Kind kind) {
        super(Material.ROCK);
        this.masonry = masonry;
        this.kind = kind;
        setHardness(2);
        setResistance(6);
        setSoundType(SoundType.STONE);
        setHarvestLevel("pickaxe", 0);
        setLightOpacity(0);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(LIT, false));
    }

    @Override protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this, new IProperty<?>[]{FACING, LIT}, new IUnlistedProperty<?>[]{JOINS});
    }

    @Override public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
            .withProperty(LIT, kind == Kind.FIREBOX && (meta & 4) != 0);
    }

    @Override public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (kind == Kind.FIREBOX && state.getValue(LIT) ? 4 : 0);
    }

    @Override public int damageDropped(IBlockState state) { return 0; }

    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float x, float y, float z,
            int meta, EntityLivingBase player) {
        EnumFacing facing = player.getHorizontalFacing();
        return getDefaultState().withProperty(FACING, kind == Kind.COLLAR ? facing : facing.getOpposite());
    }

    @Override public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withProperty(FACING, mirror.mirror(state.getValue(FACING)));
    }

    public int joins(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (!kind.hearth() && !kind.flue()) return 0;
        EnumFacing front = state.getValue(FACING);
        int result = 0;
        for (int i = 0; i < FireplaceGeometry.JOIN_SIDES.length; i++) {
            EnumFacing local = FireplaceGeometry.JOIN_SIDES[i];
            if (kind.hearth() && local.getAxis() != EnumFacing.Axis.X) continue;
            BlockPos adjacent = pos.offset(FireplaceGeometry.rotateSide(local, front));
            if (world instanceof World && !((World)world).isBlockLoaded(adjacent)) continue;
            IBlockState other = world.getBlockState(adjacent);
            if (!(other.getBlock() instanceof FireplaceBlock)) continue;
            FireplaceBlock neighbour = (FireplaceBlock)other.getBlock();
            if (masonry == neighbour.masonry && (kind.flue() ? neighbour.kind.flue()
                    : neighbour.kind.hearth() && other.getValue(FACING) == front)) result |= 1 << i;
        }
        return result;
    }

    @Override public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return ((IExtendedBlockState)state).withProperty(JOINS, joins(state, world, pos));
    }

    public List<AxisAlignedBB> boxes(IBlockState state, IBlockAccess world, BlockPos pos) {
        return FireplaceGeometry.collision(kind, joins(state, world, pos), state.getValue(FACING));
    }

    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return WindowBlock.union(boxes(state, world, pos));
    }

    @Nullable @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state, World world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB mask,
            List<AxisAlignedBB> boxes, @Nullable Entity entity) {
        for (AxisAlignedBB box : boxes(state, world, pos)) addCollisionBoxToList(pos, mask, boxes, box);
    }

    @Nullable @Override public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        RayTraceResult closest = null;
        for (AxisAlignedBB box : boxes(state, world, pos)) {
            RayTraceResult hit = rayTrace(pos, start, end, box);
            if (hit != null && (closest == null || start.squareDistanceTo(hit.hitVec) < start.squareDistanceTo(closest.hitVec)))
                closest = hit;
        }
        return closest;
    }

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) { return false; }
    @Override public int getLightValue(IBlockState state) { return kind == Kind.FIREBOX && state.getValue(LIT) ? 13 : 0; }
    @Override public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) { return getLightValue(state); }

    @SideOnly(Side.CLIENT)
    @Override public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }

    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, @Nullable ItemStack held, EnumFacing side, float x, float y, float z) {
        if (kind != Kind.FIREBOX) return false;
        boolean lit = state.getValue(LIT);
        boolean flint = held != null && held.getItem() == Items.FLINT_AND_STEEL;
        if (lit && flint) return true;
        boolean light = flint;
        boolean extinguish = lit && player.isSneaking() && hand == EnumHand.MAIN_HAND
            && held == null && player.getHeldItemOffhand() == null;
        if (!light && !extinguish) return false;
        ItemStack tool = light ? held : new ItemStack(this);
        if (!world.isBlockModifiable(player, pos) || !player.canPlayerEdit(pos, side, tool)) return false;
        if (!world.isRemote) {
            if (!world.setBlockState(pos, state.withProperty(LIT, light), 3)) return false;
            world.playSound(null, pos, light ? SoundEvents.ITEM_FLINTANDSTEEL_USE : SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.BLOCKS, .7F, 1);
            if (light && !player.capabilities.isCreativeMode) held.damageItem(1, player);
        }
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        if (kind == Kind.FIREBOX && state.getValue(LIT)) {
            world.spawnParticle(EnumParticleTypes.FLAME, pos.getX()+.5, pos.getY()+.52, pos.getZ()+.5, 0, .015, 0);
            if (random.nextInt(8) == 0)
                world.playSound(pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5, SoundEvents.BLOCK_FIRE_AMBIENT,
                    SoundCategory.BLOCKS, .45F, .95F, false);
            return;
        }
        Vec3d outlet = FireplaceDraft.smokeOutlet(world, pos, state);
        if (outlet != null)
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, pos.getX()+outlet.xCoord + (random.nextDouble()-.5)*.14,
                pos.getY()+outlet.yCoord, pos.getZ()+outlet.zCoord + (random.nextDouble()-.5)*.14, 0, .045, 0);
    }
}
