package au.com.addstar.trailblazer.listeners;

import au.com.addstar.trailblazer.TrailblazerPlugin;
import au.com.addstar.trailblazer.util.DebugLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerQuitListener implements Listener {
    private final TrailblazerPlugin plugin;

    public PlayerQuitListener(TrailblazerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("select-new-leader-on-quit", false)) {
            UUID autoLeaderUuid = plugin.getLeaderManager().getAutoLeaderUuid();
            if (plugin.getAutoLeaderBar().isRunning() && player.getUniqueId().equals(autoLeaderUuid)) {
                DebugLogger.debug("Auto-leader left - Selecting new leader");
                plugin.getServer().sendMessage(
                        plugin.getPluginConfig().getMessage("messages.auto-leader-left")
                );
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.getAutoLeaderBar().chooseNewLeader();
                }, 1L);
            }
        }
    }
}
