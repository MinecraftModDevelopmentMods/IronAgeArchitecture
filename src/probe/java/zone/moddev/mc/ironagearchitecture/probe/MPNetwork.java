package zone.moddev.mc.ironagearchitecture.probe;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.fml.common.network.*;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Test-only control and verification. Building uses vanilla player-controller packets. */
final class MPNetwork {
    static final SimpleNetworkWrapper CHANNEL=NetworkRegistry.INSTANCE.newSimpleChannel("iaa_mp_test");
    static final FMLEventChannel STATUS=NetworkRegistry.INSTANCE.newEventDrivenChannel("iaa_mp_status");
    static void register(){CHANNEL.registerMessage(CommandHandler.class,Command.class,0,Side.CLIENT);STATUS.register(new StatusHandler());}
    static void sendStatus(Status status){PacketBuffer payload=new PacketBuffer(Unpooled.buffer());status.toBytes(payload);STATUS.sendToServer(new FMLProxyPacket(payload,"iaa_mp_status"));}
    public static final class Command implements IMessage {
        int id,x,z,sequence;String kind="",hash="";
        public Command(){}Command(String kind,int id,int x,int z,int sequence,String hash){this.kind=kind;this.id=id;this.x=x;this.z=z;this.sequence=sequence;this.hash=hash;}
        public void toBytes(ByteBuf b){b.writeInt(id);b.writeInt(x);b.writeInt(z);b.writeInt(sequence);ByteBufUtils.writeUTF8String(b,kind);ByteBufUtils.writeUTF8String(b,hash);}
        public void fromBytes(ByteBuf b){id=b.readInt();x=b.readInt();z=b.readInt();sequence=b.readInt();kind=ByteBufUtils.readUTF8String(b);hash=ByteBufUtils.readUTF8String(b);}
    }
    public static final class Status implements IMessage {
        int id,sequence,operations;String kind="",detail="";
        public Status(){}Status(String kind,int id,int sequence,int operations,String detail){this.kind=kind;this.id=id;this.sequence=sequence;this.operations=operations;this.detail=detail;}
        public void toBytes(ByteBuf b){b.writeInt(id);b.writeInt(sequence);b.writeInt(operations);ByteBufUtils.writeUTF8String(b,kind);ByteBufUtils.writeUTF8String(b,detail);}
        public void fromBytes(ByteBuf b){id=b.readInt();sequence=b.readInt();operations=b.readInt();kind=ByteBufUtils.readUTF8String(b);detail=ByteBufUtils.readUTF8String(b);}
    }
    public static final class CommandHandler implements IMessageHandler<Command,IMessage>{public IMessage onMessage(Command p,MessageContext c){ArchitectureProbe.proxy.mpCommand(p);return null;}}
    public static final class StatusHandler {
        @SubscribeEvent public void receive(FMLNetworkEvent.ServerCustomPacketEvent event){
            // Forge 1.10's shared embedded channel stores NET_HANDLER in a mutable attribute.
            // Simultaneous network threads can overwrite that context. The raw packet retains
            // its own handler, so acknowledgement identity stays tied to the TCP connection.
            FMLProxyPacket packet=event.getPacket();
            if(!(packet.handler() instanceof NetHandlerPlayServer))throw new IllegalStateException("Missing packet sender");
            net.minecraft.entity.player.EntityPlayerMP player=((NetHandlerPlayServer)packet.handler()).playerEntity;
            Status status=new Status();status.fromBytes(packet.payload().duplicate());
            player.getServerWorld().addScheduledTask(()->MPServerProbe.status(player,status));
        }
    }
}
