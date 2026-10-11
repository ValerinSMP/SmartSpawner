package github.nighter.smartspawner.production;

import github.nighter.smartspawner.SmartSpawner;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/** Only placed blocks have production ownership. Retired item tags are ignored/removed, not provenance. */
public final class ProductionOwnershipItem {
    private final NamespacedKey ownerKey;
    private final NamespacedKey farmKey;
    public ProductionOwnershipItem(SmartSpawner plugin) {
        ownerKey = new NamespacedKey(plugin, "production_owner_v1");
        farmKey = new NamespacedKey(plugin, "production_farm_v1");
    }
    public ProductionOwnershipRepository.Ownership read(ItemStack item) {
        // Existing held items may carry old tags: they confer neither ownership nor a farm budget.
        return new ProductionOwnershipRepository.Ownership(null, null);
    }
    public void write(ItemStack item, ProductionOwnershipRepository.Ownership ignored) {
        // Callers pass their clone, never a cached/shared template. Keep all unrelated item data.
        if (item == null || !item.hasItemMeta()) return;
        var meta = item.getItemMeta();
        var data = meta.getPersistentDataContainer();
        data.remove(ownerKey);
        data.remove(farmKey);
        item.setItemMeta(meta);
    }
}
