package zone.moddev.mc.ironagearchitecture.block;

/** Two deliberate patterns per family; finish and facing still fit four-bit metadata. */
public enum FrameFamily {
    POST("timber_frame", "Post", "Cross Brace"),
    RAIL("timber_rail", "Sill Rail", "Head Rail"),
    STUD("timber_stud", "Stud", "Close Studs"),
    BRACE("timber_brace", "Rising Brace", "Falling Brace"),
    TERMINAL("timber_terminal", "Corner Post", "End Post"),
    INFILL("wattle_and_daub", "Infill", "Infill");

    public final String registryPrefix;
    public final String firstName, secondName;
    FrameFamily(String prefix, String first, String second) {
        registryPrefix=prefix; firstName=first; secondName=second;
    }
}
