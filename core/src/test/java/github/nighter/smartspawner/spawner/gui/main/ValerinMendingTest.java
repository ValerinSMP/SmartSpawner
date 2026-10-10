package github.nighter.smartspawner.spawner.gui.main;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValerinMendingTest {
    @Test
    void legacyRepairEnchantmentsAreRecognized() {
        for (String key : new String[]{"mending", "repairing", "repair", "REPAIRING"}) {
            assertTrue(SpawnerMenuAction.isMendingEnchantKey(key), key);
        }
    }

    @Test
    void unrelatedEnchantmentsDoNotConsumeRepairExperience() {
        for (String key : new String[]{"unbreaking", "repair_cost", "mending_bonus"}) {
            assertFalse(SpawnerMenuAction.isMendingEnchantKey(key), key);
        }
    }
}
