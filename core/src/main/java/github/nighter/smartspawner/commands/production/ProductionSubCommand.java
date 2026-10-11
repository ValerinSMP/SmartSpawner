package github.nighter.smartspawner.commands.production;

import com.mojang.brigadier.context.CommandContext;
import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.commands.BaseSubCommand;
import github.nighter.smartspawner.production.ProductionObserver;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Map;
import java.util.UUID;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import github.nighter.smartspawner.Scheduler;
import github.nighter.smartspawner.production.ProductionOwnershipRepository;

public final class ProductionSubCommand extends BaseSubCommand {
    public ProductionSubCommand(SmartSpawner plugin) { super(plugin); }
    @Override public String getName() { return "production"; }
    @Override public String getPermission() { return "smartspawner.command.production"; }
    @Override public String getDescription() { return "View persisted stock observation"; }

    @Override public LiteralArgumentBuilder<CommandSourceStack> build() {
        var root = super.build();
        root.then(Commands.literal("assign")
                .requires(source -> source.getSender().hasPermission("smartspawner.production.admin") || source.getSender().isOp())
                .then(Commands.argument("owner", StringArgumentType.word())
                        .then(Commands.argument("farm", StringArgumentType.word()).executes(this::assign))));
        root.then(Commands.literal("assign-id")
                .requires(source -> source.getSender().hasPermission("smartspawner.production.admin") || source.getSender().isOp())
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("owner", StringArgumentType.word())
                                .then(Commands.argument("farm", StringArgumentType.word()).executes(this::assignId)))));
        return root;
    }

    private int assignId(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        UUID owner;
        try { owner = UUID.fromString(StringArgumentType.getString(context, "owner")); }
        catch (IllegalArgumentException invalid) {
            plugin.getMessageService().sendMessage(sender, "production.owner_required");
            return 0;
        }
        var service = plugin.getProductionCapacityService();
        UUID farm;
        try { farm = service.farm(StringArgumentType.getString(context, "farm")); }
        catch (IllegalArgumentException invalid) { return 0; }
        logCommandExecution(context);
        plugin.getMessageService().sendMessage(sender, "production.assignment_pending");
        submitAssignment(sender, StringArgumentType.getString(context, "id"), owner, farm, "ADMIN_ASSIGN_BY_ID");
        return 1;
    }

    private int assign(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            plugin.getMessageService().sendMessage(context.getSource().getSender(), "player_only");
            return 0;
        }
        var block = player.getTargetBlockExact(8);
        var spawner = block == null ? null : plugin.getSpawnerManager().getSpawnerByLocation(block.getLocation());
        if (spawner == null) {
            plugin.getMessageService().sendMessage(player, "production.target_required");
            return 0;
        }
        if (spawner.isSelling() || plugin.getSpawnerRemovalService().isRemovalPending(spawner)) {
            plugin.getMessageService().sendMessage(player, "action_in_progress");
            return 0;
        }
        UUID owner;
        String ownerArgument = StringArgumentType.getString(context, "owner");
        try {
            owner = UUID.fromString(ownerArgument);
        } catch (IllegalArgumentException notUuid) {
            var online = plugin.getServer().getPlayerExact(ownerArgument);
            if (online == null) {
                plugin.getMessageService().sendMessage(player, "production.owner_required");
                return 0;
            }
            owner = online.getUniqueId();
        }
        var service = plugin.getProductionCapacityService();
        String farmLabel = StringArgumentType.getString(context, "farm");
        UUID farm;
        try { farm = service.farm(farmLabel); }
        catch (IllegalArgumentException invalid) { return 0; }
        logCommandExecution(context);
        plugin.getMessageService().sendMessage(player, "production.assignment_pending");
        submitAssignment(player, spawner.getSpawnerId(), owner, farm, "ADMIN_ASSIGN_OR_TRANSFER");
        return 1;
    }

    private void submitAssignment(org.bukkit.command.CommandSender sender, String id, UUID owner, UUID farm, String kind) {
        var service = plugin.getProductionCapacityService();
        var loaded = plugin.getSpawnerManager().getSpawnerById(id);
        Runnable submit = () -> {
            if (loaded != null && (loaded.isSelling() || plugin.getSpawnerRemovalService().isRemovalPending(loaded)
                    || plugin.getSpawnerManager().getSpawnerById(id) != loaded)) {
                respond(sender, "action_in_progress");
                return;
            }
            service.touch(loaded);
            service.assign(id, new ProductionOwnershipRepository.Ownership(owner, farm), kind,
                    sender instanceof Player player ? player.getUniqueId() : null)
                    .whenComplete((ignored, failure) -> respond(sender,
                            failure == null ? "production.assignment_ok" : "production.assignment_failed"));
        };
        // Prepare against loaded blocks on their region, then fence mutations while SQL commits.
        // Unloaded rows need no world access or forced chunk load.
        if (loaded == null) submit.run();
        else Scheduler.runLocationTask(loaded.getSpawnerLocation(), submit);
    }

    private void respond(org.bukkit.command.CommandSender sender, String key) {
        Runnable response = () -> plugin.getMessageService().sendMessage(sender, key);
        if (sender instanceof Player player) Scheduler.runEntityTask(player, response);
        else Scheduler.runTask(response);
    }

    @Override public int execute(CommandContext<CommandSourceStack> context) {
        var sender = context.getSource().getSender();
        ProductionObserver observer = plugin.getProductionObserver();
        if (observer == null) {
            plugin.getMessageService().sendMessage(sender, "production.disabled");
            return 0;
        }
        var status = observer.status();
        if (status.failed() || status.snapshot() == null) {
            plugin.getMessageService().sendMessage(sender,
                    status.failed() ? "production.unavailable" : "production.pending");
            return 0;
        }
        var service = plugin.getProductionCapacityService();
        long productive = service.grants().values().stream().mapToLong(g -> g.productive()).sum();
        plugin.getMessageService().sendMessage(sender, "production.policy", Map.of(
                "mode", service.enforcing() ? "enforce" : "observation",
                "state", service.state(), "productive", Long.toString(productive),
                "duration", Long.toString(service.planningMillis())));
        plugin.getMessageService().sendMessage(sender, "production.migration", Map.of(
                "assigned", Integer.toString(service.legacyAssigned()),
                "unresolved", Integer.toString(service.legacyUnresolved())));
        if (sender instanceof Player player) {
            var block = player.getTargetBlockExact(8);
            var spawner = block == null ? null : plugin.getSpawnerManager().getSpawnerByLocation(block.getLocation());
            if (spawner != null) {
                var ownership = service.ownership(spawner.getSpawnerId());
                Integer quota = service.quota(spawner);
                plugin.getMessageService().sendMessage(sender, "production.detail", Map.of(
                        "id", spawner.getSpawnerId(),
                        "owner", ownership.owner() == null ? "pending" : ownership.owner().toString(),
                        "farm", ownership.farm() == null ? "pending" : ownership.farm().toString(),
                        "stock", Integer.toString(spawner.getStackSize()),
                        "quota", quota == null ? "pending" : quota.toString(),
                        "productive", Integer.toString(service.productive(spawner))));
            }
        }
        var snapshot = status.snapshot();
        plugin.getMessageService().sendMessage(sender, "production.summary", Map.of(
                "blocks", Long.toString(snapshot.blocks()), "units", Long.toString(snapshot.units()),
                "invalid", Long.toString(snapshot.invalidBlocks()),
                "age", Long.toString(Math.max(0, System.currentTimeMillis() - snapshot.observedAtMillis()) / 1000),
                "duration", Long.toString(snapshot.durationMillis())));
        snapshot.types().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                plugin.getMessageService().sendMessage(sender, "production.type", Map.of(
                        "type", entry.getKey(), "blocks", Long.toString(entry.getValue().blocks()),
                        "units", Long.toString(entry.getValue().units()))));
        return 1;
    }
}
