package au.com.addstar.trailblazer.listeners;

import au.com.addstar.trailblazer.TrailblazerPlugin;
import au.com.addstar.trailblazer.WhitelistManager;
import au.com.addstar.trailblazer.util.DebugLogger;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {
    private final TrailblazerPlugin plugin;

    public BlockBreakListener(TrailblazerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        boolean isLeader = plugin.getLeaderManager().isLeader(player.getUniqueId());
        Material type = event.getBlock().getType();
        WhitelistManager whitelist = plugin.getWhitelistManager();

        if (!whitelist.isBreakAllowed(type)) {
            if (isLeader) {
                if (whitelist.addBreak(type)) {
                    whitelist.saveWhitelist();
                    plugin.getProgressBar().showUnlock(player.getName(), "break", type);
                }
            } else {
                event.setCancelled(true);
                player.sendActionBar(plugin.getPluginConfig().getMessage("messages.block-break-not-allowed"));
                DebugLogger.debug("BlockBreak/fail", "Player " + player.getName() + " tried to break " + type);
            }
        }
    }
}
