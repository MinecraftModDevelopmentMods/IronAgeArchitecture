package zone.moddev.mc.ironagearchitecture.probe;

import java.util.concurrent.atomic.AtomicLong;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/** Raw post-login TCP payload at the first Netty pipeline handler; no packet simulation. */
final class MPWireMeter {
    final AtomicLong inbound=new AtomicLong(),outbound=new AtomicLong();boolean tcp;String remote;
    static MPWireMeter attach(NetworkManager manager){MPWireMeter meter=new MPWireMeter();try{Channel channel=(Channel)ReflectionHelper.findField(NetworkManager.class,"channel","field_150746_k").get(manager);meter.remote=String.valueOf(channel.remoteAddress());meter.tcp=channel.remoteAddress() instanceof java.net.InetSocketAddress;if(!meter.tcp)throw new IllegalStateException("Expected TCP, found "+channel.getClass());channel.eventLoop().execute(()->channel.pipeline().addFirst("iaa_mp_meter",new ChannelDuplexHandler(){@Override public void channelRead(ChannelHandlerContext ctx,Object message)throws Exception{if(message instanceof ByteBuf)meter.inbound.addAndGet(((ByteBuf)message).readableBytes());super.channelRead(ctx,message);}@Override public void write(ChannelHandlerContext ctx,Object message,ChannelPromise promise)throws Exception{if(message instanceof ByteBuf)meter.outbound.addAndGet(((ByteBuf)message).readableBytes());super.write(ctx,message,promise);}}));return meter;}catch(Exception e){throw new RuntimeException(e);}}
}
