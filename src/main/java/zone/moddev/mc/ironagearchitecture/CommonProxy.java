package zone.moddev.mc.ironagearchitecture;

/** Keeps client model classes off dedicated servers. */
public class CommonProxy {
    public void preInit() {
        zone.moddev.mc.ironagearchitecture.block.WindowEvents events=new zone.moddev.mc.ironagearchitecture.block.WindowEvents();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(events);
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(events);
    }
}
