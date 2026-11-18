package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;

/** Reusable clipped prism kit. No world access, house meshes or dependency on client classes. */
public final class RoofMeshes {
    private static final double EPS=1e-8;
    private static final Map<Key,Mesh> CACHE=Collections.synchronizedMap(new LinkedHashMap<Key,Mesh>(256,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<Key,Mesh> e){return size()>4096;}});
    public static final Mesh EMPTY=new Mesh(Collections.emptyList(),Collections.emptyList());
    private RoofMeshes(){}
    public static final class Key {
        final int[] wood,skin;final int cover;final int hash;
        public Key(RoofCell cell,int cover){wood=cell.timber;skin=cell.skin;this.cover=cover;hash=31*(31*Arrays.hashCode(wood)+Arrays.hashCode(skin))+cover;}
        @Override public int hashCode(){return hash;}
        @Override public boolean equals(Object o){if(!(o instanceof Key))return false;Key k=(Key)o;return cover==k.cover&&Arrays.equals(wood,k.wood)&&Arrays.equals(skin,k.skin);}
    }
    public static final class Face {
        public final double[][] vertices;public final double[] normal;public final boolean skin;
        Face(List<double[]> v,boolean skin){vertices=v.toArray(new double[0][]);this.skin=skin;double[] n=cross(vertices[0],vertices[1],vertices[2]);double d=Math.sqrt(dot(n,n));normal=new double[]{n[0]/d,n[1]/d,n[2]/d};}
    }
    public static final class Mesh {
        public final List<Face> faces;public final List<AxisAlignedBB> boxes;public final AxisAlignedBB bounds;
        Mesh(List<Face> faces,List<AxisAlignedBB> boxes){this.faces=Collections.unmodifiableList(faces);this.boxes=Collections.unmodifiableList(boxes);AxisAlignedBB b=null;for(AxisAlignedBB a:boxes)b=b==null?a:b.union(a);bounds=b==null?new AxisAlignedBB(0,0,0,0,0,0):b;}
        public RayTraceResult trace(BlockPos at,Vec3d from,Vec3d to){double best=2;Face hit=null;double[] o={from.xCoord-at.getX(),from.yCoord-at.getY(),from.zCoord-at.getZ()},d={to.xCoord-from.xCoord,to.yCoord-from.yCoord,to.zCoord-from.zCoord};for(Face f:faces)for(int i=1;i<f.vertices.length-1;i++){double t=triangle(o,d,f.vertices[0],f.vertices[i],f.vertices[i+1]);if(t>=0&&t<=1&&t<best){best=t;hit=f;}}return hit==null?null:new RayTraceResult(from.addVector(d[0]*best,d[1]*best,d[2]*best),EnumFacing.getFacingFromVector((float)hit.normal[0],(float)hit.normal[1],(float)hit.normal[2]),at);}
    }
    public static Mesh get(RoofCell cell,int cover){if(cell==null)return EMPTY;Key key=new Key(cell,cover);Mesh mesh=CACHE.get(key);if(mesh!=null)return mesh;List<Face> faces=new ArrayList<>();List<AxisAlignedBB> boxes=new ArrayList<>();pieces(cell.timber,false,1,faces,boxes);if(cover>0)pieces(cell.skin,true,cover==1?1:.25,faces,boxes);mesh=faces.isEmpty()?EMPTY:new Mesh(faces,boxes);CACHE.put(key,mesh);return mesh;}
    public static void prepare(RoofCell cell){for(int c=0;c<4;c++)get(cell,c);}
    public static int cached(){return CACHE.size();}
    private static void pieces(int[] data,boolean skin,double scale,List<Face> faces,List<AxisAlignedBB> boxes){for(int i=0;i<data.length;i+=8){double a=data[i]/32.0,b=data[i+1]/32.0,c=data[i+2]/32.0,d=data[i+3]/32.0,h=data[i+4]/32.0,j=data[i+5]/32.0,depth=data[i+6]/32.0*scale;boolean axis=data[i+7]!=0;double low=Math.min(0,depth),high=Math.max(0,depth);
        double[][] p={{a,h+low,c},{b,j+low,c},{b,j+low,d},{a,h+low,d},{a,h+high,c},{b,j+high,c},{b,j+high,d},{a,h+high,d}};
        List<List<double[]>> polys=new ArrayList<>();for(int[] f:new int[][]{{0,1,2,3},{4,7,6,5},{0,4,5,1},{3,2,6,7},{0,3,7,4},{1,5,6,2}}){List<double[]> v=new ArrayList<>();for(int n:f){double[] q=p[n];v.add(axis?new double[]{q[2],q[1],q[0]}:q.clone());}if(axis)Collections.reverse(v);polys.add(v);}polys=clip(polys,0,true);polys=clip(polys,1,false);for(List<double[]> v:polys)add(faces,v,skin);
        // At most 32 slices across a steep slope: conservative error is <= 1/16.
        for(int u=0;u<32;u++){double x0=Math.max(a,u/32.0),x1=Math.min(b,(u+1)/32.0);if(x1<=x0+EPS)continue;double y0=h+(j-h)*(x0-a)/(b-a),y1=h+(j-h)*(x1-a)/(b-a),bottom=Math.max(0,Math.min(y0,y1)+low),top=Math.min(1,Math.max(y0,y1)+high);if(top<=bottom+EPS)continue;boxes.add(axis?new AxisAlignedBB(c,bottom,x0,d,top,x1):new AxisAlignedBB(x0,bottom,c,x1,top,d));}
    }}
    private static List<List<double[]>> clip(List<List<double[]>> polys,double y,boolean above){List<List<double[]>> out=new ArrayList<>();List<double[]> cap=new ArrayList<>();for(List<double[]> polygon:polys){List<double[]> v=new ArrayList<>();double[] a=polygon.get(polygon.size()-1);boolean ai=above?a[1]>=y-EPS:a[1]<=y+EPS;for(double[] b:polygon){boolean bi=above?b[1]>=y-EPS:b[1]<=y+EPS;if(ai!=bi){double t=(y-a[1])/(b[1]-a[1]);double[] q={a[0]+t*(b[0]-a[0]),y,a[2]+t*(b[2]-a[2])};v.add(q);boolean seen=false;for(double[] n:cap)if(distance(n,q)<EPS)seen=true;if(!seen)cap.add(q);}if(bi)v.add(b);a=b;ai=bi;}if(v.size()>=3)out.add(v);}if(cap.size()>=3){double x=0,z=0;for(double[] q:cap){x+=q[0];z+=q[2];}final double cx=x/cap.size(),cz=z/cap.size();cap.sort(Comparator.comparingDouble(q->Math.atan2(q[2]-cz,q[0]-cx)));if(!above)Collections.reverse(cap);out.add(cap);}return out;}
    private static void add(List<Face> faces,List<double[]> v,boolean skin){List<double[]> p=new ArrayList<>();for(double[] q:v)if(p.isEmpty()||distance(p.get(p.size()-1),q)>EPS)p.add(q);if(p.size()>1&&distance(p.get(0),p.get(p.size()-1))<EPS)p.remove(p.size()-1);boolean change=true;while(change&&p.size()>=3){change=false;for(int i=0;i<p.size();i++){double[] n=cross(p.get((i+p.size()-1)%p.size()),p.get(i),p.get((i+1)%p.size()));if(dot(n,n)<EPS*EPS){p.remove(i);change=true;break;}}}if(p.size()<3)return;if(p.size()<=4)faces.add(new Face(p,skin));else for(int i=1;i<p.size()-1;i++)faces.add(new Face(Arrays.asList(p.get(0),p.get(i),p.get(i+1)),skin));}
    private static double distance(double[] a,double[] b){double x=a[0]-b[0],y=a[1]-b[1],z=a[2]-b[2];return x*x+y*y+z*z;}
    private static double[] cross(double[] a,double[] b,double[] c){return new double[]{(b[1]-a[1])*(c[2]-a[2])-(b[2]-a[2])*(c[1]-a[1]),(b[2]-a[2])*(c[0]-a[0])-(b[0]-a[0])*(c[2]-a[2]),(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])};}
    private static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    private static double triangle(double[] o,double[] d,double[] a,double[] b,double[] c){double[] e={b[0]-a[0],b[1]-a[1],b[2]-a[2]},f={c[0]-a[0],c[1]-a[1],c[2]-a[2]},p={d[1]*f[2]-d[2]*f[1],d[2]*f[0]-d[0]*f[2],d[0]*f[1]-d[1]*f[0]};double det=dot(e,p);if(Math.abs(det)<EPS)return -1;double[] t={o[0]-a[0],o[1]-a[1],o[2]-a[2]};double u=dot(t,p)/det;if(u< -EPS||u>1+EPS)return -1;double[] q={t[1]*e[2]-t[2]*e[1],t[2]*e[0]-t[0]*e[2],t[0]*e[1]-t[1]*e[0]};double v=dot(d,q)/det;if(v< -EPS||u+v>1+EPS)return -1;return dot(f,q)/det;}
}
