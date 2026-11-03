package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import zone.moddev.mc.ironagearchitecture.EraTabs;

public final class LeadedPaneItem extends Item {
    public LeadedPaneItem(){setHasSubtypes(true);setMaxDamage(0);}
    @Override public CreativeTabs[] getCreativeTabs(){return EraTabs.forItem(this);}
    @Override public String getUnlocalizedName(ItemStack stack){return super.getUnlocalizedName()+"."+Math.floorMod(stack.getMetadata(),2);}
    @Override public void getSubItems(Item item,CreativeTabs tab,List<ItemStack> out){out.add(new ItemStack(item,1,0));out.add(new ItemStack(item,1,1));}
    @Override public void addInformation(ItemStack stack,EntityPlayer player,List<String> tooltip,boolean advanced){EraItemBlock.hint(tooltip,"pane.use");}
}
