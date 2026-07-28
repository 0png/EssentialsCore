package dev.zeropng.essentialscore.warp;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarpDataTest {
    @Test
    void roundTripsThroughYaml() {
        WarpData expected = new WarpData("shop", "公共商店", Material.EMERALD,
                UUID.randomUUID(), "world", 12.5, 70, -4.25, 90, 10);
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("warps.shop");
        expected.write(section);

        assertEquals(expected, WarpData.read("shop", yaml.getConfigurationSection("warps.shop"), ignored -> true));
    }

    @Test
    void validatesIdsNamesAndCorruptData() {
        assertTrue(WarpManager.validId("public_shop-2"));
        assertFalse(WarpManager.validId("Public Shop"));
        assertTrue(WarpManager.validDisplayName("公共商店"));
        assertFalse(WarpManager.validDisplayName(""));
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("warp.world-uuid", "broken");
        assertNull(WarpData.read("broken", yaml.getConfigurationSection("warp"), ignored -> true));
    }
}
