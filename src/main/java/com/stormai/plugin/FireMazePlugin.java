package com.stormai.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class FireMazePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("FireMaze Plugin has been enabled!");
        getCommand("firemaze").setExecutor(new CommandHandler(this));
        getServer().getPluginManager().registerEvents(new MazeListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("FireMaze Plugin has been disabled!");
    }
}