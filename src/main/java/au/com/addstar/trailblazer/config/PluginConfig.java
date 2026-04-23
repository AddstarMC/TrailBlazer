package au.com.addstar.trailblazer.config;

import au.com.addstar.trailblazer.TrailblazerPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;

public class PluginConfig {
    private final TrailblazerPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PluginConfig(TrailblazerPlugin plugin) {
        this.plugin = plugin;
    }

    public Component getMessage(String path) {
        // Call getMessage with empty map (makes it easier to use with no placeholders)
        return getMessage(path, Map.of());
    }

    public Component getMessage(String path, Map<String, String> placeholders) {
        String raw = plugin.getConfig().getString(path, "<red>Message not found: " + path + "</red>");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("<" + entry.getKey() + ">", escape(entry.getValue()));
        }
        return miniMessage.deserialize(raw);
    }

    public Component getMessage(String path, String placeholder, String value) {
        String raw = plugin.getConfig().getString(path, "<red>Message not found: " + path + "</red>");
        raw = raw.replace("<" + placeholder + ">", escape(value));
        return miniMessage.deserialize(raw);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("<", "\\<");
    }
}