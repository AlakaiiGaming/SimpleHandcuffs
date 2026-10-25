package com.alakaiigaming.simplehandcuffs;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CuffManager {

    private static final String SOUND_CUFF = "minecraft:block.chain.place";
    private static final String SOUND_RELEASE = "minecraft:block.chain.break";

    private final SimpleHandcuffs plugin;
    private final File dataFile;
    /** prisoner -> officer */
    private final Map<UUID, UUID> prisoners = new HashMap<>();
    private final Map<UUID, Long> lastArrest = new HashMap<>();
    private int epoch;

    CuffManager(SimpleHandcuffs plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        prisoners.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        epoch = data.getInt("epoch", 0);
        ConfigurationSection section = data.getConfigurationSection("prisoners");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                prisoners.put(UUID.fromString(key), UUID.fromString(section.getString(key, "")));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Skipping bad entry in data.yml: " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("epoch", epoch);
        for (Map.Entry<UUID, UUID> entry : prisoners.entrySet()) {
            data.set("prisoners." + entry.getKey(), entry.getValue().toString());
        }
        try {
            plugin.getDataFolder().mkdirs();
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Couldn't save data.yml: " + e.getMessage());
        }
    }

    public int epoch() { return epoch; }

    public boolean isCuffed(Player player) {
        return prisoners.containsKey(player.getUniqueId());
    }

    public UUID officerOf(UUID prisoner) {
        return prisoners.get(prisoner);
    }

    public Map<UUID, UUID> all() {
        return prisoners;
    }

    public List<UUID> prisonersOf(UUID officer) {
        List<UUID> list = new ArrayList<>();
        for (Map.Entry<UUID, UUID> entry : prisoners.entrySet()) {
            if (entry.getValue().equals(officer)) list.add(entry.getKey());
        }
        return list;
    }

    public boolean onCooldown(Player officer) {
        long seconds = plugin.getConfig().getLong("arrest-cooldown-seconds", 5);
        Long last = lastArrest.get(officer.getUniqueId());
        return last != null && System.currentTimeMillis() - last < seconds * 1000L;
    }

    public void cuff(Player officer, Player prisoner) {
        prisoners.put(prisoner.getUniqueId(), officer.getUniqueId());
        lastArrest.put(officer.getUniqueId(), System.currentTimeMillis());

        prisoner.leaveVehicle();
        prisoner.closeInventory();
        if (prisoner.isGliding()) prisoner.setGliding(false);

        plugin.messages().send(prisoner, "arrested", "%officer%", officer.getName());
        plugin.messages().send(officer, "arrest-made", "%prisoner%", prisoner.getName());
        prisoner.getWorld().playSound(prisoner.getLocation(), SOUND_CUFF, 1f, 0.8f);
        save();
    }

    public void release(UUID prisonerId) {
        UUID officerId = prisoners.remove(prisonerId);
        if (officerId == null) return;
        plugin.chains().remove(prisonerId);

        Player prisoner = Bukkit.getPlayer(prisonerId);
        Player officer = Bukkit.getPlayer(officerId);
        if (prisoner != null) {
            plugin.messages().send(prisoner, "released", "%officer%", nameOf(officerId));
            prisoner.getWorld().playSound(prisoner.getLocation(), SOUND_RELEASE, 1f, 1f);
        }
        if (officer != null) {
            plugin.messages().send(officer, "release-made", "%prisoner%", nameOf(prisonerId));
        }
        save();
    }

    public int releaseAll() {
        List<UUID> ids = new ArrayList<>(prisoners.keySet());
        for (UUID id : ids) release(id);
        return ids.size();
    }

    public void bumpEpoch() {
        epoch++;
        save();
    }

    public static String nameOf(UUID id) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(id);
        return player.getName() != null ? player.getName() : "someone";
    }
}
