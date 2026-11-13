package zone.moddev.mc.ironagearchitecture;

import java.util.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;
import zone.moddev.mc.ironagearchitecture.block.SmartCobProfile;

/** Cob snapshots have their own channel and are split before reaching the legacy packet limit. */
public final class SmartCobNetwork {
    private static final SimpleNetworkWrapper CHANNEL=NetworkRegistry.INSTANCE.newSimpleChannel("iaa_smart_cob");
    private SmartCobNetwork() { }
    public static void register(){CHANNEL.registerMessage(Handler.class,Packet.class,0,Side.CLIENT);}
    public static void send(EntityPlayerMP player,int dimension,int x,int z,boolean replace,Map<Long,SmartCobProfile> cells) {
        for(Packet packet:packets(dimension,x,z,replace,cells))CHANNEL.sendTo(packet,player);
    }
    public static List<Packet> packets(int dimension,int x,int z,boolean replace,Map<Long,SmartCobProfile> cells) {
        List<Packet> packets=new ArrayList<>();
        Map<Long,SmartCobProfile> batch=new HashMap<>();int bytes=0;boolean first=replace;
        for(Map.Entry<Long,SmartCobProfile> cell:cells.entrySet()) {
            int size=12+(cell.getValue()==null?0:cell.getValue().encode().length*4);
            if(!batch.isEmpty()&&(bytes+size>262144||batch.size()>=2048)) {packets.add(new Packet(dimension,x,z,first,batch));first=false;batch=new HashMap<>();bytes=0;}
            batch.put(cell.getKey(),cell.getValue());bytes+=size;
        }
        if(!batch.isEmpty()||replace)packets.add(new Packet(dimension,x,z,first,batch));
        return packets;
    }
    public static final class Packet implements IMessage {
        public int dimension,x,z;public boolean replace;public Map<Long,SmartCobProfile> cells;
        public Packet() { }
        public Packet(int dimension,int x,int z,boolean replace,Map<Long,SmartCobProfile> cells){this.dimension=dimension;this.x=x;this.z=z;this.replace=replace;this.cells=new HashMap<>(cells);}
        @Override public void toBytes(ByteBuf b) {
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
