package zone.moddev.mc.ironagearchitecture;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

/** Complete content-family switches. Framed panels require the cob family. */
public final class ArchitectureConfig {
    public static boolean cob, frames, thatch, slate, windows, clayTiles, fireplaces;
    public static EraSelection survivalEras=EraSelection.parse("all");
    private ArchitectureConfig() { }
    public static void load(File file) {
        Configuration config = new Configuration(file);
        config.load();
        cob = config.getBoolean("cob", "blocks", true, "Raw and limewashed cob blocks, stairs and slabs.");
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
        if (config.hasChanged()) config.save();
    }
}
