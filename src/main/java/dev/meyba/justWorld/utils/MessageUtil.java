package dev.meyba.justWorld.utils;

import dev.meyba.justWorld.JustWorld;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MessageUtil {

    private final JustWorld plugin;

    public MessageUtil(JustWorld plugin) {
        this.plugin = plugin;
    }

    /**
     * Get formatted prefix from config
     */
    public String getPrefix() {
        String prefix = plugin.getConfig().getString("prefix", "&x&5&A&9&E&F&F&lᴊ&x&5&C&A&0&F&F&lᴜ&x&5&E&A&2&F&F&lꜱ&x&6&0&A&4&F&F&lᴛ&x&6&2&A&6&F&F&lᴡ&x&6&4&A&8&F&F&lᴏ&x&6&6&A&A&F&F&lʀ&x&6&8&A&C&F&F&lʟ&x&6&A&A&E&F&F&lᴅ &8» &f");
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    /**
     * Get message from config with color codes translated
     */
    public String getMessage(String path) {
        String message = plugin.getConfig().getString("messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Send message with prefix to sender
     */
    public void send(CommandSender sender, String messagePath) {
        sender.sendMessage(getPrefix() + getMessage(messagePath));
    }

    /**
     * Send message with prefix and placeholder replacement
     */
    public void send(CommandSender sender, String messagePath, String placeholder, String value) {
        String message = getMessage(messagePath).replace(placeholder, value);
        sender.sendMessage(getPrefix() + message);
    }

    /**
     * Send raw message (already formatted)
     */
    public void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(getPrefix() + ChatColor.translateAlternateColorCodes('&', message));
    }
}
