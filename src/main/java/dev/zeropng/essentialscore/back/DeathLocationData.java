package dev.zeropng.essentialscore.back;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

public record DeathLocationData(UUID worldId, String worldName, double x, double y, double z,
                                float yaw, float pitch) {
    public static DeathLocationData from(Location location) {
        return new DeathLocationData(location.getWorld().getUID(), location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public Location location() {
        World world = Bukkit.getWorld(worldId);
        if (world == null && worldName != null) world = Bukkit.getWorld(worldName);
        return world == null ? null : new Location(world, x, y, z, yaw, pitch);
    }

    public void write(ConfigurationSection section) {
        section.set("world-uuid", worldId.toString());
        section.set("world-name", worldName);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("yaw", yaw);
        section.set("pitch", pitch);
    }

    public static DeathLocationData read(ConfigurationSection section) {
        if (section == null) return null;
        try {
            String worldId = section.getString("world-uuid");
            if (worldId == null) return null;
            return new DeathLocationData(UUID.fromString(worldId), section.getString("world-name"),
                    section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                    (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
