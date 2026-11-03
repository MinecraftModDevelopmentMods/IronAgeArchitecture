package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;

/** Upper-half floor. Metadata: rotation(2), joist axis(1), bracket reach(1). */
public final class JettyFloorBlock extends FrameBlock {
    public final FloorShape shape;
    public final EnumFacing bracket;
    public JettyFloorBlock(String wood,FloorShape shape,EnumFacing bracket) {
        super(wood,FrameFamily.RAIL);this.shape=shape;this.bracket=bracket;
        this.useNeighborBrightness=true;
    }
    @Override public int variants() {return 1;}
    @Override public int finish(int panel) {return 0;}
    @Override public IBlockState getStateFromMeta(int meta) {
        int panel=(meta>>2)&(bracket==null?1:3);
        if(bracket!=null&&JettyFloorGeometry.bracketAnchor(shape,bracket,(panel&2)!=0)==null) panel&=1;
        return getDefaultState().withProperty(FACING,EnumFacing.getHorizontal(meta&3)).withProperty(PANEL,panel);
    }
    @Override public int damageDropped(IBlockState state) {return 0;}
    public JettyFloorBlock canonical() {return ArchitectureBlocks.findFloor(wood,shape,null);}
    @Override public Item getItemDropped(IBlockState state,Random random,int fortune) {return Item.getItemFromBlock(canonical());}
    @Override public List<ItemStack> getDrops(IBlockAccess world,BlockPos pos,IBlockState state,int fortune) {
        List<ItemStack> result=new ArrayList<>();result.add(new ItemStack(canonical()));
        if(bracket!=null) result.add(new ItemStack(ArchitectureBlocks.findBracket(wood,(state.getValue(PANEL)&2)!=0)));
        return result;
    }
    @Override public ItemStack getPickBlock(IBlockState state,RayTraceResult hit,World world,BlockPos pos,EntityPlayer player) {return new ItemStack(canonical());}
    @Override public boolean isFullCube(IBlockState state) {return false;}
    @Override public boolean isSideSolid(IBlockState state,IBlockAccess world,BlockPos pos,EnumFacing side) {return shape==FloorShape.FULL&&side==EnumFacing.UP;}
    @Override public IBlockState getStateForPlacement(World world,BlockPos pos,EnumFacing side,float x,float y,float z,int meta,EntityLivingBase player) {
        EnumFacing facing=player.getHorizontalFacing();
        if(shape==FloorShape.HALF) {
            if(side.getAxis().isHorizontal()) facing=side.getOpposite();
            else facing=facing.getAxis()==EnumFacing.Axis.X?(x<.5?EnumFacing.WEST:EnumFacing.EAST):(z<.5?EnumFacing.NORTH:EnumFacing.SOUTH);
        } else if(shape==FloorShape.QUARTER||shape==FloorShape.L) {
            int selected=(x<.5?0:1)+(z<.5?0:2);
            for(EnumFacing candidate:EnumFacing.HORIZONTALS) {
                int mask=shape.rotatedMask(candidate);
                if((shape==FloorShape.QUARTER?mask:15^mask)==(1<<selected)) {facing=candidate;break;}
            }
        }
        EnumFacing local=unrotate(player.getHorizontalFacing(),facing);
        return getDefaultState().withProperty(FACING,facing).withProperty(PANEL,local.getAxis()==EnumFacing.Axis.X?1:0);
    }
    public static EnumFacing unrotate(EnumFacing direction,EnumFacing rotation) {
        for(EnumFacing candidate:EnumFacing.HORIZONTALS) if(rotate(candidate,rotation)==direction) return candidate;
        throw new IllegalArgumentException();
    }
    public static EnumFacing rotate(EnumFacing direction,EnumFacing rotation) {
        double[] a=WallGeometry.rotate(8,0,8,rotation),b=WallGeometry.rotate(8+direction.getFrontOffsetX(),0,8+direction.getFrontOffsetZ(),rotation);
        return EnumFacing.getFacingFromVector((float)(b[0]-a[0]),0,(float)(b[2]-a[2]));
    }
    @Override public IBlockState withMirror(IBlockState state,Mirror mirror) {
        if(mirror==Mirror.NONE) return state;
        int mask=shape.rotatedMask(state.getValue(FACING)),mirrored=0;
        for(int q=0;q<4;q++) if((mask&(1<<q))!=0) {
            int x=q%2,z=q/2;
            if(mirror==Mirror.FRONT_BACK) x=1-x;else z=1-z;
            mirrored|=1<<(x+z*2);
        }
        EnumFacing facing=mirror.mirror(state.getValue(FACING));
        if(shape.rotatedMask(facing)!=mirrored) for(EnumFacing candidate:EnumFacing.HORIZONTALS) if(shape.rotatedMask(candidate)==mirrored) {facing=candidate;break;}
        EnumFacing axis=rotate((state.getValue(PANEL)&1)==0?EnumFacing.NORTH:EnumFacing.EAST,state.getValue(FACING));
        int panel=(unrotate(mirror.mirror(axis),facing).getAxis()==EnumFacing.Axis.X?1:0)|(state.getValue(PANEL)&2);
        EnumFacing nextBracket=bracket==null?null:unrotate(mirror.mirror(rotate(bracket,state.getValue(FACING))),facing);
        return ArchitectureBlocks.findFloor(wood,shape,nextBracket).getDefaultState().withProperty(FACING,facing).withProperty(PANEL,panel);
    }
    public boolean covers(IBlockState state,double x,double z) {
        return x>=0&&x<16&&z>=0&&z<16&&(shape.rotatedMask(state.getValue(FACING))&(1<<((x<8?0:1)+(z<8?0:2))))!=0;
    }
    @Override public int connections(IBlockState state,IBlockAccess world,BlockPos pos) {
        int mask=0;EnumFacing facing=state.getValue(FACING);
        for(int q=0;q<4;q++) if((shape.mask&(1<<q))!=0) for(EnumFacing side:EnumFacing.HORIZONTALS) {
            double[] p=WallGeometry.rotate(q%2*8+4+side.getFrontOffsetX()*8,0,q/2*8+4+side.getFrontOffsetZ()*8,facing);
            int dx=(int)Math.floor(p[0]/16),dz=(int)Math.floor(p[2]/16);
            IBlockState next=world.getBlockState(pos.add(dx,0,dz));
            if(next.getBlock() instanceof JettyFloorBlock) {
                JettyFloorBlock block=(JettyFloorBlock)next.getBlock();
                if(wood.equals(block.wood)&&block.covers(next,p[0]-dx*16,p[2]-dz*16)) mask|=1<<(q*4+side.getHorizontalIndex());
            }
        }
        IBlockState below=world.getBlockState(pos.down());
        int support=0;
        if(below.getBlock() instanceof FrameBlock&&!(below.getBlock() instanceof JettyFloorBlock)||below.getBlock() instanceof WindowBlock) {
            for(int q=0;q<4;q++) {
                double[] point=WallGeometry.rotate(q%2*8+4,0,q/2*8+4,facing);
                boolean occupied=true;
                if(below.getBlock() instanceof WindowBlock&&((WindowBlock)below.getBlock()).thin) {
                    EnumFacing turn=below.getValue(WindowBlock.FACING),inverse=turn==EnumFacing.EAST?EnumFacing.WEST:turn==EnumFacing.WEST?EnumFacing.EAST:turn;
                    occupied=(WallGeometry.rotate(point[0],0,point[2],inverse)[2]>=8)==((WindowBlock)below.getBlock()).back;
                }
                if(below.getBlock() instanceof PartialWallBlock) {
                    EnumFacing turn=below.getValue(FACING);
                    EnumFacing inverse=turn==EnumFacing.EAST?EnumFacing.WEST:turn==EnumFacing.WEST?EnumFacing.EAST:turn;
                    double[] local=WallGeometry.rotate(point[0],0,point[2],inverse);
                    occupied=((PartialWallBlock)below.getBlock()).profile.contains(local[0],local[2]);
                }
                if(occupied) support|=1<<q;
            }
        } else if(!(below.getBlock() instanceof JettyFloorBlock)&&below.getBlock().isSideSolid(below,world,pos.down(),EnumFacing.UP)) support=15;
        return mask|((support&shape.mask)<<16);
    }
    @Override public List<WallGeometry.Part> parts(IBlockState state,int joins) {
        boolean full=(state.getValue(PANEL)&2)!=0;
        if(bracket!=null&&JettyFloorGeometry.bracketAnchor(shape,bracket,full)==null) full=false;
        // Some hidden bracket directions have no valid footprint. They are never placeable.
        EnumFacing supported=bracket!=null&&JettyFloorGeometry.bracketAnchor(shape,bracket,full)==null?null:bracket;
        return JettyFloorGeometry.parts(shape,(state.getValue(PANEL)&1)!=0,joins,supported,full);
    }
}
