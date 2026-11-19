package zone.moddev.mc.ironagearchitecture;

import io.netty.channel.*;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/** Forge 1.10 forwards handshake completion through a shared embedded channel. */
public final class LegacyHandshakeGuard {
    private static final Object HANDSHAKES=new Object();
    private static final String NAME="iaa_handshake_guard";

    @SubscribeEvent public void registered(FMLNetworkEvent.CustomPacketRegistrationEvent<?> event) {
        if(!(event.getHandler() instanceof NetHandlerPlayServer))return;
        ChannelPipeline pipeline=event.getManager().channel().pipeline();
        // REGISTER precedes the client's handshake acknowledgements. Install on this
        // connection's event loop before those packets can reach NetworkDispatcher.
        if(pipeline.get(NAME)!=null)return;
        pipeline.addBefore("fml:packet_handler",NAME,new ChannelInboundHandlerAdapter(){
            @Override public void channelRead(ChannelHandlerContext context,Object packet)throws Exception {
                if(packet instanceof CPacketCustomPayload&&"FML|HS".equals(((CPacketCustomPayload)packet).getChannelName())) {
                    synchronized(HANDSHAKES){context.fireChannelRead(packet);}
                }else context.fireChannelRead(packet);
            }
        });
    }
}
