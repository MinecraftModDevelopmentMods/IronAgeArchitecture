package zone.moddev.mc.ironagearchitecture.block;

import java.awt.BasicStroke;
import java.awt.geom.*;
import java.util.*;

/** Pure horizontal contour solver. Every accepted cut stays inside the supplied material. */
public final class SmartCobGeometry {
    public static final double SKIN=1/16.0, FLATNESS=1/512.0;
    private SmartCobGeometry() { }
    public static long key(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
    public static int x(long key){return (int)(key>>32);}
    public static int z(long key){return (int)key;}
    public static Area footprint(Set<Long> cells) {
        Area area=new Area();for(long cell:cells) area.add(new Area(new Rectangle2D.Double(x(cell),z(cell),1,1)));return area;
    }
    public static final class Section {
        public final Area shape;
        public final Map<Long,SmartCobProfile> profiles;
        Section(Area shape,Map<Long,SmartCobProfile> profiles){this.shape=new Area(shape);this.profiles=Collections.unmodifiableMap(profiles);}
    }
    public static Section solve(Set<Long> cells,Area contacts) {
        Area original=footprint(cells);
        if(cells.isEmpty()) return new Section(original,Collections.emptyMap());
        Area chosen=null;
        for(double scale:new double[]{1,.75,.5,.25,0}) {
            Area shaped=round(original,contacts,scale);
            Area outside=new Area(shaped);outside.subtract(original);
            if(!outside.isEmpty()||components(shaped)>components(original)||!thickEnough(shaped,contacts)) continue;
            boolean nonempty=true;
            for(long cell:cells) if(!shaped.intersects(x(cell)+.0001,z(cell)+.0001,.9998,.9998)) {nonempty=false;break;}
            if(nonempty) {chosen=shaped;break;}
        }
        if(chosen==null) chosen=original;
        Map<Long,SmartCobProfile> profiles=new HashMap<>();
        List<List<double[]>> global=loops(chosen);
        for(long cell:cells) profiles.put(cell,clip(chosen,global,x(cell),z(cell)));
        return new Section(chosen,profiles);
    }
    private static Area round(Area original,Area contacts,double scale) {
        if(scale==0) return new Area(original);
        List<List<double[]>> boundaries=loops(original);
        Path2D.Double path=new Path2D.Double(Path2D.WIND_NON_ZERO);
        for(List<double[]> source:boundaries) {
            List<double[]> points=collinear(scale==1?shortcuts(source,original,contacts):source);
            int count=points.size();double[][] in=new double[count][],out=new double[count][];
            for(int i=0;i<count;i++) {
                double[] a=points.get((i+count-1)%count),b=points.get(i),c=points.get((i+1)%count);
                double ax=b[0]-a[0],az=b[1]-a[1],bx=c[0]-b[0],bz=c[1]-b[1];
                double la=Math.hypot(ax,az),lb=Math.hypot(bx,bz),turn=ax*bz-az*bx;
                double radius=0;
                if(turn>1e-7&&!contacts.intersects(b[0]-.125,b[1]-.125,.25,.25)) {
                    double nx=-az/la-bz/lb,nz=ax/la+bx/lb,len=Math.hypot(nx,nz);nx/=len;nz/=len;
                    double reach=0;
                    for(double d=1/32.0;d<=3;d+=1/32.0) {if(!original.contains(b[0]+nx*d,b[1]+nz*d)) break;reach=d;}
                    double wall=Math.max(.5,reach/Math.sqrt(2));
                    radius=Math.min(2,wall)*scale;
                    radius=Math.min(radius,Math.min(la,lb)*.49);
                    for(double d=SKIN;d<radius;d+=SKIN) {
                        double px=b[0]-ax/la*d-az/la*.00001,pz=b[1]-az/la*d+ax/la*.00001;
                        double qx=b[0]+bx/lb*d-bz/lb*.00001,qz=b[1]+bz/lb*d+bx/lb*.00001;
                        if(contacts.contains(px,pz)||contacts.contains(qx,qz)){radius=Math.max(0,d-SKIN);break;}
                    }
                }
                in[i]=new double[]{b[0]-ax/la*radius,b[1]-az/la*radius};
                out[i]=new double[]{b[0]+bx/lb*radius,b[1]+bz/lb*radius};
            }
            path.moveTo(out[0][0],out[0][1]);
            for(int step=1;step<=count;step++) {
                int i=step%count;double[] b=points.get(i);
                path.lineTo(in[i][0],in[i][1]);path.quadTo(b[0],b[1],out[i][0],out[i][1]);
            }
            path.closePath();
        }
        Area result=new Area(path);result.intersect(original);
        // A shallow inward skin also rounds room-side corners without adding material to air.
        result.subtract(new Area(new BasicStroke((float)(2*SKIN*scale),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND).createStrokedShape(result)));
        Area fixed=new Area(contacts);fixed.intersect(original);result.add(fixed);
        Path2D.Double linear=new Path2D.Double();linear.append(result.getPathIterator(null,FLATNESS),false);
        return new Area(linear);
    }
    private static List<double[]> shortcuts(List<double[]> input,Area original,Area contacts) {
        // Decide all stair tips against the same outline. Removing one tip first would
        // hide the stair context of its neighbour and leave alternating square teeth.
        List<double[]> p=new ArrayList<>();
        for(int i=0;i<input.size();i++) {
            int n=input.size();double[] before=input.get((i+n-2)%n),a=input.get((i+n-1)%n),b=input.get(i),c=input.get((i+1)%n),after=input.get((i+2)%n);
            double start=turn(before,a,b),middle=turn(a,b,c),end=turn(b,c,after);
            boolean tip=middle>1e-7&&(start< -1e-7||end< -1e-7)&&distance(a,c)<=4;
            if(tip&&safeChord(Arrays.asList(a,b,c),original,contacts))continue;
            p.add(b);
        }
        // Visibility shortcuts remove stair teeth across several cells, before fitting the curves.
        for(int pass=0;pass<2;pass++) for(int i=0;i<p.size()&&p.size()>4;i++) {
            for(int span=Math.min(5,p.size()-2);span>=2;span--) {
                if(i+span>=p.size()) continue;
                double[] a=p.get(i),b=p.get(i+span);
                double length=Math.hypot(b[0]-a[0],b[1]-a[1]);
                if(length>4||length<.01||Math.abs(a[0]-b[0])<.001||Math.abs(a[1]-b[1])<.001) continue;
                boolean left=false,right=false;
                for(int j=1;j<span;j++) {double[] v=p.get(i+j-1),w=p.get(i+j),q=p.get(i+j+1);double turn=(w[0]-v[0])*(q[1]-w[1])-(w[1]-v[1])*(q[0]-w[0]);left|=turn>1e-7;right|=turn< -1e-7;}
                if(span==2) {
                    double[] before=p.get((i+p.size()-1)%p.size()),after=p.get((i+span+1)%p.size());
                    double[] first=p.get(i+1),last=p.get(i+span-1);
                    double start=(a[0]-before[0])*(first[1]-a[1])-(a[1]-before[1])*(first[0]-a[0]);
                    double end=(b[0]-last[0])*(after[1]-b[1])-(b[1]-last[1])*(after[0]-b[0]);
                    if(start>=-1e-7||end>=-1e-7)continue;
                } else if(!left||!right)continue;
                Path2D.Double cut=new Path2D.Double();cut.moveTo(a[0],a[1]);
                for(int j=1;j<=span;j++) cut.lineTo(p.get(i+j)[0],p.get(i+j)[1]);cut.closePath();
                Area removed=new Area(cut);Area outside=new Area(removed);outside.subtract(original);
                Area fixed=new Area(removed);fixed.intersect(contacts);
                if(!outside.isEmpty()||!fixed.isEmpty()) continue;
                // Chord must stay inside material on its left, not bridge a room or a doorway.
                boolean safe=true;double nx=-(b[1]-a[1])/length*.001,nz=(b[0]-a[0])/length*.001;
                for(double t=.01;t<1&&safe;t+=.04)for(double depth:new double[]{.001,.125,.25,.375,.5})
                    if(!original.contains(a[0]+(b[0]-a[0])*t+nx*depth/.001,a[1]+(b[1]-a[1])*t+nz*depth/.001)){safe=false;break;}
                if(!safe) continue;
                for(int j=1;j<span;j++) p.remove(i+1);
                break;
            }
        }
        return p;
    }
    private static double turn(double[] a,double[] b,double[] c){return (b[0]-a[0])*(c[1]-b[1])-(b[1]-a[1])*(c[0]-b[0]);}
    private static boolean safeChord(List<double[]> path,Area original,Area contacts) {
        double[] a=path.get(0),b=path.get(path.size()-1);double length=distance(a,b);
        Path2D.Double polygon=new Path2D.Double();polygon.moveTo(a[0],a[1]);for(int i=1;i<path.size();i++)polygon.lineTo(path.get(i)[0],path.get(i)[1]);polygon.closePath();
        Area cut=new Area(polygon),outside=new Area(cut);outside.subtract(original);cut.intersect(contacts);
        if(!outside.isEmpty()||!cut.isEmpty())return false;
        double nx=-(b[1]-a[1])/length,nz=(b[0]-a[0])/length;
        for(double t=.01;t<1;t+=.04)for(double depth:new double[]{.001,.125,.25,.375,.5})
            if(!original.contains(a[0]+(b[0]-a[0])*t+nx*depth,a[1]+(b[1]-a[1])*t+nz*depth))return false;
        return true;
    }
    private static List<double[]> collinear(List<double[]> input) {
        List<double[]> result=new ArrayList<>(input);boolean changed=true;
        while(changed&&result.size()>3) {changed=false;for(int i=0;i<result.size();i++) {
            double[] a=result.get((i+result.size()-1)%result.size()),b=result.get(i),c=result.get((i+1)%result.size());
            if(Math.abs((b[0]-a[0])*(c[1]-b[1])-(b[1]-a[1])*(c[0]-b[0]))<1e-8) {result.remove(i);changed=true;break;}
        }}return result;
    }
    /** Openings cut a common structural outline; they never refit the whole course. */
    public static Section slice(Section structural,Set<Long> cells) {
        Area shape=footprint(cells);shape.intersect(structural.shape);
        Map<Long,SmartCobProfile> profiles=new HashMap<>();List<List<double[]>> global=loops(shape),uv=loops(structural.shape);
        for(long cell:cells)profiles.put(cell,clip(shape,global,uv,x(cell),z(cell)));
        return new Section(shape,profiles);
    }
    public static List<List<double[]>> loops(Area area) {
        List<List<double[]>> result=new ArrayList<>();List<double[]> points=null;double[] p=new double[6];
        PathIterator iterator=area.getPathIterator(null,FLATNESS);
        while(!iterator.isDone()) {
            int type=iterator.currentSegment(p);
            if(type==PathIterator.SEG_MOVETO) {points=new ArrayList<>();points.add(new double[]{p[0],p[1]});}
            else if(type==PathIterator.SEG_LINETO&&distance(points.get(points.size()-1),p)>1e-8) points.add(new double[]{p[0],p[1]});
            else if(type==PathIterator.SEG_CLOSE&&points!=null&&points.size()>=3) {
                if(distance(points.get(0),points.get(points.size()-1))<1e-8) points.remove(points.size()-1);
                double[] a=points.get(0),b=points.get(1);double len=distance(a,b);
                if(!area.contains((a[0]+b[0])/2-(b[1]-a[1])/len*1e-6,(a[1]+b[1])/2+(b[0]-a[0])/len*1e-6)) Collections.reverse(points);
                result.add(points);
            }
            iterator.next();
        }
        return result;
    }
    private static double distance(double[] a,double[] b){return Math.hypot(a[0]-b[0],a[1]-b[1]);}
    public static int components(Area area) {
        int count=0;
        for(List<double[]> loop:loops(area)) {double signed=0;for(int i=0;i<loop.size();i++){double[] a=loop.get(i),b=loop.get((i+1)%loop.size());signed+=a[0]*b[1]-a[1]*b[0];}if(signed>1e-7)count++;}
        return count;
    }
    public static boolean thickEnough(Area area) {
        return thickEnough(area,new Area());
    }
    private static boolean thickEnough(Area area,Area contacts) {
        List<List<double[]>> loops=loops(area);
        for(List<double[]> loop:loops) for(int i=0;i<loop.size();i++) {
            double[] a=loop.get(i),b=loop.get((i+1)%loop.size());double length=distance(a,b);
            if(length<1e-5) continue;
            double nx=-(b[1]-a[1])/length,nz=(b[0]-a[0])/length;
            double px=(a[0]+b[0])/2,pz=(a[1]+b[1])/2;
            if(contacts.contains(px+nx*.00001,pz+nz*.00001)) continue;
            // Half-block inward normal clearance is the minimum physical wall thickness.
            for(double d=1/32.0;d<.5-1e-4;d+=1/32.0) if(!area.contains(px+nx*d,pz+nz*d)) return false;
        }
        return true;
    }
    private static SmartCobProfile clip(Area shape,List<List<double[]>> global,int x,int z) {
        return clip(shape,global,global,x,z);
    }
    private static SmartCobProfile clip(Area shape,List<List<double[]>> global,List<List<double[]>> uv,int x,int z) {
        Area cell=new Area(shape);cell.intersect(new Area(new Rectangle2D.Double(x,z,1,1)));
        List<Integer> code=new ArrayList<>();
        for(List<double[]> loop:loops(cell)) for(int i=0;i<=loop.size();i++) {
            if(i==loop.size()) {code.add(-1);code.add(0);continue;}
            double[] a=loop.get(i),b=loop.get((i+1)%loop.size());double len=distance(a,b);
            double nx=(b[1]-a[1])/len,nz=-(b[0]-a[0])/len;
            boolean exterior=!shape.contains((a[0]+b[0])/2+nx*.00001,(a[1]+b[1])/2+nz*.00001);
            int qx=(int)Math.round((a[0]-x)*SmartCobProfile.Q),qz=(int)Math.round((a[1]-z)*SmartCobProfile.Q);
            double[] n=normal(global,a);
            double[] phase=normal(uv,a);if(Math.hypot(phase[0],phase[1])>.5)n[2]=phase[2];
            int packed=((int)Math.round(n[0]*511)+511)<<10|((int)Math.round(n[1]*511)+511)|(((int)Math.round(n[2]*2048)&2047)<<20);
            code.add((qx<<13)|qz|(exterior?SmartCobProfile.EDGE:0));code.add(packed);
        }
        int[] result=new int[code.size()];for(int i=0;i<result.length;i++) result[i]=code.get(i);
        return new SmartCobProfile(result);
    }
    private static double[] normal(List<List<double[]>> loops,double[] p) {
        double nx=0,nz=0,u=0;boolean found=false;
        for(List<double[]> loop:loops) {
          double perimeter=0;for(int i=0;i<loop.size();i++) perimeter+=distance(loop.get(i),loop.get((i+1)%loop.size()));
          double prefix=0;
          for(int i=0;i<loop.size();i++) {
            double[] a=loop.get(i),b=loop.get((i+1)%loop.size());
            double len=distance(a,b);
            if(Line2D.ptSegDist(a[0],a[1],b[0],b[1],p[0],p[1])<1e-6) {
                nx+=(b[1]-a[1])/len;nz-=(b[0]-a[0])/len;
                if(!found) {u=(prefix+distance(a,p))/perimeter*Math.max(1,Math.round(perimeter));found=true;}
            }
            prefix+=len;
          }
        }
        double len=Math.hypot(nx,nz);return len<1e-8?new double[3]:new double[]{nx/len,nz/len,u};
    }
}
