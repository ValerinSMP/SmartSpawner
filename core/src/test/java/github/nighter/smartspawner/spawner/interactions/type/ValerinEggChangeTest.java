package github.nighter.smartspawner.spawner.interactions.type;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValerinEggChangeTest {
    @Test
    void onlySingleSpawnersPassTheDefaultLimit() {
        assertFalse(SpawnEggHandler.exceedsEggChangeStackLimit(1, 1));
        assertTrue(SpawnEggHandler.exceedsEggChangeStackLimit(2, 1));
        assertTrue(SpawnEggHandler.exceedsEggChangeStackLimit(1000, 1));
    }

    @Test
    void configuredLimitAndLegacyDisableValuesAreHonored() {
        assertFalse(SpawnEggHandler.exceedsEggChangeStackLimit(5, 5));
        assertTrue(SpawnEggHandler.exceedsEggChangeStackLimit(6, 5));
        assertFalse(SpawnEggHandler.exceedsEggChangeStackLimit(1000, 0));
        assertFalse(SpawnEggHandler.exceedsEggChangeStackLimit(1000, -1));
    }
}
