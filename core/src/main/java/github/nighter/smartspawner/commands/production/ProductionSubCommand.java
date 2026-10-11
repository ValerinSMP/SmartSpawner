package github.nighter.smartspawner.commands.production;

import com.mojang.brigadier.context.CommandContext;
import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.commands.BaseSubCommand;
import github.nighter.smartspawner.production.ProductionObserver;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Map;

public final class ProductionSubCommand extends BaseSubCommand {
    public ProductionSubCommand(SmartSpawner plugin) { super(plugin); }
    @Override public String getName() { return "production"; }
    @Override public String getPermission() { return "smartspawner.command.production"; }
    @Override public String getDescription() { return "View persisted stock observation"; }

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
