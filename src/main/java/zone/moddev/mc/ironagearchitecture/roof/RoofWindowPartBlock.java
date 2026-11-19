package zone.moddev.mc.ironagearchitecture.roof;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.*;
import net.minecraftforge.common.property.*;

/** Helpers have exactly one saved state and no item. */
public final class RoofWindowPartBlock extends RoofWindowBlock {
    public RoofWindowPartBlock(){super(true);}
    @Override protected BlockStateContainer createBlockState(){return new ExtendedBlockState(this,new IProperty<?>[0],new IUnlistedProperty<?>[]{RoofFrameBlock.CELL});}
    @Override public IBlockState getStateFromMeta(int m){return getDefaultState();}
    @Override public int getMetaFromState(IBlockState state){return 0;}
}
