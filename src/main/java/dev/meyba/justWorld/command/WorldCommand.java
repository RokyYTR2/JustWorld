package dev.meyba.justWorld.command;

import dev.meyba.justWorld.JustWorld;
import dev.meyba.justWorld.managers.ConfirmationManager;
import dev.meyba.justWorld.managers.PortalManager;
import dev.meyba.justWorld.other.WorldData;
import dev.meyba.justWorld.utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class WorldCommand implements CommandExecutor, TabCompleter {
    private final JustWorld plugin;
    private final ChatUtil msg;
    private final Map<String, BukkitTask> pregenTasks;

    public WorldCommand(JustWorld plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageUtil();
        this.pregenTasks = new ConcurrentHashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "delete", "remove" -> handleDelete(sender, args);
            case "load" -> handleLoad(sender, args);
            case "unload" -> handleUnload(sender, args);
            case "tp", "teleport" -> handleTeleport(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            case "help" -> sendHelp(sender);
            case "gui" -> handleGUI(sender);
            case "setspawn" -> handleSetSpawn(sender, args);
            case "clone" -> handleClone(sender, args);
            case "import" -> handleImport(sender, args);
            case "rename" -> handleRename(sender, args);
            case "confirm" -> handleConfirm(sender);
            case "cancel" -> handleCancel(sender);
            case "pregen", "pregenerate" -> handlePregen(sender, args);
            case "portal" -> handlePortal(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg.send(sender, "usage-create");
            msg.send(sender, "generator-hint");
            return;
        }

        String worldName = args[1];
        World.Environment environment = World.Environment.NORMAL;
        WorldData.GeneratorType generatorType = WorldData.GeneratorType.DEFAULT;
        long seed = 0;

        if (args.length >= 3) {
            String type = args[2].toUpperCase();

            if (type.equals("VOID") || type.equals("FLAT")) {
                try {
                    generatorType = WorldData.GeneratorType.valueOf(type);
                } catch (IllegalArgumentException e) {
                    msg.send(sender, "invalid-environment");
                    return;
                }
            } else {
                try {
                    environment = switch (type) {
                        case "NORMAL" -> World.Environment.NORMAL;
                        case "NETHER" -> World.Environment.NETHER;
                        case "END" -> World.Environment.THE_END;
                        default -> throw new IllegalArgumentException("Invalid environment");
                    };
                } catch (IllegalArgumentException e) {
                    msg.send(sender, "invalid-environment");
                    return;
                }
            }
        }

        if (args.length >= 4) {
            try {
                seed = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                msg.send(sender, "invalid-seed");
                return;
            }
        }

        String messageKey = switch (generatorType) {
            case VOID -> "creating-void";
            case FLAT -> "creating-flat";
            default -> "creating-world";
        };
        msg.send(sender, messageKey, "{world}", worldName);

        WorldData worldData = WorldData.builder(worldName)
                .environment(environment)
                .seed(seed)
                .generatorType(generatorType)
                .build();

        plugin.getWorldManager().createWorld(worldData).thenAccept(result -> {
            if (result.isSuccess()) {
                String message = msg.getMessage("world-created")
                        .replace("{world}", worldName)
                        .replace("{time}", result.getFormattedTime());
                sender.sendMessage(msg.getPrefix() + message);
            } else {
                msg.send(sender, "world-create-failed");
            }
        });
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg.send(sender, "usage-delete");
            return;
        }

        String worldName = args[1];
        World world = plugin.getWorldManager().getWorld(worldName);

        if (world == null) {
            msg.send(sender, "world-not-found", "{world}", worldName);
            return;
        }

        boolean forceConfirm = args.length >= 3 && args[2].equalsIgnoreCase("confirm");

        if (forceConfirm) {
            executeDelete(sender, worldName);
            return;
        }

        plugin.getConfirmationManager().requestConfirmation(
            sender,
            ConfirmationManager.ConfirmationType.DELETE,
            worldName,
            s -> executeDelete(s, worldName)
        );
    }

    private void executeDelete(CommandSender sender, String worldName) {
        msg.send(sender, "deleting-world", "{world}", worldName);

        plugin.getWorldManager().deleteWorld(worldName).thenAccept(success -> {
            if (success) {
                msg.send(sender, "world-deleted", "{world}", worldName);
            } else {
                msg.send(sender, "world-delete-failed");
            }
        });
    }

    private void handleLoad(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg.send(sender, "usage-load");
            return;
        }

        String worldName = args[1];
        msg.send(sender, "loading-world", "{world}", worldName);

        plugin.getWorldManager().loadWorld(worldName).thenAccept(world -> {
            if (world != null) {
                msg.send(sender, "world-loaded", "{world}", worldName);
            } else {
                msg.send(sender, "world-load-failed", "{world}", worldName);
            }
        });
    }

    private void handleUnload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg.send(sender, "usage-unload");
            return;
        }

        String worldName = args[1];
        msg.send(sender, "unloading-world", "{world}", worldName);

        plugin.getWorldManager().unloadWorld(worldName).thenAccept(success -> {
            if (success) {
                msg.send(sender, "world-unloaded", "{world}", worldName);
            } else {
                msg.send(sender, "world-unload-failed");
            }
        });
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "player-only");
            return;
        }

        if (args.length < 2) {
            msg.send(sender, "usage-tp");
            return;
        }

        String worldName = args[1];

        World world = plugin.getWorldManager().getWorld(worldName);

        if (world != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.teleport(world.getSpawnLocation());
                msg.send(player, "teleported", "{world}", worldName);
            });
            return;
        }

        msg.send(player, "loading-world", "{world}", worldName);

        plugin.getWorldManager().loadWorld(worldName).thenAccept(loadedWorld -> {
            if (loadedWorld == null) {
                msg.send(player, "world-not-found", "{world}", worldName);
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.teleport(loadedWorld.getSpawnLocation());
                msg.send(player, "teleported", "{world}", worldName);
            });
        });
    }

    private void handleList(CommandSender sender) {
        List<World> worlds = plugin.getWorldManager().getAllWorlds();

        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ʟᴏᴀᴅᴇᴅ ᴡᴏʀʟᴅꜱ (" + worlds.size() + "):");
        worlds.forEach(world -> {
            sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "- " + world.getName() +
                    " (" + world.getEnvironment() + ", " +
                    world.getPlayers().size() + " ᴘʟᴀʏᴇʀꜱ)");
        });
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg.send(sender, "usage-info");
            return;
        }

        String worldName = args[1];
        World world = plugin.getWorldManager().getWorld(worldName);

        if (world == null) {
            msg.send(sender, "world-not-loaded", "{world}", worldName);
            return;
        }

        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ᴡᴏʀʟᴅ ɪɴꜰᴏ: " + worldName);
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ᴇɴᴠɪʀᴏɴᴍᴇɴᴛ: " + world.getEnvironment());
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ꜱᴇᴇᴅ: " + world.getSeed());
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ᴘʟᴀʏᴇʀꜱ: " + world.getPlayers().size());
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ᴘᴠᴘ: " + world.getPVP());
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ᴅɪꜰꜰɪᴄᴜʟᴛʏ: " + world.getDifficulty());
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ꜱᴘᴀᴡɴ: " +
                world.getSpawnLocation().getBlockX() + ", " +
                world.getSpawnLocation().getBlockY() + ", " +
                world.getSpawnLocation().getBlockZ());
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ᴋᴇᴇᴘ ꜱᴘᴀᴡɴ ʟᴏᴀᴅᴇᴅ: " + world.getKeepSpawnInMemory());
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        msg.loadMessages();
        msg.send(sender, "config-reloaded");
    }

    private void handleGUI(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "player-only");
            return;
        }
        plugin.getWorldGUI().open(player);
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg.send(sender, "player-only");
            return;
        }

        World world;
        if (args.length >= 2) {
            world = plugin.getWorldManager().getWorld(args[1]);
            if (world == null) {
                msg.send(sender, "world-not-found", "{world}", args[1]);
                return;
            }
        } else {
            world = player.getWorld();
        }

        Location loc = player.getLocation();
        world.setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        msg.send(sender, "spawn-set", "{world}", world.getName());
    }

    private void handleClone(CommandSender sender, String[] args) {
        if (args.length < 3) {
            msg.send(sender, "usage-clone");
            return;
        }

        String sourceName = args[1];
        String targetName = args[2];

        World sourceWorld = plugin.getWorldManager().getWorld(sourceName);
        if (sourceWorld == null) {
            msg.send(sender, "world-not-found", "{world}", sourceName);
            return;
        }

        String cloningMsg = msg.getMessage("cloning-world")
                .replace("{source}", sourceName)
                .replace("{target}", targetName);
        sender.sendMessage(msg.getPrefix() + cloningMsg);

        plugin.getWorldManager().cloneWorld(sourceName, targetName).thenAccept(success -> {
            if (success) {
                String clonedMsg = msg.getMessage("world-cloned")
                        .replace("{source}", sourceName)
                        .replace("{target}", targetName);
                sender.sendMessage(msg.getPrefix() + clonedMsg);
            } else {
                msg.send(sender, "world-clone-failed");
            }
        });
    }

    private void handleImport(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg.send(sender, "usage-import");
            return;
        }

        String worldName = args[1];
        msg.send(sender, "importing-world", "{world}", worldName);

        plugin.getWorldManager().importWorld(worldName).thenAccept(success -> {
            if (success) {
                msg.send(sender, "world-imported", "{world}", worldName);
            } else {
                msg.send(sender, "world-import-failed", "{world}", worldName);
            }
        });
    }

    private void handleRename(CommandSender sender, String[] args) {
        if (args.length < 3) {
            msg.send(sender, "usage-rename");
            return;
        }

        String oldName = args[1];
        String newName = args[2];

        World world = plugin.getWorldManager().getWorld(oldName);
        if (world == null) {
            msg.send(sender, "world-not-found", "{world}", oldName);
            return;
        }

        boolean forceConfirm = args.length >= 4 && args[3].equalsIgnoreCase("confirm");

        if (forceConfirm) {
            executeRename(sender, oldName, newName);
            return;
        }

        plugin.getConfirmationManager().requestConfirmation(
            sender,
            ConfirmationManager.ConfirmationType.RENAME,
            oldName + " -> " + newName,
            s -> executeRename(s, oldName, newName)
        );
    }

    private void executeRename(CommandSender sender, String oldName, String newName) {
        String renamingMsg = msg.getMessage("renaming-world")
                .replace("{old}", oldName)
                .replace("{new}", newName);
        sender.sendMessage(msg.getPrefix() + renamingMsg);

        plugin.getWorldManager().renameWorld(oldName, newName).thenAccept(success -> {
            if (success) {
                String renamedMsg = msg.getMessage("world-renamed")
                        .replace("{old}", oldName)
                        .replace("{new}", newName);
                sender.sendMessage(msg.getPrefix() + renamedMsg);
            } else {
                msg.send(sender, "world-rename-failed");
            }
        });
    }

    private void handleConfirm(CommandSender sender) {
        plugin.getConfirmationManager().confirm(sender);
    }

    private void handleCancel(CommandSender sender) {
        plugin.getConfirmationManager().cancel(sender);
    }

    private void handlePregen(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg.send(sender, "usage-pregen");
            return;
        }

        String subCommand = args[1].toLowerCase();

        if (subCommand.equals("stop") || subCommand.equals("cancel")) {
            if (args.length < 3) {
                msg.send(sender, "usage-pregen-stop");
                return;
            }
            String worldName = args[2];
            BukkitTask task = pregenTasks.remove(worldName);
            if (task != null) {
                task.cancel();
                msg.send(sender, "pregen-stopped", "{world}", worldName);
            } else {
                msg.send(sender, "pregen-not-running", "{world}", worldName);
            }
            return;
        }

        if (subCommand.equals("status")) {
            if (pregenTasks.isEmpty()) {
                msg.send(sender, "pregen-no-tasks");
            } else {
                sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ᴀᴄᴛɪᴠᴇ ᴘʀᴇɢᴇɴᴇʀᴀᴛɪᴏɴꜱ:");
                pregenTasks.keySet().forEach(world ->
                    sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "- " + world));
            }
            return;
        }

        String worldName = args[1];
        int radius;

        if (args.length < 3) {
            msg.send(sender, "usage-pregen");
            return;
        }

        try {
            radius = Integer.parseInt(args[2]);
            if (radius < 1 || radius > 500) {
                msg.send(sender, "pregen-invalid-radius");
                return;
            }
        } catch (NumberFormatException e) {
            msg.send(sender, "pregen-invalid-radius");
            return;
        }

        World world = plugin.getWorldManager().getWorld(worldName);
        if (world == null) {
            msg.send(sender, "world-not-found", "{world}", worldName);
            return;
        }

        if (pregenTasks.containsKey(worldName)) {
            msg.send(sender, "pregen-already-running", "{world}", worldName);
            return;
        }

        startPregeneration(sender, world, radius);
    }

    private void startPregeneration(CommandSender sender, World world, int radius) {
        String worldName = world.getName();
        int totalChunks = (radius * 2 + 1) * (radius * 2 + 1);
        AtomicInteger generated = new AtomicInteger(0);
        AtomicInteger currentX = new AtomicInteger(-radius);
        AtomicInteger currentZ = new AtomicInteger(-radius);
        AtomicInteger lastProgress = new AtomicInteger(-1);

        Location center = world.getSpawnLocation();
        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;

        int chunksPerTick = plugin.getConfig().getInt("performance.pregen-chunks-per-tick", 4);

        msg.send(sender, "pregen-started", "{world}", worldName, "{chunks}", String.valueOf(totalChunks));

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (int i = 0; i < chunksPerTick && currentX.get() <= radius; i++) {
                int chunkX = centerChunkX + currentX.get();
                int chunkZ = centerChunkZ + currentZ.get();

                currentZ.incrementAndGet();
                if (currentZ.get() > radius) {
                    currentZ.set(-radius);
                    currentX.incrementAndGet();
                }

                if (!world.isChunkGenerated(chunkX, chunkZ)) {
                    world.getChunkAt(chunkX, chunkZ);
                }
                generated.incrementAndGet();
            }

            int progress = (generated.get() * 100) / totalChunks;
            if (progress / 10 > lastProgress.get()) {
                lastProgress.set(progress / 10);
                sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ᴘʀᴇɢᴇɴ: " + progress + "% (" + generated.get() + "/" + totalChunks + ")");
            }

            if (currentX.get() > radius) {
                pregenTasks.remove(worldName).cancel();
                msg.send(sender, "pregen-complete", "{world}", worldName, "{chunks}", String.valueOf(totalChunks));
            }
        }, 1L, 2L);

        pregenTasks.put(worldName, task);
    }

    private void handlePortal(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg.send(sender, "usage-portal");
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "link" -> handlePortalLink(sender, args);
            case "unlink" -> handlePortalUnlink(sender, args);
            case "list" -> handlePortalList(sender);
            default -> msg.send(sender, "usage-portal");
        }
    }

    private void handlePortalLink(CommandSender sender, String[] args) {
        if (args.length < 5) {
            msg.send(sender, "usage-portal-link");
            return;
        }

        String portalType = args[2].toLowerCase();
        String fromWorld = args[3];
        String toWorld = args[4];

        if (!portalType.equals("nether") && !portalType.equals("end")) {
            msg.send(sender, "portal-invalid-type");
            return;
        }

        PortalManager portalManager = plugin.getPortalManager();

        if (portalType.equals("nether")) {
            portalManager.setNetherLink(fromWorld, toWorld);
        } else {
            portalManager.setEndLink(fromWorld, toWorld);
        }

        String linkedMsg = msg.getMessage("portal-linked")
                .replace("{type}", portalType.toUpperCase())
                .replace("{from}", fromWorld)
                .replace("{to}", toWorld);
        sender.sendMessage(msg.getPrefix() + linkedMsg);
    }

    private void handlePortalUnlink(CommandSender sender, String[] args) {
        if (args.length < 4) {
            msg.send(sender, "usage-portal-unlink");
            return;
        }

        String portalType = args[2].toLowerCase();
        String fromWorld = args[3];

        if (!portalType.equals("nether") && !portalType.equals("end")) {
            msg.send(sender, "portal-invalid-type");
            return;
        }

        PortalManager portalManager = plugin.getPortalManager();

        if (portalType.equals("nether")) {
            portalManager.removeNetherLink(fromWorld);
        } else {
            portalManager.removeEndLink(fromWorld);
        }

        msg.send(sender, "portal-unlinked", "{type}", portalType.toUpperCase(), "{world}", fromWorld);
    }

    private void handlePortalList(CommandSender sender) {
        PortalManager portalManager = plugin.getPortalManager();

        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ᴘᴏʀᴛᴀʟ ʟɪɴᴋꜱ:");

        Map<String, PortalManager.PortalLink> netherLinks = portalManager.getAllNetherLinks();
        Map<String, PortalManager.PortalLink> endLinks = portalManager.getAllEndLinks();

        if (netherLinks.isEmpty() && endLinks.isEmpty()) {
            sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ɴᴏ ᴘᴏʀᴛᴀʟ ʟɪɴᴋꜱ ᴄᴏɴꜰɪɢᴜʀᴇᴅ.");
            return;
        }

        if (!netherLinks.isEmpty()) {
            sender.sendMessage(msg.getPrefix() + ChatColor.GOLD + "ɴᴇᴛʜᴇʀ:");
            netherLinks.forEach((from, link) ->
                sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "  " + from + " -> " + link.targetWorld()));
        }

        if (!endLinks.isEmpty()) {
            sender.sendMessage(msg.getPrefix() + ChatColor.DARK_PURPLE + "ᴇɴᴅ:");
            endLinks.forEach((from, link) ->
                sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "  " + from + " -> " + link.targetWorld()));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ʜᴇʟᴘ ᴍᴇɴᴜ:");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world create <ɴᴀᴍᴇ> [ᴛʏᴘᴇ] [ꜱᴇᴇᴅ] - ᴄʀᴇᴀᴛᴇꜱ ᴀ ɴᴇᴡ ᴡᴏʀʟᴅ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world delete <ɴᴀᴍᴇ> - ᴅᴇʟᴇᴛᴇꜱ ᴀ ᴡᴏʀʟᴅ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world clone <ꜱᴏᴜʀᴄᴇ> <ɴᴇᴡɴᴀᴍᴇ> - ᴄʟᴏɴᴇꜱ ᴀ ᴡᴏʀʟᴅ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world rename <ᴏʟᴅ> <ɴᴇᴡ> - ʀᴇɴᴀᴍᴇꜱ ᴀ ᴡᴏʀʟᴅ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world import <ɴᴀᴍᴇ> - ɪᴍᴘᴏʀᴛꜱ ᴀ ᴡᴏʀʟᴅ ꜰʀᴏᴍ ꜰᴏʟᴅᴇʀ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world load <ɴᴀᴍᴇ> - ʟᴏᴀᴅꜱ ᴀ ᴡᴏʀʟᴅ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world unload <ɴᴀᴍᴇ> - ᴜɴʟᴏᴀᴅꜱ ᴀ ᴡᴏʀʟᴅ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world tp <ɴᴀᴍᴇ> - ᴛᴇʟᴇᴘᴏʀᴛꜱ ᴛᴏ ᴀ ᴡᴏʀʟᴅ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world setspawn [ᴡᴏʀʟᴅ] - ꜱᴇᴛꜱ ᴡᴏʀʟᴅ ꜱᴘᴀᴡɴ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world list - ʟɪꜱᴛꜱ ᴀʟʟ ᴡᴏʀʟᴅꜱ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world gui - ᴏᴘᴇɴꜱ ᴡᴏʀʟᴅ ɢᴜɪ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world info <ɴᴀᴍᴇ> - ᴠɪᴇᴡꜱ ᴡᴏʀʟᴅ ɪɴꜰᴏ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world pregen <ᴡᴏʀʟᴅ> <ʀᴀᴅɪᴜꜱ> - ᴘʀᴇɢᴇɴᴇʀᴀᴛᴇꜱ ᴄʜᴜɴᴋꜱ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world portal <ʟɪɴᴋ|ᴜɴʟɪɴᴋ|ʟɪꜱᴛ> - ᴍᴀɴᴀɢᴇꜱ ᴘᴏʀᴛᴀʟꜱ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world confirm/cancel - ᴄᴏɴꜰɪʀᴍꜱ/ᴄᴀɴᴄᴇʟꜱ ᴀᴄᴛɪᴏɴ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world reload - ʀᴇʟᴏᴀᴅꜱ ᴛʜᴇ ᴄᴏɴꜰɪɢ.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "delete", "clone", "rename", "import", "load", "unload", "tp", "setspawn", "list", "gui", "info", "pregen", "pregenerate", "portal", "confirm", "cancel", "reload", "help"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "delete", "load", "unload", "tp", "info", "setspawn", "clone", "rename" ->
                        completions.addAll(plugin.getWorldManager().getAllWorlds().stream()
                                .map(World::getName)
                                .toList());
                case "import" ->
                        completions.addAll(plugin.getWorldManager().getUnloadedWorlds());
                case "pregen", "pregenerate" -> {
                        completions.addAll(plugin.getWorldManager().getAllWorlds().stream()
                                .map(World::getName)
                                .toList());
                        completions.addAll(Arrays.asList("stop", "status"));
                }
                case "portal" ->
                        completions.addAll(Arrays.asList("link", "unlink", "list"));
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "create" -> completions.addAll(Arrays.asList("normal", "nether", "end", "void", "flat"));
                case "pregen", "pregenerate" -> {
                    if (args[1].equalsIgnoreCase("stop")) {
                        completions.addAll(pregenTasks.keySet());
                    } else {
                        completions.addAll(Arrays.asList("5", "10", "20", "50", "100"));
                    }
                }
                case "portal" -> {
                    if (args[1].equalsIgnoreCase("link") || args[1].equalsIgnoreCase("unlink")) {
                        completions.addAll(Arrays.asList("nether", "end"));
                    }
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("portal")) {
            if (args[1].equalsIgnoreCase("link") || args[1].equalsIgnoreCase("unlink")) {
                completions.addAll(plugin.getWorldManager().getAllWorlds().stream()
                        .map(World::getName)
                        .toList());
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("portal") && args[1].equalsIgnoreCase("link")) {
            completions.addAll(plugin.getWorldManager().getAllWorlds().stream()
                    .map(World::getName)
                    .toList());
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}