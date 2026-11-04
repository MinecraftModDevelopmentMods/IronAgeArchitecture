package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.block.BlockLadder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.block.FireplaceBlock;
import zone.moddev.mc.ironagearchitecture.block.FireplaceBlock.Kind;
import zone.moddev.mc.ironagearchitecture.block.FireplaceBlock.Masonry;

final class RyeFireplacePreview {
    static final BlockPos LOWER_FIRE = new BlockPos(-38, 10, 4);
    static final BlockPos UPPER_FIRE = new BlockPos(-39, 14, 4);
    static final BlockPos LOWER_POT = new BlockPos(-38, 22, 4);
    static final BlockPos UPPER_POT = new BlockPos(-39, 22, 4);

    private RyeFireplacePreview() { }

    static void build(WorldServer world) {
        surround(world, LOWER_FIRE, Masonry.STONE, 2, true);
        surround(world, UPPER_FIRE, Masonry.STONE, 1, false);
        for (int dx : new int[]{-1, 1})
            set(world, LOWER_FIRE.add(dx, 0, 0), Masonry.STONE, Kind.HEARTH, EnumFacing.NORTH, false);
        stack(world, LOWER_FIRE, LOWER_POT, 18, EnumFacing.NORTH, Kind.POT_CAP);
        stack(world, UPPER_FIRE, UPPER_POT, 18, EnumFacing.NORTH, Kind.POT_CAP);
        for (int y = 10; y <= 14; y++) {
            BlockPos at = new BlockPos(-40, y, 2);
            world.setBlockState(at.west(), Blocks.STONEBRICK.getDefaultState(), 2);
            world.setBlockState(at, Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING, EnumFacing.EAST), 2);
        }

        BlockPos brickFire = new BlockPos(-45, 12, 6);
        surround(world, brickFire, Masonry.BRICK, 1, true);
        stack(world, brickFire, new BlockPos(-45, 24, 6), 21, EnumFacing.WEST, Kind.SLAB_CAP);
    }

    private static void surround(WorldServer world, BlockPos at, Masonry style, int width, boolean lit) {
        IBlockState backing = (style == Masonry.BRICK ? Blocks.BRICK_BLOCK : Blocks.STONEBRICK).getDefaultState();
        set(world, at, style, Kind.FIREBOX, EnumFacing.NORTH, lit);
        for (int dx = -width; dx <= width; dx++) {
            for (int dy = 0; dy <= 2; dy++) world.setBlockState(at.add(dx, dy, 1), backing, 2);
            set(world, at.add(dx, 1, -1), style, Kind.LINTEL, EnumFacing.NORTH, false);
            if (Math.abs(dx) == width) {
                set(world, at.add(dx, 0, -1), style, Kind.JAMB, EnumFacing.NORTH, false);
                world.setBlockState(at.add(dx, 0, 0), backing, 2);
            }
            if (Math.abs(dx) < width) world.setBlockState(at.add(dx, 2, -1), backing, 2);
        }
    }

    private static void stack(WorldServer world, BlockPos source, BlockPos outlet, int roof, EnumFacing pitch, Kind cap) {
        for (int y = source.getY() + 1; y < outlet.getY(); y++)
            set(world, new BlockPos(source.getX(), y, source.getZ()), Masonry.BRICK,
                y == roof ? Kind.COLLAR : Kind.FLUE, pitch, false);
        set(world, outlet, Masonry.BRICK, cap, pitch, false);
    }

    private static void set(WorldServer world, BlockPos pos, Masonry masonry, Kind kind, EnumFacing facing, boolean lit) {
        FireplaceBlock block = ArchitectureBlocks.fireplace(masonry, kind);
        world.setBlockState(pos, block.getDefaultState().withProperty(FireplaceBlock.FACING, facing)
            .withProperty(FireplaceBlock.LIT, lit), 3);
    }
}
