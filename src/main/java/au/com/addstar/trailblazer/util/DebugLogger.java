package au.com.addstar.trailblazer.util;

import au.com.addstar.trailblazer.TrailblazerPlugin;
import org.bukkit.Bukkit;

public class DebugLogger {
    public static void debug(String message) {
        if (TrailblazerPlugin.getInstance().isDebugEnabled()) {
            Bukkit.getLogger().info("[Trailblazer DEBUG] " + message);
        }
    }

    public static void debug(String context, String message) {
        if (TrailblazerPlugin.getInstance().isDebugEnabled()) {
            Bukkit.getLogger().info("[Trailblazer DEBUG] [" + context + "] " + message);
        }
    }
}