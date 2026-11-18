package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.*;

/** A reusable reference to a server-side drawing. The item never contains trusted geometry. */
public final class RoofClipboardItem extends Item {
    public RoofClipboardItem(){setMaxStackSize(1);setCreativeTab(EraTabs.tab(Era.IRON_AGE));}
    @Override public CreativeTabs[] getCreativeTabs(){return EraTabs.forItem(this);}
    @Override public void getSubItems(Item item,CreativeTabs tab,List<ItemStack> stacks){if(ArchitectureConfig.buildingMode.automatic())stacks.add(new ItemStack(item));}
    public static UUID bound(ItemStack stack){return stack!=null&&stack.getItem() instanceof RoofClipboardItem&&stack.hasTagCompound()&&stack.getTagCompound().hasUniqueId("drawing")?stack.getTagCompound().getUniqueId("drawing"):null;}
    public static void bind(ItemStack stack,RoofPlan p,int dimension){NBTTagCompound n=new NBTTagCompound();n.setUniqueId("drawing",p.id);n.setInteger("dimension",dimension);n.setInteger("tool",0);RoofFeature f=new RoofFeature();f.type=RoofFeature.WINDOW;f.width=1;f.wood=p.wood;n.setTag("options",f.write());stack.setTagCompound(n);}
    @Override public ActionResult<ItemStack> onItemRightClick(ItemStack stack,World w,EntityPlayer player,EnumHand hand){if(w.isRemote&&(player.isSneaking()||!IronAgeArchitecture.proxy.clipboardPoint(stack,player)))IronAgeArchitecture.proxy.clipboardScreen(stack);return new ActionResult<>(EnumActionResult.SUCCESS,stack);}
    @Override public EnumActionResult onItemUse(ItemStack stack,EntityPlayer player,World w,BlockPos pos,EnumHand hand,EnumFacing side,float x,float y,float z){if(w.getTileEntity(pos) instanceof ArchitectWorkbench.Desk){if(!w.isRemote){ArchitectWorkbench.Desk d=(ArchitectWorkbench.Desk)w.getTileEntity(pos);if(d.drawing==null)RoofConstruction.detect(d);RoofClipboardItem.bind(stack,d.drawing,w.provider.getDimension());player.sendStatusMessage(new net.minecraft.util.text.TextComponentString("Clipboard linked. Use it in the air to choose a tool."));}return EnumActionResult.SUCCESS;}if(!w.isRemote)RoofDrafts.point(w,player,stack,pos,side);return EnumActionResult.SUCCESS;}
    @Override public void addInformation(ItemStack stack,EntityPlayer player,List<String> lines,boolean advanced){lines.add(bound(stack)==null?"Use on an architect's workbench to link a drawing.":"Use in the air for tools; use on the roof to edit its drawing.");lines.add("Changes are built and paid for at the workbench.");}
}
