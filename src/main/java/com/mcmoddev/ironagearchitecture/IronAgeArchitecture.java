package com.mcmoddev.ironagearchitecture;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

@Mod(
        modid = IronAgeArchitecture.MODID,
        name = IronAgeArchitecture.NAME,
        version = IronAgeArchitecture.VERSION,
        updateJSON = IronAgeArchitecture.UPDATE_JSON,
        certificateFingerprint = "@FINGERPRINT@",
        acceptedMinecraftVersions = IronAgeArchitecture.MC_VERSION
)
public class IronAgeArchitecture {

    public static final String MODID = "examplemod";
    public static final String NAME = "Example Mod";
    //TODO Set the version here (Major.minor.bug The build number is added on by jenkins)
    public static final String VERSION = "0.0.0";
    public static final String UPDATE_JSON = "https://raw.githubusercontent.com/MinecraftModDevelopmentMods/IronAgeArchitecture/master/update.json";
    public static final String MC_VERSION = "[1.12, 1.12.2]";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @Mod.EventHandler
    public void onFingerprintViolation(@Nonnull FMLFingerprintViolationEvent event) {
        LOGGER.warn("Invalid fingerprint detected! The file " + event.getSource().getName() +
                " may have been tampered with. This version will NOT be supported! Please download the mod from " +
                "CurseForge for a supported and signed version of the mod.");
    }
}
