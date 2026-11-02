package zone.moddev.mc.ironagearchitecture.block;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.Mirror;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraft.block.properties.IProperty;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult;

/** Deliberate wall modules; joins are render-derived rather than saved or tile-backed. */
public class FrameBlock extends BlockHorizontal {
    public static final PropertyInteger PANEL = PropertyInteger.create("panel", 0, 3);
    public static final IUnlistedProperty<Integer> CONNECTIONS = new IUnlistedProperty<Integer>() {
        @Override public String getName() { return "connections"; }
        @Override public boolean isValid(Integer value) { return value!=null&&value>=0&&value<(1<<28); }
        @Override public Class<Integer> getType() { return Integer.class; }
        @Override public String valueToString(Integer value) { return value.toString(); }
    };
    public final String wood;
    public final FrameFamily family;
    public FrameBlock(String wood,FrameFamily family) {
        super(family==FrameFamily.INFILL?Material.GROUND:Material.WOOD);
        this.wood=wood;this.family=family;
        setHardness(2F); setResistance(5F); setSoundType(SoundType.WOOD); setHarvestLevel("axe", 0);
        if(family==FrameFamily.INFILL) {setSoundType(SoundType.GROUND);setHarvestLevel("shovel",0);}
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(PANEL, 0));
        setLightOpacity(255);
    }
    public int variants() { return family==FrameFamily.INFILL?2:4; }
    public int finish(int panel) { return family==FrameFamily.INFILL?panel&1:panel>>1; }
    @Override protected BlockStateContainer createBlockState() { return new ExtendedBlockState(this,new IProperty<?>[]{FACING,PANEL},new IUnlistedProperty<?>[]{CONNECTIONS}); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3)).withProperty(PANEL, ((meta >> 2) & 3)%variants()); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getHorizontalIndex() | (state.getValue(PANEL) << 2); }
    @Override public int damageDropped(IBlockState state) { return state.getValue(PANEL)%variants(); }
    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float x, float y, float z, int meta, EntityLivingBase player) {
        return getDefaultState().withProperty(FACING, player.getHorizontalFacing().getOpposite()).withProperty(PANEL, Math.floorMod(meta, variants()));
    }
    @Override public IBlockState withRotation(IBlockState state, Rotation rotation) { return state.withProperty(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public IBlockState withMirror(IBlockState state, Mirror mirror) {
        EnumFacing old=state.getValue(FACING),next=mirror.mirror(old);
        int panel=state.getValue(PANEL);
        if(family==FrameFamily.TERMINAL&&(panel&1)==0) {
            EnumFacing other=mirror.mirror(old.rotateYCCW());
            for(EnumFacing candidate:EnumFacing.HORIZONTALS)
                if((candidate==next&&candidate.rotateYCCW()==other)||(candidate==other&&candidate.rotateYCCW()==next))
                    return state.withProperty(FACING,candidate);
        }
        boolean reversed=mirror.mirror(old.rotateY())!=next.rotateY();
        if(reversed&&family==FrameFamily.BRACE) panel^=1;
        if(reversed&&family==FrameFamily.TERMINAL) next=next.getOpposite();
        return state.withProperty(FACING,next).withProperty(PANEL,panel);
    }
    private boolean sameTimber(FrameBlock other) { return wood!=null&&wood.equals(other.wood); }
    private int railRequest(IBlockAccess world,BlockPos pos,EnumFacing direction,EnumFacing plane) {
        IBlockState previous=world.getBlockState(pos);BlockPos previousPos=pos;
        for(int distance=1;distance<=16;distance++) {
            IBlockState neighbour=world.getBlockState(pos.offset(direction,distance));
            if(!(neighbour.getBlock() instanceof FrameBlock)) return 0;
            FrameBlock block=(FrameBlock)neighbour.getBlock();
            if(block instanceof JettyFloorBlock) return 0;
            if((block instanceof PartialWallBlock||previous.getBlock() instanceof PartialWallBlock)
                &&!WallRuns.touches(previous,previousPos,neighbour,pos.offset(direction,distance),direction)) return 0;
            if(!sameTimber(block)||neighbour.getValue(FACING).getAxis()!=plane.getAxis()) return 0;
            if(block.family==FrameFamily.RAIL) return 1<<(neighbour.getValue(PANEL)&1);
            if(block.family==FrameFamily.INFILL||block.family==FrameFamily.TERMINAL) return 0;
            previous=neighbour;previousPos=pos.offset(direction,distance);
        }
        return 0;
    }
    public int connections(IBlockState state,IBlockAccess world,BlockPos pos) {
        int mask=0;
        EnumFacing facing=state.getValue(FACING);
        for(EnumFacing direction:EnumFacing.HORIZONTALS) {
            IBlockState neighbour=world.getBlockState(pos.offset(direction));
            if(neighbour.getBlock() instanceof FrameBlock&&!(neighbour.getBlock() instanceof JettyFloorBlock)) mask|=WallGeometry.bit(direction);
            if(family!=FrameFamily.INFILL) {
                EnumFacing plane=direction.getAxis()==facing.getAxis()?facing.rotateY():facing;
                int request=railRequest(world,pos,direction,plane);
                if((request&1)!=0) mask|=WallGeometry.bit(direction)<<4;
                if((request&2)!=0) mask|=WallGeometry.bit(direction)<<8;
            }
        }
        if(family==FrameFamily.RAIL) {
            mask|=verticalStud(world,pos.down(),facing)<<12;
            mask|=verticalStud(world,pos.up(),facing)<<16;
        }
        if(family==FrameFamily.POST&&(state.getValue(PANEL)&1)==0||family==FrameFamily.TERMINAL||family==FrameFamily.RAIL) {
            mask|=braceJoins(world,pos,facing)<<20;
            if(family==FrameFamily.TERMINAL&&(state.getValue(PANEL)&1)==0) mask|=braceJoins(world,pos,facing.rotateYCCW())<<24;
        }
        return mask;
    }
    private int verticalStud(IBlockAccess world,BlockPos pos,EnumFacing facing) {
        IBlockState state=world.getBlockState(pos);
        if(!(state.getBlock() instanceof FrameBlock)) return 0;
        FrameBlock other=(FrameBlock)state.getBlock();
        if(!sameTimber(other)||state.getValue(FACING).getAxis()!=facing.getAxis()) return 0;
        if(other.family==FrameFamily.POST&&(state.getValue(PANEL)&1)==0) return 1;
        if(other.family==FrameFamily.STUD) return (state.getValue(PANEL)&1)==0?2:3;
        return 0;
    }
    private int braceJoins(IBlockAccess world,BlockPos pos,EnumFacing plane) {
        int result=0;
        for(int sign:new int[]{-1,1}) for(int dy:new int[]{-1,1}) {
            IBlockState state=world.getBlockState(pos.offset(plane.rotateY(),sign).up(dy));
            if(!(state.getBlock() instanceof FrameBlock)) continue;
            FrameBlock other=(FrameBlock)state.getBlock();
            if(!sameTimber(other)||other.family!=FrameFamily.BRACE||state.getValue(FACING).getAxis()!=plane.getAxis()) continue;
            boolean rising=(state.getValue(PANEL)&1)==0;
            if(state.getValue(FACING)!=plane) rising=!rising;
            if(dy==(rising?sign:-sign)) result|=rising?(sign>0?1:2):(sign>0?4:8);
        }
        return result;
    }
    @Override public IBlockState getExtendedState(IBlockState state,IBlockAccess world,BlockPos pos) {
        return ((IExtendedBlockState)state).withProperty(CONNECTIONS,connections(state,world,pos));
    }
    public List<WallGeometry.Part> parts(IBlockState state,int mask) { return WallGeometry.parts(family,state.getValue(PANEL),state.getValue(FACING),mask); }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    // These are filled wall cells. Forge 1.10 must sample the neighbouring cell for
    // face light even though the plaster has a small cosmetic recess. Opacity still
    // blocks light through the wall; collision follows the actual shared mesh bounds.
    @Override public boolean isFullCube(IBlockState state) { return true; }
    @Override public boolean isSideSolid(IBlockState state,IBlockAccess world,BlockPos pos,EnumFacing side) { return true; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state,IBlockAccess world,BlockPos pos) {
        AxisAlignedBB result=null;
        for(WallGeometry.Part part:parts(state,connections(state,world,pos))) result=result==null?part.bounds(state.getValue(FACING)):result.union(part.bounds(state.getValue(FACING)));
        return result;
    }
    @Override public void addCollisionBoxToList(IBlockState state,World world,BlockPos pos,AxisAlignedBB mask,List<AxisAlignedBB> boxes,@Nullable Entity entity) {
        for(WallGeometry.Part part:parts(state,connections(state,world,pos))) addCollisionBoxToList(pos,mask,boxes,part.bounds(state.getValue(FACING)));
    }
    @Nullable @Override public RayTraceResult collisionRayTrace(IBlockState state,World world,BlockPos pos,Vec3d start,Vec3d end) {
        RayTraceResult result=null;double closest=Double.POSITIVE_INFINITY;
        for(WallGeometry.Part part:parts(state,connections(state,world,pos))) {
            RayTraceResult hit=rayTrace(pos,start,end,part.bounds(state.getValue(FACING)));
            if(hit!=null&&start.squareDistanceTo(hit.hitVec)<closest) {result=hit;closest=start.squareDistanceTo(hit.hitVec);}
        }
        return result;
    }
}
