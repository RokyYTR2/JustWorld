package dev.meyba.justWorld;

import org.bukkit.plugin.java.JavaPlugin;

public final class JustWorld extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("JustWorld has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("JustWorld has been disabled!");
    }
}