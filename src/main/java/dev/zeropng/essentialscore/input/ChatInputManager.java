package dev.zeropng.essentialscore.input;

import dev.zeropng.essentialscore.config.PluginSettings;
import dev.zeropng.essentialscore.localization.MessageService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class ChatInputManager implements Listener {
    private final JavaPlugin plugin;
    private final MessageService messages;
    private final PluginSettings settings;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public ChatInputManager(JavaPlugin plugin, MessageService messages, PluginSettings settings) {
        this.plugin = plugin;
        this.messages = messages;
        this.settings = settings;
    }

    public void start(Player player, String promptKey, Map<String, ?> values,
                      Function<String, Boolean> handler, Runnable onCancel) {
        cancelSilently(player.getUniqueId());
        player.closeInventory();
        messages.send(player, promptKey, values);
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Session removed = sessions.remove(player.getUniqueId());
            if (removed != null) messages.send(player, "common.input-timeout");
        }, settings.inputTimeout() * 20L);
        sessions.put(player.getUniqueId(), new Session(handler, onCancel, timeout));
    }

    public boolean isWaiting(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) return;
        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> handle(event.getPlayer(), input));
    }

    private void handle(Player player, String input) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (input.equalsIgnoreCase("cancel")) {
            sessions.remove(player.getUniqueId());
            session.timeout.cancel();
            messages.send(player, "common.cancelled");
            session.onCancel.run();
            return;
        }
        if (session.handler.apply(input)) {
            sessions.remove(player.getUniqueId());
            session.timeout.cancel();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelSilently(event.getPlayer().getUniqueId());
    }

    public void cancelAll() {
        sessions.keySet().forEach(this::cancelSilently);
    }

    private void cancelSilently(UUID playerId) {
        Session removed = sessions.remove(playerId);
        if (removed != null) removed.timeout.cancel();
    }

    private record Session(Function<String, Boolean> handler, Runnable onCancel, BukkitTask timeout) {
    }
}
