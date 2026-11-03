package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.*;

/** Window geometry in pixels. UVs remain in the closed sash's coordinates. */
public final class WindowGeometry {
    public static final int UP=1,DOWN=2,LEFT=4,RIGHT=8;
    public enum Material { WOOD,DAUB,LEAD,GLASS }
    public static final class Face {
        public final Material material;
        /** x,y,z,u,v; positions relative to the cell being rendered. */
        public final double[][] vertices;
        Face(Material material,double[][] vertices) {this.material=material;this.vertices=vertices;}
    }
    private WindowGeometry() { }
    private static void box(List<Part> out,double x,double y,double z,double xx,double yy,double zz,Grain grain) {WallGeometry.box(out,x,y,z,xx,yy,zz,grain);}
    private static List<Part> fixed(boolean thin,int joins) {
        List<Part> result=new ArrayList<>();double depth=thin?8:16;
        for(double x:new double[]{0,14.5}) {
            box(result,x,0,0,x+1.5,16,3,Grain.VERTICAL);
            box(result,x,0,3,x+1.5,16,depth-1,Grain.DAUB);
            box(result,x,0,depth-1,x+1.5,16,depth,Grain.VERTICAL);
        }
        if((joins&DOWN)==0) box(result,1.5,0,0,14.5,1.5,depth,Grain.EAST_WEST);
        if((joins&UP)==0) box(result,1.5,14.5,0,14.5,16,depth,Grain.EAST_WEST);
        return result;
    }
    private static List<Part> sash(int joins) {
        List<Part> result=new ArrayList<>();double b=(joins&DOWN)!=0?0:1.5,t=(joins&UP)!=0?16:14.5;
        box(result,1.5,b,1.7,2.25,t,2.4,Grain.VERTICAL);
        box(result,13.75,b,1.7,14.5,t,2.4,Grain.VERTICAL);
        if((joins&DOWN)==0) box(result,2.25,b,1.7,13.75,b+.75,2.4,Grain.EAST_WEST);
        if((joins&UP)==0) box(result,2.25,t-.75,1.7,13.75,t,2.4,Grain.EAST_WEST);
        return result;
    }
    private static double[] move(double[] p,boolean open,boolean right,EnumFacing facing,boolean back) {
        double x=p[0],z=p[2];
        if(open) {double hinge=right?14.5:1.5;x=hinge+(right?-1:1)*(p[2]-2);z=2+(right?1:-1)*(p[0]-hinge);}
        return WallGeometry.rotate(x,p[1],z+(back?8:0),facing);
    }
    private static double[][] clip(double[][] input,int axis,double edge,boolean greater) {
        if(input.length==0) return input;
        List<double[]> out=new ArrayList<>();double[] previous=input[input.length-1];
        double before=(previous[axis]-edge)*(greater?1:-1);
        for(double[] current:input) {
            double after=(current[axis]-edge)*(greater?1:-1);
            boolean a=before>=-1e-9,b=after>=-1e-9;
            if(a!=b) {double t=Math.max(0,Math.min(1,-before/(after-before)));double[] q=new double[current.length];for(int i=0;i<q.length;i++) q[i]=previous[i]+t*(current[i]-previous[i]);out.add(q);}
            if(b) out.add(current);previous=current;before=after;
        }
        List<double[]> clean=new ArrayList<>();
        for(double[] p:out) if(clean.isEmpty()||distance(p,clean.get(clean.size()-1))>1e-8) clean.add(p);
        if(clean.size()>1&&distance(clean.get(0),clean.get(clean.size()-1))<1e-8) clean.remove(clean.size()-1);
        return clean.toArray(new double[clean.size()][]);
    }
    private static double distance(double[] a,double[] b) {double result=0;for(int i=0;i<Math.min(3,a.length);i++) result+=Math.abs(a[i]-b[i]);return result;}
    private static void emit(List<Face> out,List<Part> parts,Material material,boolean moving,boolean open,boolean right,EnumFacing facing,boolean proxy,boolean back) {
        for(SurfaceGeometry.Face face:SurfaceGeometry.faces(parts,EnumFacing.NORTH)) {
            double[][] polygon=new double[face.points.length][];
            for(int i=0;i<polygon.length;i++) {
                double[] p=face.points[i],v=move(p,moving&&open,right,facing,back);
                double u=face.part.grain==Grain.EAST_WEST?p[1]:p[0],vv=face.part.grain==Grain.EAST_WEST?p[0]:16-p[1];
                if(material==Material.GLASS) {u=2+p[0]*.75;vv=2+(16-p[1])*.75;}
                if(material==Material.LEAD) {u=8;vv=8;}
                polygon[i]=new double[]{v[0]-(proxy?facing.getFrontOffsetX()*16:0),v[1],v[2]-(proxy?facing.getFrontOffsetZ()*16:0),u,vv};
            }
            for(int axis=0;axis<3;axis++) {polygon=clip(polygon,axis,0,true);polygon=clip(polygon,axis,16,false);}
            if(SurfaceGeometry.area(polygon)>1e-7) out.add(new Face(material==Material.WOOD&&face.part.grain==Grain.DAUB?Material.DAUB:material,polygon));
        }
    }
    private static List<Part> glass(int joins,boolean pane) {
        List<Part> result=new ArrayList<>();double b=pane?1:(joins&DOWN)!=0?0:2.25,t=pane?15:(joins&UP)!=0?16:13.75;
        box(result,pane?1:2.25,b,2.02,pane?15:13.75,t,2.10,Grain.DAUB);return result;
    }
    private static List<Part> leading(boolean diamond,int joins,boolean pane) {
        List<Part> result=new ArrayList<>();double low=pane?1:2.25,high=pane?15:13.75,b=pane?1:(joins&DOWN)!=0?0:2.25,t=pane?15:(joins&UP)!=0?16:13.75;
        for(int orientation=0;orientation<2;orientation++) for(int line=-32;line<=48;line+=4) {
            double[][] poly;
            if(diamond) {
                double sign=orientation==0?1:-1;
                poly=new double[][]{{-32,sign*-32+line-.15},{48,sign*48+line-.15},{48,sign*48+line+.15},{-32,sign*-32+line+.15}};
            } else if(orientation==0) poly=new double[][]{{line-.15,-32},{line+.15,-32},{line+.15,48},{line-.15,48}};
            else poly=new double[][]{{-32,line-.15},{48,line-.15},{48,line+.15},{-32,line+.15}};
            poly=clip(clip(clip(clip(poly,0,low,true),0,high,false),1,b,true),1,t,false);
            if(poly.length>=3) for(boolean rear:new boolean[]{false,true}) {
                double z=rear?2.10+orientation*.04:1.90-orientation*.04;
                result.add(new Part(poly,z,z+.07,Grain.DAUB));
            }
        }
        return result;
    }
    public static List<Face> faces(boolean thin,boolean diamond,boolean right,boolean open,EnumFacing facing,int joins,boolean proxy,boolean back) {
        List<Face> out=new ArrayList<>();
        if(!proxy) emit(out,fixed(thin,joins),Material.WOOD,false,open,right,facing,false,back);
        emit(out,sash(joins),Material.WOOD,true,open,right,facing,proxy,back);
        emit(out,glass(joins,false),Material.GLASS,true,open,right,facing,proxy,back);
        emit(out,leading(diamond,joins,false),Material.LEAD,true,open,right,facing,proxy,back);
        return out;
    }
    public static List<Face> faces(boolean thin,boolean diamond,boolean right,boolean open,EnumFacing facing,int joins,boolean proxy) {return faces(thin,diamond,right,open,facing,joins,proxy,false);}
    public static List<AxisAlignedBB> boxes(boolean thin,boolean right,boolean open,EnumFacing facing,int joins,boolean proxy) {return boxes(thin,right,open,facing,joins,proxy,false);}
    public static AxisAlignedBB sweep(boolean right,EnumFacing facing) {return sweep(right,facing,false);}
    public static List<Face> paneFaces(boolean diamond) {
        List<Face> out=new ArrayList<>();emit(out,glass(0,true),Material.GLASS,false,false,false,EnumFacing.NORTH,false,false);
        emit(out,leading(diamond,0,true),Material.LEAD,false,false,false,EnumFacing.NORTH,false,false);return out;
    }
    private static AxisAlignedBB transform(AxisAlignedBB box,boolean open,boolean right,EnumFacing facing,boolean back) {
        double[] bounds={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(double x:new double[]{box.minX,box.maxX}) for(double y:new double[]{box.minY,box.maxY}) for(double z:new double[]{box.minZ,box.maxZ}) {
            double[] p=move(new double[]{x*16,y*16,z*16},open,right,facing,back);for(int i=0;i<3;i++) {bounds[i]=Math.min(bounds[i],p[i]/16);bounds[i+3]=Math.max(bounds[i+3],p[i]/16);}
        }
        return new AxisAlignedBB(bounds[0],bounds[1],bounds[2],bounds[3],bounds[4],bounds[5]);
    }
    public static List<AxisAlignedBB> boxes(boolean thin,boolean right,boolean open,EnumFacing facing,int joins,boolean proxy,boolean back) {
        List<AxisAlignedBB> result=new ArrayList<>();
        if(!proxy) for(Part part:fixed(thin,joins)) result.add(transform(part.bounds(EnumFacing.NORTH),false,right,facing,back));
        double b=(joins&DOWN)!=0?0:1.5,t=(joins&UP)!=0?16:14.5;
        AxisAlignedBB leaf=transform(new AxisAlignedBB(1.5/16,b/16,1.7/16,14.5/16,t/16,2.4/16),open,right,facing,back);
        if(proxy) leaf=leaf.offset(-facing.getFrontOffsetX(),0,-facing.getFrontOffsetZ());
        double x=Math.max(0,leaf.minX),y=Math.max(0,leaf.minY),z=Math.max(0,leaf.minZ),xx=Math.min(1,leaf.maxX),yy=Math.min(1,leaf.maxY),zz=Math.min(1,leaf.maxZ);
        if(xx>x+1e-8&&yy>y+1e-8&&zz>z+1e-8) result.add(new AxisAlignedBB(x,y,z,xx,yy,zz));
        return result;
    }
    public static AxisAlignedBB sweep(boolean right,EnumFacing facing,boolean back) {
        AxisAlignedBB closed=transform(new AxisAlignedBB(1.5/16,0,1.7/16,14.5/16,1,2.4/16),false,right,facing,back);
        return closed.union(transform(new AxisAlignedBB(1.5/16,0,1.7/16,14.5/16,1,2.4/16),true,right,facing,back));
    }
}
