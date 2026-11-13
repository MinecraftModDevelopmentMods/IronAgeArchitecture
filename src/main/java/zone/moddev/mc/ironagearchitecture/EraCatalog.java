package zone.moddev.mc.ironagearchitecture;

import java.util.EnumSet;
import net.minecraft.block.Block;
import net.minecraft.item.*;
import zone.moddev.mc.ironagearchitecture.block.*;

/** One membership definition supplies both creative browsing and survival recipes. */
public final class EraCatalog {
    private EraCatalog() { }
    public static EnumSet<Era> eras(Item item) {
        if(item==null||item.getRegistryName()==null||!IronAgeArchitecture.MODID.equals(item.getRegistryName().getResourceDomain())) return EnumSet.noneOf(Era.class);
        if(item instanceof JettyBracketItem||item instanceof LeadedPaneItem||item instanceof GutterItem||item instanceof SoffitItem) return EnumSet.of(Era.LATE_MEDIEVAL_TUDOR);
        if(!(item instanceof ItemBlock)) return EnumSet.noneOf(Era.class);
        Block block=((ItemBlock)item).getBlock();
        boolean shared=block instanceof SmartCobBlock||ArchitectureBlocks.COB.contains(block)||ArchitectureBlocks.SLABS.contains(block)||ArchitectureBlocks.STAIRS.contains(block)
            ||block instanceof FrameBlock&&((FrameBlock)block).family==FrameFamily.INFILL
            ||block instanceof RoofBlock&&"thatch".equals(((RoofBlock) block).materialKey);
        return shared?EnumSet.allOf(Era.class):EnumSet.of(Era.LATE_MEDIEVAL_TUDOR);
    }
    public static boolean canCraft(ItemStack output) {return output!=null&&ArchitectureConfig.survivalEras.permits(eras(output.getItem()));}
    public static void assignPrimaryTabs() {
        for(Block block:ArchitectureBlocks.VISIBLE) {
            Item item=Item.getItemFromBlock(block);
            Era era=eras(item).iterator().next();
            block.setCreativeTab(EraTabs.tab(era));item.setCreativeTab(EraTabs.tab(era));
        }
        for(JettyBracketItem item:ArchitectureBlocks.BRACKETS) item.setCreativeTab(EraTabs.tab(Era.LATE_MEDIEVAL_TUDOR));
    }
}
