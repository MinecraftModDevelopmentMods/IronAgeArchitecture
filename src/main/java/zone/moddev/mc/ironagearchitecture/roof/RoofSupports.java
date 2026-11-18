package zone.moddev.mc.ironagearchitecture.roof;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.block.*;

/** One bounded ring around the detected floor; never a building-wide support survey. */
public final class RoofSupports {
    private RoofSupports(){}
    private static boolean wall(World w,BlockPos at){if(!w.isBlockLoaded(at))return false;IBlockState s=w.getBlockState(at);return !(s.getBlock() instanceof JettyFloorBlock)&&!(s.getBlock() instanceof RoofFrameBlock)&&!(s.getBlock() instanceof RoofWindowBlock)&&(s.isFullCube()||s.getBlock() instanceof SmartCobBlock||s.getBlock() instanceof FrameBlock&&!(s.getBlock() instanceof PartialWallBlock));}
    public static boolean ring(World w,RoofPlan p){for(int x=p.minX-1;x<=p.maxX+1;x++)if(!wall(w,new BlockPos(x,p.floorY,p.minZ-1))||!wall(w,new BlockPos(x,p.floorY,p.maxZ+1)))return false;for(int z=p.minZ;z<=p.maxZ;z++)if(!wall(w,new BlockPos(p.minX-1,p.floorY,z))||!wall(w,new BlockPos(p.maxX+1,p.floorY,z)))return false;return true;}
    public static String validate(World w,RoofPlan p){for(int x=p.minX;x<=p.maxX;x++)for(int z=p.minZ;z<=p.maxZ;z++)if(x==p.minX||x==p.maxX||z==p.minZ||z==p.maxZ)if(!RoofConstruction.floor(w,new BlockPos(x,p.floorY,z)))return "Complete the outer floor perimeter, or trim the drawing.";if(p.wallInset==1&&!ring(w,p))return "Complete the surrounding wall course for an inset attic floor.";return "";}
    public static void detect(World w,RoofPlan p){p.wallInset=p.seat==1?1:p.seat==2?0:ring(w,p)?1:0;RoofGables.detect(w,p);}
}
