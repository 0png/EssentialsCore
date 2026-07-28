package dev.zeropng.essentialscore.config;

import dev.zeropng.essentialscore.util.AtomicFiles;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

public final class PluginSettings {
    private final JavaPlugin plugin;
    private final FileConfiguration config;

    public PluginSettings(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        applyDefaults();
    }

    private void applyDefaults() {
        config.addDefault("language", "zh-TW");
        config.addDefault("home.max-homes", 3);
        config.addDefault("home.cooldown-seconds", 30);
        config.addDefault("home.teleport-delay-seconds", 3);
        config.addDefault("tpa.request-expiry-seconds", 60);
        config.addDefault("tpa.send-cooldown-seconds", 30);
        config.addDefault("tpa.teleport-delay-seconds", 3);
        config.addDefault("warp.cooldown-seconds", 30);
        config.addDefault("warp.teleport-delay-seconds", 3);
        config.addDefault("pet.protection-enabled", true);
        config.addDefault("input.timeout-seconds", 60);
        config.options().copyDefaults(true);
    }

    public String language() {
        String value = config.getString("language", "zh-TW");
        if (value.equalsIgnoreCase("en")) return "en";
        if (!value.equalsIgnoreCase("zh-TW")) {
            plugin.getLogger().warning("Unsupported language '" + value + "'; falling back to zh-TW.");
        }
        return "zh-TW";
    }

    public int maxHomes() { return bounded("home.max-homes", 3, 1, 100); }
    public int homeCooldown() { return bounded("home.cooldown-seconds", 30, 0, 86400); }
    public int homeDelay() { return bounded("home.teleport-delay-seconds", 3, 0, 300); }
    public int tpaExpiry() { return bounded("tpa.request-expiry-seconds", 60, 5, 600); }
    public int tpaCooldown() { return bounded("tpa.send-cooldown-seconds", 30, 0, 86400); }
    public int tpaDelay() { return bounded("tpa.teleport-delay-seconds", 3, 0, 300); }
    public int warpCooldown() { return bounded("warp.cooldown-seconds", 30, 0, 86400); }
    public int warpDelay() { return bounded("warp.teleport-delay-seconds", 3, 0, 300); }
    public boolean petProtection() { return config.getBoolean("pet.protection-enabled", true); }
    public int inputTimeout() { return bounded("input.timeout-seconds", 60, 10, 600); }

    private int bounded(String path, int fallback, int minimum, int maximum) {
        int value = config.getInt(path, fallback);
        return Math.max(minimum, Math.min(maximum, value));
    }

    public void setInt(String path, int value) {
        config.set(path, value);
        save();
    }

    public void setBoolean(String path, boolean value) {
        config.set(path, value);
        save();
    }

    public void save() {
        Path target = plugin.getDataFolder().toPath().resolve("config.yml");
        try {
            AtomicFiles.writeString(target, config.saveToString());
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", exception);
        }
    }
}
