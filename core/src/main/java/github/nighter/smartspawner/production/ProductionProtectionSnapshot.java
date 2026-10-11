package github.nighter.smartspawner.production;

import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.Scheduler;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;

/** Bounded global-thread metadata snapshots. No block/chunk loads, SQL or UUID network lookup. */
public final class ProductionProtectionSnapshot implements AutoCloseable {
    private final SmartSpawner plugin;
    private final boolean worldGuard;
    private final boolean royaleEnabled;
    private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public ProductionProtectionSnapshot(SmartSpawner plugin) {
        this.plugin = plugin;
        worldGuard = plugin.getIntegrationManager().isHasWorldGuard();
        royaleEnabled = plugin.getServer().getPluginManager().isPluginEnabled("RoyaleProtectionBlocks");
    }

    public CompletableFuture<Map<String, LegacyProductionMigration.Claim>> snapshot(List<LegacyProductionMigration.Row> input) {
        var result = new CompletableFuture<Map<String, LegacyProductionMigration.Claim>>();
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Closed"));
        pending.add(result);
        result.whenComplete((ignored, failure) -> pending.remove(result));
        List<LegacyProductionMigration.Row> rows = List.copyOf(input);
        Map<String, LegacyProductionMigration.Claim> claims = new HashMap<>();
        class Batch implements Runnable {
            int cursor;
            @Override public void run() {
                if (closed || result.isDone()) { result.completeExceptionally(new IllegalStateException("Closed")); return; }
                try {
                    long started = System.nanoTime();
                    int count = 0;
                    do {
                        if (cursor >= rows.size()) { result.complete(Map.copyOf(claims)); return; }
                        var row = rows.get(cursor++);
                        claims.put(row.id(), claim(row));
                        count++;
                    } while (count < 32 && System.nanoTime() - started < 2_000_000L);
                    Scheduler.runTaskLater(this, 1);
                } catch (Exception failure) { result.completeExceptionally(failure); }
            }
        }
        Scheduler.runTask(new Batch());
        return result;
    }

    private LegacyProductionMigration.Claim claim(LegacyProductionMigration.Row row) throws Exception {
        if (royaleEnabled) return royale(row); // API failure is not an unprotected claim.
        if (!worldGuard) return LegacyProductionMigration.Claim.none();
        var world = plugin.getServer().getWorld(row.world());
        if (world == null) throw new IllegalStateException("Protection world unavailable");
        var manager = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(world));
        if (manager == null) throw new IllegalStateException("Protection manager unavailable");
        Map<String, Set<UUID>> roots = new TreeMap<>();
        for (var region : manager.getApplicableRegions(BlockVector3.at(row.x(), row.y(), row.z()))) {
            if (region.getId().equals("__global__")) continue;
            var root = region;
            Set<String> visited = new HashSet<>();
            while (root.getParent() != null && visited.add(root.getId())) root = root.getParent();
            roots.put("wg:" + row.world() + ":" + root.getId(), Set.copyOf(root.getOwners().getUniqueIds()));
        }
        return reduce(roots);
    }

    private LegacyProductionMigration.Claim royale(LegacyProductionMigration.Row row) throws Exception {
        // Public optional RPB API. Reflection avoids shading/linking its private distribution.
        var installed = plugin.getServer().getPluginManager().getPlugin("RoyaleProtectionBlocks");
        if (installed == null || !installed.isEnabled()) throw new IllegalStateException("Protection plugin unavailable");
        ClassLoader loader = installed.getClass().getClassLoader();
        var api = Class.forName("royale.RoyaleProtectionBlocks.Plugin.API.RoyaleProtectionBlocksAPI", true, loader);
        var locationClass = Class.forName("royale.RoyaleProtectionBlocks.Plugin.API.Objects.SimpleLocation", true, loader);
        var protectionClass = Class.forName("royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtection", true, loader);
        var serviceClass = Class.forName("royale.RoyaleProtectionBlocks.Plugin.API.Services.Protections.ProtectionsService", true, loader);
        Object instance = api.getMethod("getInstance").invoke(null);
        if (instance == null) throw new IllegalStateException("Protection API not ready");
        Object service = api.getMethod("getProtectionsService").invoke(instance);
        Object point = locationClass.getConstructor(String.class, double.class, double.class, double.class)
                .newInstance(row.world(), (double) row.x(), (double) row.y(), (double) row.z());
        Object query = serviceClass.getMethod("query", locationClass, boolean.class).invoke(service, point, true);
        if (query == null) throw new IllegalStateException("Protection query unavailable");
        var queryClass = Class.forName("royale.RoyaleProtectionBlocks.Plugin.API.Services.Protections.Objects.CachedQuery", true, loader);
        var candidates = (List<?>) queryClass.getMethod("getProtections").invoke(query);
        Map<String, Set<UUID>> roots = new TreeMap<>();
        for (Object candidate : List.copyOf(candidates)) {
            // Cached query includes nearby protections: exact containment MUST be checked.
            if (!(Boolean) protectionClass.getMethod("isInside", locationClass, boolean.class).invoke(candidate, point, true)) continue;
            Object root = protectionClass.getMethod("getParentProtection").invoke(candidate);
            if (root == null) root = candidate;
            String id = (String) protectionClass.getMethod("getProtectionId").invoke(root);
            UUID owner = (UUID) protectionClass.getMethod("getOwnerUuid").invoke(root);
            roots.put("rpb:" + row.world() + ":" + id, owner == null ? Set.of() : Set.of(owner));
        }
        return reduce(roots);
    }

    static LegacyProductionMigration.Claim reduce(Map<String, Set<UUID>> roots) {
        if (roots.isEmpty()) return LegacyProductionMigration.Claim.none();
        if (roots.size() != 1) return new LegacyProductionMigration.Claim(null, null, true);
        var entry = roots.entrySet().iterator().next();
        return new LegacyProductionMigration.Claim(entry.getKey(),
                entry.getValue().size() == 1 ? entry.getValue().iterator().next() : null,
                entry.getValue().size() > 1);
    }

    @Override public void close() {
        closed = true;
        pending.forEach(future -> future.completeExceptionally(new IllegalStateException("Closed")));
        pending.clear();
    }
}
