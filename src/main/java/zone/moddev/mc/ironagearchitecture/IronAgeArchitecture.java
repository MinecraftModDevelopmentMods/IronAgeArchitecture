package zone.moddev.mc.ironagearchitecture;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/** Permanent building components in the Iron Age series. */
@Mod(modid = IronAgeArchitecture.MODID, name = "Iron Age Architecture", version = "0.1.2.110021",
        acceptedMinecraftVersions = "[1.10.2]", dependencies = "required-after:Forge@[12.18.3.2511,)")
public final class IronAgeArchitecture {
    public static final String MODID = "ironagearchitecture";
    @SidedProxy(clientSide = "zone.moddev.mc.ironagearchitecture.client.ClientProxy",
            serverSide = "zone.moddev.mc.ironagearchitecture.CommonProxy")
    public static CommonProxy proxy;
    public static final CreativeTabs TAB = new CreativeTabs(MODID) {
        @Override public Item getTabIconItem() {
            return ArchitectureBlocks.COB.isEmpty() ? Item.getItemFromBlock(Blocks.BRICK_BLOCK)
                    : Item.getItemFromBlock(ArchitectureBlocks.COB.get(0));
        }
    };
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        ArchitectureConfig.load(event.getSuggestedConfigurationFile());
        ArchitectureBlocks.register();
        proxy.preInit();
    }
    @Mod.EventHandler public void init(FMLInitializationEvent event) { ArchitectureRecipes.register(); }
}
