package dev.zeropng.essentialscore.warp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;
import java.util.function.Predicate;

public record WarpData(String id, String displayName, Material icon, UUID worldId, String worldName,
                       double x, double y, double z, float yaw, float pitch) {
    public Location location() {
        World world = Bukkit.getWorld(worldId);
        if (world == null && worldName != null) world = Bukkit.getWorld(worldName);
        return world == null ? null : new Location(world, x, y, z, yaw, pitch);
    }

    public void write(ConfigurationSection section) {
        section.set("display-name", displayName);
        section.set("icon", icon.name());
        section.set("world-uuid", worldId.toString());
        section.set("world-name", worldName);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("yaw", yaw);
        section.set("pitch", pitch);
    }

    public static WarpData at(String id, String displayName, Material icon, Location location) {
        return new WarpData(id, displayName, icon, location.getWorld().getUID(), location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static WarpData read(String id, ConfigurationSection section) {
        return read(id, section, material -> material.isItem() && !material.isAir());
    }

    static WarpData read(String id, ConfigurationSection section, Predicate<Material> validIcon) {
        try {
            String worldId = section.getString("world-uuid");
            if (worldId == null) return null;
            Material icon = Material.matchMaterial(section.getString("icon", "ENDER_PEARL"));
            if (icon == null || !validIcon.test(icon)) icon = Material.ENDER_PEARL;
            return new WarpData(id, section.getString("display-name", id), icon, UUID.fromString(worldId),
                    section.getString("world-name"), section.getDouble("x"), section.getDouble("y"),
                    section.getDouble("z"), (float) section.getDouble("yaw"),
                    (float) section.getDouble("pitch"));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
