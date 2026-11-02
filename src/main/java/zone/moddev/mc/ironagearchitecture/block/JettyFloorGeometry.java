package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.util.EnumFacing;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.*;

/** Floor boards, cropped joists, perimeter beams and attached brackets share one mesh. */
public final class JettyFloorGeometry {
    private JettyFloorGeometry() { }
    public static boolean exposed(FloorShape shape,int q,EnumFacing side,int joins) {
        int x=q%2+side.getFrontOffsetX(),z=q/2+side.getFrontOffsetZ();
        if(x>=0&&x<2&&z>=0&&z<2&&(shape.mask&(1<<(x+2*z)))!=0) return false;
        return (joins&(1<<(q*4+side.getHorizontalIndex())))==0;
    }
    public static List<Part> parts(FloorShape shape,boolean alongX,int joins,EnumFacing bracket,boolean fullReach) {
        List<Part> result=new ArrayList<>();
        for(int q=0;q<4;q++) if((shape.mask&(1<<q))!=0) {
            int x=q%2*8,z=q/2*8;
            boolean n=exposed(shape,q,EnumFacing.NORTH,joins),s=exposed(shape,q,EnumFacing.SOUTH,joins),
                w=exposed(shape,q,EnumFacing.WEST,joins),e=exposed(shape,q,EnumFacing.EAST,joins);
            WallGeometry.box(result,x,14,z,x+8,16,z+8,alongX?Grain.BOARDS_Z:Grain.BOARDS_X);
            if(n) WallGeometry.box(result,x,8,z,x+8,14,z+3,Grain.EAST_WEST);
            if(s) WallGeometry.box(result,x,8,z+5,x+8,14,z+8,Grain.EAST_WEST);
            if(w) WallGeometry.box(result,x,8,z+(n?3:0),x+3,14,z+8-(s?3:0),Grain.NORTH_SOUTH);
            if(e) WallGeometry.box(result,x+5,8,z+(n?3:0),x+8,14,z+8-(s?3:0),Grain.NORTH_SOUTH);
            if(alongX) WallGeometry.box(result,x+(w?3:0),8,z+3,x+8-(e?3:0),14,z+5,Grain.EAST_WEST);
            else WallGeometry.box(result,x+3,8,z+(n?3:0),x+5,14,z+8-(s?3:0),Grain.NORTH_SOUTH);
            // A corner tie is below the joists, avoiding coplanar timber faces.
            if((n||s)&&(e||w)) {
                boolean falling=n&&e||s&&w;
                double[][] outline={{x,z+(falling?8:0)},{x+1.25,z+(falling?8:0)},
                    {x+8,z+(falling?1.25:6.75)},{x+8,z+(falling?0:8)}, {x+6.75,z+(falling?0:8)},{x,z+(falling?6.75:1.25)}};
                if(falling) { // Reverse winding after the Z reflection.
                    for(int i=0;i<outline.length/2;i++){double[] p=outline[i];outline[i]=outline[outline.length-1-i];outline[outline.length-1-i]=p;}
                }
                result.add(new Part(outline,8.015,10,falling?Grain.FALLING:Grain.RISING,EnumFacing.NORTH,true));
            }
        }
        if(bracket!=null) {
            double[] anchor=bracketAnchor(shape,bracket,fullReach);
            if(anchor==null) throw new IllegalArgumentException("Bracket outside floor footprint");
            double reach=fullReach?16:8;
            // Build a 2-pixel diagonal web along X, then turn it towards the selected edge.
            EnumFacing local=bracket==EnumFacing.NORTH?EnumFacing.EAST:bracket==EnumFacing.SOUTH?EnumFacing.WEST:
                bracket==EnumFacing.WEST?EnumFacing.NORTH:EnumFacing.SOUTH;
            // Transform into block coordinates before extrusion, retaining a convex sloping web.
            double tangent=anchor[1],edge=anchor[0];
            double[][] points=new double[][]{{edge,6},{edge,8},{edge+reach,2},{edge+reach,0},{edge+reach-2,0},{edge,6}};
            // local web x increases inward from the edge; localFacing places it on the chosen side.
            result.add(new Part(points,tangent-1.5,tangent+1.5,Grain.FALLING,local));
        }
        return Collections.unmodifiableList(result);
    }
    /** Anchor in coordinates whose X axis runs inward from the selected perimeter edge. */
    public static double[] bracketAnchor(FloorShape shape,EnumFacing side,boolean full) {
        EnumFacing local=side==EnumFacing.NORTH?EnumFacing.EAST:side==EnumFacing.SOUTH?EnumFacing.WEST:side==EnumFacing.WEST?EnumFacing.NORTH:EnumFacing.SOUTH;
        double reach=full?16:8;
        for(double tangent:new double[]{8,4,12}) for(double edge:new double[]{0,8}) {
            boolean fits=true;
            for(double d:new double[]{.1,reach-.1}) for(double t:new double[]{tangent-1.49,tangent+1.49}) {
                double[] p=WallGeometry.rotate(edge+d,0,t,local);
                if(!shape.contains(p[0],p[2])) fits=false;
            }
            double[] outside=WallGeometry.rotate(edge-.1,0,tangent,local);
            if(fits&&!shape.contains(outside[0],outside[2])) return new double[]{edge,tangent};
        }
        return null;
    }
}
