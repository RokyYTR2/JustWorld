package dev.meyba.justWorld;

import dev.meyba.justWorld.command.WorldCommand;
import dev.meyba.justWorld.world.WorldManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class JustWorld extends JavaPlugin {

    private WorldManager worldManager;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // Inicializace WorldManager
        this.worldManager = new WorldManager(this);
        getLogger().info("WorldManager inicializován");

        // Registrace příkazů
        WorldCommand worldCommand = new WorldCommand(this);
        getCommand("world").setExecutor(worldCommand);
        getCommand("world").setTabCompleter(worldCommand);
        getLogger().info("Příkazy zaregistrovány");

        // Asynchronní načtení všech světů
        worldManager.loadAllWorldsAsync().thenRun(() -> {
            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info("JustWorld byl aktivován za " + loadTime + "ms!");
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("JustWorld byl deaktivován!");
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }
}