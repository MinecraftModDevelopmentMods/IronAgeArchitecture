package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;

/** Fixed half-block-deep roof skins, split at the cell floor and baked once. */
public final class ThatchGeometry {
    public static final int STEPS=16;
    public static final double THICKNESS=.5;
    private static final double EPS=1e-9;
    public static final Mesh EMPTY=new Mesh(Collections.emptyList(),Collections.emptyList());
    private static final Mesh[] MAIN=new Mesh[ThatchShape.COUNT*4],BELOW=new Mesh[MAIN.length];
    static {for(int p=0;p<ThatchShape.COUNT;p++){Mesh[] pair=build(ThatchShape.of(p,EnumFacing.NORTH));for(EnumFacing facing:EnumFacing.HORIZONTALS){int code=ThatchShape.of(p,facing).code;MAIN[code]=rotate(pair[0],facing);BELOW[code]=rotate(pair[1],facing);}}}
    private ThatchGeometry(){}
    public static Mesh mesh(ThatchShape shape){return MAIN[shape.code];}
    public static Mesh underside(ThatchShape shape){return BELOW[shape.code];}
    public static final class Face {
        public final double[][] vertices;public final double[] normal;public final boolean surface;
        Face(double[][] points,boolean surface){vertices=points;this.surface=surface;double[] n=cross(points[0],points[1],points[2]);double len=Math.sqrt(dot(n,n));normal=new double[]{n[0]/len,n[1]/len,n[2]/len};}
    }
    public static final class Mesh {
        public final List<Face> faces;public final List<AxisAlignedBB> boxes;public final AxisAlignedBB bounds;
        Mesh(List<Face> faces,List<AxisAlignedBB> boxes){this.faces=Collections.unmodifiableList(faces);this.boxes=Collections.unmodifiableList(boxes);bounds=boxes.isEmpty()?new AxisAlignedBB(0,0,0,0,0,0):WindowBlock.union(boxes);}
        public RayTraceResult trace(BlockPos pos,Vec3d start,Vec3d end){double nearest=Double.POSITIVE_INFINITY;Face best=null;double[] o={start.xCoord-pos.getX(),start.yCoord-pos.getY(),start.zCoord-pos.getZ()},d={end.xCoord-start.xCoord,end.yCoord-start.yCoord,end.zCoord-start.zCoord};for(Face face:faces)for(int i=1;i+1<face.vertices.length;i++){double t=triangle(o,d,face.vertices[0],face.vertices[i],face.vertices[i+1]);if(t>=0&&t<=1&&t<nearest){nearest=t;best=face;}}if(best==null)return null;double[] n=best.normal;EnumFacing side=Math.abs(n[1])>=Math.max(Math.abs(n[0]),Math.abs(n[2]))?(n[1]>0?EnumFacing.UP:EnumFacing.DOWN):EnumFacing.getFacingFromVector((float)n[0],(float)n[1],(float)n[2]);return new RayTraceResult(start.addVector(d[0]*nearest,d[1]*nearest,d[2]*nearest),side,pos);}
    }
    private static Mesh[] build(ThatchShape shape){
        List<Face> whole=new ArrayList<>(),top=new ArrayList<>();boolean solid=shape.profile==ThatchShape.SOLID_PROFILE;double min=1,gradient=0;
        for(int i=0;i<8;i++){
            double[] a=ThatchShape.point(i),b=ThatchShape.point((i+1)&7);
            double[][] points={{.5,shape.centre(),.5},{b[0],shape.perimeter((i+1)&7),b[1]},{a[0],shape.perimeter(i),a[1]}};
            Face face=new Face(points,true);top.add(face);whole.add(face);gradient=Math.max(gradient,(Math.abs(face.normal[0])+Math.abs(face.normal[2]))/face.normal[1]);
            double[][] bottom=new double[3][];for(int j=0;j<3;j++){double[] v=points[2-j];bottom[j]=new double[]{v[0],solid?0:v[1]-THICKNESS,v[2]};min=Math.min(min,v[1]);}add(whole,false,Arrays.asList(bottom));
        }
        for(int i=0;i<8;i++){double[] a=ThatchShape.point(i),b=ThatchShape.point((i+1)&7);double ah=shape.perimeter(i),bh=shape.perimeter((i+1)&7);add(whole,false,Arrays.asList(new double[]{a[0],solid?0:ah-THICKNESS,a[1]},new double[]{a[0],ah,a[1]},new double[]{b[0],bh,b[1]},new double[]{b[0],solid?0:bh-THICKNESS,b[1]}));}
        int steps=gradient>1+EPS?32:STEPS;Mesh[] result=new Mesh[2];
        for(int part=0;part<2;part++){
            if(part==1&&(solid||min>=THICKNESS-EPS)){result[part]=EMPTY;continue;}
            List<Face> faces=new ArrayList<>();
            for(Face f:whole){if(onPlane(f,0)&&(part==1&&f.normal[1]<-EPS||part==0&&f.normal[1]>EPS))continue;List<double[]> polygon=clip(Arrays.asList(f.vertices),part==0?0:-1,true);polygon=clip(polygon,part==0?1:0,false);add(faces,f.surface,shift(polygon,part));}
            if(!solid)for(Face f:top){boolean cut=false;for(double[] v:f.vertices)if(v[1]<THICKNESS-EPS)cut=true;if(!cut)continue;List<double[]> cap=clip(Arrays.asList(f.vertices),THICKNESS,false);List<double[]> flat=new ArrayList<>();for(double[] v:cap)flat.add(new double[]{v[0],part,v[2]});if(part==0)Collections.reverse(flat);add(faces,false,flat);}
            merge(faces);faces=quads(faces);List<AxisAlignedBB> boxes=new ArrayList<>();
            for(int z=0;z<steps;z++)for(int x=0;x<steps;){int start=x;double[] span=span(shape,solid,part,x,z,steps);while(++x<steps&&sameSpan(span,span(shape,solid,part,x,z,steps))){}if(span==null)continue;boxes.add(new AxisAlignedBB(start/(double)steps,span[0],z/(double)steps,x/(double)steps,span[1],(z+1)/(double)steps));}
            result[part]=faces.isEmpty()?EMPTY:new Mesh(faces,boxes);
        }return result;
    }
    private static boolean onPlane(Face f,double y){for(double[] v:f.vertices)if(Math.abs(v[1]-y)>EPS)return false;return true;}
    private static double[] span(ThatchShape s,boolean solid,int part,int x,int z,int steps){double min=1,max=0;for(int a=0;a<2;a++)for(int b=0;b<2;b++){double h=s.height((x+a)/(double)steps,(z+b)/(double)steps);min=Math.min(min,h);max=Math.max(max,h);}if(part==1){if(min>=THICKNESS-EPS)return null;return new double[]{Math.max(0,1+min-THICKNESS),1};}if(max<=EPS)return null;return new double[]{solid?0:Math.max(0,min-THICKNESS),max};}
    private static boolean sameSpan(double[] a,double[] b){return a==null?b==null:b!=null&&Math.abs(a[0]-b[0])<EPS&&Math.abs(a[1]-b[1])<EPS;}
    private static List<double[]> shift(List<double[]> polygon,int y){if(y==0)return polygon;List<double[]> out=new ArrayList<>();for(double[] v:polygon)out.add(new double[]{v[0],v[1]+y,v[2]});return out;}
    private static List<double[]> clip(List<double[]> polygon,double plane,boolean above){List<double[]> out=new ArrayList<>();if(polygon.isEmpty())return out;double[] a=polygon.get(polygon.size()-1);boolean ai=above?a[1]>=plane-EPS:a[1]<=plane+EPS;for(double[] b:polygon){boolean bi=above?b[1]>=plane-EPS:b[1]<=plane+EPS;if(ai!=bi){double t=(plane-a[1])/(b[1]-a[1]);out.add(new double[]{a[0]+(b[0]-a[0])*t,plane,a[2]+(b[2]-a[2])*t});}if(bi)out.add(b);a=b;ai=bi;}return out;}
    private static void add(List<Face> out,boolean surface,List<double[]> polygon){List<double[]> clean=clean(polygon);if(clean.size()>=3)out.add(new Face(clean.toArray(new double[0][]),surface));}
    private static List<double[]> clean(List<double[]> polygon){List<double[]> p=new ArrayList<>();for(double[] v:polygon)if(p.isEmpty()||distance(p.get(p.size()-1),v)>EPS*EPS)p.add(v);if(p.size()>1&&distance(p.get(0),p.get(p.size()-1))<EPS*EPS)p.remove(p.size()-1);boolean changed=true;while(changed&&p.size()>=3){changed=false;for(int i=0;i<p.size();i++){double[] a=p.get((i+p.size()-1)%p.size()),b=p.get(i),c=p.get((i+1)%p.size());double[] n=cross(a,b,c);if(dot(n,n)<EPS*EPS){p.remove(i);changed=true;break;}}}return p;}
    private static void merge(List<Face> faces){boolean changed=true;while(changed){changed=false;outer:for(int a=0;a<faces.size();a++)for(int b=a+1;b<faces.size();b++){Face x=faces.get(a),y=faces.get(b);if(x.surface!=y.surface||dot(x.normal,y.normal)<1-EPS)continue;double[] origin=x.vertices[0];boolean plane=true;for(double[] v:y.vertices)if(Math.abs((v[0]-origin[0])*x.normal[0]+(v[1]-origin[1])*x.normal[1]+(v[2]-origin[2])*x.normal[2])>EPS){plane=false;break;}if(!plane)continue;for(int i=0;i<x.vertices.length;i++)for(int j=0;j<y.vertices.length;j++)if(distance(x.vertices[i],y.vertices[(j+1)%y.vertices.length])<EPS*EPS&&distance(x.vertices[(i+1)%x.vertices.length],y.vertices[j])<EPS*EPS){List<double[]> p=new ArrayList<>();for(int k=0;k<x.vertices.length;k++)p.add(x.vertices[(i+1+k)%x.vertices.length]);for(int k=2;k<y.vertices.length;k++)p.add(y.vertices[(j+k)%y.vertices.length]);p=clean(p);if(!convex(p,x.normal))continue;faces.set(a,new Face(p.toArray(new double[0][]),x.surface));faces.remove(b);changed=true;break outer;}}}}
    private static boolean convex(List<double[]> p,double[] normal){if(p.size()<3)return false;for(int i=0;i<p.size();i++)if(dot(cross(p.get(i),p.get((i+1)%p.size()),p.get((i+2)%p.size())),normal)<-EPS)return false;return true;}
    private static List<Face> quads(List<Face> faces){List<Face> out=new ArrayList<>();for(Face f:faces)if(f.vertices.length<=4)out.add(f);else for(int i=1;i+1<f.vertices.length;i++)add(out,f.surface,Arrays.asList(f.vertices[0],f.vertices[i],f.vertices[i+1]));return out;}
    private static Mesh rotate(Mesh mesh,EnumFacing facing){if(mesh==EMPTY||facing==EnumFacing.NORTH)return mesh;List<Face> faces=new ArrayList<>();for(Face f:mesh.faces){double[][] p=new double[f.vertices.length][];for(int i=0;i<p.length;i++)p[i]=RoofGeometry.rotate(f.vertices[i][0],f.vertices[i][1],f.vertices[i][2],facing);faces.add(new Face(p,f.surface));}List<AxisAlignedBB> boxes=new ArrayList<>();for(AxisAlignedBB b:mesh.boxes){double[] a=RoofGeometry.rotate(b.minX,b.minY,b.minZ,facing),c=RoofGeometry.rotate(b.maxX,b.maxY,b.maxZ,facing);boxes.add(new AxisAlignedBB(Math.min(a[0],c[0]),b.minY,Math.min(a[2],c[2]),Math.max(a[0],c[0]),b.maxY,Math.max(a[2],c[2])));}return new Mesh(faces,boxes);}
    private static double[] cross(double[] a,double[] b,double[] c){return new double[]{(b[1]-a[1])*(c[2]-a[2])-(b[2]-a[2])*(c[1]-a[1]),(b[2]-a[2])*(c[0]-a[0])-(b[0]-a[0])*(c[2]-a[2]),(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])};}
    private static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    private static double distance(double[] a,double[] b){double x=a[0]-b[0],y=a[1]-b[1],z=a[2]-b[2];return x*x+y*y+z*z;}
    private static double triangle(double[] o,double[] d,double[] a,double[] b,double[] c){double x=b[0]-a[0],y=b[1]-a[1],z=b[2]-a[2],u=c[0]-a[0],v=c[1]-a[1],w=c[2]-a[2],px=d[1]*w-d[2]*v,py=d[2]*u-d[0]*w,pz=d[0]*v-d[1]*u,det=x*px+y*py+z*pz;if(Math.abs(det)<1e-12)return -1;double tx=o[0]-a[0],ty=o[1]-a[1],tz=o[2]-a[2],alpha=(tx*px+ty*py+tz*pz)/det;if(alpha< -1e-9||alpha>1+1e-9)return -1;double qx=ty*z-tz*y,qy=tz*x-tx*z,qz=tx*y-ty*x,beta=(d[0]*qx+d[1]*qy+d[2]*qz)/det;if(beta< -1e-9||alpha+beta>1+1e-9)return -1;return (u*qx+v*qy+w*qz)/det;}
}
