package dev.meyba.justWorld.world;

import dev.meyba.justWorld.JustWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

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

    public CompletableFuture<WorldCreationResult> createWorldAsync(WorldData worldData) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                World world = Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    WorldCreator creator = worldData.toWorldCreator();

                    creator.keepSpawnInMemory(false);

                    World newWorld = creator.createWorld();
                    if (newWorld != null) {
                        configureWorld(newWorld, worldData);
                        worldDataMap.put(newWorld.getName(), worldData);

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

    public CompletableFuture<World> loadWorldAsync(String worldName) {
        return CompletableFuture.supplyAsync(() -> {
            WorldData data = worldDataMap.get(worldName);
            if (data == null) {
                File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
                if (!worldFolder.exists()) {
                    return null;
                }
            }

            try {
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
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error loading world: " + ex.getMessage());
            return null;
        });
    }

    public CompletableFuture<Boolean> unloadWorldAsync(String worldName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) return false;

                    World spawnWorld = Bukkit.getWorlds().get(0);
                    world.getPlayers().forEach(player ->
                        player.teleport(spawnWorld.getSpawnLocation())
                    );

                    return Bukkit.unloadWorld(world, true);
                }).get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error unloading world: " + ex.getMessage());
            return false;
        });
    }

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
                WorldData.GeneratorType generatorType = WorldData.GeneratorType.DEFAULT;
                try {
                    generatorType = WorldData.GeneratorType.valueOf(
                            worldSection.getString("generatorType", "DEFAULT").toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }

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
                        .generatorType(generatorType)
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
                config.set(path + "generatorType", data.generatorType().name());
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