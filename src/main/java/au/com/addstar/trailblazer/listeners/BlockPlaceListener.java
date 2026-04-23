package au.com.addstar.trailblazer.listeners;

import au.com.addstar.trailblazer.TrailblazerPlugin;
import au.com.addstar.trailblazer.WhitelistManager;
import au.com.addstar.trailblazer.util.DebugLogger;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {
    private final TrailblazerPlugin plugin;

    public BlockPlaceListener(TrailblazerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        Material type = event.getBlockPlaced().getType();
        boolean isLeader = plugin.getLeaderManager().isLeader(player.getUniqueId());
        WhitelistManager whitelist = plugin.getWhitelistManager();

        if (!whitelist.isPlaceAllowed(type)) {
            if (isLeader) {
                if (whitelist.addPlace(type)) {
                    whitelist.saveWhitelist();
                    plugin.getProgressBar().showUnlock(player.getName(), "place", type);
                }
            } else {
                event.setCancelled(true);
                player.sendActionBar(plugin.getPluginConfig().getMessage("messages.block-place-not-allowed"));
                DebugLogger.debug("BlockPlace/fail", "Player " + player.getName() + " tried to place " + type);
            }
        }
    }
}
