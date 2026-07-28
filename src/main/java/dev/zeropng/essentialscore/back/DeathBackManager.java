package dev.zeropng.essentialscore.back;

import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.storage.DataStore;
import dev.zeropng.essentialscore.teleport.TeleportCoordinator;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DeathBackManager implements Listener {
    private static final String DATA_PATH = "last-death";

    private final JavaPlugin plugin;
    private final DataStore store;
    private final MessageService messages;
    private final TeleportCoordinator teleports;
    private final Set<UUID> pendingTitles = new HashSet<>();

    public DeathBackManager(JavaPlugin plugin, DataStore store, MessageService messages,
                            TeleportCoordinator teleports) {
        this.plugin = plugin;
        this.store = store;
        this.messages = messages;
        this.teleports = teleports;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection playerData = store.player(player.getUniqueId(), true);
        playerData.set(DATA_PATH, null);
        ConfigurationSection section = playerData.createSection(DATA_PATH);
        DeathLocationData.from(player.getLocation()).write(section);
        store.save();
        pendingTitles.add(player.getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!pendingTitles.remove(player.getUniqueId())) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || player.isDead()) return;
            player.showTitle(Title.title(
                    messages.component("back.death-title"),
                    messages.component("back.death-subtitle"),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofMillis(700))
            ));
        }, 10L);
    }

    public void teleportBack(Player player) {
        DeathLocationData death = deathLocation(player.getUniqueId());
        if (death == null) {
            messages.send(player, "back.no-death");
            return;
        }
        Location destination = death.location();
        if (destination == null) {
            messages.send(player, "back.world-missing");
            return;
        }
        if (!teleports.start(player, death::location, 0, "back.teleport-failed", ignored -> { })) {
            messages.send(player, "error.busy");
        }
    }

    public DeathLocationData deathLocation(UUID playerId) {
        ConfigurationSection player = store.player(playerId, false);
        return player == null ? null : DeathLocationData.read(player.getConfigurationSection(DATA_PATH));
    }
}
