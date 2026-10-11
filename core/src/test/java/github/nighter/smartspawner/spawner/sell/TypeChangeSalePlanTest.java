package github.nighter.smartspawner.spawner.sell;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TypeChangeSalePlanTest {
    @Test void valuesAllExistingLootBeforeTypeChange() {
        var plan = TypeChangeSalePlan.requireAllPriced(Map.of("rod", 10L, "dust", 5L),
                item -> item.equals("rod") ? 2.0 : 3.0);
        assertEquals(35.0, plan.value());
        assertEquals(15, plan.quantity());
    }
    @Test void oneUnpricedItemRejectsWholeSaleAndDoesNotModifyStock() {
        var stock = Map.of("rod", 10L, "unpriced", 5L);
        assertThrows(IllegalArgumentException.class, () -> TypeChangeSalePlan.requireAllPriced(stock,
                item -> item.equals("rod") ? 2.0 : 0.0));
        assertEquals(15, stock.values().stream().mapToLong(Long::longValue).sum());
    }
    @Test void nonFinitePricesNegativeQuantitiesAndOverflowAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> TypeChangeSalePlan.requireAllPriced(Map.of("a", 1L), k -> Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> TypeChangeSalePlan.requireAllPriced(Map.of("a", 1L), k -> Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> TypeChangeSalePlan.requireAllPriced(Map.of("a", -1L), k -> 1));
        assertThrows(IllegalArgumentException.class, () -> TypeChangeSalePlan.requireAllPriced(Map.of("a", 2L), k -> Double.MAX_VALUE));
    }
}
