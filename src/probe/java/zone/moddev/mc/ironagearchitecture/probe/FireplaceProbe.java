package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.ArchitectureConfig;
import zone.moddev.mc.ironagearchitecture.block.FireplaceBlock;
import zone.moddev.mc.ironagearchitecture.block.FireplaceBlock.Kind;
import zone.moddev.mc.ironagearchitecture.block.FireplaceBlock.Masonry;
import zone.moddev.mc.ironagearchitecture.block.FireplaceDraft;
import zone.moddev.mc.ironagearchitecture.block.RoofGeometry;

final class FireplaceProbe {
    static void verify(WorldServer world, boolean reload) {
        if (!ArchitectureConfig.fireplaces) {
            require(ArchitectureBlocks.FIREPLACES.isEmpty(), "disabled registration");
            return;
        }
        require(ArchitectureBlocks.FIREPLACES.size() == (ArchitectureConfig.clayTiles ? 16 : 14), "registration");
        for (FireplaceBlock block : ArchitectureBlocks.FIREPLACES) {
            for (int meta = 0; meta < 16; meta++) {
                IBlockState state = block.getStateFromMeta(meta);
                require(block.getMetaFromState(state) == (meta & (block.kind == Kind.FIREBOX ? 7 : 3)), "metadata");
                require(block.damageDropped(state) == 0, "canonical drop");
            }
        }

        BlockPos lower = new BlockPos(95, 105, 95);
        BlockPos upper = lower.east().up(4);
        BlockPos first = lower.up(8), second = first.east();
        // Forge's light propagation needs the surrounding chunks loaded in this disposable fixture.
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            world.getChunkFromBlockCoords(first.add(16 * x, 0, 16 * z));
        if (reload) {
            require(!smokes(world, first) && smokes(world, second), "saved fires and draft");
            return;
        }
        IBlockState firebox = block(Masonry.STONE, Kind.FIREBOX).getDefaultState();
        IBlockState flue = block(Masonry.BRICK, Kind.FLUE).getDefaultState();
        IBlockState pot = block(Masonry.BRICK, Kind.POT_CAP).getDefaultState();
        world.setBlockState(lower, firebox, 3);
        world.setBlockState(upper, firebox, 3);
        for (int y = lower.getY() + 1; y < first.getY(); y++)
            world.setBlockState(new BlockPos(lower.getX(), y, lower.getZ()), flue, 3);
        for (int y = upper.getY() + 1; y < second.getY(); y++)
            world.setBlockState(new BlockPos(upper.getX(), y, upper.getZ()), flue, 3);
        world.setBlockState(first, pot, 3);
        world.setBlockState(second, pot, 3);
        require(!smokes(world, first) && !smokes(world, second), "cold outlets");
        FireplaceBlock chimney = (FireplaceBlock)flue.getBlock();
        require(chimney.joins(flue, world, first.down()) == 2
            && chimney.joins(flue, world, second.down()) == 1, "masonry seam across chunks");
        require(chimney.collisionRayTrace(flue, world, first.down(),
            point(first.down(), .5, -.1, .5), point(first.down(), .5, 1.1, .5)) == null, "hollow channel");
        require(chimney.collisionRayTrace(flue, world, first.down(),
            point(first.down(), .2, -.1, .5), point(first.down(), .2, 1.1, .5)) != null, "masonry targeting");

        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        ItemStack tool = new ItemStack(Items.FLINT_AND_STEEL);
        require(use(world, lower, player, tool) && tool.getItemDamage() == 1, "flint and steel");
        require(use(world, lower, player, tool) && tool.getItemDamage() == 1 && world.isAirBlock(lower.north()),
            "repeated click does not start vanilla fire");
        int emitted = world.getLightFor(EnumSkyBlock.BLOCK, lower);
        require(emitted == 13, "emitted block light (got " + emitted + ")");
        require(smokes(world, first) && !smokes(world, second), "only downstairs smokes");
        require(use(world, upper, player, tool) && smokes(world, first) && smokes(world, second), "both floors smoke");
        world.setBlockState(first.up(), Blocks.STONE.getDefaultState(), 3);
        require(!smokes(world, first) && smokes(world, second), "blocked pot independent");
        world.setBlockToAir(first.up());
        world.setBlockState(first, flue, 3);
        require(smokes(world, first), "open flue");
        world.setBlockState(first, block(Masonry.BRICK, Kind.SLAB_CAP).getDefaultState(), 3);
        require(smokes(world, first), "side vent");
        for (EnumFacing side : EnumFacing.HORIZONTALS)
            if (side != EnumFacing.EAST) world.setBlockState(first.offset(side), Blocks.STONE.getDefaultState(), 3);
        require(!smokes(world, first) && smokes(world, second), "blocked side vents");
        for (EnumFacing side : EnumFacing.HORIZONTALS) if (side != EnumFacing.EAST) world.setBlockToAir(first.offset(side));
        world.setBlockState(first, pot, 3);
        world.setBlockToAir(first.down());
        require(!smokes(world, first) && smokes(world, second), "disconnected channel");
        world.setBlockState(first.down(), flue, 3);
        player.setSneaking(true);
        try { require(use(world, lower, player, null), "snuff fire"); }
        finally { player.setSneaking(false); }
        require(!smokes(world, first) && smokes(world, second)
            && world.getLightFor(EnumSkyBlock.BLOCK, lower) < 13, "only upstairs smokes after snuffing");

        if (ArchitectureConfig.clayTiles) {
            FireplaceBlock collar = block(Masonry.BRICK, Kind.COLLAR);
            for (EnumFacing facing : EnumFacing.HORIZONTALS) {
                BlockPos pos = first.down(2);
                IBlockState state = collar.getDefaultState().withProperty(FireplaceBlock.FACING, facing);
                world.setBlockState(pos, state, 3);
                require(collar.joins(state, world, pos) != 0, "paired roof collar " + facing);
                world.setBlockToAir(pos.east());
                double[] high = RoofGeometry.rotate(.06, .65, .1, facing);
                double[] low = RoofGeometry.rotate(.06, .65, .9, facing);
                boolean firstHit = collar.boxes(state, world, pos).stream()
                    .anyMatch(box -> box.isVecInside(new Vec3d(high[0], high[1], high[2])));
                boolean secondHit = collar.boxes(state, world, pos).stream()
                    .anyMatch(box -> box.isVecInside(new Vec3d(low[0], low[1], low[2])));
                require(firstHit != secondHit, "roof collar slope " + facing);
                world.setBlockState(pos.east(), flue, 3);
            }
        }
        BlockPos tall = first.south(4).up(32);
        for (int distance = 1; distance <= FireplaceDraft.MAX_DEPTH; distance++)
            world.setBlockState(tall.down(distance), flue, 2);
        world.setBlockState(tall, pot, 2);
        world.setBlockState(tall.down(FireplaceDraft.MAX_DEPTH + 1), firebox.withProperty(FireplaceBlock.LIT, true), 2);
        require(!smokes(world, tall), "scan stops after 32 blocks");
        world.setBlockState(tall.down(FireplaceDraft.MAX_DEPTH), firebox.withProperty(FireplaceBlock.LIT, true), 2);
        require(smokes(world, tall), "32nd block is included");
        System.out.println("IAA_FIREPLACE_PASS twoFloors=true separateDraft=true chunkBorder=true light=true caps=3 scan=32");
    }

    private static Vec3d point(BlockPos pos, double x, double y, double z) {
        return new Vec3d(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
    }

    private static boolean use(WorldServer world, BlockPos pos, FakePlayer player, ItemStack held) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().onBlockActivated(world, pos, state, player, EnumHand.MAIN_HAND, held, EnumFacing.NORTH, .5F, .3F, .5F);
    }

    private static boolean smokes(WorldServer world, BlockPos pos) {
        return FireplaceDraft.smokeOutlet(world, pos, world.getBlockState(pos)) != null;
    }

    private static FireplaceBlock block(Masonry masonry, Kind kind) {
        return ArchitectureBlocks.fireplace(masonry, kind);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Fireplace probe failed: " + message);
    }
}
