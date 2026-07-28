package dev.zeropng.essentialscore.fun;

import dev.zeropng.essentialscore.config.PluginSettings;
import dev.zeropng.essentialscore.localization.MessageService;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ExperimentalFeatureListener implements Listener {
    private static final long AFFECTION_COOLDOWN_MILLIS = 2_000L;

    private final PluginSettings settings;
    private final MessageService messages;
    private final Map<UUID, Long> affectionCooldowns = new HashMap<>();

    public ExperimentalFeatureListener(PluginSettings settings, MessageService messages) {
        this.settings = settings;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.HAND && petAffection(event)) return;
        universalLeash(event);
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
