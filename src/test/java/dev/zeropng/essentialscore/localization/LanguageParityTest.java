package dev.zeropng.essentialscore.localization;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LanguageParityTest {
    @Test
    void englishAndTraditionalChineseContainTheSameKeys() throws Exception {
        File en = resource("lang/en.yml");
        File zh = resource("lang/zh_TW.yml");
        Yaml yaml = new Yaml();
        Map<String, Object> english = yaml.load(Files.readString(en.toPath()));
        Map<String, Object> chinese = yaml.load(Files.readString(zh.toPath()));
        assertEquals(english.keySet(), chinese.keySet());
        assertNotNull(chinese.get("menu.home.delete-confirm"));
        assertNotNull(english.get("pet.protected"));
    }

    private static File resource(String name) throws URISyntaxException {
        var url = LanguageParityTest.class.getClassLoader().getResource(name);
        assertNotNull(url);
        return Path.of(url.toURI()).toFile();
    }
}
