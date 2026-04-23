package au.com.addstar.trailblazer.ui;

import au.com.addstar.trailblazer.TrailblazerPlugin;
import au.com.addstar.trailblazer.WhitelistManager;
import au.com.addstar.trailblazer.config.PluginConfig;
import au.com.addstar.trailblazer.util.DebugLogger;
import au.com.addstar.trailblazer.util.MessageUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

public class UnlockProgressBar {
    private final TrailblazerPlugin plugin;
    private final PluginConfig config;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final BossBar bossBar;
    private BukkitTask hideTask;

    public UnlockProgressBar(TrailblazerPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
        this.bossBar = BossBar.bossBar(Component.empty(), 0f, Color.PURPLE, Overlay.PROGRESS);
    }

    public void showUnlock(String playerName, String action, Object type) {
        WhitelistManager whitelist = plugin.getWhitelistManager();

        int total = Material.values().length;
        int unlocked = whitelist.getAllWhitelistedMaterials().size();
        float progress = (float) unlocked / total;

        // Humanize the unlocked type (block/item/entity)
        String itemName = (type instanceof Material mat)
                ? MessageUtil.humanizeEnum(mat)
                : (type instanceof EntityType ent)
                ? MessageUtil.humanizeEnum(ent)
                : type.toString();

        String actionName = MessageUtil.humanizeEnumName(action);
        DebugLogger.debug("UnlockProgress/success", "Player " + playerName + " unlocked " + actionName + " for " + itemName);

        // Chat message
        Component chat = config.getMessage("messages.chat-unlock", Map.of(
                "player", playerName,
                "item", itemName,
                "action", actionName
        ));
        Bukkit.getServer().sendMessage(chat);

        // Boss bar message
        Component title = config.getMessage("messages.bossbar-unlock", Map.of(
                "player", playerName,
                "item", itemName,
                "action", actionName
        ));

        bossBar.name(title);
        bossBar.progress(progress);
        bossBar.color(Color.PURPLE);
        bossBar.overlay(Overlay.PROGRESS);

        Bukkit.getOnlinePlayers().forEach(p -> p.showBossBar(bossBar));

        // Cancel previous hide task if needed
        if (hideTask != null) hideTask.cancel();

        // Auto-hide after 6 seconds
        hideTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(bossBar));
        }, 20 * 6);
    }
}