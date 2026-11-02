package zone.moddev.mc.ironagearchitecture.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

/** Block-local wall geometry in pixels, shared by collision and client meshes. */
public final class WallGeometry {
    public enum Grain { DAUB, VERTICAL, EAST_WEST, NORTH_SOUTH, RISING, FALLING, BOARDS_X, BOARDS_Z }
    public static final double RECESS=.75, POST=3, DEPTH=1.25;
    private WallGeometry() { }

    public static final class Part {
        public final double[][] outline;
        public final double front, back;
        public final Grain grain;
        public final EnumFacing localFacing;
        public final boolean horizontal;
        public Part(double[][] outline,double front,double back,Grain grain) {
            this(outline,front,back,grain,EnumFacing.NORTH);
        }
        public Part(double[][] outline,double front,double back,Grain grain,EnumFacing localFacing) {
            this(outline,front,back,grain,localFacing,false);
        }
        public Part(double[][] outline,double front,double back,Grain grain,EnumFacing localFacing,boolean horizontal) {
            this.outline=outline;this.front=front;this.back=back;this.grain=grain;
            this.localFacing=localFacing;this.horizontal=horizontal;
        }
        public double[] transform(double x,double y,double z,EnumFacing facing) {
            double[] local=horizontal?rotate(x,z,y,localFacing):rotate(x,y,z,localFacing);
            return rotate(local[0],local[1],local[2],facing);
        }
        public AxisAlignedBB bounds(EnumFacing facing) {
            double minX=16,minY=16,minZ=16,maxX=0,maxY=0,maxZ=0;
            for(double[] p:outline) for(double z:new double[]{front,back}) {
                double[] v=transform(p[0],p[1],z,facing);
                minX=Math.min(minX,v[0]);maxX=Math.max(maxX,v[0]);
                minY=Math.min(minY,v[1]);maxY=Math.max(maxY,v[1]);
                minZ=Math.min(minZ,v[2]);maxZ=Math.max(maxZ,v[2]);
            }
            return new AxisAlignedBB(minX/16,minY/16,minZ/16,maxX/16,maxY/16,maxZ/16);
        }
    }
    public static double[] rotate(double x,double y,double z,EnumFacing facing) {
        double[] result=RoofGeometry.rotate(x/16,y/16,z/16,facing);
        return new double[]{result[0]*16,result[1]*16,result[2]*16};
    }
    public static void box(List<Part> parts,double x0,double y0,double z0,double x1,double y1,double z1,Grain grain) {
        if(x1>x0&&y1>y0&&z1>z0) parts.add(new Part(new double[][]{{x0,y0},{x1,y0},{x1,y1},{x0,y1}},z0,z1,grain));
    }
    private static void facePost(List<Part> parts,double left,double width) {
        // A single member passes through the wall, with exposed grain on both elevations.
        box(parts,left,0,0,left+width,16,16,Grain.VERTICAL);
    }
    private static void studExtension(List<Part> parts,int code,double y0,double y1) {
        if(code==1) box(parts,6.5,y0,0,9.5,y1,16,Grain.VERTICAL);
        if(code==2) box(parts,7,y0,0,9,y1,16,Grain.VERTICAL);
        if(code==3) {
            box(parts,4.5,y0,0,6,y1,16,Grain.VERTICAL);
            box(parts,10,y0,0,11.5,y1,16,Grain.VERTICAL);
        }
    }
    private static void diagonal(List<Part> parts,boolean rising) {
        diagonal(parts,rising,0,16,EnumFacing.NORTH);
    }
    public static double[][] clip(double[][] polygon,double edge,boolean keepGreater) {
        if(polygon.length==0) return polygon;
        List<double[]> result=new ArrayList<>();
        double[] previous=polygon[polygon.length-1];
        boolean wasInside=keepGreater?previous[0]>=edge:previous[0]<=edge;
        for(double[] current:polygon) {
            boolean inside=keepGreater?current[0]>=edge:current[0]<=edge;
            if(inside!=wasInside) {
                double t=(edge-previous[0])/(current[0]-previous[0]);
                result.add(new double[]{edge,previous[1]+t*(current[1]-previous[1])});
            }
            if(inside) result.add(current);
            previous=current;wasInside=inside;
        }
        return result.toArray(new double[result.size()][]);
    }
    private static void diagonal(List<Part> parts,boolean rising,double x0,double x1,EnumFacing localFacing) {
        double w=1.25;
        double[][] polygon={{0,0},{w,0},{16,16-w},{16,16},{16-w,16},{0,w}};
        if(!rising) {
            double[][] mirrored=new double[polygon.length][];
            for(int i=0;i<polygon.length;i++) mirrored[polygon.length-1-i]=new double[]{16-polygon[i][0],polygon[i][1]};
            polygon=mirrored;
        }
        Grain grain=rising?Grain.RISING:Grain.FALLING;
        polygon=clip(clip(polygon,x0,true),x1,false);
        // Braces sit a fraction behind posts/rails, avoiding coplanar overlapping timber faces.
        double inset=rising?.035:.045;
        parts.add(new Part(polygon,inset,DEPTH,grain,localFacing));
        parts.add(new Part(polygon,16-DEPTH,16-inset,grain,localFacing));
    }
    public static int bit(EnumFacing direction) { return 1<<direction.getHorizontalIndex(); }
    private static boolean requested(int mask,int shift,EnumFacing worldDirection) { return (mask&(bit(worldDirection)<<shift))!=0; }

    public static List<Part> parts(FrameFamily family,int panel,EnumFacing facing,int mask) {
        List<Part> result=new ArrayList<>();
        boolean corner=family==FrameFamily.TERMINAL&&(panel&1)==0;
        if(corner) {
            // L-shaped infill joins the east and south wall runs without a hole at either face.
            box(result,RECESS,0,RECESS,16,16,16-RECESS,Grain.DAUB);
            box(result,RECESS,0,16-RECESS,16-RECESS,16,16,Grain.DAUB);
            box(result,0,0,0,POST,16,POST,Grain.VERTICAL);
            box(result,16-POST,0,16-POST,16,16,16,Grain.VERTICAL);
        } else {
            box(result,0,0,RECESS,16,16,16-RECESS,Grain.DAUB);
            // Perpendicular wall branches extend the core to their shared boundary.
            if(requested(mask,0,facing)) box(result,RECESS,0,0,16-RECESS,16,RECESS,Grain.DAUB);
            if(requested(mask,0,facing.getOpposite())) box(result,RECESS,0,16-RECESS,16-RECESS,16,16,Grain.DAUB);
            switch(family) {
                case POST:
                    if((panel&1)==0) facePost(result,6.5,POST);
                    else {diagonal(result,true);diagonal(result,false);}
                    break;
                case STUD:
                    if((panel&1)==0) facePost(result,7,2);
                    else {facePost(result,4.5,1.5);facePost(result,10,1.5);}
                    break;
                case BRACE: diagonal(result,(panel&1)==0);break;
                case TERMINAL: facePost(result,0,POST);break;
                default: break;
            }
        }
        if(family==FrameFamily.INFILL) return Collections.unmodifiableList(result);
        if(family==FrameFamily.RAIL) {
            double bottom=(panel&1)==0?0:13,top=bottom+3;
            studExtension(result,(mask>>12)&15,0,bottom);
            studExtension(result,(mask>>16)&15,top,16);
        }
        if(family==FrameFamily.POST&&(panel&1)==0||family==FrameFamily.TERMINAL||family==FrameFamily.RAIL) {
            for(int plane=0;plane<(corner?2:1);plane++) {
                int joins=(mask>>(20+plane*4))&15;
                boolean whole=family!=FrameFamily.POST;
                EnumFacing local=plane==0?EnumFacing.NORTH:EnumFacing.WEST;
                if((joins&1)!=0) diagonal(result,true,whole?0:8,16,local);
                if((joins&2)!=0) diagonal(result,true,0,whole?16:8,local);
                if((joins&4)!=0) diagonal(result,false,whole?0:8,16,local);
                if((joins&8)!=0) diagonal(result,false,0,whole?16:8,local);
            }
        }
        for(int height=0;height<2;height++) {
            int shift=height==0?4:8;
            boolean left=requested(mask,shift,facing.rotateYCCW());
            boolean right=requested(mask,shift,facing.rotateY());
            boolean north=requested(mask,shift,facing),south=requested(mask,shift,facing.getOpposite());
            boolean own=family==FrameFamily.RAIL&&(panel&1)==height;
            if(own) left=right=true;
            double y0=height==0?0:13,y1=y0+3;
            double x0=left?0:8,x1=right?16:8;
            if(corner) {
                if(right) {
                    box(result,0,y0,.01,16,y1,DEPTH,Grain.EAST_WEST);
                    box(result,0,y0,16-DEPTH,16,y1,16-.01,Grain.EAST_WEST);
                }
                if(south) {
                    box(result,.01,y0,0,DEPTH,y1,16,Grain.NORTH_SOUTH);
                    box(result,16-DEPTH,y0,0,16-.01,y1,16,Grain.NORTH_SOUTH);
                }
            } else {
                // Braces/studs are intersections within a rail run; principal posts can terminate one.
                if((family==FrameFamily.BRACE||family==FrameFamily.STUD||(family==FrameFamily.POST&&(panel&1)!=0))&&(left||right)) {x0=0;x1=16;}
                if(family==FrameFamily.TERMINAL&&right) x0=0;
                box(result,x0,y0,.01,x1,y1,DEPTH,Grain.EAST_WEST);
                box(result,x0,y0,16-DEPTH,x1,y1,16-.01,Grain.EAST_WEST);
                if(north||south) {
                    double z0=north?0:8,z1=south?16:8;
                    box(result,.01,y0,z0,DEPTH,y1,z1,Grain.NORTH_SOUTH);
                    box(result,16-DEPTH,y0,z0,16-.01,y1,z1,Grain.NORTH_SOUTH);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }
}
