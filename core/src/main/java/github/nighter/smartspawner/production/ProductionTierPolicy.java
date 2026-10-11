package github.nighter.smartspawner.production;

import java.util.function.IntFunction;

/** Effective permission decisions only; no primary group or operator flag. */
public final class ProductionTierPolicy {
    public record Decision(boolean allowed, boolean explicitDeny) {}
    private ProductionTierPolicy() {}
    public static int resolve(IntFunction<Decision> capacity, IntFunction<Boolean> rank) {
        for (int tier = 100; tier >= 60; tier -= 10) {
            Decision decision = capacity.apply(tier);
            if (decision.explicitDeny()) continue;
            if (decision.allowed() || rank.apply(tier)) return tier;
        }
        return 50;
    }
}
