package dev.meyba.justWorld;

import dev.meyba.justWorld.command.JustWorldCommand;
import dev.meyba.justWorld.command.WorldCommand;
import dev.meyba.justWorld.utils.ChatUtil;
import dev.meyba.justWorld.utils.VersionChecker;
import dev.meyba.justWorld.world.WorldManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class JustWorld extends JavaPlugin {
    private WorldManager worldManager;
    private ChatUtil chatUtil;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        saveDefaultConfig();

        chatUtil = new ChatUtil(this);

        worldManager = new WorldManager(this);
        getLogger().info("WorldManager initialized");

        WorldCommand worldCommand = new WorldCommand(this);
        getCommand("world").setExecutor(worldCommand);
        getCommand("world").setTabCompleter(worldCommand);

        JustWorldCommand justWorldCommand = new JustWorldCommand(this);
        getCommand("justworld").setExecutor(justWorldCommand);

        getLogger().info("Commands registered");

        new VersionChecker(this, "RokyYTR2", "JustWorld").checkForUpdates();

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

    public ChatUtil getMessageUtil() {
        return chatUtil;
    }
}