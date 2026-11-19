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
    private static final FMLEventChannel ACTIONS=NetworkRegistry.INSTANCE.newEventDrivenChannel("iaa_roof_edit");
    private static final SimpleNetworkWrapper CHANNEL=NetworkRegistry.INSTANCE.newSimpleChannel("iaa_roof_desk");
    private static final class Pending {boolean replace;final Map<Long,RoofCell> cells=new LinkedHashMap<>();}
    private static final Map<EntityPlayerMP,LinkedHashMap<Long,Pending>> DIRTY=new WeakHashMap<>();
    public static final java.util.concurrent.atomic.AtomicLong encodedBytes=new java.util.concurrent.atomic.AtomicLong(),decodedBytes=new java.util.concurrent.atomic.AtomicLong(),decodedCells=new java.util.concurrent.atomic.AtomicLong();
    private static final int TICK_BYTES=256*1024;
    private static int rotation;
    private static final Map<EntityPlayerMP,ArrayDeque<Snapshot>> OUT=new WeakHashMap<>();
    private static final Map<EntityPlayerMP,Long> LAST=new WeakHashMap<>();
    public static void register(){ACTIONS.register(new RequestHandler());CHANNEL.registerMessage(ReplyHandler.class,Reply.class,1,Side.CLIENT);CHANNEL.registerMessage(SnapshotHandler.class,Snapshot.class,2,Side.CLIENT);net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(new Events());}
    public static void request(BlockPos pos,int action,RoofPlan plan,boolean preview){Request r=new Request();r.pos=pos;r.action=action;r.preview=preview;if(plan!=null){r.settings=plan.write(new NBTTagCompound());r.settings.removeTag("cells");}sendRequest(r);}
    public static void clipboard(UUID id,int action,RoofPlan plan,boolean preview,NBTTagCompound options){Request r=new Request();r.pos=BlockPos.ORIGIN;r.clipboard=true;r.drawing=id;r.action=action;r.preview=preview;if(plan!=null){r.settings=plan.write(new NBTTagCompound());r.settings.removeTag("cells");}if(options!=null)r.settings=options;sendRequest(r);}
    private static void sendRequest(Request r){net.minecraft.network.PacketBuffer bytes=new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());r.toBytes(bytes);ACTIONS.sendToServer(new net.minecraftforge.fml.common.network.internal.FMLProxyPacket(bytes,"iaa_roof_edit"));}
    public static void replyDrawing(EntityPlayerMP player,RoofPlan plan,boolean preview,net.minecraft.item.ItemStack held){if(plan==null)return;Reply r=new Reply();r.clipboard=true;r.dimension=player.dimension;r.pos=new BlockPos((plan.minX+plan.maxX)/2,plan.floorY+1,(plan.minZ+plan.maxZ)/2);r.plan=plan.copy();r.preview=preview;r.status=plan.valid()?"Ready: return to the workbench to build.":plan.error;if(held!=null&&held.hasTagCompound())r.tool=held.getTagCompound().copy();CHANNEL.sendTo(r,player);}
    public static boolean pending(EntityPlayerMP p){return DIRTY.containsKey(p)||OUT.containsKey(p);}
    public static void reply(EntityPlayerMP player,Desk d,boolean preview){
        Reply r=new Reply();r.dimension=player.dimension;r.pos=d.getPos();r.plan=d.drawing==null?null:d.drawing.copy();r.preview=preview;r.active=d.active;r.status=d.status;
        if(!d.active&&d.drawing!=null){if(d.drawing.version>=2){RoofDesignConstruction.Quote q=RoofDesignConstruction.quote(d,d.drawing,player);r.additions=q.additions;r.returns=q.returns;r.reuse=q.reuse;r.canBuild=q.error.isEmpty()&&(player.capabilities.isCreativeMode||RoofDesignConstruction.supplied(d,q));r.status=!q.error.isEmpty()?q.error:r.canBuild?"Ready: "+q.additions.units()+" new units; "+q.reuse.units()+" reused.":"Supply missing materials; see the bill.";}else{r.status=RoofConstruction.validate(d,d.drawing,player);r.canBuild=r.status.isEmpty()&&(player.capabilities.isCreativeMode||RoofConstruction.available(d,d.drawing.wood)>=RoofConstruction.required(d,d.drawing));if(r.status.isEmpty())r.status="Ready: "+RoofConstruction.required(d,d.drawing)+" timber units; "+RoofConstruction.available(d,d.drawing.wood)+" supplied.";}}
        r.index=d.index;r.total=d.operations.size();CHANNEL.sendTo(r,player);
    }
    public static void changed(World w,BlockPos pos,RoofCell c){for(net.minecraft.entity.player.EntityPlayer p:w.playerEntities)if(p instanceof EntityPlayerMP&&((WorldServer)w).getPlayerChunkMap().isPlayerWatchingChunk((EntityPlayerMP)p,pos.getX()>>4,pos.getZ()>>4))pending((EntityPlayerMP)p,pos.getX()>>4,pos.getZ()>>4).cells.put(pos.toLong(),c);}
    private static Pending pending(EntityPlayerMP p,int x,int z){return DIRTY.computeIfAbsent(p,k->new LinkedHashMap<>()).computeIfAbsent(RoofPlan.column(x,z),k->new Pending());}
    public static void snapshot(EntityPlayerMP p,int x,int z,Map<Long,RoofCell> cells){Pending next=pending(p,x,z);next.replace=true;next.cells.clear();}
    public static void unwatch(EntityPlayerMP p,int x,int z){Map<Long,Pending> q=DIRTY.get(p);if(q!=null)q.remove(RoofPlan.column(x,z));ArrayDeque<Snapshot> out=OUT.get(p);if(out!=null)out.removeIf(s->s.x==x&&s.z==z);}
    public static final class Request implements IMessage {
        BlockPos pos;int action;boolean preview,clipboard;UUID drawing;NBTTagCompound settings=new NBTTagCompound();
        public Request(){}
        public void toBytes(ByteBuf b){b.writeLong(pos.toLong());b.writeByte(action);b.writeBoolean(preview);b.writeBoolean(clipboard);if(clipboard){b.writeLong(drawing.getMostSignificantBits());b.writeLong(drawing.getLeastSignificantBits());}ByteBufUtils.writeTag(b,settings);}
        public void fromBytes(ByteBuf b){if(b.readableBytes()>24576)throw new IllegalArgumentException("Oversized roof request");pos=BlockPos.fromLong(b.readLong());action=b.readUnsignedByte();preview=b.readBoolean();clipboard=b.readBoolean();if(clipboard)drawing=new UUID(b.readLong(),b.readLong());settings=ByteBufUtils.readTag(b);if(settings==null||settings.hasKey("cells")||action>9)throw new IllegalArgumentException("Invalid roof request");}
    }
    public static final class RequestHandler {
        @SubscribeEvent public void receive(FMLNetworkEvent.ServerCustomPacketEvent event){
            // Forge 1.10's shared channel attribute can change on another network thread.
            // The packet carries its own authenticated connection through dispatch.
            net.minecraftforge.fml.common.network.internal.FMLProxyPacket packet=event.getPacket();if(!(packet.handler() instanceof net.minecraft.network.NetHandlerPlayServer))return;EntityPlayerMP p=((net.minecraft.network.NetHandlerPlayServer)packet.handler()).playerEntity;Request r=new Request();r.fromBytes(packet.payload().duplicate());accept(r,p);
        }
        static void accept(Request r,EntityPlayerMP p){p.getServerWorld().addScheduledTask(()->{
            if(r.clipboard){net.minecraft.item.ItemStack held=RoofDrafts.held(p,r.drawing);RoofPlan current=RoofDrafts.read(p.world,r.drawing);if(held==null||current==null||!RoofDrafts.near(p,current))return;
                try{if(r.action==6||r.action==7)current=RoofDrafts.undo(p.world,p,r.drawing,r.action==7);else if(r.action==8){BlockPos hit=BlockPos.fromLong(r.settings.getLong("hit"));if(zone.moddev.mc.ironagearchitecture.block.SmartThatchBlock.eyes(p).squareDistanceTo(new net.minecraft.util.math.Vec3d(hit).addVector(.5,.5,.5))<=49)RoofDrafts.point(p.world,p,held,hit,net.minecraft.util.EnumFacing.getFront(r.settings.getByte("face")));return;}else if(r.action==9){int mode=r.settings.getInteger("tool");if(mode<0||mode>5)return;RoofFeature options=RoofFeature.read(r.settings.getCompoundTag("options"));held.getTagCompound().setInteger("tool",mode);held.getTagCompound().setInteger("selected",r.settings.hasKey("selected")?r.settings.getInteger("selected"):-1);held.getTagCompound().setTag("options",options.write());}else if(r.action==0&&r.settings.hasKey("settings")){RoofPlan next=RoofPlan.read(r.settings);next.id=r.drawing;current=RoofDrafts.accept(p.world,p,next);}replyDrawing(p,current,r.preview,held);}catch(IllegalArgumentException bad){p.sendStatusMessage(new net.minecraft.util.text.TextComponentString(bad.getMessage()));replyDrawing(p,RoofDrafts.read(p.world,r.drawing),true,held);}return;
            }
            if(!(p.openContainer instanceof Storage))return;Desk d=((Storage)p.openContainer).desk;if(!d.getPos().equals(r.pos)||!d.isUsableByPlayer(p))return;long now=p.world.getTotalWorldTime();if(r.action==0&&LAST.getOrDefault(p,Long.MIN_VALUE/2)+2>now)return;LAST.put(p,now);RoofConstruction.session(p,d);
            try{if(r.action==3){RoofConstruction.cancel(d);}else if(!d.active){
                if(d.drawing!=null){RoofPlan shared=RoofData.get(p.world).drawing(d.drawing.id);if(shared!=null&&shared.revision>d.drawing.revision)d.drawing=RoofPlanner.solve(shared.copy());}
                if(r.action==4||d.drawing==null)RoofConstruction.detect(d);
                else if(r.action==5){if(!zone.moddev.mc.ironagearchitecture.ArchitectureBlocks.available(zone.moddev.mc.ironagearchitecture.ArchitectureBlocks.ROOF_CLIPBOARD)||!zone.moddev.mc.ironagearchitecture.EraCatalog.canCraft(new net.minecraft.item.ItemStack(zone.moddev.mc.ironagearchitecture.ArchitectureBlocks.ROOF_CLIPBOARD)))throw new IllegalArgumentException("Clipboard crafting is unavailable in this mode or era.");if(d.getStackInSlot(27)==null){if(d.count(net.minecraft.init.Items.PAPER,-1)<1)throw new IllegalArgumentException("Supply one paper and one plank for a clipboard.");int wood=-1;for(int i=0;i<6;i++)if(d.count(net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.PLANKS),i)>0){wood=i;break;}if(wood<0)throw new IllegalArgumentException("Supply one paper and one plank for a clipboard.");d.take(net.minecraft.init.Items.PAPER,-1,1);d.take(net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.PLANKS),wood,1);d.setInventorySlotContents(27,new net.minecraft.item.ItemStack(zone.moddev.mc.ironagearchitecture.ArchitectureBlocks.ROOF_CLIPBOARD));}else RoofClipboardItem.bind(d.getStackInSlot(27),d.drawing,p.dimension);}
                else if(r.settings.hasKey("settings")){RoofPlan next=RoofPlan.read(r.settings);if(next.floorY!=d.getPos().getY()-1||Math.abs((long)next.minX-d.getPos().getX())>32||Math.abs((long)next.maxX-d.getPos().getX())>32||Math.abs((long)next.minZ-d.getPos().getZ())>32||Math.abs((long)next.maxZ-d.getPos().getZ())>32)throw new IllegalArgumentException("Floor bounds are out of reach");if(next.revision!=d.drawing.revision)throw new IllegalArgumentException("The drawing changed. Try the edit again.");next.id=d.drawing.id;next.revision++;if(next.version>=2)RoofSupports.detect(p.world,next);d.automatic=next.minX==d.drawing.minX&&next.maxX==d.drawing.maxX&&next.minZ==d.drawing.minZ&&next.maxZ==d.drawing.maxZ&&d.automatic;d.drawing=RoofPlanner.solve(next);d.status="";RoofData.get(p.world).draft(d.drawing);}
                else if(d.drawing.cells.isEmpty()&&d.drawing.error.isEmpty())RoofPlanner.solve(d.drawing);
                if(r.action==2)RoofConstruction.start(d,p);if(r.action==1)RoofConstruction.preview(p,d,r.preview);d.markDirty();
            }reply(p,d,r.preview&&!d.active);}catch(IllegalArgumentException bad){d.status=bad.getMessage();p.sendStatusMessage(new net.minecraft.util.text.TextComponentString(bad.getMessage()));reply(p,d,false);}
        });}
    }
    public static final class Reply implements IMessage {
        public int dimension,index,total;public BlockPos pos;public RoofPlan plan;public boolean preview,active,canBuild,clipboard;public String status;public NBTTagCompound tool=new NBTTagCompound();public RoofMaterials.Stock additions=new RoofMaterials.Stock(),returns=new RoofMaterials.Stock(),reuse=new RoofMaterials.Stock();
        public Reply(){}
        public void toBytes(ByteBuf b){b.writeInt(dimension);b.writeLong(pos.toLong());NBTTagCompound n=new NBTTagCompound();if(plan!=null)n.setTag("plan",plan.write(new NBTTagCompound()));n.setString("status",status);n.setBoolean("preview",preview);n.setBoolean("active",active);n.setBoolean("canBuild",canBuild);n.setInteger("index",index);n.setInteger("total",total);n.setBoolean("clipboard",clipboard);n.setTag("tool",tool);n.setTag("additions",additions.write());n.setTag("returns",returns.write());n.setTag("reuse",reuse.write());ByteBufUtils.writeTag(b,n);}
        public void fromBytes(ByteBuf b){dimension=b.readInt();pos=BlockPos.fromLong(b.readLong());NBTTagCompound n=ByteBufUtils.readTag(b);plan=n.hasKey("plan")?RoofPlan.read(n.getCompoundTag("plan")):null;status=n.getString("status");preview=n.getBoolean("preview");active=n.getBoolean("active");canBuild=n.getBoolean("canBuild");index=n.getInteger("index");total=n.getInteger("total");clipboard=n.getBoolean("clipboard");tool=n.getCompoundTag("tool");additions=RoofMaterials.Stock.read(n.getTagList("additions",10));returns=RoofMaterials.Stock.read(n.getTagList("returns",10));reuse=RoofMaterials.Stock.read(n.getTagList("reuse",10));}
    }
    public static final class ReplyHandler implements IMessageHandler<Reply,IMessage>{public IMessage onMessage(Reply r,MessageContext c){IronAgeArchitecture.proxy.roofReply(r);return null;}}
    public static final class Snapshot implements IMessage {
        public int dimension,x,z;public boolean first;public Map<Long,RoofCell> cells=new LinkedHashMap<>();
        public Snapshot(){}
        public int estimatedBytes(){int n=128;for(RoofCell c:cells.values())n+=(c==null?128:c.estimatedBytes());return n;}
        public void toBytes(ByteBuf b){int start=b.writerIndex();b.writeInt(dimension);b.writeInt(x);b.writeInt(z);b.writeBoolean(first);NBTTagCompound n=new NBTTagCompound();NBTTagList list=new NBTTagList();for(Map.Entry<Long,RoofCell> e:cells.entrySet()){NBTTagCompound a=e.getValue()==null?new NBTTagCompound():e.getValue().write();if(e.getValue()==null)a.setBoolean("delete",true);a.setLong("pos",e.getKey());list.appendTag(a);}n.setTag("cells",list);ByteBufUtils.writeTag(b,n);encodedBytes.addAndGet(b.writerIndex()-start);}
        public void fromBytes(ByteBuf b){int start=b.readerIndex();dimension=b.readInt();x=b.readInt();z=b.readInt();first=b.readBoolean();NBTTagCompound n=ByteBufUtils.readTag(b);NBTTagList list=n.getTagList("cells",10);if(list.tagCount()>64)throw new IllegalArgumentException("Roof packet too large");for(int i=0;i<list.tagCount();i++){NBTTagCompound a=list.getCompoundTagAt(i);BlockPos p=BlockPos.fromLong(a.getLong("pos"));if((p.getX()>>4)!=x||(p.getZ()>>4)!=z)throw new IllegalArgumentException("Wrong roof chunk");cells.put(p.toLong(),a.getBoolean("delete")?null:RoofCell.read(a));}decodedBytes.addAndGet(b.readerIndex()-start);decodedCells.addAndGet(cells.size());}
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
                while(budget<TICK_BYTES&&System.nanoTime()<end){
                    if(packets.isEmpty()){
                        if(pending.isEmpty())break;
                        long key=pending.keySet().iterator().next();Pending next=pending.remove(key);int x=(int)(key>>32),z=(int)key;
                        if(!((WorldServer)p.world).getPlayerChunkMap().isPlayerWatchingChunk(p,x,z)){pending.put(key,next);break;}
                        Map<Long,RoofCell> all=next.replace?RoofData.get(p.world).chunk(p.world,x,z):next.cells;
                        Snapshot a=new Snapshot();a.dimension=p.dimension;a.x=x;a.z=z;a.first=next.replace;
                        for(Map.Entry<Long,RoofCell> entry:all.entrySet()){
                            RoofCell cell=entry.getValue();int size=cell==null?128:cell.estimatedBytes();
                            if(!a.cells.isEmpty()&&(a.cells.size()==64||a.estimatedBytes()+size>16384)){packets.add(a);a=new Snapshot();a.dimension=p.dimension;a.x=x;a.z=z;}
                            a.cells.put(entry.getKey(),cell);
                        }
                        packets.add(a);
                    }
                    Snapshot a=packets.peek();int size=a.estimatedBytes();if(budget>0&&budget+size>TICK_BYTES)break;
                    packets.remove();CHANNEL.sendTo(a,p);budget+=size;
                }
                if(pending.isEmpty()&&packets.isEmpty()){DIRTY.remove(p);OUT.remove(p);}
                if(System.nanoTime()>=end||budget>=TICK_BYTES)break;
            }
        }
    }
}
