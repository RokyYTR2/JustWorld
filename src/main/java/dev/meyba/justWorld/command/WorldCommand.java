package dev.meyba.justWorld.command;

import dev.meyba.justWorld.JustWorld;
import dev.meyba.justWorld.utils.MessageUtil;
import dev.meyba.justWorld.world.WorldData;
import org.bukkit.ChatColor;
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
    private final MessageUtil msg;

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

        // Send appropriate creation message based on generator type
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

        plugin.getWorldManager().createWorldAsync(worldData).thenAccept(result -> {
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

        plugin.getWorldManager().deleteWorldAsync(worldName).thenAccept(success -> {
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

        plugin.getWorldManager().loadWorldAsync(worldName).thenAccept(world -> {
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

        plugin.getWorldManager().unloadWorldAsync(worldName).thenAccept(success -> {
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

        // OPTIMIZATION: Check if world is already loaded (no async needed)
        World world = plugin.getWorldManager().getWorld(worldName);

        if (world != null) {
            // World already loaded - instant teleport!
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.teleport(world.getSpawnLocation());
                msg.send(player, "teleported", "{world}", worldName);
            });
            return;
        }

        // World not loaded - load it asynchronously
        msg.send(player, "loading-world", "{world}", worldName);

        plugin.getWorldManager().loadWorldAsync(worldName).thenAccept(loadedWorld -> {
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

        sender.sendMessage(msg.getPrefix() + ChatColor.GOLD + "ʟᴏᴀᴅᴇᴅ ᴡᴏʀʟᴅꜱ (" + worlds.size() + ")");
        worlds.forEach(world -> {
            sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "- " + world.getName() +
                    ChatColor.GRAY + " (" + world.getEnvironment() + ", " +
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

        sender.sendMessage(msg.getPrefix() + ChatColor.GOLD + "ᴡᴏʀʟᴅ ɪɴꜰᴏ: " + ChatColor.WHITE + worldName);
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "ᴇɴᴠɪʀᴏɴᴍᴇɴᴛ: " + ChatColor.WHITE + world.getEnvironment());
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "ꜱᴇᴇᴅ: " + ChatColor.WHITE + world.getSeed());
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "ᴘʟᴀʏᴇʀꜱ: " + ChatColor.WHITE + world.getPlayers().size());
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "ᴘᴠᴘ: " + ChatColor.WHITE + world.getPVP());
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "ᴅɪꜰꜰɪᴄᴜʟᴛʏ: " + ChatColor.WHITE + world.getDifficulty());
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "ꜱᴘᴀᴡɴ: " + ChatColor.WHITE +
                world.getSpawnLocation().getBlockX() + ", " +
                world.getSpawnLocation().getBlockY() + ", " +
                world.getSpawnLocation().getBlockZ());
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "ᴋᴇᴇᴘ ꜱᴘᴀᴡɴ ʟᴏᴀᴅᴇᴅ: " + ChatColor.WHITE + world.getKeepSpawnInMemory());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg.getPrefix() + ChatColor.GOLD + ChatColor.BOLD + "ᴊᴜꜱᴛᴡᴏʀʟᴅ ᴄᴏᴍᴍᴀɴᴅꜱ");
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "/world create <name> [type] [seed]" + ChatColor.GRAY + " - ᴄʀᴇᴀᴛᴇ ᴀ ɴᴇᴡ ᴡᴏʀʟᴅ");
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "/world delete <name>" + ChatColor.GRAY + " - ᴅᴇʟᴇᴛᴇ ᴀ ᴡᴏʀʟᴅ");
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "/world load <name>" + ChatColor.GRAY + " - ʟᴏᴀᴅ ᴀ ᴡᴏʀʟᴅ");
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "/world unload <name>" + ChatColor.GRAY + " - ᴜɴʟᴏᴀᴅ ᴀ ᴡᴏʀʟᴅ");
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "/world tp <name>" + ChatColor.GRAY + " - ᴛᴇʟᴇᴘᴏʀᴛ ᴛᴏ ᴀ ᴡᴏʀʟᴅ");
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "/world list" + ChatColor.GRAY + " - ʟɪꜱᴛ ᴀʟʟ ᴡᴏʀʟᴅꜱ");
        sender.sendMessage(msg.getPrefix() + ChatColor.YELLOW + "/world info <name>" + ChatColor.GRAY + " - ᴠɪᴇᴡ ᴡᴏʀʟᴅ ɪɴꜰᴏ");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "delete", "load", "unload", "tp", "list", "info"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "delete", "load", "unload", "tp", "info" ->
                        completions.addAll(plugin.getWorldManager().getAllWorlds().stream()
                                .map(World::getName)
                                .toList());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            completions.addAll(Arrays.asList("normal", "nether", "end", "void", "flat"));
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
