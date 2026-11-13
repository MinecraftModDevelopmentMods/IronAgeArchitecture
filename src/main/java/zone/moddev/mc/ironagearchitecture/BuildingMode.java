package zone.moddev.mc.ironagearchitecture;

import java.util.Locale;

/** Controls wall acquisition and, with the compatibility setting, registration. */
public enum BuildingMode {
    MANUAL, HYBRID, AUTOMATIC;

    public boolean manual() { return this != AUTOMATIC; }
    public boolean automatic() { return this != MANUAL; }

    public static BuildingMode parse(String value) {
        try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException failure) { return AUTOMATIC; }
    }
}
