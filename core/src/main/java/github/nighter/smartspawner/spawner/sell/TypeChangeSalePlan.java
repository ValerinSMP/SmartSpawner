package github.nighter.smartspawner.spawner.sell;

import java.util.Map;
import java.util.function.ToDoubleFunction;

/** All-or-nothing preflight. Never silently sells or discards an unpriced item. */
public record TypeChangeSalePlan(double value, long quantity) {
    public static <K> TypeChangeSalePlan requireAllPriced(Map<K, Long> stock, ToDoubleFunction<K> prices) {
        double value = 0;
        long quantity = 0;
        for (var entry : stock.entrySet()) {
            double price = prices.applyAsDouble(entry.getKey());
            if (entry.getValue() <= 0 || !Double.isFinite(price) || price <= 0)
                throw new IllegalArgumentException("Unpriced or invalid stock");
            quantity = Math.addExact(quantity, entry.getValue());
            value += price * entry.getValue();
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Invalid sale total");
        }
        return new TypeChangeSalePlan(value, quantity);
    }
}
