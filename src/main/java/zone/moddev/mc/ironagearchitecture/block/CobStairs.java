package zone.moddev.mc.ironagearchitecture.block;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;

/** Uses the vanilla stair placement and corner solver. */
public final class CobStairs extends BlockStairs {
    public CobStairs(IBlockState base) { super(base); setHarvestLevel("shovel", 0); }
}
