package zone.moddev.mc.ironagearchitecture.client;

import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import zone.moddev.mc.ironagearchitecture.roof.*;

/** A personal, bounded construction ghost, rebuilt only when a server drawing changes. */
public final class WorkbenchClient {
    public static RoofNetwork.Reply latest;
    private static List<double[][]> ghost=Collections.emptyList();
    private static boolean show;
    public static void reply(RoofNetwork.Reply r){Minecraft mc=Minecraft.getMinecraft();if(mc.world==null||mc.world.provider.getDimension()!=r.dimension)return;latest=r;show=r.preview&&!r.active;List<double[][]> next=new ArrayList<>();if(show&&r.plan!=null)for(Map.Entry<Long,RoofCell> entry:r.plan.cells.entrySet()){BlockPos at=BlockPos.fromLong(entry.getKey());for(RoofMeshes.Face face:RoofMeshes.get(entry.getValue(),0).faces){double[][] points=new double[face.vertices.length][];for(int i=0;i<points.length;i++){double[] p=face.vertices[i];points[i]=new double[]{p[0]+at.getX(),p[1]+at.getY(),p[2]+at.getZ()};}next.add(points);}}ghost=Collections.unmodifiableList(next);if(mc.currentScreen instanceof Screen)((Screen)mc.currentScreen).receive(r);}
    @SubscribeEvent public void unload(WorldEvent.Unload e){if(e.getWorld().isRemote){latest=null;show=false;ghost=Collections.emptyList();}}
    @SubscribeEvent public void render(RenderWorldLastEvent e){Minecraft mc=Minecraft.getMinecraft();if(!show||latest==null||mc.player==null||mc.player.getDistanceSq(latest.pos)>4096)return;net.minecraft.entity.Entity camera=mc.getRenderViewEntity();if(camera==null)return;double x=camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*e.getPartialTicks(),y=camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*e.getPartialTicks(),z=camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*e.getPartialTicks();boolean clear=latest.status.startsWith("Ready");GlStateManager.pushMatrix();GlStateManager.translate(-x,-y,-z);GlStateManager.disableTexture2D();GlStateManager.disableLighting();GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);GlStateManager.depthMask(false);GlStateManager.disableCull();Tessellator t=Tessellator.getInstance();VertexBuffer b=t.getBuffer();b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_COLOR);for(double[][] face:ghost)for(int i=0;i<4;i++){double[] p=face[Math.min(i,face.length-1)];b.pos(p[0],p[1],p[2]).color(clear?.28f:1f,clear?.85f:.35f,.5f,.35f).endVertex();}t.draw();GlStateManager.depthMask(true);GlStateManager.enableCull();GlStateManager.disableBlend();GlStateManager.enableTexture2D();GlStateManager.popMatrix();}
    public static final class Screen extends GuiContainer {
        private final ArchitectWorkbench.Desk desk;
        private RoofPlan plan;
        private RoofNetwork.Reply state;
        private int pending;
        private boolean preview;
        private final int mapX=194,mapY=102,mapSize=90;
        private int dragX,dragZ;private boolean dragging;
        private boolean holes;
        public Screen(EntityPlayer player,ArchitectWorkbench.Desk desk){super(new ArchitectWorkbench.Storage(player.inventory,desk));this.desk=desk;xSize=320;ySize=240;}
        @Override public void initGui(){super.initGui();buttons();RoofNetwork.request(desk.getPos(),0,null,false);}
        private void buttons(){
            buttonList.clear();buttonList.add(new GuiButton(0,guiLeft+8,guiTop+18,98,14,"Detect floor"));buttonList.add(new GuiButton(1,guiLeft+110,guiTop+18,98,14,"Rotate ridge"));buttonList.add(new GuiButton(2,guiLeft+212,guiTop+18,100,14,"Timber"));
            for(int i=0;i<4;i++){int x=i%2==0?8:168,y=guiTop+34+(i/2)*16;buttonList.add(new GuiButton(10+i*2,guiLeft+x,y,18,14,"-"));buttonList.add(new GuiButton(11+i*2,guiLeft+x+126,y,18,14,"+"));}
            buttonList.add(new GuiButton(3,guiLeft+8,guiTop+67,98,16,"Show preview"));buttonList.add(new GuiButton(4,guiLeft+110,guiTop+67,98,16,"Build frame"));buttonList.add(new GuiButton(5,guiLeft+212,guiTop+67,100,16,"Cancel"));buttonList.add(new GuiButton(6,guiLeft+179,guiTop+85,133,14,"Map: floor bounds"));
        }
        public void receive(RoofNetwork.Reply r){if(!r.pos.equals(desk.getPos()))return;state=r;plan=r.plan==null?null:r.plan.copy();preview=r.preview;for(GuiButton b:buttonList){b.enabled=!r.active||b.id==5;if(b.id==5)b.enabled=r.active;if(b.id==1&&plan!=null)b.displayString=plan.alongX?"Ridge E / W":"Ridge N / S";if(b.id==3)b.displayString=preview?"Hide preview":"Show preview";if(b.id==4)b.enabled=r.canBuild;if(b.id==4)b.displayString=r.active?"Building...":"Build / repair";if(b.id==2)b.displayString=plan==null?"Timber":zone.moddev.mc.ironagearchitecture.ArchitectureBlocks.WOODS[plan.wood].replace('_',' ');}}
        @Override public void updateScreen(){super.updateScreen();if(pending>0&&--pending==0)send(0);if(state!=null&&state.active&&mc.player.ticksExisted%20==0)RoofNetwork.request(desk.getPos(),0,null,false);}
        private void send(int action){RoofNetwork.request(desk.getPos(),action,plan,preview);}
        @Override protected void actionPerformed(GuiButton b)throws IOException {if(b.id==0){send(4);return;}if(b.id==5){send(3);return;}if(plan==null)return;if(b.id==1)plan.alongX=!plan.alongX;else if(b.id==2)plan.wood=(plan.wood+1)%6;else if(b.id==3){preview=!preview;send(1);return;}else if(b.id==4){send(2);return;}else if(b.id==6){holes=!holes;b.displayString=holes?"Map: roof openings":"Map: floor bounds";return;}else if(b.id>=10){int row=(b.id-10)/2,delta=(b.id&1)==0?-1:1;if(row==0)plan.pitch=Math.max(0,Math.min(2,plan.pitch+delta));if(row==1)plan.base=Math.max(0,Math.min(8,plan.base+delta));if(row==2)plan.eave=Math.max(0,Math.min(2,plan.eave+delta));if(row==3)plan.gable=Math.max(0,Math.min(2,plan.gable+delta));}pending=5;}
        private int mapScale(){return plan==null?4:Math.max(2,Math.min(10,mapSize/Math.max(plan.width()+4,plan.length()+4)));}
        private int[] mapCell(int x,int y){if(plan==null)return null;int scale=mapScale(),mx=x-guiLeft-mapX,my=y-guiTop-mapY;if(mx<0||my<0||mx>=mapSize||my>=mapSize)return null;return new int[]{plan.minX-2+mx/scale,plan.minZ-2+my/scale};}
        @Override protected void mouseClicked(int x,int y,int button)throws IOException{super.mouseClicked(x,y,button);if(button!=0||plan==null||state!=null&&state.active)return;int[] p=mapCell(x,y);if(p==null)return;if(holes){if(p[0]>=plan.minX&&p[0]<=plan.maxX&&p[1]>=plan.minZ&&p[1]<=plan.maxZ){long key=RoofPlan.column(p[0],p[1]);if(!plan.openings.remove(key))plan.openings.add(key);pending=5;}}else{dragX=p[0];dragZ=p[1];dragging=true;}}
        @Override protected void mouseReleased(int x,int y,int button){super.mouseReleased(x,y,button);if(!dragging||plan==null)return;int[] p=mapCell(x,y);if(p!=null){plan.minX=Math.min(dragX,p[0]);plan.maxX=Math.max(dragX,p[0]);plan.minZ=Math.min(dragZ,p[1]);plan.maxZ=Math.max(dragZ,p[1]);pending=5;}dragging=false;}
        @Override protected void drawGuiContainerBackgroundLayer(float partial,int mx,int my){drawRect(guiLeft,guiTop,guiLeft+xSize,guiTop+ySize,0xffddd4c2);drawRect(guiLeft+5,guiTop+94,guiLeft+174,guiTop+236,0xffb8ad98);for(SlotBox slot:slots()){drawRect(guiLeft+slot.x-1,guiTop+slot.y-1,guiLeft+slot.x+17,guiTop+slot.y+17,0xffd0c7b3);drawRect(guiLeft+slot.x,guiTop+slot.y,guiLeft+slot.x+16,guiTop+slot.y+16,0xff827867);}drawRect(guiLeft+mapX-2,guiTop+mapY-2,guiLeft+mapX+mapSize+2,guiTop+mapY+mapSize+2,0xff4b5147);if(plan!=null){int scale=mapScale();for(int x=plan.minX;x<=plan.maxX;x++)for(int z=plan.minZ;z<=plan.maxZ;z++){int px=guiLeft+mapX+(x-plan.minX+2)*scale,pz=guiTop+mapY+(z-plan.minZ+2)*scale;if(px+scale>guiLeft+mapX+mapSize||pz+scale>guiTop+mapY+mapSize)continue;int color=plan.openings.contains(RoofPlan.column(x,z))?0xff242a28:0xffb59b71;drawRect(px,pz,px+scale-1,pz+scale-1,color);}int bx=guiLeft+mapX+(desk.getPos().getX()-plan.minX+2)*scale,bz=guiTop+mapY+(desk.getPos().getZ()-plan.minZ+2)*scale;drawRect(bx,bz,bx+scale-1,bz+scale-1,0xff83baea);}}
        private static final class SlotBox {int x,y;SlotBox(int x,int y){this.x=x;this.y=y;}}
        private List<SlotBox> slots(){List<SlotBox> out=new ArrayList<>();for(net.minecraft.inventory.Slot s:inventorySlots.inventorySlots)out.add(new SlotBox(s.xPos,s.yPos));return out;}
        @Override protected void drawGuiContainerForegroundLayer(int mx,int my){
            fontRendererObj.drawString("Architect's Workbench",8,6,0x30291e);
            if(plan!=null){fontRendererObj.drawString("Ridge: +"+(plan.base*.5+.25+RoofPlan.PITCHES[plan.pitch]*(plan.alongX?plan.length():plan.width())/2),178,6,0x30291e);String[] text={new String[]{"Shallow 1:2","Standard 1:1","Steep 2:1"}[plan.pitch],"Base: "+plan.base*.5,"Eaves: "+plan.eave*.5,"Gables: "+plan.gable*.5};for(int i=0;i<4;i++)fontRendererObj.drawString(text[i],i%2==0?30:190,37+(i/2)*16,0x30291e);fontRendererObj.drawString(plan.width()+" x "+plan.length()+" | "+plan.timber()+" timber",178,194,0x30291e);}
            String status=state==null?"Reading drawing...":state.status;List<String> lines=fontRendererObj.listFormattedStringToWidth(status,134);int y=205;for(String line:lines){fontRendererObj.drawString(line,178,y,status.startsWith("Ready")?0x285635:0x82372a);y+=9;if(y>233)break;}
            fontRendererObj.drawString("Materials",12,87,0x30291e);
        }
        @Override public void drawScreen(int mx,int my,float partial){drawDefaultBackground();super.drawScreen(mx,my,partial);}
    }
}
