package au.com.addstar.trailblazer.listeners;

import au.com.addstar.trailblazer.TrailblazerPlugin;
import au.com.addstar.trailblazer.WhitelistManager;
import au.com.addstar.trailblazer.util.DebugLogger;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class PortalUseListener implements Listener {
    private final TrailblazerPlugin plugin;

    public PortalUseListener(TrailblazerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        boolean isLeader = plugin.getLeaderManager().isLeader(player.getUniqueId());
        WhitelistManager whitelist = plugin.getWhitelistManager();

        String key = "PORTAL_" + getPortalType(event);
        if (!whitelist.isUseAllowed(key)) {
            if (isLeader) {
                if (whitelist.addUse(key)) {
                    whitelist.saveWhitelist();
                    plugin.getProgressBar().showUnlock(player.getName(), "portal", key);
                }
            } else {
                event.setCancelled(true);
                player.sendActionBar(plugin.getPluginConfig().getMessage("messages.portal-not-allowed"));
                DebugLogger.debug("PortalUse/fail", "Player " + player.getName() + " tried to enter " + key);
            }
        }
    }

    private String getPortalType(PlayerPortalEvent event) {
        if (event.getTo() == null || event.getTo().getWorld() == null) return "OTHER";
        return switch (event.getTo().getWorld().getEnvironment()) {
            case NETHER -> "NETHER";
            case THE_END -> "END";
            default -> "OTHER";
        };
    }
}
