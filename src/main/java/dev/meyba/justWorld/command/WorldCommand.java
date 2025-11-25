package dev.meyba.justWorld.command;

import dev.meyba.justWorld.JustWorld;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WorldCommand implements CommandExecutor, TabCompleter {
    private final JustWorld plugin;
    private final ChatUtil msg;

    public WorldCommand(JustWorld plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageUtil();
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

        if (world.getPlayers().size() > 0 && args.length < 3) {
            msg.send(sender, "world-has-players", "{world}", worldName);
            return;
        }

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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "ʜᴇʟᴘ ᴍᴇɴᴜ:");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world create <ɴᴀᴍᴇ> [ᴛʏᴘᴇ] [ꜱᴇᴇᴅ] - ᴄʀᴇᴀᴛᴇꜱ ᴀ ɴᴇᴡ ᴡᴏʀʟᴅ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world delete <ɴᴀᴍᴇ> - ᴅᴇʟᴇᴛᴇꜱ ᴀ ᴡᴏʀʟᴅ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world clone <ꜱᴏᴜʀᴄᴇ> <ɴᴇᴡɴᴀᴍᴇ> - ᴄʟᴏɴᴇꜱ ᴀ ᴡᴏʀʟᴅ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world import <ɴᴀᴍᴇ> - ɪᴍᴘᴏʀᴛꜱ ᴀ ᴡᴏʀʟᴅ ꜰʀᴏᴍ ꜰᴏʟᴅᴇʀ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world load <ɴᴀᴍᴇ> - ʟᴏᴀᴅꜱ ᴀ ᴡᴏʀʟᴅ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world unload <ɴᴀᴍᴇ> - ᴜɴʟᴏᴀᴅꜱ ᴀ ᴡᴏʀʟᴅ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world tp <ɴᴀᴍᴇ> - ᴛᴇʟᴇᴘᴏʀᴛꜱ ᴛᴏ ᴀ ᴡᴏʀʟᴅ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world setspawn [ᴡᴏʀʟᴅ] - ꜱᴇᴛꜱ ᴡᴏʀʟᴅ ꜱᴘᴀᴡɴ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world list - ʟɪꜱᴛꜱ ᴀʟʟ ᴡᴏʀʟᴅꜱ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world gui - ᴏᴘᴇɴꜱ ᴡᴏʀʟᴅ ɢᴜɪ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world info <ɴᴀᴍᴇ> - ᴠɪᴇᴡꜱ ᴡᴏʀʟᴅ ɪɴꜰᴏ.");
        sender.sendMessage(msg.getPrefix() + ChatColor.GRAY + "/world reload - ʀᴇʟᴏᴀᴅꜱ ᴛʜᴇ ᴄᴏɴꜰɪɢ.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "delete", "clone", "import", "load", "unload", "tp", "setspawn", "list", "gui", "info", "reload", "help"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "delete", "load", "unload", "tp", "info", "setspawn", "clone" ->
                        completions.addAll(plugin.getWorldManager().getAllWorlds().stream()
                                .map(World::getName)
                                .toList());
                case "import" ->
                        completions.addAll(plugin.getWorldManager().getUnloadedWorlds());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            completions.addAll(Arrays.asList("normal", "nether", "end", "void", "flat"));
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}