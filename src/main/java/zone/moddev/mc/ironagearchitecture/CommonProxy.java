package zone.moddev.mc.ironagearchitecture;

/** Keeps client model classes off dedicated servers. */
public class CommonProxy {
    public zone.moddev.mc.ironagearchitecture.block.SmartCobProfile cobProfile(net.minecraft.world.IBlockAccess access,net.minecraft.util.math.BlockPos pos) {
        return access instanceof net.minecraft.world.World?zone.moddev.mc.ironagearchitecture.block.SmartCobData.get((net.minecraft.world.World)access).profile(pos):zone.moddev.mc.ironagearchitecture.block.SmartCobProfile.SQUARE;
    }
    public void cobSnapshot(SmartCobNetwork.Packet packet) { }
    public int smartLayout(net.minecraft.world.IBlockAccess access,net.minecraft.util.math.BlockPos pos) {
        if(access instanceof net.minecraft.world.World)
            return zone.moddev.mc.ironagearchitecture.block.SmartWallData.get((net.minecraft.world.World)access).code(pos);
        return 0;
    }
    public void smartSnapshot(SmartWallNetwork.Packet packet) { }
    public void preInit() {
        SmartCobNetwork.register();
        zone.moddev.mc.ironagearchitecture.block.SmartCobEvents cob=new zone.moddev.mc.ironagearchitecture.block.SmartCobEvents();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(cob);
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(cob);
        SmartWallNetwork.register();
        zone.moddev.mc.ironagearchitecture.block.SmartWallEvents smart=new zone.moddev.mc.ironagearchitecture.block.SmartWallEvents();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(smart);
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(smart);
        zone.moddev.mc.ironagearchitecture.block.WindowEvents events=new zone.moddev.mc.ironagearchitecture.block.WindowEvents();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(events);
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(events);
    }
}
