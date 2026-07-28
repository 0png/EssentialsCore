package dev.zeropng.essentialscore.tpa;

import dev.zeropng.essentialscore.config.PluginSettings;
import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.teleport.TeleportCoordinator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TpaManager {
    private final MessageService messages;
    private final PluginSettings settings;
    private final TeleportCoordinator teleports;
    private final Clock clock;
    private final Map<RequestKey, TpaRequest> requests = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TpaManager(JavaPlugin plugin, MessageService messages, PluginSettings settings,
                      TeleportCoordinator teleports) {
        this(messages, settings, teleports, Clock.systemUTC());
        Bukkit.getScheduler().runTaskTimer(plugin, this::expireRequests, 20L, 20L);
    }

    TpaManager(MessageService messages, PluginSettings settings, TeleportCoordinator teleports, Clock clock) {
        this.messages = messages;
        this.settings = settings;
        this.teleports = teleports;
        this.clock = clock;
    }

    public boolean send(Player requester, Player recipient, TpaType type) {
        if (requester.getUniqueId().equals(recipient.getUniqueId())) {
            messages.send(requester, "tpa.self");
            return false;
        }
        long remaining = cooldownRemaining(requester.getUniqueId());
        if (remaining > 0) {
            messages.send(requester, "tpa.cooldown", Map.of("seconds", remaining));
            return false;
        }
        long now = clock.millis();
        TpaRequest request = new TpaRequest(requester.getUniqueId(), requester.getName(),
                recipient.getUniqueId(), type, now, now + settings.tpaExpiry() * 1000L);
        requests.put(new RequestKey(requester.getUniqueId(), recipient.getUniqueId(), type), request);
        cooldowns.put(requester.getUniqueId(), now + settings.tpaCooldown() * 1000L);
        messages.send(requester, "tpa.sent", Map.of("player", recipient.getName()));
        notifyRecipient(recipient, request);
        return true;
    }

    private void notifyRecipient(Player recipient, TpaRequest request) {
        String key = request.type() == TpaType.TO_TARGET ? "tpa.received-to" : "tpa.received-here";
        Component accept = messages.component("tpa.accept-button")
                .clickEvent(ClickEvent.runCommand(internalCommand(true, request.requesterId())))
                .hoverEvent(HoverEvent.showText(messages.component("menu.requests.accept")));
        Component deny = messages.component("tpa.deny-button")
                .clickEvent(ClickEvent.runCommand(internalCommand(false, request.requesterId())))
                .hoverEvent(HoverEvent.showText(messages.component("menu.requests.deny")));
        recipient.sendMessage(messages.component("prefix")
                .append(messages.component(key, Map.of("player", request.requesterName())))
                .append(accept).append(Component.space()).append(deny));
    }

    public List<TpaRequest> incoming(UUID recipientId) {
        long now = clock.millis();
        return requests.values().stream()
                .filter(request -> request.recipientId().equals(recipientId) && request.expiresAt() > now)
                .sorted(Comparator.comparingLong(TpaRequest::createdAt).reversed())
                .toList();
    }

    public TpaRequest findIncoming(UUID recipientId, String requesterName) {
        return incoming(recipientId).stream()
                .filter(request -> requesterName == null || request.requesterName().equalsIgnoreCase(requesterName))
                .findFirst().orElse(null);
    }

    public TpaRequest findIncoming(UUID recipientId, UUID requesterId) {
        return incoming(recipientId).stream()
                .filter(request -> request.requesterId().equals(requesterId))
                .findFirst().orElse(null);
    }

    public boolean accept(Player recipient, String requesterName) {
        return accept(recipient, findIncoming(recipient.getUniqueId(), requesterName));
    }

    public boolean accept(Player recipient, UUID requesterId) {
        return accept(recipient, findIncoming(recipient.getUniqueId(), requesterId));
    }

    private boolean accept(Player recipient, TpaRequest request) {
        if (request == null) {
            messages.send(recipient, "error.no-request");
            return false;
        }
        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester == null || !requester.isOnline()) {
            remove(request);
            messages.send(recipient, "tpa.target-offline");
            return false;
        }
        Player mover = request.type() == TpaType.TO_TARGET ? requester : recipient;
        Player destination = request.type() == TpaType.TO_TARGET ? recipient : requester;
        if (teleports.isPending(mover.getUniqueId())) {
            messages.send(recipient, "error.busy");
            return false;
        }
        remove(request);
        messages.send(recipient, "tpa.accepted");
        if (!recipient.getUniqueId().equals(requester.getUniqueId())) messages.send(requester, "tpa.accepted");
        return teleports.start(mover,
                () -> destination.isOnline() ? destination.getLocation() : null,
                settings.tpaDelay(), "tpa.target-offline", ignored -> { });
    }

    public boolean deny(Player recipient, String requesterName) {
        return deny(recipient, findIncoming(recipient.getUniqueId(), requesterName));
    }

    public boolean deny(Player recipient, UUID requesterId) {
        return deny(recipient, findIncoming(recipient.getUniqueId(), requesterId));
    }

    private boolean deny(Player recipient, TpaRequest request) {
        if (request == null) {
            messages.send(recipient, "error.no-request");
            return false;
        }
        remove(request);
        messages.send(recipient, "tpa.denied");
        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester != null) messages.send(requester, "tpa.denied-sender", Map.of("player", recipient.getName()));
        return true;
    }

    private void remove(TpaRequest request) {
        requests.remove(new RequestKey(request.requesterId(), request.recipientId(), request.type()), request);
    }

    private void expireRequests() {
        long now = clock.millis();
        List<TpaRequest> expired = new ArrayList<>();
        for (TpaRequest request : requests.values()) if (request.expiresAt() <= now) expired.add(request);
        for (TpaRequest request : expired) {
            remove(request);
            Player requester = Bukkit.getPlayer(request.requesterId());
            Player recipient = Bukkit.getPlayer(request.recipientId());
            if (requester != null) messages.send(requester, "tpa.expired",
                    Map.of("player", recipient == null ? request.recipientId().toString() : recipient.getName()));
        }
    }

    private long cooldownRemaining(UUID playerId) {
        long expiry = cooldowns.getOrDefault(playerId, 0L);
        long remaining = expiry - clock.millis();
        if (remaining <= 0) {
            cooldowns.remove(playerId);
            return 0;
        }
        return (remaining + 999L) / 1000L;
    }

    private record RequestKey(UUID requester, UUID recipient, TpaType type) {
    }

    static String internalCommand(boolean accept, UUID requesterId) {
        return "/ec " + (accept ? "tpaccept " : "tpdeny ") + requesterId;
    }
}
