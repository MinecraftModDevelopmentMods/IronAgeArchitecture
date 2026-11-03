package zone.moddev.mc.ironagearchitecture;

import java.util.EnumSet;
import org.junit.Test;
import static org.junit.Assert.*;

public class EraSelectionTest {
    @Test public void defaultAllIncludesEveryEra() {assertEquals(EnumSet.allOf(Era.class),EraSelection.parse("all").allowed);}
    @Test public void oneOrMoreIdsAreAllowed() {
        assertEquals(EnumSet.of(Era.IRON_AGE),EraSelection.parse("iron_age").allowed);
        assertEquals(EnumSet.allOf(Era.class),EraSelection.parse("iron_age","late_medieval_tudor").allowed);
    }
    @Test public void sharedMaterialsNeedOnlyOneAllowedEra() {
        assertTrue(EraSelection.parse("iron_age").permits(EnumSet.allOf(Era.class)));
        assertFalse(EraSelection.parse("iron_age").permits(EnumSet.of(Era.LATE_MEDIEVAL_TUDOR)));
    }
    @Test public void emptySelectionDisablesRecipes() {assertFalse(EraSelection.parse().permits(EnumSet.allOf(Era.class)));}
    @Test public void invalidIdsNeverEnableForbiddenEras() {
        EraSelection selection=EraSelection.parse("unknown-era");assertTrue(selection.allowed.isEmpty());assertEquals(1,selection.unknown.size());
    }
    @Test public void whitespaceCaseAndDuplicatesAreHarmless() {
        assertEquals(EnumSet.of(Era.IRON_AGE),EraSelection.parse(" IRON_AGE ","iron_age","").allowed);
    }
}
