package github.nighter.smartspawner.production;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProductionTierPolicyTest {
    @Test void defaultsAndAllInheritedRankTiers() {
        assertEquals(50, ProductionTierPolicy.resolve(t -> new ProductionTierPolicy.Decision(false, false), t -> false));
        for (int expected : new int[]{60,70,80,90,100}) {
            assertEquals(expected, ProductionTierPolicy.resolve(t -> new ProductionTierPolicy.Decision(false, false),
                    t -> t <= expected));
        }
    }
    @Test void effectiveDenialsBeatRankFallbackAndNumericGrantsStillWork() {
        assertEquals(90, ProductionTierPolicy.resolve(t -> new ProductionTierPolicy.Decision(false, t == 100),
                t -> true));
        assertEquals(50, ProductionTierPolicy.resolve(t -> new ProductionTierPolicy.Decision(false, true),
                t -> true));
        assertEquals(80, ProductionTierPolicy.resolve(t -> new ProductionTierPolicy.Decision(t == 80, false),
                t -> false));
    }
}
