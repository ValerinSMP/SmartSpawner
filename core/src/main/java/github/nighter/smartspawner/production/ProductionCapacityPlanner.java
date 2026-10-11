package github.nighter.smartspawner.production;

import java.util.*;

/** Pure deterministic allocator. Stock is never modified. */
public final class ProductionCapacityPlanner {
    public record Stock(String id, String world, int x, int z, String type,
                        UUID owner, UUID farm, int quantity) {}
    public record Context(UUID owner, String world) {}
    public record Grant(Stock stock, int productive, String reason) {
        public Grant {
            if (productive < 0 || productive > Math.max(0, stock.quantity()))
                throw new IllegalArgumentException("Production allocation outside conserved stock");
        }
    }
    private ProductionCapacityPlanner() {}

    public static Map<String, Grant> allocate(Collection<Stock> stock,
            Map<Context, Integer> quotas, int farmCap, int span, int spatialCap) {
        if (farmCap < 1 || span < 1 || span > 65 || spatialCap < 1)
            throw new IllegalArgumentException("Invalid production policy");
        List<Stock> ordered = stock.stream().sorted(Comparator.comparing(Stock::id)).toList();
        Map<String, Grant> result = new LinkedHashMap<>();
        Map<OwnerType, Integer> used = new HashMap<>();
        Map<UUID, Integer> farms = new HashMap<>();
        Map<String, NavigableMap<Long, NavigableMap<Long, Integer>>> cells = new HashMap<>();
        long[][] prefix = new long[span * 2][span * 2];
        Map<OwnerType, Integer> strictQuotas = new HashMap<>();
        for (Stock entry : ordered) {
            if (entry.owner() != null && entry.farm() != null) {
                Integer quota = quotas.get(new Context(entry.owner(), entry.world()));
                if (quota != null) strictQuotas.merge(new OwnerType(entry.owner(), entry.type()), quota, Math::min);
            }
        }
        Set<String> ids = new HashSet<>();
        for (Stock entry : ordered) {
            if (!ids.add(entry.id())) throw new IllegalArgumentException("Duplicate spawner id");
            int quantity = Math.max(0, entry.quantity());
            if (entry.owner() == null || entry.farm() == null) {
                result.put(entry.id(), new Grant(entry, 0, "OWNERSHIP_OR_FARM_UNRESOLVED"));
                continue;
            }
            Integer quota = quotas.get(new Context(entry.owner(), entry.world()));
            if (quota == null) {
                result.put(entry.id(), new Grant(entry, 0, "PERMISSION_CONTEXT_UNAVAILABLE"));
                continue;
            }
            OwnerType key = new OwnerType(entry.owner(), entry.type());
            quota = strictQuotas.get(key);
            int allowed = Math.min(quantity, Math.min(Math.max(0, quota - used.getOrDefault(key, 0)),
                    Math.max(0, farmCap - farms.getOrDefault(entry.farm(), 0))));
            NavigableMap<Long, NavigableMap<Long, Integer>> worldCells = cells.computeIfAbsent(entry.world(), k -> new TreeMap<>());
            if (allowed > 0) allowed = Math.min(allowed, spatialFree(worldCells, entry.x(), entry.z(), span, spatialCap, prefix));
            if (allowed > 0) {
                used.merge(key, allowed, Integer::sum);
                farms.merge(entry.farm(), allowed, Integer::sum);
                worldCells.computeIfAbsent((long) entry.x(), k -> new TreeMap<>()).merge((long) entry.z(), allowed, Integer::sum);
            }
            result.put(entry.id(), new Grant(entry, allowed, allowed == quantity ? "PRODUCTIVE" : "STOCK_HELD"));
        }
        return Map.copyOf(result);
    }

    // A window containing the candidate starts at any integer X/Z in these span ranges.
    // A local 2D prefix sum tests ALL such windows, including borders/corners and neighbours.
    // Long coordinates prevent int overflow. Height/type/activators are deliberately absent.
    private static int spatialFree(NavigableMap<Long, NavigableMap<Long, Integer>> cells, int x, int z, int span, int cap, long[][] prefix) {
        int size = span * 2 - 1;
        long left = (long) x - span + 1, bottom = (long) z - span + 1;
        for (long[] row : prefix) Arrays.fill(row, 0L);
        for (var column : cells.subMap(left, true, left + size - 1, true).entrySet()) {
            for (var row : column.getValue().subMap(bottom, true, bottom + size - 1, true).entrySet()) {
                prefix[(int) (column.getKey() - left) + 1][(int) (row.getKey() - bottom) + 1] = row.getValue();
            }
        }
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                prefix[i + 1][j + 1] += prefix[i][j + 1] + prefix[i + 1][j] - prefix[i][j];
            }
        }
        int free = cap;
        for (int i = 0; i < span; i++) {
            for (int j = 0; j < span; j++) {
                long occupied = prefix[i + span][j + span] - prefix[i][j + span]
                        - prefix[i + span][j] + prefix[i][j];
                // Keep the long comparison explicit. GraalVM 25.0.3 miscompiles
                // the nested min/max plus narrowing conversion after this loop warms up.
                // Reproduced with the deployed JAR; interpreter and HotSpot C2 agree.
                long remaining = (long) cap - occupied;
                if (remaining <= 0L) return 0;
                if (remaining < free) free = (int) remaining;
            }
        }
        return free;
    }

    private record OwnerType(UUID owner, String type) {}
}
