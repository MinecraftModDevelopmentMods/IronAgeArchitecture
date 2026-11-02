package zone.moddev.mc.ironagearchitecture.block;

import java.util.Random;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** One finish per slab family, with normal vanilla double-slab handling. */
public final class CobSlab extends BlockSlab {
    private static final PropertyBool VARIANT = PropertyBool.create("variant");
    private final boolean doubled;
    private CobSlab half;
    public CobSlab(boolean doubled) {
        super(Material.GROUND);
        this.doubled = doubled;
        setHardness(1.2F); setResistance(4F); setSoundType(SoundType.GROUND); setHarvestLevel("shovel", 0);
        setDefaultState(blockState.getBaseState().withProperty(VARIANT, false).withProperty(HALF, EnumBlockHalf.BOTTOM));
    }
    public void setHalf(CobSlab half) { this.half = half; }
    @Override public boolean isDouble() { return doubled; }
    @Override public String getUnlocalizedName(int meta) { return getUnlocalizedName(); }
    @Override public IProperty<?> getVariantProperty() { return VARIANT; }
    @Override public Comparable<?> getTypeForItem(ItemStack stack) { return false; }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, VARIANT, HALF); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(HALF, (meta & 8) == 0 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP); }
    @Override public int getMetaFromState(IBlockState state) { return !doubled && state.getValue(HALF) == EnumBlockHalf.TOP ? 8 : 0; }
    @Override public int damageDropped(IBlockState state) { return 0; }
    @Override public Item getItemDropped(IBlockState state, Random random, int fortune) { return Item.getItemFromBlock(half); }
}
