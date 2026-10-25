package com.alakaiigaming.simplehandcuffs;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleHandcuffs extends JavaPlugin {

    private Messages messages;
    private CuffItem cuffItem;
    private CuffManager cuffs;
    private ChainRenderer chains;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messages = new Messages(this);
        chains = new ChainRenderer(this);
        cuffs = new CuffManager(this);
        cuffs.load();
        cuffItem = new CuffItem(this);

        Bukkit.getPluginManager().registerEvents(new CuffListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LockListener(this), this);

        PluginCommand command = getCommand("cuffs");
        if (command != null) {
            CuffsCommand executor = new CuffsCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        Bukkit.getScheduler().runTaskTimer(this, new FollowTask(this), 20L, 2L);
    }

    @Override
    public void onDisable() {
        if (chains != null) chains.clearAll();
        if (cuffs != null) cuffs.save();
    }

    public Messages messages() { return messages; }
    public CuffItem cuffItem() { return cuffItem; }
    public CuffManager cuffs() { return cuffs; }
    public ChainRenderer chains() { return chains; }
}
