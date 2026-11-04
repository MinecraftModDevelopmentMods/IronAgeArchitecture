package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
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
import zone.moddev.mc.ironagearchitecture.block.DownpipeBlock;
import zone.moddev.mc.ironagearchitecture.block.DrainGeometry;
import zone.moddev.mc.ironagearchitecture.block.DrainRun;
import zone.moddev.mc.ironagearchitecture.block.FasciaBlock;

final class GutterProbe {
    private GutterProbe() { }

    static int verifyOldPreview(WorldServer world) {
        FasciaBlock fascia = ArchitectureBlocks.CLAY_FASCIA;
        int count = 0;
        for (int[] run : new int[][]{{-58,-51,20}, {-41,-34,15}}) {
            for (int x = run[0]; x <= run[1]; x++) {
                for (int z : new int[]{-1,7}) {
                    EnumFacing facing = z == -1 ? EnumFacing.SOUTH : EnumFacing.NORTH;
                    IBlockState old = world.getBlockState(new BlockPos(x, run[2], z));
                    require(old.equals(fascia.getDefaultState().withProperty(FasciaBlock.FACING, facing)),
                        "Original fascia changed at " + x + "," + run[2] + "," + z);
                    count++;
                }
            }
        }
        System.out.println("IAA_LEGACY_FASCIA_PASS bareFascias=" + count);
        return count;
    }

    static void verify(WorldServer world) {
        if (ArchitectureBlocks.GUTTER == null) {
            for (String id : new String[]{"iron_gutter", "iron_gutter_outlet", "iron_downpipe", "iron_downpipe_offset", "iron_downpipe_shoe"})
                require(Item.getByNameOrId("ironagearchitecture:" + id) == null, "Disabled drain item " + id);
            return;
        }
        BlockPos pos = new BlockPos(95, 100, 95);
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        try {
            for (EnumFacing facing : EnumFacing.HORIZONTALS) verifyFacing(world, player, pos, facing);
        } finally {
            player.setSneaking(false);
            player.setHeldItem(EnumHand.MAIN_HAND, null);
        }
        System.out.println("IAA_GUTTER_PASS facings=4 chunkJoin=true bothOffsets=true rainGuards=true shoeAbovePaving=true");
    }

    private static void verifyFacing(WorldServer world, FakePlayer player, BlockPos pos, EnumFacing facing) {
        FasciaBlock fascia = ArchitectureBlocks.CLAY_FASCIA;
        EnumFacing outside = facing.getOpposite();
        IBlockState bare = fascia.getStateFromMeta(facing.getHorizontalIndex());
        require(bare.getValue(FasciaBlock.FITTING) == 0, "Legacy fascia gained a fitting");
        world.setBlockState(pos, bare, 2);
        player.setSneaking(false);
        player.setPosition(pos.getX()-4, pos.getY(), pos.getZ()-4);
        player.rotationYaw = outside.getHorizontalIndex() * 90;
        ItemStack outlet = new ItemStack(ArchitectureBlocks.GUTTER_OUTLET, 2);
        require(use(world, player, pos, outlet, facing) == EnumActionResult.PASS && outlet.stackSize == 2,
            "Fitting attached to the inner face");
        require(use(world, player, pos, outlet, outside) == EnumActionResult.SUCCESS && outlet.stackSize == 1,
            "Fitting did not attach from outside");
        IBlockState fitted = world.getBlockState(pos);
        require(fitted.getValue(FasciaBlock.FITTING) == 2 && pos.equals(DrainRun.exposedInletForDrip(fitted, world, pos)),
            "Exposed outlet did not drip");
        require(use(world, player, pos, outlet, outside) == EnumActionResult.FAIL && outlet.stackSize == 1,
            "Fitting attached twice");

        BlockPos left = pos.offset(facing.rotateYCCW()), right = pos.offset(facing.rotateY());
        world.setBlockState(left, bare.withProperty(FasciaBlock.FITTING, 1), 2);
        world.setBlockState(right, bare.withProperty(FasciaBlock.FITTING, 1), 2);
        require(fascia.getActualState(fitted, world, pos).getValue(FasciaBlock.ENDS) == 0, "Joined gutter has end caps");
        world.setBlockState(right, bare.withProperty(FasciaBlock.FITTING, 1)
            .withProperty(FasciaBlock.FACING, facing.getOpposite()), 2);
        require(fascia.getActualState(fitted, world, pos).getValue(FasciaBlock.ENDS) == 2, "Reversed gutter joined");
        world.setBlockToAir(left);
        world.setBlockToAir(right);

        DownpipeBlock pipe = ArchitectureBlocks.DOWNPIPE, offset = ArchitectureBlocks.DOWNPIPE_OFFSET;
        BlockPos first = pos.down(), elbow = first.down(), inner = elbow.offset(facing), foot = inner.down();
        require(use(world, player, pos, new ItemStack(pipe), EnumFacing.DOWN) == EnumActionResult.SUCCESS
            && world.getBlockState(first).equals(pipe.state(facing, 2)), "Pipe failed to inherit the outlet port");
        require(DrainRun.exposedInletForDrip(fitted, world, pos) == null
            && pos.equals(DrainRun.exposedInletForDrip(world.getBlockState(first), world, first)),
            "Drip did not move to the bottom of the pipe");
        require(use(world, player, first, new ItemStack(offset, 1, 1), EnumFacing.DOWN) == EnumActionResult.SUCCESS
            && world.getBlockState(elbow).equals(offset.state(facing, 1)), "Half offset failed to inherit the outer port");
        require(use(world, player, elbow, new ItemStack(pipe), EnumFacing.DOWN) == EnumActionResult.SUCCESS
            && world.getBlockState(elbow.down()).equals(pipe.state(facing, 1)), "Half lane did not line up");
        require(pos.equals(DrainRun.exposedInletForDrip(world.getBlockState(elbow.down()), world, elbow.down())), "Half route is disconnected");
        world.setBlockToAir(elbow.down());

        world.setBlockState(first, offset.state(facing, 0), 2);
        world.setBlockState(elbow, offset.state(facing, 2), 2);
        require(use(world, player, elbow, new ItemStack(offset, 1, 3), facing) == EnumActionResult.SUCCESS
            && world.getBlockState(inner).equals(offset.state(facing, 3)), "Full offset failed at its side port");
        require(use(world, player, inner, new ItemStack(pipe), EnumFacing.DOWN) == EnumActionResult.SUCCESS
            && world.getBlockState(foot).equals(pipe.state(facing, 0)), "Wall lane did not line up");
        IBlockState footState = world.getBlockState(foot);
        require(pos.equals(DrainRun.exposedInletForDrip(footState, world, foot)), "Full offset lost its rain path");

        Vec3d start = DrainGeometry.point(7, 0, facing).addVector(foot.getX(), foot.getY(), foot.getZ());
        Vec3d end = DrainGeometry.point(7, 16, facing).addVector(foot.getX(), foot.getY(), foot.getZ());
        require(pipe.collisionRayTrace(footState, world, foot, end, start) != null
            && pipe.collisionRayTrace(footState, world, foot, end.addVector(.3,0,.3), start.addVector(.3,0,.3)) == null,
            "Pipe ray covered empty space or missed the pipe");
        for (int profile = 0; profile < 4; profile++)
            for (AxisAlignedBB box : offset.boxes(offset.state(facing, profile)))
                require(box.minX >= 0 && box.minY >= 0 && box.minZ >= 0
                    && box.maxX <= 1 && box.maxY <= 1 && box.maxZ <= 1, "Elbow collision escaped its cell");

        verifyShoe(world, player, pos, foot, facing);

        world.setBlockState(foot.down(), Blocks.STONE.getDefaultState(), 2);
        require(DrainRun.exposedInletForDrip(footState, world, foot) == null, "Blocked pipe dripped");
        world.setBlockToAir(foot.down());
        world.setBlockState(pos.up(), Blocks.STONE.getDefaultState(), 2);
        require(DrainRun.exposedInletForDrip(footState, world, foot) == null, "Sheltered inlet dripped");
        world.setBlockToAir(pos.up());
        if (!world.isRaining()) require(!DrainRun.canDrip(footState, world, foot), "Dry inlet dripped");
        world.setBlockToAir(inner);
        require(DrainRun.exposedInletForDrip(footState, world, foot) == null, "Disconnected pipe dripped");
        world.setBlockToAir(foot);
        world.setBlockToAir(elbow);
        world.setBlockToAir(first);

        require(fascia.getDrops(world, pos, fitted, 0).stream().anyMatch(s -> s.getItem() == ArchitectureBlocks.GUTTER_OUTLET),
            "Harvest lost its outlet");
        player.setSneaking(true);
        player.setHeldItem(EnumHand.MAIN_HAND, null);
        player.setHeldItem(EnumHand.OFF_HAND, null);
        Vec3d hit = DrainGeometry.point(2, 15.8, facing);
        require(fascia.onBlockActivated(world, pos, fitted, player, EnumHand.MAIN_HAND, null, outside,
            (float)hit.xCoord, (float)hit.yCoord, (float)hit.zCoord) && world.getBlockState(pos).equals(bare),
            "Empty hand could not recover the fitting");
        world.setBlockToAir(pos);
    }

    private static void verifyShoe(WorldServer world, FakePlayer player, BlockPos inlet, BlockPos pipePos, EnumFacing facing) {
        DownpipeBlock shoe = ArchitectureBlocks.DOWNPIPE_SHOE;
        BlockPos at = pipePos.down();
        world.setBlockState(at.down(), Blocks.COBBLESTONE.getDefaultState(), 2);
        IBlockState original = world.getBlockState(pipePos);
        try {
            for (int lane = 0; lane < 3; lane++) {
                world.setBlockState(pipePos, ArchitectureBlocks.DOWNPIPE.state(facing, lane), 2);
                require(use(world, player, pipePos, new ItemStack(shoe), EnumFacing.DOWN) == EnumActionResult.SUCCESS,
                    "Could not place shoe above paving");
                IBlockState state = world.getBlockState(at);
                require(state.equals(shoe.state(facing, lane)), "Shoe did not inherit its pipe lane");
                double[][] path = shoe.path(state);
                double[] mouth = path[path.length - 1];
                Vec3d from = DrainGeometry.point(mouth[0], 16, facing).addVector(at.getX(), at.getY(), at.getZ());
                Vec3d to = DrainGeometry.point(mouth[0], mouth[1] - .5, facing).addVector(at.getX(), at.getY(), at.getZ());
                require(shoe.collisionRayTrace(state, world, at, from, to) != null, "Angled shoe mouth could not be targeted");
                require(shoe.getBoundingBox(state, world, at).minY > 0 && !state.getMaterial().isSolid(),
                    "Shoe has no space for drips above paving");
                if (lane == 0) {
                    require(inlet.equals(DrainRun.exposedInletForDrip(state, world, at)),
                        "Connected shoe could not discharge over paving");
                    world.setBlockToAir(pipePos);
                    require(DrainRun.exposedInletForDrip(state, world, at) == null, "Disconnected shoe dripped");
                }
                world.setBlockToAir(at);
            }
        } finally {
            world.setBlockState(pipePos, original, 2);
            world.setBlockToAir(at.down());
        }
    }

    private static EnumActionResult use(WorldServer world, FakePlayer player, BlockPos pos, ItemStack stack, EnumFacing side) {
        return stack.getItem().onItemUse(stack, player, world, pos, EnumHand.MAIN_HAND, side, .5F, .1F, .5F);
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }
}
