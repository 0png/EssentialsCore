package dev.zeropng.essentialscore.storage;

import dev.zeropng.essentialscore.util.AtomicFiles;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class DataStore {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public DataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) plugin.saveResource("data.yml", false);
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public YamlConfiguration data() {
        return data;
    }

    public ConfigurationSection player(UUID playerId, boolean create) {
        String path = "players." + playerId;
        ConfigurationSection section = data.getConfigurationSection(path);
        return section == null && create ? data.createSection(path) : section;
    }

    public void recordName(UUID playerId, String name) {
        player(playerId, true).set("last-known-name", name);
    }

    public UUID findKnownPlayer(String name) {
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) return null;
        for (String key : players.getKeys(false)) {
            String knownName = players.getString(key + ".last-known-name");
            if (knownName != null && knownName.equalsIgnoreCase(name)) {
                try {
                    return UUID.fromString(key);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public Map<UUID, String> knownPlayers() {
        Map<UUID, String> result = new HashMap<>();
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) return result;
        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String name = players.getString(key + ".last-known-name");
                if (name != null) result.put(uuid, name);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Skipping invalid player UUID in data.yml: " + key);
            }
        }
        return result;
    }

    public synchronized void save() {
        try {
            AtomicFiles.writeString(file.toPath(), data.saveToString());
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", exception);
        }
    }
}
