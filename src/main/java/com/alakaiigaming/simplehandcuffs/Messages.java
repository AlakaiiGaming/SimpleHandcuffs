package com.alakaiigaming.simplehandcuffs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Messages {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final SimpleHandcuffs plugin;
    private final Map<UUID, Long> lastLockedNotice = new HashMap<>();

    Messages(SimpleHandcuffs plugin) {
        this.plugin = plugin;
    }

    public static Component color(String text) {
        Component component = LEGACY.deserialize(text == null ? "" : text);
        return component.decoration(TextDecoration.ITALIC, false);
    }

    public void send(CommandSender to, String key, String... replacements) {
        String text = plugin.getConfig().getString("messages." + key);
        if (text == null || text.isEmpty()) return;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        to.sendMessage(color(prefix + text));
    }

    public void sendThrottled(org.bukkit.entity.Player to, String key) {
        long now = System.currentTimeMillis();
        Long last = lastLockedNotice.get(to.getUniqueId());
        if (last != null && now - last < 1000) return;
        lastLockedNotice.put(to.getUniqueId(), now);
        send(to, key);
    }

    public void forget(UUID player) {
        lastLockedNotice.remove(player);
    }
}
