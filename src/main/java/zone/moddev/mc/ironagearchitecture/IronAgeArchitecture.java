package zone.moddev.mc.ironagearchitecture;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/** Permanent building components in the Iron Age series. */
@Mod(modid = IronAgeArchitecture.MODID, name = "Iron Age Architecture", version = "0.1.12.110021",
        acceptedMinecraftVersions = "[1.10.2]", dependencies = "required-after:Forge@[12.18.3.2511,)")
public final class IronAgeArchitecture {
    public static final String MODID = "ironagearchitecture";
    @SidedProxy(clientSide = "zone.moddev.mc.ironagearchitecture.client.ClientProxy",
            serverSide = "zone.moddev.mc.ironagearchitecture.CommonProxy")
    public static CommonProxy proxy;
    /** Compatibility alias; palette membership now controls the actual creative tabs. */
    @Deprecated public static final CreativeTabs TAB = EraTabs.tab(Era.LATE_MEDIEVAL_TUDOR);
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        ArchitectureConfig.load(event.getSuggestedConfigurationFile());
        ArchitectureBlocks.register();
        proxy.preInit();
    }
    @Mod.EventHandler public void init(FMLInitializationEvent event) { ArchitectureRecipes.register(); }
}
