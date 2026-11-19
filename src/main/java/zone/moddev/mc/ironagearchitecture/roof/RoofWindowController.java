package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/** Event-driven window operation, including fragments hosted by another roof cell. */
public final class RoofWindowController {
    private static final Map<World,Set<BlockPos>> BLOCKED=new WeakHashMap<>();
    private static final Map<World,Map<BlockPos,Long>> RETRY=new WeakHashMap<>();
    private static void remember(World world,RoofWindow n,boolean entity){BLOCKED.computeIfAbsent(world,k->new HashSet<>()).add(n.owner);if(entity)RETRY.computeIfAbsent(world,k->new HashMap<>()).put(n.owner,world.getTotalWorldTime()+5);}
    private static final Map<World,ArrayDeque<BlockPos>> REPAIR=new WeakHashMap<>();
    public static void loaded(World w,int x,int z){if(w.isRemote)return;ArrayDeque<BlockPos> jobs=REPAIR.computeIfAbsent(w,k->new ArrayDeque<>());for(int a=x-1;a<=x+1;a++)for(int b=z-1;b<=z+1;b++)for(Map.Entry<Long,RoofCell> e:RoofData.get(w).chunk(w,a,b).entrySet())if(!e.getValue().windows.isEmpty()){BlockPos at=BlockPos.fromLong(e.getKey());if(!jobs.contains(at))jobs.add(at);}}
    private static void repair(World w,BlockPos at){if(!w.isBlockLoaded(at))return;RoofCell c=RoofData.get(w).at(w,at);if(c==null||RoofConstruction.busy(w,c.plan))return;for(RoofWindow n:new ArrayList<>(c.windows)){
        if(!w.isBlockLoaded(n.owner))continue;RoofCell root=RoofData.get(w).at(w,n.owner);RoofWindow owner=null;if(root!=null)for(RoofWindow a:root.windows)if(a.id.equals(n.id)&&a.owner.equals(n.owner))owner=a;
        if(RoofData.get(w).removed(c.plan,n.id)||owner==null||!RoofDesignInteraction.host(w.getBlockState(n.owner))){List<RoofPart> ps=new ArrayList<>(c.parts);ps.removeIf(p->p.id.equals(n.id));List<RoofWindow> ws=new ArrayList<>(c.windows);ws.removeIf(a->a.id.equals(n.id));RoofConstruction.changing=true;try{RoofDesignInteraction.write(w,at,new RoofCell(c.plan,c.owner,ps,ws));}finally{RoofConstruction.changing=false;}continue;}
        boolean complete=true,missing=false;for(BlockPos p:RoofWindowGeometry.candidates(owner)){if(!w.isBlockLoaded(p)){complete=false;break;}RoofCell a=RoofData.get(w).at(w,p);boolean found=false;if(a!=null)for(RoofWindow b:a.windows)if(b.id.equals(owner.id)&&b.open==owner.open)found=true;if(!found&&!RoofWindowGeometry.mesh(owner.slice(p)).faces.isEmpty())missing=true;}
        if(complete&&missing)set(w,root,owner,owner.open,owner.powered,null,false);if(complete)neighbor(w,n.owner);
    }}
    private RoofWindowController(){}
    private static void message(EntityPlayer p,String s){if(p!=null)p.sendStatusMessage(new TextComponentString(s));}
    public static boolean toggle(World w,BlockPos at,String id,EntityPlayer player){RoofCell c=RoofData.get(w).at(w,at);if(c==null)return false;for(RoofWindow n:c.windows)if(n.id.equals(id))return set(w,c,n,!n.open,powered(w,c,n),player,true);return false;}
    private static boolean powered(World w,RoofCell c,RoofWindow n){RoofPlan p=RoofData.get(w).interaction(c.plan,n.owner);if(p==null)return n.powered;boolean power=false;Set<BlockPos> visited=new HashSet<>();for(RoofCell other:RoofData.get(w).owned(w,p).values())for(RoofWindow a:other.windows)if(column(n,a)&&visited.add(a.owner))power|=w.isBlockLoaded(a.owner)&&w.isBlockPowered(a.owner);return power;}
    private static boolean column(RoofWindow a,RoofWindow b){return a.id.equals(b.id)||a.vertical&&b.vertical&&a.owner.getX()==b.owner.getX()&&a.owner.getZ()==b.owner.getZ()&&a.style==b.style&&a.facing==b.facing&&a.right==b.right&&Math.abs(a.owner.getY()-b.owner.getY())<=1;}
    public static boolean set(World w,RoofCell source,RoofWindow target,boolean open,boolean power,EntityPlayer player,boolean sound){if(w.isRemote||RoofConstruction.busy(w,source.plan)||RoofData.get(w).removed(source.plan,target.id))return false;RoofPlan p=RoofData.get(w).interaction(source.plan,target.owner);if(p==null)return false;Map<Long,RoofCell> all=RoofData.get(w).owned(w,p);Map<String,RoofWindow> units=new LinkedHashMap<>();for(RoofCell c:all.values())for(RoofWindow n:c.windows)if(column(target,n))units.putIfAbsent(n.id,n.slice(n.owner));if(units.isEmpty())return false;
        Map<Long,RoofCell> next=new TreeMap<>();for(Map.Entry<Long,RoofCell> e:all.entrySet()){RoofCell c=e.getValue();List<RoofPart> ps=new ArrayList<>();List<RoofWindow> ws=new ArrayList<>();for(RoofPart part:c.parts)if(!units.containsKey(part.id))ps.add(part);for(RoofWindow n:c.windows)if(!units.containsKey(n.id))ws.add(n);if(ps.size()!=c.parts.size()||ws.size()!=c.windows.size())next.put(e.getKey(),new RoofCell(c.plan,c.owner,ps,ws));}
        for(RoofWindow original:units.values()){
            if(!w.isBlockLoaded(original.owner)){message(player,"Bring every window section into loaded chunks.");return false;}
            RoofWindow n=original.state(open,power);
            if(original.open!=open)for(BlockPos at:RoofWindowGeometry.sweepCandidates(original)){
                List<AxisAlignedBB> sweep=RoofWindowGeometry.sweepBoxes(original.slice(at));if(sweep.isEmpty())continue;
                if(!w.isBlockLoaded(at)){message(player,"The window's swing reaches an unloaded chunk.");return false;}
                RoofCell base=next.containsKey(at.toLong())?next.get(at.toLong()):all.get(at.toLong());
                if(base==null&&!w.isAirBlock(at)){message(player,"Clear the window's swing first.");if(power&&open)remember(w,target,false);return false;}
                for(AxisAlignedBB a:sweep){AxisAlignedBB box=a.expand(.021,.021,.021);if(base!=null)for(AxisAlignedBB b:RoofMeshes.get(base,0).boxes)if(box.intersectsWith(b)){message(player,"Clear the neighbouring roof from the window's swing.");remember(w,target,false);return false;}for(Entity entity:w.getEntitiesWithinAABB(Entity.class,box.offset(at)))if(!entity.isDead&&!entity.noClip&&entity.canBeCollidedWith()){message(player,"Move clear of the window's swing.");if(power&&open)remember(w,target,true);return false;}}
            }

            for(BlockPos at:RoofWindowGeometry.candidates(n)){
                RoofWindow slice=n.slice(at);RoofMeshes.Mesh mesh=RoofWindowGeometry.mesh(slice);if(mesh.faces.isEmpty())continue;
                if(!w.isBlockLoaded(at)){message(player,"The window's opening space is unloaded.");return false;}
                RoofCell c=next.containsKey(at.toLong())?next.get(at.toLong()):all.get(at.toLong());IBlockState state=w.getBlockState(at);if(c==null&&!w.isAirBlock(at)){message(player,"Clear the space outside the window first.");return false;}if(c==null)c=new RoofCell(source.plan,at,Collections.emptyList(),Collections.emptyList());if(player!=null&&(!w.isBlockModifiable(player,at)||!player.canPlayerEdit(at,EnumFacing.UP,null)))return false;
                if(open)for(AxisAlignedBB a:RoofWindowGeometry.movingBoxes(slice)){AxisAlignedBB inner=a.expand(-.002,-.002,-.002);if(inner.maxX<=inner.minX||inner.maxY<=inner.minY||inner.maxZ<=inner.minZ)continue;for(AxisAlignedBB b:RoofMeshes.get(c,0).boxes)if(inner.intersectsWith(b)){message(player,"The window cannot clear the neighbouring roof.");return false;}for(Entity e:w.getEntitiesWithinAABB(Entity.class,inner.offset(at)))if(!e.isDead&&!e.noClip&&e.canBeCollidedWith()){message(player,"Move clear of the opening window.");return false;}}
                List<RoofPart> ps=new ArrayList<>(c.parts);List<RoofWindow> ws=new ArrayList<>(c.windows);ps.add(new RoofPart(n.id,n.owner,RoofPart.WINDOW,n.style,new int[0]));ws.add(slice);next.put(at.toLong(),new RoofCell(c.plan,c.owner,ps,ws));
            }
        }
        RoofConstruction.changing=true;try{for(Map.Entry<Long,RoofCell> e:next.entrySet())RoofDesignInteraction.write(w,BlockPos.fromLong(e.getKey()),e.getValue());}finally{RoofConstruction.changing=false;}
        Set<BlockPos> waiting=BLOCKED.get(w);if(waiting!=null)for(RoofWindow n:units.values())waiting.remove(n.owner);
        if(sound&&target.open!=open)w.playSound(null,target.owner,open?SoundEvents.BLOCK_WOODEN_DOOR_OPEN:SoundEvents.BLOCK_WOODEN_DOOR_CLOSE,SoundCategory.BLOCKS,1,1);return true;
    }
    public static final class Events {
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent public void edited(net.minecraftforge.event.world.BlockEvent e){if(e.getWorld().isRemote||RoofConstruction.changing)return;Set<BlockPos> waiting=BLOCKED.get(e.getWorld());if(waiting!=null)for(BlockPos root:new ArrayList<>(waiting))if(Math.abs(root.getX()-e.getPos().getX())<=4&&Math.abs(root.getZ()-e.getPos().getZ())<=4&&Math.abs(root.getY()-e.getPos().getY())<=6)RETRY.computeIfAbsent(e.getWorld(),k->new HashMap<>()).put(root,e.getWorld().getTotalWorldTime()+1);}
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent public void tick(net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent e){if(e.phase!=net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END||e.world.isRemote)return;ArrayDeque<BlockPos> repairs=REPAIR.get(e.world);if(repairs!=null)for(int i=0;i<8&&!repairs.isEmpty();i++)repair(e.world,repairs.removeFirst());Map<BlockPos,Long> jobs=RETRY.get(e.world);if(jobs==null||jobs.isEmpty())return;int n=0;for(Map.Entry<BlockPos,Long> job:new ArrayList<>(jobs.entrySet()))if(job.getValue()<=e.world.getTotalWorldTime()){jobs.remove(job.getKey());if(e.world.isBlockLoaded(job.getKey()))neighbor(e.world,job.getKey());if(++n==8)break;}}
    }
    public static void neighbor(World w,BlockPos at){RoofCell c=RoofData.get(w).at(w,at);if(c==null)return;Set<String> seen=new HashSet<>();for(RoofWindow n:c.windows)if(seen.add(n.id)){boolean power=powered(w,c,n);if(power!=n.powered)set(w,c,n,power,power,null,true);}}
}
