package dev.meyba.justWorld.utils;

import dev.meyba.justWorld.JustWorld;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ChatUtil {
    private final JustWorld plugin;
    private YamlConfiguration messagesConfig;

    public ChatUtil(JustWorld plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getPrefix() {
        String prefix = plugin.getConfig().getString("prefix", "&x&5&A&9&E&F&F&lᴊ&x&5&C&A&0&F&F&lᴜ&x&5&E&A&2&F&F&lꜱ&x&6&0&A&4&F&F&lᴛ&x&6&2&A&6&F&F&lᴡ&x&6&4&A&8&F&F&lᴏ&x&6&6&A&A&F&F&lʀ&x&6&8&A&C&F&F&lʟ&x&6&A&A&E&F&F&lᴅ &8» &f");
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    public String getMessage(String path) {
        String message = messagesConfig.getString(path, "");
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void send(CommandSender sender, String messagePath) {
        sender.sendMessage(getPrefix() + getMessage(messagePath));
    }

    public void send(CommandSender sender, String messagePath, String placeholder, String value) {
        String message = getMessage(messagePath).replace(placeholder, value);
        sender.sendMessage(getPrefix() + message);
    }

    public void send(CommandSender sender, String messagePath, String... placeholders) {
        String message = getMessage(messagePath);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        sender.sendMessage(getPrefix() + message);
    }
}