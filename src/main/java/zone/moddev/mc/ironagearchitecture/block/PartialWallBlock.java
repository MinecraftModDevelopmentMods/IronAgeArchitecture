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

/** Fixed partial footprint with neighbour-derived timber joins. */
public final class PartialWallBlock extends FrameBlock {
    public final WallProfile profile;
    public PartialWallBlock(String wood,FrameFamily family,WallProfile profile) {
        super(wood,family);this.profile=profile;
        this.useNeighborBrightness=true;
    }
    @Override public int variants() {return family==FrameFamily.INFILL||family==FrameFamily.TERMINAL&&!profile.corner?2:4;}
    @Override public int finish(int panel) {return variants()==2?panel&1:panel>>1;}
    public PartialWallBlock canonical() {
        return ArchitectureBlocks.findPartial(wood,family,profile==WallProfile.SHORT_RIGHT?WallProfile.SHORT_LEFT:
            profile==WallProfile.CORNER_HALF_RIGHT?WallProfile.CORNER_HALF_LEFT:profile);
    }
    @Override public Item getItemDropped(IBlockState state,Random random,int fortune) {return Item.getItemFromBlock(canonical());}
    @Override public ItemStack getPickBlock(IBlockState state,RayTraceResult hit,World world,BlockPos pos,EntityPlayer player) {
        return new ItemStack(canonical(),1,damageDropped(state));
    }
    @Override public IBlockState getStateForPlacement(World world,BlockPos pos,EnumFacing side,float x,float y,float z,int meta,EntityLivingBase player) {
        EnumFacing facing=side.getAxis().isHorizontal()?side.getOpposite():player.getHorizontalFacing().getOpposite();
        if(!side.getAxis().isHorizontal()) {
            if(facing.getAxis()==EnumFacing.Axis.X) facing=x<.5?EnumFacing.WEST:EnumFacing.EAST;
            else facing=z<.5?EnumFacing.NORTH:EnumFacing.SOUTH;
        }
        IBlockState previous=world.getBlockState(pos.offset(side.getOpposite()));
        if(!player.isSneaking()&&previous.getBlock() instanceof PartialWallBlock) {
            PartialWallBlock block=(PartialWallBlock)previous.getBlock();
            if(!block.profile.corner&&!profile.corner) facing=previous.getValue(FACING);
        }
        WallProfile chosen=profile;
        double[] local=WallGeometry.rotate(x*16,0,z*16,inverse(facing));
        if(profile==WallProfile.SHORT_LEFT||profile==WallProfile.SHORT_RIGHT) chosen=local[0]<8?WallProfile.SHORT_LEFT:WallProfile.SHORT_RIGHT;
        if(profile==WallProfile.CORNER_HALF_LEFT||profile==WallProfile.CORNER_HALF_RIGHT) chosen=local[0]<local[2]?WallProfile.CORNER_HALF_LEFT:WallProfile.CORNER_HALF_RIGHT;
        PartialWallBlock placed=ArchitectureBlocks.findPartial(wood,family,chosen);
        return placed.getDefaultState().withProperty(FACING,facing).withProperty(PANEL,Math.floorMod(meta,variants()));
    }
    private static EnumFacing inverse(EnumFacing facing) {
        return facing==EnumFacing.EAST?EnumFacing.WEST:facing==EnumFacing.WEST?EnumFacing.EAST:facing;
    }
    @Override public IBlockState withMirror(IBlockState state,Mirror mirror) {
        if(mirror==Mirror.NONE) return state;
        if(profile.corner) {
            EnumFacing old=state.getValue(FACING);
            double[] anchor=WallGeometry.rotate(profile.x0,0,profile.z0,old);
            if(mirror==Mirror.FRONT_BACK) anchor[0]=16-anchor[0];else anchor[2]=16-anchor[2];
            int target=0;
            for(int q=0;q<4;q++) {
                double x=q%2*8+4,z=q/2*8+4;
                double mx=mirror==Mirror.FRONT_BACK?16-x:x,mz=mirror==Mirror.LEFT_RIGHT?16-z:z;
                double[] p=WallGeometry.rotate(mx,0,mz,inverse(old));
                if(profile.contains(p[0],p[2])) target|=1<<q;
            }
            for(WallProfile candidate:WallProfile.values()) if(candidate.corner&&candidate.quarters()==profile.quarters())
                for(EnumFacing turn:EnumFacing.HORIZONTALS) {
                    double[] point=WallGeometry.rotate(candidate.x0,0,candidate.z0,turn);
                    if(Math.abs(point[0]-anchor[0])>.001||Math.abs(point[2]-anchor[2])>.001) continue;
                    int footprint=0;
                    for(int q=0;q<4;q++) {double[] p=WallGeometry.rotate(q%2*8+4,0,q/2*8+4,inverse(turn));if(candidate.contains(p[0],p[2])) footprint|=1<<q;}
                    if(footprint==target) return ArchitectureBlocks.findPartial(wood,family,candidate).getDefaultState().withProperty(FACING,turn).withProperty(PANEL,state.getValue(PANEL));
                }
            throw new IllegalStateException("No mirrored corner");
        }
        EnumFacing old=state.getValue(FACING),next=mirror.mirror(old);
        boolean reversed=mirror.mirror(old.rotateY())!=next.rotateY();
        WallProfile p=profile;
        if(reversed) {
            if(p==WallProfile.SHORT_LEFT) p=WallProfile.SHORT_RIGHT;else if(p==WallProfile.SHORT_RIGHT) p=WallProfile.SHORT_LEFT;
            if(p==WallProfile.CORNER_HALF_LEFT) {p=WallProfile.CORNER_HALF_RIGHT;next=next.rotateYCCW();}
            else if(p==WallProfile.CORNER_HALF_RIGHT) {p=WallProfile.CORNER_HALF_LEFT;next=next.rotateYCCW();}
            else if(p.corner) next=next.rotateYCCW();
        }
        int panel=state.getValue(PANEL);
        if(reversed&&(family==FrameFamily.BRACE)) panel^=1;
        return ArchitectureBlocks.findPartial(wood,family,p).getDefaultState().withProperty(FACING,next).withProperty(PANEL,panel);
    }
    @Override public boolean isFullCube(IBlockState state) {return false;}
    @Override public boolean isSideSolid(IBlockState state,IBlockAccess world,BlockPos pos,EnumFacing side) {
        // Only a complete wall face can support attachments; never the empty half.
        return profile==WallProfile.THIN&&side==state.getValue(FACING);
    }
    @Override public List<WallGeometry.Part> parts(IBlockState state,int mask) {
        return PartialWallGeometry.parts(profile,family,state.getValue(PANEL),state.getValue(FACING),mask);
    }
    private boolean timber(FrameBlock other) {return wood!=null&&wood.equals(other.wood);}
    @Override public int connections(IBlockState state,IBlockAccess world,BlockPos pos) {
        int mask=0;
        for(EnumFacing direction:EnumFacing.HORIZONTALS) {
            IBlockState neighbour=world.getBlockState(pos.offset(direction));
            if(WallRuns.touches(state,pos,neighbour,pos.offset(direction),direction)) mask|=WallGeometry.bit(direction);
            if(family==FrameFamily.INFILL) continue;
            IBlockState previous=state;BlockPos previousPos=pos;
            for(int distance=1;distance<=16;distance++) {
                BlockPos at=pos.offset(direction,distance);IBlockState next=world.getBlockState(at);
                if(next.getBlock() instanceof WindowBlock) {
                    WindowBlock window=(WindowBlock)next.getBlock();
                    if(wood!=null&&wood.equals(window.wood)&&WallRuns.touches(previous,previousPos,next,at,direction)) {
                        int joins=window.joins(next,world,at);if((joins&WindowGeometry.DOWN)==0) mask|=WallGeometry.bit(direction)<<4;if((joins&WindowGeometry.UP)==0) mask|=WallGeometry.bit(direction)<<8;
                    }
                    break;
                }
                if(!(next.getBlock() instanceof FrameBlock)||!timber((FrameBlock)next.getBlock())||!WallRuns.touches(previous,previousPos,next,at,direction)) break;
                FrameBlock block=(FrameBlock)next.getBlock();
                if(block.family==FrameFamily.RAIL) {mask|=WallGeometry.bit(direction)<<((next.getValue(PANEL)&1)==0?4:8);break;}
                if(block.family==FrameFamily.INFILL||block.family==FrameFamily.TERMINAL) break;
                previous=next;previousPos=at;
            }
        }
        mask=RailJoins.bridge(state,world,pos,mask);
        if(family==FrameFamily.RAIL) for(int dy:new int[]{-1,1}) {
            IBlockState next=world.getBlockState(pos.up(dy));
            if(!(next.getBlock() instanceof PartialWallBlock)) continue;
            PartialWallBlock other=(PartialWallBlock)next.getBlock();
            if(!timber(other)||other.profile!=profile||next.getValue(FACING)!=state.getValue(FACING)) continue;
            int code=other.family==FrameFamily.POST&&(next.getValue(PANEL)&1)==0?1:other.family==FrameFamily.STUD?((next.getValue(PANEL)&1)==0?2:3):0;
            mask|=code<<(dy<0?12:16);
        }
        if(family==FrameFamily.POST&&(state.getValue(PANEL)&1)==0||family==FrameFamily.RAIL||family==FrameFamily.TERMINAL) {
            EnumFacing plane=state.getValue(FACING);
            for(int sign:new int[]{-1,1}) for(int dy:new int[]{-1,1}) {
                BlockPos at=pos.offset(plane.rotateY(),sign).up(dy);IBlockState next=world.getBlockState(at);
                if(!(next.getBlock() instanceof PartialWallBlock)) continue;
                PartialWallBlock other=(PartialWallBlock)next.getBlock();
                if(!timber(other)||other.family!=FrameFamily.BRACE) continue;
                boolean aligned=false;
                for(WallRuns.Run a:WallRuns.of(state,pos)) for(WallRuns.Run b:WallRuns.of(next,at)) if(a.aligned(b)) aligned=true;
                if(!aligned) continue;
                boolean rising=(next.getValue(PANEL)&1)==0;if(next.getValue(FACING)!=plane) rising=!rising;
                if(dy==(rising?sign:-sign)) mask|=(rising?(sign>0?1:2):(sign>0?4:8))<<20;
            }
        }
        return mask;
    }
}
