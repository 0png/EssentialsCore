package dev.zeropng.essentialscore.home;

import dev.zeropng.essentialscore.config.PluginSettings;
import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.storage.DataStore;
import dev.zeropng.essentialscore.teleport.TeleportCoordinator;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.text.Normalizer;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class HomeManager {
    public enum NameResult { VALID, INVALID, RESERVED, DUPLICATE }

    private final DataStore store;
    private final PluginSettings settings;
    private final MessageService messages;
    private final TeleportCoordinator teleports;
    private final Clock clock;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public HomeManager(DataStore store, PluginSettings settings, MessageService messages,
                       TeleportCoordinator teleports) {
        this(store, settings, messages, teleports, Clock.systemUTC());
    }

    HomeManager(DataStore store, PluginSettings settings, MessageService messages,
                TeleportCoordinator teleports, Clock clock) {
        this.store = store;
        this.settings = settings;
        this.messages = messages;
        this.teleports = teleports;
        this.clock = clock;
    }

    public List<HomeData> homes(UUID playerId) {
        ConfigurationSection player = store.player(playerId, false);
        ConfigurationSection homes = player == null ? null : player.getConfigurationSection("homes");
        List<HomeData> result = new ArrayList<>();
        if (homes == null) return result;
        for (String key : homes.getKeys(false)) {
            ConfigurationSection section = homes.getConfigurationSection(key);
            if (section == null) continue;
            HomeData home = HomeData.read(key, section);
            if (home != null) result.add(home);
        }
        result.sort(Comparator.comparing(HomeData::name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public HomeData find(UUID playerId, String key) {
        return homes(playerId).stream().filter(home -> home.key().equals(key)).findFirst().orElse(null);
    }

    public NameResult validateName(UUID playerId, String rawName) {
        String display = displayName(rawName);
        if (display.equalsIgnoreCase("cancel")) return NameResult.RESERVED;
        if (!validCharacters(display)) return NameResult.INVALID;
        String key = normalize(display);
        return find(playerId, key) == null ? NameResult.VALID : NameResult.DUPLICATE;
    }

    public HomeData create(Player player, String rawName) {
        String name = displayName(rawName);
        String key = normalize(name);
        Location location = player.getLocation();
        ConfigurationSection homes = store.player(player.getUniqueId(), true).getConfigurationSection("homes");
        if (homes == null) homes = store.player(player.getUniqueId(), true).createSection("homes");
        ConfigurationSection section = homes.createSection(key);
        section.set("name", name);
        section.set("world-uuid", location.getWorld().getUID().toString());
        section.set("world-name", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
        store.save();
        return HomeData.read(key, section);
    }

    public boolean delete(UUID playerId, String key) {
        ConfigurationSection player = store.player(playerId, false);
        ConfigurationSection homes = player == null ? null : player.getConfigurationSection("homes");
        if (homes == null || !homes.contains(key)) return false;
        homes.set(key, null);
        store.save();
        return true;
    }

    public void teleport(Player player, HomeData home) {
        long remaining = cooldownRemaining(player.getUniqueId());
        if (remaining > 0) {
            messages.send(player, "teleport.cooldown", Map.of("seconds", remaining));
            return;
        }
        if (home.location() == null) {
            messages.send(player, "home.world-missing");
            return;
        }
        if (!teleports.start(player, home::location, settings.homeDelay(), "home.teleport-failed", success -> {
            if (success) cooldowns.put(player.getUniqueId(), clock.millis() + settings.homeCooldown() * 1000L);
        })) {
            messages.send(player, "error.busy");
        }
    }

    public long cooldownRemaining(UUID playerId) {
        long expiry = cooldowns.getOrDefault(playerId, 0L);
        long millis = expiry - clock.millis();
        if (millis <= 0) {
            cooldowns.remove(playerId);
            return 0;
        }
        return (millis + 999L) / 1000L;
    }

    public static String displayName(String input) {
        return Normalizer.normalize(input.trim().replaceAll("\\s+", " "), Normalizer.Form.NFC);
    }

    public static String normalize(String input) {
        return displayName(input).toLowerCase(Locale.ROOT);
    }

    public static boolean validCharacters(String value) {
        int count = value.codePointCount(0, value.length());
        if (count < 1 || count > 24) return false;
        return value.codePoints().allMatch(codePoint -> Character.isLetterOrDigit(codePoint)
                || codePoint == ' ' || codePoint == '_' || codePoint == '-');
    }
}
