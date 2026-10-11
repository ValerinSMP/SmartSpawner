package github.nighter.smartspawner.production;

import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.spawner.properties.SpawnerData;
import net.luckperms.api.LuckPermsProvider;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Logger;

/** Server-local durable ownership and stock accounting. SQL/permission resolution stay on one worker. */
public final class ProductionCapacityService implements AutoCloseable {
    private final SmartSpawner plugin;
    private final ProductionObserver.Connections connections;
    private final ProductionOwnershipRepository repository;
    private final ProductionPermissions permissions;
    private final ProductionProtectionSnapshot protectionSnapshot;
    private final boolean legacyMigration;
    private volatile int legacyAssigned, legacyUnresolved;
    private final ScheduledExecutorService worker;
    private final Logger logger;
    private final AutoCloseable permissionSubscription;
    private final boolean enforcing;
    private final int farmCap, span, spatialCap;
    private final ConcurrentMap<String, ProductionCapacityPlanner.Stock> overlay = new ConcurrentHashMap<>();
    private final ConcurrentMap<String,Integer> heights = new ConcurrentHashMap<>();
    private final Set<String> removed = ConcurrentHashMap.newKeySet();
    private final Set<String> inFlightAssignments = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingAssignments = ConcurrentHashMap.newKeySet();
    private final AtomicLong changes = new AtomicLong();
    private final ProductionAllocationFence allocationFence = new ProductionAllocationFence();
    private final AtomicLong allocationRevision = new AtomicLong();
    private final AtomicBoolean queued = new AtomicBoolean();
    private volatile Map<String, ProductionOwnershipRepository.Ownership> owners = Map.of();
    private volatile ProductionFarmGroups farmGroups = new ProductionFarmGroups(List.of());
    private Map<String, ProductionCapacityPlanner.Stock> persisted = Map.of();
    private volatile Map<String, ProductionCapacityPlanner.Grant> grants = Map.of();
    private volatile Map<ProductionCapacityPlanner.Context, Integer> resolvedQuotas = Map.of();
    private volatile boolean ready, closed, ownershipLoaded;
    private volatile String state = "STARTING";
    private volatile long planningMillis;

    public ProductionCapacityService(SmartSpawner plugin, boolean enforcing, int farmCap, int span, int spatialCap) {
        if (farmCap < 1 || span < 1 || span > 65 || spatialCap < 1) throw new IllegalArgumentException("Invalid production policy");
        this.plugin = plugin;
        this.connections = plugin.getDatabaseManager()::getConnection;
        this.repository = new ProductionOwnershipRepository(plugin.getDatabaseManager().getTableSpawners());
        this.enforcing = enforcing;
        this.legacyMigration = plugin.getConfig().getBoolean("production.legacy-migration.enabled", false);
        this.protectionSnapshot = new ProductionProtectionSnapshot(plugin);
        this.farmCap = farmCap;
        this.span = span;
        this.spatialCap = spatialCap;
        this.logger = plugin.getLogger();
        Map<Integer, String> ranks = new HashMap<>();
        String[] labels = {"guardian", "ascendido", "arcano", "elite", "leyenda"};
        for (int i = 0; i < labels.length; i++) {
            int tier = 60 + i * 10;
            ranks.put(tier, plugin.getConfig().getString("production.rank-permissions." + tier, "group." + labels[i]));
        }
        this.permissions = plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")
                ? new ProductionPermissions(LuckPermsProvider.get(), ranks) : null;
        worker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(() -> {
                try { r.run(); }
                finally { if (permissions != null) permissions.close(); }
            }, "SmartSpawner-production-capacity");
            t.setDaemon(true);
            return t;
        });
        permissionSubscription = permissions == null ? null : permissions.listen(plugin, () -> {
            ready = false;
            changes.incrementAndGet();
            requestPlan();
        });
        worker.scheduleWithFixedDelay(this::refreshPersisted, 0, 60, TimeUnit.SECONDS);
    }

    private void refreshPersisted() {
        if (closed) return;
        try (Connection connection = connections.open()) {
            repository.initialize(connection);
            owners = repository.owners(connection);
            farmGroups = new ProductionFarmGroups(repository.farmLinks(connection));
            persisted = repository.stock(connection, owners);
            repository.heights(connection).forEach(heights::putIfAbsent);
            if (legacyMigration && !plugin.getDatabaseManager().isCrossServer()) {
                migrateLegacy(connection);
                owners = repository.owners(connection);
            farmGroups = new ProductionFarmGroups(repository.farmLinks(connection));
            }
            ownershipLoaded = true;
            plan();
        } catch (Exception failure) {
            ready = false;
            state = "MIGRATING_LEGACY".equals(state) ? "MIGRATION_UNAVAILABLE" : "STORAGE_UNAVAILABLE";
            logger.warning("Production accounting unavailable; enforcement holds production, stock is preserved.");
        }
    }

    private void migrateLegacy(Connection connection) throws Exception {
        var rows = repository.legacyRows(connection).stream().filter(row -> {
            return !removed.contains(row.id()) && !pendingAssignments.contains(row.id());
        }).toList();
        if (rows.isEmpty()) { legacyUnresolved = 0; return; }
        // Keep existing allocations usable while bounded metadata batches inspect protection overlaps.
        var claims = protectionSnapshot.snapshot(rows).get(120, TimeUnit.SECONDS);
        Map<String, UUID> names = new HashMap<>();
        int assigned = 0, unresolved = 0;
        for (var row : rows) {
            if (closed) return;
            if (removed.contains(row.id()) || pendingAssignments.contains(row.id())) continue;
            var claim = claims.getOrDefault(row.id(), LegacyProductionMigration.Claim.none());
            var prior = owners.get(row.id());
            reconcileFarm(connection, claim, prior);
            if (prior != null && prior.owner() != null && prior.farm() != null) continue;
            String name = row.lastPlayer() == null ? "" : row.lastPlayer().toLowerCase(Locale.ROOT);
            if (!names.containsKey(name)) names.put(name, permissions == null ? null : permissions.resolveName(name));
            var decision = LegacyProductionMigration.decide(repositoryAuthority(), row, names.get(name),
                    claims.getOrDefault(row.id(), LegacyProductionMigration.Claim.none()), owners.get(row.id()));
            if (decision == null) { unresolved++; continue; }
            if (repository.assignMigrated(connection, row.id(), decision)) assigned++;
        }
        farmGroups = new ProductionFarmGroups(repository.farmLinks(connection));
        legacyAssigned += assigned;
        if (assigned > 0 || legacyUnresolved != unresolved) logger.info("Production migration: assigned "
                + assigned + " blocks; unresolved " + unresolved + ". Retrospective assignment, unverified provenance.");
        legacyUnresolved = unresolved;
    }

    private void reconcileFarm(Connection connection, LegacyProductionMigration.Claim claim,
            ProductionOwnershipRepository.Ownership prior) throws Exception {
        if (claim.farmKeys().isEmpty()) return;
        UUID first = ProductionFarmGroups.physical(repositoryAuthority(), claim.farmKey());
        List<ProductionFarmGroups.Link> links = new ArrayList<>();
        for (String key : claim.farmKeys()) links.add(new ProductionFarmGroups.Link(first,
                ProductionFarmGroups.physical(repositoryAuthority(),key)));
        if (prior != null && prior.farm() != null) links.add(new ProductionFarmGroups.Link(first,prior.farm()));
        links.removeIf(link -> farmGroups.canonical(link.left()).equals(farmGroups.canonical(link.right())));
        if (!links.isEmpty()) {
            repository.linkFarms(connection,links);
            farmGroups = new ProductionFarmGroups(repository.farmLinks(connection));
        }
    }

    private String repositoryAuthority() { return plugin.getDatabaseManager().getTableSpawners(); }

    private void plan() {
        if (closed) return;
        ready = false;
        state = "PLANNING";
        if (plugin.getDatabaseManager().isCrossServer()) {
            ready = false;
            state = "CROSS_SERVER_AUTHORITY_REQUIRED";
            return;
        }
        if (permissions == null) {
            ready = false;
            state = "LUCKPERMS_REQUIRED";
            return;
        }
        long version = changes.get(), started = System.nanoTime();
        Map<String, ProductionCapacityPlanner.Stock> stock = new HashMap<>(persisted);
        stock.putAll(overlay);
        removed.forEach(stock::remove);
        stock.replaceAll((id, value) -> {
            var ownership = owners.get(id);
            return new ProductionCapacityPlanner.Stock(id, value.world(), value.x(), value.z(), value.type(),
                    ownership == null ? null : ownership.owner(), ownership == null ? null : farmGroups.canonical(ownership.farm()), value.quantity());
        });
        Set<UUID> neededOwners = new HashSet<>();
        stock.values().forEach(value -> {
            if (value.owner() != null && value.farm() != null && value.quantity() > 0) neededOwners.add(value.owner());
        });
        permissions.retain(neededOwners);
        Map<ProductionCapacityPlanner.Context, Integer> quotas = new HashMap<>();
        Set<ProductionCapacityPlanner.Context> resolved = new HashSet<>();
        for (var value : stock.values()) {
            if (value.owner() == null || value.farm() == null || value.quantity() <= 0) continue;
            var context = new ProductionCapacityPlanner.Context(value.owner(), value.world());
            if (resolved.add(context)) {
                Integer quota = permissions.quota(context.owner(), context.world());
                if (quota != null) quotas.put(context, quota);
            }
        }
        var next = ProductionCapacityPlanner.allocate(stock.values(), quotas, farmCap, span, spatialCap);
        if (closed) return;
        if (changes.get() != version) {
            requestPlan();
            return;
        }
        allocationFence.publish(() -> {
            if (closed || changes.get() != version) return;
            planningMillis = (System.nanoTime() - started) / 1_000_000;
            grants = next;
            resolvedQuotas = Map.copyOf(quotas);
            allocationRevision.incrementAndGet();
            ready = true;
            state = "READY";
        });
        if (!ready && !closed) requestPlan();
    }

    private void requestPlan() {
        if (closed || !queued.compareAndSet(false, true)) return;
        worker.schedule(() -> {
            queued.set(false);
            try { plan(); }
            catch (Exception failure) { ready = false; state = "PLANNING_UNAVAILABLE"; }
        }, 500, TimeUnit.MILLISECONDS);
    }

    /** Caller is the block's region thread; only immutable primitives leave it. */
    public void touch(SpawnerData spawner) {
        if (closed || spawner == null || spawner.getSpawnerLocation().getWorld() == null) return;
        var location = spawner.getSpawnerLocation();
        var value = new ProductionCapacityPlanner.Stock(spawner.getSpawnerId(), location.getWorld().getName(),
                location.getBlockX(), location.getBlockZ(), type(spawner), null, null, spawner.getStackSize());
        heights.put(value.id(), location.getBlockY());
        removed.remove(value.id());
        var previous = overlay.put(value.id(), value);
        if (!value.equals(previous)) {
            changes.incrementAndGet();
            requestPlan();
        }
    }

    public void remove(String id) {
        overlay.remove(id);
        heights.remove(id);
        removed.add(id); // A stale persisted row cannot reappear before its normal deletion flush.
        changes.incrementAndGet();
        requestPlan();
    }

    public ProductionOwnershipRepository.Ownership ownership(String id) {
        return owners.getOrDefault(id, new ProductionOwnershipRepository.Ownership(null, null));
    }

    /** Fresh placement uses the online placer's UUID, even when the item carries an old owner. */
    public void placed(SpawnerData spawner, ProductionOwnershipRepository.Ownership declaration, UUID placer) {
        touch(spawner);
        String id = spawner.getSpawnerId();
        var location = spawner.getSpawnerLocation();
        var row = new LegacyProductionMigration.Row(id, location.getWorld().getName(), location.getBlockX(),
                location.getBlockY(), location.getBlockZ(), spawner.getLastInteractedPlayer());
        pendingAssignments.add(id);
        changes.incrementAndGet();
        // Persist the known owner before protection lookup: an API failure must not lose the placer.
        assign(id, ProductionFarmGroups.placedOwner(placer), "NEW_PLACER_PENDING_FARM", placer).whenComplete((ignored, ownerFailure) -> {
            if (closed || ownerFailure != null) return;
            protectionSnapshot.snapshot(List.of(row)).whenComplete((claims, failure) -> {
                if (closed) return;
                worker.execute(() -> {
                    if (closed || removed.contains(id)) return;
                    try (Connection connection = connections.open()) {
                        if (failure != null) throw new IllegalStateException("Protection snapshot unavailable",failure);
                        var prior = owners.get(id);
                        if (prior == null || prior.farm() != null) return; // A later confirmed transfer wins.
                        var claim = claims.get(id);
                        reconcileFarm(connection,claim,prior);
                        var decision = LegacyProductionMigration.decide(repositoryAuthority(),row,null,claim,prior);
                        if (decision != null) assign(id,decision.ownership(),"NEW_PLACER_DECLARATION",placer);
                    } catch (Exception unavailable) {
                        // Owner-only is durable. Periodic migration retries the physical budget, without holding other farms.
                        logger.warning("Placement farm unavailable; owner retained for automatic retry.");
                    }
                });
            });
        });
    }

    public CompletableFuture<Void> assign(String id, ProductionOwnershipRepository.Ownership ownership, String kind) {
        return assign(id, ownership, kind, null);
    }

    public CompletableFuture<Void> assign(String id, ProductionOwnershipRepository.Ownership ownership, String kind, UUID actor) {
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Closed"));
        if (id == null || !id.matches("[A-Za-z0-9_-]{1,64}")) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid spawner id"));
        if (!inFlightAssignments.add(id)) return CompletableFuture.failedFuture(new IllegalStateException("Assignment already pending"));
        pendingAssignments.add(id);
        changes.incrementAndGet();
        CompletableFuture<Void> result = new CompletableFuture<>();
        worker.execute(() -> {
            try (Connection connection = connections.open()) {
                repository.initialize(connection);
                if (removed.contains(id) || (!overlay.containsKey(id) && !persisted.containsKey(id))) throw new IllegalArgumentException("Unknown spawner");
                repository.assign(connection, id, ownership, kind, actor);
                var next = new HashMap<>(owners);
                next.put(id, ownership);
                owners = Map.copyOf(next);
                pendingAssignments.remove(id);
                changes.incrementAndGet();
                plan();
                result.complete(null);
                logger.info("Production ownership assignment committed: " + id + " (" + kind + "). Provenance remains unverified.");
            } catch (Exception failure) {
                // Keep the fence until a deliberate retry/reconciliation. Never silently re-enable an ambiguous transfer.
                pendingAssignments.add(id);
                ready = false;
                state = "ASSIGNMENT_UNCONFIRMED";
                result.completeExceptionally(failure);
                logger.warning("Production assignment failed; spawner is fenced until assignment is retried.");
            } finally {
                inFlightAssignments.remove(id);
            }
        });
        return result;
    }

    public int productive(SpawnerData spawner) {
        if (removed.contains(spawner.getSpawnerId())) return 0;
        if (!enforcing) return spawner.getStackSize();
        if (!ready || pendingAssignments.contains(spawner.getSpawnerId())) return 0;
        var grant = grants.get(spawner.getSpawnerId());
        var ownership = ownership(spawner.getSpawnerId());
        var location = spawner.getSpawnerLocation();
        if (grant == null || grant.stock().x() != location.getBlockX() || grant.stock().z() != location.getBlockZ()
                || !grant.stock().world().equals(location.getWorld().getName()) || !grant.stock().type().equals(type(spawner))
                || !Objects.equals(grant.stock().owner(), ownership.owner())
                || !Objects.equals(grant.stock().farm(), farmGroups.canonical(ownership.farm()))) return 0;
        return Math.min(spawner.getStackSize(), grant.productive());
    }

    public static String type(SpawnerData spawner) {
        return spawner.isItemSpawner() ? "ITEM:" + spawner.getSpawnedItemMaterial().name()
                : "MOB:" + spawner.getEntityType().name();
    }

    public UUID farm(String label) {
        if (label.isBlank() || label.length() > 64) throw new IllegalArgumentException("Invalid farm label");
        // Explicit administrative label, one non-additive budget across owners, worlds and heights.
        return UUID.nameUUIDFromBytes(("SmartSpawner-farm-v1:" + plugin.getDatabaseManager().getTableSpawners()
                + ":" + label.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
    }

    public ProductionAllocationFence.Lease acquire(SpawnerData spawner, long expectedRevision) {
        return allocationFence.acquire(() -> revision() == expectedRevision && productive(spawner) > 0);
    }

    public boolean canMutate(String id) { return ownershipLoaded && !pendingAssignments.contains(id); }
    public long revision() { return enforcing ? allocationRevision.get() + changes.get() : 0; }
    public Integer quota(SpawnerData spawner) {
        if (!ready) return null;
        var owner = ownership(spawner.getSpawnerId()).owner();
        return owner == null ? null : resolvedQuotas.get(new ProductionCapacityPlanner.Context(owner,
                spawner.getSpawnerLocation().getWorld().getName()));
    }
    public CompletableFuture<OwnSpawnerListing.Page> ownListing(UUID viewer,int page) {
        return ownListing(viewer,page,OwnSpawnerListing.Filter.ALL,null,OwnSpawnerListing.Sort.TYPE);
    }
    public CompletableFuture<OwnSpawnerListing.Page> ownListing(UUID viewer,int page,OwnSpawnerListing.Filter filter,
            String type,OwnSpawnerListing.Sort sort) {
        if (closed || !ownershipLoaded) return CompletableFuture.failedFuture(new IllegalStateException("Accounting pending"));
        return CompletableFuture.supplyAsync(() -> {
            Map<String,ProductionCapacityPlanner.Stock> stock=new HashMap<>(persisted);
            stock.putAll(overlay);
            removed.forEach(stock::remove);
            var ownership=owners;
            var allocation=grants;
            boolean valid=ready;
            List<OwnSpawnerListing.Entry> entries=new ArrayList<>();
            for(var value:stock.values()) {
                var owner=ownership.get(value.id());
                if(owner==null || !viewer.equals(owner.owner())) continue;
                var grant=allocation.get(value.id());
                int productive=!enforcing?value.quantity():!valid || pendingAssignments.contains(value.id()) || grant==null
                        || !Objects.equals(owner.owner(),grant.stock().owner()) || !Objects.equals(farmGroups.canonical(owner.farm()),grant.stock().farm())
                        || !value.type().equals(grant.stock().type()) ? 0 : Math.min(value.quantity(),grant.productive());
                entries.add(new OwnSpawnerListing.Entry(value.id(),owner.owner(),value.world(),value.x(),
                        heights.getOrDefault(value.id(),0),value.z(),value.type(),value.quantity(),productive));
            }
            return OwnSpawnerListing.page(viewer,entries,page,filter,type,sort);
        },worker);
    }

    public int legacyAssigned() { return legacyAssigned; }
    public int legacyUnresolved() { return legacyUnresolved; }
    public boolean enforcing() { return enforcing; }
    public String state() { return state; }
    public long planningMillis() { return planningMillis; }
    public Map<String, ProductionCapacityPlanner.Grant> grants() { return grants; }
    @Override public void close() {
        closed = true;
        ready = false;
        if (permissionSubscription != null) {
            try { permissionSubscription.close(); }
            catch (Exception failure) { logger.warning("Production permission subscription cleanup failed."); }
        }
        protectionSnapshot.close();
        worker.shutdownNow();
    }
}
