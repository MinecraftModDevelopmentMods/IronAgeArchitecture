package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.*;

public final class SmartThatchItem extends EraItemBlock {
    public SmartThatchItem(SmartThatchBlock block){super(block);}
    @Override public void getSubItems(Item item,CreativeTabs tab,List<ItemStack> items){if(ArchitectureConfig.buildingMode.automatic())items.add(new ItemStack(item));}
    @Override public EnumActionResult onItemUse(ItemStack stack,EntityPlayer player,World world,BlockPos pos,EnumHand hand,EnumFacing side,float x,float y,float z){if(SmartThatchBlock.active(world.getBlockState(pos))&&SmartThatchBlock.parent(world,pos)!=null)return super.onItemUse(stack,player,world,pos.up(),hand,side,x,y-1,z);return super.onItemUse(stack,player,world,pos,hand,side,x,y,z);}
    @Override public boolean placeBlockAt(ItemStack stack,EntityPlayer player,World world,BlockPos pos,EnumFacing side,float x,float y,float z,IBlockState state){
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){
            BlockPos at=pos.add(dx,dy,dz);if(!world.isBlockLoaded(at))continue;boolean added=at.equals(pos);IBlockState old=world.getBlockState(at);if(!added&&!SmartThatchBlock.owner(old))continue;
            ThatchShape shape=ThatchShape.at(world,at,added?state:old,pos,state),before=added?null:ThatchShape.at(world,at,old);if(shape==before)continue;
            List<AxisAlignedBB> after=volumes(shape,available(world,at.down(),pos,state)),previous=before==null?java.util.Collections.emptyList():volumes(before,available(world,at.down(),null,null));
            for(Entity entity:world.getEntitiesWithinAABB(Entity.class,new AxisAlignedBB(at.down(),at.add(1,1,1)))){if(entity.isDead||entity.noClip||!entity.canBeCollidedWith())continue;AxisAlignedBB box=entity.getEntityBoundingBox();boolean was=false,now=false;for(AxisAlignedBB b:previous)if(b.offset(at).intersectsWith(box)){was=true;break;}for(AxisAlignedBB b:after)if(b.offset(at).intersectsWith(box)){now=true;break;}if(now&&!was)return false;}
        }
        return super.placeBlockAt(stack,player,world,pos,side,x,y,z,state);
    }
    private static boolean available(World world,BlockPos at,BlockPos placed,IBlockState next){IBlockState s=at.equals(placed)?next:world.getBlockState(at);return s.getBlock()==net.minecraft.init.Blocks.AIR||s.getBlock() instanceof SmartThatchBlock&&((SmartThatchBlock)s.getBlock()).helper;}
    private static List<AxisAlignedBB> volumes(ThatchShape shape,boolean under){if(!under||ThatchGeometry.underside(shape)==ThatchGeometry.EMPTY)return ThatchGeometry.mesh(shape).boxes;java.util.List<AxisAlignedBB> out=new java.util.ArrayList<>(ThatchGeometry.mesh(shape).boxes);if(under)for(AxisAlignedBB b:ThatchGeometry.underside(shape).boxes)out.add(b.offset(0,-1,0));return out;}
    @Override public void addInformation(ItemStack stack,EntityPlayer player,List<String> tooltip,boolean advanced){hint(tooltip,"smart_thatch.place");hint(tooltip,"smart_thatch.space");hint(tooltip,ArchitectureConfig.buildingMode==BuildingMode.AUTOMATIC?"smart_thatch.craft.auto":ArchitectureConfig.buildingMode==BuildingMode.MANUAL?"smart_thatch.craft.manual":"smart_thatch.craft");}
}
