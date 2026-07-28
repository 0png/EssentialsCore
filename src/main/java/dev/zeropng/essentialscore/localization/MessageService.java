package dev.zeropng.essentialscore.localization;

import dev.zeropng.essentialscore.config.PluginSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;

public final class MessageService {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final YamlConfiguration selected;
    private final YamlConfiguration bundledSelected;
    private final YamlConfiguration english;
    private final YamlConfiguration bundledEnglish;

    public MessageService(JavaPlugin plugin, PluginSettings settings) {
        this.plugin = plugin;
        ensureLanguage("en");
        ensureLanguage("zh_TW");
        String chosen = settings.language().equals("en") ? "en" : "zh_TW";
        this.english = YamlConfiguration.loadConfiguration(languageFile("en"));
        this.selected = YamlConfiguration.loadConfiguration(languageFile(chosen));
        this.bundledEnglish = loadBundledLanguage("en");
        this.bundledSelected = chosen.equals("en") ? bundledEnglish : loadBundledLanguage(chosen);
    }

    private void ensureLanguage(String code) {
        File file = languageFile(code);
        if (!file.exists()) {
            plugin.saveResource("lang/" + code + ".yml", false);
        }
    }

    private File languageFile(String code) {
        return new File(plugin.getDataFolder(), "lang/" + code + ".yml");
    }

    private YamlConfiguration loadBundledLanguage(String code) {
        InputStream stream = plugin.getResource("lang/" + code + ".yml");
        if (stream == null) {
            plugin.getLogger().warning("Bundled language file is missing: lang/" + code + ".yml");
            return new YamlConfiguration();
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Could not load bundled language " + code, exception);
            return new YamlConfiguration();
        }
    }

    public Component component(String key) {
        return component(key, Map.of());
    }

    public Component component(String key, Map<String, ?> values) {
        // Installed language files may be from an older plugin version. Always keep the
        // current JAR resources in the fallback chain so newly added keys work without
        // deleting or overwriting a server owner's customised language files.
        String source = findSource(key, selected, bundledSelected, english, bundledEnglish);
        if (source == null) source = key;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            source = source.replace("<" + entry.getKey() + ">", String.valueOf(entry.getValue()));
        }
        try {
            return miniMessage.deserialize(source);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Invalid MiniMessage at language key " + key, exception);
            return Component.text(key);
        }
    }

    static String findSource(String key, YamlConfiguration... languages) {
        for (YamlConfiguration language : languages) {
            if (language == null) continue;
            String value = language.getString(key);
            if (value != null) return value;
        }
        return null;
    }

    public Component parse(String miniMessageText) {
        try {
            return miniMessage.deserialize(miniMessageText);
        } catch (RuntimeException exception) {
            return Component.text(miniMessageText);
        }
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, ?> values) {
        sender.sendMessage(component("prefix").append(component(key, values)));
    }
}
