package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.block.*;
import net.minecraft.block.material.*;
import net.minecraft.block.properties.*;
import net.minecraft.block.state.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.common.property.*;

/** All roof-window appearances share these owner/helper identities. */
public class RoofWindowBlock extends Block {
    public static final PropertyDirection FACING=BlockHorizontal.FACING;
    public static final PropertyBool OPEN=PropertyBool.create("open"),POWERED=PropertyBool.create("powered");
    public final boolean helper;
    public RoofWindowBlock(boolean helper){super(Material.WOOD);this.helper=helper;setHardness(2);setResistance(5);setSoundType(SoundType.WOOD);setLightOpacity(0);useNeighborBrightness=true;if(!helper)setDefaultState(blockState.getBaseState().withProperty(FACING,EnumFacing.NORTH).withProperty(OPEN,false).withProperty(POWERED,false));}
    @Override protected BlockStateContainer createBlockState(){return new ExtendedBlockState(this,new IProperty<?>[]{FACING,OPEN,POWERED},new IUnlistedProperty<?>[]{RoofFrameBlock.CELL});}
    @Override public IBlockState getStateFromMeta(int m){return getDefaultState().withProperty(FACING,EnumFacing.getHorizontal(m&3)).withProperty(OPEN,!helper&&(m&4)!=0).withProperty(POWERED,!helper&&(m&8)!=0);}
    @Override public int getMetaFromState(IBlockState s){return helper?0:s.getValue(FACING).getHorizontalIndex()|(s.getValue(OPEN)?4:0)|(s.getValue(POWERED)?8:0);}
    @Override public IBlockState getExtendedState(IBlockState s,IBlockAccess w,BlockPos p){RoofCell c=RoofFrameBlock.cell(w,p);return c==null?s:((IExtendedBlockState)s).withProperty(RoofFrameBlock.CELL,c);}
    @Override public boolean isOpaqueCube(IBlockState s){return false;}@Override public boolean isFullCube(IBlockState s){return false;}@Override public boolean isSideSolid(IBlockState s,IBlockAccess w,BlockPos p,EnumFacing f){return false;}
    @Override public EnumPushReaction getMobilityFlag(IBlockState s){return EnumPushReaction.BLOCK;}
    @Override public boolean canRenderInLayer(IBlockState s,BlockRenderLayer l){return l==BlockRenderLayer.SOLID||l==BlockRenderLayer.TRANSLUCENT;}
    private RoofMeshes.Mesh mesh(IBlockAccess w,BlockPos p){return RoofMeshes.get(RoofFrameBlock.cell(w,p),0);}
    @Override public AxisAlignedBB getBoundingBox(IBlockState s,IBlockAccess w,BlockPos p){return mesh(w,p).bounds;}
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s,World w,BlockPos p){return NULL_AABB;}
    @Override public void addCollisionBoxToList(IBlockState s,World w,BlockPos p,AxisAlignedBB box,List<AxisAlignedBB> out,@Nullable Entity e){for(AxisAlignedBB b:mesh(w,p).boxes)addCollisionBoxToList(p,box,out,b);}
    @Override public RayTraceResult collisionRayTrace(IBlockState s,World w,BlockPos p,Vec3d a,Vec3d b){return mesh(w,p).trace(p,a,b);}
    @Override public boolean onBlockActivated(World w,BlockPos p,IBlockState s,EntityPlayer player,EnumHand hand,@Nullable ItemStack held,EnumFacing side,float x,float y,float z){return RoofDesignInteraction.use(w,p,player,held,hand);}
    @Override public boolean removedByPlayer(IBlockState s,World w,BlockPos p,EntityPlayer player,boolean harvest){return w.isRemote||RoofDesignInteraction.mine(w,p,player);}
    @Override public List<ItemStack> getDrops(IBlockAccess w,BlockPos p,IBlockState s,int fortune){return Collections.emptyList();}
    @Override public ItemStack getPickBlock(IBlockState s,RayTraceResult hit,World w,BlockPos p,EntityPlayer player){RoofPart part=RoofDesignInteraction.target(w,p,player);return part==null?null:RoofMaterials.item(part);}
    @Override public void neighborChanged(IBlockState s,World w,BlockPos p,Block b){if(!w.isRemote&&!RoofConstruction.changing)RoofWindowController.neighbor(w,p);}
    @Override public void breakBlock(World w,BlockPos p,IBlockState s){if(!w.isRemote&&!RoofConstruction.changing){RoofCell c=RoofData.get(w).at(w,p);if(c!=null)RoofDesignInteraction.replaced(w,p,c,false);}super.breakBlock(w,p,s);}
    @Override public void onBlockExploded(World w,BlockPos p,Explosion e){RoofCell c=RoofData.get(w).at(w,p);if(c!=null)RoofDesignInteraction.replaced(w,p,c,true);}
}
