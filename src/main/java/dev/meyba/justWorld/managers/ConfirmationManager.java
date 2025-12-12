package dev.meyba.justWorld.managers;

import dev.meyba.justWorld.JustWorld;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ConfirmationManager {
    private final JustWorld plugin;
    private final Map<UUID, PendingConfirmation> pendingConfirmations;
    private final Map<UUID, BukkitTask> expirationTasks;
    private final int expirationSeconds;

    public ConfirmationManager(JustWorld plugin) {
        this.plugin = plugin;
        this.pendingConfirmations = new ConcurrentHashMap<>();
        this.expirationTasks = new ConcurrentHashMap<>();
        this.expirationSeconds = plugin.getConfig().getInt("settings.confirmation-timeout", 30);
    }

    public void requestConfirmation(CommandSender sender, ConfirmationType type, String target, Consumer<CommandSender> onConfirm) {
        if (!(sender instanceof Player player)) {
            onConfirm.accept(sender);
            return;
        }

        UUID playerId = player.getUniqueId();

        cancelPending(playerId);

        PendingConfirmation confirmation = new PendingConfirmation(type, target, onConfirm);
        pendingConfirmations.put(playerId, confirmation);

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (pendingConfirmations.remove(playerId) != null) {
                expirationTasks.remove(playerId);
                plugin.getMessageUtil().send(player, "confirmation-expired");
            }
        }, expirationSeconds * 20L);

        expirationTasks.put(playerId, task);

        String messageKey = switch (type) {
            case DELETE -> "confirm-delete";
            case RENAME -> "confirm-rename";
            case UNLOAD -> "confirm-unload";
        };

        plugin.getMessageUtil().send(player, messageKey, "{target}", target, "{seconds}", String.valueOf(expirationSeconds));
        plugin.getMessageUtil().send(player, "confirm-hint");
    }

    public boolean confirm(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageUtil().send(sender, "no-pending-confirmation");
            return false;
        }

        UUID playerId = player.getUniqueId();
        PendingConfirmation confirmation = pendingConfirmations.remove(playerId);

        if (confirmation == null) {
            plugin.getMessageUtil().send(player, "no-pending-confirmation");
            return false;
        }

        BukkitTask task = expirationTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }

        confirmation.onConfirm().accept(sender);
        return true;
    }

    public boolean cancel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageUtil().send(sender, "no-pending-confirmation");
            return false;
        }

        UUID playerId = player.getUniqueId();
        PendingConfirmation confirmation = pendingConfirmations.remove(playerId);

        if (confirmation == null) {
            plugin.getMessageUtil().send(player, "no-pending-confirmation");
            return false;
        }

        cancelPending(playerId);
        plugin.getMessageUtil().send(player, "confirmation-cancelled");
        return true;
    }

    private void cancelPending(UUID playerId) {
        pendingConfirmations.remove(playerId);
        BukkitTask task = expirationTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public void shutdown() {
        expirationTasks.values().forEach(BukkitTask::cancel);
        expirationTasks.clear();
        pendingConfirmations.clear();
    }

    public enum ConfirmationType {
        DELETE,
        RENAME,
        UNLOAD
    }

    private record PendingConfirmation(ConfirmationType type, String target, Consumer<CommandSender> onConfirm) {}
}