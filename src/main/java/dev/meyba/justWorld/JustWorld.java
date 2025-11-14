package dev.meyba.justWorld;

import dev.meyba.justWorld.command.WorldCommand;
import dev.meyba.justWorld.world.WorldManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class JustWorld extends JavaPlugin {

    private WorldManager worldManager;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // Initialize WorldManager
        this.worldManager = new WorldManager(this);
        getLogger().info("WorldManager initialized");

        // Register commands
        WorldCommand worldCommand = new WorldCommand(this);
        getCommand("world").setExecutor(worldCommand);
        getCommand("world").setTabCompleter(worldCommand);
        getLogger().info("Commands registered");

        // Asynchronously load all worlds
        worldManager.loadAllWorldsAsync().thenRun(() -> {
            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info("JustWorld enabled in " + loadTime + "ms!");
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("JustWorld disabled!");
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }
}