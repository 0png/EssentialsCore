package dev.zeropng.essentialscore.localization;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageServiceTest {
    @Test
    void fallsBackToBundledLanguageWhenInstalledFileIsOutdated() {
        YamlConfiguration installed = new YamlConfiguration();
        installed.set("menu.main.home", "自訂 Home");

        YamlConfiguration bundled = new YamlConfiguration();
        bundled.set("menu.main.help", "指令說明");

        assertEquals("指令說明",
                MessageService.findSource("menu.main.help", installed, bundled));
    }

    @Test
    void keepsServerCustomisationsAheadOfBundledDefaults() {
        YamlConfiguration installed = new YamlConfiguration();
        installed.set("back.death-title", "自訂死亡標題");

        YamlConfiguration bundled = new YamlConfiguration();
        bundled.set("back.death-title", "預設死亡標題");

        assertEquals("自訂死亡標題",
                MessageService.findSource("back.death-title", installed, bundled));
    }

    @Test
    void returnsNullOnlyWhenNoLanguageContainsTheKey() {
        assertNull(MessageService.findSource("missing.key", new YamlConfiguration()));
    }
}
