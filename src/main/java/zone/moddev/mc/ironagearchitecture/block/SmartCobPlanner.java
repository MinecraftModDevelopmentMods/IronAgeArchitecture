package zone.moddev.mc.ironagearchitecture.block;

import java.awt.geom.*;
import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Bounded connected wall surveys, solved in horizontal courses and clipped back to each owner. */
public final class SmartCobPlanner {
    public static final int MAX_CELLS=2048,MAX_WIDTH=48,MAX_HEIGHT=32;
    private SmartCobPlanner() { }
    public static Map<BlockPos,SmartCobProfile> survey(World world,BlockPos seed) {
        if(!world.isBlockLoaded(seed)||!(world.getBlockState(seed).getBlock() instanceof SmartCobBlock)) return null;
        Set<BlockPos> visited=new HashSet<>();ArrayDeque<BlockPos> pending=new ArrayDeque<>();pending.add(seed);
        Map<Integer,Set<Long>> layers=new TreeMap<>();Map<Integer,Area> contacts=new HashMap<>();
        int minX=seed.getX(),maxX=minX,minZ=seed.getZ(),maxZ=minZ,minY=seed.getY(),maxY=minY;
        while(!pending.isEmpty()) {
            BlockPos at=pending.removeFirst();if(!visited.add(at)||at.getY()<0||at.getY()>255) continue;
            if(!world.isBlockLoaded(at)) return null;
            if(!(world.getBlockState(at).getBlock() instanceof SmartCobBlock)) continue;
            minX=Math.min(minX,at.getX());maxX=Math.max(maxX,at.getX());minZ=Math.min(minZ,at.getZ());maxZ=Math.max(maxZ,at.getZ());minY=Math.min(minY,at.getY());maxY=Math.max(maxY,at.getY());
            Set<Long> row=layers.computeIfAbsent(at.getY(),y->new HashSet<>());row.add(SmartCobGeometry.key(at.getX(),at.getZ()));
            int count=0;for(Set<Long> layer:layers.values()) count+=layer.size();
            if(count>MAX_CELLS||maxX-minX>=MAX_WIDTH||maxZ-minZ>=MAX_WIDTH||maxY-minY>=MAX_HEIGHT) return null;
            for(EnumFacing side:EnumFacing.values()) {
                BlockPos next=at.offset(side);if(next.getY()<0||next.getY()>255) continue;
                if(!world.isBlockLoaded(next)) return null;
                IBlockState neighbour=world.getBlockState(next);
                if(neighbour.getBlock() instanceof SmartCobBlock) pending.add(next);
                // An eave beside the top course covers the wall from above. It does
                // not require a flush vertical joint and must not square off every course.
                else if(side.getAxis().isHorizontal()&&!(neighbour.getBlock() instanceof RoofBlock)&&!neighbour.getBlock().isAir(neighbour,world,next)) {
                    double x=at.getX(),z=at.getZ(),w=1,d=1;
                    // Keep the joint's central face fixed, while its two exposed
                    // edges retain the common skin. Full-depth patches would leave
                    // one-pixel pilasters beside every door and window.
                    if(side.getAxis()==EnumFacing.Axis.X) {z+=SmartCobGeometry.SKIN;d-=2*SmartCobGeometry.SKIN;w=.5;if(side==EnumFacing.EAST)x+=.5;}
                    else {x+=SmartCobGeometry.SKIN;w-=2*SmartCobGeometry.SKIN;d=.5;if(side==EnumFacing.SOUTH)z+=.5;}
                    contacts.computeIfAbsent(at.getY(),y->new Area()).add(new Area(new Rectangle2D.Double(x,z,w,d)));
                }
            }
        }
        Set<Long> columns=new HashSet<>();for(Set<Long> layer:layers.values())columns.addAll(layer);
        Map<Integer,Set<Long>> structural=new HashMap<>();Map<Set<Long>,Area> fixedByBand=new HashMap<>();
        for(Map.Entry<Integer,Set<Long>> layer:layers.entrySet()) {
            int y=layer.getKey();Set<Long> plan=new HashSet<>(layer.getValue());
            for(long column:columns)if(!plan.contains(column)) {
                boolean above=false,below=false;
                for(int d=1;d<=3;d++){above|=layers.getOrDefault(y+d,Collections.emptySet()).contains(column);below|=layers.getOrDefault(y-d,Collections.emptySet()).contains(column);}
                BlockPos at=new BlockPos(SmartCobGeometry.x(column),y,SmartCobGeometry.z(column));IBlockState gap=world.getBlockState(at);
                boolean aperture=gap.getBlock() instanceof net.minecraft.block.BlockDoor||gap.getBlock() instanceof WindowBlock||gap.getMaterial()==net.minecraft.block.material.Material.GLASS;
                boolean doorway=y-minY<=2&&world.getBlockState(new BlockPos(at.getX(),minY-1,at.getZ())).isFullCube();
                if(above&&(below||aperture||doorway))plan.add(column);
            }
            structural.put(y,plan);fixedByBand.computeIfAbsent(plan,k->new Area()).add(contacts.getOrDefault(y,new Area()));
        }
        Map<BlockPos,SmartCobProfile> result=new HashMap<>();Map<Set<Long>,SmartCobGeometry.Section> shared=new HashMap<>();
        for(Map.Entry<Integer,Set<Long>> layer:layers.entrySet()) {
            Set<Long> plan=structural.get(layer.getKey());SmartCobGeometry.Section base=shared.get(plan);
            if(base==null){base=SmartCobGeometry.solve(plan,fixedByBand.get(plan));shared.put(plan,base);}
            SmartCobGeometry.Section section=SmartCobGeometry.slice(base,layer.getValue());
            for(Map.Entry<Long,SmartCobProfile> cell:section.profiles.entrySet())result.put(new BlockPos(SmartCobGeometry.x(cell.getKey()),layer.getKey(),SmartCobGeometry.z(cell.getKey())),cell.getValue());
        }
        return result;
    }
}
