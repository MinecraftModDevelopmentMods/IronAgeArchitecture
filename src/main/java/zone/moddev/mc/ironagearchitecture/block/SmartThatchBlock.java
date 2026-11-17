package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;

/** One material ID, vanilla metadata and a fixed neighbour-selected roof kit. */
public final class SmartThatchBlock extends Block {
    public static final net.minecraft.block.properties.PropertyDirection FACING=BlockHorizontal.FACING;
    public static final net.minecraft.block.properties.PropertyInteger PROFILE=net.minecraft.block.properties.PropertyInteger.create("profile",0,ThatchShape.COUNT-1);
    public static final net.minecraft.block.properties.PropertyBool LINKED=net.minecraft.block.properties.PropertyBool.create("linked");
    public final boolean helper;
    public SmartThatchBlock(boolean helper){super(Material.GRASS);this.helper=helper;setHardness(.8F);setResistance(1);setSoundType(SoundType.PLANT);setHarvestLevel("axe",0);setLightOpacity(helper?0:255);useNeighborBrightness=true;setDefaultState(blockState.getBaseState().withProperty(FACING,EnumFacing.NORTH).withProperty(PROFILE,ThatchShape.FLAT_PROFILE).withProperty(LINKED,false));}
    public static boolean owner(IBlockState state){return state.getBlock() instanceof SmartThatchBlock&&!((SmartThatchBlock)state.getBlock()).helper;}
    public static boolean roof(IBlockState state){return state.getBlock() instanceof RoofBlock||state.getBlock() instanceof SmartThatchBlock;}
    public static boolean active(IBlockState state){return state.getBlock() instanceof SmartThatchBlock&&((SmartThatchBlock)state.getBlock()).helper&&state.getValue(LINKED);}
    public static BlockPos parent(IBlockAccess world,BlockPos pos){BlockPos above=pos.up();return (!(world instanceof World)||((World)world).isBlockLoaded(above))&&owner(world.getBlockState(above))?above:null;}
    @Override protected BlockStateContainer createBlockState(){return new BlockStateContainer(this,FACING,PROFILE,LINKED);}
    @Override public IBlockState getStateFromMeta(int meta){return getDefaultState().withProperty(FACING,helper?EnumFacing.NORTH:EnumFacing.getHorizontal(meta&3)).withProperty(LINKED,helper&&(meta&8)!=0);}
    @Override public int getMetaFromState(IBlockState state){return helper?(active(state)?8:0):state.getValue(FACING).getHorizontalIndex();}
    @Override public int damageDropped(IBlockState state){return 0;}
    @Override public IBlockState getStateForPlacement(World world,BlockPos pos,EnumFacing side,float x,float y,float z,int meta,EntityLivingBase player){return getDefaultState().withProperty(FACING,player.getHorizontalFacing());}
    @Override public IBlockState getActualState(IBlockState state,IBlockAccess world,BlockPos pos){if(helper){BlockPos above=active(state)?parent(world,pos):null;if(above==null)return getDefaultState();ThatchShape shape=ThatchShape.at(world,above,world.getBlockState(above));return state.withProperty(FACING,shape.facing).withProperty(PROFILE,shape.profile);}ThatchShape shape=ThatchShape.at(world,pos,state);return state.withProperty(FACING,shape.facing).withProperty(PROFILE,shape.profile);}
    public ThatchGeometry.Mesh mesh(IBlockAccess world,BlockPos pos){if(helper){BlockPos above=active(world.getBlockState(pos))?parent(world,pos):null;return above==null?ThatchGeometry.EMPTY:ThatchGeometry.underside(ThatchShape.at(world,above,world.getBlockState(above)));}return ThatchGeometry.mesh(ThatchShape.at(world,pos,world.getBlockState(pos)));}
    @Override public boolean isOpaqueCube(IBlockState state){return false;}
    @Override public boolean isFullCube(IBlockState state){return false;}
    @Override public boolean isAir(IBlockState state,IBlockAccess world,BlockPos pos){return helper&&!active(state);}
    @Override public boolean isReplaceable(IBlockAccess world,BlockPos pos){return helper;}
    @Override public boolean canCollideCheck(IBlockState state,boolean liquids){return !helper||active(state);}
    @Override public boolean isSideSolid(IBlockState state,IBlockAccess world,BlockPos pos,EnumFacing side){return !helper&&side==EnumFacing.DOWN&&ThatchShape.at(world,pos,state).profile==ThatchShape.SOLID_PROFILE;}
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,World world,BlockPos pos){return NULL_AABB;}
    @Override public AxisAlignedBB getBoundingBox(IBlockState state,IBlockAccess world,BlockPos pos){return mesh(world,pos).bounds;}
    @Override public void addCollisionBoxToList(IBlockState state,World world,BlockPos pos,AxisAlignedBB query,List<AxisAlignedBB> boxes,@Nullable Entity entity){for(AxisAlignedBB box:mesh(world,pos).boxes)addCollisionBoxToList(pos,query,boxes,box);}
    @Override public RayTraceResult collisionRayTrace(IBlockState state,World world,BlockPos pos,Vec3d start,Vec3d end){return mesh(world,pos).trace(pos,start,end);}
    public static Vec3d eyes(EntityPlayer player){return new Vec3d(player.posX,player.posY+player.getEyeHeight(),player.posZ);}
    public static Vec3d look(EntityPlayer player){double yaw=Math.toRadians(player.rotationYaw),pitch=Math.toRadians(player.rotationPitch);return new Vec3d(-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch));}
    @Override public Item getItemDropped(IBlockState state,Random random,int fortune){return helper?null:Item.getItemFromBlock(this);}
    @Override public int quantityDropped(Random random){return helper?0:1;}
    @Override public ItemStack getPickBlock(IBlockState state,RayTraceResult hit,World world,BlockPos pos,EntityPlayer player){if(!helper)return new ItemStack(this);BlockPos above=parent(world,pos);return active(state)&&above!=null?new ItemStack(world.getBlockState(above).getBlock()):null;}
    @Override public boolean canPlaceBlockAt(World world,BlockPos pos){return world.getBlockState(pos).getBlock().isReplaceable(world,pos);}
    @Override public void onBlockAdded(World world,BlockPos pos,IBlockState state){SmartThatchEvents.edited(world,pos);}
    @Override public void breakBlock(World world,BlockPos pos,IBlockState state){if(!helper)SmartThatchEvents.removeBelow(world,pos);else if(active(state)&&!SmartThatchEvents.applying(world)){Block replacement=world.getBlockState(pos).getBlock();BlockPos above=parent(world,pos);if(above!=null&&(replacement==net.minecraft.init.Blocks.AIR||replacement==net.minecraft.init.Blocks.FIRE)){if(world.isRemote)world.setBlockToAir(above);else world.destroyBlock(above,false);}}SmartThatchEvents.edited(world,pos);super.breakBlock(world,pos,state);}
    @Override public void updateTick(World world,BlockPos pos,IBlockState state,Random random){if(!helper)SmartThatchEvents.edited(world,pos);}
    @Override public void neighborChanged(IBlockState state,World world,BlockPos pos,Block changed){SmartThatchEvents.edited(world,pos);}
    @Override public boolean removedByPlayer(IBlockState state,World world,BlockPos pos,EntityPlayer player,boolean harvest){if(!helper)return super.removedByPlayer(state,world,pos,player,harvest);BlockPos above=active(state)?parent(world,pos):null;if(above!=null&&(!world.isBlockModifiable(player,above)||!player.canPlayerEdit(above,EnumFacing.UP,player.getHeldItemMainhand())))return false;if(above!=null){if(world.isRemote)world.setBlockToAir(above);else world.destroyBlock(above,!player.capabilities.isCreativeMode);}else world.setBlockToAir(pos);return true;}
    @Override public void harvestBlock(World world,EntityPlayer player,BlockPos pos,IBlockState state,@Nullable net.minecraft.tileentity.TileEntity tile,@Nullable ItemStack stack){if(!helper)super.harvestBlock(world,player,pos,state,tile,stack);}
    @Override public java.util.List<ItemStack> getDrops(IBlockAccess world,BlockPos pos,IBlockState state,int fortune){return helper?java.util.Collections.emptyList():super.getDrops(world,pos,state,fortune);}
    @Override public void onBlockExploded(World world,BlockPos pos,Explosion explosion){if(helper){BlockPos above=active(world.getBlockState(pos))?parent(world,pos):null;if(above!=null)world.destroyBlock(above,true);else world.setBlockToAir(pos);}else super.onBlockExploded(world,pos,explosion);}
    @Override public IBlockState withRotation(IBlockState state,Rotation rotation){return helper?state:state.withProperty(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public IBlockState withMirror(IBlockState state,Mirror mirror){return withRotation(state,mirror.toRotation(state.getValue(FACING)));}
    @Override public int getPackedLightmapCoords(IBlockState state,IBlockAccess world,BlockPos pos){return SmartLighting.packed(world,pos);}
}
