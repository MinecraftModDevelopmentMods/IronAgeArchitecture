package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.common.property.*;

/** Outside-cell portion of an open sash. No ItemBlock, independent drops or tile entity. */
public final class WindowLeafBlock extends BlockHorizontal {
    public WindowLeafBlock(){super(Material.WOOD);setHardness(2);setLightOpacity(0);useNeighborBrightness=true;setDefaultState(blockState.getBaseState().withProperty(FACING,EnumFacing.NORTH));}
    @Override protected BlockStateContainer createBlockState(){return new ExtendedBlockState(this,new IProperty<?>[]{FACING},new IUnlistedProperty<?>[]{WindowView.PROPERTY});}
    @Override public IBlockState getStateFromMeta(int meta){return getDefaultState().withProperty(FACING,EnumFacing.getHorizontal(meta&3));}
    @Override public int getMetaFromState(IBlockState state){return state.getValue(FACING).getHorizontalIndex();}
    public BlockPos owner(IBlockState state,BlockPos pos){return pos.offset(state.getValue(FACING));}
    public WindowView view(IBlockState state,IBlockAccess world,BlockPos pos) {
        BlockPos owner=owner(state,pos);if(world instanceof World&&!((World)world).isBlockLoaded(owner)) return null;IBlockState parent=world.getBlockState(owner);
        if(!(parent.getBlock() instanceof WindowBlock)||!parent.getValue(WindowBlock.OPEN)||!owner.offset(parent.getValue(WindowBlock.FACING)).equals(pos)) return null;
        return new WindowView(parent,((WindowBlock)parent.getBlock()).joins(parent,world,owner),true);
    }
    @Override public IBlockState getExtendedState(IBlockState state,IBlockAccess world,BlockPos pos){return ((IExtendedBlockState)state).withProperty(WindowView.PROPERTY,view(state,world,pos));}
    private List<AxisAlignedBB> boxes(IBlockState state,IBlockAccess world,BlockPos pos){WindowView view=view(state,world,pos);if(view==null) return Collections.emptyList();WindowBlock block=(WindowBlock)view.state.getBlock();return WindowGeometry.boxes(block.thin,block.right,true,view.state.getValue(WindowBlock.FACING),view.joins,true,block.back);}
    @Override public boolean isOpaqueCube(IBlockState state){return false;}
    @Override public boolean isFullCube(IBlockState state){return false;}
    @Override public boolean isSideSolid(IBlockState state,IBlockAccess world,BlockPos pos,EnumFacing side){return false;}
    @Override public boolean canRenderInLayer(IBlockState state,BlockRenderLayer layer){return layer==BlockRenderLayer.SOLID||layer==BlockRenderLayer.TRANSLUCENT;}
    @Override public AxisAlignedBB getBoundingBox(IBlockState state,IBlockAccess world,BlockPos pos){return WindowBlock.union(boxes(state,world,pos));}
    @Override public void addCollisionBoxToList(IBlockState state,World world,BlockPos pos,AxisAlignedBB mask,List<AxisAlignedBB> boxes,@Nullable Entity entity){for(AxisAlignedBB box:boxes(state,world,pos)) addCollisionBoxToList(pos,mask,boxes,box);}
    @Override public RayTraceResult collisionRayTrace(IBlockState state,World world,BlockPos pos,Vec3d start,Vec3d end){WindowView view=view(state,world,pos);return view==null?null:((WindowBlock)view.state.getBlock()).trace(pos,start,end,boxes(state,world,pos));}
    @Override public boolean onBlockActivated(World world,BlockPos pos,IBlockState state,EntityPlayer player,EnumHand hand,@Nullable ItemStack held,EnumFacing side,float x,float y,float z) {
        WindowView view=view(state,world,pos);return view!=null&&((WindowBlock)view.state.getBlock()).onBlockActivated(world,owner(state,pos),view.state,player,hand,held,side,x,y,z);
    }
    @Override public boolean removedByPlayer(IBlockState state,World world,BlockPos pos,EntityPlayer player,boolean harvest) {
        WindowView view=view(state,world,pos);BlockPos owner=owner(state,pos);
        if(view!=null&&(!world.isBlockModifiable(player,owner)||!player.canPlayerEdit(owner,EnumFacing.UP,player.getHeldItemMainhand()))) return false;
        if(!world.isRemote) {if(view!=null) world.destroyBlock(owner,!player.capabilities.isCreativeMode);else world.setBlockToAir(pos);}return true;
    }
    @Override public void harvestBlock(World world,EntityPlayer player,BlockPos pos,IBlockState state,@Nullable net.minecraft.tileentity.TileEntity tile,@Nullable ItemStack stack){ }
    @Override public List<ItemStack> getDrops(IBlockAccess world,BlockPos pos,IBlockState state,int fortune){return Collections.emptyList();}
    @Override public ItemStack getPickBlock(IBlockState state,RayTraceResult hit,World world,BlockPos pos,EntityPlayer player){WindowView view=view(state,world,pos);return view==null?null:new ItemStack(((WindowBlock)view.state.getBlock()).canonical());}
    @Override public void breakBlock(World world,BlockPos pos,IBlockState state){if(!world.isRemote&&!WindowController.busy()) {BlockPos owner=owner(state,pos);if(world.isBlockLoaded(owner)) WindowController.set(world,WindowController.column(world,owner),false,null,null,false);}super.breakBlock(world,pos,state);}
    @Override public void neighborChanged(IBlockState state,World world,BlockPos pos,Block changed){WindowController.repair(world,pos);}
    @Override public void onBlockExploded(World world,BlockPos pos,Explosion explosion){WindowView view=view(world.getBlockState(pos),world,pos);if(view!=null) world.destroyBlock(owner(world.getBlockState(pos),pos),true);else world.setBlockToAir(pos);}
}
