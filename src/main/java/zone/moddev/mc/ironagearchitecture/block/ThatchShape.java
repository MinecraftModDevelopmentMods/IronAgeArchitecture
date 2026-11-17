package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;

/** A finite kit selected from a 3x3 local height stencil, without roof surveys. */
public final class ThatchShape {
    private static final EnumFacing[] DIRECTIONS={EnumFacing.NORTH,EnumFacing.EAST,EnumFacing.SOUTH,EnumFacing.WEST};
    private static final int[] KEYS={0,1,3,5,7,15,16,18,20,22,32,34,36,38,64,68,80,84,112,116,128,129,132,133,160,176,208,224,256,272,273,320,352,368,400,402,416,417,418,419,640,656,704,705,800,848,849,850,851,1280,1281,1283,1285,1287,1295};
    public static final int COUNT=KEYS.length+1,SOLID_PROFILE=COUNT-1,FLAT_PROFILE=40;
    private static final int ABSENT=9;
    private static final double[][] PERIMETER={{0,0},{.5,0},{1,0},{1,.5},{1,1},{.5,1},{0,1},{0,.5}};
    private static final int[][] AROUND={{1,3,0},{1,5,2},{7,5,8},{7,3,6}};
    private static final int[] EDGES={1,5,7,3},POW3={1,3,9,27};
    private static final ThatchShape[] SHAPES=new ThatchShape[COUNT*4],LOOKUP=new ThatchShape[1296];
    static {for(int profile=0;profile<COUNT;profile++)for(EnumFacing facing:DIRECTIONS)SHAPES[profile*4+facing.getHorizontalIndex()]=new ThatchShape(profile,facing);for(int p=0;p<KEYS.length;p++){int c=KEYS[p]/16,m=KEYS[p]&15;for(EnumFacing d:DIRECTIONS){int key=c*16+m;if(LOOKUP[key]==null)LOOKUP[key]=of(p,d);c=(c%27)*3+c/27;m=((m<<1)&15)|(m>>3);}}}
    public final int profile,code;public final EnumFacing facing;
    private ThatchShape(int profile,EnumFacing facing){this.profile=profile;this.facing=facing;code=profile*4+facing.getHorizontalIndex();}
    public static ThatchShape of(int profile,EnumFacing facing){return SHAPES[profile*4+facing.getHorizontalIndex()];}
    public static ThatchShape of(ThatchForm form,EnumFacing facing){if(form==ThatchForm.SOLID)return of(SOLID_PROFILE,facing);int[] c=new int[4];int mask=0;for(int i=0;i<4;i++){double[] a=point(i*2),b=point((i*2+2)%8),m=point(i*2+1);c[i]=(int)Math.round(template(form,a[0],a[1])*2);if(c[i]!=1&&c[i]==(int)Math.round(template(form,b[0],b[1])*2)&&template(form,m[0],m[1])==c[i]/2.0)mask|=1<<i;}ThatchShape base=from(c,mask);int turn=turn(base.facing)+turn(facing);return of(base.profile,DIRECTIONS[turn&3]);}
    private static int turn(EnumFacing d){for(int i=0;i<4;i++)if(DIRECTIONS[i]==d)return i;throw new IllegalArgumentException();}
    public static ThatchShape from(int[] corners,int mask){return lookup((corners[0]+corners[1]*3+corners[2]*9+corners[3]*27)*16+mask);}
    private static ThatchShape lookup(int key){ThatchShape s=LOOKUP[key];if(s==null)throw new IllegalArgumentException("Invalid finite thatch profile "+key);return s;}
    public double corner(int i){if(profile==SOLID_PROFILE)return 1;int c=KEYS[profile]/16;for(int j=0;j<i;j++)c/=3;return (c%3)/2.0;}
    public double edge(int i){double a=corner(i),b=corner((i+1)&3);if(profile==SOLID_PROFILE)return 1;return a==b&&a!=.5?((KEYS[profile]&(1<<i))!=0?a:.5):(a+b)/2;}
    public double centre(){if(profile==SOLID_PROFILE)return 1;double min=1,max=0;for(int i=0;i<4;i++){min=Math.min(min,corner(i));max=Math.max(max,corner(i));}return min==max?.5:(min+max)/2;}
    public double perimeter(int i){return (i&1)==0?corner(i/2):edge(i/2);}
    public static double[] point(int i){return PERIMETER[i];}
    public double height(double x,double z){for(int i=0;i<8;i++){double[] a=point(i),b=point((i+1)&7);double ax=a[0]-.5,az=a[1]-.5,bx=b[0]-.5,bz=b[1]-.5,px=x-.5,pz=z-.5,det=ax*bz-bx*az,u=(px*bz-bx*pz)/det,v=(ax*pz-px*az)/det;if(u>=-1e-9&&v>=-1e-9&&u+v<=1+1e-9)return centre()*(1-u-v)+perimeter(i)*u+perimeter((i+1)&7)*v;}throw new IllegalArgumentException("Outside roof cell");}
    public static boolean material(IBlockState state){return SmartThatchBlock.owner(state)||state.getBlock() instanceof RoofBlock&&"thatch".equals(((RoofBlock)state.getBlock()).materialKey);}
    private static IBlockState read(IBlockAccess world,BlockPos pos,BlockPos placed,IBlockState next){if(pos.equals(placed))return next;if(world instanceof World&&!((World)world).isBlockLoaded(pos))return net.minecraft.init.Blocks.AIR.getDefaultState();return world.getBlockState(pos);}
    public static ThatchShape at(IBlockAccess world,BlockPos pos,IBlockState state){return at(world,pos,state,null,null);}
    public static ThatchShape at(IBlockAccess world,BlockPos pos,IBlockState state,BlockPos placed,IBlockState next){
        if(material(read(world,pos.up(),placed,next)))return of(SOLID_PROFILE,EnumFacing.NORTH);
        int[] levels=new int[9];Arrays.fill(levels,ABSENT);levels[4]=0;IBlockState legacy=null;
        for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){if(x==0&&z==0)continue;BlockPos at=pos.add(x,0,z);int index=(z+1)*3+x+1;for(int y=1;y>=-1;y--){IBlockState found=read(world,at.up(y),placed,next);if(material(found)){levels[index]=y;if(y==0&&(x==0||z==0)&&found.getBlock() instanceof RoofBlock)legacy=found;break;}}}
        ThatchShape shape=select(levels);
        if(shape.profile==FLAT_PROFILE){if(legacy!=null){RoofBlock b=(RoofBlock)legacy.getBlock();return of(b.isRidge()?ThatchForm.RIDGE:ThatchForm.SLOPE,legacy.getValue(RoofBlock.FACING));}return of(FLAT_PROFILE,state.getValue(SmartThatchBlock.FACING));}return shape;
    }
    /** Row-major 3x3 owner elevations relative to the centre; 9 means absent. */
    public static ThatchShape select(int[] h){int corners=0;for(int i=0;i<4;i++){int min=0,max=0;int[] n=AROUND[i];for(int j=0;j<3;j++){if(j==2&&h[n[0]]==ABSENT&&h[n[1]]==ABSENT)continue;int y=h[n[j]];if(y==ABSENT)continue;min=Math.min(min,y);max=Math.max(max,y);}corners+=Math.max(0,Math.min(2,min+max+1))*POW3[i];}int mask=0;for(int i=0;i<4;i++){int c=(corners/POW3[i])%3;if(c!=1&&c==(corners/POW3[(i+1)&3])%3&&h[EDGES[i]]!=0)mask|=1<<i;}return lookup(corners*16+mask);}
    private static double template(ThatchForm f,double x,double z){switch(f){case FLAT:return .5;case SLOPE:return 1-z;case EAVE:return 1-.5*z;case HIP:return Math.min(1-x,1-z);case EAVE_HIP:return .5+.5*Math.min(1-x,1-z);case VALLEY:return Math.max(1-x,1-z);case RIDGE:return Math.min(x,1-x);case RIDGE_END:return Math.min(Math.min(x,1-x),z);case VALLEY_RUN:return 1-Math.min(x,1-x);case VALLEY_END:return Math.max(1-Math.min(x,1-x),1-z);case CAP:return Math.min(Math.min(x,1-x),Math.min(z,1-z));case BASIN:return 1-Math.min(Math.min(x,1-x),Math.min(z,1-z));default:return 1;}}
}
