package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.*;
import net.minecraft.block.state.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.common.property.*;
import zone.moddev.mc.ironagearchitecture.*;

/** Two identities pack all 24 wood/cover combinations. No per-member tile entities. */
public final class RoofFrameBlock extends Block {
    public static final PropertyInteger WOOD=PropertyInteger.create("wood",0,5);
    public static final PropertyBool SECOND=PropertyBool.create("second");
    public static final IUnlistedProperty<RoofCell> CELL=new IUnlistedProperty<RoofCell>(){public String getName(){return "cell";}public boolean isValid(RoofCell c){return c!=null;}public Class<RoofCell> getType(){return RoofCell.class;}public String valueToString(RoofCell c){return c==null?"none":c.plan.toString();}};
    public final boolean covered;
    public RoofFrameBlock(boolean covered){super(Material.WOOD);this.covered=covered;setHardness(2);setResistance(5);setSoundType(SoundType.WOOD);setHarvestLevel("axe",0);setLightOpacity(0);useNeighborBrightness=true;setDefaultState(blockState.getBaseState().withProperty(WOOD,0).withProperty(SECOND,false));}
    public static int cover(IBlockState s){return s.getBlock() instanceof RoofFrameBlock?(((RoofFrameBlock)s.getBlock()).covered?2:0)+(s.getValue(SECOND)?1:0):0;}
    public static IBlockState state(int wood,int cover){RoofFrameBlock b=cover<2?ArchitectureBlocks.ROOF_FRAME:ArchitectureBlocks.ROOF_FRAME_COVERED;return b.getDefaultState().withProperty(WOOD,wood).withProperty(SECOND,(cover&1)!=0);}
    public static RoofCell cell(IBlockAccess access,BlockPos at){return IronAgeArchitecture.proxy.roofCell(access,at);}
    public static RoofMeshes.Mesh mesh(IBlockState s,IBlockAccess w,BlockPos p){return RoofMeshes.get(cell(w,p),cover(s));}
    @Override protected BlockStateContainer createBlockState(){return new ExtendedBlockState(this,new IProperty<?>[]{WOOD,SECOND},new IUnlistedProperty<?>[]{CELL});}
    @Override public IBlockState getStateFromMeta(int m){return getDefaultState().withProperty(WOOD,Math.min(m&7,5)).withProperty(SECOND,(m&8)!=0);}
    @Override public int getMetaFromState(IBlockState s){return s.getValue(WOOD)|(s.getValue(SECOND)?8:0);}
    @Override public IBlockState getExtendedState(IBlockState s,IBlockAccess w,BlockPos p){RoofCell c=cell(w,p);return c==null?s:((IExtendedBlockState)s).withProperty(CELL,c);}
    @Override public boolean isOpaqueCube(IBlockState s){return false;}@Override public boolean isFullCube(IBlockState s){return false;}@Override public boolean isSideSolid(IBlockState s,IBlockAccess w,BlockPos p,EnumFacing f){return false;}
    @Override public int getLightOpacity(IBlockState s,IBlockAccess w,BlockPos p){RoofCell c=cell(w,p);if(c!=null&&c.designed){for(RoofPart part:c.parts)if(part.kind==RoofPart.COVER&&part.material>0)for(int i=0;i<part.data.length;i+=8){double mid=(part.data[i+7]&4)!=0?(part.data[i+3]+part.data[i+4]+part.data[i+5])/3.0:(part.data[i+4]+part.data[i+5])*.5;if(mid>=0&&mid<32)return 255;}return 0;}return cover(s)>0?255:0;}
    @Override public boolean canRenderInLayer(IBlockState s,BlockRenderLayer l){return l==BlockRenderLayer.SOLID||l==BlockRenderLayer.TRANSLUCENT;}
    @Override public net.minecraft.block.material.EnumPushReaction getMobilityFlag(IBlockState s){return net.minecraft.block.material.EnumPushReaction.BLOCK;}
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s,World w,BlockPos p){return NULL_AABB;}
    @Override public AxisAlignedBB getBoundingBox(IBlockState s,IBlockAccess w,BlockPos p){return mesh(s,w,p).bounds;}
    @Override public void addCollisionBoxToList(IBlockState s,World w,BlockPos p,AxisAlignedBB box,List<AxisAlignedBB> out,@Nullable Entity entity){for(AxisAlignedBB b:mesh(s,w,p).boxes)addCollisionBoxToList(p,box,out,b);}
    @Override public RayTraceResult collisionRayTrace(IBlockState s,World w,BlockPos p,Vec3d a,Vec3d b){return mesh(s,w,p).trace(p,a,b);}
    @Override public boolean onBlockActivated(World w,BlockPos p,IBlockState s,EntityPlayer player,EnumHand hand,@Nullable ItemStack held,EnumFacing side,float x,float y,float z){RoofCell c=cell(w,p);if(c!=null&&c.designed)return RoofDesignInteraction.use(w,p,player,held,hand);if(held!=null&&held.getItem() instanceof RoofClipboardItem)return false;if(w.isRemote)return true;return RoofConstruction.cover(w,p,player,held,hand);}
    @Override public boolean removedByPlayer(IBlockState s,World w,BlockPos p,EntityPlayer player,boolean harvest){RoofCell designed=cell(w,p);if(designed!=null&&designed.designed)return w.isRemote||RoofDesignInteraction.mine(w,p,player);RoofCell c=cell(w,p);if(c!=null&&RoofConstruction.busy(w,c.plan)){if(!w.isRemote)player.sendStatusMessage(new net.minecraft.util.text.TextComponentString("Cancel construction at the workbench before dismantling this frame."));return false;}if(c!=null&&!c.timber()&&!c.owner.equals(p)){if(w.isRemote)return false;BlockPos owner=c.owner;if(!w.isBlockModifiable(player,owner)||!player.canPlayerEdit(owner,EnumFacing.UP,player.getHeldItemMainhand()))return false;IBlockState original=w.getBlockState(owner);RoofCell link=cell(w,owner);if(original.getBlock() instanceof RoofFrameBlock&&link!=null&&link.plan.equals(c.plan))w.destroyBlock(owner,!player.capabilities.isCreativeMode);return w.isAirBlock(p);}return harvest&&!player.capabilities.isCreativeMode||super.removedByPlayer(s,w,p,player,harvest);}
    @Override public void harvestBlock(World w,EntityPlayer player,BlockPos p,IBlockState s,@Nullable net.minecraft.tileentity.TileEntity tile,@Nullable ItemStack tool){RoofCell c=cell(w,p);if(c!=null&&c.designed)return;try{super.harvestBlock(w,player,p,s,tile,tool);}finally{if(w.getBlockState(p).getBlock()==s.getBlock())w.setBlockToAir(p);}}
    @Override public void onBlockExploded(World w,BlockPos p,Explosion explosion){RoofCell c=cell(w,p);if(c!=null&&c.designed){RoofDesignInteraction.replaced(w,p,c,true);return;}if(c!=null&&!c.timber()&&!c.owner.equals(p)){IBlockState owner=w.getBlockState(c.owner);RoofCell link=cell(w,c.owner);if(owner.getBlock() instanceof RoofFrameBlock&&link!=null&&link.plan.equals(c.plan))w.destroyBlock(c.owner,true);}if(w.getBlockState(p).getBlock()==this)w.setBlockToAir(p);}
    @Override public List<ItemStack> getDrops(IBlockAccess w,BlockPos p,IBlockState s,int fortune){List<ItemStack> drops=new ArrayList<>();RoofCell c=cell(w,p);if(c==null||c.designed)return drops;if(c.timber())drops.add(new ItemStack(ArchitectureBlocks.ROOF_TIMBER,1,s.getValue(WOOD)));if(c.timber()&&cover(s)>0){ItemStack material=RoofConstruction.coverItem(cover(s));if(material!=null)drops.add(material);}return drops;}
    @Override public ItemStack getPickBlock(IBlockState s,RayTraceResult hit,World w,BlockPos p,EntityPlayer player){RoofCell c=cell(w,p);if(c!=null&&c.designed){RoofPart part=RoofDesignInteraction.target(w,p,player);return part==null?new ItemStack(ArchitectureBlocks.ROOF_TIMBER):RoofMaterials.item(part);}return new ItemStack(ArchitectureBlocks.ROOF_TIMBER,1,s.getValue(WOOD));}
    @Override public void breakBlock(World w,BlockPos p,IBlockState s){if(!w.isRemote&&!RoofConstruction.changing){RoofCell c=cell(w,p);if(c!=null&&c.designed)RoofDesignInteraction.replaced(w,p,c,false);else RoofConstruction.broken(w,p,s);}super.breakBlock(w,p,s);}
    @Override public void neighborChanged(IBlockState s,World w,BlockPos p,Block b){if(!w.isRemote&&!RoofConstruction.changing)RoofWindowController.neighbor(w,p);}
    @Override @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT) public void randomDisplayTick(IBlockState state,World world,BlockPos pos,java.util.Random random){if(random.nextInt(12)==0&&RoofGutters.outlet(world,pos)!=null)zone.moddev.mc.ironagearchitecture.block.DrainRun.showDrip(state,world,pos);}
    @Override public int getFlammability(IBlockAccess w,BlockPos p,EnumFacing f){return 20;}@Override public int getFireSpreadSpeed(IBlockAccess w,BlockPos p,EnumFacing f){return 5;}
}
