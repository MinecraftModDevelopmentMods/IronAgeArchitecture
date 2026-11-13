package zone.moddev.mc.ironagearchitecture.block;

import java.util.*;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import zone.moddev.mc.ironagearchitecture.ArchitectureConfig;

/** Loaded, coplanar wall sections. A missing chunk is unknown, never a facade edge. */
public final class SmartWallPlanner {
    public static final int MAX_CELLS=2048, MAX_WIDTH=48, MAX_HEIGHT=32;
    private SmartWallPlanner() { }

    public static final class Plan {
        public final Map<BlockPos,Integer> cells;
        public final EnumFacing normal;
        Plan(Map<BlockPos,Integer> cells,EnumFacing normal){this.cells=cells;this.normal=normal;}
    }

    public static Plan survey(World world,BlockPos seed,EnumFacing normal) {
        if(!world.isBlockLoaded(seed)) return null;
        IBlockState source=world.getBlockState(seed);
        if(!(source.getBlock() instanceof SmartWallBlock)) return null;
        SmartWallBlock smart=(SmartWallBlock)source.getBlock();
        double plane=plane(source,world,seed,normal);
        ArrayDeque<BlockPos> pending=new ArrayDeque<>();
        Set<BlockPos> visited=new HashSet<>();
        Map<BlockPos,Integer> kinds=new HashMap<>();
        pending.add(seed);
        EnumFacing tangent=normal.getAxis()==EnumFacing.Axis.Z?EnumFacing.EAST:EnumFacing.SOUTH;
        int minU=coordinate(seed,normal),maxU=minU,minY=seed.getY(),maxY=minY;
        while(!pending.isEmpty()) {
            BlockPos at=pending.removeFirst();
            if(!visited.add(at)||at.getY()<0||at.getY()>=256) continue;
            if(!world.isBlockLoaded(at)) return null;
            int kind=kind(world,at,normal,plane,smart.wood);
            if(kind==0) continue;
            kinds.put(at,kind);
            int u=coordinate(at,normal);
            minU=Math.min(minU,u);maxU=Math.max(maxU,u);minY=Math.min(minY,at.getY());maxY=Math.max(maxY,at.getY());
            if(kinds.size()>MAX_CELLS||maxU-minU>=MAX_WIDTH||maxY-minY>=MAX_HEIGHT) return null;
            pending.add(at.offset(tangent));pending.add(at.offset(tangent.getOpposite()));
            pending.add(at.up());pending.add(at.down());
        }
        int[][] grid=new int[maxY-minY+1][maxU-minU+1];
        for(Map.Entry<BlockPos,Integer> entry:kinds.entrySet())
            grid[entry.getKey().getY()-minY][coordinate(entry.getKey(),normal)-minU]=entry.getValue();
        int[][] framing=SmartFacade.arrange(grid);
        Map<BlockPos,Integer> result=new HashMap<>();
        for(BlockPos pos:kinds.keySet()) {
            IBlockState state=world.getBlockState(pos);
            if(!(state.getBlock() instanceof SmartWallBlock)) continue;
            int code=framing[pos.getY()-minY][coordinate(pos,normal)-minU];
            if(normal==EnumFacing.SOUTH||normal==EnumFacing.WEST)
                code=(code&~(SmartFacade.LEFT|SmartFacade.RIGHT|SmartFacade.RISE|SmartFacade.FALL))
                    |((code&SmartFacade.LEFT)<<1)|((code&SmartFacade.RIGHT)>>1)
                    |((code&SmartFacade.RISE)<<1)|((code&SmartFacade.FALL)>>1);
            BlockPos outside=pos.offset(normal);
            if(!world.isBlockLoaded(outside)) return null;
            result.put(pos,code);
        }
        return new Plan(result,normal);
    }

    private static int coordinate(BlockPos pos,EnumFacing side){return side.getAxis()==EnumFacing.Axis.Z?pos.getX():pos.getZ();}
    private static double plane(IBlockState state,World world,BlockPos pos,EnumFacing side) {
        AxisAlignedBB bounds;
        if(state.getBlock() instanceof FrameBlock&&!(state.getBlock() instanceof SmartWallBlock)) {
            if(state.getBlock() instanceof PartialWallBlock) {
                WallProfile profile=((PartialWallBlock)state.getBlock()).profile;
                EnumFacing facing=state.getValue(FrameBlock.FACING);
                double[] a=WallGeometry.rotate(profile.x0,0,profile.z0,facing),b=WallGeometry.rotate(profile.x1,16,profile.z1,facing);
                bounds=new AxisAlignedBB(Math.min(a[0],b[0])/16,0,Math.min(a[2],b[2])/16,Math.max(a[0],b[0])/16,1,Math.max(a[2],b[2])/16);
            } else bounds=new AxisAlignedBB(0,0,0,1,1,1);
        } else bounds=state.getBoundingBox(world,pos);
        return side==EnumFacing.NORTH?bounds.minZ:side==EnumFacing.SOUTH?bounds.maxZ:side==EnumFacing.WEST?bounds.minX:bounds.maxX;
    }
    private static int kind(World world,BlockPos pos,EnumFacing normal,double plane,String wood) {
        IBlockState state=world.getBlockState(pos);
        if(state.getBlock() instanceof SmartWallBlock) {
            SmartWallBlock block=(SmartWallBlock)state.getBlock();
            return block.wood.equals(wood)&&Math.abs(plane(state,world,pos,normal)-plane)<.001?SmartFacade.WALL:0;
        }
        if(state.getBlock() instanceof WindowBlock) {
            WindowBlock window=(WindowBlock)state.getBlock();
            if(window.wood.equals(wood)&&state.getValue(WindowBlock.FACING).getAxis()==normal.getAxis()) {
                double depth=window.thin?(window.back?8:0):0;
                double back=window.thin?depth+8:16;
                double[] a=WallGeometry.rotate(0,0,depth,state.getValue(WindowBlock.FACING));
                double[] z=WallGeometry.rotate(16,0,back,state.getValue(WindowBlock.FACING));
                double edge=normal==EnumFacing.NORTH?Math.min(a[2],z[2])/16:normal==EnumFacing.SOUTH?Math.max(a[2],z[2])/16
                    :normal==EnumFacing.WEST?Math.min(a[0],z[0])/16:Math.max(a[0],z[0])/16;
                return Math.abs(edge-plane)<.001?SmartFacade.OPENING:0;
            }
        }
        if(state.getBlock() instanceof BlockDoor)
            return SmartFacade.OPENING;
        if(state.getBlock() instanceof FrameBlock&&!(state.getBlock() instanceof JettyFloorBlock)) {
            FrameBlock block=(FrameBlock)state.getBlock();
            if(block.wood!=null&&!wood.equals(block.wood)||state.getValue(FrameBlock.FACING).getAxis()!=normal.getAxis()
                ||Math.abs(plane(state,world,pos,normal)-plane)>.08) return 0;
            if(ArchitectureConfig.buildingMode==zone.moddev.mc.ironagearchitecture.BuildingMode.HYBRID) {
                if(block.family==FrameFamily.POST) return (state.getValue(FrameBlock.PANEL)&1)==0?SmartFacade.POST:SmartFacade.CROSS;
                if(block.family==FrameFamily.TERMINAL) return SmartFacade.POST;
                if(block.family==FrameFamily.RAIL) return (state.getValue(FrameBlock.PANEL)&1)==0?SmartFacade.RAIL:SmartFacade.HEAD;
                if(block.family==FrameFamily.BRACE) {
                    boolean rising=(state.getValue(FrameBlock.PANEL)&1)==0;
                    EnumFacing facing=state.getValue(FrameBlock.FACING);
                    if(facing==EnumFacing.SOUTH||facing==EnumFacing.WEST) rising=!rising;
                    return rising?SmartFacade.RISING:SmartFacade.FALLING;
                }
            }
            return SmartFacade.WALL;
        }
        return 0;
    }
}
