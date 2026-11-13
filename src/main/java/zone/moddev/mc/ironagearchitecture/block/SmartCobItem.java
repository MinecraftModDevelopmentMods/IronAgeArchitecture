package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.ArchitectureConfig;

public final class SmartCobItem extends EraItemBlock {
    public SmartCobItem(SmartCobBlock block){super(block);setHasSubtypes(true);setMaxDamage(0);}
    @Override public int getMetadata(int damage){return damage&1;}
    @Override public String getUnlocalizedName(ItemStack stack){return super.getUnlocalizedName()+"."+(stack.getMetadata()&1);}
    @Override public void getSubItems(Item item,CreativeTabs tab,List<ItemStack> items){if(ArchitectureConfig.buildingMode.automatic())for(int finish=0;finish<2;finish++)items.add(new ItemStack(item,1,finish));}
    @Override public boolean placeBlockAt(ItemStack stack,EntityPlayer player,World world,BlockPos pos,EnumFacing side,float x,float y,float z,IBlockState state){
        if(!world.checkNoEntityCollision(new AxisAlignedBB(pos)))return false;return super.placeBlockAt(stack,player,world,pos,side,x,y,z,state);
    }
    @Override public void addInformation(ItemStack stack,EntityPlayer player,List<String> tooltip,boolean advanced){hint(tooltip,"smart_cob.place");hint(tooltip,"smart_cob.craft");if((stack.getMetadata()&1)==0)hint(tooltip,"wash");}
}
