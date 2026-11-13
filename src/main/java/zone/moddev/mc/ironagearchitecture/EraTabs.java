package zone.moddev.mc.ironagearchitecture;

import java.util.EnumMap;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;

/** Creative availability is independent of the server's survival recipe selection. */
public final class EraTabs {
    private static final EnumMap<Era,CreativeTabs> TABS=new EnumMap<>(Era.class);
    static {
        for(final Era era:Era.values()) TABS.put(era,new CreativeTabs("ironagearchitecture."+era.id) {
            @Override public void displayAllRelevantItems(java.util.List<net.minecraft.item.ItemStack> items) {
                super.displayAllRelevantItems(items);
                if(era==Era.LATE_MEDIEVAL_TUDOR)
                    items.sort(java.util.Comparator.comparingInt(stack->stack.getItem() instanceof zone.moddev.mc.ironagearchitecture.block.SmartWallItem?0:1));
            }
            @Override public Item getTabIconItem() {
                if(era==Era.LATE_MEDIEVAL_TUDOR&&ArchitectureConfig.buildingMode.automatic()&&!ArchitectureBlocks.SMART_WALLS.isEmpty())
                    return Item.getItemFromBlock(ArchitectureBlocks.findSmart("oak",0));
                if(era==Era.LATE_MEDIEVAL_TUDOR&&!ArchitectureBlocks.FRAMES.isEmpty()) return Item.getItemFromBlock(ArchitectureBlocks.FRAMES.get(0));
                if(!ArchitectureBlocks.COB.isEmpty()) return Item.getItemFromBlock(ArchitectureBlocks.COB.get(0));
                return Item.getItemFromBlock(Blocks.BRICK_BLOCK);
            }
        });
    }
    private EraTabs() { }
    public static CreativeTabs tab(Era era) {return TABS.get(era);}
    public static CreativeTabs[] forItem(Item item) {return EraCatalog.eras(item).stream().map(EraTabs::tab).toArray(CreativeTabs[]::new);}
}
