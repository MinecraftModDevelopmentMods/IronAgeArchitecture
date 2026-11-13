package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.*;

/** Remove buried faces and assign one owner to coplanar timber intersections. */
public final class SurfaceGeometry {
    private static final double EPS=1e-8;
    private SurfaceGeometry() { }
    public static final class Face {
        public final Part part;
        public final double[][] points;
        Face(Part part,double[][] points) {this.part=part;this.points=points;}
    }
    private static boolean box(Part part) {
        if(part.outline.length!=4) return false;
        for(int i=0;i<4;i++) {
            double[] a=part.outline[i],b=part.outline[(i+1)%4];
            if(Math.abs(a[0]-b[0])>EPS&&Math.abs(a[1]-b[1])>EPS) return false;
        }
        return true;
    }
    private static int priority(Part part) {
        switch(part.grain) {
            case BOARDS_X:case BOARDS_Z:return 5;
            case VERTICAL:return 4;
            case EAST_WEST:case NORTH_SOUTH:return 3;
            case RISING:case FALLING:return 2;
            default:return 0;
        }
    }
    private static double[] point(Part part,int index,boolean back) {return new double[]{part.outline[index][0],part.outline[index][1],back?part.back:part.front};}
    private static List<double[][]> raw(Part part) {
        int n=part.outline.length;List<double[][]> faces=new ArrayList<>();
        if(n<3) return faces;
        double[][] front=new double[n][],back=new double[n][];
        for(int i=0;i<n;i++) {front[i]=point(part,(n-i)%n,false);back[i]=point(part,i,true);}
        faces.add(clean(Arrays.asList(front)));faces.add(clean(Arrays.asList(back)));
        for(int i=0;i<n;i++) faces.add(new double[][]{point(part,i,false),point(part,(i+1)%n,false),point(part,(i+1)%n,true),point(part,i,true)});
        return faces;
    }
    private static double[] localBox(Part owner,AxisAlignedBB bounds,EnumFacing facing) {
        double[] box={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(double x:new double[]{bounds.minX,bounds.maxX}) for(double y:new double[]{bounds.minY,bounds.maxY}) for(double z:new double[]{bounds.minZ,bounds.maxZ}) {
            double[] p=owner.inverse(x*16,y*16,z*16,facing);
            for(int axis=0;axis<3;axis++) {box[axis]=Math.min(box[axis],p[axis]);box[axis+3]=Math.max(box[axis+3],p[axis]);}
        }
        return box;
    }
    private static double[] normal(double[][] points) {
        for(int i=1;i<points.length-1;i++) {
            double[] a=points[0],b=points[i],c=points[i+1];
            double[] n={(b[1]-a[1])*(c[2]-a[2])-(b[2]-a[2])*(c[1]-a[1]),
                (b[2]-a[2])*(c[0]-a[0])-(b[0]-a[0])*(c[2]-a[2]),
                (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])};
            double length=Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);
            if(length>EPS) {for(int j=0;j<3;j++) n[j]/=length;return n;}
        }
        return new double[3];
    }
    private static double[][] clean(List<double[]> input) {
        List<double[]> result=new ArrayList<>();
        for(double[] point:input) if(result.isEmpty()||distance(point,result.get(result.size()-1))>EPS) result.add(point);
        if(result.size()>1&&distance(result.get(0),result.get(result.size()-1))<EPS) result.remove(result.size()-1);
        return result.toArray(new double[result.size()][]);
    }
    private static double distance(double[] a,double[] b) {return Math.abs(a[0]-b[0])+Math.abs(a[1]-b[1])+Math.abs(a[2]-b[2]);}
    private static double[][] clip(double[][] polygon,int axis,double edge,boolean positive,boolean inside) {
        if(polygon.length==0) return polygon;
        List<double[]> out=new ArrayList<>();double[] previous=polygon[polygon.length-1];
        double before=(previous[axis]-edge)*(positive?1:-1);
        boolean was=inside?before>=-EPS:before<-EPS;
        for(double[] current:polygon) {
            double after=(current[axis]-edge)*(positive?1:-1);boolean now=inside?after>=-EPS:after<-EPS;
            if(was!=now) {
                double t=(-EPS-before)/(after-before);double[] cut=new double[3];
                for(int i=0;i<3;i++) cut[i]=previous[i]+t*(current[i]-previous[i]);out.add(cut);
            }
            if(now) out.add(current);
            previous=current;before=after;was=now;
        }
        return clean(out);
    }
    public static double area(double[][] polygon) {
        if(polygon.length<3) return 0;
        double[] sum=new double[3];
        for(int i=0;i<polygon.length;i++) {
            double[] a=polygon[i],b=polygon[(i+1)%polygon.length];
            sum[0]+=a[1]*b[2]-a[2]*b[1];sum[1]+=a[2]*b[0]-a[0]*b[2];sum[2]+=a[0]*b[1]-a[1]*b[0];
        }
        return Math.sqrt(sum[0]*sum[0]+sum[1]*sum[1]+sum[2]*sum[2])/2;
    }
    private static List<double[][]> subtract(double[][] face,double[] box,boolean ownerWins) {
        for(int axis=0;axis<3;axis++) {
            double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
            for(double[] p:face) {min=Math.min(min,p[axis]);max=Math.max(max,p[axis]);}
            if(max<box[axis]-EPS||min>box[axis+3]+EPS) return Collections.singletonList(face);
        }
        double[] normal=normal(face);
        if(ownerWins) for(int axis=0;axis<3;axis++) {
            double edge=normal[axis]>0?box[axis+3]:box[axis];
            if(Math.abs(normal[axis])>.999&&Math.abs(face[0][axis]-edge)<EPS) return Collections.singletonList(face);
        }
        List<double[][]> visible=new ArrayList<>();double[][] remaining=face;
        for(int plane=0;plane<6&&remaining.length>=3;plane++) {
            int axis=plane%3;boolean positive=plane<3;double edge=box[axis+(positive?0:3)];
            double[][] outside=clip(remaining,axis,edge,positive,false);
            if(area(outside)>1e-6) visible.add(outside);
            remaining=clip(remaining,axis,edge,positive,true);
        }
        return visible;
    }
    private static double dot(double[] a,double[] b) {return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    /** Convex cover planes in the owner's texture coordinates: outward normal and offset. */
    private static List<double[]> planes(Part owner,Part cover,EnumFacing facing) {
        double x=0,y=0;
        for(double[] p:cover.outline) {x+=p[0];y+=p[1];}
        double[] world=cover.transform(x/cover.outline.length,y/cover.outline.length,(cover.front+cover.back)/2,facing);
        double[] centre=owner.inverse(world[0],world[1],world[2],facing);
        List<double[]> planes=new ArrayList<>();
        for(double[][] face:raw(cover)) {
            double[][] local=new double[face.length][];
            for(int i=0;i<face.length;i++) {
                world=cover.transform(face[i][0],face[i][1],face[i][2],facing);
                local[i]=owner.inverse(world[0],world[1],world[2],facing);
            }
            if(area(local)<1e-6) continue;
            double[] n=normal(local);double edge=dot(n,local[0]);
            // Horizontal parts reflect the local axes, so derive the outside from the solid.
            if(dot(n,centre)>edge) {for(int axis=0;axis<3;axis++) n[axis]=-n[axis];edge=-edge;}
            planes.add(new double[]{n[0],n[1],n[2],edge});
        }
        return planes;
    }
    private static double[][] clip(double[][] polygon,double[] plane,boolean inside) {
        if(polygon.length==0) return polygon;
        List<double[]> out=new ArrayList<>();double[] previous=polygon[polygon.length-1];
        double before=plane[3]-dot(plane,previous);boolean was=inside?before>=-EPS:before<-EPS;
        for(double[] current:polygon) {
            double after=plane[3]-dot(plane,current);boolean now=inside?after>=-EPS:after<-EPS;
            if(was!=now) {
                double t=(-EPS-before)/(after-before);double[] cut=new double[3];
                for(int axis=0;axis<3;axis++) cut[axis]=previous[axis]+t*(current[axis]-previous[axis]);
                out.add(cut);
            }
            if(now) out.add(current);
            previous=current;before=after;was=now;
        }
        return clean(out);
    }
    private static List<double[][]> subtract(double[][] face,List<double[]> planes,boolean ownerWins) {
        double[] n=normal(face);
        for(double[] plane:planes) {
            boolean outside=true;
            for(double[] p:face) if(plane[3]-dot(plane,p)>=-EPS) {outside=false;break;}
            if(outside) return Collections.singletonList(face);
            if(ownerWins&&dot(n,plane)>.999999&&Math.abs(dot(plane,face[0])-plane[3])<EPS)
                return Collections.singletonList(face);
        }
        List<double[][]> visible=new ArrayList<>();double[][] remaining=face;
        for(double[] plane:planes) {
            if(remaining.length<3) break;
            double[][] outside=clip(remaining,plane,false);
            if(area(outside)>1e-6) visible.add(outside);
            remaining=clip(remaining,plane,true);
        }
        return visible;
    }
    public static List<Face> faces(List<Part> parts,EnumFacing facing) {
        List<Face> result=new ArrayList<>();
        for(int i=0;i<parts.size();i++) {
            Part owner=parts.get(i);List<double[][]> faces=raw(owner);
            for(int j=0;j<parts.size()&&!faces.isEmpty();j++) {
                if(i==j) continue;
                Part cover=parts.get(j);boolean rectangular=box(cover);
                double[] bounds=rectangular?localBox(owner,cover.bounds(facing),facing):null;
                List<double[]> planes=rectangular?null:planes(owner,cover,facing);
                boolean wins=priority(owner)>priority(cover)||priority(owner)==priority(cover)&&i<j;
                List<double[][]> next=new ArrayList<>();
                for(double[][] face:faces) if(area(face)>1e-6)
                    next.addAll(rectangular?subtract(face,bounds,wins):subtract(face,planes,wins));
                faces=next;
            }
            for(double[][] face:faces) if(area(face)>1e-6) result.add(new Face(owner,face));
        }
        return result;
    }
}
