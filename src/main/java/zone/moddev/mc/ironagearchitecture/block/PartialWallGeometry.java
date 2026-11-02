package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.util.EnumFacing;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.Part;
import zone.moddev.mc.ironagearchitecture.block.WallGeometry.Grain;

/** Half-depth members retain their face widths; short braces are clipped, never scaled. */
public final class PartialWallGeometry {
    private PartialWallGeometry() { }
    public static List<Part> parts(WallProfile profile,FrameFamily family,int panel,EnumFacing facing,int mask) {
        List<Part> result=new ArrayList<>();
        if(profile.corner) {
            int x=profile.x0,z=profile.z0;
            double r=WallGeometry.RECESS;
            WallGeometry.box(result,x+r,0,z+r,profile.x1,16,z+8-r,Grain.DAUB);
            WallGeometry.box(result,x+r,0,z+8-r,x+8-r,16,profile.z1,Grain.DAUB);
            // Shared outer post and the matching inside arris, both within the footprint.
            double outer=(panel&1)==0?3:1.5,inner=(panel&1)==0?1.5:3;
            WallGeometry.box(result,x,0,z,x+outer,16,z+outer,Grain.VERTICAL);
            WallGeometry.box(result,x+8-inner,0,z+8-inner,x+8,16,z+8,Grain.VERTICAL);
            for(int h=0;h<2;h++) {
                int bits=(mask>>(h==0?4:8))&15;
                if(bits==0) continue;
                double y=h==0?0:13;
                WallGeometry.box(result,x,y,z+.01,profile.x1,y+3,z+1.25,Grain.EAST_WEST);
                WallGeometry.box(result,x,y,z+6.75,profile.x1,y+3,z+7.99,Grain.EAST_WEST);
                WallGeometry.box(result,x+.01,y,z,x+1.25,y+3,profile.z1,Grain.NORTH_SOUTH);
                WallGeometry.box(result,x+6.75,y,z,x+7.99,y+3,profile.z1,Grain.NORTH_SOUTH);
            }
        } else {
            WallGeometry.box(result,profile.x0,0,.75,profile.x1,16,7.25,Grain.DAUB);
            int sourcePanel=family==FrameFamily.TERMINAL?1:panel;
            for(Part part:WallGeometry.parts(family,sourcePanel,facing,mask)) {
                if(part.grain==Grain.DAUB) continue;
                double[][] polygon=part.outline;
                // Principal/end posts belong to the short piece, not a severed half-post.
                if(profile!=WallProfile.THIN&&part.grain==Grain.VERTICAL) {
                    double shift=family==FrameFamily.TERMINAL?profile.x0:(profile.x0+profile.x1)/2.0-8;
                    polygon=new double[part.outline.length][];
                    for(int i=0;i<polygon.length;i++) polygon[i]=new double[]{part.outline[i][0]+shift,part.outline[i][1]};
                }
                polygon=WallGeometry.clip(WallGeometry.clip(polygon,profile.x0,true),profile.x1,false);
                if(polygon.length<3) continue;
                double front=part.front,back=part.back;
                if(front>=8) {front-=8;back-=8;}
                else if(back>8) back=8;
                if(back>front) result.add(new Part(polygon,front,back,part.grain,part.localFacing));
            }
        }
        return Collections.unmodifiableList(result);
    }
}
