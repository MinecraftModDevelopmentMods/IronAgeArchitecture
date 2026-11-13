package zone.moddev.mc.ironagearchitecture.block;

import java.awt.geom.*;
import java.util.*;
import net.minecraft.util.math.AxisAlignedBB;

/** Immutable cell-clipped contours. Coordinates are exact multiples of 1/4096 block. */
public final class SmartCobProfile {
    public static final int Q=4096, EDGE=1<<26;
    public static final SmartCobProfile SQUARE=new SmartCobProfile(new int[]{EDGE,0,EDGE|(Q<<13),0,EDGE|(Q<<13)|Q,0,EDGE|Q,0,-1,0});
    private final int[] code;
    private transient Area area;
    private transient List<AxisAlignedBB> boxes;
    public SmartCobProfile(int[] code) {
        if(code.length<8||code.length>4096||(code.length&1)!=0) throw new IllegalArgumentException("Invalid cob profile length");
        this.code=code.clone();int count=0;
        for(int i=0;i<code.length;i+=2) {
            int p=code[i];
            if(p==-1) {if(count<3) throw new IllegalArgumentException("Incomplete cob contour");count=0;continue;}
            if(p<0||(p>>>27)!=0||((p>>>13)&8191)>Q||(p&8191)>Q) throw new IllegalArgumentException("Cob contour outside cell");
            count++;
        }
        if(count!=0) throw new IllegalArgumentException("Unclosed cob contour");
    }
    public int[] encode(){return code.clone();}
    public List<List<double[]>> loops() {
        List<List<double[]>> result=new ArrayList<>();List<double[]> loop=new ArrayList<>();
        for(int i=0;i<code.length;i+=2) {
            int p=code[i],n=code[i+1];
            if(p==-1) {result.add(loop);loop=new ArrayList<>();}
            else loop.add(new double[]{((p>>>13)&8191)/(double)Q,(p&8191)/(double)Q,(p&EDGE)!=0?1:0,
                n==0?0:(((n>>>10)&1023)-511)/511.0,n==0?0:((n&1023)-511)/511.0,(n>>>20)/2048.0});
        }
        return result;
    }
    public Area area() {
        if(area==null) {
            Path2D.Double path=new Path2D.Double(Path2D.WIND_NON_ZERO);
            for(List<double[]> loop:loops()) {path.moveTo(loop.get(0)[0],loop.get(0)[1]);for(int i=1;i<loop.size();i++) path.lineTo(loop.get(i)[0],loop.get(i)[1]);path.closePath();}
            area=new Area(path);
        }
        return new Area(area);
    }
    public List<AxisAlignedBB> boxes() {
        if(boxes==null) {
            Area shape=area();List<AxisAlignedBB> result=new ArrayList<>();
            for(int z=0;z<32;z++) for(int x=0;x<32;) {
                if(!shape.intersects(x/32.0,z/32.0,1/32.0,1/32.0)) {x++;continue;}
                int start=x++;
                while(x<32&&shape.intersects(x/32.0,z/32.0,1/32.0,1/32.0)) x++;
                AxisAlignedBB next=new AxisAlignedBB(start/32.0,0,z/32.0,x/32.0,1,(z+1)/32.0);
                int merge=-1;
                for(int i=result.size()-1;i>=0;i--) {AxisAlignedBB old=result.get(i);if(old.minX==next.minX&&old.maxX==next.maxX&&old.maxZ==next.minZ) {merge=i;break;}}
                if(merge<0) result.add(next);else {AxisAlignedBB old=result.get(merge);result.set(merge,new AxisAlignedBB(old.minX,0,old.minZ,old.maxX,1,next.maxZ));}
            }
            boxes=Collections.unmodifiableList(result);
        }
        return boxes;
    }
    @Override public boolean equals(Object other){return other instanceof SmartCobProfile&&Arrays.equals(code,((SmartCobProfile)other).code);}
    @Override public int hashCode(){return Arrays.hashCode(code);}
}
