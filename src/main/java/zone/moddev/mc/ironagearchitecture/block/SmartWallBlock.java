package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.common.property.*;
import zone.moddev.mc.ironagearchitecture.*;

/** A persistent smart identity; framing never changes its occupied wall footprint. */
public final class SmartWallBlock extends FrameBlock {
    public static final IUnlistedProperty<Integer> LAYOUT=new IUnlistedProperty<Integer>() {
        public String getName(){return "smart_layout";}
        public boolean isValid(Integer value){return value!=null;}
        public Class<Integer> getType(){return Integer.class;}
        public String valueToString(Integer value){return value.toString();}
    };
    /** 0 full, 1 half, 2 two joined half panels. */
    public final int profile;

    public SmartWallBlock(String wood,int profile) {
        super(wood,FrameFamily.POST);this.profile=profile;
        useNeighborBrightness=profile!=0;
    }
    @Override public void onBlockAdded(World world,BlockPos pos,IBlockState state){SmartWallEvents.changed(world,pos);}
    @Override public void breakBlock(World world,BlockPos pos,IBlockState state){
        SmartWallEvents.changed(world,pos);super.breakBlock(world,pos,state);
    }
    @Override public int variants(){return 2;}
    @Override public int finish(int panel){return panel&1;}
    /** Full walls use bit 3 for the L footprint; the old low-eight states stay unchanged. */
    public int profile(IBlockState state) {return profile==0&&state.getValue(PANEL)>=2?2:profile;}
    public IBlockState state(int shape,EnumFacing facing,int finish) {
        if(shape!=profile && !(profile==0&&shape==2)) throw new IllegalArgumentException("Wrong smart wall profile");
        return getDefaultState().withProperty(FACING,facing).withProperty(PANEL,(finish&1)+(profile==0&&shape==2?2:0));
    }
    @Override public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING,EnumFacing.getHorizontal(meta&3))
            .withProperty(PANEL,(meta>>2)&(profile==0?3:1));
    }
    @Override public int damageDropped(IBlockState state) {return finish(state.getValue(PANEL));}
    @Override protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this,new IProperty<?>[]{FACING,PANEL},
            new IUnlistedProperty<?>[]{CONNECTIONS,TimberCorners.PROPERTY,LAYOUT});
    }
    public int mask(IBlockState state){return SmartWallGeometry.mask(profile(state),state.getValue(FACING));}
    @Override public IBlockState getStateForPlacement(World world,BlockPos pos,EnumFacing side,float x,float y,float z,int meta,EntityLivingBase player) {
        EnumFacing facing=side.getAxis().isHorizontal()?side.getOpposite():player.getHorizontalFacing().getOpposite();
        if(profile==1&&!player.isSneaking()) {
            EnumFacing inherited=WallRuns.straightHalfFacing(world.getBlockState(pos.offset(side.getOpposite())));
            if(inherited!=null) facing=inherited;
        }
        return getDefaultState().withProperty(FACING,facing).withProperty(PANEL,meta&1);
    }
    @Override public IBlockState getExtendedState(IBlockState state,IBlockAccess world,BlockPos pos) {
        return ((IExtendedBlockState)state).withProperty(CONNECTIONS,SmartWallGeometry.coverage(world,pos)).withProperty(TimberCorners.PROPERTY,0)
            .withProperty(LAYOUT,IronAgeArchitecture.proxy.smartLayout(world,pos));
    }
    @Override public IBlockState withMirror(IBlockState state,net.minecraft.util.Mirror mirror) {
        if(profile(state)!=2||mirror==net.minecraft.util.Mirror.NONE) return state.withProperty(FACING,mirror.mirror(state.getValue(FACING)));
        int original=mask(state),target=0;
        for(int q=0;q<4;q++) if((original&(1<<q))!=0) {
            int x=q%2,z=q/2;
            if(mirror==net.minecraft.util.Mirror.FRONT_BACK) x=1-x;else z=1-z;
            target|=1<<(x+z*2);
        }
        for(EnumFacing facing:EnumFacing.HORIZONTALS) if(SmartWallGeometry.mask(profile(state),facing)==target)
            return state.withProperty(FACING,facing);
        throw new IllegalStateException("Missing mirrored smart corner");
    }
    @Override public int connections(IBlockState state,IBlockAccess world,BlockPos pos){return 0;}
    @Override public List<WallGeometry.Part> parts(IBlockState state,int code){
        return SmartWallGeometry.parts(mask(state),code==0?SmartWallGeometry.FALLBACK:code,0);
    }
    @Override public List<WallGeometry.Part> parts(IBlockState state,int code,int coverage){
        return SmartWallGeometry.parts(mask(state),code==0?SmartWallGeometry.FALLBACK:code,coverage);
    }
    @Nullable @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,World world,BlockPos pos){return NULL_AABB;}
    @Override public boolean isFullCube(IBlockState state){return profile(state)==0;}
    @Override public boolean getUseNeighborBrightness(IBlockState state){return profile(state)!=0;}
    @Override public boolean isSideSolid(IBlockState state,IBlockAccess world,BlockPos pos,EnumFacing side) {
        if(profile(state)==0) return true;
        if(!side.getAxis().isHorizontal()) return false;
        int mask=mask(state);
        for(AxisAlignedBB box:SmartWallGeometry.boxes(mask)) {
            if(side==EnumFacing.NORTH&&box.minZ==0||side==EnumFacing.SOUTH&&box.maxZ==1
                ||side==EnumFacing.WEST&&box.minX==0||side==EnumFacing.EAST&&box.maxX==1) {
                int required=side==EnumFacing.NORTH?3:side==EnumFacing.SOUTH?12:side==EnumFacing.WEST?5:10;
                return (mask&required)==required;
            }
        }
        return false;
    }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state,IBlockAccess world,BlockPos pos){
        return WindowBlock.union(SmartWallGeometry.boxes(mask(state)));
    }
    @Override public void addCollisionBoxToList(IBlockState state,World world,BlockPos pos,AxisAlignedBB query,
            List<AxisAlignedBB> boxes,@Nullable Entity entity) {
        for(AxisAlignedBB box:SmartWallGeometry.boxes(mask(state))) addCollisionBoxToList(pos,query,boxes,box);
    }
    @Nullable @Override public RayTraceResult collisionRayTrace(IBlockState state,World world,BlockPos pos,Vec3d start,Vec3d end) {
        RayTraceResult closest=null;
        for(AxisAlignedBB box:SmartWallGeometry.boxes(mask(state))) {
            RayTraceResult hit=rayTrace(pos,start,end,box);
            if(hit!=null&&(closest==null||start.squareDistanceTo(hit.hitVec)<start.squareDistanceTo(closest.hitVec))) closest=hit;
        }
        return closest;
    }
    @Override public List<ItemStack> getDrops(IBlockAccess world,BlockPos pos,IBlockState state,int fortune) {
        return Collections.singletonList(getSilkTouchDrop(state));
    }
    @Override protected ItemStack getSilkTouchDrop(IBlockState state) {
        int shape=profile(state);
        return new ItemStack(ArchitectureBlocks.findSmart(wood,shape==2?1:shape),shape==2?2:1,finish(state.getValue(PANEL)));
    }
    @Override public ItemStack getPickBlock(IBlockState state,RayTraceResult hit,World world,BlockPos pos,EntityPlayer player) {
        int shape=profile(state);
        return new ItemStack(ArchitectureBlocks.findSmart(wood,shape==2?1:shape),1,finish(state.getValue(PANEL)));
    }
}
