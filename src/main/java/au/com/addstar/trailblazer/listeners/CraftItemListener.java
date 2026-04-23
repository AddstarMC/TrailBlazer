package au.com.addstar.trailblazer.listeners;

import au.com.addstar.trailblazer.TrailblazerPlugin;
import au.com.addstar.trailblazer.WhitelistManager;
import au.com.addstar.trailblazer.util.DebugLogger;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;

public class CraftItemListener implements Listener {
    private final TrailblazerPlugin plugin;

    public CraftItemListener(TrailblazerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        Material type = event.getRecipe().getResult().getType();

        boolean isLeader = plugin.getLeaderManager().isLeader(player.getUniqueId());
        WhitelistManager whitelist = plugin.getWhitelistManager();

        if (!whitelist.isCraftAllowed(type)) {
            if (isLeader) {
                if (whitelist.addCraft(type)) {
                    whitelist.saveWhitelist();
                    plugin.getProgressBar().showUnlock(player.getName(), "craft", type);
                }
            } else {
                event.setCancelled(true);
                player.sendActionBar(plugin.getPluginConfig().getMessage("messages.craft-not-allowed"));
                player.closeInventory();
                DebugLogger.debug("CraftItem/fail", "Player " + player.getName() + " tried to craft " + type);
            }
        }
    }
}
