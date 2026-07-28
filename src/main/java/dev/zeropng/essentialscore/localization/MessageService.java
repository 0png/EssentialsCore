package dev.zeropng.essentialscore.localization;

import dev.zeropng.essentialscore.config.PluginSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.logging.Level;

public final class MessageService {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final YamlConfiguration selected;
    private final YamlConfiguration english;

    public MessageService(JavaPlugin plugin, PluginSettings settings) {
        this.plugin = plugin;
        ensureLanguage("en");
        ensureLanguage("zh_TW");
        this.english = YamlConfiguration.loadConfiguration(languageFile("en"));
        String chosen = settings.language().equals("en") ? "en" : "zh_TW";
        this.selected = YamlConfiguration.loadConfiguration(languageFile(chosen));
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

    public Component component(String key) {
        return component(key, Map.of());
    }

    public Component component(String key, Map<String, ?> values) {
        String source = selected.getString(key);
        if (source == null) source = english.getString(key);
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
