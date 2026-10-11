package github.nighter.smartspawner.production;

import java.util.*;
import java.nio.charset.StandardCharsets;

/** Explicit owner-approved retrospective rule. Assignment is not evidence of purchase. */
public final class LegacyProductionMigration {
    public record Row(String id, String world, int x, int y, int z, String lastPlayer) {}
    public record Claim(String farmKey, UUID owner, boolean ambiguous) {
        public static Claim none() { return new Claim(null, null, false); }
    }
    public record Decision(ProductionOwnershipRepository.Ownership ownership, String kind) {}
    private LegacyProductionMigration() {}

    public static boolean compatibleForStack(ProductionOwnershipRepository.Ownership target,
            ProductionOwnershipRepository.Ownership incoming) {
        return (incoming.owner() == null || incoming.owner().equals(target.owner()))
                && (incoming.farm() == null || incoming.farm().equals(target.farm()));
    }

    public static Decision decide(String authority, Row row, UUID lastPlayer, Claim claim,
            ProductionOwnershipRepository.Ownership existing) {
        if (existing != null && existing.owner() != null && existing.farm() != null)
            return new Decision(existing, "EXISTING_ASSIGNMENT");
        if (claim.ambiguous() && claim.farmKey() == null) return null;
        UUID owner = existing != null && existing.owner() != null ? existing.owner()
                : lastPlayer != null ? lastPlayer : claim.owner();
        if (owner == null) return null;
        String key = claim.farmKey() != null ? claim.farmKey()
                : "unprotected-cell:" + row.world() + ":" + Math.floorDiv(row.x(), 33) + ":" + Math.floorDiv(row.z(), 33);
        UUID farm = existing != null && existing.farm() != null ? existing.farm()
                : UUID.nameUUIDFromBytes(("SmartSpawner-migration-v1:" + authority + ":" + key).getBytes(StandardCharsets.UTF_8));
        String kind = existing != null && existing.owner() != null ? "RETAINED_OWNER_AUTO_FARM"
                : lastPlayer != null ? "LEGACY_LAST_INTERACTOR" : "LEGACY_PROTECTION_OWNER";
        return new Decision(new ProductionOwnershipRepository.Ownership(owner, farm), kind);
    }
}
