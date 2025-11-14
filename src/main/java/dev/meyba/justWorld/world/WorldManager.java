package dev.meyba.justWorld.world;

import dev.meyba.justWorld.JustWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WorldManager {

    private final JustWorld plugin;
    private final Map<String, WorldData> worldDataMap;
    private final File worldsFile;

    public WorldManager(JustWorld plugin) {
        this.plugin = plugin;
        this.worldDataMap = new ConcurrentHashMap<>();
        this.worldsFile = new File(plugin.getDataFolder(), "worlds.yml");
        loadWorldsData();
    }

    /**
     * Asynchronously creates a new world with performance optimizations
     */
    public CompletableFuture<WorldCreationResult> createWorldAsync(WorldData worldData) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                // Create world on main thread (required by Bukkit API)
                World world = Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    WorldCreator creator = worldData.toWorldCreator();

                    // Performance optimization: Don't load spawn chunks during creation
                    creator.keepSpawnLoaded(false);

                    World newWorld = creator.createWorld();
                    if (newWorld != null) {
                        configureWorld(newWorld, worldData);
                        worldDataMap.put(newWorld.getName(), worldData);

                        // Save asynchronously without blocking
                        saveWorldsDataAsync();
                    }
                    return newWorld;
                }).get();

                long creationTime = System.currentTimeMillis() - startTime;
                return new WorldCreationResult(world, creationTime);

            } catch (Exception ex) {
                plugin.getLogger().severe("Error creating world: " + ex.getMessage());
                return new WorldCreationResult(null, System.currentTimeMillis() - startTime);
            }
        });
    }

    /**
     * Asynchronously loads an existing world
     */
    public CompletableFuture<World> loadWorldAsync(String worldName) {
        return CompletableFuture.supplyAsync(() -> {
            WorldData data = worldDataMap.get(worldName);
            if (data == null) {
                // Try to load world even if not in config
                File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
                if (!worldFolder.exists()) {
                    return null;
                }
            }

            return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                World world = Bukkit.getWorld(worldName);
                if (world == null && data != null) {
                    world = data.toWorldCreator().createWorld();
                    if (world != null) {
                        configureWorld(world, data);
                    }
                }
                return world;
            }).get();
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error loading world: " + ex.getMessage());
            return null;
        });
    }

    /**
     * Asynchronously unloads a world from server (without deleting files)
     */
    public CompletableFuture<Boolean> unloadWorldAsync(String worldName) {
        return CompletableFuture.supplyAsync(() -> {
            return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                World world = Bukkit.getWorld(worldName);
                if (world == null) return false;

                // Teleport all players to spawn world
                World spawnWorld = Bukkit.getWorlds().get(0);
                world.getPlayers().forEach(player ->
                    player.teleport(spawnWorld.getSpawnLocation())
                );

                return Bukkit.unloadWorld(world, true);
            }).get();
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error unloading world: " + ex.getMessage());
            return false;
        });
    }

    /**
     * Asynchronously deletes a world (including all files)
     */
    public CompletableFuture<Boolean> deleteWorldAsync(String worldName) {
        return unloadWorldAsync(worldName).thenApplyAsync(success -> {
            if (!success) return false;

            worldDataMap.remove(worldName);
            saveWorldsDataAsync();

            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            return deleteDirectory(worldFolder);
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error deleting world: " + ex.getMessage());
            return false;
        });
    }

    /**
     * Loads all auto-load worlds at server startup
     */
    public CompletableFuture<Void> loadAllWorldsAsync() {
        List<CompletableFuture<World>> futures = new ArrayList<>();

        worldDataMap.values().stream()
                .filter(WorldData::autoLoad)
                .forEach(data -> futures.add(loadWorldAsync(data.name())));

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> plugin.getLogger().info(
                        "Loaded " + futures.size() + " worlds asynchronously!"
                ));
    }

    public World getWorld(String name) {
        return Bukkit.getWorld(name);
    }

    public List<World> getAllWorlds() {
        return Bukkit.getWorlds();
    }

    public WorldData getWorldData(String name) {
        return worldDataMap.get(name);
    }

    public Collection<WorldData> getAllWorldData() {
        return worldDataMap.values();
    }

    private void configureWorld(World world, WorldData data) {
        world.setPVP(data.pvpEnabled());
        world.setKeepSpawnInMemory(data.keepSpawnInMemory());
    }

    private void loadWorldsData() {
        if (!worldsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                worldsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Cannot create worlds.yml: " + e.getMessage());
                return;
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(worldsFile);
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");

        if (worldsSection == null) return;

        worldsSection.getKeys(false).forEach(worldName -> {
            ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldName);
            if (worldSection == null) return;

            try {
                WorldData data = WorldData.builder(worldName)
                        .environment(World.Environment.valueOf(
                                worldSection.getString("environment", "NORMAL")))
                        .worldType(org.bukkit.WorldType.valueOf(
                                worldSection.getString("type", "NORMAL")))
                        .generateStructures(worldSection.getBoolean("generateStructures", true))
                        .seed(worldSection.getLong("seed", 0))
                        .pvpEnabled(worldSection.getBoolean("pvp", true))
                        .keepSpawnInMemory(worldSection.getBoolean("keepSpawnInMemory", false))
                        .autoLoad(worldSection.getBoolean("autoLoad", true))
                        .build();

                worldDataMap.put(worldName, data);
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading world " + worldName + ": " + e.getMessage());
            }
        });

        plugin.getLogger().info("Loaded " + worldDataMap.size() + " worlds from configuration");
    }

    private CompletableFuture<Void> saveWorldsDataAsync() {
        return CompletableFuture.runAsync(() -> {
            YamlConfiguration config = new YamlConfiguration();

            worldDataMap.forEach((name, data) -> {
                String path = "worlds." + name + ".";
                config.set(path + "environment", data.environment().name());
                config.set(path + "type", data.worldType().name());
                config.set(path + "generateStructures", data.generateStructures());
                config.set(path + "seed", data.seed());
                config.set(path + "pvp", data.pvpEnabled());
                config.set(path + "keepSpawnInMemory", data.keepSpawnInMemory());
                config.set(path + "autoLoad", data.autoLoad());
            });

            try {
                config.save(worldsFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Error saving worlds.yml: " + e.getMessage());
            }
        });
    }

    private boolean deleteDirectory(File directory) {
        if (!directory.exists()) return true;

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return directory.delete();
    }
}
