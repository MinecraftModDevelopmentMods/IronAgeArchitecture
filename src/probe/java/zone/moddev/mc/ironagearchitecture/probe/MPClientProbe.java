package zone.moddev.mc.ironagearchitecture.probe;

import java.util.*;
import java.nio.file.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.network.play.client.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.block.*;
import zone.moddev.mc.ironagearchitecture.roof.*;
import zone.moddev.mc.ironagearchitecture.client.WorkbenchClient;

/** Actual native Forge client: TCP join, vanilla creative actions, and received-shape verification. */
final class MPClientProbe {
    static MPClientProbe INSTANCE;final int id=Integer.getInteger("iaa.mpId",0),fps=Integer.getInteger("iaa.mpFps",10);final Path root=Paths.get(System.getProperty("iaa.mpRoot")),folder=Paths.get(System.getProperty("iaa.mpClientDir"));
    final List<Double> loginTimes=new ArrayList<>();
    final Queue<MPNetwork.Command> commands=new ConcurrentLinkedQueue<>();final Map<String,List<Double>> frames=new LinkedHashMap<>();
    String phase="startup",error="";int ticks,opIndex,opTick,retries,operations;boolean menu,connecting,joined,stopped,building,built,verified;long start=System.nanoTime(),connectAt,joinedAt,lastFrame,verifyAt;
    BlockPos home,station;List<MPPlan.Op> plan;MPNetwork.Command verify;Future<MPFingerprint.Result> digest;MPWireMeter meter;
    BlockPos roofBench;int roofStage,roofTicks;UUID roofId;boolean roofEdited,roofBuilt;
    MPClientProbe(){INSTANCE=this;net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);}
    @SubscribeEvent public void chat(net.minecraftforge.client.event.ClientChatReceivedEvent e){System.out.println("IAA_MP_CHAT type="+e.getType()+" text="+e.getMessage().getUnformattedText());}
    static void command(MPNetwork.Command packet){if(INSTANCE!=null)INSTANCE.commands.add(packet);}
    void status(String kind,int seq,String detail){MPNetwork.sendStatus(new MPNetwork.Status(kind,id,seq,operations,detail));}
    @SubscribeEvent public void frame(TickEvent.RenderTickEvent event){if(event.phase!=TickEvent.Phase.END||!joined)return;long now=System.nanoTime();if(lastFrame!=0)frames.computeIfAbsent(phase,k->new ArrayList<>()).add((now-lastFrame)/1e6);lastFrame=now;}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END||stopped)return;try{advance();}catch(Throwable failure){failure.printStackTrace();fail(failure.toString());}}
    void advance()throws Exception{
        Minecraft mc=Minecraft.getMinecraft();ticks++;
        if(System.nanoTime()-start>900000000000L){fail("Client exceeded 15 minutes");return;}
        if(!menu&&mc.currentScreen instanceof GuiMainMenu&&ticks>40){menu=true;mc.gameSettings.pauseOnLostFocus=false;mc.gameSettings.renderDistanceChunks=Integer.getInteger("iaa.mpViewDistance",6);mc.gameSettings.limitFramerate=fps;mc.gameSettings.enableVsync=false;mc.gameSettings.clouds=0;org.lwjgl.opengl.Display.setLocation(id==0?24: -30000,id==0?24:-30000);Map<String,Object> ready=new LinkedHashMap<>();ready.put("ready",true);ready.put("id",id);ready.put("sha256",System.getProperty("iaa.runtimeSha256"));MPFiles.write(folder.resolve("menu-ready.json"),ready);}
        if(menu&&!connecting&&Files.exists(root.resolve("connect.flag"))){connecting=true;connectAt=System.nanoTime();phase="join";ServerData data=new ServerData("IAA multiplayer qualification","127.0.0.1:"+System.getProperty("iaa.mpPort"),false);mc.setServerData(data);mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(),mc,data));}
        if(connecting&&!joined&&mc.player!=null&&mc.world!=null){if(mc.getIntegratedServer()!=null)throw new IllegalStateException("Integrated server is forbidden in this test");joined=true;joinedAt=System.nanoTime();loginTimes.add((joinedAt-connectAt)/1e6);meter=MPWireMeter.attach(mc.getConnection().getNetworkManager());mc.displayGuiScreen(null);mc.gameSettings.hideGUI=true;status("ready",0,"TCP client joined in "+(joinedAt-connectAt)/1e6+"ms");}
        if(!joined){if(connecting&&System.nanoTime()-connectAt>180000000000L)fail("TCP login timed out");return;}
        if(mc.player==null||mc.world==null){fail("Disconnected before completion");return;}
        mc.player.capabilities.allowFlying=true;mc.player.capabilities.isFlying=true;
        MPNetwork.Command command;while((command=commands.poll())!=null)apply(mc,command);
        if(stopped||!joined)return;
        if(phase.equals("idle")&&station!=null){double x=(station.getX()>>4)*16-.45,z=(station.getZ()>>4)*16+2.5+1.5*Math.sin(ticks/40.0+id);mc.player.setPosition(x,71,z);}
        if(building)build(mc);
        if(verify!=null){if(System.nanoTime()-verifyAt>120000000000L){fail("Received shapes never matched: sequence "+verify.sequence);return;}if(digest==null&&ticks%20==0)digest=MPFingerprint.submit(MPFingerprint.capture(mc.world,verify.x,verify.z));if(digest!=null&&digest.isDone()){MPFingerprint.Result r=digest.get();digest=null;boolean materials=station!=null&&mc.world.getBlockState(station.add(6,0,6)).getBlock()==Blocks.STONEBRICK;if(built&&(verify.kind.endsWith("building")||verify.kind.endsWith("return")))for(Map.Entry<BlockPos,Block> e:MPPlan.finalBlocks(id,home).entrySet())materials&=mc.world.getBlockState(e.getKey()).getBlock()==e.getValue();if(materials&&r.hash.equals(verify.hash)){status("verified",verify.sequence,"hash="+r.hash+" roof="+r.roof+" cob="+r.cob+" timber="+r.timber);verified=true;verify=null;}}}
    }
    void apply(Minecraft mc,MPNetwork.Command command){if(command.id!=id)throw new IllegalStateException("Wrong recipient");if(command.kind.equals("reconnect")){mc.getConnection().getNetworkManager().closeChannel(new net.minecraft.util.text.TextComponentString("Qualification reconnect"));mc.loadWorld(null);mc.displayGuiScreen(new GuiMainMenu());joined=connecting=verified=false;meter=null;verify=null;digest=null;lastFrame=0;phase="reconnect";return;}if(command.kind.startsWith("phase:")){phase=command.kind.substring(6);lastFrame=0;return;}if(command.kind.startsWith("move:")){phase=command.kind.substring(5);station=new BlockPos(command.x,MPPlan.FLOOR,command.z);if(home==null)home=station;Vec3d at=MPPlan.position(station,command.kind.endsWith("building"),id);mc.player.setPosition(at.xCoord,at.yCoord,at.zCoord);mc.player.capabilities.allowFlying=true;mc.player.capabilities.isFlying=true;mc.player.sendPlayerAbilities();mc.player.rotationYaw=135;mc.player.rotationPitch=20;lastFrame=0;return;}if(command.kind.equals("roof-drawing")){roofBench=new BlockPos(command.x,121,command.z);roofStage=roofTicks=0;return;}if(command.kind.equals("build")){home=new BlockPos(command.x,MPPlan.FLOOR,command.z);plan=MPPlan.steps(home);building=true;opIndex=opTick=retries=operations=0;phase="building";return;}if(command.kind.startsWith("verify:")){verify=command;verified=false;verifyAt=System.nanoTime();digest=null;phase=command.kind;return;}if(command.kind.equals("stop")){error=command.hash;finish(mc);}}
    void build(Minecraft mc){if(roofBench!=null&&!roofBuilt){roofDrawing(mc);return;}if(opIndex>=plan.size()){building=false;built=true;status("built",0,"all vanilla operations completed");return;}MPPlan.Op op=plan.get(opIndex);Block expected=op.roof?ArchitectureBlocks.SMART_THATCH:MPPlan.wall(id);opTick++;
        if(opTick==1){mc.player.setPosition(op.at.getX()+.5,op.at.getY()+1.6,op.at.getZ()+2.5);aim(mc,new Vec3d(op.at).addVector(.5,.5,.5));mc.getConnection().sendPacket(new CPacketPlayer.PositionRotation(mc.player.posX,mc.player.posY,mc.player.posZ,mc.player.rotationYaw,mc.player.rotationPitch,false));}
        if(opTick==4){if(op.remove){Vec3d target=new Vec3d(op.at).addVector(.5,.5,.5);if(op.roof){double score=-1e9;for(ThatchGeometry.Face f:((SmartThatchBlock)expected).mesh(mc.world,op.at).faces){double x=0,y=0,z=0;for(double[] v:f.vertices){x+=v[0];y+=v[1];z+=v[2];}x/=f.vertices.length;y/=f.vertices.length;z/=f.vertices.length;double s=f.normal[1]*10-Math.abs(x-.5)-Math.abs(z-.5);if(s>score){score=s;target=new Vec3d(op.at).addVector(x,y,z);}}}aim(mc,target);mc.getConnection().sendPacket(new CPacketPlayer.Rotation(mc.player.rotationYaw,mc.player.rotationPitch,false));mc.playerController.clickBlock(op.at,EnumFacing.UP);}else{ItemStack stack=new ItemStack(expected,64,op.roof?0:MPPlan.finish(id));mc.player.inventory.currentItem=0;mc.player.inventory.setInventorySlotContents(0,stack);mc.getConnection().sendPacket(new CPacketHeldItemChange(0));mc.getConnection().sendPacket(new CPacketCreativeInventoryAction(36,stack.copy()));BlockPos support=null;EnumFacing side=null;
            if(mc.world.getBlockState(op.at).getBlock()==ArchitectureBlocks.SMART_THATCH_PART){support=op.at;side=EnumFacing.UP;}else for(EnumFacing direction:new EnumFacing[]{EnumFacing.DOWN,EnumFacing.WEST,EnumFacing.EAST,EnumFacing.NORTH,EnumFacing.SOUTH,EnumFacing.UP}){BlockPos at=op.at.offset(direction);if(!mc.world.isAirBlock(at)&&mc.world.getBlockState(at).getBlock()!=ArchitectureBlocks.SMART_THATCH_PART){support=at;side=direction.getOpposite();break;}}
            if(support!=null){Vec3d hit=new Vec3d(support).addVector(.5+side.getFrontOffsetX()*.5,.5+side.getFrontOffsetY()*.5,.5+side.getFrontOffsetZ()*.5);aim(mc,hit);mc.getConnection().sendPacket(new CPacketPlayer.Rotation(mc.player.rotationYaw,mc.player.rotationPitch,false));mc.playerController.processRightClickBlock(mc.player,mc.world,stack,support,side,hit,EnumHand.MAIN_HAND);}}}
        // TCP delivers each action reliably. A delayed helper replacement must not cause
        // another click: once the server has installed the owner, that click would target
        // the cell above it. Wait for the authoritative update instead of resending.
        if(opTick>=10){Block current=mc.world.getBlockState(op.at).getBlock();boolean ok=op.remove?current!=expected:current==expected;if(ok){operations++;opIndex++;opTick=retries=0;}else if(opTick>=100){throw new IllegalStateException("Vanilla action timed out index="+opIndex+" at="+op.at+" current="+current.getRegistryName()+" expected="+expected.getRegistryName()+" remove="+op.remove);}}}
    void roofDrawing(Minecraft mc){roofTicks++;if(roofTicks>1200)throw new IllegalStateException("Clipboard construction timeout stage="+roofStage);ItemStack board=mc.player.inventory.getStackInSlot(8);if(board==null||RoofClipboardItem.bound(board)==null)return;
        if(roofStage==0){roofId=RoofClipboardItem.bound(board);status("roof-approach",0,"approach the attic bench");roofStage=10;roofTicks=0;return;}if(roofStage==10){if(Math.abs(mc.player.posY-roofBench.getY()-1)>1||mc.player.getDistanceSq(roofBench)>49||roofTicks<12)return;mc.player.inventory.currentItem=8;mc.getConnection().sendPacket(new CPacketHeldItemChange(8));RoofNetwork.clipboard(roofId,0,null,false,null);roofStage=1;roofTicks=0;return;}

        RoofNetwork.Reply latest=WorkbenchClient.latest;if(roofTicks%40==0)status("roof-wait",0,"stage="+roofStage+" latest="+(latest==null||latest.plan==null?"none":latest.plan.id+"/"+latest.plan.revision+"/"+latest.plan.base+"/"+latest.clipboard)+" held="+mc.player.inventory.currentItem);if(latest==null||latest.plan==null||!latest.plan.id.equals(roofId)||roofTicks<12)return;
        if(roofStage==1&&latest.clipboard){RoofPlan next=latest.plan.copy();next.base=1;next.frame=2;status("roof-edit",0,"revision="+next.revision+" base="+next.base+" id="+next.id+" held="+mc.player.inventory.currentItem);RoofNetwork.clipboard(roofId,0,next,false,null);roofStage=2;roofTicks=0;return;}
        if(roofStage==2&&latest.clipboard&&latest.plan.base==1){roofEdited=true;mc.player.inventory.currentItem=7;mc.getConnection().sendPacket(new CPacketHeldItemChange(7));mc.playerController.processRightClickBlock(mc.player,mc.world,null,roofBench,EnumFacing.UP,new Vec3d(roofBench).addVector(.5,1,.5),EnumHand.MAIN_HAND);roofStage=3;roofTicks=0;return;}
        if(roofStage==3&&!latest.clipboard&&mc.player.openContainer instanceof ArchitectWorkbench.Storage){RoofNetwork.request(roofBench,2,null,false);roofStage=4;roofTicks=0;return;}
        if(roofStage==4){if(roofTicks%20==0)RoofNetwork.request(roofBench,0,null,false);if(!latest.clipboard&&!latest.active&&latest.total>0&&latest.index==latest.total){roofStage=5;roofTicks=0;mc.player.closeScreen();mc.displayGuiScreen(null);status("roof-built",0,"clipboard revision "+latest.plan.revision+"; "+latest.total+" queued cell operations");}}
        if(roofStage==5&&mc.player.posY<110&&roofTicks>=12)roofBuilt=true;
    }
    static void aim(Minecraft mc,Vec3d target){double dx=target.xCoord-mc.player.posX,dy=target.yCoord-mc.player.posY-mc.player.getEyeHeight(),dz=target.zCoord-mc.player.posZ;mc.player.rotationYaw=(float)(Math.toDegrees(Math.atan2(dz,dx))-90);mc.player.rotationPitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));}
    void finish(Minecraft mc){stopped=true;Map<String,Object> result=new LinkedHashMap<>();result.put("passed",error.isEmpty()&&built&&operations==91&&verified);result.put("id",id);result.put("error",error);result.put("runtimeSha256",System.getProperty("iaa.runtimeSha256"));result.put("integratedServer",mc.getIntegratedServer()!=null);result.put("tcp",meter!=null&&meter.tcp);result.put("loginTimesMs",loginTimes);result.put("joinMs",joinedAt==0?null:Double.valueOf((joinedAt-connectAt)/1e6));result.put("operations",operations);result.put("clipboardEdited",roofEdited);result.put("clipboardRoofBuilt",roofBuilt);result.put("decodedRoofPayloadBytes",RoofNetwork.decodedBytes.get());result.put("decodedRoofCells",RoofNetwork.decodedCells.get());result.put("fpsCap",fps);result.put("heapLimitMiB",Runtime.getRuntime().maxMemory()/1048576);if(meter!=null){result.put("inboundBytes",meter.inbound.get());result.put("outboundBytes",meter.outbound.get());}Map<String,Object> stats=new LinkedHashMap<>();for(Map.Entry<String,List<Double>> e:frames.entrySet()){List<Double> a=e.getValue();if(a.isEmpty())continue;Collections.sort(a);Map<String,Object> row=new LinkedHashMap<>();row.put("frames",a.size());row.put("p50Ms",a.get((a.size()-1)/2));row.put("p95Ms",a.get((a.size()-1)*95/100));row.put("maxMs",a.get(a.size()-1));stats.put(e.getKey(),row);}result.put("frameIntervals",stats);MPFiles.write(folder.resolve("client-result.json"),result);if(id==0&&mc.world!=null)net.minecraft.util.ScreenShotHelper.saveScreenshot(mc.mcDataDir,"multiplayer-final.png",mc.displayWidth,mc.displayHeight,mc.getFramebuffer());System.out.println("IAA_MP_CLIENT_DONE id="+id+" error="+error);mc.shutdown();}
    void fail(String reason){if(stopped)return;error=reason;try{if(joined)status("error",0,reason);}catch(Throwable ignored){}finish(Minecraft.getMinecraft());}
}
