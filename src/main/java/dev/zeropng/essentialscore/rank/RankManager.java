package dev.zeropng.essentialscore.rank;

import dev.zeropng.essentialscore.storage.DataStore;
import dev.zeropng.essentialscore.util.AtomicFiles;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

public final class RankManager {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final MiniMessage STRICT_MINI_MESSAGE = MiniMessage.builder().strict(true).build();
    private static final Map<String, String> NAMED_COLORS = Map.ofEntries(
            Map.entry("black", "#000000"), Map.entry("dark_blue", "#0000AA"),
            Map.entry("dark_green", "#00AA00"), Map.entry("dark_aqua", "#00AAAA"),
            Map.entry("dark_red", "#AA0000"), Map.entry("dark_purple", "#AA00AA"),
            Map.entry("gold", "#FFAA00"), Map.entry("gray", "#AAAAAA"),
            Map.entry("dark_gray", "#555555"), Map.entry("blue", "#5555FF"),
            Map.entry("green", "#55FF55"), Map.entry("aqua", "#55FFFF"),
            Map.entry("red", "#FF5555"), Map.entry("light_purple", "#FF55FF"),
            Map.entry("yellow", "#FFFF55"), Map.entry("white", "#FFFFFF")
    );

    private final JavaPlugin plugin;
    private final DataStore store;
    private final File file;
    private final YamlConfiguration ranksFile;
    private final Map<String, RankData> ranks = new ConcurrentHashMap<>();
    private final Map<UUID, String> assignments = new ConcurrentHashMap<>();
    private String defaultRank;

    public RankManager(JavaPlugin plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
        this.file = new File(plugin.getDataFolder(), "ranks.yml");
        if (!file.exists()) plugin.saveResource("ranks.yml", false);
        this.ranksFile = YamlConfiguration.loadConfiguration(file);
        load();
        loadAssignments();
    }

    private void loadAssignments() {
        assignments.clear();
        ConfigurationSection players = store.data().getConfigurationSection("players");
        if (players == null) return;
        for (String key : players.getKeys(false)) {
            try {
                String rank = players.getString(key + ".rank");
                if (rank != null) assignments.put(UUID.fromString(key), rank.toLowerCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Skipping invalid player UUID while loading Rank assignments: " + key);
            }
        }
    }

    private void load() {
        ranks.clear();
        ConfigurationSection section = ranksFile.getConfigurationSection("ranks");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection rank = section.getConfigurationSection(id);
                if (rank == null || !validId(id)) continue;
                String name = rank.getString("display-name", id);
                String prefix = rank.getString("prefix", "");
                String color = rank.getString("color", "#FFFFFF");
                if (!validName(name) || !validPrefix(prefix) || !validColor(color)) {
                    plugin.getLogger().warning("Skipping invalid Rank definition: " + id);
                    continue;
                }
                ranks.put(id, new RankData(id, name, prefix, color));
            }
        }
        defaultRank = ranksFile.getString("default-rank", "member").toLowerCase(Locale.ROOT);
        if (!ranks.containsKey(defaultRank)) {
            if (!ranks.containsKey("member")) {
                ranks.put("member", new RankData("member", "Member", "<gray>[Member]</gray> ", "#AAAAAA"));
            }
            defaultRank = "member";
            saveRanks();
        }
    }

    public List<RankData> all() {
        return ranks.values().stream().sorted(Comparator.comparing(RankData::id)).toList();
    }

    public RankData get(String id) {
        return id == null ? null : ranks.get(id.toLowerCase(Locale.ROOT));
    }

    public RankData defaultRank() {
        return ranks.get(defaultRank);
    }

    public String assignedId(UUID playerId) {
        String id = assignments.get(playerId);
        return get(id) == null ? defaultRank : id.toLowerCase(Locale.ROOT);
    }

    public RankData assigned(UUID playerId) {
        return get(assignedId(playerId));
    }

    public void assign(UUID playerId, String rankId) {
        String normalized = rankId.toLowerCase(Locale.ROOT);
        assignments.put(playerId, normalized);
        store.player(playerId, true).set("rank", normalized);
        store.save();
    }

    public boolean create(String id, String displayName) {
        id = id.toLowerCase(Locale.ROOT);
        if (!validId(id) || !validName(displayName) || ranks.containsKey(id)) return false;
        String prefix = "<white>[" + MiniMessage.miniMessage().escapeTags(displayName) + "]</white> ";
        RankData created = new RankData(id, displayName, prefix, "#FFFFFF");
        ranks.put(id, created);
        saveRanks();
        ConfigurationSection persisted = YamlConfiguration.loadConfiguration(file)
                .getConfigurationSection("ranks." + id);
        if (persisted != null
                && displayName.equals(persisted.getString("display-name"))
                && prefix.equals(persisted.getString("prefix"))) return true;

        ranks.remove(id, created);
        plugin.getLogger().severe("Rank creation could not be verified in ranks.yml: " + id);
        return false;
    }

    public boolean editName(String id, String name) {
        RankData rank = get(id);
        if (rank == null || !validName(name)) return false;
        ranks.put(rank.id(), new RankData(rank.id(), name, rank.prefix(), rank.color()));
        saveRanks();
        return true;
    }

    public boolean editPrefix(String id, String prefix) {
        RankData rank = get(id);
        if (rank == null || !validPrefix(prefix)) return false;
        ranks.put(rank.id(), new RankData(rank.id(), rank.displayName(), prefix, rank.color()));
        saveRanks();
        return true;
    }

    public boolean editColor(String id, String color) {
        RankData rank = get(id);
        if (rank == null || !validColor(color)) return false;
        ranks.put(rank.id(), new RankData(rank.id(), rank.displayName(), rank.prefix(), color));
        saveRanks();
        return true;
    }

    public boolean setDefault(String id) {
        RankData rank = get(id);
        if (rank == null) return false;
        defaultRank = rank.id();
        saveRanks();
        return true;
    }

    public int delete(String id) {
        RankData rank = get(id);
        if (rank == null || rank.id().equals(defaultRank)) return -1;
        ranks.remove(rank.id());
        int changed = 0;
        ConfigurationSection players = store.data().getConfigurationSection("players");
        if (players != null) {
            for (String playerId : players.getKeys(false)) {
                if (rank.id().equalsIgnoreCase(players.getString(playerId + ".rank", ""))) {
                    players.set(playerId + ".rank", defaultRank);
                    try {
                        assignments.put(UUID.fromString(playerId), defaultRank);
                    } catch (IllegalArgumentException ignored) {
                        // Invalid UUID entries are already ignored by the data loader.
                    }
                    changed++;
                }
            }
        }
        store.save();
        saveRanks();
        return changed;
    }

    private void saveRanks() {
        ranksFile.set("default-rank", defaultRank);
        ranksFile.set("ranks", null);
        for (RankData rank : ranks.values()) {
            String path = "ranks." + rank.id();
            ranksFile.set(path + ".display-name", rank.displayName());
            ranksFile.set(path + ".prefix", rank.prefix());
            ranksFile.set(path + ".color", rank.color());
        }
        try {
            AtomicFiles.writeString(file.toPath(), ranksFile.saveToString());
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save ranks.yml", exception);
        }
    }

    public String placeholderRank(UUID playerId) {
        return assigned(playerId).displayName();
    }

    public String placeholderPrefix(UUID playerId) {
        return LegacyComponentSerializer.legacySection().serialize(assigned(playerId).prefixComponent());
    }

    public static boolean validId(String id) {
        return id != null && ID_PATTERN.matcher(id).matches();
    }

    public static boolean validName(String name) {
        if (name == null) return false;
        int length = name.codePointCount(0, name.length());
        return length >= 1 && length <= 32;
    }

    public static boolean validPrefix(String prefix) {
        if (prefix == null || prefix.length() > 128) return false;
        try {
            STRICT_MINI_MESSAGE.deserialize(prefix);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static boolean validColor(String color) {
        return parseColorOrNull(color) != null;
    }

    public static TextColor parseColor(String color) {
        TextColor parsed = parseColorOrNull(color);
        return parsed == null ? TextColor.color(0xFFFFFF) : parsed;
    }

    private static TextColor parseColorOrNull(String color) {
        if (color == null) return null;
        String normalized = color.toLowerCase(Locale.ROOT);
        String hex = NAMED_COLORS.getOrDefault(normalized, color);
        try {
            if (!hex.matches("#[0-9a-fA-F]{6}")) return null;
            return TextColor.color(Integer.parseInt(hex.substring(1), 16));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
