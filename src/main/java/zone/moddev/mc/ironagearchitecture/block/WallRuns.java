package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/** World-space occupied strips; a neighbour's axis alone is not a connection. */
public final class WallRuns {
    private WallRuns() { }
    public static final class Run {
        public final EnumFacing tangent;
        public final double low,high,depth0,depth1;
        Run(EnumFacing tangent,double low,double high,double depth0,double depth1) {
            this.tangent=tangent;this.low=low;this.high=high;this.depth0=depth0;this.depth1=depth1;
        }
        public boolean aligned(Run other) {
            return tangent.getAxis()==other.tangent.getAxis()&&Math.min(depth1,other.depth1)>Math.max(depth0,other.depth0)+.001
                &&(Math.abs(depth0-other.depth0)<.001||Math.abs(depth1-other.depth1)<.001);
        }
        public boolean touches(Run other,EnumFacing direction) {
            return aligned(other)&&Math.abs(direction.getAxisDirection()==EnumFacing.AxisDirection.POSITIVE?high-other.low:low-other.high)<.001;
        }
    }
    private static void add(List<Run> list,BlockPos pos,EnumFacing facing,double x0,double x1,double z0,double z1) {
        double[] a=WallGeometry.rotate(x0,0,z0,facing),b=WallGeometry.rotate(x1,0,z1,facing);
        EnumFacing tangent=facing.rotateY();
        double low,high,d0,d1;
        if(tangent.getAxis()==EnumFacing.Axis.X) {
            low=pos.getX()*16+Math.min(a[0],b[0]);high=pos.getX()*16+Math.max(a[0],b[0]);
            d0=pos.getZ()*16+Math.min(a[2],b[2]);d1=pos.getZ()*16+Math.max(a[2],b[2]);
        } else {
            low=pos.getZ()*16+Math.min(a[2],b[2]);high=pos.getZ()*16+Math.max(a[2],b[2]);
            d0=pos.getX()*16+Math.min(a[0],b[0]);d1=pos.getX()*16+Math.max(a[0],b[0]);
        }
        list.add(new Run(tangent,low,high,d0,d1));
    }
    public static List<Run> of(IBlockState state,BlockPos pos) {
        List<Run> list=new ArrayList<>();
        if(!(state.getBlock() instanceof FrameBlock)||state.getBlock() instanceof JettyFloorBlock) return list;
        EnumFacing facing=state.getValue(FrameBlock.FACING);
        if(state.getBlock() instanceof PartialWallBlock) {
            WallProfile p=((PartialWallBlock)state.getBlock()).profile;
            if(!p.corner) add(list,pos,facing,p.x0,p.x1,p.z0,p.z1);
            else {
                add(list,pos,facing,p.x0,p.x1,p.z0,p.z0+8);
                // The perpendicular strip has its own tangent, normal and half-grid plane.
                EnumFacing other=facing.rotateYCCW();
                double[] a=WallGeometry.rotate(p.x0,0,p.z0,facing),b=WallGeometry.rotate(p.x0+8,0,p.z1,facing);
                boolean x=other.rotateY().getAxis()==EnumFacing.Axis.X;
                list.add(new Run(other.rotateY(),(x?pos.getX():pos.getZ())*16+Math.min(a[x?0:2],b[x?0:2]),
                    (x?pos.getX():pos.getZ())*16+Math.max(a[x?0:2],b[x?0:2]),
                    (x?pos.getZ():pos.getX())*16+Math.min(a[x?2:0],b[x?2:0]),
                    (x?pos.getZ():pos.getX())*16+Math.max(a[x?2:0],b[x?2:0])));
            }
        } else {
            add(list,pos,facing,0,16,0,16);
            FrameBlock block=(FrameBlock)state.getBlock();
            if(block.family==FrameFamily.TERMINAL&&(state.getValue(FrameBlock.PANEL)&1)==0) add(list,pos,facing.rotateYCCW(),0,16,0,16);
        }
        return list;
    }
    public static boolean touches(IBlockState a,BlockPos pa,IBlockState b,BlockPos pb,EnumFacing direction) {
        for(Run x:of(a,pa)) for(Run y:of(b,pb)) if(x.touches(y,direction)) return true;
        return false;
    }
}
