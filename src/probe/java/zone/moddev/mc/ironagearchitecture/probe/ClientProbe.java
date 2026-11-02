package zone.moddev.mc.ironagearchitecture.probe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import zone.moddev.mc.ironagearchitecture.ArchitectureBlocks;
import zone.moddev.mc.ironagearchitecture.block.RoofBlock;
import zone.moddev.mc.ironagearchitecture.client.RoofBakedModel;
import zone.moddev.mc.ironagearchitecture.block.FrameBlock;
import zone.moddev.mc.ironagearchitecture.client.WallBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;

/** Opt-in native client capture. No registration or launch path in the player mod. */
public final class ClientProbe extends ProbeProxy {
    private int phase, ticks;
    @Override public void init() { if(Boolean.getBoolean("iaa.clientProbe")) FMLCommonHandler.instance().bus().register(this); }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getMinecraft();
        ticks++;
        if(phase==0&&mc.currentScreen instanceof GuiMainMenu&&ticks>60) {
            mc.gameSettings.pauseOnLostFocus=false;
            phase=1;ticks=0;
            mc.launchIntegratedServer("Jettied Street Preview","Jettied Street Preview",new WorldSettings(137,GameType.CREATIVE,false,false,WorldType.FLAT));
        } else if(phase==1&&mc.player!=null) {
            phase=2;ticks=0;
            mc.displayGuiScreen(null);mc.gameSettings.hideGUI=true;mc.gameSettings.renderDistanceChunks=8;
            mc.player.capabilities.allowFlying=true;mc.player.capabilities.isFlying=true;
            lookAt(mc,48,102,-85,48,73,4);
        } else if(phase==2&&ticks>260) {
            for(RoofBlock roof:ArchitectureBlocks.ROOFS) for(IBlockState state:roof.getBlockState().getValidStates()) {
                IBakedModel model=mc.getBlockRendererDispatcher().getModelForState(state);
                if(!(model instanceof RoofBakedModel)||model.getQuads(state,null,0).isEmpty()) throw new IllegalStateException("Roof model missing "+state);
            }
            for(FrameBlock wall:ArchitectureBlocks.meshes()) {
                for(IBlockState state:wall.getBlockState().getValidStates()) {
                    IBakedModel model=mc.getBlockRendererDispatcher().getModelForState(state);
                    if(!(model instanceof WallBakedModel)||model.getQuads(state,null,0).isEmpty()) throw new IllegalStateException("Wall model missing "+state);
                    for(net.minecraft.client.renderer.block.model.BakedQuad quad:model.getQuads(state,null,0))
                        if(quad.getSprite().getIconName().contains("missingno")) throw new IllegalStateException("Missing material sprite "+state);
                }
                for(int meta=0;ArchitectureBlocks.VISIBLE.contains(wall)&&meta<wall.variants();meta++) {
                    IBakedModel model=mc.getRenderItem().getItemModelMesher().getItemModel(new ItemStack(wall,1,meta));
                    if(!(model instanceof WallBakedModel)||model.getQuads(null,null,0).isEmpty()) throw new IllegalStateException("Wall item model missing "+wall+" "+meta);
                }
            }
            capture(mc,"jettied-street-overview.png");
            lookAt(mc,-5,74,-7,3,69.5,2);phase=3;ticks=0;
        } else if(phase==3&&ticks>120) {
            capture(mc,"half-block-corner.png");lookAt(mc,24,65.6,-3.5,24,68.7,-.2);phase=4;ticks=0;
        } else if(phase==4&&ticks>120) {
            capture(mc,"whole-block-underside.png");
            lookAt(mc,34,81,-10,43,73,1);phase=5;ticks=0;
        } else if(phase==5&&ticks>120) {
            capture(mc,"successive-half-jetties.png");lookAt(mc,55,80,-9,64,72,1);phase=6;ticks=0;
        } else if(phase==6&&ticks>120) {
            capture(mc,"two-sided-jetty.png");lookAt(mc,5,71,5,5,70,-1);phase=7;ticks=0;
        } else if(phase==7&&ticks>120) {
            capture(mc,"upper-room-interior.png");
            System.out.println("IAA_CLIENT_PROBE_PASS");phase=8;
            mc.shutdown();
        }
    }
    private void lookAt(Minecraft mc,double x,double y,double z,double tx,double ty,double tz) {
        double dx=tx-x,dz=tz-z;
        float yaw=(float)(Math.toDegrees(Math.atan2(dz,dx))-90);
        float pitch=(float)Math.toDegrees(Math.atan2(y+mc.player.getEyeHeight()-ty,Math.sqrt(dx*dx+dz*dz)));
        mc.player.setPositionAndRotation(x,y,z,yaw,pitch);
    }
    private void capture(Minecraft mc,String name) {
        ScreenShotHelper.saveScreenshot(mc.mcDataDir,name,mc.displayWidth,mc.displayHeight,mc.getFramebuffer());
    }
}
