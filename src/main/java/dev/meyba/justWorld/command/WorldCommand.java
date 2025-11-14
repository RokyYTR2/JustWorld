package dev.meyba.justWorld.command;

import dev.meyba.justWorld.JustWorld;
import dev.meyba.justWorld.world.WorldData;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldType;
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

        // Všechny operace běží asynchronně!
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
            sender.sendMessage(ChatColor.RED + "Použití: /world create <název> [normal|nether|end] [seed]");
            return;
        }

        String worldName = args[1];
        World.Environment environment = World.Environment.NORMAL;
        long seed = 0;

        if (args.length >= 3) {
            try {
                environment = World.Environment.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Neplatný environment! Použij: normal, nether, end");
                return;
            }
        }

        if (args.length >= 4) {
            try {
                seed = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Neplatný seed!");
                return;
            }
        }

        sender.sendMessage(ChatColor.YELLOW + "Vytvářím svět " + worldName + " asynchronně...");

        WorldData worldData = WorldData.builder(worldName)
                .environment(environment)
                .seed(seed)
                .build();

        plugin.getWorldManager().createWorldAsync(worldData).thenAccept(world -> {
            if (world != null) {
                sender.sendMessage(ChatColor.GREEN + "Svět " + worldName + " byl úspěšně vytvořen!");
            } else {
                sender.sendMessage(ChatColor.RED + "Chyba při vytváření světa!");
            }
        });
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Použití: /world delete <název>");
            return;
        }

        String worldName = args[1];
        World world = plugin.getWorldManager().getWorld(worldName);

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "Svět " + worldName + " neexistuje!");
            return;
        }

        if (world.getPlayers().size() > 0 && args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Ve světě jsou hráči! Použij /world delete <název> confirm");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Mažu svět " + worldName + " asynchronně...");

        plugin.getWorldManager().deleteWorldAsync(worldName).thenAccept(success -> {
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Svět " + worldName + " byl úspěšně smazán!");
            } else {
                sender.sendMessage(ChatColor.RED + "Chyba při mazání světa!");
            }
        });
    }

    private void handleLoad(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Použití: /world load <název>");
            return;
        }

        String worldName = args[1];
        sender.sendMessage(ChatColor.YELLOW + "Načítám svět " + worldName + " asynchronně...");

        plugin.getWorldManager().loadWorldAsync(worldName).thenAccept(world -> {
            if (world != null) {
                sender.sendMessage(ChatColor.GREEN + "Svět " + worldName + " byl úspěšně načten!");
            } else {
                sender.sendMessage(ChatColor.RED + "Svět " + worldName + " neexistuje nebo se nepodařilo načíst!");
            }
        });
    }

    private void handleUnload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Použití: /world unload <název>");
            return;
        }

        String worldName = args[1];
        sender.sendMessage(ChatColor.YELLOW + "Odebírám svět " + worldName + " asynchronně...");

        plugin.getWorldManager().unloadWorldAsync(worldName).thenAccept(success -> {
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Svět " + worldName + " byl úspěšně odebrán!");
            } else {
                sender.sendMessage(ChatColor.RED + "Chyba při odebírání světa!");
            }
        });
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Tento příkaz mohou použít pouze hráči!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Použití: /world tp <název>");
            return;
        }

        String worldName = args[1];

        plugin.getWorldManager().loadWorldAsync(worldName).thenAccept(world -> {
            if (world == null) {
                player.sendMessage(ChatColor.RED + "Svět " + worldName + " neexistuje!");
                return;
            }

            // Teleport musí být na main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.teleport(world.getSpawnLocation());
                player.sendMessage(ChatColor.GREEN + "Byl jsi teleportován do světa " + worldName + "!");
            });
        });
    }

    private void handleList(CommandSender sender) {
        List<World> worlds = plugin.getWorldManager().getAllWorlds();

        sender.sendMessage(ChatColor.GOLD + "=== Načtené světy (" + worlds.size() + ") ===");
        worlds.forEach(world -> {
            sender.sendMessage(ChatColor.YELLOW + "- " + world.getName() +
                    ChatColor.GRAY + " (" + world.getEnvironment() + ", " +
                    world.getPlayers().size() + " hráčů)");
        });
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Použití: /world info <název>");
            return;
        }

        String worldName = args[1];
        World world = plugin.getWorldManager().getWorld(worldName);

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "Svět " + worldName + " není načten!");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Info o světě " + worldName + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Environment: " + ChatColor.WHITE + world.getEnvironment());
        sender.sendMessage(ChatColor.YELLOW + "Seed: " + ChatColor.WHITE + world.getSeed());
        sender.sendMessage(ChatColor.YELLOW + "Hráči: " + ChatColor.WHITE + world.getPlayers().size());
        sender.sendMessage(ChatColor.YELLOW + "PVP: " + ChatColor.WHITE + world.getPVP());
        sender.sendMessage(ChatColor.YELLOW + "Difficulty: " + ChatColor.WHITE + world.getDifficulty());
        sender.sendMessage(ChatColor.YELLOW + "Spawn: " + ChatColor.WHITE +
                world.getSpawnLocation().getBlockX() + ", " +
                world.getSpawnLocation().getBlockY() + ", " +
                world.getSpawnLocation().getBlockZ());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== JustWorld Příkazy ===");
        sender.sendMessage(ChatColor.YELLOW + "/world create <název> [typ] [seed]" + ChatColor.GRAY + " - Vytvoří nový svět");
        sender.sendMessage(ChatColor.YELLOW + "/world delete <název>" + ChatColor.GRAY + " - Smaže svět");
        sender.sendMessage(ChatColor.YELLOW + "/world load <název>" + ChatColor.GRAY + " - Načte svět");
        sender.sendMessage(ChatColor.YELLOW + "/world unload <název>" + ChatColor.GRAY + " - Odebere svět");
        sender.sendMessage(ChatColor.YELLOW + "/world tp <název>" + ChatColor.GRAY + " - Teleportuje do světa");
        sender.sendMessage(ChatColor.YELLOW + "/world list" + ChatColor.GRAY + " - Seznam světů");
        sender.sendMessage(ChatColor.YELLOW + "/world info <název>" + ChatColor.GRAY + " - Info o světě");
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
            completions.addAll(Arrays.asList("normal", "nether", "end"));
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
