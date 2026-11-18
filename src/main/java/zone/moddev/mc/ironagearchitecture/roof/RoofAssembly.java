package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/** Bounded prism splitter used only while a drawing changes. */
final class RoofAssembly {
    final RoofPlan plan;
    final Map<Long,List<RoofPart>> parts=new TreeMap<>();
    final Map<Long,List<RoofWindow>> windows=new TreeMap<>();
    RoofAssembly(RoofPlan p){plan=p;}
    void box(String id,int kind,int material,double u0,double u1,double v0,double v1,double bottom,double top){if(top>bottom+1e-8)emit(id,kind,material,u0,u1,v0,v1,bottom,bottom,top-bottom,plan.alongX,EnumFacing.NORTH);}
    void slope(String id,int kind,int material,double u0,double u1,double v0,double v1,double h0,double h1,double depth){emit(id,kind,material,u0,u1,v0,v1,h0,h1,depth,plan.alongX,EnumFacing.NORTH);}
    void across(String id,int kind,int material,double u0,double u1,double v0,double v1,double h0,double h1,double depth){emit(id,kind,material,v0,v1,u0,u1,h0,h1,depth,!plan.alongX,EnumFacing.NORTH);}
    void wall(String id,int material,double u0,double u1,double v0,double v1,double bottom0,double bottom1,double top0,double top1,boolean across){
        if(across)emitWall(id,RoofPart.WALL,material,v0,v1,u0,u1,bottom0,bottom1,top0,top1,!plan.alongX);else emitWall(id,RoofPart.WALL,material,u0,u1,v0,v1,bottom0,bottom1,top0,top1,plan.alongX);
    }
    void gableTimber(String id,int material,double a,double b,double c,double d,double l,double r,double h,double j){emitWall(id,RoofPart.TIMBER,material,a,b,c,d,l,r,h,j,plan.alongX);}
    private void emitWall(String id,int kind,int material,double a,double b,double c,double d,double l,double r,double h,double j,boolean axis){
        // Two independent planes form a closed cheek, without stair-stepped caps.
        if(b<=a||d<=c||Math.max(h-l,j-r)<1e-8)return;
        if(h<l||j<r){double t=(h-l)/((h-l)-(j-r)),x=a+(b-a)*t,low=l+(r-l)*t,top=h+(j-h)*t;if(h<l){emitWall(id,kind,material,x,b,c,d,low,r,top,j,axis);}else emitWall(id,kind,material,a,x,c,d,l,low,h,top,axis);return;}
        int y0=(int)Math.floor(Math.min(l,r)+1e-8),y1=(int)Math.ceil(Math.max(h,j)-1e-8);
        if(y1-y0>48)throw new IllegalArgumentException("Reduce this dormer's height.");
        for(int u=(int)Math.floor(a+1e-8);u<Math.ceil(b-1e-8);u++)for(int v=(int)Math.floor(c+1e-8);v<Math.ceil(d-1e-8);v++)for(int y=y0;y<y1;y++){
            double x=Math.max(a,u),xx=Math.min(b,u+1),z=Math.max(c,v),zz=Math.min(d,v+1),t=(x-a)/(b-a),tt=(xx-a)/(b-a),lo=l+(r-l)*t,rr=l+(r-l)*tt,hi=h+(j-h)*t,jj=h+(j-h)*tt;
            if(Math.max(hi,jj)<=y+1e-8||Math.min(lo,rr)>=y+1-1e-8)continue;
            int wx=axis?v:u,wz=axis?u:v;if(plan.openings.contains(RoofPlan.column(wx,wz)))continue;
            if(y<0||y>255||y<plan.floorY-6||y-plan.floorY>48)throw new IllegalArgumentException("The dormer extends beyond its height limit.");
            BlockPos at=new BlockPos(wx,y,wz);String unit=id+":"+wx+":"+y+":"+wz;int depths=(q(lo-hi)&65535)|(q(rr-jj)<<16);int[] data={q(x-u),q(xx-u),q(z-v),q(zz-v),q(hi-y),q(jj-y),depths,(axis?1:0)|2};
            List<RoofPart> ps=parts.computeIfAbsent(at.toLong(),k->new ArrayList<>());int found=-1;for(int i=0;i<ps.size();i++)if(ps.get(i).id.equals(unit))found=i;if(found<0)ps.add(new RoofPart(unit,at,kind,material,data));else{RoofPart old=ps.get(found);int[] all=Arrays.copyOf(old.data,old.data.length+8);System.arraycopy(data,0,all,old.data.length,8);ps.set(found,new RoofPart(unit,at,RoofPart.WALL,material,all));}
            if(parts.size()>RoofPlan.MAX_CELLS)throw new IllegalArgumentException("This roof needs more than 1,024 cells.");
        }
    }
    void surface(String id,int kind,int material,List<double[]> polygon,double depth,boolean across){
        if(polygon.size()<3)return;boolean axis=across?!plan.alongX:plan.alongX;List<double[]> source=new ArrayList<>();for(double[] q:polygon)source.add(across?new double[]{q[1],q[0],q[2]}:q.clone());double a=1e9,b=-1e9,c=1e9,d=-1e9;for(double[] q:source){a=Math.min(a,q[0]);b=Math.max(b,q[0]);c=Math.min(c,q[1]);d=Math.max(d,q[1]);}
        for(int u=(int)Math.floor(a+1e-8);u<Math.ceil(b-1e-8);u++)for(int v=(int)Math.floor(c+1e-8);v<Math.ceil(d-1e-8);v++){List<double[]> poly=clipSurface(clipSurface(clipSurface(clipSurface(source,0,u,true),0,u+1,false),1,v,true),1,v+1,false);if(poly.size()<3)continue;double low=1e9,high=-1e9;for(double[] q:poly){low=Math.min(low,q[2]+Math.min(0,depth));high=Math.max(high,q[2]+Math.max(0,depth));}for(int y=(int)Math.floor(low+1e-8);y<Math.ceil(high-1e-8);y++){int x=axis?v:u,z=axis?u:v;if(plan.openings.contains(RoofPlan.column(x,z)))continue;if(y<0||y>255||y<plan.floorY-6||y-plan.floorY>48)throw new IllegalArgumentException("Roof intersection exceeds the height limit.");BlockPos at=new BlockPos(x,y,z);String unit=kind==RoofPart.TIMBER?id+":"+x+":"+y+":"+z:id;for(int j=1;j<poly.size()-1;j++){double[][] tri={poly.get(0),poly.get(j),poly.get(j+1)};double area=(tri[1][0]-tri[0][0])*(tri[2][1]-tri[0][1])-(tri[2][0]-tri[0][0])*(tri[1][1]-tri[0][1]);if(Math.abs(area)<1e-8)continue;int[] data=new int[8];for(int i=0;i<3;i++){data[i]=(q(tri[i][0]-u)&65535)|(q(tri[i][1]-v)<<16);data[3+i]=q(tri[i][2]-y);}data[6]=q(depth);data[7]=(axis?1:0)|4;List<RoofPart> ps=parts.computeIfAbsent(at.toLong(),k->new ArrayList<>());int found=-1;for(int i=0;i<ps.size();i++)if(ps.get(i).id.equals(unit)&&ps.get(i).kind==kind)found=i;if(found<0)ps.add(new RoofPart(unit,at,kind,material,data));else{RoofPart old=ps.get(found);int[] all=Arrays.copyOf(old.data,old.data.length+8);System.arraycopy(data,0,all,old.data.length,8);ps.set(found,new RoofPart(unit,at,kind,material,all));}}if(parts.size()>RoofPlan.MAX_CELLS)throw new IllegalArgumentException("This roof needs more than 1,024 cells.");}}
    }
    private static List<double[]> clipSurface(List<double[]> p,int axis,double edge,boolean greater){List<double[]> out=new ArrayList<>();if(p.isEmpty())return out;double[] a=p.get(p.size()-1);boolean ai=greater?a[axis]>=edge-1e-9:a[axis]<=edge+1e-9;for(double[] b:p){boolean bi=greater?b[axis]>=edge-1e-9:b[axis]<=edge+1e-9;if(ai!=bi){double t=(edge-a[axis])/(b[axis]-a[axis]);out.add(new double[]{a[0]+t*(b[0]-a[0]),a[1]+t*(b[1]-a[1]),a[2]+t*(b[2]-a[2])});}if(bi)out.add(b);a=b;ai=bi;}return out;}
    void fascia(String id,int wood,double u0,double u1,double v0,double v1,double bottom,double top,EnumFacing face){emit(id,RoofPart.FASCIA,wood,u0,u1,v0,v1,bottom,bottom,top-bottom,plan.alongX,face);}
    private void emit(String id,int kind,int material,double u0,double u1,double v0,double v1,double h0,double h1,double depth,boolean axis,EnumFacing face){
        if(u1<=u0+1e-8||v1<=v0+1e-8||Math.abs(depth)<1e-8)return;
        int low=(int)Math.floor(Math.min(h0,h1)+Math.min(0,depth)+1e-8),high=(int)Math.ceil(Math.max(h0,h1)+Math.max(0,depth)-1e-8);
        if(high-low>48)throw new IllegalArgumentException("Reduce this roof's height.");
        for(int u=(int)Math.floor(u0+1e-8);u<Math.ceil(u1-1e-8);u++)for(int v=(int)Math.floor(v0+1e-8);v<Math.ceil(v1-1e-8);v++)for(int y=low;y<high;y++){
            double a=Math.max(u0,u),b=Math.min(u1,u+1),c=Math.max(v0,v),d=Math.min(v1,v+1),ha=h0+(h1-h0)*(a-u0)/(u1-u0),hb=h0+(h1-h0)*(b-u0)/(u1-u0);
            if(Math.max(ha,hb)+Math.max(0,depth)<=y+1e-8||Math.min(ha,hb)+Math.min(0,depth)>=y+1-1e-8)continue;
            int x=axis?v:u,z=axis?u:v;if(plan.openings.contains(RoofPlan.column(x,z)))continue;
            if(y<0||y>255||y<plan.floorY-6||y-plan.floorY>48)throw new IllegalArgumentException("The planned roof extends beyond its height limit.");
            BlockPos at=new BlockPos(x,y,z);String resource=(kind==RoofPart.TIMBER||kind==RoofPart.WALL||kind==RoofPart.FASCIA||kind==RoofPart.SOFFIT)?id+":"+x+":"+y+":"+z:id;
            List<RoofPart> list=parts.computeIfAbsent(at.toLong(),k->new ArrayList<>());int[] data={q(a-u),q(b-u),q(c-v),q(d-v),q(ha-y),q(hb-y),q(depth),axis?1:0};
            int found=-1;for(int i=0;i<list.size();i++)if(list.get(i).id.equals(resource)&&list.get(i).kind==kind&&list.get(i).material==material){found=i;break;}
            if(found<0)list.add(new RoofPart(resource,at,kind,material,data,face));else{RoofPart old=list.get(found);int[] joined=Arrays.copyOf(old.data,old.data.length+8);System.arraycopy(data,0,joined,old.data.length,8);list.set(found,new RoofPart(resource,at,kind,material,joined,face));}
            if(parts.size()>RoofPlan.MAX_CELLS)throw new IllegalArgumentException("This roof needs more than 1,024 cells. Reduce its size or split the drawing.");
        }
    }
    static int q(double x){return (int)Math.round(x*32);}
    void window(RoofWindow window){
        removeWindow(window.owner);
        for(BlockPos at:RoofWindowGeometry.candidates(window)){
            RoofWindow slice=window.slice(at);if(RoofWindowGeometry.mesh(slice).faces.isEmpty())continue;
            List<RoofWindow> list=windows.computeIfAbsent(at.toLong(),k->new ArrayList<>());list.add(slice);
            parts.computeIfAbsent(at.toLong(),k->new ArrayList<>()).add(new RoofPart(window.id,window.owner,RoofPart.WINDOW,window.style,new int[0]));
        }
        // A window owns its whole root cell; its surround supplies the missing infill.
        parts.remove(window.owner.toLong());
        parts.computeIfAbsent(window.owner.toLong(),k->new ArrayList<>()).add(new RoofPart(window.id,window.owner,RoofPart.WINDOW,window.style,new int[0]));
    }
    void removeWindow(BlockPos owner){Set<String> ids=new HashSet<>();for(List<RoofWindow> list:windows.values())for(RoofWindow w:list)if(w.owner.equals(owner))ids.add(w.id);if(ids.isEmpty())return;for(List<RoofWindow> list:windows.values())list.removeIf(w->ids.contains(w.id));for(List<RoofPart> list:parts.values())list.removeIf(p->ids.contains(p.id));}
    void finish(){
        Map<String,BlockPos> roots=new HashMap<>();for(Map.Entry<Long,List<RoofPart>> e:parts.entrySet())for(RoofPart p:e.getValue())roots.putIfAbsent(p.id,BlockPos.fromLong(e.getKey()));
        Set<Long> keys=new TreeSet<>(parts.keySet());keys.addAll(windows.keySet());if(keys.size()>RoofPlan.MAX_CELLS)throw new IllegalArgumentException("This roof needs more than 1,024 cells. Reduce its size or split the drawing.");
        plan.cells.clear();for(long key:keys){BlockPos at=BlockPos.fromLong(key);List<RoofPart> list=new ArrayList<>();for(RoofPart p:parts.getOrDefault(key,Collections.emptyList()))list.add(new RoofPart(p.id,p.kind==RoofPart.WINDOW?p.root:roots.get(p.id),p.kind,p.material,p.data,p.facing));RoofCell cell=new RoofCell(plan.id,at,list,windows.getOrDefault(key,Collections.emptyList()));if(!cell.empty())plan.cells.put(key,cell);}
    }
}
