package github.nighter.smartspawner.production;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.query.QueryOptions;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Evaluate the responsible owner's effective permissions in the farm world, never an activator/OP. */
public final class ProductionPermissions implements AutoCloseable {
    private final LuckPerms permissions;
    private final java.util.Map<Integer, String> rankPermissions;
    private final java.util.concurrent.ConcurrentMap<UUID, net.luckperms.api.model.user.User> acquired = new java.util.concurrent.ConcurrentHashMap<>();
    public ProductionPermissions(LuckPerms permissions, java.util.Map<Integer, String> rankPermissions) {
        this.permissions = permissions;
        this.rankPermissions = java.util.Map.copyOf(rankPermissions);
    }
    public AutoCloseable listen(Object plugin, Runnable invalidation) {
        return permissions.getEventBus().subscribe(plugin, net.luckperms.api.event.node.NodeMutateEvent.class, event -> invalidation.run());
    }
    public void retain(java.util.Set<UUID> needed) {
        acquired.forEach((id, user) -> {
            if (!needed.contains(id) && acquired.remove(id, user)) permissions.getUserManager().cleanupUser(user);
        });
    }
    @Override public void close() {
        acquired.forEach((id, user) -> permissions.getUserManager().cleanupUser(user));
        acquired.clear();
    }
    public UUID resolveName(String name) throws Exception {
        if (name == null || name.isBlank()) return null;
        try { return UUID.fromString(name); }
        catch (IllegalArgumentException notUuid) {
            if (name.length()>64 || name.chars().anyMatch(Character::isISOControl)) return null;
            try { return permissions.getUserManager().lookupUniqueId(name).get(10, TimeUnit.SECONDS); }
            catch (IllegalArgumentException invalidName) { return null; }
        }
    }

    public Integer quota(UUID owner, String world) {
        try {
            var user = permissions.getUserManager().getUser(owner);
            if (user == null) {
                user = permissions.getUserManager().loadUser(owner).get(10, TimeUnit.SECONDS);
                acquired.put(owner, user);
            }
            {
                // Farm permission contexts are server/world. Unknown dynamic contexts cannot
                // be reconstructed safely for offline owners, so never grant through them.
                for (var node : user.resolveInheritedNodes(QueryOptions.nonContextual())) {
                    String key = node.getKey();
                    if (key.startsWith("group.") || key.startsWith("smartspawner.") || key.equals("*")) {
                        for (var context : node.getContexts().toSet()) {
                            if (!context.getKey().equals("world") && !context.getKey().equals("server")) return null;
                        }
                    }
                }
                var contexts = ImmutableContextSet.builder()
                        .addAll(permissions.getContextManager().getStaticContext())
                        .add("world", world).build();
                var options = QueryOptions.contextual(contexts, permissions.getContextManager().getStaticQueryOptions().flags());
                var data = user.getCachedData().getPermissionData(options);
                var actualNodes = user.resolveInheritedNodes(options);
                java.util.function.Predicate<net.luckperms.api.node.Node> actual = cause ->
                        cause != null && actualNodes.stream().anyMatch(node ->
                                node.getKey().equals(cause.getKey()) && node.getValue() == cause.getValue());
                return ProductionTierPolicy.resolve(tier -> {
                    var result = data.queryPermission("smartspawner.production.capacity." + tier);
                    boolean deny = actual.test(result.node()) && result.result() == net.luckperms.api.util.Tristate.FALSE;
                    boolean granted = actual.test(result.node()) && result.result().asBoolean();
                    return new ProductionTierPolicy.Decision(granted, deny);
                }, tier -> {
                    var result = data.queryPermission(rankPermissions.get(tier));
                    // Ignore platform/default operator answers without an actual permission node.
                    return actual.test(result.node()) && result.result().asBoolean();
                });
            }
        } catch (Exception unavailable) {
            return null;
        }
    }
}
