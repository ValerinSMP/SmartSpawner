package github.nighter.smartspawner.production;

import java.util.*;
import java.nio.charset.StandardCharsets;

/** Durable overlapping territorial budgets. Owners remain independent of this grouping. */
public final class ProductionFarmGroups {
    public record Link(UUID left, UUID right) {}
    private final Map<UUID, UUID> groups;
    public ProductionFarmGroups(Collection<Link> links) {
        Map<UUID, UUID> parent = new HashMap<>();
        for (var link : links) {
            UUID left = root(parent, link.left()), right = root(parent, link.right());
            if (!left.equals(right)) {
                UUID first = left.toString().compareTo(right.toString()) < 0 ? left : right;
                parent.put(first.equals(left) ? right : left, first);
            }
        }
        Map<UUID, UUID> resolved = new HashMap<>();
        for (UUID id : List.copyOf(parent.keySet())) resolved.put(id, root(parent, id));
        groups = Map.copyOf(resolved);
    }
    private static UUID root(Map<UUID, UUID> parent, UUID id) {
        UUID next = parent.get(id);
        if (next == null) { parent.put(id,id); return id; }
        UUID cursor = id;
        while (!parent.get(cursor).equals(cursor)) cursor = parent.get(cursor);
        while (!id.equals(cursor)) { UUID previous = parent.put(id,cursor); id = previous; }
        return cursor;
    }
    public UUID canonical(UUID id) { return id == null ? null : groups.getOrDefault(id,id); }
    public static UUID physical(String authority, String key) {
        return UUID.nameUUIDFromBytes(("SmartSpawner-migration-v1:" + authority + ":" + key).getBytes(StandardCharsets.UTF_8));
    }
    public static LegacyProductionMigration.Claim claim(Map<String, Set<UUID>> roots) {
        if (roots.isEmpty()) return LegacyProductionMigration.Claim.none();
        UUID common = null;
        boolean safe = true;
        for (var owners : roots.values()) {
            if (owners.size() != 1) { safe = false; continue; }
            UUID owner = owners.iterator().next();
            if (common == null) common = owner;
            else if (!common.equals(owner)) safe = false;
        }
        return new LegacyProductionMigration.Claim(Collections.min(roots.keySet()), safe ? common : null,
                !safe, roots.keySet());
    }
    public static boolean canAddToStack(UUID placer, ProductionOwnershipRepository.Ownership target) {
        return placer != null && placer.equals(target.owner());
    }
    /** A fresh placed block belongs to its actual placer, not an item tag or territory owner. */
    public static ProductionOwnershipRepository.Ownership placedOwner(UUID placer) {
        return new ProductionOwnershipRepository.Ownership(Objects.requireNonNull(placer),null);
    }
}
