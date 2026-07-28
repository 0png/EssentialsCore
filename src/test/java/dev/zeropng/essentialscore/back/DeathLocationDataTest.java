package dev.zeropng.essentialscore.back;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeathLocationDataTest {
    @Test
    void roundTripsThroughYaml() {
        UUID worldId = UUID.randomUUID();
        DeathLocationData expected = new DeathLocationData(worldId, "world", 12.5, 64.0, -8.25, 90.0F, 12.0F);
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("last-death");
        expected.write(section);

        assertEquals(expected, DeathLocationData.read(yaml.getConfigurationSection("last-death")));
    }

    @Test
    void rejectsCorruptWorldUuid() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("last-death.world-uuid", "not-a-uuid");
        assertNull(DeathLocationData.read(yaml.getConfigurationSection("last-death")));
    }
}
