package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.ArchitectureConfig;
import zone.moddev.mc.ironagearchitecture.block.*;

final class ClayTileProbe {
    private static final String PREFIX = "ironagearchitecture:clay_tile_";

    static boolean isClay(Block block) {
        return block instanceof DownpipeBlock || block instanceof TileHangingBlock || block instanceof RoofBlock && "clay_tile".equals(((RoofBlock) block).materialKey);
    }

    static void verify(WorldServer world) {
        if (!ArchitectureConfig.clayTiles) {
            for (String name : new String[]{"roof", "ridge", "hanging", "hanging_thin", "fascia", "verge", "ridge_end"}) {
                require(Block.getBlockFromName(PREFIX + name) == null, "Disabled tile block registered: " + name);
            }
            return;
        }
        TileHangingBlock thin = ArchitectureBlocks.CLAY_HANGING_THIN;
        RidgeBlock ridge = (RidgeBlock) Block.getBlockFromName(PREFIX + "ridge");
        BlockPos pos = new BlockPos(87, 100, -84);
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            IBlockState panel = thin.getDefaultState().withProperty(TileHangingBlock.FACING, facing);
            world.setBlockState(pos, panel, 2);
            Vec3d solid = point(pos, 8, .5, 4, facing);
            Vec3d empty = point(pos, 8, .5, 12, facing);
            require(!world.getCollisionBoxes(player, around(solid)).isEmpty(), "Missing tile collision " + facing);
            require(world.getCollisionBoxes(player, around(empty)).isEmpty(), "Collision in empty tile half " + facing);
            require(thin.collisionRayTrace(panel, world, pos, solid.addVector(0, 1, 0), solid.addVector(0, -1, 0)) != null,
                "Tile cannot be targeted " + facing);
            require(thin.collisionRayTrace(panel, world, pos, empty.addVector(0, 1, 0), empty.addVector(0, -1, 0)) == null,
                "Empty tile half is targetable " + facing);

            IBlockState cap = ridge.getDefaultState().withProperty(RidgeBlock.FACING, facing);
            BlockPos capPos = pos.up(4);
            world.setBlockState(capPos, cap, 2);
            require(ridge.getActualState(cap, world, capPos).getValue(RidgeBlock.ENDS) == RidgeBlock.End.SINGLE, "Single clay cap");
            world.setBlockState(capPos.offset(facing), cap.withProperty(RidgeBlock.FACING, facing.getOpposite()), 2);
            require(ridge.getActualState(cap, world, capPos).getValue(RidgeBlock.ENDS) == RidgeBlock.End.END, "Clay cap end");
            world.setBlockState(capPos.offset(facing.getOpposite()), cap, 2);
            require(ridge.getActualState(cap, world, capPos).getValue(RidgeBlock.ENDS) == RidgeBlock.End.MIDDLE, "Joined clay caps");
            world.setBlockToAir(capPos.offset(facing));
            world.setBlockToAir(capPos.offset(facing.getOpposite()));

            if (ArchitectureConfig.frames) {
                JettyFloorBlock floor = ArchitectureBlocks.findFloor("oak", FloorShape.FULL, null);
                IBlockState deck = floor.getDefaultState().withProperty(FrameBlock.FACING, facing);
                require((floor.connections(deck, world, pos.up()) >> 16) == 3, "Tile wall supports wrong deck quarters " + facing);
                FrameBlock wall = ArchitectureConfig.buildingMode.manual()?ArchitectureBlocks.findPartial("oak", FrameFamily.POST, WallProfile.THIN):ArchitectureBlocks.findSmart("oak",1);
                IBlockState placed = wall.getStateForPlacement(world, pos.up(), EnumFacing.UP, .5F, 1, .5F, 0, player);
                require(placed.getValue(FrameBlock.FACING) == facing, "Timber did not inherit the tile plane");
                world.setBlockState(pos, placed, 2);
                require(thin.getStateForPlacement(world, pos.up(), EnumFacing.UP, .5F, 1, .5F, 0, player)
                    .getValue(TileHangingBlock.FACING) == facing, "Tile did not inherit the timber plane");
            }
            if (ArchitectureConfig.windows) verifyWindowPlacement(world, player, pos, panel, facing);
            world.setBlockToAir(pos);
            world.setBlockToAir(capPos);
        }
        verifyRoofTrim(world, player, pos);
        verifyEntityPlacement(world, player, pos);
        System.out.println("IAA_CLAY_TILE_PASS fourFacings=true collision=true ray=true ridge=true windowAlignment=" + ArchitectureConfig.windows);
    }

    private static void verifyRoofTrim(WorldServer world, FakePlayer player, BlockPos pos) {
        TrimmedRoofBlock verge = ArchitectureBlocks.CLAY_VERGE;
        TrimmedRoofBlock fascia = ArchitectureBlocks.CLAY_FASCIA;
        TrimmedRoofBlock end = ArchitectureBlocks.CLAY_RIDGE_END;
        RidgeBlock ridge = (RidgeBlock) Block.getBlockFromName(PREFIX + "ridge");
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            player.rotationYaw = facing.getHorizontalIndex() * 90;
            for (boolean right : new boolean[]{false, true}) {
                double lip = right ? .75 : .25;
                double[] hit = RoofGeometry.rotate(lip, 1, .25, facing);
                IBlockState state = verge.getStateForPlacement(world, pos, EnumFacing.UP,
                    (float)hit[0], 1, (float)hit[2], 0, player);
                require(state.getValue(RoofBlock.FACING) == facing && state.getValue(TrimmedRoofBlock.RIGHT) == right,
                    "Verge lost its placement hand " + facing);
                require(verge.getStateFromMeta(verge.getMetaFromState(state)).equals(state), "Verge metadata " + facing);
                world.setBlockState(pos, state, 2);
                Vec3d below = roofPoint(pos, lip, .25, .25, facing);
                Vec3d support = roofPoint(pos, 1-lip, .25, .25, facing);
                Vec3d board = roofPoint(pos, lip, .67, .25, facing);
                require(world.getCollisionBoxes(player, around(below)).isEmpty(), "Verge blocks clear space beneath lip");
                require(!world.getCollisionBoxes(player, around(support)).isEmpty(), "Verge lost its solid half");
                require(verge.collisionRayTrace(state, world, pos, board.addVector(0, .02, 0), board.addVector(0, -.3, 0)) != null,
                    "Verge board is not targetable");
            }
            IBlockState eave = fascia.getDefaultState().withProperty(RoofBlock.FACING, facing);
            world.setBlockState(pos, eave, 2);
            require(world.getCollisionBoxes(player, around(roofPoint(pos, .5, .07, .94, facing))).isEmpty(),
                "Fascia margin is not clear");
            require(!world.getCollisionBoxes(player, around(roofPoint(pos, .5, .1, .75, facing))).isEmpty(),
                "Fascia board has no collision");

            IBlockState cap = ridge.getDefaultState().withProperty(RoofBlock.FACING, facing);
            world.setBlockState(pos, cap, 2);
            world.setBlockState(pos.offset(facing), end.getDefaultState().withProperty(RoofBlock.FACING, facing), 2);
            require(ridge.getActualState(cap, world, pos).getValue(RidgeBlock.ENDS) == RidgeBlock.End.END,
                "Ridge does not meet trim end");
            world.setBlockToAir(pos.offset(facing));
            world.setBlockToAir(pos);
        }
        System.out.println("IAA_ROOF_TRIM_PASS facings=4 vergeHands=2 eaveClearance=true ridgeJoin=true");
    }

    private static Vec3d roofPoint(BlockPos pos, double x, double y, double z, EnumFacing facing) {
        double[] p = RoofGeometry.rotate(x, y, z, facing);
        return new Vec3d(pos.getX()+p[0], pos.getY()+p[1], pos.getZ()+p[2]);
    }

    private static void verifyWindowPlacement(WorldServer world, FakePlayer player, BlockPos pos,
            IBlockState panel, EnumFacing facing) {
        for (boolean inside : new boolean[]{false, true}) {
            world.setBlockState(pos, panel, 2);
            Vec3d standing = point(pos, 8, 0, inside ? 48 : -32, facing);
            player.setPosition(standing.xCoord, standing.yCoord, standing.zCoord);
            player.rotationYaw = facing.getOpposite().getHorizontalIndex() * 90;
            ItemStack glazing = new ItemStack(ArchitectureBlocks.findWindow("oak", 1, true, true, false));
            require(place(glazing, player, world, pos, EnumFacing.UP, .5F, .5F), "Window could not be placed against tiles");
            IBlockState window = world.getBlockState(pos.up());
            WindowBlock block = (WindowBlock) window.getBlock();
            require(window.getValue(WindowBlock.FACING) == (inside ? facing.getOpposite() : facing)
                && block.back == inside, "Window lost tile plane or outward facing");
            ItemStack tile = new ItemStack(panel.getBlock());
            player.setHeldItem(EnumHand.MAIN_HAND, tile);
            require(!block.onBlockActivated(world, pos.up(), window, player, EnumHand.MAIN_HAND, tile, facing.rotateY(), .5F, .5F, .5F),
                "Tile item toggles the window");
            EnumFacing lateral = facing.rotateY();
            require(place(tile, player, world, pos.up(), lateral, .5F, .5F), "Could not place beside the thin window");
            BlockPos adjacent = pos.up().offset(lateral);
            require(world.getBlockState(adjacent).equals(panel), "Tile failed to inherit window half " + facing + " inside=" + inside);
            world.setBlockToAir(adjacent);
            world.setBlockToAir(pos.up());
            player.setHeldItem(EnumHand.MAIN_HAND, null);
        }
    }

    private static void verifyEntityPlacement(WorldServer world, FakePlayer player, BlockPos pos) {
        TileHangingBlock panel = ArchitectureBlocks.CLAY_HANGING_THIN;
        world.setBlockState(pos.down(), panel.getDefaultState(), 2);
        player.setPosition(pos.getX() + 2, pos.getY(), pos.getZ() + 2);
        player.rotationYaw = 0;
        player.setSneaking(true);
        EntityArmorStand stand = new EntityArmorStand(world, pos.getX() + .5, pos.getY(), pos.getZ() + .25);
        require(world.spawnEntity(stand), "Could not spawn tile placement obstruction");
        try {
            ItemStack stack = new ItemStack(panel, 2);
            require(!place(stack, player, world, pos.down(), EnumFacing.UP, .5F, .25F) && stack.stackSize == 2,
                "Thin tile overlaps an entity in its chosen half");
            require(place(stack, player, world, pos.down(), EnumFacing.UP, .5F, .75F)
                && world.getBlockState(pos).getValue(TileHangingBlock.FACING) == EnumFacing.SOUTH,
                "Sneaking failed to choose the free half");
        } finally {
            world.removeEntityDangerously(stand);
            world.setBlockToAir(pos);
            world.setBlockToAir(pos.down());
            player.setSneaking(false);
        }
    }

    private static boolean place(ItemStack stack, FakePlayer player, WorldServer world, BlockPos pos, EnumFacing side, float x, float z) {
        return stack.getItem().onItemUse(stack, player, world, pos, EnumHand.MAIN_HAND, side, x, 1, z) == EnumActionResult.SUCCESS;
    }

    private static AxisAlignedBB around(Vec3d p) {
        return new AxisAlignedBB(p.xCoord-.05, p.yCoord-.05, p.zCoord-.05, p.xCoord+.05, p.yCoord+.05, p.zCoord+.05);
    }

    private static Vec3d point(BlockPos pos, double x, double y, double z, EnumFacing facing) {
        double[] p = WallGeometry.rotate(x, 0, z, facing);
        return new Vec3d(pos.getX()+p[0]/16, pos.getY()+y, pos.getZ()+p[2]/16);
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }
}
