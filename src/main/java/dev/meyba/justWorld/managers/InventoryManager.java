package dev.meyba.justWorld.managers;

import dev.meyba.justWorld.JustWorld;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class InventoryManager implements Listener {
    private final JustWorld plugin;
    private final File inventoriesFolder;
    private final Map<String, String> worldGroups;
    private final Set<String> enabledWorlds;
    private boolean enabled;
    private boolean separateGamemodes;

    public InventoryManager(JustWorld plugin) {
        this.plugin = plugin;
        this.inventoriesFolder = new File(plugin.getDataFolder(), "inventories");
        this.worldGroups = new HashMap<>();
        this.enabledWorlds = new HashSet<>();

        if (!inventoriesFolder.exists()) {
            inventoriesFolder.mkdirs();
        }

        loadConfig();
    }

    private void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("per-world-inventory.enabled", false);
        this.separateGamemodes = plugin.getConfig().getBoolean("per-world-inventory.separate-gamemodes", true);

        worldGroups.clear();
        enabledWorlds.clear();

        ConfigurationSection groupsSection = plugin.getConfig().getConfigurationSection("per-world-inventory.groups");
        if (groupsSection != null) {
            groupsSection.getKeys(false).forEach(groupName -> {
                List<String> worlds = groupsSection.getStringList(groupName);
                worlds.forEach(world -> worldGroups.put(world, groupName));
            });
        }

        List<String> enabledList = plugin.getConfig().getStringList("per-world-inventory.enabled-worlds");
        enabledWorlds.addAll(enabledList);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();

        if (!isInventorySeparated(fromWorld) && !isInventorySeparated(toWorld)) {
            return;
        }

        String fromGroup = getWorldGroup(fromWorld);
        String toGroup = getWorldGroup(toWorld);

        if (fromGroup.equals(toGroup)) {
            return;
        }

        savePlayerData(player, fromGroup);
        loadPlayerData(player, toGroup);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        if (!isInventorySeparated(worldName)) return;

        String group = getWorldGroup(worldName);
        loadPlayerData(player, group);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        if (!isInventorySeparated(worldName)) return;

        String group = getWorldGroup(worldName);
        savePlayerData(player, group);
    }

    private boolean isInventorySeparated(String worldName) {
        if (enabledWorlds.isEmpty()) {
            return true;
        }
        return enabledWorlds.contains(worldName) || worldGroups.containsKey(worldName);
    }

    private String getWorldGroup(String worldName) {
        return worldGroups.getOrDefault(worldName, worldName);
    }

    private void savePlayerData(Player player, String group) {
        CompletableFuture.runAsync(() -> {
            File playerFile = getPlayerFile(player.getUniqueId(), group);
            YamlConfiguration config = new YamlConfiguration();

            String gmPrefix = separateGamemodes ? player.getGameMode().name() + "." : "";

            config.set(gmPrefix + "inventory", player.getInventory().getContents());
            config.set(gmPrefix + "armor", player.getInventory().getArmorContents());
            config.set(gmPrefix + "offhand", player.getInventory().getItemInOffHand());
            config.set(gmPrefix + "enderchest", player.getEnderChest().getContents());

            config.set(gmPrefix + "health", player.getHealth());
            config.set(gmPrefix + "max-health", player.getMaxHealth());
            config.set(gmPrefix + "food", player.getFoodLevel());
            config.set(gmPrefix + "saturation", player.getSaturation());
            config.set(gmPrefix + "exp", player.getExp());
            config.set(gmPrefix + "level", player.getLevel());

            List<Map<String, Object>> effects = new ArrayList<>();
            for (PotionEffect effect : player.getActivePotionEffects()) {
                Map<String, Object> effectData = new HashMap<>();
                effectData.put("type", effect.getType().getKey().getKey());
                effectData.put("duration", effect.getDuration());
                effectData.put("amplifier", effect.getAmplifier());
                effectData.put("ambient", effect.isAmbient());
                effectData.put("particles", effect.hasParticles());
                effects.add(effectData);
            }
            config.set(gmPrefix + "effects", effects);

            try {
                config.save(playerFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save inventory for " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    private void loadPlayerData(Player player, String group) {
        File playerFile = getPlayerFile(player.getUniqueId(), group);

        if (!playerFile.exists()) {
            clearPlayerData(player);
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        String gmPrefix = separateGamemodes ? player.getGameMode().name() + "." : "";

        if (!config.contains(gmPrefix + "inventory")) {
            clearPlayerData(player);
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                @SuppressWarnings("unchecked")
                List<ItemStack> inventoryList = (List<ItemStack>) config.getList(gmPrefix + "inventory");
                if (inventoryList != null) {
                    player.getInventory().setContents(inventoryList.toArray(new ItemStack[0]));
                }

                @SuppressWarnings("unchecked")
                List<ItemStack> armorList = (List<ItemStack>) config.getList(gmPrefix + "armor");
                if (armorList != null) {
                    player.getInventory().setArmorContents(armorList.toArray(new ItemStack[0]));
                }

                ItemStack offhand = config.getItemStack(gmPrefix + "offhand");
                if (offhand != null) {
                    player.getInventory().setItemInOffHand(offhand);
                }

                @SuppressWarnings("unchecked")
                List<ItemStack> enderList = (List<ItemStack>) config.getList(gmPrefix + "enderchest");
                if (enderList != null) {
                    player.getEnderChest().setContents(enderList.toArray(new ItemStack[0]));
                }

                double maxHealth = config.getDouble(gmPrefix + "max-health", 20.0);
                player.setMaxHealth(maxHealth);
                player.setHealth(Math.min(config.getDouble(gmPrefix + "health", 20.0), maxHealth));
                player.setFoodLevel(config.getInt(gmPrefix + "food", 20));
                player.setSaturation((float) config.getDouble(gmPrefix + "saturation", 5.0));
                player.setExp((float) config.getDouble(gmPrefix + "exp", 0));
                player.setLevel(config.getInt(gmPrefix + "level", 0));

                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }

                List<Map<?, ?>> effects = config.getMapList(gmPrefix + "effects");
                for (Map<?, ?> effectData : effects) {
                    try {
                        String typeName = (String) effectData.get("type");
                        PotionEffectType type = Registry.EFFECT.get(
                                NamespacedKey.minecraft(typeName.toLowerCase()));
                        if (type != null) {
                            int duration = ((Number) effectData.get("duration")).intValue();
                            int amplifier = ((Number) effectData.get("amplifier")).intValue();
                            Object ambientObj = effectData.get("ambient");
                            Object particlesObj = effectData.get("particles");
                            boolean ambient = ambientObj instanceof Boolean ? (Boolean) ambientObj : false;
                            boolean particles = particlesObj instanceof Boolean ? (Boolean) particlesObj : true;
                            player.addPotionEffect(new PotionEffect(type, duration, amplifier, ambient, particles));
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load inventory for " + player.getName() + ": " + e.getMessage());
                clearPlayerData(player);
            }
        });
    }

    private void clearPlayerData(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().setItemInOffHand(null);
            player.getEnderChest().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(5.0f);
            player.setExp(0);
            player.setLevel(0);
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
        });
    }

    private File getPlayerFile(UUID playerId, String group) {
        File groupFolder = new File(inventoriesFolder, group);
        if (!groupFolder.exists()) {
            groupFolder.mkdirs();
        }
        return new File(groupFolder, playerId.toString() + ".yml");
    }
}