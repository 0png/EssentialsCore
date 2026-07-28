package dev.zeropng.essentialscore.teleport;

import dev.zeropng.essentialscore.localization.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TeleportCoordinator implements Listener {
    private final JavaPlugin plugin;
    private final MessageService messages;
    private final Map<UUID, Pending> pending = new HashMap<>();

    public TeleportCoordinator(JavaPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public boolean isPending(UUID playerId) {
        return pending.containsKey(playerId);
    }

    public boolean start(Player player, Supplier<Location> destination, int delaySeconds,
                         String failureKey, Consumer<Boolean> completed) {
        if (pending.containsKey(player.getUniqueId())) return false;
        Location start = player.getLocation();
        Pending state = new Pending(start.getWorld().getUID(), start.getBlockX(), start.getBlockY(),
                start.getBlockZ(), destination, failureKey, completed, delaySeconds);
        pending.put(player.getUniqueId(), state);
        if (delaySeconds <= 0) {
            finish(player, state);
            return true;
        }
        messages.send(player, "teleport.starting", Map.of("seconds", delaySeconds));
        state.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(player.getUniqueId()), 20L, 20L);
        return true;
    }

    private void tick(UUID playerId) {
        Pending state = pending.get(playerId);
        Player player = Bukkit.getPlayer(playerId);
        if (state == null || player == null) {
            cancelSilently(playerId);
            return;
        }
        state.remaining--;
        if (state.remaining <= 0) {
            finish(player, state);
        } else {
            player.sendActionBar(messages.component("teleport.countdown", Map.of("seconds", state.remaining)));
        }
    }

    private void finish(Player player, Pending state) {
        if (pending.remove(player.getUniqueId(), state) && state.task != null) state.task.cancel();
        Location target = state.destination.get();
        if (target == null || target.getWorld() == null) {
            messages.send(player, state.failureKey);
            state.completed.accept(false);
            return;
        }
        player.teleportAsync(target, PlayerTeleportEvent.TeleportCause.PLUGIN).whenComplete((success, throwable) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    boolean worked = throwable == null && Boolean.TRUE.equals(success);
                    messages.send(player, worked ? "teleport.success" : state.failureKey);
                    state.completed.accept(worked);
                }));
    }

    public void cancelForMove(Player player) {
        cancel(player, "teleport.cancelled-move");
    }

    public void cancelForDamage(Player player) {
        cancel(player, "teleport.cancelled-damage");
    }

    private void cancel(Player player, String messageKey) {
        Pending removed = pending.remove(player.getUniqueId());
        if (removed == null) return;
        if (removed.task != null) removed.task.cancel();
        messages.send(player, messageKey);
        removed.completed.accept(false);
    }

    private void cancelSilently(UUID playerId) {
        Pending removed = pending.remove(playerId);
        if (removed != null) {
            if (removed.task != null) removed.task.cancel();
            removed.completed.accept(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Pending state = pending.get(event.getPlayer().getUniqueId());
        if (state == null || event.getTo() == null) return;
        Location to = event.getTo();
        if (!to.getWorld().getUID().equals(state.worldId) || to.getBlockX() != state.x
                || to.getBlockY() != state.y || to.getBlockZ() != state.z) {
            cancelForMove(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getFinalDamage() > 0) cancelForDamage(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelSilently(event.getPlayer().getUniqueId());
    }

    public void cancelAll() {
        for (UUID id : pending.keySet().toArray(UUID[]::new)) cancelSilently(id);
    }

    private static final class Pending {
        private final UUID worldId;
        private final int x;
        private final int y;
        private final int z;
        private final Supplier<Location> destination;
        private final String failureKey;
        private final Consumer<Boolean> completed;
        private int remaining;
        private BukkitTask task;

        private Pending(UUID worldId, int x, int y, int z, Supplier<Location> destination,
                        String failureKey, Consumer<Boolean> completed, int remaining) {
            this.worldId = worldId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.destination = destination;
            this.failureKey = failureKey;
            this.completed = completed;
            this.remaining = remaining;
        }
    }
}
