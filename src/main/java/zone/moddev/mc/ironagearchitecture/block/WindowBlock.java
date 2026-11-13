package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.*;
import net.minecraft.block.state.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.common.property.*;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;

/** Facing, open and powered consume exactly four saved bits. Hinge/style belong to identity. */
public final class WindowBlock extends BlockHorizontal {
    public static final PropertyBool OPEN=PropertyBool.create("open"),POWERED=PropertyBool.create("powered");
    public final String wood;public final int finish;public final boolean thin,diamond,right,back;
    public WindowBlock(String wood,int finish,boolean thin,boolean diamond,boolean right,boolean back) {
        super(Material.WOOD);this.wood=wood;this.finish=finish;this.thin=thin;this.diamond=diamond;this.right=right;this.back=back;
        setHardness(2);setResistance(4);setSoundType(SoundType.WOOD);setLightOpacity(0);useNeighborBrightness=true;
        setDefaultState(blockState.getBaseState().withProperty(FACING,EnumFacing.NORTH).withProperty(OPEN,false).withProperty(POWERED,false));
    }
    @Override protected BlockStateContainer createBlockState() {return new ExtendedBlockState(this,new IProperty<?>[]{FACING,OPEN,POWERED},new IUnlistedProperty<?>[]{WindowView.PROPERTY});}
    @Override public IBlockState getStateFromMeta(int meta){return getDefaultState().withProperty(FACING,EnumFacing.getHorizontal(meta&3)).withProperty(OPEN,(meta&4)!=0).withProperty(POWERED,(meta&8)!=0);}
    @Override public int getMetaFromState(IBlockState state){return state.getValue(FACING).getHorizontalIndex()|(state.getValue(OPEN)?4:0)|(state.getValue(POWERED)?8:0);}
    public WindowBlock canonical(){return ArchitectureBlocks.findWindow(wood,finish,thin,diamond,false);}
    public boolean matches(IBlockState a,IBlockState b,boolean vertical) {
        if(!(b.getBlock() instanceof WindowBlock)) return false;WindowBlock other=(WindowBlock)b.getBlock();
        return wood.equals(other.wood)&&finish==other.finish&&thin==other.thin&&back==other.back&&a.getValue(FACING)==b.getValue(FACING)
            &&(!vertical||diamond==other.diamond&&right==other.right);
    }
    public int joins(IBlockState state,IBlockAccess world,BlockPos pos) {
        int result=0;if(matches(state,world.getBlockState(pos.up()),true)) result|=WindowGeometry.UP;
        if(matches(state,world.getBlockState(pos.down()),true)) result|=WindowGeometry.DOWN;
        EnumFacing facing=state.getValue(FACING);
        if(matches(state,neighbor(world,pos.offset(facing.rotateYCCW())),false)) result|=WindowGeometry.LEFT;
        if(matches(state,neighbor(world,pos.offset(facing.rotateY())),false)) result|=WindowGeometry.RIGHT;
        return result;
    }
    private IBlockState neighbor(IBlockAccess world,BlockPos pos){return world instanceof World&&!((World)world).isBlockLoaded(pos)?net.minecraft.init.Blocks.AIR.getDefaultState():world.getBlockState(pos);}
    @Override public IBlockState getExtendedState(IBlockState state,IBlockAccess world,BlockPos pos){return ((IExtendedBlockState)state).withProperty(WindowView.PROPERTY,new WindowView(state,joins(state,world,pos),false));}
    public List<AxisAlignedBB> boxes(IBlockState state,IBlockAccess world,BlockPos pos,boolean proxy){return WindowGeometry.boxes(thin,right,state.getValue(OPEN),state.getValue(FACING),joins(state,world,pos),proxy,back);}
    @Override public boolean isOpaqueCube(IBlockState state){return false;}
    @Override public boolean isFullCube(IBlockState state){return false;}
    @Override public boolean isSideSolid(IBlockState state,IBlockAccess world,BlockPos pos,EnumFacing side){return false;}
    @Override public boolean isPassable(IBlockAccess world,BlockPos pos){return world.getBlockState(pos).getValue(OPEN);}
    @Override public boolean canRenderInLayer(IBlockState state,BlockRenderLayer layer){return layer==BlockRenderLayer.SOLID||layer==BlockRenderLayer.TRANSLUCENT;}
    @Override public AxisAlignedBB getBoundingBox(IBlockState state,IBlockAccess world,BlockPos pos){return union(boxes(state,world,pos,false));}
    static AxisAlignedBB union(List<AxisAlignedBB> boxes){AxisAlignedBB result=null;for(AxisAlignedBB box:boxes) result=result==null?box:result.union(box);return result==null?NULL_AABB:result;}
    @Override public void addCollisionBoxToList(IBlockState state,World world,BlockPos pos,AxisAlignedBB mask,List<AxisAlignedBB> boxes,@Nullable Entity entity){for(AxisAlignedBB box:boxes(state,world,pos,false)) addCollisionBoxToList(pos,mask,boxes,box);}
    @Override public RayTraceResult collisionRayTrace(IBlockState state,World world,BlockPos pos,Vec3d start,Vec3d end){return trace(pos,start,end,boxes(state,world,pos,false));}
    RayTraceResult trace(BlockPos pos,Vec3d start,Vec3d end,List<AxisAlignedBB> boxes) {
        RayTraceResult nearest=null;double distance=Double.POSITIVE_INFINITY;
        for(AxisAlignedBB box:boxes){RayTraceResult hit=rayTrace(pos,start,end,box);if(hit!=null&&start.squareDistanceTo(hit.hitVec)<distance){distance=start.squareDistanceTo(hit.hitVec);nearest=hit;}}
        return nearest;
    }
    @Override public boolean onBlockActivated(World world,BlockPos pos,IBlockState state,EntityPlayer player,EnumHand hand,@Nullable ItemStack held,EnumFacing side,float x,float y,float z) {
        ItemStack main=player.getHeldItemMainhand(),off=player.getHeldItemOffhand();
        if(player.isSneaking()||allowsPlacement(main)||allowsPlacement(off)) return false;
        if(!world.isRemote) WindowController.toggle(world,pos,player);return true;
    }
    private static boolean allowsPlacement(@Nullable ItemStack held) {
        return held != null && (held.getItem() instanceof WindowItem || held.getItem() instanceof TileHangingItem || held.getItem() instanceof SmartWallItem);
    }
    @Override public void neighborChanged(IBlockState state,World world,BlockPos pos,Block changed){if(!world.isRemote) WindowController.neighbor(world,pos);}
    @Override public void onBlockAdded(World world,BlockPos pos,IBlockState state){if(!world.isRemote) WindowController.neighbor(world,pos);}
    @Override public void breakBlock(World world,BlockPos pos,IBlockState state){WindowController.removeLeaf(world,pos,state);super.breakBlock(world,pos,state);}
    @Override public Item getItemDropped(IBlockState state,Random random,int fortune){return Item.getItemFromBlock(canonical());}
    @Override public int damageDropped(IBlockState state){return 0;}
    @Override public ItemStack getPickBlock(IBlockState state,RayTraceResult hit,World world,BlockPos pos,EntityPlayer player){return new ItemStack(canonical());}
    @Override public IBlockState withRotation(IBlockState state,Rotation rotation){return state.withProperty(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public IBlockState withMirror(IBlockState state,Mirror mirror){return mirror==Mirror.NONE?state:ArchitectureBlocks.findWindow(wood,finish,thin,diamond,!right,back).getDefaultState().withProperty(FACING,mirror.mirror(state.getValue(FACING))).withProperty(OPEN,state.getValue(OPEN)).withProperty(POWERED,state.getValue(POWERED));}
}
