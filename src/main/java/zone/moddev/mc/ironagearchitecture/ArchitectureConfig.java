package zone.moddev.mc.ironagearchitecture;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

/** Complete content-family switches. Framed panels require the cob family. */
public final class ArchitectureConfig {
    public static boolean cob, smartCob, frames, thatch, slate, windows, clayTiles, fireplaces;
    public static BuildingMode buildingMode = BuildingMode.AUTOMATIC;
    public static boolean retainLegacyWallIds;
    public static boolean registerManualWalls() { return buildingMode.manual() || retainLegacyWallIds; }
    public static EraSelection survivalEras=EraSelection.parse("all");
    private ArchitectureConfig() { }
    public static void load(File file) {
        boolean existingConfig = file.isFile();
        Configuration config = new Configuration(file);
        config.load();
        cob = config.getBoolean("cob", "blocks", true, "Raw and limewashed cob blocks, stairs and slabs.");
        smartCob=config.getBoolean("smartCob","blocks",true,"Rounded smart cob walls; requires cob. Keeps its block ID in Manual mode so saved walls remain readable.")&&cob;
        frames = config.getBoolean("timberFrames", "blocks", true, "All six timber frames; also requires cob.") && cob;
        windows = config.getBoolean("leadedWindows", "blocks", true, "Openable leaded windows; requires timberFrames and cob.") && frames;
        thatch = config.getBoolean("thatch", "blocks", true, "Thatch slopes and ridge caps.");
        slate = config.getBoolean("slate", "blocks", true, "Crafted slate slopes and ridge caps.");
        clayTiles = config.getBoolean("clayTiles", "blocks", true, "Clay tile roofs, ridge caps and hanging tiles.");
        fireplaces = config.getBoolean("fireplaces", "blocks", true, "Hearths, fireboxes, surrounds and chimney parts.");
        net.minecraftforge.common.config.Property eras=config.get("recipes","survivalEras",new String[]{"all"},
            "Allowed survival palettes: all, iron_age, late_medieval_tudor. Select one or more IDs. An empty list disables Architecture recipes. Shared materials stay craftable when any selected era uses them. Does not hide creative items or remove existing blocks. Restart required.");
        eras.setRequiresMcRestart(true);
        survivalEras=EraSelection.parse(eras.getStringList());
        for(String unknown:survivalEras.unknown) org.apache.logging.log4j.LogManager.getLogger("IronAgeArchitecture").warn("Ignoring unknown survival era '{}'. Valid IDs: iron_age, late_medieval_tudor, all",unknown);
        net.minecraftforge.common.config.Property mode = config.get("building", "mode", "automatic",
            "Wall building mode: automatic, hybrid or manual. Automatic uses smart walls and omits manual wall block IDs unless retainLegacyWallIds is enabled. Match the server settings. Restart required.");
        mode.setRequiresMcRestart(true);
        buildingMode = BuildingMode.parse(mode.getString());
        net.minecraftforge.common.config.Property legacy = config.get("building", "retainLegacyWallIds", existingConfig,
            "Keep the older manual wall and separate smart-corner IDs when loading existing worlds. Enabled when upgrading an existing config. New installations default to false to save block IDs. Do not turn this off for a world containing those blocks. Match the server settings. Restart required.");
        legacy.setRequiresMcRestart(true);
        retainLegacyWallIds = legacy.getBoolean();
        if (config.hasChanged()) config.save();
    }
}
