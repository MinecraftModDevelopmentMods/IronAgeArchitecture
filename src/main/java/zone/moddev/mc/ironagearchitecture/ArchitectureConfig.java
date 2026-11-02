package zone.moddev.mc.ironagearchitecture;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

/** Complete content-family switches. Framed panels require the cob family. */
public final class ArchitectureConfig {
    public static boolean cob, frames, thatch, slate;
    private ArchitectureConfig() { }
    public static void load(File file) {
        Configuration config = new Configuration(file);
        config.load();
        cob = config.getBoolean("cob", "blocks", true, "Raw and limewashed cob blocks, stairs and slabs.");
        frames = config.getBoolean("timberFrames", "blocks", true, "All six timber frames; also requires cob.") && cob;
        thatch = config.getBoolean("thatch", "blocks", true, "Thatch slopes and ridge caps.");
        slate = config.getBoolean("slate", "blocks", true, "Crafted slate slopes and ridge caps.");
        if (config.hasChanged()) config.save();
    }
}
