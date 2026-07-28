package dev.zeropng.essentialscore.warp;

import dev.zeropng.essentialscore.config.PluginSettings;
import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.teleport.TeleportCoordinator;
import dev.zeropng.essentialscore.util.AtomicFiles;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;

public final class WarpManager {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_-]{1,32}");

    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final MessageService messages;
    private final TeleportCoordinator teleports;
    private final File file;
    private final YamlConfiguration yaml;
    private final Map<String, WarpData> warps = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Clock clock;

    public WarpManager(JavaPlugin plugin, PluginSettings settings, MessageService messages,
                       TeleportCoordinator teleports) {
        this(plugin, settings, messages, teleports, Clock.systemUTC());
    }

    WarpManager(JavaPlugin plugin, PluginSettings settings, MessageService messages,
                TeleportCoordinator teleports, Clock clock) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.teleports = teleports;
        this.clock = clock;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
        if (!file.exists()) plugin.saveResource("warps.yml", false);
        this.yaml = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        warps.clear();
        ConfigurationSection section = yaml.getConfigurationSection("warps");
        if (section == null) return;
        for (String rawId : section.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            ConfigurationSection entry = section.getConfigurationSection(rawId);
            WarpData warp = entry == null ? null : WarpData.read(id, entry);
            if (!validId(id) || warp == null || !validDisplayName(warp.displayName())) {
                plugin.getLogger().warning("Skipping invalid Warp definition: " + rawId);
                continue;
            }
            warps.put(id, warp);
        }
    }

    public List<WarpData> all() {
        return warps.values().stream().sorted(Comparator.comparing(WarpData::displayName,
                String.CASE_INSENSITIVE_ORDER).thenComparing(WarpData::id)).toList();
    }

    public WarpData get(String id) {
        return id == null ? null : warps.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean create(String rawId, String displayName, Location location) {
        String id = rawId.toLowerCase(Locale.ROOT);
        if (!validId(id) || !validDisplayName(displayName) || warps.containsKey(id)) return false;
        warps.put(id, WarpData.at(id, displayName, Material.ENDER_PEARL, location));
        save();
        return true;
    }

    public boolean rename(String id, String displayName) {
        WarpData old = get(id);
        if (old == null || !validDisplayName(displayName)) return false;
        warps.put(old.id(), new WarpData(old.id(), displayName, old.icon(), old.worldId(), old.worldName(),
                old.x(), old.y(), old.z(), old.yaw(), old.pitch()));
        save();
        return true;
    }

    public boolean setIcon(String id, Material icon) {
        WarpData old = get(id);
        if (old == null || icon == null || !icon.isItem() || icon.isAir()) return false;
        warps.put(old.id(), new WarpData(old.id(), old.displayName(), icon, old.worldId(), old.worldName(),
                old.x(), old.y(), old.z(), old.yaw(), old.pitch()));
        save();
        return true;
    }

    public boolean setLocation(String id, Location location) {
        WarpData old = get(id);
        if (old == null) return false;
        warps.put(old.id(), WarpData.at(old.id(), old.displayName(), old.icon(), location));
        save();
        return true;
    }

    public boolean delete(String id) {
        WarpData removed = get(id);
        if (removed == null) return false;
        warps.remove(removed.id());
        save();
        return true;
    }

    public void teleport(Player player, WarpData warp) {
        long remaining = cooldownRemaining(player.getUniqueId());
        if (remaining > 0) {
            messages.send(player, "warp.cooldown", Map.of("seconds", remaining));
            return;
        }
        if (warp.location() == null) {
            messages.send(player, "warp.world-missing");
            return;
        }
        if (!teleports.start(player, warp::location, settings.warpDelay(), "warp.teleport-failed", success -> {
            if (success) cooldowns.put(player.getUniqueId(), clock.millis() + settings.warpCooldown() * 1000L);
        })) messages.send(player, "error.busy");
    }

    public long cooldownRemaining(UUID playerId) {
        long millis = cooldowns.getOrDefault(playerId, 0L) - clock.millis();
        if (millis <= 0) {
            cooldowns.remove(playerId);
            return 0;
        }
        return (millis + 999L) / 1000L;
    }

    public void save() {
        yaml.set("warps", null);
        ConfigurationSection root = yaml.createSection("warps");
        for (WarpData warp : warps.values()) {
            ConfigurationSection section = root.createSection(warp.id());
            warp.write(section);
        }
        try {
            AtomicFiles.writeString(file.toPath(), yaml.saveToString());
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save warps.yml", exception);
        }
    }

    public static boolean validId(String id) {
        return id != null && ID_PATTERN.matcher(id).matches();
    }

    public static boolean validDisplayName(String name) {
        if (name == null) return false;
        int length = name.strip().codePointCount(0, name.strip().length());
        return length >= 1 && length <= 32;
    }
}
