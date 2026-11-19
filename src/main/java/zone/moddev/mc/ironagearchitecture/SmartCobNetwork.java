package zone.moddev.mc.ironagearchitecture;

import java.util.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;
import zone.moddev.mc.ironagearchitecture.block.SmartCobProfile;
import zone.moddev.mc.ironagearchitecture.block.SmartCobData;
import zone.moddev.mc.ironagearchitecture.block.SmartWallData;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Cob snapshots have their own channel and are split before reaching the legacy packet limit. */
public final class SmartCobNetwork {
    private static final SimpleNetworkWrapper CHANNEL=NetworkRegistry.INSTANCE.newSimpleChannel("iaa_smart_cob");
    private static final Map<EntityPlayerMP,LinkedHashMap<Long,Pending>> QUEUED=new WeakHashMap<>();
    private static final class Pending {
        final int dimension,x,z;boolean replace;final Map<Long,SmartCobProfile> changes=new HashMap<>();List<Packet> packets;int index;
        Pending(int dimension,int x,int z){this.dimension=dimension;this.x=x;this.z=z;}
    }
    private int rotation;
    private SmartCobNetwork() { }
    public static void register(){CHANNEL.registerMessage(Handler.class,Packet.class,0,Side.CLIENT);net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(new SmartCobNetwork());}
    public static void send(EntityPlayerMP player,int dimension,int x,int z,boolean replace,Map<Long,SmartCobProfile> cells) {
        LinkedHashMap<Long,Pending> queue=QUEUED.computeIfAbsent(player,p->new LinkedHashMap<>());long key=SmartWallData.chunkKey(x,z);Pending next=queue.get(key);
        if(next==null||next.dimension!=dimension){next=new Pending(dimension,x,z);queue.put(key,next);}
        next.replace|=replace;if(!next.replace)next.changes.putAll(cells);else next.changes.clear();next.packets=null;next.index=0;
    }
    public static boolean pending(EntityPlayerMP player){Map<Long,Pending> queue=QUEUED.get(player);return queue!=null&&!queue.isEmpty();}
    public static void unwatch(EntityPlayerMP player,int x,int z){Map<Long,Pending> queue=QUEUED.get(player);if(queue!=null)queue.remove(SmartWallData.chunkKey(x,z));}
    @SubscribeEvent public void logout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent e){QUEUED.remove(e.player);}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||QUEUED.isEmpty())return;long deadline=System.nanoTime()+2000000L;int bytes=0;List<EntityPlayerMP> players=new ArrayList<>(QUEUED.keySet());Collections.rotate(players,-(rotation++%players.size()));
        for(EntityPlayerMP player:players){LinkedHashMap<Long,Pending> queue=QUEUED.get(player);if(player.isDead||player.connection==null){QUEUED.remove(player);continue;}int playerBytes=0;
            while(!queue.isEmpty()&&bytes<131072&&playerBytes<32768&&System.nanoTime()<deadline){long selected=0;Pending next=null;double nearest=Double.MAX_VALUE;
                for(Map.Entry<Long,Pending> entry:queue.entrySet()){Pending p=entry.getValue();double dx=p.x*16+8-player.posX,dz=p.z*16+8-player.posZ,d=dx*dx+dz*dz;if(p.packets!=null||d<nearest){nearest=d;next=p;selected=entry.getKey();if(p.packets!=null)break;}}
                if(next.dimension!=player.dimension){queue.remove(selected);continue;}
                if(next.packets==null)next.packets=packets(next.dimension,next.x,next.z,next.replace,next.replace?SmartCobData.get(player.world).chunk(next.x,next.z):next.changes);
                if(next.packets.isEmpty()){queue.remove(selected);continue;}
                Packet packet=next.packets.get(next.index);CHANNEL.sendTo(packet,player);bytes+=packet.encoded.length;playerBytes+=packet.encoded.length;if(++next.index==next.packets.size())queue.remove(selected);
            }
            if(queue.isEmpty())QUEUED.remove(player);if(System.nanoTime()>=deadline||bytes>=131072)break;
        }
    }
    public static List<Packet> packets(int dimension,int x,int z,boolean replace,Map<Long,SmartCobProfile> cells) {
        List<Packet> packets=new ArrayList<>();
        Map<Long,SmartCobProfile> batch=new HashMap<>();int bytes=0;boolean first=replace;
        for(Map.Entry<Long,SmartCobProfile> cell:cells.entrySet()) {
            int size=12+(cell.getValue()==null?0:cell.getValue().encodedLength()*4);
            if(!batch.isEmpty()&&(bytes+size>262144||batch.size()>=2048)) {packets.add(new Packet(dimension,x,z,first,batch));first=false;batch=new HashMap<>();bytes=0;}
            batch.put(cell.getKey(),cell.getValue());bytes+=size;
        }
        if(!batch.isEmpty()||replace)packets.add(new Packet(dimension,x,z,first,batch));
        return packets;
    }
    public static final class Packet implements IMessage {
        public int dimension,x,z;public boolean replace;public Map<Long,SmartCobProfile> cells;
        private byte[] encoded;
        public Packet() { }
        public Packet(int dimension,int x,int z,boolean replace,Map<Long,SmartCobProfile> cells){this.dimension=dimension;this.x=x;this.z=z;this.replace=replace;this.cells=new HashMap<>(cells);ByteBuf b=Unpooled.buffer();try{encode(b);encoded=new byte[b.readableBytes()];b.readBytes(encoded);}finally{b.release();}}
        @Override public void toBytes(ByteBuf b){if(encoded!=null)b.writeBytes(encoded);else encode(b);}
        private void encode(ByteBuf b) {
            b.writeInt(dimension);b.writeInt(x);b.writeInt(z);b.writeBoolean(replace);b.writeInt(cells.size());
            for(Map.Entry<Long,SmartCobProfile> cell:cells.entrySet()) {b.writeLong(cell.getKey());int[] code=cell.getValue()==null?new int[0]:cell.getValue().encode();b.writeInt(code.length);for(int v:code)b.writeInt(v);}
        }
        @Override public void fromBytes(ByteBuf b) {
            dimension=b.readInt();x=b.readInt();z=b.readInt();replace=b.readBoolean();int count=b.readInt();
            if(count<0||count>2048||b.readableBytes()>300000) throw new IllegalArgumentException("Invalid cob snapshot size");
            cells=new HashMap<>();
            for(int i=0;i<count;i++) {long at=b.readLong();BlockPos p=BlockPos.fromLong(at);int length=b.readInt();
                if((p.getX()>>4)!=x||(p.getZ()>>4)!=z||length<0||length>4096||(length&1)!=0||b.readableBytes()<length*4) throw new IllegalArgumentException("Invalid cob snapshot cell");
                int[] code=new int[length];for(int j=0;j<length;j++) code[j]=b.readInt();cells.put(at,length==0?null:new SmartCobProfile(code));
            }
            if(b.readableBytes()!=0) throw new IllegalArgumentException("Trailing cob snapshot data");
        }
    }
    public static final class Handler implements IMessageHandler<Packet,IMessage> {
        @Override public IMessage onMessage(Packet packet,MessageContext context){IronAgeArchitecture.proxy.cobSnapshot(packet);return null;}
    }
}
