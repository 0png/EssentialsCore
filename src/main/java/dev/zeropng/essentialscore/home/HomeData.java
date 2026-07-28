package dev.zeropng.essentialscore.home;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

public record HomeData(String key, String name, UUID worldId, String worldName,
                       double x, double y, double z, float yaw, float pitch) {
    public Location location() {
        World world = Bukkit.getWorld(worldId);
        if (world == null && worldName != null) world = Bukkit.getWorld(worldName);
        return world == null ? null : new Location(world, x, y, z, yaw, pitch);
    }

    public static HomeData read(String key, ConfigurationSection section) {
        try {
            String worldId = section.getString("world-uuid");
            if (worldId == null) return null;
            return new HomeData(key, section.getString("name", key), UUID.fromString(worldId),
                    section.getString("world-name"), section.getDouble("x"), section.getDouble("y"),
                    section.getDouble("z"), (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
