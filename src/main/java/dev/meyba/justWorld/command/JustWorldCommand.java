package dev.meyba.justWorld.command;

import dev.meyba.justWorld.JustWorld;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class JustWorldCommand implements CommandExecutor {
    private final JustWorld plugin;

    public JustWorldCommand(JustWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            handleReload(sender);
            return true;
        }

        showInfo(sender);
        return true;
    }

    private void showInfo(CommandSender sender) {
        List<World> worlds = plugin.getWorldManager().getAllWorlds();
        int totalPlayers = worlds.stream().mapToInt(w -> w.getPlayers().size()).sum();
        int configuredWorlds = plugin.getWorldManager().getAllWorldData().size();

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "JustWorld");
        sender.sendMessage(ChatColor.GRAY + "Ultra-fast async world management");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Authors: " + ChatColor.WHITE + String.join(", ", plugin.getDescription().getAuthors()));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "=== Statistics ===");
        sender.sendMessage(ChatColor.YELLOW + "Loaded Worlds: " + ChatColor.WHITE + worlds.size());
        sender.sendMessage(ChatColor.YELLOW + "Configured Worlds: " + ChatColor.WHITE + configuredWorlds);
        sender.sendMessage(ChatColor.YELLOW + "Total Players: " + ChatColor.WHITE + totalPlayers);
        sender.sendMessage(ChatColor.YELLOW + "Server Uptime: " + ChatColor.WHITE + getUptime());
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "=== Performance ===");

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        double tps = getTPS();

        sender.sendMessage(ChatColor.YELLOW + "Memory Usage: " + ChatColor.WHITE +
                usedMemory + "MB / " + maxMemory + "MB");
        sender.sendMessage(ChatColor.YELLOW + "TPS: " + getTPSColor(tps) + String.format("%.2f", tps));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Commands: /world help | /justworld reload");
        sender.sendMessage("");
    }

    private void handleReload(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Reloading JustWorld configuration...");

        try {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error reloading config: " + e.getMessage());
        }
    }

    private String getUptime() {
        long uptimeMillis = System.currentTimeMillis() - plugin.getServer().getStartTime();
        long seconds = uptimeMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    private double getTPS() {
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            Object recentTps = server.getClass().getField("recentTps").get(server);
            return ((double[]) recentTps)[0];
        } catch (Exception e) {
            return 20.0;
        }
    }

    private ChatColor getTPSColor(double tps) {
        if (tps >= 19.0) return ChatColor.GREEN;
        if (tps >= 17.0) return ChatColor.YELLOW;
        return ChatColor.RED;
    }
}