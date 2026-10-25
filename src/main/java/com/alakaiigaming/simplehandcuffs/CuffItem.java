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
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Messages.color(plugin.getConfig().getString("item.name", "&6Handcuffs")));

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

    public boolean isCuffs(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return false;
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
