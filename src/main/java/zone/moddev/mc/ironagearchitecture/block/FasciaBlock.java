package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;

public final class FasciaBlock extends TrimmedRoofBlock {
    public static final PropertyInteger FITTING = PropertyInteger.create("fitting", 0, 2);
    public static final PropertyInteger ENDS = PropertyInteger.create("ends", 0, 3);

    public final boolean lined;

    public FasciaBlock(boolean lined) {
        super(Kind.FASCIA);
        this.lined = lined;
        setDefaultState(getDefaultState().withProperty(FITTING, 0).withProperty(ENDS, 3));
    }

    @Override protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, FITTING, ENDS);
    }
    @Override public IBlockState getStateFromMeta(int meta) {
        int fitting = meta >> 2 & 3;
        return getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
            .withProperty(FITTING, fitting == 3 ? 0 : fitting);
    }
    @Override public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | state.getValue(FITTING) << 2;
    }
    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side,
            float x, float y, float z, int meta, EntityLivingBase player) {
        return getDefaultState().withProperty(FACING, player.getHorizontalFacing());
    }
    @Override public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (state.getValue(FITTING) == 0) return state;
        EnumFacing facing = state.getValue(FACING);
        int ends = connects(world, pos.offset(facing.rotateYCCW()), facing) ? 0 : 1;
        if (!connects(world, pos.offset(facing.rotateY()), facing)) ends |= 2;
        return state.withProperty(ENDS, ends);
    }

    private boolean connects(IBlockAccess world, BlockPos pos, EnumFacing facing) {
        if (world instanceof World && !((World)world).isBlockLoaded(pos)) return false;
        IBlockState other = world.getBlockState(pos);
        return other.getBlock() instanceof FasciaBlock && other.getValue(FACING) == facing && other.getValue(FITTING) != 0;
    }

    @Override public List<AxisAlignedBB> collision(IBlockState state) {
        List<AxisAlignedBB> boxes = RoofTrimGeometry.boxes(Kind.FASCIA, false, state.getValue(FACING));
        if (state.getValue(FITTING) != 0)
            boxes.addAll(DrainGeometry.gutterBoxes(state.getValue(FITTING) == 2, state.getValue(ENDS), state.getValue(FACING)));
        return boxes;
    }

    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, @Nullable ItemStack held, EnumFacing side, float x, float y, float z) {
        if (!player.isSneaking() || hand != EnumHand.MAIN_HAND || held != null || player.getHeldItemOffhand() != null)
            return false;

        int fitting = state.getValue(FITTING);
        boolean removeSoffit = lined && side == EnumFacing.DOWN && onUnderside(state, x, y, z);
        boolean removeGutter = fitting != 0 && localDepth(state, x, y, z) >= RoofTrimGeometry.EAVE + 1e-4 && y <= 3.0 / 16;
        if (!removeSoffit && !removeGutter) return false;

        ItemStack recovered = removeSoffit ? new ItemStack(ArchitectureBlocks.SOFFIT) : fittingStack(fitting);
        if (!world.isBlockModifiable(player, pos) || !player.canPlayerEdit(pos, side, recovered)) return false;
        if (!world.isRemote) {
            IBlockState replacement = removeSoffit ? withLining(state, false) : state.withProperty(FITTING, 0);
            if (!world.setBlockState(pos, replacement, 3)) return false;
            if (!player.capabilities.isCreativeMode && !player.inventory.addItemStackToInventory(recovered))
                spawnAsEntity(world, pos, recovered);
            if (removeSoffit) playLining(world, player, pos);
            else playFit(world, player, pos);
        }
        return true;
    }

    public static IBlockState withLining(IBlockState state, boolean lined) {
        FasciaBlock target = lined ? ArchitectureBlocks.CLAY_FASCIA_LINED : ArchitectureBlocks.CLAY_FASCIA;
        return target.getDefaultState().withProperty(FACING, state.getValue(FACING)).withProperty(FITTING, state.getValue(FITTING));
    }

    static boolean onUnderside(IBlockState state, float x, float y, float z) {
        return y <= 1e-4 && localDepth(state, x, y, z) <= RoofTrimGeometry.EAVE + 1e-4;
    }

    private static double localDepth(IBlockState state, float x, float y, float z) {
        EnumFacing facing = state.getValue(FACING);
        EnumFacing inverse = facing.getAxis() == EnumFacing.Axis.X ? facing.getOpposite() : facing;
        return RoofGeometry.rotate(x, y, z, inverse)[2];
    }

    static void playLining(World world, EntityPlayer player, BlockPos pos) {
        world.playSound(player, pos, SoundType.WOOD.getPlaceSound(), SoundCategory.BLOCKS, .6F, .9F);
    }

    public static void playFit(World world, EntityPlayer player, BlockPos pos) {
        world.playSound(player, pos, SoundType.METAL.getPlaceSound(), SoundCategory.BLOCKS, .6F, .9F);
    }

    private static ItemStack fittingStack(int fitting) {
        return new ItemStack(fitting == 2 ? ArchitectureBlocks.GUTTER_OUTLET : ArchitectureBlocks.GUTTER);
    }

    @Override public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Item.getItemFromBlock(ArchitectureBlocks.CLAY_FASCIA);
    }

    @Override public ItemStack getPickBlock(IBlockState state, RayTraceResult hit, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(ArchitectureBlocks.CLAY_FASCIA);
    }

    @Override public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        List<ItemStack> drops = super.getDrops(world, pos, state, fortune);
        int fitting = state.getValue(FITTING);
        if (fitting != 0) drops.add(fittingStack(fitting));
        if (lined) drops.add(new ItemStack(ArchitectureBlocks.SOFFIT));
        return drops;
    }

    @SideOnly(Side.CLIENT)
    @Override public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        DrainRun.showDrip(state, world, pos);
    }
}
