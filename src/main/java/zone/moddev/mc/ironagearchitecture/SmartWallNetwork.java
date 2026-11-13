package zone.moddev.mc.ironagearchitecture;

import java.util.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;

/** Server-owned chunk snapshots and changed cells; the client never proposes framing. */
public final class SmartWallNetwork {
    private static final SimpleNetworkWrapper CHANNEL=NetworkRegistry.INSTANCE.newSimpleChannel("iaa_smart_walls");
    private SmartWallNetwork() { }
    public static void register(){CHANNEL.registerMessage(Handler.class,Packet.class,0,Side.CLIENT);}
    public static void send(EntityPlayerMP player,int dimension,int x,int z,boolean replace,Map<Long,Integer> cells) {
        CHANNEL.sendTo(new Packet(dimension,x,z,replace,cells),player);
    }

    public static final class Packet implements IMessage {
        public int dimension,x,z,mode;public boolean replace;public Map<Long,Integer> cells;
        public Packet(){ }
        Packet(int dimension,int x,int z,boolean replace,Map<Long,Integer> cells) {
            this.dimension=dimension;this.x=x;this.z=z;this.replace=replace;this.cells=new HashMap<>(cells);
        }
        @Override public void toBytes(ByteBuf buffer) {
            buffer.writeInt(dimension);buffer.writeInt(x);buffer.writeInt(z);buffer.writeBoolean(replace);buffer.writeByte(ArchitectureConfig.buildingMode.ordinal());
            buffer.writeInt(cells.size());
            for(Map.Entry<Long,Integer> cell:cells.entrySet()){buffer.writeLong(cell.getKey());buffer.writeInt(cell.getValue());}
        }
        @Override public void fromBytes(ByteBuf buffer) {
            dimension=buffer.readInt();x=buffer.readInt();z=buffer.readInt();replace=buffer.readBoolean();mode=buffer.readUnsignedByte();
            if(mode>=BuildingMode.values().length) throw new IllegalArgumentException("Unknown building mode");
            int count=buffer.readInt();
            if(count<0||count>65536||buffer.readableBytes()!=count*12) throw new IllegalArgumentException("Invalid smart wall snapshot");
            cells=new HashMap<>();
            for(int i=0;i<count;i++) cells.put(buffer.readLong(),buffer.readInt());
        }
    }
    public static final class Handler implements IMessageHandler<Packet,IMessage> {
        @Override public IMessage onMessage(Packet message,MessageContext context){
            IronAgeArchitecture.proxy.smartSnapshot(message);return null;
        }
    }
}
