package github.nighter.smartspawner.production;

import java.util.*;

/**
 * Offline, immutable-snapshot planner. NOT wired to generation, SQL, permissions or inventories.
 * Input must include all durable placements, including unloaded chunks and worlds.
 * O(n^3) spatial evaluation is deliberately bounded; it must never run on a server tick.
 */
public final class ProductionSimulation {
    private ProductionSimulation() {}

    public enum Ownership { VERIFIED, LEGACY_UNRESOLVED }
    public enum PermissionStatus { RESOLVED, UNKNOWN }
    public record Quota(PermissionStatus status, long perType) {
        public Quota {
            Objects.requireNonNull(status);
            if (perType < 0) throw new IllegalArgumentException("Negative quota");
        }
    }
    public record Placement(String id, String world, int x, int y, int z, String baseType,
                            UUID responsible, Ownership ownership, long stock,
                            long selectionPriority, long durableOrder) {
        public Placement {
            Objects.requireNonNull(id); Objects.requireNonNull(world);
            Objects.requireNonNull(baseType); Objects.requireNonNull(ownership);
            if (id.isBlank() || world.isBlank() || baseType.isBlank() || stock <= 0
                    || durableOrder < 0 || (ownership == Ownership.VERIFIED && responsible == null)) {
                throw new IllegalArgumentException("Invalid placement");
            }
        }
    }
    /** One side has span+1 integer block positions; all heights count together. */
    public record SpatialProposal(int span, long cap) {
        public SpatialProposal {
            if (span < 0 || cap < 0) throw new IllegalArgumentException("Invalid spatial proposal");
        }
    }
    public record Allocation(String id, long stock, long wouldProduce, long wouldHold, String reason) {}
    public record Report(List<Allocation> allocations, long totalStock, long wouldProduce, long wouldHold) {
        public Report { allocations = List.copyOf(allocations); }
    }
    private record OwnerType(UUID owner, String type) {}
    private record Window(String world, int x, int z) {}

    /**
     * Highest EFFECTIVE allowed permission, already resolved in the exact Survival context.
     * Callers must not feed raw groups/wildcard strings, OP or primary_group into this method.
     * These are selected policy values, not shipped runtime configuration.
     */
    public static long tierQuota(Set<Integer> effectiveAllowedTiers) {
        long limit = 50;
        for (int tier : effectiveAllowedTiers) {
            if (tier != 50 && tier != 60 && tier != 70 && tier != 80 && tier != 90 && tier != 100) {
                throw new IllegalArgumentException("Unsupported effective tier");
            }
            limit = Math.max(limit, tier);
        }
        return limit;
    }

    public static Report plan(Collection<Placement> snapshot, Map<UUID, Quota> quotas,
                              Optional<SpatialProposal> spatial) {
        List<Placement> rows = new ArrayList<>(snapshot);
        Set<String> ids = new HashSet<>();
        for (Placement p : rows) if (!ids.add(p.id())) throw new IllegalArgumentException("Duplicate placement");
        rows.sort(Comparator.comparingLong(Placement::selectionPriority).reversed()
                .thenComparingLong(Placement::durableOrder).thenComparing(Placement::id));
        // Exact reference solver only. Runtime implementation must use a spatial index.
        if (spatial.isPresent() && rows.size() > 256) {
            throw new IllegalArgumentException("Spatial reference solver is limited to 256 placements");
        }
        Set<Window> windows = new HashSet<>();
        if (spatial.isPresent()) {
            for (Placement a : rows) for (Placement b : rows) {
                if (a.world().equals(b.world())) windows.add(new Window(a.world(), a.x(), b.z()));
            }
        }
        Map<Window, Long> windowUsed = new HashMap<>();
        Map<OwnerType, Long> used = new HashMap<>();
        List<Allocation> result = new ArrayList<>();
        long total = 0, producing = 0;
        for (Placement p : rows) {
            total = Math.addExact(total, p.stock());
            Quota q = p.responsible() == null ? null : quotas.get(p.responsible());
            long n = 0;
            String reason = "OWNERSHIP_UNRESOLVED";
            if (p.ownership() == Ownership.VERIFIED) {
                reason = "PERMISSION_CONTEXT_UNKNOWN";
                if (q != null && q.status() == PermissionStatus.RESOLVED) {
                    OwnerType key = new OwnerType(p.responsible(), p.baseType());
                    n = Math.min(p.stock(), Math.max(0, q.perType() - used.getOrDefault(key, 0L)));
                    reason = n == p.stock() ? "WITHIN_OWNER_QUOTA" : "OWNER_TYPE_QUOTA";
                    if (spatial.isPresent()) {
                        SpatialProposal s = spatial.get();
                        long before = n;
                        for (Window w : windows) if (contains(w, p, s.span())) {
                            n = Math.min(n, Math.max(0, s.cap() - windowUsed.getOrDefault(w, 0L)));
                        }
                        if (n < before) reason = "SPATIAL_PROPOSAL";
                        for (Window w : windows) if (contains(w, p, s.span())) {
                            windowUsed.put(w, Math.addExact(windowUsed.getOrDefault(w, 0L), n));
                        }
                    }
                    used.put(key, Math.addExact(used.getOrDefault(key, 0L), n));
                }
            }
            producing = Math.addExact(producing, n);
            result.add(new Allocation(p.id(), p.stock(), n, p.stock() - n, reason));
        }
        return new Report(result, total, producing, total - producing);
    }

    private static boolean contains(Window w, Placement p, int span) {
        return w.world().equals(p.world()) && p.x() >= w.x() && p.z() >= w.z()
                && (long) p.x() - w.x() <= span && (long) p.z() - w.z() <= span;
    }
}
