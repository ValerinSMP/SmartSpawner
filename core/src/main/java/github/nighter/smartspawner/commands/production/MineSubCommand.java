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
    private final Map<UUID,Object> requests=new java.util.concurrent.ConcurrentHashMap<>();
    public MineSubCommand(SmartSpawner plugin) {
        super(plugin);
        plugin.getServer().getPluginManager().registerEvents(this,plugin);
    }
    @Override public String getName() { return "mine"; }
    @Override public String getPermission() { return "smartspawner.command.mine"; }
    @Override public String getDescription() { return "Consulta tus spawners, filtros y producción"; }
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
        request(player,page,OwnSpawnerListing.Filter.ALL,null,OwnSpawnerListing.Sort.TYPE);
        return 1;
    }
    private void request(Player player,int page,OwnSpawnerListing.Filter filter,String type,OwnSpawnerListing.Sort sort) {
        UUID viewer=player.getUniqueId();
        if (!player.hasPermission(getPermission())) return;
        Object token=new Object(); requests.put(viewer,token);
        Inventory expected=player.getOpenInventory().getTopInventory();
        plugin.getProductionCapacityService().ownListing(viewer,page,filter,type,sort).whenComplete((result,failure) ->
                Scheduler.runEntityTask(player,() -> {
                    if (!requests.remove(viewer,token) || player.getOpenInventory().getTopInventory()!=expected) return;
                    if (!player.isOnline() || !viewer.equals(player.getUniqueId()) || !player.hasPermission(getPermission())) return;
                    if (failure!=null) { plugin.getMessageService().sendMessage(player,"mine.pending"); return; }
                    open(player,result,filter,type,sort);
                }));
    }
    private void open(Player player,OwnSpawnerListing.Page page,OwnSpawnerListing.Filter filter,String type,OwnSpawnerListing.Sort sort) {
        var holder=new Holder(player.getUniqueId(),page.number(),page.pages(),filter,type,sort,page.types());
        Map<String,String> summary=Map.of("page",number(page.number()),"pages",number(page.pages()),"blocks",number(page.blocks()),
                "stock",number(page.stock()),"productive",number(page.productive()));
        Inventory inventory=Bukkit.createInventory(holder,54,plugin.getLanguageManager().commandGui().title("mine_title",summary));
        holder.inventory=inventory;
        for(int i=0;i<page.entries().size();i++) {
            var row=page.entries().get(i);
            Map<String,String> values=Map.of("type",typeName(row.type()),"world",row.world(),"x",Integer.toString(row.x()),
                    "y",Integer.toString(row.y()),"z",Integer.toString(row.z()),"stock",number(row.stock()),
                    "productive",number(row.productive()),"held",number(row.stock()-row.productive()));
            inventory.setItem(i,item(Material.SPAWNER,"mine_item",values));
        }
        if(page.number()>1) inventory.setItem(45,item(Material.ARROW,"mine_previous",summary));
        inventory.setItem(46,item(Material.SPAWNER,"mine_type",Map.of("type",type==null?label("all"):typeName(type))));
        inventory.setItem(48,item(Material.HOPPER,"mine_filter",Map.of("filter",label("filter_"+filter.name().toLowerCase(Locale.ROOT)))));
        inventory.setItem(49,item(Material.BARRIER,"mine_close",summary));
        inventory.setItem(50,item(Material.COMPARATOR,"mine_sort",Map.of("sort",label("sort_"+sort.name().toLowerCase(Locale.ROOT)))));
        inventory.setItem(51,item(Material.PAPER,"mine_summary",summary));
        if(page.number()<page.pages()) inventory.setItem(53,item(Material.ARROW,"mine_next",summary));
        player.openInventory(inventory);
        if(page.blocks()==0) plugin.getMessageService().sendMessage(player,page.types().isEmpty()?"mine.empty":"mine.no_results");
        else player.playSound(player.getLocation(),Sound.UI_BUTTON_CLICK,1.0f,1.0f);
    }
    private String number(long value) { return plugin.getLanguageManager().formatNumber(value); }
    private String label(String key) { return plugin.getLanguageManager().commandGui().name("mine_labels."+key); }
    private String typeName(String type) {
        String raw=type.substring(type.indexOf(':')+1);
        if(type.startsWith("MOB:")) {
            try { return plugin.getLanguageManager().getFormattedMobName(org.bukkit.entity.EntityType.valueOf(raw)); }
            catch(IllegalArgumentException ignored) { }
        }
        return raw.replace('_',' ').toLowerCase(Locale.ROOT);
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
        if(slot==49) { requests.remove(player.getUniqueId()); player.closeInventory(); player.playSound(player.getLocation(),Sound.UI_BUTTON_CLICK,1.0f,1.0f); }
        else if(slot==45 && holder.page>1) request(player,holder.page-1,holder.filter,holder.type,holder.sort);
        else if(slot==53 && holder.page<holder.pages) request(player,holder.page+1,holder.filter,holder.type,holder.sort);
        else if(slot==48) request(player,1,holder.filter.next(),holder.type,holder.sort);
        else if(slot==50) request(player,1,holder.filter,holder.type,holder.sort.next());
        else if(slot==46) {
            int index=holder.type==null?-1:holder.types.indexOf(holder.type);
            String next=index+1<holder.types.size()?holder.types.get(index+1):null;
            request(player,1,holder.filter,next,holder.sort);
        }
    }
    @EventHandler public void close(InventoryCloseEvent event) {
        if(event.getInventory().getHolder(false) instanceof Holder) requests.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler public void quit(org.bukkit.event.player.PlayerQuitEvent event) { requests.remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void drag(InventoryDragEvent event) {
        if(event.getView().getTopInventory().getHolder(false) instanceof Holder) event.setCancelled(true);
    }
    private static final class Holder implements InventoryHolder {
        final UUID viewer; final int page,pages; final OwnSpawnerListing.Filter filter;
        final String type; final OwnSpawnerListing.Sort sort; final List<String> types; Inventory inventory;
        Holder(UUID viewer,int page,int pages,OwnSpawnerListing.Filter filter,String type,OwnSpawnerListing.Sort sort,List<String> types) {
            this.viewer=viewer;this.page=page;this.pages=pages;this.filter=filter;this.type=type;this.sort=sort;this.types=types;
        }
        @Override public Inventory getInventory() { return inventory; }
    }
}
