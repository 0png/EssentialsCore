package dev.zeropng.essentialscore.fun;

import dev.zeropng.essentialscore.config.PluginSettings;
import dev.zeropng.essentialscore.localization.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ExperimentalFeatureListener implements Listener {
    private static final long AFFECTION_COOLDOWN_MILLIS = 2_000L;
    private static final int[][] FOLLOW_OFFSETS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
            {2, 0}, {-2, 0}, {0, 2}, {0, -2}
    };

    private final PluginSettings settings;
    private final MessageService messages;
    private final JavaPlugin plugin;
    private final Map<UUID, Long> affectionCooldowns = new HashMap<>();

    public ExperimentalFeatureListener(JavaPlugin plugin, PluginSettings settings, MessageService messages) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.HAND && petAffection(event)) return;
        universalLeash(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFenceInteract(PlayerInteractEvent event) {
        Block fence = event.getClickedBlock();
        if (!settings.universalLead() || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND || fence == null
                || !Tag.FENCES.isTagged(fence.getType())) return;

        // Let vanilla transfer every entity it recognises first, then attach any
        // additional LivingEntity accepted by the universal lead feature.
        Bukkit.getScheduler().runTask(plugin, () -> attachRemainingToFence(event.getPlayer(), fence));
    }

    private void attachRemainingToFence(Player player, Block fence) {
        if (!player.isOnline() || !fence.getChunk().isLoaded() || !Tag.FENCES.isTagged(fence.getType())) return;
        Location centre = fence.getLocation().add(0.5D, 0.5D, 0.5D);
        List<LivingEntity> remaining = new ArrayList<>();
        for (LivingEntity living : fence.getWorld().getNearbyEntitiesByType(LivingEntity.class, centre, 12.0D)) {
            if (isLeashedTo(living, player)) remaining.add(living);
        }
        if (remaining.isEmpty()) return;

        LeashHitch hitch = fence.getWorld().getNearbyEntitiesByType(LeashHitch.class, centre, 0.75D)
                .stream().findFirst().orElse(null);
        boolean created = hitch == null;
        if (hitch == null) hitch = fence.getWorld().spawn(fence.getLocation(), LeashHitch.class);

        int transferred = 0;
        for (LivingEntity living : remaining) {
            PlayerLeashEntityEvent leashEvent = new PlayerLeashEntityEvent(
                    living, hitch, player, EquipmentSlot.HAND);
            if (leashEvent.callEvent() && living.setLeashHolder(hitch)) transferred++;
        }
        if (created && transferred == 0) hitch.remove();
    }

    private static boolean isLeashedTo(LivingEntity living, Player player) {
        if (!living.isLeashed()) return false;
        try {
            Entity holder = living.getLeashHolder();
            return holder.getUniqueId().equals(player.getUniqueId());
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!settings.universalLead() || event.getTo() == null) return;
        Location source = event.getFrom();
        List<LivingEntity> followers = new ArrayList<>();
        for (LivingEntity living : source.getWorld().getNearbyEntitiesByType(
                LivingEntity.class, source, 12.0D)) {
            if (isLeashedTo(living, event.getPlayer())) followers.add(living);
        }
        if (followers.isEmpty()) return;
        Bukkit.getScheduler().runTask(plugin, () -> teleportFollowers(event.getPlayer(), followers));
    }

    private void teleportFollowers(Player player, List<LivingEntity> followers) {
        if (!player.isOnline()) return;
        Location playerLocation = player.getLocation();
        Set<FollowerSpot> reserved = new HashSet<>();
        for (LivingEntity follower : followers) {
            if (!follower.isValid() || follower.isDead()) continue;
            Location target = findFollowerLocation(follower, playerLocation, reserved);
            follower.teleportAsync(target, PlayerTeleportEvent.TeleportCause.PLUGIN)
                    .whenComplete((worked, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (throwable != null || !Boolean.TRUE.equals(worked) || !player.isOnline()
                                || !follower.isValid() || !follower.getWorld().equals(player.getWorld())) return;
                        PlayerLeashEntityEvent leashEvent = new PlayerLeashEntityEvent(
                                follower, player, player, EquipmentSlot.HAND);
                        if (leashEvent.callEvent()) follower.setLeashHolder(player);
                    }));
        }
    }

    private static Location findFollowerLocation(LivingEntity follower, Location playerLocation,
                                                  Set<FollowerSpot> reserved) {
        for (int[] offset : FOLLOW_OFFSETS) {
            int x = playerLocation.getBlockX() + offset[0];
            int z = playerLocation.getBlockZ() + offset[1];
            for (int y = playerLocation.getBlockY() + 1; y >= playerLocation.getBlockY() - 1; y--) {
                FollowerSpot spot = new FollowerSpot(x, y, z);
                if (reserved.contains(spot)
                        || !playerLocation.getWorld().getBlockAt(x, y - 1, z).getType().isSolid()
                        || !playerLocation.getWorld().getBlockAt(x, y, z).isPassable()
                        || !playerLocation.getWorld().getBlockAt(x, y + 1, z).isPassable()) continue;

                Location candidate = new Location(playerLocation.getWorld(), x + 0.5D, y, z + 0.5D,
                        playerLocation.getYaw(), 0.0F);
                org.bukkit.util.BoundingBox movedBox = follower.getBoundingBox().clone().shift(
                        candidate.getX() - follower.getX(), candidate.getY() - follower.getY(),
                        candidate.getZ() - follower.getZ());
                if (follower.getWorld().equals(playerLocation.getWorld()) && follower.wouldCollideUsing(movedBox)) {
                    continue;
                }
                reserved.add(spot);
                return candidate;
            }
        }
        return playerLocation.clone();
    }

    private record FollowerSpot(int x, int y, int z) {
    }

    private boolean petAffection(PlayerInteractEntityEvent event) {
        if (!settings.petAffection() || !event.getPlayer().isSneaking()
                || event.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR
                || !(event.getRightClicked() instanceof Tameable pet) || !pet.isTamed()
                || !event.getPlayer().getUniqueId().equals(pet.getOwnerUniqueId())) return false;

        event.setCancelled(true);
        long now = System.currentTimeMillis();
        long readyAt = affectionCooldowns.getOrDefault(event.getPlayer().getUniqueId(), 0L);
        if (readyAt > now) return true;
        affectionCooldowns.put(event.getPlayer().getUniqueId(), now + AFFECTION_COOLDOWN_MILLIS);

        double height = Math.max(0.5D, pet.getHeight());
        pet.getWorld().spawnParticle(Particle.HEART, pet.getLocation().add(0.0D, height * 0.8D, 0.0D),
                5, 0.35D, 0.25D, 0.35D, 0.02D);
        pet.getWorld().playSound(pet.getLocation(), Sound.ENTITY_CAT_PURR, 0.65F, 1.15F);
        messages.send(event.getPlayer(), "experimental.pet-affection-success");
        return true;
    }

    private void universalLeash(PlayerInteractEntityEvent event) {
        if (!settings.universalLead() || event.getRightClicked() instanceof Player
                || event.getRightClicked() instanceof ArmorStand
                || !(event.getRightClicked() instanceof LivingEntity living)) return;

        ItemStack lead = itemInHand(event.getPlayer(), event.getHand());
        if (lead.getType() != Material.LEAD || living.isLeashed()) return;

        if (living instanceof Tameable pet && pet.isTamed() && pet.getOwnerUniqueId() != null
                && !event.getPlayer().getUniqueId().equals(pet.getOwnerUniqueId())) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "experimental.lead-owned-pet");
            return;
        }

        PlayerLeashEntityEvent leashEvent = new PlayerLeashEntityEvent(
                living, event.getPlayer(), event.getPlayer(), event.getHand());
        event.setCancelled(true);
        if (!leashEvent.callEvent()) return;

        if (!living.setLeashHolder(event.getPlayer())) {
            messages.send(event.getPlayer(), "experimental.lead-failed");
            return;
        }
        consumeLead(event.getPlayer(), event.getHand(), lead);
        messages.send(event.getPlayer(), "experimental.lead-success");
    }

    private static ItemStack itemInHand(Player player, EquipmentSlot hand) {
        return hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
    }

    private static void consumeLead(Player player, EquipmentSlot hand, ItemStack lead) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        ItemStack remaining = lead.getAmount() <= 1 ? new ItemStack(Material.AIR) : lead.clone();
        if (!remaining.getType().isAir()) remaining.setAmount(lead.getAmount() - 1);
        if (hand == EquipmentSlot.OFF_HAND) player.getInventory().setItemInOffHand(remaining);
        else player.getInventory().setItemInMainHand(remaining);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        affectionCooldowns.remove(event.getPlayer().getUniqueId());
    }
}
