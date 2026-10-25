package com.alakaiigaming.simplehandcuffs;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CuffItem {

    private final SimpleHandcuffs plugin;
    private final NamespacedKey idKey;
    private final NamespacedKey epochKey;

    CuffItem(SimpleHandcuffs plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "cuffs");
        this.epochKey = new NamespacedKey(plugin, "epoch");
    }

    public ItemStack create() {
        // a stick, so getting cuffs doesn't grant "Into Fire"
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Messages.color(plugin.getConfig().getString("item.name", "&6Handcuffs")));
        setModel(meta, plugin.getConfig().getString("item.model", "minecraft:blaze_rod"));

        List<Component> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("item.lore")) {
            lore.add(Messages.color(line));
        }
        meta.lore(lore);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        // unique id so they don't stack
        data.set(idKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        data.set(epochKey, PersistentDataType.INTEGER, plugin.cuffs().epoch());
        item.setItemMeta(meta);
        return item;
    }

    // item_model only exists on 1.21.4+; older servers just show a stick
    private static void setModel(ItemMeta meta, String model) {
        NamespacedKey key = model == null ? null : NamespacedKey.fromString(model);
        if (key == null) return;
        try {
            ItemMeta.class.getMethod("setItemModel", NamespacedKey.class).invoke(meta, key);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public boolean isCuffs(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(idKey, PersistentDataType.STRING);
    }

    // made before the last /cuffs removeall
    public boolean isStale(ItemStack item) {
        if (!isCuffs(item)) return false;
        Integer epoch = item.getItemMeta().getPersistentDataContainer().get(epochKey, PersistentDataType.INTEGER);
        return epoch == null || epoch < plugin.cuffs().epoch();
    }

    public int purge(Inventory inventory) {
        int removed = 0;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isStale(contents[i])) {
                inventory.setItem(i, null);
                removed++;
            }
        }
        return removed;
    }

    public void purge(Player player) {
        purge(player.getInventory());
        purge(player.getEnderChest());
        if (isStale(player.getItemOnCursor())) player.setItemOnCursor(null);
    }
}
