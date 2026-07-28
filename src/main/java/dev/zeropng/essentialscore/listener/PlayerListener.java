package dev.zeropng.essentialscore.listener;

import dev.zeropng.essentialscore.storage.DataStore;
import dev.zeropng.essentialscore.rank.NameTagManager;
import dev.zeropng.essentialscore.localization.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class PlayerListener implements Listener {
    private final DataStore store;
    private final JavaPlugin plugin;
    private final NameTagManager nameTags;
    private final MessageService messages;

    public PlayerListener(JavaPlugin plugin, DataStore store, NameTagManager nameTags, MessageService messages) {
        this.plugin = plugin;
        this.store = store;
        this.nameTags = nameTags;
        this.messages = messages;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        store.recordName(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        store.save();
        Bukkit.getScheduler().runTask(plugin, () -> nameTags.refreshPlayer(event.getPlayer()));
        if (event.getPlayer().isOp()) {
            event.getPlayer().sendMessage(messages.component("op.join-banner", Map.of(
                    "player", event.getPlayer().getName(),
                    "version", plugin.getPluginMeta().getVersion(),
                    "author", String.join(", ", plugin.getPluginMeta().getAuthors())
            )));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        nameTags.removePlayer(event.getPlayer());
    }
}
