package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import net.minecraft.util.math.BlockPos;

/** Explicit planning only. Emits a small vocabulary of clipped, grid-aligned timber prisms. */
public final class RoofPlanner {
    private RoofPlanner(){}
    private static final class Parts {final List<Integer> wood=new ArrayList<>(),skin=new ArrayList<>();}
    public static RoofPlan solve(RoofPlan p){
        if(p.version>=2)return RoofDesignPlanner.solve(p);
        p.cells.clear();p.error="";
        if(p.width()<4||p.length()<4||p.width()>32||p.length()>32||p.floorY<0||p.floorY>250||p.pitch<0||p.pitch>2||p.base<0||p.base>8||p.eave<0||p.eave>2||p.gable<0||p.gable>2||p.wood<0||p.wood>5){p.error="Choose a 4 to 32 block rectangle and valid roof settings.";return p;}
        Map<Long,Parts> parts=new TreeMap<>();
        double u0=p.alongX?p.minZ:p.minX,u1=(p.alongX?p.maxZ:p.maxX)+1,v0=p.alongX?p.minX:p.minZ,v1=(p.alongX?p.maxX:p.maxZ)+1;
        double mid=(u0+u1)/2,rate=RoofPlan.PITCHES[p.pitch],y0=p.floorY+1+p.base*.5+.25;
        double a=u0-p.eave*.5,b=u1+p.eave*.5,c=v0-p.gable*.5,d=v1+p.gable*.5;
        // A rafter in every covering column and narrow battens keep every roof section attachable.
        for(int u=(int)Math.floor(a);u<Math.ceil(b);u++)for(int v=(int)Math.floor(c);v<Math.ceil(d);v++){
            int x=p.alongX?v:u,z=p.alongX?u:v;if(p.openings.contains(RoofPlan.column(x,z)))continue;
            double left=Math.max(a,u),right=Math.min(b,u+1),front=Math.max(c,v),back=Math.min(d,v+1);
            for(int side=0;side<2;side++){
                double lo=side==0?left:Math.max(left,mid),hi=side==0?Math.min(right,mid):right;if(hi<=lo)continue;
                double h0=y0+rate*Math.min(lo-u0,u1-lo),h1=y0+rate*Math.min(hi-u0,u1-hi);
                emit(parts,p,lo,hi,front,back,h0,h1,.5,true);
                double centre=(front+back)/2;
                emit(parts,p,lo,hi,Math.max(front,centre-.09375),Math.min(back,centre+.09375),h0-.0625,h1-.0625,-.1875,false);
                double band=(lo+hi)/2;
                double bl=Math.max(lo,band-.0625),br=Math.min(hi,band+.0625);
                double bh0=h0+(h1-h0)*(bl-lo)/(hi-lo),bh1=h0+(h1-h0)*(br-lo)/(hi-lo);
                emit(parts,p,bl,br,front,back,bh0,bh1,-.0625,false);
            }
        }
        // Main trusses include a collar high enough to leave the central attic open.
        int spans=Math.max(1,(int)Math.ceil((v1-v0-1)/4));
        double apex=y0+rate*(u1-u0)/2;
        box(parts,p,mid-.125,mid+.125,c,d,apex-.375,apex-.125,false);
        for(double u:new double[]{u0+.125,u1-.125})box(parts,p,u-.125,u+.125,v0,v1,y0-.25,y0,false);
        for(int i=0;i<=spans;i++){
            double v=v0+.125+(v1-v0-.25)*i/spans;
            double ridge=y0+rate*(u1-u0)/2,collar=Math.max(y0,Math.min(ridge-.375,p.floorY+3.25));
            double inset=(collar-y0)/rate;
            box(parts,p,u0+inset,u1-inset,v-.125,v+.125,collar-.1875,collar,false);
            for(double u:new double[]{u0+.125,u1-.125})box(parts,p,u-.125,u+.125,v-.125,v+.125,p.floorY+1,y0,false);
        }
        if(parts.size()>RoofPlan.MAX_CELLS){p.error="This roof needs more than 1,024 frame cells. Select a smaller area.";return p;}
        Map<Long,BlockPos> owners=new HashMap<>();
        for(Map.Entry<Long,Parts> e:parts.entrySet())if(!e.getValue().wood.isEmpty()){BlockPos at=BlockPos.fromLong(e.getKey());long column=RoofPlan.column(at.getX(),at.getZ());BlockPos old=owners.get(column);if(old==null||at.getY()>old.getY())owners.put(column,at);}
        for(Map.Entry<Long,Parts> e:parts.entrySet()){BlockPos at=BlockPos.fromLong(e.getKey());if(at.getY()<0||at.getY()<p.floorY-3||at.getY()>255||at.getY()-p.floorY>48){p.error="The roof reaches beyond the allowed height. Raise its base or reduce its pitch.";p.cells.clear();return p;}Parts q=e.getValue();BlockPos owner=owners.get(RoofPlan.column(at.getX(),at.getZ()));if(owner==null){p.error="Roof section has no timber support.";p.cells.clear();return p;}p.cells.put(e.getKey(),new RoofCell(p.id,owner,array(q.wood),array(q.skin)));}
        return p;
    }
    private static int[] array(List<Integer> list){int[] a=new int[list.size()];for(int i=0;i<a.length;i++)a[i]=list.get(i);return a;}
    private static void box(Map<Long,Parts> out,RoofPlan p,double u0,double u1,double v0,double v1,double y0,double y1,boolean skin){if(u1>u0&&v1>v0&&y1>y0)emit(out,p,u0,u1,v0,v1,y0,y0,y1-y0,skin);}
    // Piece: u0,u1,v0,v1,h0,h1,depth,axis. A negative depth goes below its sloping datum.
    private static void emit(Map<Long,Parts> out,RoofPlan p,double u0,double u1,double v0,double v1,double h0,double h1,double depth,boolean skin){
        int low=(int)Math.floor(Math.min(h0,h1)+Math.min(0,depth)+1e-8),high=(int)Math.ceil(Math.max(h0,h1)+Math.max(0,depth)-1e-8);
        for(int u=(int)Math.floor(u0+1e-8);u<Math.ceil(u1-1e-8);u++)for(int v=(int)Math.floor(v0+1e-8);v<Math.ceil(v1-1e-8);v++)for(int y=low;y<high;y++){
            double a=Math.max(u0,u),b=Math.min(u1,u+1),c=Math.max(v0,v),d=Math.min(v1,v+1),ha=h0+(h1-h0)*(a-u0)/(u1-u0),hb=h0+(h1-h0)*(b-u0)/(u1-u0);
            if(Math.max(ha,hb)+Math.max(0,depth)<=y+1e-8||Math.min(ha,hb)+Math.min(0,depth)>=y+1-1e-8)continue;
            int x=p.alongX?v:u,z=p.alongX?u:v;if(p.openings.contains(RoofPlan.column(x,z)))continue;
            Parts part=out.computeIfAbsent(new BlockPos(x,y,z).toLong(),k->new Parts());List<Integer> list=skin?part.skin:part.wood;
            for(int n:new int[]{q(a-u),q(b-u),q(c-v),q(d-v),q(ha-y),q(hb-y),q(depth),p.alongX?1:0})list.add(n);
        }
    }
    private static int q(double x){return (int)Math.round(x*32);}
}
