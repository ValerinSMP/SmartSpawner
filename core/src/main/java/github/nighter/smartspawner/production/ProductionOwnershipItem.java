package github.nighter.smartspawner.production;

import github.nighter.smartspawner.SmartSpawner;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.UUID;

/** Ownership declaration only. This is NOT an authenticity or issuance/provenance API. */
public final class ProductionOwnershipItem {
    private final NamespacedKey ownerKey;
    private final NamespacedKey farmKey;
    public ProductionOwnershipItem(SmartSpawner plugin) {
        ownerKey = new NamespacedKey(plugin, "production_owner_v1");
        farmKey = new NamespacedKey(plugin, "production_farm_v1");
    }
    public ProductionOwnershipRepository.Ownership read(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return new ProductionOwnershipRepository.Ownership(null, null);
        var data = item.getItemMeta().getPersistentDataContainer();
        return new ProductionOwnershipRepository.Ownership(parse(data.get(ownerKey, PersistentDataType.STRING)),
                parse(data.get(farmKey, PersistentDataType.STRING)));
    }
    public void write(ItemStack item, ProductionOwnershipRepository.Ownership ownership) {
        // Stamp only the caller's clone, never a factory cache/shared template.
        var meta = item.getItemMeta();
        var data = meta.getPersistentDataContainer();
        data.remove(ownerKey);
        data.remove(farmKey);
        if (ownership != null) {
            if (ownership.owner() != null) data.set(ownerKey, PersistentDataType.STRING, ownership.owner().toString());
            if (ownership.farm() != null) data.set(farmKey, PersistentDataType.STRING, ownership.farm().toString());
        }
        item.setItemMeta(meta);
    }
    private UUID parse(String value) {
        try { return value == null ? null : UUID.fromString(value); }
        catch (IllegalArgumentException invalid) { return null; }
    }
}
