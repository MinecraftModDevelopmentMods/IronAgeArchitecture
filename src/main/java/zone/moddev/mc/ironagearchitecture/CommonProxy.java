package zone.moddev.mc.ironagearchitecture;

/** Keeps client model classes off dedicated servers. */
public class CommonProxy {
    public int smartLayout(net.minecraft.world.IBlockAccess access,net.minecraft.util.math.BlockPos pos) {
        if(access instanceof net.minecraft.world.World)
            return zone.moddev.mc.ironagearchitecture.block.SmartWallData.get((net.minecraft.world.World)access).code(pos);
        return 0;
    }
    public void smartSnapshot(SmartWallNetwork.Packet packet) { }
    public void preInit() {
        SmartWallNetwork.register();
        zone.moddev.mc.ironagearchitecture.block.SmartWallEvents smart=new zone.moddev.mc.ironagearchitecture.block.SmartWallEvents();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(smart);
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(smart);
        zone.moddev.mc.ironagearchitecture.block.WindowEvents events=new zone.moddev.mc.ironagearchitecture.block.WindowEvents();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(events);
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(events);
    }
}
