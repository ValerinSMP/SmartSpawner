package github.nighter.smartspawner.commands.production;

import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.Scheduler;
import github.nighter.smartspawner.commands.BaseSubCommand;
import github.nighter.smartspawner.production.OwnSpawnerListing;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import java.util.*;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.TextDecoration;

/** Player-only read-only listing. Never opens administrative actions or loads chunks. */
public final class MineSubCommand extends BaseSubCommand implements Listener {
    public MineSubCommand(SmartSpawner plugin) {
        super(plugin);
        plugin.getServer().getPluginManager().registerEvents(this,plugin);
    }
    @Override public String getName() { return "mine"; }
    @Override public String getPermission() { return "smartspawner.command.mine"; }
    @Override public String getDescription() { return "View spawners assigned to your production capacity"; }
    @Override public LiteralArgumentBuilder<CommandSourceStack> build() {
        return super.build().then(Commands.argument("page",IntegerArgumentType.integer(1,1000000))
                .executes(context -> { logCommandExecution(context); return executePage(context,IntegerArgumentType.getInteger(context,"page")); }));
    }
    @Override public int execute(CommandContext<CommandSourceStack> context) { return executePage(context,1); }
    private int executePage(CommandContext<CommandSourceStack> context,int page) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            plugin.getMessageService().sendMessage(context.getSource().getSender(),"player_only");
            return 0;
        }
        request(player,page);
        return 1;
    }
    private void request(Player player,int page) {
        UUID viewer=player.getUniqueId();
        if (!player.hasPermission(getPermission())) return;
        plugin.getProductionCapacityService().ownListing(viewer,page).whenComplete((result,failure) ->
                Scheduler.runEntityTask(player,() -> {
                    if (!player.isOnline() || !viewer.equals(player.getUniqueId()) || !player.hasPermission(getPermission())) return;
                    if (failure!=null) { plugin.getMessageService().sendMessage(player,"mine.pending"); return; }
                    open(player,result);
                }));
    }
    private void open(Player player,OwnSpawnerListing.Page page) {
        var holder=new Holder(player.getUniqueId(),page.number(),page.pages());
        Inventory inventory=Bukkit.createInventory(holder,54,plugin.getLanguageManager().commandGui().title("mine_title"));
        holder.inventory=inventory;
        for(int i=0;i<page.entries().size();i++) {
            var row=page.entries().get(i);
            String raw=row.type().substring(row.type().indexOf(':')+1).replace('_',' ').toLowerCase(Locale.ROOT);
            String type=raw.isEmpty()?raw:Character.toUpperCase(raw.charAt(0))+raw.substring(1);
            Map<String,String> values=Map.of("type",type,"world",row.world(),"x",Integer.toString(row.x()),
                    "y",Integer.toString(row.y()),"z",Integer.toString(row.z()),"stock",Integer.toString(row.stock()),
                    "productive",Integer.toString(row.productive()),"held",Integer.toString(row.stock()-row.productive()));
            inventory.setItem(i,item(Material.SPAWNER,"mine_item",values));
        }
        if(page.number()>1) inventory.setItem(45,item(Material.ARROW,"mine_previous",Map.of()));
        inventory.setItem(49,item(Material.BARRIER,"mine_close",Map.of("page",Integer.toString(page.number()),
                "pages",Integer.toString(page.pages()),"blocks",Integer.toString(page.blocks()),
                "stock",Long.toString(page.stock()),"productive",Long.toString(page.productive()))));
        if(page.number()<page.pages()) inventory.setItem(53,item(Material.ARROW,"mine_next",Map.of()));
        player.openInventory(inventory);
        if(page.blocks()==0) plugin.getMessageService().sendMessage(player,"mine.empty");
    }
    private ItemStack item(Material material,String path,Map<String,String> values) {
        var stack=new ItemStack(material);
        var meta=stack.getItemMeta();
        var text=LegacyComponentSerializer.legacySection();
        meta.displayName(text.deserialize(plugin.getLanguageManager().commandGui().name(path+".name",values))
                .decoration(TextDecoration.ITALIC,false));
        meta.lore(plugin.getLanguageManager().commandGui().loreList(path+".lore",values).stream()
                .map(line -> text.deserialize(line).decoration(TextDecoration.ITALIC,false)).toList());
        stack.setItemMeta(meta);
        return stack;
    }
    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Holder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !holder.viewer.equals(player.getUniqueId())
                || !player.hasPermission(getPermission())) return;
        int slot=event.getRawSlot();
        if(slot==49) player.closeInventory();
        else if(slot==45 && holder.page>1) request(player,holder.page-1);
        else if(slot==53 && holder.page<holder.pages) request(player,holder.page+1);
    }
    @EventHandler public void drag(InventoryDragEvent event) {
        if(event.getView().getTopInventory().getHolder(false) instanceof Holder) event.setCancelled(true);
    }
    private static final class Holder implements InventoryHolder {
        final UUID viewer; final int page,pages; Inventory inventory;
        Holder(UUID viewer,int page,int pages) { this.viewer=viewer;this.page=page;this.pages=pages; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
