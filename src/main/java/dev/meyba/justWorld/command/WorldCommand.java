package dev.meyba.justWorld.command;

import dev.meyba.justWorld.JustWorld;
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

    public WorldCommand(JustWorld plugin) {
        this.plugin = plugin;
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
            sender.sendMessage(ChatColor.RED + "Usage: /world create <name> [normal|nether|end|void|flat] [seed]");
            sender.sendMessage(ChatColor.GRAY + "Generator types: normal (default), void (fastest!), flat (fast)");
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
                    sender.sendMessage(ChatColor.RED + "Invalid generator type!");
                    return;
                }
            } else {
                try {
                    environment = World.Environment.valueOf(type);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid type! Use: normal, nether, end, void, flat");
                    return;
                }
            }
        }

        if (args.length >= 4) {
            try {
                seed = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid seed!");
                return;
            }
        }

        String typeInfo = generatorType == WorldData.GeneratorType.VOID ? " (VOID - Ultra Fast!)" :
                         generatorType == WorldData.GeneratorType.FLAT ? " (FLAT - Very Fast!)" : "";

        sender.sendMessage(ChatColor.YELLOW + "Creating world " + ChatColor.WHITE + worldName +
                          ChatColor.YELLOW + typeInfo + " asynchronously...");

        WorldData worldData = WorldData.builder(worldName)
                .environment(environment)
                .seed(seed)
                .generatorType(generatorType)
                .build();

        plugin.getWorldManager().createWorldAsync(worldData).thenAccept(result -> {
            if (result.isSuccess()) {
                sender.sendMessage(ChatColor.GREEN + "World " + ChatColor.WHITE + worldName +
                        ChatColor.GREEN + " created in " + ChatColor.GOLD + result.getFormattedTime() + ChatColor.GREEN + "!");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to create world!");
            }
        });
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /world delete <name>");
            return;
        }

        String worldName = args[1];
        World world = plugin.getWorldManager().getWorld(worldName);

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World " + worldName + " doesn't exist!");
            return;
        }

        if (world.getPlayers().size() > 0 && args.length < 3) {
            sender.sendMessage(ChatColor.RED + "There are players in this world! Use /world delete <name> confirm");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Deleting world " + ChatColor.WHITE + worldName + ChatColor.YELLOW + " asynchronously...");

        plugin.getWorldManager().deleteWorldAsync(worldName).thenAccept(success -> {
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "World " + ChatColor.WHITE + worldName + ChatColor.GREEN + " deleted successfully!");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to delete world!");
            }
        });
    }

    private void handleLoad(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /world load <name>");
            return;
        }

        String worldName = args[1];
        sender.sendMessage(ChatColor.YELLOW + "Loading world " + ChatColor.WHITE + worldName + ChatColor.YELLOW + " asynchronously...");

        plugin.getWorldManager().loadWorldAsync(worldName).thenAccept(world -> {
            if (world != null) {
                sender.sendMessage(ChatColor.GREEN + "World " + ChatColor.WHITE + worldName + ChatColor.GREEN + " loaded successfully!");
            } else {
                sender.sendMessage(ChatColor.RED + "World " + worldName + " doesn't exist or failed to load!");
            }
        });
    }

    private void handleUnload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /world unload <name>");
            return;
        }

        String worldName = args[1];
        sender.sendMessage(ChatColor.YELLOW + "Unloading world " + ChatColor.WHITE + worldName + ChatColor.YELLOW + " asynchronously...");

        plugin.getWorldManager().unloadWorldAsync(worldName).thenAccept(success -> {
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "World " + ChatColor.WHITE + worldName + ChatColor.GREEN + " unloaded successfully!");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to unload world!");
            }
        });
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /world tp <name>");
            return;
        }

        String worldName = args[1];

        World world = plugin.getWorldManager().getWorld(worldName);

        if (world != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.teleport(world.getSpawnLocation());
                player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.WHITE + worldName + ChatColor.GREEN + "!");
            });
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Loading world " + worldName + "...");

        plugin.getWorldManager().loadWorldAsync(worldName).thenAccept(loadedWorld -> {
            if (loadedWorld == null) {
                player.sendMessage(ChatColor.RED + "World " + worldName + " doesn't exist!");
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.teleport(loadedWorld.getSpawnLocation());
                player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.WHITE + worldName + ChatColor.GREEN + "!");
            });
        });
    }

    private void handleList(CommandSender sender) {
        List<World> worlds = plugin.getWorldManager().getAllWorlds();

        sender.sendMessage(ChatColor.GOLD + "=== Loaded Worlds (" + worlds.size() + ") ===");
        worlds.forEach(world -> {
            sender.sendMessage(ChatColor.YELLOW + "- " + world.getName() +
                    ChatColor.GRAY + " (" + world.getEnvironment() + ", " +
                    world.getPlayers().size() + " players)");
        });
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /world info <name>");
            return;
        }

        String worldName = args[1];
        World world = plugin.getWorldManager().getWorld(worldName);

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World " + worldName + " is not loaded!");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== World Info: " + worldName + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Environment: " + ChatColor.WHITE + world.getEnvironment());
        sender.sendMessage(ChatColor.YELLOW + "Seed: " + ChatColor.WHITE + world.getSeed());
        sender.sendMessage(ChatColor.YELLOW + "Players: " + ChatColor.WHITE + world.getPlayers().size());
        sender.sendMessage(ChatColor.YELLOW + "PVP: " + ChatColor.WHITE + world.getPVP());
        sender.sendMessage(ChatColor.YELLOW + "Difficulty: " + ChatColor.WHITE + world.getDifficulty());
        sender.sendMessage(ChatColor.YELLOW + "Spawn: " + ChatColor.WHITE +
                world.getSpawnLocation().getBlockX() + ", " +
                world.getSpawnLocation().getBlockY() + ", " +
                world.getSpawnLocation().getBlockZ());
        sender.sendMessage(ChatColor.YELLOW + "Keep Spawn Loaded: " + ChatColor.WHITE + world.getKeepSpawnInMemory());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== JustWorld Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/world create <name> [type] [seed]" + ChatColor.GRAY + " - Create a new world");
        sender.sendMessage(ChatColor.YELLOW + "/world delete <name>" + ChatColor.GRAY + " - Delete a world");
        sender.sendMessage(ChatColor.YELLOW + "/world load <name>" + ChatColor.GRAY + " - Load a world");
        sender.sendMessage(ChatColor.YELLOW + "/world unload <name>" + ChatColor.GRAY + " - Unload a world");
        sender.sendMessage(ChatColor.YELLOW + "/world tp <name>" + ChatColor.GRAY + " - Teleport to a world");
        sender.sendMessage(ChatColor.YELLOW + "/world list" + ChatColor.GRAY + " - List all worlds");
        sender.sendMessage(ChatColor.YELLOW + "/world info <name>" + ChatColor.GRAY + " - View world info");
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