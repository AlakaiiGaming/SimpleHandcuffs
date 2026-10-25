package com.alakaiigaming.simplehandcuffs;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

final class LockListener implements Listener {

    private static final Set<String> BLOCKED_TELEPORTS = Set.of("ENDER_PEARL", "CHORUS_FRUIT", "CONSUMABLE_EFFECT");

    private final SimpleHandcuffs plugin;

    LockListener(SimpleHandcuffs plugin) {
        this.plugin = plugin;
    }

    private boolean cuffed(Entity entity) {
        return entity instanceof Player player && plugin.cuffs().isCuffed(player);
    }

    private void deny(Entity entity, Cancellable event) {
        if (!cuffed(entity)) return;
        event.setCancelled(true);
        plugin.messages().sendThrottled((Player) entity, "locked");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) { deny(event.getWhoClicked(), event); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) { deny(event.getWhoClicked(), event); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            plugin.cuffItem().purge(event.getInventory());
            plugin.cuffItem().purge(player);
        }
        deny(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) { deny(event.getPlayer(), event); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent event) {
        Item item = event.getItem();
        if (plugin.cuffItem().isStale(item.getItemStack())) {
            event.setCancelled(true);
            item.remove();
            return;
        }
        if (cuffed(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwap(PlayerSwapHandItemsEvent event) { deny(event.getPlayer(), event); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHotbar(PlayerItemHeldEvent event) { deny(event.getPlayer(), event); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!cuffed(event.getPlayer())) return;
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) { deny(event.getPlayer(), event); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) { deny(event.getPlayer(), event); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onConsume(PlayerItemConsumeEvent event) { deny(event.getPlayer(), event); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) { deny(event.getPlayer(), event); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent event) { deny(event.getPlayer(), event); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAttack(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }
        deny(damager, event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() != null) deny(event.getRemover(), event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMount(VehicleEnterEvent event) { deny(event.getEntered(), event); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onGlide(EntityToggleGlideEvent event) {
        if (event.isGliding() && cuffed(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent event) {
        if (cuffed(event.getPlayer()) && BLOCKED_TELEPORTS.contains(event.getCause().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!cuffed(event.getPlayer())) return;

        String label = event.getMessage().substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);

        List<String> allowed = plugin.getConfig().getStringList("allowed-commands");
        for (String command : allowed) {
            if (command.toLowerCase(Locale.ROOT).equals(label)) return;
        }
        event.setCancelled(true);
        plugin.messages().send(event.getPlayer(), "command-blocked");
    }

    // officer offline: prisoner stays put
    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID officer = plugin.cuffs().officerOf(player.getUniqueId());
        if (officer == null || plugin.getServer().getPlayer(officer) != null) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) return;
        Location stay = from.clone();
        stay.setY(Math.min(from.getY(), to.getY()));
        stay.setYaw(to.getYaw());
        stay.setPitch(to.getPitch());
        event.setTo(stay);
    }
}
