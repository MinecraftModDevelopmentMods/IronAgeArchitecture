package zone.moddev.mc.ironagearchitecture.block;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.*;

public final class SmartWallItem extends EraItemBlock {
    private final SmartWallBlock wall;
    public SmartWallItem(SmartWallBlock wall){super(wall);this.wall=wall;setHasSubtypes(true);setMaxDamage(0);}
    @Override public int getMetadata(int damage){return damage&1;}
    @Override public String getUnlocalizedName(ItemStack stack){return super.getUnlocalizedName()+"."+(stack.getMetadata()&1);}
    @Override public void getSubItems(Item item,CreativeTabs tab,List<ItemStack> list){
        if(ArchitectureConfig.buildingMode.automatic()) for(int finish=0;finish<2;finish++) list.add(new ItemStack(item,1,finish));
    }
    @Override public EnumActionResult onItemUse(ItemStack stack,EntityPlayer player,World world,BlockPos pos,
            EnumHand hand,EnumFacing side,float x,float y,float z) {
        IBlockState old=world.getBlockState(pos);
        if(player.isSneaking()&&wall.profile==1&&old.getBlock() instanceof SmartWallBlock&&side.getAxis().isHorizontal()) {
            SmartWallBlock previous=(SmartWallBlock)old.getBlock();
            if(previous.profile(old)==1&&previous.wood.equals(wall.wood)&&previous.finish(old.getValue(FrameBlock.PANEL))==getMetadata(stack.getMetadata())) {
                int merged=previous.mask(old)|SmartWallGeometry.mask(wall,side.getOpposite());
                int count=Integer.bitCount(merged);
                if(count==3||count==4) {
                    int shape=count==3?2:0;
                    SmartWallBlock target=ArchitectureBlocks.findSmart(wall.wood,shape);
                    for(EnumFacing facing:EnumFacing.HORIZONTALS) if(SmartWallGeometry.mask(shape,facing)==merged) {
                        IBlockState next=target.state(shape,facing,getMetadata(stack.getMetadata()));
                        if(!player.canPlayerEdit(pos,side,stack)||!world.isBlockModifiable(player,pos)) return EnumActionResult.FAIL;
                        for(AxisAlignedBB box:SmartWallGeometry.boxes(merged))
                            if(!world.checkNoEntityCollision(box.offset(pos))) return EnumActionResult.FAIL;
                        if(!world.isRemote) {
                            if(!world.setBlockState(pos,next,3)) return EnumActionResult.FAIL;
                            if(!player.capabilities.isCreativeMode) stack.stackSize--;
                        }
                        return EnumActionResult.SUCCESS;
                    }
                }
            }
        }
        return super.onItemUse(stack,player,world,pos,hand,side,x,y,z);
    }
    @Override public boolean placeBlockAt(ItemStack stack,EntityPlayer player,World world,BlockPos pos,
            EnumFacing side,float x,float y,float z,IBlockState state) {
        for(AxisAlignedBB box:SmartWallGeometry.boxes(wall.mask(state))) if(!world.checkNoEntityCollision(box.offset(pos))) return false;
        return super.placeBlockAt(stack,player,world,pos,side,x,y,z,state);
    }
    @Override public void addInformation(ItemStack stack,EntityPlayer player,List<String> tooltip,boolean advanced){
        hint(tooltip,"smart.place");
        if(wall.profile==1) hint(tooltip,"smart.corner");
        hint(tooltip,ArchitectureConfig.buildingMode==BuildingMode.AUTOMATIC?"smart.craft.auto":"smart.craft");
        if((stack.getMetadata()&1)==0) hint(tooltip,"wash");
    }
}
