package dev.meyba.justWorld.managers;

import dev.meyba.justWorld.JustWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PortalManager implements Listener {
    private final JustWorld plugin;
    private final File portalsFile;
    private final Map<String, PortalLink> netherLinks;
    private final Map<String, PortalLink> endLinks;
    private final boolean enabled;

    public PortalManager(JustWorld plugin) {
        this.plugin = plugin;
        this.portalsFile = new File(plugin.getDataFolder(), "portals.yml");
        this.netherLinks = new HashMap<>();
        this.endLinks = new HashMap<>();
        this.enabled = plugin.getConfig().getBoolean("portals.enabled", true);
        loadPortalLinks();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        World fromWorld = event.getFrom().getWorld();
        if (fromWorld == null) return;

        String fromWorldName = fromWorld.getName();

        final PortalLink link;
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            link = netherLinks.get(fromWorldName);
        } else if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            link = endLinks.get(fromWorldName);
        } else {
            link = null;
        }

        if (link == null) return;

        String targetWorldName = link.targetWorld();
        World targetWorld = Bukkit.getWorld(targetWorldName);

        if (targetWorld == null) {
            plugin.getWorldManager().loadWorld(targetWorldName).thenAccept(loaded -> {
                if (loaded != null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        teleportToWorld(player, loaded, event.getCause());
                    });
                } else {
                    plugin.getMessageUtil().send(player, "portal-world-not-found", "{world}", targetWorldName);
                }
            });
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        teleportToWorld(player, targetWorld, event.getCause());
    }

    private void teleportToWorld(Player player, World targetWorld, PlayerTeleportEvent.TeleportCause cause) {
        Location from = player.getLocation();
        Location target;

        if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            double ratio;
            if (from.getWorld() != null && from.getWorld().getEnvironment() == World.Environment.NETHER) {
                ratio = 8.0;
            } else if (targetWorld.getEnvironment() == World.Environment.NETHER) {
                ratio = 0.125;
            } else {
                ratio = 1.0;
            }

            double x = from.getX() * ratio;
            double z = from.getZ() * ratio;
            double y = Math.min(Math.max(from.getY(), targetWorld.getMinHeight() + 1), targetWorld.getMaxHeight() - 1);

            target = new Location(targetWorld, x, y, z, from.getYaw(), from.getPitch());
            target = findSafeLocation(target);
        } else {
            target = targetWorld.getSpawnLocation();
        }

        player.teleport(target);
        plugin.getMessageUtil().send(player, "portal-teleported", "{world}", targetWorld.getName());
    }

    private Location findSafeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) return location;

        int x = location.getBlockX();
        int z = location.getBlockZ();

        int y;
        if (world.getEnvironment() == World.Environment.NETHER) {
            y = 32;
            for (int checkY = 32; checkY < 120; checkY++) {
                if (world.getBlockAt(x, checkY, z).getType().isAir() &&
                    world.getBlockAt(x, checkY + 1, z).getType().isAir() &&
                    !world.getBlockAt(x, checkY - 1, z).getType().isAir()) {
                    y = checkY;
                    break;
                }
            }
        } else {
            y = world.getHighestBlockYAt(x, z) + 1;
        }

        return new Location(world, x + 0.5, y, z + 0.5, location.getYaw(), location.getPitch());
    }

    public void setNetherLink(String fromWorld, String toWorld) {
        netherLinks.put(fromWorld, new PortalLink(toWorld, PortalType.NETHER));
        savePortalLinksAsync();
    }

    public void setEndLink(String fromWorld, String toWorld) {
        endLinks.put(fromWorld, new PortalLink(toWorld, PortalType.ENDER));
        savePortalLinksAsync();
    }

    public void removeNetherLink(String fromWorld) {
        netherLinks.remove(fromWorld);
        savePortalLinksAsync();
    }

    public void removeEndLink(String fromWorld) {
        endLinks.remove(fromWorld);
        savePortalLinksAsync();
    }

    public Map<String, PortalLink> getAllNetherLinks() {
        return new HashMap<>(netherLinks);
    }

    public Map<String, PortalLink> getAllEndLinks() {
        return new HashMap<>(endLinks);
    }

    private void loadPortalLinks() {
        if (!portalsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                portalsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Cannot create portals.yml: " + e.getMessage());
                return;
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(portalsFile);

        ConfigurationSection netherSection = config.getConfigurationSection("nether");
        if (netherSection != null) {
            netherSection.getKeys(false).forEach(fromWorld -> {
                String toWorld = netherSection.getString(fromWorld);
                if (toWorld != null) {
                    netherLinks.put(fromWorld, new PortalLink(toWorld, PortalType.NETHER));
                }
            });
        }

        ConfigurationSection endSection = config.getConfigurationSection("end");
        if (endSection != null) {
            endSection.getKeys(false).forEach(fromWorld -> {
                String toWorld = endSection.getString(fromWorld);
                if (toWorld != null) {
                    endLinks.put(fromWorld, new PortalLink(toWorld, PortalType.ENDER));
                }
            });
        }

        plugin.getLogger().info("Loaded " + netherLinks.size() + " nether links and " + endLinks.size() + " end links");
    }

    private CompletableFuture<Void> savePortalLinksAsync() {
        return CompletableFuture.runAsync(() -> {
            YamlConfiguration config = new YamlConfiguration();

            netherLinks.forEach((from, link) -> config.set("nether." + from, link.targetWorld()));
            endLinks.forEach((from, link) -> config.set("end." + from, link.targetWorld()));

            try {
                config.save(portalsFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Error saving portals.yml: " + e.getMessage());
            }
        });
    }

    public record PortalLink(String targetWorld, PortalType type) {}
}