package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.SoundType;
import net.minecraft.world.World;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;

/** Inventory metadata describes the panel, never its world-facing bits. */
public final class FrameItem extends EraItemBlock {
    public FrameItem(FrameBlock block) { super(block); setHasSubtypes(true); setMaxDamage(0); }
    @Override public int getMetadata(int damage) { return Math.floorMod(damage, ((FrameBlock)block).variants()); }
    @Override public String getUnlocalizedName(ItemStack stack) { return super.getUnlocalizedName() + "." + getMetadata(stack.getMetadata()); }
    @Override public EnumActionResult onItemUse(ItemStack stack,EntityPlayer player,World world,BlockPos pos,EnumHand hand,EnumFacing side,float x,float y,float z) {
        if(!(block instanceof PartialWallBlock)&&!(block instanceof JettyFloorBlock)) return super.onItemUse(stack,player,world,pos,hand,side,x,y,z);
        if(!world.getBlockState(pos).getBlock().isReplaceable(world,pos)) pos=pos.offset(side);
        if(stack.stackSize==0||!player.canPlayerEdit(pos,side,stack)||!world.canBlockBePlaced(block,pos,true,side,null,stack)) return EnumActionResult.FAIL;
        IBlockState chosen=block.getStateForPlacement(world,pos,side,x,y,z,getMetadata(stack.getMetadata()),player,stack);
        FrameBlock placed=(FrameBlock)chosen.getBlock();
        for(WallGeometry.Part part:placed.parts(chosen,placed.connections(chosen,world,pos)))
            if(!world.checkNoEntityCollision(part.bounds(chosen.getValue(FrameBlock.FACING)).offset(pos))) return EnumActionResult.FAIL;
        if(!placeBlockAt(stack,player,world,pos,side,x,y,z,chosen)) return EnumActionResult.FAIL;
        SoundType sound=placed.getSoundType(chosen,world,pos,player);
        world.playSound(player,pos,sound.getPlaceSound(),SoundCategory.BLOCKS,(sound.getVolume()+1)/2,sound.getPitch()*.8F);
        if(!player.capabilities.isCreativeMode) stack.stackSize--;
        return EnumActionResult.SUCCESS;
    }
    @Override public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> list) {
        if(!zone.moddev.mc.ironagearchitecture.ArchitectureBlocks.available(this)) return;
        for (int meta = 0; meta < ((FrameBlock)block).variants(); meta++) list.add(new ItemStack(item, 1, meta));
    }
    @Override public void addInformation(ItemStack stack,EntityPlayer player,List<String> tooltip,boolean advanced) {
        if(block instanceof PartialWallBlock) {
            tooltip.add("Click a half to choose placement; sneak to avoid inheriting a wall plane.");
            if(((PartialWallBlock)block).profile==WallProfile.SHORT_LEFT) tooltip.add("Click the left or right half for the matching cut.");
        }
        if(block instanceof JettyFloorBlock) {
            tooltip.add("Upper-half timber floor; face the direction of the joists.");
            if(((JettyFloorBlock)block).shape==FloorShape.L) tooltip.add("Click the corner that should remain empty.");
            switch(((JettyFloorBlock)block).shape) {
                case FULL:hint(tooltip,"floor.full");break;
                case HALF:hint(tooltip,"cut");break;
                case QUARTER:hint(tooltip,"floor.quarter");break;
                case L:hint(tooltip,"floor.l");break;
            }
            if(((JettyFloorBlock)block).shape==FloorShape.FULL) hint(tooltip,"floor.brackets");
        } else {
            FrameBlock wall=(FrameBlock)block;
            boolean corner=wall instanceof PartialWallBlock&&((PartialWallBlock)wall).profile.corner;
            if(wall.family==FrameFamily.INFILL) hint(tooltip,wall instanceof PartialWallBlock?
                (((PartialWallBlock)wall).profile==WallProfile.THIN?"cut":"wall.short"):"wall.infill");
            else {
                if(wall.variants()==4) hint(tooltip,"wall.swap");
                if(corner) hint(tooltip,"wall.corner");
                else if(wall.family==FrameFamily.POST) {hint(tooltip,"wall.post");hint(tooltip,"wall.patterns");}
                else hint(tooltip,"wall.reset");
            }
            if(wall.finish(getMetadata(stack.getMetadata()))==0) hint(tooltip,"wash");
        }
    }
}
