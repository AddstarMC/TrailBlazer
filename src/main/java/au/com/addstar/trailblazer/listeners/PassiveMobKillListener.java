package au.com.addstar.trailblazer.listeners;

import au.com.addstar.trailblazer.TrailblazerPlugin;
import au.com.addstar.trailblazer.WhitelistManager;
import au.com.addstar.trailblazer.util.DebugLogger;
import org.bukkit.GameMode;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PassiveMobKillListener implements Listener {
    private final TrailblazerPlugin plugin;

    public PassiveMobKillListener(TrailblazerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        EntityType type = target.getType();

        if (!isPassive(target)) return;

        boolean isLeader = plugin.getLeaderManager().isLeader(player.getUniqueId());
        WhitelistManager whitelist = plugin.getWhitelistManager();

        if (!whitelist.isMobKillAllowed(type)) {
            if (isLeader) {
                if (whitelist.addMob(type)) {
                    whitelist.saveWhitelist();
                    plugin.getProgressBar().showUnlock(player.getName(), "kill", type);
                }
            } else {
                event.setCancelled(true);
                player.sendActionBar(plugin.getPluginConfig().getMessage("messages.kill-not-allowed"));
                DebugLogger.debug("MobKill/fail", "Player " + player.getName() + " tried to kill " + type);
            }
        }
    }

    private boolean isPassive(LivingEntity entity) {
        return entity instanceof Animals
                || entity instanceof Ambient
                || entity instanceof WaterMob
                || entity instanceof Villager
                || entity instanceof Allay
                || entity.getType() == EntityType.SNIFFER;
    }
}
