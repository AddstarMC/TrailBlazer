package au.com.addstar.trailblazer;

import au.com.addstar.trailblazer.commands.TrailblazerCommand;
import au.com.addstar.trailblazer.config.PluginConfig;
import au.com.addstar.trailblazer.listeners.*;
import au.com.addstar.trailblazer.ui.AutoLeaderBar;
import au.com.addstar.trailblazer.ui.UnlockProgressBar;
import au.com.addstar.trailblazer.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class TrailblazerPlugin extends JavaPlugin {

    private static TrailblazerPlugin instance;
    private WhitelistManager whitelistManager;
    private LeaderManager leaderManager;
    private UnlockProgressBar progressBar;
    private AutoLeaderBar autoLeaderBar;
    private PluginConfig pluginConfig;
    private boolean debugEnabled = false;

    // TODO: Add Village type trading whitelist

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        pluginConfig = new PluginConfig(this);
        whitelistManager = new WhitelistManager(this);
        leaderManager = new LeaderManager(this);
        whitelistManager.loadWhitelist();
        progressBar = new UnlockProgressBar(this);
        autoLeaderBar = new AutoLeaderBar(this);

        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftItemListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PassiveMobKillListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalUseListener(this), this);
        getCommand("trailblazer").setExecutor(new TrailblazerCommand(this));

        getServer().getConsoleSender().sendMessage(
                MessageUtil.mm("<green><bold>Trailblazer enabled!</bold></green>")
        );
    }

    @Override
    public void onDisable() {
        if (autoLeaderBar != null) {
            autoLeaderBar.stopTimer();
        }
        getServer().getScheduler().cancelTasks(this);
        whitelistManager.flushSave();
    }

    public void reload() {
        reloadConfig();
        pluginConfig = new PluginConfig(this);
        leaderManager.reloadFromConfig();

        // Reload whitelist from disk
        whitelistManager.loadWhitelist();

        getServer().getConsoleSender().sendMessage(
                MessageUtil.mm("<green>Trailblazer config and whitelist reloaded.</green>")
        );
    }

    public static TrailblazerPlugin getInstance() {
        return instance;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
    }

    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    public LeaderManager getLeaderManager() {
        return leaderManager;
    }

    public AutoLeaderBar getAutoLeaderBar() {
        return autoLeaderBar;
    }

    public UnlockProgressBar getProgressBar() {
        return progressBar;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }
}
