package zone.moddev.mc.ironagearchitecture.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.creativetab.CreativeTabs;
import zone.moddev.mc.ironagearchitecture.EraTabs;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import java.util.List;

public class EraItemBlock extends ItemBlock {
    public EraItemBlock(Block block) {super(block);}
    @Override public CreativeTabs[] getCreativeTabs() {return EraTabs.forItem(this);}
    protected static void hint(List<String> tooltip,String key) {tooltip.add(net.minecraft.util.text.translation.I18n.translateToLocal("hint.ironagearchitecture."+key));}
    @Override public void addInformation(ItemStack stack,EntityPlayer player,List<String> tooltip,boolean advanced) {
        if(block instanceof RoofBlock) hint(tooltip,((RoofBlock)block).isRidge()?"roof.slope":"roof.ridge");
        if(ArchitectureBlocks.COB.contains(block)) hint(tooltip,"cob");
        if(ArchitectureBlocks.STAIRS.contains(block)) hint(tooltip,"stair");
        if(ArchitectureBlocks.COB.indexOf(block)==0||ArchitectureBlocks.STAIRS.indexOf(block)==0) hint(tooltip,"wash");
    }
}
