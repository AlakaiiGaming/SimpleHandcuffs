package com.alakaiigaming.simplehandcuffs;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class CuffsCommand implements TabExecutor {

    private final SimpleHandcuffs plugin;

    CuffsCommand(SimpleHandcuffs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages msg = plugin.messages();
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";

        switch (sub) {
            case "give" -> {
                if (!sender.hasPermission("handcuffs.create")) {
                    msg.send(sender, "no-permission");
                    return true;
                }
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayerExact(args[1]);
                } else {
                    target = sender instanceof Player p ? p : null;
                }
                if (target == null) {
                    msg.send(sender, "player-not-found");
                    return true;
                }
                int amount = 1;
                if (args.length >= 3) {
                    try {
                        amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
                    } catch (NumberFormatException e) {
                        msg.send(sender, "usage");
                        return true;
                    }
                }
                for (int i = 0; i < amount; i++) {
                    ItemStack cuffs = plugin.cuffItem().create();
                    Map<Integer, ItemStack> left = target.getInventory().addItem(cuffs);
                    left.values().forEach(item -> target.getWorld().dropItem(target.getLocation(), item));
                }
                msg.send(target, "received");
                if (target != sender) {
                    msg.send(sender, "given", "%amount%", String.valueOf(amount), "%player%", target.getName());
                }
            }
            case "removeall" -> {
                if (!sender.hasPermission("handcuffs.removeall")) {
                    msg.send(sender, "no-permission");
                    return true;
                }
                int released = plugin.cuffs().releaseAll();
                plugin.cuffs().bumpEpoch();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    plugin.cuffItem().purge(player);
                }
                msg.send(sender, "removed-all", "%count%", String.valueOf(released));
            }
            default -> msg.send(sender, "usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("handcuffs.create")) out.add("give");
            if (sender.hasPermission("handcuffs.removeall")) out.add("removeall");
            out.removeIf(s -> !s.startsWith(args[0].toLowerCase(Locale.ROOT)));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("handcuffs.create")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(player.getName());
                }
            }
        }
        return out;
    }
}
