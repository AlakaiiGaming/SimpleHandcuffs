package com.alakaiigaming.simplehandcuffs;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

final class CuffListener implements Listener {

    private final SimpleHandcuffs plugin;

    CuffListener(SimpleHandcuffs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRightClickPlayer(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;
        if (target.hasMetadata("NPC")) return;

        Player officer = event.getPlayer();
        ItemStack hand = officer.getInventory().getItemInMainHand();
        CuffItem items = plugin.cuffItem();
        if (!items.isCuffs(hand)) return;
        event.setCancelled(true);

        if (items.isStale(hand)) {
            officer.getInventory().setItemInMainHand(null);
            plugin.messages().send(officer, "cuffs-gone");
            return;
        }

        CuffManager cuffs = plugin.cuffs();
        if (cuffs.isCuffed(officer)) return;

        UUID currentOfficer = cuffs.officerOf(target.getUniqueId());
        if (currentOfficer != null) {
            if (currentOfficer.equals(officer.getUniqueId())) {
                cuffs.release(target.getUniqueId());
            } else {
                plugin.messages().send(officer, "already-cuffed",
                        "%prisoner%", target.getName(), "%officer%", CuffManager.nameOf(currentOfficer));
            }
            return;
        }

        if (target.hasPermission("handcuffs.immune")) {
            plugin.messages().send(officer, "immune", "%prisoner%", target.getName());
            return;
        }
        int max = plugin.getConfig().getInt("max-prisoners-per-officer", 1);
        if (max > 0 && cuffs.prisonersOf(officer.getUniqueId()).size() >= max) {
            plugin.messages().send(officer, "max-prisoners");
            return;
        }
        if (cuffs.onCooldown(officer)) {
            plugin.messages().send(officer, "cooldown");
            return;
        }

        cuffs.cuff(officer, target);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.cuffItem().purge(player);

        UUID officer = plugin.cuffs().officerOf(player.getUniqueId());
        if (officer != null) {
            plugin.messages().send(player, "still-arrested", "%officer%", CuffManager.nameOf(officer));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.chains().remove(event.getPlayer().getUniqueId());
        plugin.messages().forget(event.getPlayer().getUniqueId());
        for (UUID prisoner : plugin.cuffs().prisonersOf(event.getPlayer().getUniqueId())) {
            plugin.chains().remove(prisoner);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        CuffManager cuffs = plugin.cuffs();
        if (cuffs.officerOf(id) != null) cuffs.release(id);
        for (UUID prisoner : cuffs.prisonersOf(id)) cuffs.release(prisoner);
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (plugin.cuffItem().isCuffs(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnace(FurnaceBurnEvent event) {
        if (plugin.cuffItem().isCuffs(event.getFuel())) event.setCancelled(true);
    }
}
