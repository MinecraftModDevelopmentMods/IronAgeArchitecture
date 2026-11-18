package zone.moddev.mc.ironagearchitecture.roof;

import java.util.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraftforge.fml.common.network.*;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import zone.moddev.mc.ironagearchitecture.IronAgeArchitecture;
import zone.moddev.mc.ironagearchitecture.roof.ArchitectWorkbench.*;

/** Small validated GUI requests; bounded chunk snapshots are drained separately from building. */
public final class RoofNetwork {
    private static final SimpleNetworkWrapper CHANNEL=NetworkRegistry.INSTANCE.newSimpleChannel("iaa_roof_desk");
    private static final class Pending {boolean replace;final Map<Long,RoofCell> cells=new LinkedHashMap<>();}
    private static final Map<EntityPlayerMP,LinkedHashMap<Long,Pending>> DIRTY=new WeakHashMap<>();
    private static int rotation;
    private static final Map<EntityPlayerMP,ArrayDeque<Snapshot>> OUT=new WeakHashMap<>();
    private static final Map<EntityPlayerMP,Long> LAST=new WeakHashMap<>();
    public static void register(){CHANNEL.registerMessage(RequestHandler.class,Request.class,0,Side.SERVER);CHANNEL.registerMessage(ReplyHandler.class,Reply.class,1,Side.CLIENT);CHANNEL.registerMessage(SnapshotHandler.class,Snapshot.class,2,Side.CLIENT);net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(new Events());}
    public static void request(BlockPos pos,int action,RoofPlan plan,boolean preview){Request r=new Request();r.pos=pos;r.action=action;r.preview=preview;if(plan!=null){r.settings=plan.write(new NBTTagCompound());r.settings.removeTag("cells");}CHANNEL.sendToServer(r);}
    public static boolean pending(EntityPlayerMP p){return DIRTY.containsKey(p)||OUT.containsKey(p);}
    public static void reply(EntityPlayerMP player,Desk d,boolean preview){Reply r=new Reply();r.dimension=player.dimension;r.pos=d.getPos();r.plan=d.drawing==null?null:d.drawing.copy();r.preview=preview;r.active=d.active;r.canBuild=!d.active&&d.drawing!=null&&RoofConstruction.validate(d,d.drawing,player).isEmpty()&&(player.capabilities.isCreativeMode||RoofConstruction.available(d,d.drawing.wood)>=RoofConstruction.required(d,d.drawing));r.status=d.active?d.status:d.drawing==null?d.status:RoofConstruction.validate(d,d.drawing,player);if(r.status.isEmpty())r.status="Ready: "+RoofConstruction.required(d,d.drawing)+" timber units; "+RoofConstruction.available(d,d.drawing.wood)+" supplied.";if(!d.status.isEmpty()&&(d.status.startsWith("Supply")||d.status.startsWith("Construction cancelled")))r.status=d.status;r.index=d.index;r.total=d.operations.size();CHANNEL.sendTo(r,player);}
    public static void changed(World w,BlockPos pos,RoofCell c){for(net.minecraft.entity.player.EntityPlayer p:w.playerEntities)if(p instanceof EntityPlayerMP&&((WorldServer)w).getPlayerChunkMap().isPlayerWatchingChunk((EntityPlayerMP)p,pos.getX()>>4,pos.getZ()>>4))pending((EntityPlayerMP)p,pos.getX()>>4,pos.getZ()>>4).cells.put(pos.toLong(),c);}
    private static Pending pending(EntityPlayerMP p,int x,int z){return DIRTY.computeIfAbsent(p,k->new LinkedHashMap<>()).computeIfAbsent(RoofPlan.column(x,z),k->new Pending());}
    public static void snapshot(EntityPlayerMP p,int x,int z,Map<Long,RoofCell> cells){Pending next=pending(p,x,z);next.replace=true;next.cells.clear();}
    public static void unwatch(EntityPlayerMP p,int x,int z){Map<Long,Pending> q=DIRTY.get(p);if(q!=null)q.remove(RoofPlan.column(x,z));ArrayDeque<Snapshot> out=OUT.get(p);if(out!=null)out.removeIf(s->s.x==x&&s.z==z);}
    public static final class Request implements IMessage {
        BlockPos pos;int action;boolean preview;NBTTagCompound settings=new NBTTagCompound();
        public Request(){}
        public void toBytes(ByteBuf b){b.writeLong(pos.toLong());b.writeByte(action);b.writeBoolean(preview);ByteBufUtils.writeTag(b,settings);}
        public void fromBytes(ByteBuf b){if(b.readableBytes()>24576)throw new IllegalArgumentException("Oversized roof request");pos=BlockPos.fromLong(b.readLong());action=b.readUnsignedByte();preview=b.readBoolean();settings=ByteBufUtils.readTag(b);if(settings==null||settings.hasKey("cells")||action>4)throw new IllegalArgumentException("Invalid roof request");}
    }
    public static final class RequestHandler implements IMessageHandler<Request,IMessage> {
        public IMessage onMessage(Request r,MessageContext context){EntityPlayerMP p=context.getServerHandler().playerEntity; p.getServerWorld().addScheduledTask(()->{
            if(!(p.openContainer instanceof Storage))return;Desk d=((Storage)p.openContainer).desk;if(!d.getPos().equals(r.pos)||!d.isUsableByPlayer(p))return;long now=p.world.getTotalWorldTime();if(r.action==0&&LAST.getOrDefault(p,Long.MIN_VALUE/2)+2>now)return;LAST.put(p,now);RoofConstruction.session(p,d);
            try{if(r.action==3){RoofConstruction.cancel(d);}else if(!d.active){if(r.action==4||d.drawing==null){RoofConstruction.detect(d);}else if(r.settings.hasKey("settings")){RoofPlan next=RoofPlan.read(r.settings);if(next.floorY!=d.getPos().getY()-1||Math.abs((long)next.minX-d.getPos().getX())>32||Math.abs((long)next.maxX-d.getPos().getX())>32||Math.abs((long)next.minZ-d.getPos().getZ())>32||Math.abs((long)next.maxZ-d.getPos().getZ())>32)throw new IllegalArgumentException("Floor bounds are out of reach");next.id=d.drawing.id;d.automatic=next.minX==d.drawing.minX&&next.maxX==d.drawing.maxX&&next.minZ==d.drawing.minZ&&next.maxZ==d.drawing.maxZ&&d.automatic;d.drawing=RoofPlanner.solve(next);d.status="";}else if(d.drawing.cells.isEmpty()&&d.drawing.error.isEmpty()){RoofPlanner.solve(d.drawing);}if(r.action==2)RoofConstruction.start(d,p);if(r.action==1)RoofConstruction.preview(p,d,r.preview);d.markDirty();}reply(p,d,r.preview&&!d.active);}catch(IllegalArgumentException bad){d.status="Invalid roof settings.";reply(p,d,false);}
        });return null;}
    }
    public static final class Reply implements IMessage {
        public int dimension,index,total;public BlockPos pos;public RoofPlan plan;public boolean preview,active,canBuild;public String status;
        public Reply(){}
        public void toBytes(ByteBuf b){b.writeInt(dimension);b.writeLong(pos.toLong());NBTTagCompound n=new NBTTagCompound();if(plan!=null)n.setTag("plan",plan.write(new NBTTagCompound()));n.setString("status",status);n.setBoolean("preview",preview);n.setBoolean("active",active);n.setBoolean("canBuild",canBuild);n.setInteger("index",index);n.setInteger("total",total);ByteBufUtils.writeTag(b,n);}
        public void fromBytes(ByteBuf b){dimension=b.readInt();pos=BlockPos.fromLong(b.readLong());NBTTagCompound n=ByteBufUtils.readTag(b);plan=n.hasKey("plan")?RoofPlan.read(n.getCompoundTag("plan")):null;status=n.getString("status");preview=n.getBoolean("preview");active=n.getBoolean("active");canBuild=n.getBoolean("canBuild");index=n.getInteger("index");total=n.getInteger("total");}
    }
    public static final class ReplyHandler implements IMessageHandler<Reply,IMessage>{public IMessage onMessage(Reply r,MessageContext c){IronAgeArchitecture.proxy.roofReply(r);return null;}}
    public static final class Snapshot implements IMessage {
        public int dimension,x,z;public boolean first;public Map<Long,RoofCell> cells=new LinkedHashMap<>();
        public Snapshot(){}
        public int estimatedBytes(){int n=128;for(RoofCell c:cells.values())n+=128+(c==null?0:4*(c.timber.length+c.skin.length));return n;}
        public void toBytes(ByteBuf b){b.writeInt(dimension);b.writeInt(x);b.writeInt(z);b.writeBoolean(first);NBTTagCompound n=new NBTTagCompound();NBTTagList list=new NBTTagList();for(Map.Entry<Long,RoofCell> e:cells.entrySet()){NBTTagCompound a=e.getValue()==null?new NBTTagCompound():e.getValue().write();if(e.getValue()==null)a.setBoolean("delete",true);a.setLong("pos",e.getKey());list.appendTag(a);}n.setTag("cells",list);ByteBufUtils.writeTag(b,n);}
        public void fromBytes(ByteBuf b){dimension=b.readInt();x=b.readInt();z=b.readInt();first=b.readBoolean();NBTTagCompound n=ByteBufUtils.readTag(b);NBTTagList list=n.getTagList("cells",10);if(list.tagCount()>64)throw new IllegalArgumentException("Roof packet too large");for(int i=0;i<list.tagCount();i++){NBTTagCompound a=list.getCompoundTagAt(i);BlockPos p=BlockPos.fromLong(a.getLong("pos"));if((p.getX()>>4)!=x||(p.getZ()>>4)!=z)throw new IllegalArgumentException("Wrong roof chunk");cells.put(p.toLong(),a.getBoolean("delete")?null:RoofCell.read(a));}}
    }
    public static final class SnapshotHandler implements IMessageHandler<Snapshot,IMessage>{public IMessage onMessage(Snapshot r,MessageContext c){IronAgeArchitecture.proxy.roofSnapshot(r);return null;}}
    public static final class Events {
        @SubscribeEvent public void logout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent e){DIRTY.remove(e.player);OUT.remove(e.player);LAST.remove(e.player);}
        @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){
            if(e.phase!=TickEvent.Phase.END||DIRTY.isEmpty())return;
            long end=System.nanoTime()+2000000;int budget=0;List<EntityPlayerMP> players=new ArrayList<>(DIRTY.keySet());Collections.rotate(players,-(rotation++%players.size()));
            for(EntityPlayerMP p:players){
                if(p.connection==null){DIRTY.remove(p);OUT.remove(p);continue;}
                LinkedHashMap<Long,Pending> pending=DIRTY.get(p);ArrayDeque<Snapshot> packets=OUT.computeIfAbsent(p,k->new ArrayDeque<>());
                while(budget<65536&&System.nanoTime()<end){
                    if(packets.isEmpty()){
                        if(pending.isEmpty())break;
                        long key=pending.keySet().iterator().next();Pending next=pending.remove(key);int x=(int)(key>>32),z=(int)key;
                        if(!((WorldServer)p.world).getPlayerChunkMap().isPlayerWatchingChunk(p,x,z)){pending.put(key,next);break;}
                        Map<Long,RoofCell> all=next.replace?RoofData.get(p.world).chunk(p.world,x,z):next.cells;
                        Snapshot a=new Snapshot();a.dimension=p.dimension;a.x=x;a.z=z;a.first=next.replace;
                        for(Map.Entry<Long,RoofCell> entry:all.entrySet()){
                            RoofCell cell=entry.getValue();int size=128+(cell==null?0:4*(cell.timber.length+cell.skin.length));
                            if(!a.cells.isEmpty()&&(a.cells.size()==64||a.estimatedBytes()+size>16384)){packets.add(a);a=new Snapshot();a.dimension=p.dimension;a.x=x;a.z=z;}
                            a.cells.put(entry.getKey(),cell);
                        }
                        packets.add(a);
                    }
                    Snapshot a=packets.peek();int size=a.estimatedBytes();if(budget>0&&budget+size>65536)break;
                    packets.remove();CHANNEL.sendTo(a,p);budget+=size;
                }
                if(pending.isEmpty()&&packets.isEmpty()){DIRTY.remove(p);OUT.remove(p);}
                if(System.nanoTime()>=end||budget>=65536)break;
            }
        }
    }
}
