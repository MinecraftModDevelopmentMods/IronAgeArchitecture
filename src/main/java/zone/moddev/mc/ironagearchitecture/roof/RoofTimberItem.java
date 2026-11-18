package zone.moddev.mc.ironagearchitecture.roof;

import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import zone.moddev.mc.ironagearchitecture.*;

/** Recoverable construction units; geometry is assigned by the drawing, never item metadata. */
public final class RoofTimberItem extends Item {
    public RoofTimberItem(){setHasSubtypes(true);setMaxDamage(0);setCreativeTab(EraTabs.tab(Era.IRON_AGE));}
    @Override public String getUnlocalizedName(ItemStack stack){return super.getUnlocalizedName()+"."+Math.floorMod(stack.getMetadata(),6);}
    @Override public CreativeTabs[] getCreativeTabs(){return EraTabs.forItem(this);}
    @Override public void getSubItems(Item item,CreativeTabs tab,List<ItemStack> items){if(ArchitectureConfig.buildingMode.automatic())for(int i=0;i<6;i++)items.add(new ItemStack(item,1,i));}
    @Override public void addInformation(ItemStack stack,EntityPlayer player,List<String> lines,boolean advanced){for(String key:new String[]{"use","craft"})lines.add(net.minecraft.util.text.translation.I18n.translateToLocal("hint.ironagearchitecture.roof_timber."+key));}
}
