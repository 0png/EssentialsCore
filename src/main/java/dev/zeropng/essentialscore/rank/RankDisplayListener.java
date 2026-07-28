package dev.zeropng.essentialscore.rank;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class RankDisplayListener implements Listener {
    private final RankManager ranks;

    public RankDisplayListener(RankManager ranks) {
        this.ranks = ranks;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        RankData rank = ranks.assigned(event.getPlayer().getUniqueId());
        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) ->
                format(rank, sourceDisplayName, message)));
    }

    static Component format(RankData rank, Component playerName, Component message) {
        return rank.prefixComponent()
                .append(playerName.colorIfAbsent(rank.textColor()))
                .append(Component.text(": "))
                .append(message);
    }
}
