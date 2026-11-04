package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.ArchitectureConfig;
import zone.moddev.mc.ironagearchitecture.block.*;

/** Three attached cottages with eaves and projecting gables over a stepped street. */
final class RyeTilePreview {
    static final String NAME = "Rye Downpipe Shoe Preview";
    static final BlockPos RAIN_FOOT = new BlockPos(-35, 10, 0);
    static final BlockPos SPAWN = new BlockPos(-34, 9, -5);

    static void build(WorldServer world) {
        for (int x = -67; x <= -23; x++) {
            int street = streetHeight(x);
            for (int z = -15; z <= 15; z++) {
                for (int y = 3; y < street - 1; y++) set(world, x, y, z, Blocks.DIRT.getDefaultState());
                boolean road = z >= -7 && z <= -1;
                IBlockState top = road ? ((x * 13 + z * 7) % 19 == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).getDefaultState()
                    : Blocks.GRASS.getDefaultState();
                set(world, x, street - 1, z, top);
                if (road && street < streetHeight(x - 1)) {
                    set(world, x, street, z, Blocks.STONE_STAIRS.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.WEST));
                }
            }
        }
        house(world, -57, 14, 7, 7, 7, "spruce", false, false);
        house(world, -50, 12, 9, 9, 6, "dark_oak", true, false);
        house(world, -41, 10, 7, 7, 6, "oak", false, true);
        halfJettyDrain(world);
        fullJettyDrain(world);

        // A short garden wall and hedge frame the exposed downhill end of the row.
        for (int z = 1; z <= 7; z++) {
            int x = -33;
            int ground = streetHeight(x);
            set(world, x, ground, z, Blocks.COBBLESTONE_WALL.getStateFromMeta(z % 3 == 0 ? 1 : 0));
            if (z >= 4) set(world, x, ground + 1, z, Blocks.LEAVES.getStateFromMeta(4));
        }
    }

    private static void halfJettyDrain(WorldServer world) {
        int outlet = -56;
        IBlockState floor = ArchitectureBlocks.findFloor("spruce", FloorShape.HALF, null).getDefaultState()
            .withProperty(FrameBlock.FACING, EnumFacing.SOUTH);
        PartialWallBlock post = ArchitectureBlocks.findPartial("spruce", FrameFamily.POST, WallProfile.THIN);
        for (int x = -57; x <= -51; x++) {
            if (x == outlet) {
                for (int y = 18; y <= 19; y++) set(world, x, y, 0,
                    ArchitectureBlocks.findWall("spruce", FrameFamily.POST).getDefaultState()
                        .withProperty(FrameBlock.FACING, EnumFacing.NORTH).withProperty(FrameBlock.PANEL, 2));
                continue;
            }
            set(world, x, 17, -1, floor);
            for (int y = 18; y <= 19; y++) {
                IBlockState old = world.getBlockState(new BlockPos(x, y, 0));
                IBlockState thin;
                if (old.getBlock() instanceof WindowBlock) {
                    thin = ArchitectureBlocks.findWindow("spruce", 1, true, false, x < -54, true).getDefaultState()
                        .withProperty(WindowBlock.FACING, EnumFacing.NORTH);
                } else {
                    thin = post.getDefaultState().withProperty(FrameBlock.FACING, EnumFacing.SOUTH)
                        .withProperty(FrameBlock.PANEL, 2);
                }
                set(world, x, y, -1, thin);
                set(world, x, y, 0, Blocks.AIR.getDefaultState());
            }
        }
        gutter(world, -58, -51, 20, outlet);
        for (int y = 17; y <= 19; y++) pipe(world, outlet, y, -1, false, 2);
        pipe(world, outlet, 16, -1, true, 1);
        pipe(world, outlet, 15, -1, false, 1);
        world.setBlockState(new BlockPos(outlet, 14, -1), ArchitectureBlocks.DOWNPIPE_SHOE.state(EnumFacing.SOUTH, 1), 2);
    }

    private static void fullJettyDrain(WorldServer world) {
        int outlet = -35;
        // Set the brick ground floor one block back beneath the timber storey.
        for (int x = -41; x <= -35; x++) for (int y = 10; y <= 12; y++) {
            IBlockState old = world.getBlockState(new BlockPos(x, y, 0));
            set(world, x, y, 1, old);
            set(world, x, y, 0, Blocks.AIR.getDefaultState());
        }
        gutter(world, -41, -34, 15, outlet);
        pipe(world, outlet, 14, -1, true, 0);
        pipe(world, outlet, 13, -1, false, 0);
        pipe(world, outlet, 12, -1, true, 2);
        pipe(world, outlet, 12, 0, true, 3);
        pipe(world, outlet, 11, 0, false, 0);
        world.setBlockState(RAIN_FOOT, ArchitectureBlocks.DOWNPIPE_SHOE.state(EnumFacing.SOUTH, 0), 2);
    }

    private static void gutter(WorldServer world, int from, int to, int y, int outlet) {
        for (int x = from; x <= to; x++) {
            BlockPos pos = new BlockPos(x, y, -1);
            IBlockState fascia = world.getBlockState(pos);
            if (fascia.getBlock() != ArchitectureBlocks.CLAY_FASCIA)
                throw new IllegalStateException("Preview gutter is missing its fascia at " + pos);
            world.setBlockState(pos, fascia.withProperty(FasciaBlock.FITTING, x == outlet ? 2 : 1), 2);
        }
    }

    private static void pipe(WorldServer world, int x, int y, int z, boolean offset, int profile) {
        DownpipeBlock piece = offset ? ArchitectureBlocks.DOWNPIPE_OFFSET : ArchitectureBlocks.DOWNPIPE;
        set(world, x, y, z, piece.state(EnumFacing.SOUTH, profile));
    }

    private static int streetHeight(int x) { return 14 - Math.floorDiv(x + 57, 4); }

    private static void house(WorldServer world, int start, int base, int width, int depth, int height,
            String wood, boolean tileFront, boolean brickBase) {
        int top = base + height;
        IBlockState fullTiles = ArchitectureBlocks.CLAY_HANGING.getDefaultState();
        // Recess the street gable by half a block under the thin verge.
        IBlockState thinTiles = ArchitectureBlocks.CLAY_HANGING_THIN.getDefaultState().withProperty(TileHangingBlock.FACING, EnumFacing.SOUTH);
        IBlockState floor = ArchitectureBlocks.findFloor(wood, FloorShape.FULL, null).getDefaultState();
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                for (int y = base - 1; y >= 3; y--) {
                    BlockPos at = new BlockPos(start + x, y, z);
                    if (!world.isAirBlock(at)) break;
                    world.setBlockState(at, Blocks.STONEBRICK.getDefaultState(), 2);
                }
                set(world, start + x, base - 1, z, Blocks.STONEBRICK.getDefaultState());
                set(world, start + x, base + 3, z, floor);
                boolean front = z == 0;
                if (!front && z != depth - 1 && x != 0 && x != width - 1) continue;
                EnumFacing facing = front ? EnumFacing.NORTH : z == depth - 1 ? EnumFacing.SOUTH : x == 0 ? EnumFacing.WEST : EnumFacing.EAST;
                int along = facing.getAxis() == EnumFacing.Axis.Z ? x : z;
                for (int y = 0; y < height; y++) {
                    if (y == 3) continue;
                    IBlockState state;
                    if (front && x == width / 2 && y < 2) {
                        state = Blocks.SPRUCE_DOOR.getStateFromMeta(y == 0 ? 3 : 8);
                    } else if (front && ArchitectureConfig.windows && windowBay(x, y, width, height, tileFront)) {
                        boolean recessed = tileFront && y >= 4;
                        state = ArchitectureBlocks.findWindow(wood, 1, recessed, tileFront, x < width / 2, recessed)
                            .getDefaultState().withProperty(WindowBlock.FACING, facing);
                    } else if (tileFront && front && y >= 4) {
                        state = thinTiles;
                    } else if (brickBase && y < 3) {
                        state = Blocks.BRICK_BLOCK.getDefaultState();
                    } else if (y == 0) {
                        state = ((x + z) % 5 == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).getDefaultState();
                    } else {
                        FrameFamily family = y == height - 1 ? FrameFamily.RAIL
                            : along % 3 == 0 ? FrameFamily.POST : FrameFamily.INFILL;
                        int panel = family == FrameFamily.INFILL ? 1 : y == height - 1 ? 3 : 2;
                        state = ArchitectureBlocks.findWall(family == FrameFamily.INFILL ? null : wood, family).getDefaultState()
                            .withProperty(FrameBlock.FACING, facing).withProperty(FrameBlock.PANEL, panel);
                    }
                    set(world, start + x, base + y, z, state);
                }
            }
        }

        // The entrance hood uses existing slabs and fence brackets.
        if (tileFront) {
            for (int x = width / 2 - 1; x <= width / 2 + 1; x++) {
                set(world, start + x, base + 2, -1, Blocks.WOODEN_SLAB.getStateFromMeta(5 | 8));
            }
            set(world, start + width / 2 - 1, base + 1, -1, Blocks.DARK_OAK_FENCE.getDefaultState());
            set(world, start + width / 2 + 1, base + 1, -1, Blocks.DARK_OAK_FENCE.getDefaultState());
        }

        int span = tileFront ? width : depth;
        for (int x = 0; x < width; x++) for (int z = 0; z < depth; z++) {
            int rise = roofRise(tileFront ? x : z, span);
            boolean gableEnd = tileFront ? z == 0 || z == depth - 1 : x == 0 || x == width - 1;
            if (gableEnd) {
                IBlockState gable = tileFront && z == 0 ? thinTiles : fullTiles;
                for (int y = 0; y < rise; y++) set(world, start + x, top + y, z, gable);
            }
        }

        RoofBlock slope = (RoofBlock) Block.getBlockFromName("ironagearchitecture:clay_tile_roof");
        RoofBlock cap = (RoofBlock) Block.getBlockFromName("ironagearchitecture:clay_tile_ridge");
        // Extend exposed edges by a row, except the recessed gable and the party walls.
        int firstX = tileFront || brickBase ? 0 : -1;
        int lastX = brickBase ? width : width - 1;
        int firstZ = tileFront ? 0 : -1;
        for (int x = firstX; x <= lastX; x++) for (int z = firstZ; z <= depth; z++) {
            int across = tileFront ? x : z;
            boolean apex = across == span / 2;
            EnumFacing facing = apex ? (tileFront ? EnumFacing.NORTH : EnumFacing.EAST)
                : tileFront ? (across < span / 2 ? EnumFacing.EAST : EnumFacing.WEST)
                : (across < span / 2 ? EnumFacing.SOUTH : EnumFacing.NORTH);
            RoofBlock piece = apex ? cap : slope;
            if (tileFront && z == 0) piece = apex ? ArchitectureBlocks.CLAY_RIDGE_END : ArchitectureBlocks.CLAY_VERGE;
            if (!tileFront && (z == -1 || z == depth)) piece = ArchitectureBlocks.CLAY_FASCIA;
            IBlockState roof = piece.getDefaultState().withProperty(RoofBlock.FACING, facing);
            if (piece == ArchitectureBlocks.CLAY_VERGE) roof = roof.withProperty(TrimmedRoofBlock.RIGHT, facing == EnumFacing.WEST);
            set(world, start + x, top + roofRise(across, span), z, roof);
        }

        if (tileFront) chimney(world, start + width / 2 + 1, top, top + span / 2 + 3, depth - 3);
        if (brickBase) chimney(world, start + 2, top, top + span / 2 + 2, depth / 2 + 1);
    }

    private static boolean windowBay(int x, int y, int width, int height, boolean tileFront) {
        if (y == 1 || y == 2) return x == 1 || x == width - 2;
        if (y < 4 || y >= height - (tileFront ? 0 : 1)) return false;
        return x == 1 || x == 2 || x == width - 3 || x == width - 2;
    }

    private static int roofRise(int across, int span) {
        return Math.min(across, span - 1 - across);
    }

    private static void chimney(WorldServer world, int x, int bottom, int top, int z) {
        for (int y = bottom; y < top; y++) set(world, x, y, z, Blocks.BRICK_BLOCK.getDefaultState());
        set(world, x, top, z, Blocks.FLOWER_POT.getDefaultState());
    }

    private static void set(WorldServer world, int x, int y, int z, IBlockState state) {
        world.setBlockState(new BlockPos(x, y, z), state, 2);
    }
}
