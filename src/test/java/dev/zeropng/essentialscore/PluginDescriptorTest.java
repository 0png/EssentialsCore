package dev.zeropng.essentialscore;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PluginDescriptorTest {
    @Test
    void declaresEveryPublicCommandExceptRtp() throws Exception {
        var url = getClass().getClassLoader().getResource("plugin.yml");
        assertNotNull(url);
        File file = Path.of(url.toURI()).toFile();
        Map<String, Object> yaml = new Yaml().load(Files.readString(file.toPath()));
        @SuppressWarnings("unchecked")
        Map<String, Object> commands = (Map<String, Object>) yaml.get("commands");
        assertNotNull(commands);
        assertEquals(Set.of("ec", "home", "sethome", "tpa", "tpahere", "tpaccept", "tpdeny",
                "pet", "sit", "lay", "hat", "back", "trash", "warp", "rank"), commands.keySet());
        assertFalse(commands.containsKey("rtp"));
        assertEquals("1.3.0", yaml.get("version"));
        assertEquals("26.2", yaml.get("api-version"));
        assertTrue(((List<?>) yaml.get("softdepend")).contains("PlaceholderAPI"));
    }
}
