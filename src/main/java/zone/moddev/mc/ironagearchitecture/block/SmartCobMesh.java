package zone.moddev.mc.ironagearchitecture.block;

import java.awt.geom.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;

/** Shared closed skin, cap subtraction and exact triangle selection. */
public final class SmartCobMesh {
    private static final Map<SmartCobView,List<Face>> CACHE=new ConcurrentHashMap<>();
    private SmartCobMesh() { }
    public static final class Face {
        public final double[][] points,normals,uv;
        Face(double[][] points,double[][] normals,double[][] uv){this.points=points;this.normals=normals;this.uv=uv;}
    }
    public static List<Face> faces(SmartCobView view) {
        List<Face> found=CACHE.get(view);if(found!=null)return found;
        List<Face> result=new ArrayList<>();
        for(List<double[]> loop:view.profile.loops()) for(int i=0;i<loop.size();i++) {
            double[] a=loop.get(i),b=loop.get((i+1)%loop.size());if(a[2]==0)continue;
            double length=Math.hypot(b[0]-a[0],b[1]-a[1]);if(length<1e-7)continue;
            double nx=(b[1]-a[1])/length,nz=-(b[0]-a[0])/length;
            double[] na=a[3]*nx+a[4]*nz>.9?new double[]{a[3],0,a[4]}:new double[]{nx,0,nz};
            double[] nb=b[3]*nx+b[4]*nz>.9?new double[]{b[3],0,b[4]}:new double[]{nx,0,nz};
            double ua=a[5],ub=b[5];if(ub<ua-1e-6)ub++;if(Math.abs(ua-ub)<.001&&length>.9)ub=ua+1;
            // A newly cut reveal can meet the outer contour's independent phase.
            if(Math.abs((ub-ua)-length)>length*.18+.001){ua=0;ub=length;}
            if(ub>1&&ua<1) {
                double t=(1-ua)/(ub-ua);double[] m={a[0]+t*(b[0]-a[0]),a[1]+t*(b[1]-a[1])};
                double[] nm={na[0]+t*(nb[0]-na[0]),0,na[2]+t*(nb[2]-na[2])};double n=Math.hypot(nm[0],nm[2]);nm[0]/=n;nm[2]/=n;
                side(result,a,m,na,nm,ua,1);side(result,m,b,nm,nb,0,ub-1);
            } else side(result,a,b,na,nb,ua,ub);
        }
        if(!view.topSolid) {Area cap=view.profile.area();if(view.above!=null)cap.subtract(view.above.area());caps(result,cap,true);}
        if(!view.bottomSolid) {Area cap=view.profile.area();if(view.below!=null)cap.subtract(view.below.area());caps(result,cap,false);}
        found=Collections.unmodifiableList(result);if(CACHE.size()>256)CACHE.clear();CACHE.put(view,found);return found;
    }
    private static void side(List<Face> out,double[] a,double[] b,double[] na,double[] nb,double ua,double ub) {
        out.add(new Face(new double[][]{{a[0],0,a[1]},{a[0],1,a[1]},{b[0],1,b[1]},{b[0],0,b[1]}},
            new double[][]{na,na,nb,nb},new double[][]{{ua,1},{ua,0},{ub,0},{ub,1}}));
    }
    private static void caps(List<Face> out,Area shape,boolean top) {
        List<List<double[]>> loops=SmartCobGeometry.loops(shape);TreeSet<Double> heights=new TreeSet<>();
        for(List<double[]> loop:loops)for(double[] p:loop)heights.add(p[1]);
        Double previous=null;
        for(double z:heights) {
            if(previous==null){previous=z;continue;}double low=previous,mid=(low+z)/2;previous=z;if(z-low<1e-7)continue;
            List<double[][]> edges=new ArrayList<>();
            for(List<double[]> loop:loops)for(int i=0;i<loop.size();i++) {double[] a=loop.get(i),b=loop.get((i+1)%loop.size());if(Math.min(a[1],b[1])<mid&&Math.max(a[1],b[1])>mid)edges.add(new double[][]{a,b});}
            edges.sort(Comparator.comparingDouble(e->at(e,mid)));
            for(int i=0;i+1<edges.size();i+=2) {
                double y=top?1:0;double[][] p={{at(edges.get(i),low),y,low},{at(edges.get(i+1),low),y,low},{at(edges.get(i+1),z),y,z},{at(edges.get(i),z),y,z}};
                if(top){double[] swap=p[1];p[1]=p[3];p[3]=swap;}
                double[][] normals=new double[4][],uv=new double[4][];
                for(int v=0;v<4;v++){normals[v]=new double[]{0,top?1:-1,0};uv[v]=new double[]{p[v][0],p[v][2]};}
                out.add(new Face(p,normals,uv));
            }
        }
    }
    private static double at(double[][] edge,double z){return edge[0][0]+(edge[1][0]-edge[0][0])*(z-edge[0][1])/(edge[1][1]-edge[0][1]);}
    public static RayTraceResult trace(SmartCobView view,BlockPos pos,Vec3d start,Vec3d end) {
        double[] origin={start.xCoord-pos.getX(),start.yCoord-pos.getY(),start.zCoord-pos.getZ()};
        double[] direction={end.xCoord-start.xCoord,end.yCoord-start.yCoord,end.zCoord-start.zCoord};double nearest=2;Face selected=null;
        for(Face face:faces(view)) for(int i=1;i<3;i++) {
            double t=triangle(origin,direction,face.points[0],face.points[i],face.points[i+1]);if(t>=0&&t<=1&&t<nearest){nearest=t;selected=face;}
        }
        if(selected==null)return null;double[] n=selected.normals[0];
        return new RayTraceResult(new Vec3d(start.xCoord+direction[0]*nearest,start.yCoord+direction[1]*nearest,start.zCoord+direction[2]*nearest),EnumFacing.getFacingFromVector((float)n[0],(float)n[1],(float)n[2]),pos);
    }
    private static double triangle(double[] o,double[] d,double[] a,double[] b,double[] c) {
        double[] e1={b[0]-a[0],b[1]-a[1],b[2]-a[2]},e2={c[0]-a[0],c[1]-a[1],c[2]-a[2]};
        double[] p=cross(d,e2);double det=dot(e1,p);if(Math.abs(det)<1e-10)return -1;
        double[] s={o[0]-a[0],o[1]-a[1],o[2]-a[2]};double u=dot(s,p)/det;if(u<-1e-8||u>1.00000001)return -1;
        double[] q=cross(s,e1);double v=dot(d,q)/det;if(v<-1e-8||u+v>1.00000001)return -1;return dot(e2,q)/det;
    }
    private static double[] cross(double[] a,double[] b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
    private static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
}
