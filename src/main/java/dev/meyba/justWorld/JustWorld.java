package dev.meyba.justWorld;

import dev.meyba.justWorld.command.WorldCommand;
import dev.meyba.justWorld.gui.WorldGUI;
import dev.meyba.justWorld.managers.WorldManager;
import dev.meyba.justWorld.utils.ChatUtil;
import dev.meyba.justWorld.utils.VersionChecker;
import org.bukkit.plugin.java.JavaPlugin;

public final class JustWorld extends JavaPlugin {
    private WorldManager worldManager;
    private ChatUtil chatUtil;
    private WorldGUI worldGUI;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        saveDefaultConfig();

        chatUtil = new ChatUtil(this);

        worldManager = new WorldManager(this);
        getLogger().info("WorldManager initialized");

        worldGUI = new WorldGUI(this);
        getServer().getPluginManager().registerEvents(worldGUI, this);

        WorldCommand worldCommand = new WorldCommand(this);
        getCommand("world").setExecutor(worldCommand);
        getCommand("world").setTabCompleter(worldCommand);
        getCommand("justworld").setExecutor(worldCommand);
        getCommand("justworld").setTabCompleter(worldCommand);

        getLogger().info("Commands registered");

        new VersionChecker(this, "RokyYTR2", "JustWorld").checkForUpdates();

        worldManager.loadAllWorlds().thenRun(() -> {
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

    public WorldGUI getWorldGUI() {
        return worldGUI;
    }
}