package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.*;
import net.minecraft.block.state.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.common.property.*;
import zone.moddev.mc.ironagearchitecture.IronAgeArchitecture;

/** One identity and one finish bit. The shape is a server-owned derived contour. */
public final class SmartCobBlock extends Block {
    public static final PropertyInteger FINISH=PropertyInteger.create("finish",0,1);
    public static final IUnlistedProperty<SmartCobView> VIEW=new IUnlistedProperty<SmartCobView>() {
        public String getName(){return "cob_view";}public boolean isValid(SmartCobView v){return v!=null;}
        public Class<SmartCobView> getType(){return SmartCobView.class;}public String valueToString(SmartCobView v){return Integer.toHexString(v.hashCode());}
    };
    public SmartCobBlock(){super(Material.GROUND);setHardness(1.2F);setResistance(4);setSoundType(SoundType.GROUND);setHarvestLevel("shovel",0);setLightOpacity(255);useNeighborBrightness=true;setDefaultState(blockState.getBaseState().withProperty(FINISH,0));}
    @Override protected BlockStateContainer createBlockState(){return new ExtendedBlockState(this,new IProperty<?>[]{FINISH},new IUnlistedProperty<?>[]{VIEW});}
    @Override public IBlockState getStateFromMeta(int meta){return getDefaultState().withProperty(FINISH,meta&1);}
    @Override public int getMetaFromState(IBlockState state){return state.getValue(FINISH);}
    @Override public int damageDropped(IBlockState state){return state.getValue(FINISH);}
    @Override public boolean isOpaqueCube(IBlockState state){return false;}
    @Override public boolean isFullCube(IBlockState state){return false;}
    @Override public int getPackedLightmapCoords(IBlockState state,IBlockAccess world,BlockPos pos) {
        int light=world.getCombinedLight(pos,0),sky=(light>>16)&255,block=light&255;
        // A curved face can sit in a neighbouring cob cell's cut-away space. Voxel-only
        // light sampling there otherwise produces pitch-black strips along the contour.
        for(EnumFacing side:EnumFacing.values()) {int next=world.getCombinedLight(pos.offset(side),0);sky=Math.max(sky,(next>>16)&255);block=Math.max(block,next&255);}
        return sky<<16|block;
    }
    public SmartCobView view(IBlockAccess world,BlockPos pos) {
        IBlockState up=world.getBlockState(pos.up()),down=world.getBlockState(pos.down());
        return new SmartCobView(IronAgeArchitecture.proxy.cobProfile(world,pos),
            up.getBlock() instanceof SmartCobBlock?IronAgeArchitecture.proxy.cobProfile(world,pos.up()):null,
            down.getBlock() instanceof SmartCobBlock?IronAgeArchitecture.proxy.cobProfile(world,pos.down()):null,
            up.isOpaqueCube(),down.isOpaqueCube());
    }
    @Override public IBlockState getExtendedState(IBlockState state,IBlockAccess world,BlockPos pos){return ((IExtendedBlockState)state).withProperty(VIEW,view(world,pos));}
    @Override public boolean isSideSolid(IBlockState state,IBlockAccess world,BlockPos pos,EnumFacing side) {
        java.awt.geom.Area area=IronAgeArchitecture.proxy.cobProfile(world,pos).area();
        if(!side.getAxis().isHorizontal()) return area.contains(.25,.25,.5,.5);
        for(int i=4;i<12;i++){double u=(i+.5)/16;double x=side==EnumFacing.WEST?SmartCobGeometry.SKIN+.001:side==EnumFacing.EAST?1-SmartCobGeometry.SKIN-.001:u,z=side==EnumFacing.NORTH?SmartCobGeometry.SKIN+.001:side==EnumFacing.SOUTH?1-SmartCobGeometry.SKIN-.001:u;if(!area.contains(x,z))return false;}return true;
    }
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,World world,BlockPos pos){return NULL_AABB;}
    @Override public AxisAlignedBB getBoundingBox(IBlockState state,IBlockAccess world,BlockPos pos){return WindowBlock.union(IronAgeArchitecture.proxy.cobProfile(world,pos).boxes());}
    @Override public void addCollisionBoxToList(IBlockState state,World world,BlockPos pos,AxisAlignedBB query,List<AxisAlignedBB> boxes,@Nullable Entity entity){for(AxisAlignedBB box:IronAgeArchitecture.proxy.cobProfile(world,pos).boxes())addCollisionBoxToList(pos,query,boxes,box);}
    @Override public RayTraceResult collisionRayTrace(IBlockState state,World world,BlockPos pos,Vec3d start,Vec3d end){return SmartCobMesh.trace(view(world,pos),pos,start,end);}
    @Override public void onBlockAdded(World world,BlockPos pos,IBlockState state){SmartCobEvents.changed(world,pos);}
    @Override public void neighborChanged(IBlockState state,World world,BlockPos pos,Block changed){SmartCobEvents.changed(world,pos);}
    @Override public void breakBlock(World world,BlockPos pos,IBlockState state){SmartCobEvents.changed(world,pos);super.breakBlock(world,pos,state);}
    @Override public ItemStack getPickBlock(IBlockState state,RayTraceResult hit,World world,BlockPos pos,net.minecraft.entity.player.EntityPlayer player){return new ItemStack(this,1,damageDropped(state));}
}
