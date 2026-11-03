package zone.moddev.mc.ironagearchitecture.block;

import net.minecraft.block.*;
import net.minecraft.item.ItemSlab;
import net.minecraft.creativetab.CreativeTabs;
import zone.moddev.mc.ironagearchitecture.EraTabs;

public final class EraItemSlab extends ItemSlab {
    public EraItemSlab(Block block,BlockSlab half,BlockSlab doubled) {super(block,half,doubled);}
    @Override public CreativeTabs[] getCreativeTabs() {return EraTabs.forItem(this);}
    @Override public void addInformation(net.minecraft.item.ItemStack stack,net.minecraft.entity.player.EntityPlayer player,java.util.List<String> tooltip,boolean advanced) {
        EraItemBlock.hint(tooltip,"slab");
        if(zone.moddev.mc.ironagearchitecture.ArchitectureBlocks.SLABS.indexOf(block)==0) EraItemBlock.hint(tooltip,"wash");
    }
}
