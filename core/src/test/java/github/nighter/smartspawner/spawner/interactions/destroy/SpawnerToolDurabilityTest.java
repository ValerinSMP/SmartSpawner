package github.nighter.smartspawner.spawner.interactions.destroy;

import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import static org.junit.jupiter.api.Assertions.*;

class SpawnerToolDurabilityTest {
    @Test void ecoPickaxeSurvivesVanillaBreakingPoint() {
        Fixture f = new Fixture(2030, 8124, false);
        f.damage(1);
        assertFalse(f.broken);
        assertEquals(2031, f.damage);
        assertTrue(f.metaApplied);
    }
    @Test void ecoPickaxeSurvivesStackRemovalAfterVanillaLimit() {
        Fixture f = new Fixture(4000, 8124, false);
        f.damage(50);
        assertFalse(f.broken);
        assertEquals(4050, f.damage);
    }
    @Test void ecoPickaxeBreaksAtItsActualLimit() {
        Fixture f = new Fixture(8123, 8124, false);
        f.damage(1);
        assertTrue(f.broken);
        assertFalse(f.metaApplied);
    }
    @Test void lowerCustomDurabilityIsAlsoRespected() {
        Fixture f = new Fixture(99, 100, false);
        f.damage(1);
        assertTrue(f.broken);
    }
    @Test void vanillaPickaxeRetainsItsNormalBreakingPoint() {
        Fixture f = new Fixture(2030, null, false);
        f.damage(1);
        assertTrue(f.broken);
    }
    @Test void vanillaPickaxeRetainsNormalDamageBelowLimit() {
        Fixture f = new Fixture(100, null, false);
        f.damage(50);
        assertEquals(150, f.damage);
        assertFalse(f.broken);
    }
    @Test void unbreakablePickaxeIsUntouched() {
        Fixture f = new Fixture(4000, 8124, true);
        f.damage(50);
        assertEquals(4000, f.damage);
        assertFalse(f.metaApplied);
        assertFalse(f.broken);
    }
    @Test void disabledDurabilityLossIsUntouchedEvenAboveVanillaLimit() {
        Fixture f = new Fixture(4000, 8124, false);
        f.damage(0);
        assertEquals(4000, f.damage);
        assertFalse(f.metaApplied);
        assertFalse(f.broken);
    }
    @Test void largeCustomDamageDoesNotOverflowAndRepairTheTool() {
        Fixture f = new Fixture(Integer.MAX_VALUE - 10, Integer.MAX_VALUE, false);
        f.damage(50);
        assertTrue(f.broken);
        assertFalse(f.metaApplied);
    }

    private static final class Fixture {
        int damage;
        boolean broken, metaApplied;
        final Damageable meta;
        Fixture(int damage, Integer customMax, boolean unbreakable) {
            this.damage = damage;
            meta = (Damageable) Proxy.newProxyInstance(Damageable.class.getClassLoader(),
                new Class<?>[]{Damageable.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isUnbreakable" -> unbreakable;
                    case "hasMaxDamage" -> customMax != null;
                    case "getMaxDamage" -> {
                        assertNotNull(customMax, "getMaxDamage requires hasMaxDamage");
                        yield customMax;
                    }
                    case "getDamage" -> this.damage;
                    case "setDamage" -> { this.damage = (int) args[0]; metaApplied = true; yield null; }
                    default -> throw new AssertionError("Unexpected metadata mutation: " + method.getName());
                });
        }
        void damage(int loss) { broken = SpawnerBreakListener.applyDurabilityLoss(meta, 2031, loss); }
    }
}
