package dev.zeropng.essentialscore.lay;

import dev.zeropng.essentialscore.localization.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.block.Action;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class LayManager implements Listener {
    private final MessageService messages;
    private final Set<UUID> laying = new HashSet<>();

    public LayManager(MessageService messages) {
        this.messages = messages;
    }

    public boolean toggle(Player player) {
        if (isLaying(player)) {
            stand(player, true);
            return false;
        }
        if (player.isInsideVehicle() || player.isFlying() || player.isGliding()) {
            messages.send(player, "lay.cannot");
            return false;
        }
        laying.add(player.getUniqueId());
        player.setPose(Pose.SLEEPING, true);
        messages.send(player, "lay.laid");
        return true;
    }

    public boolean isLaying(Player player) {
        return laying.contains(player.getUniqueId());
    }

    public void stand(Player player) {
        stand(player, false);
    }

    private void stand(Player player, boolean notify) {
        if (!laying.remove(player.getUniqueId())) return;
        player.setPose(Pose.STANDING, false);
        if (notify) messages.send(player, "lay.stood");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) stand(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (isLaying(event.getPlayer()) && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK)) stand(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!isLaying(event.getPlayer()) || event.getTo() == null) return;
        if (Double.compare(event.getFrom().getX(), event.getTo().getX()) != 0
                || Double.compare(event.getFrom().getY(), event.getTo().getY()) != 0
                || Double.compare(event.getFrom().getZ(), event.getTo().getZ()) != 0) stand(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) stand(player, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        stand(event.getPlayer(), false);
    }

    @EventHandler public void onDeath(PlayerDeathEvent event) { stand(event.getPlayer(), false); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { stand(event.getPlayer(), false); }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) stand(player, false);
        laying.clear();
    }
}
