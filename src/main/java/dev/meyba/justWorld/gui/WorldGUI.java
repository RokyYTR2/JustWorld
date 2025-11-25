package dev.meyba.justWorld.gui;

import dev.meyba.justWorld.JustWorld;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class WorldGUI implements Listener {
    private static final String GUI_NAME = ChatColor.translateAlternateColorCodes('&', "&lᴡᴏʀʟᴅ ʟɪꜱᴛ");
    private final JustWorld plugin;
    private static final Map<Player, Integer> playerPages = new HashMap<>();

    public WorldGUI(JustWorld plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        int page = playerPages.getOrDefault(player, 1);
        Inventory inventory = createInventory(player, page);
        player.openInventory(inventory);
    }

    private Inventory createInventory(Player player, int page) {
        String title = GUI_NAME + ChatColor.translateAlternateColorCodes('&', " &8(ᴘᴀɢᴇ " + page + ")");
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        setupInventory(inventory, player, page);
        return inventory;
    }

    private void setupInventory(Inventory inventory, Player player, int page) {
        inventory.clear();

        ItemStack grayGlass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", Collections.emptyList());
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, grayGlass);
        }
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, grayGlass);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, grayGlass);
            inventory.setItem(i + 8, grayGlass);
        }

        List<World> worlds = plugin.getWorldManager().getAllWorlds();
        int itemsPerPage = 28;
        int totalPages = Math.max(1, (int) Math.ceil((double) worlds.size() / itemsPerPage));
        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, worlds.size());

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int slotIndex = 0;

        for (int i = startIndex; i < endIndex && slotIndex < slots.length; i++) {
            World world = worlds.get(i);
            ItemStack worldItem = createWorldItem(world);
            inventory.setItem(slots[slotIndex], worldItem);
            slotIndex++;
        }

        if (page > 1) {
            ItemStack prevPage = createItem(Material.ARROW, "&aᴘʀᴇᴠɪᴏᴜꜱ ᴘᴀɢᴇ", Arrays.asList("&7ᴄʟɪᴄᴋ ᴛᴏ ɢᴏ ᴛᴏ ᴘʀᴇᴠɪᴏᴜꜱ ᴘᴀɢᴇ."));
            inventory.setItem(48, prevPage);
        }

        if (page < totalPages) {
            ItemStack nextPage = createItem(Material.ARROW, "&aɴᴇxᴛ ᴘᴀɢᴇ", Arrays.asList("&7ᴄʟɪᴄᴋ ᴛᴏ ɢᴏ ᴛᴏ ɴᴇxᴛ ᴘᴀɢᴇ."));
            inventory.setItem(50, nextPage);
        }

        ItemStack pageInfo = createItem(Material.PAPER, "&aᴘᴀɢᴇ " + page + "/" + totalPages,
            Arrays.asList("&8» &7ᴛᴏᴛᴀʟ ᴡᴏʀʟᴅꜱ: &a" + worlds.size()));
        inventory.setItem(49, pageInfo);
    }

    private ItemStack createWorldItem(World world) {
        Material material;
        switch (world.getEnvironment()) {
            case NETHER:
                material = Material.NETHERRACK;
                break;
            case THE_END:
                material = Material.END_STONE;
                break;
            default:
                material = Material.GRASS_BLOCK;
                break;
        }

        List<String> lore = Arrays.asList(
            "&8» &7ᴇɴᴠɪʀᴏɴᴍᴇɴᴛ: &a" + world.getEnvironment().name(),
            "&8» &7ᴘʟᴀʏᴇʀꜱ: &a" + world.getPlayers().size(),
            "&8» &7ᴅɪꜰꜰɪᴄᴜʟᴛʏ: &a" + world.getDifficulty().name(),
            "&8» &7ᴘᴠᴘ: &a" + (world.getPVP() ? "ᴇɴᴀʙʟᴇᴅ" : "ᴅɪꜱᴀʙʟᴇᴅ"),
            "",
            "&aʟᴇꜰᴛ-ᴄʟɪᴄᴋ ᴛᴏ ᴛᴇʟᴇᴘᴏʀᴛ",
            "&eʀɪɢʜᴛ-ᴄʟɪᴄᴋ ᴛᴏ ᴠɪᴇᴡ ɪɴꜰᴏ",
            "&cꜱʜɪꜰᴛ + ʀɪɢʜᴛ-ᴄʟɪᴄᴋ ᴛᴏ ᴅᴇʟᴇᴛᴇ"
        );

        return createItem(material, "&a" + world.getName(), lore);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(lore.stream()
                .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                .collect(Collectors.toList()));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(GUI_NAME)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        int currentPage = playerPages.getOrDefault(player, 1);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (event.getSlot() == 48 && clicked.getType() == Material.ARROW) {
            if (currentPage > 1) {
                playerPages.put(player, currentPage - 1);
                player.openInventory(createInventory(player, currentPage - 1));
            }
            return;
        }

        if (event.getSlot() == 50 && clicked.getType() == Material.ARROW) {
            List<World> worlds = plugin.getWorldManager().getAllWorlds();
            int totalPages = Math.max(1, (int) Math.ceil((double) worlds.size() / 28));
            if (currentPage < totalPages) {
                playerPages.put(player, currentPage + 1);
                player.openInventory(createInventory(player, currentPage + 1));
            }
            return;
        }

        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE ||
            clicked.getType() == Material.PAPER) return;

        if (clicked.getType() == Material.GRASS_BLOCK ||
            clicked.getType() == Material.NETHERRACK ||
            clicked.getType() == Material.END_STONE) {

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;

            String worldName = ChatColor.stripColor(meta.getDisplayName());
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                plugin.getMessageUtil().send(player, "world-not-found", "{world}", worldName);
                return;
            }

            if (event.isShiftClick() && event.isRightClick()) {
                player.closeInventory();
                plugin.getMessageUtil().send(player, "deleting-world", "{world}", worldName);
                plugin.getWorldManager().deleteWorld(worldName).thenAccept(success -> {
                    if (success) {
                        plugin.getMessageUtil().send(player, "world-deleted", "{world}", worldName);
                    } else {
                        plugin.getMessageUtil().send(player, "world-delete-failed");
                    }
                });
            } else if (event.isLeftClick()) {
                player.closeInventory();
                player.teleport(world.getSpawnLocation());
                plugin.getMessageUtil().send(player, "teleported", "{world}", worldName);
            } else if (event.isRightClick()) {
                player.closeInventory();
                String prefix = plugin.getMessageUtil().getPrefix();
                player.sendMessage(prefix + ChatColor.GRAY + "ᴡᴏʀʟᴅ ɪɴꜰᴏ: " + worldName);
                player.sendMessage(prefix + ChatColor.GRAY + "ᴇɴᴠɪʀᴏɴᴍᴇɴᴛ: " + world.getEnvironment());
                player.sendMessage(prefix + ChatColor.GRAY + "ꜱᴇᴇᴅ: " + world.getSeed());
                player.sendMessage(prefix + ChatColor.GRAY + "ᴘʟᴀʏᴇʀꜱ: " + world.getPlayers().size());
                player.sendMessage(prefix + ChatColor.GRAY + "ᴘᴠᴘ: " + world.getPVP());
                player.sendMessage(prefix + ChatColor.GRAY + "ᴅɪꜰꜰɪᴄᴜʟᴛʏ: " + world.getDifficulty());
            }
        }
    }
}
