package dev.zeropng.essentialscore.pet;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.UUID;

public record PetRecord(UUID entityId, UUID ownerId, EntityType type, String name,
                        UUID worldId, String worldName, double x, double y, double z,
                        int chunkX, int chunkZ) {
    public World world() {
        World world = Bukkit.getWorld(worldId);
        return world == null && worldName != null ? Bukkit.getWorld(worldName) : world;
    }

    public static PetRecord read(String key, ConfigurationSection section) {
        try {
            String owner = section.getString("owner-uuid");
            String world = section.getString("world-uuid");
            String type = section.getString("type");
            if (owner == null || world == null || type == null) return null;
            return new PetRecord(UUID.fromString(key), UUID.fromString(owner), EntityType.valueOf(type),
                    section.getString("name"), UUID.fromString(world), section.getString("world-name"),
                    section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                    section.getInt("chunk-x"), section.getInt("chunk-z"));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
