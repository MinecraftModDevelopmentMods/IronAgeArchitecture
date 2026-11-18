package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import zone.moddev.mc.ironagearchitecture.*;
import zone.moddev.mc.ironagearchitecture.block.*;
import zone.moddev.mc.ironagearchitecture.roof.ArchitectWorkbench.Desk;

/** The only world-writing roof planner. Its queue is shared by every dimension and workbench. */
public final class RoofConstruction {
    private static final Set<Desk> JOBS=Collections.newSetFromMap(new WeakHashMap<Desk,Boolean>());
    private static final Map<EntityPlayerMP,Session> SESSIONS=new WeakHashMap<>();
    public static boolean changing;
    public static long changes,lastWorkNanos,maxWorkNanos;
    private static int rotation;
    private static final class Session {Desk desk;boolean preview;long dirty;Session(Desk d){desk=d;}}
    public static boolean floor(World w,BlockPos p){if(!w.isBlockLoaded(p))return false;Block b=w.getBlockState(p).getBlock();return b instanceof JettyFloorBlock&&((JettyFloorBlock)b).shape==FloorShape.FULL;}
    public static void detect(Desk d){RoofPlan settings=d.drawing;World w=d.getWorld();BlockPos start=d.getPos().down();RoofPlan previous=RoofData.get(w).find(start);if(previous!=null&&settings==null){d.drawing=previous;d.automatic=false;d.status="Saved frame selected. Strip its covering before changing it.";return;}RoofPlan p=new RoofPlan();p.minX=p.maxX=start.getX();p.minZ=p.maxZ=start.getZ();p.floorY=start.getY();Block b=w.getBlockState(start).getBlock();if(b instanceof JettyFloorBlock)p.wood=Arrays.asList(ArchitectureBlocks.WOODS).indexOf(((JettyFloorBlock)b).wood);Set<Long> seen=new HashSet<>();ArrayDeque<BlockPos> todo=new ArrayDeque<>();todo.add(start);boolean bounded=false;while(!todo.isEmpty()){BlockPos at=todo.remove();if(!seen.add(at.toLong()))continue;if(Math.abs(at.getX()-start.getX())>31||Math.abs(at.getZ()-start.getZ())>31||seen.size()>4096){bounded=true;break;}if(!floor(w,at))continue;p.minX=Math.min(p.minX,at.getX());p.maxX=Math.max(p.maxX,at.getX());p.minZ=Math.min(p.minZ,at.getZ());p.maxZ=Math.max(p.maxZ,at.getZ());for(EnumFacing f:EnumFacing.HORIZONTALS)todo.add(at.offset(f));}p.alongX=p.width()>p.length();if(p.width()==p.length())p.alongX=true;if(settings!=null){p.id=settings.id;p.pitch=settings.pitch;p.base=settings.base;p.eave=settings.eave;p.gable=settings.gable;p.wood=settings.wood;p.alongX=settings.alongX;p.openings.addAll(settings.openings);}d.drawing=RoofPlanner.solve(p);if(bounded)d.drawing.error="Floor detection reached its limit. Trim the selected rectangle.";d.status=d.drawing.error;d.automatic=true;d.markDirty();}
    public static String validate(Desk d,RoofPlan p,EntityPlayer player){World w=d.getWorld();if(!p.valid())return p.error.isEmpty()?"Refresh the roof drawing to begin.":p.error;if(!p.contains(d.getPos().down()))return "Keep the workbench inside the selected floor area.";for(int x=p.minX;x<=p.maxX;x++)for(int z=p.minZ;z<=p.maxZ;z++)if(x==p.minX||x==p.maxX||z==p.minZ||z==p.maxZ){BlockPos at=new BlockPos(x,p.floorY,z);if(!floor(w,at))return "Complete the outer floor perimeter, or trim the rectangle.";}
        RoofPlan old=RoofData.get(w).find(d.getPos().down());if(old!=null&&!old.id.equals(p.id))return "Select the existing frame before revising this roof.";
        for(RoofPlan other:RoofData.get(w).plans.values())if(!other.id.equals(p.id)&&other.floorY==p.floorY&&other.minX<=p.maxX&&other.maxX>=p.minX&&other.minZ<=p.maxZ&&other.maxZ>=p.minZ)return "This area already belongs to another roof.";
        Set<Long> all=new HashSet<>(p.cells.keySet());if(old!=null)all.addAll(old.cells.keySet());for(long key:all){BlockPos at=BlockPos.fromLong(key);if(!w.isBlockLoaded(at))return "Bring every part of the planned roof into loaded chunks.";IBlockState state=w.getBlockState(at);RoofCell c=RoofData.get(w).at(w,at);boolean own=state.getBlock() instanceof RoofFrameBlock&&c!=null&&c.plan.equals(p.id);if(own&&RoofFrameBlock.cover(state)!=0)return "Strip all roofing before changing the frame.";if(!own&&!w.isAirBlock(at))return "Clear the obstruction at "+at.getX()+", "+at.getY()+", "+at.getZ()+".";if(player!=null&&(!w.isBlockModifiable(player,at)||!player.canPlayerEdit(at,EnumFacing.UP,new ItemStack(ArchitectureBlocks.ROOF_TIMBER))))return "You cannot build at one of the planned positions.";RoofCell next=p.cells.get(key);if(next!=null&&blocked(w,at,next))return "Move clear of the proposed timbers before building.";}
        for(Desk job:new ArrayList<>(JOBS))if(job!=d&&job.active&&job.getWorld()==w&&job.drawing!=null){for(long cell:p.cells.keySet())if(job.drawing.cells.containsKey(cell))return "Another workbench is constructing here.";}
        return "";
    }
    private static boolean blocked(World w,BlockPos at,RoofCell c){RoofMeshes.Mesh m=RoofMeshes.get(c,0);for(Entity entity:w.getEntitiesWithinAABB(Entity.class,new AxisAlignedBB(at,at.add(1,1,1))))if(!entity.isDead&&!entity.noClip&&entity.canBeCollidedWith())for(AxisAlignedBB box:m.boxes)if(box.offset(at).intersectsWith(entity.getEntityBoundingBox()))return true;return false;}
    public static int required(Desk d,RoofPlan p){int n=0;World w=d.getWorld();for(Map.Entry<Long,RoofCell> e:p.cells.entrySet())if(e.getValue().timber()){BlockPos at=BlockPos.fromLong(e.getKey());RoofCell old=RoofData.get(w).at(w,at);IBlockState s=w.getBlockState(at);if(!(s.getBlock() instanceof RoofFrameBlock)||old==null||!old.plan.equals(p.id)||!old.timber()||s.getValue(RoofFrameBlock.WOOD)!=p.wood)n++;}return n;}
    public static int available(Desk d,int wood){int n=d.count(ArchitectureBlocks.ROOF_TIMBER,wood);if(ArchitectureConfig.survivalEras.permits(java.util.EnumSet.allOf(Era.class)))n+=2*Math.min(d.count(Item.getItemFromBlock(Blocks.PLANKS),wood),d.count(Items.STICK,-1));return n;}
    public static boolean start(Desk d,EntityPlayer player){if(d.active)return false;RoofPlan p=d.drawing;if(p==null)return false;RoofPlanner.solve(p);String error=validate(d,p,player);if(!error.isEmpty()){d.status=error;return false;}int cost=required(d,p);boolean free=player!=null&&player.capabilities.isCreativeMode;if(!free&&available(d,p.wood)<cost){d.status="Supply "+cost+" matching timber units (or planks and sticks).";return false;}
        d.prior=RoofData.get(d.getWorld()).find(d.getPos().down());d.present.clear();Set<Long> all=new LinkedHashSet<>();if(d.prior!=null)all.addAll(d.prior.cells.keySet());all.addAll(p.cells.keySet());for(long key:all){BlockPos at=BlockPos.fromLong(key);if(d.getWorld().getBlockState(at).getBlock() instanceof RoofFrameBlock)d.present.add(key);}
        if(!free){int units=Math.min(cost,d.count(ArchitectureBlocks.ROOF_TIMBER,p.wood));d.take(ArchitectureBlocks.ROOF_TIMBER,p.wood,units);int pairs=(cost-units+1)/2;d.take(Item.getItemFromBlock(Blocks.PLANKS),p.wood,pairs);d.take(Items.STICK,-1,pairs);if(units+pairs*2>cost)d.give(new ItemStack(ArchitectureBlocks.ROOF_TIMBER,1,p.wood));}d.creative=free;d.remaining=free?0:cost;
        d.operations.clear();d.operations.addAll(all);d.index=0;d.active=true;d.preview=false;for(Session session:SESSIONS.values())if(session.desk==d)session.preview=false;d.status="Building frame...";RoofData.get(d.getWorld()).savePlan(p);d.markDirty();enqueue(d);return true;
    }
    public static void enqueue(Desk d){JOBS.add(d);}
    public static boolean busy(World w,UUID plan){for(Desk d:new ArrayList<>(JOBS))if(!d.isInvalid()&&d.active&&d.getWorld()==w&&d.drawing!=null&&d.drawing.id.equals(plan))return true;return false;}
    public static void cancel(Desk d){JOBS.remove(d);if(d.active&&!d.creative&&d.remaining>0)d.give(new ItemStack(ArchitectureBlocks.ROOF_TIMBER,d.remaining,d.drawing.wood));d.remaining=0;d.active=false;d.prior=null;d.operations.clear();d.present.clear();d.index=0;d.status="Construction cancelled. Completed sections remain in place.";d.markDirty();}
    private static boolean step(Desk d){World w=d.getWorld();if(d.isInvalid()||!w.isBlockLoaded(d.getPos())||w.getTileEntity(d.getPos())!=d)return false;if(d.index>=d.operations.size()){d.active=false;d.status="Frame complete. Fit thatch, slate or tiles by hand.";d.prior=null;d.present.clear();d.remaining=0;d.markDirty();return true;}long key=d.operations.get(d.index);BlockPos at=BlockPos.fromLong(key);if(!w.isBlockLoaded(at)){d.status="Paused: a roof chunk is unloaded.";return false;}RoofCell old=RoofData.get(w).at(w,at),next=d.drawing.cells.get(key);IBlockState state=w.getBlockState(at);boolean present=d.present.contains(key),own=state.getBlock() instanceof RoofFrameBlock&&old!=null&&old.plan.equals(d.drawing.id);if(present&&!own||!present&&!w.isAirBlock(at)||own&&RoofFrameBlock.cover(state)>0){d.status="Paused: clear "+at.getX()+", "+at.getY()+", "+at.getZ()+" or cancel.";return false;}if(next!=null&&blocked(w,at,next)){d.status="Paused: someone is in the way of a timber.";return false;}
        boolean reuse=own&&old.timber()&&next!=null&&next.timber()&&state.getValue(RoofFrameBlock.WOOD)==d.drawing.wood;
        changing=true;try{if(own&&old.timber()&&!reuse&&!d.creative)d.give(new ItemStack(ArchitectureBlocks.ROOF_TIMBER,1,state.getValue(RoofFrameBlock.WOOD)));if(next==null){RoofData.get(w).put(w,at,null);w.setBlockToAir(at);}else{RoofData.get(w).put(w,at,next);w.setBlockState(at,RoofFrameBlock.state(d.drawing.wood,0),3);if(next.timber()&&!reuse&&!d.creative){if(d.remaining<=0)throw new IllegalStateException("Missing reserved roof timber");d.remaining--;}}}finally{changing=false;}d.index++;changes++;d.status="Building frame: "+d.index+" / "+d.operations.size();d.markDirty();return true;
    }
    public static ItemStack coverItem(int cover){if(cover==1&&ArchitectureBlocks.SMART_THATCH!=null)return new ItemStack(ArchitectureBlocks.SMART_THATCH);String name=cover==2?"slate_roof":cover==3?"clay_tile_roof":null;Block b=name==null?null:Block.getBlockFromName(IronAgeArchitecture.MODID+":"+name);return b==null||b==Blocks.AIR?null:new ItemStack(b);}
    public static boolean cover(World w,BlockPos p,EntityPlayer player,ItemStack held,EnumHand hand){RoofCell c=RoofData.get(w).at(w,p);if(c==null||busy(w,c.plan))return true;IBlockState original=w.getBlockState(p);int current=RoofFrameBlock.cover(original),desired=0;if(held!=null){for(int i=1;i<=3;i++){ItemStack item=coverItem(i);if(item!=null&&held.isItemEqual(item))desired=i;}}boolean strip=player.isSneaking()&&held==null;if(strip&&current==0||!strip&&(desired==0||current!=0))return false;if(!c.coverable()){player.sendStatusMessage(new net.minecraft.util.text.TextComponentString("Fit roofing to a sloping rafter section."));return true;}List<BlockPos> section=new ArrayList<>();for(int y=Math.max(0,p.getY()-48);y<=Math.min(255,p.getY()+48);y++){BlockPos at=new BlockPos(p.getX(),y,p.getZ());if(!w.isBlockLoaded(at))continue;RoofCell part=RoofData.get(w).at(w,at);if(part!=null&&part.plan.equals(c.plan)&&part.owner.equals(c.owner)&&part.coverable()&&w.getBlockState(at).getBlock() instanceof RoofFrameBlock){if(!w.isBlockModifiable(player,at)||!player.canPlayerEdit(at,EnumFacing.UP,held))return true;if(!strip){for(Entity e:w.getEntitiesWithinAABB(Entity.class,new AxisAlignedBB(at,at.add(1,1,1))))if(!e.isDead&&!e.noClip&&e.canBeCollidedWith())for(AxisAlignedBB b:RoofMeshes.get(part,desired).boxes)if(b.offset(at).intersectsWith(e.getEntityBoundingBox())){player.sendStatusMessage(new net.minecraft.util.text.TextComponentString("Move clear before fitting the covering."));return true;}}section.add(at);}}
        if(section.isEmpty())return true;changing=true;try{for(BlockPos at:section)w.setBlockState(at,RoofFrameBlock.state(original.getValue(RoofFrameBlock.WOOD),strip?0:desired),3);}finally{changing=false;}if(strip){ItemStack item=coverItem(current);if(item!=null&&!player.capabilities.isCreativeMode){if(!player.inventory.addItemStackToInventory(item))player.dropItem(item,false);}}else if(!player.capabilities.isCreativeMode){held.stackSize--;if(held.stackSize<=0)player.setHeldItem(hand,null);}w.playSound(null,p,SoundEvents.BLOCK_WOOD_PLACE,SoundCategory.BLOCKS,1,1);return true;
    }
    public static void broken(World w,BlockPos p,IBlockState state){
        RoofData data=RoofData.get(w);RoofCell old=data.at(w,p);if(old==null)return;
        data.put(w,p,null);
        if(!old.coverable())return;
        if(!old.timber()&&!p.equals(old.owner)&&(w.isAirBlock(p)||w.getBlockState(p).getBlock()==Blocks.FIRE)){
            RoofCell owner=data.at(w,old.owner);if(owner!=null&&owner.plan.equals(old.plan)&&w.getBlockState(old.owner).getBlock() instanceof RoofFrameBlock)w.destroyBlock(old.owner,false);
            return;
        }
        List<BlockPos> column=new ArrayList<>();BlockPos owner=null;
        for(int y=Math.max(0,p.getY()-48);y<=Math.min(255,p.getY()+48);y++){
            BlockPos at=new BlockPos(p.getX(),y,p.getZ());if(at.equals(p)||!w.isBlockLoaded(at))continue;
            RoofCell c=data.at(w,at);if(c==null||!c.plan.equals(old.plan)||!c.owner.equals(old.owner)||!(w.getBlockState(at).getBlock() instanceof RoofFrameBlock))continue;
            column.add(at);if(c.timber()&&(owner==null||at.getY()>owner.getY()))owner=at;
        }
        changing=true;try{for(BlockPos at:column){RoofCell c=data.at(w,at);IBlockState s=w.getBlockState(at);
            if(!c.timber()){data.put(w,at,null);w.setBlockToAir(at);}
            else{data.put(w,at,new RoofCell(c.plan,owner==null?at:owner,c.timber,c.skin));w.setBlockState(at,RoofFrameBlock.state(s.getValue(RoofFrameBlock.WOOD),0),3);}
        }}finally{changing=false;}
    }
    public static void session(EntityPlayerMP p,Desk d){Session s=SESSIONS.get(p);if(s==null||s.desk!=d)SESSIONS.put(p,new Session(d));}
    public static boolean previewing(EntityPlayerMP p,Desk d){Session s=SESSIONS.get(p);return s!=null&&s.desk==d&&s.preview;}
    public static void preview(EntityPlayerMP p,Desk d,boolean on){session(p,d);SESSIONS.get(p).preview=on;d.preview=on;}
    public static final class Events {
        @SubscribeEvent public void edited(BlockEvent e){if(e.getWorld().isRemote||changing||!(e instanceof BlockEvent.BreakEvent||e instanceof BlockEvent.PlaceEvent))return;for(Map.Entry<EntityPlayerMP,Session> entry:new ArrayList<>(SESSIONS.entrySet())){Session s=entry.getValue();RoofPlan p=s.desk.drawing;if(s.preview&&p!=null&&s.desk.getWorld()==e.getWorld()&&e.getPos().getX()>=p.minX-2&&e.getPos().getX()<=p.maxX+2&&e.getPos().getZ()>=p.minZ-2&&e.getPos().getZ()<=p.maxZ+2)s.dirty=e.getWorld().getTotalWorldTime()+4;}}
        @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END)return;long start=System.nanoTime(),deadline=start+2000000;int changed=0;List<Desk> jobs=new ArrayList<>(JOBS);if(!jobs.isEmpty())Collections.rotate(jobs,-(rotation++%jobs.size()));for(Desk d:jobs){if(d.isInvalid()||!d.active){JOBS.remove(d);continue;}while(changed<16&&System.nanoTime()<deadline&&d.active){if(!step(d))break;changed++;}if(!d.active)JOBS.remove(d);if(changed>=16||System.nanoTime()>=deadline)break;}lastWorkNanos=System.nanoTime()-start;if(!jobs.isEmpty())maxWorkNanos=Math.max(maxWorkNanos,lastWorkNanos);for(Map.Entry<EntityPlayerMP,Session> entry:new ArrayList<>(SESSIONS.entrySet())){EntityPlayerMP p=entry.getKey();Session s=entry.getValue();if(p.isDead||p.connection==null||s.desk.isInvalid()||p.world!=s.desk.getWorld()||p.getDistanceSq(s.desk.getPos())>4096){SESSIONS.remove(p);continue;}if(s.preview&&s.dirty>0&&p.world.getTotalWorldTime()>=s.dirty&&!s.desk.active){s.dirty=0;if(s.desk.automatic)detect(s.desk);else RoofPlanner.solve(s.desk.drawing);RoofNetwork.reply(p,s.desk,s.preview);}}}
    }
}
