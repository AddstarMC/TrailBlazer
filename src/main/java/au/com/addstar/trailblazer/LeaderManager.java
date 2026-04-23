package au.com.addstar.trailblazer;

import au.com.addstar.trailblazer.config.PluginConfig;
import au.com.addstar.trailblazer.util.DebugLogger;
import au.com.addstar.trailblazer.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderManager {
    private final TrailblazerPlugin plugin;
    private final PluginConfig config;
    private final Set<UUID> permanentLeaders = new HashSet<>();
    private final Set<UUID> temporaryLeaders = new HashSet<>();
    private final Map<UUID, Integer> scheduledTasks = new HashMap<>();
    private final Random random = new Random();
    private UUID autoLeaderUuid = null;

    public LeaderManager(TrailblazerPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
        List<String> list = plugin.getConfig().getStringList("permanent-leaders");
        for (String uuidStr : list) {
            try {
                permanentLeaders.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in config: " + uuidStr);
            }
        }
    }

    public boolean isLeader(UUID uuid) {
        return permanentLeaders.contains(uuid) || temporaryLeaders.contains(uuid);
    }

    public boolean addTemporaryLeader(UUID uuid, long durationSecs, String reason) {
        if (isLeader(uuid)) return false;

        temporaryLeaders.add(uuid);

        if (durationSecs > 0) {
            int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(
                    TrailblazerPlugin.getInstance(),
                    () -> removeTemporaryLeader(uuid, true),
                    durationSecs * 20L
            );
            scheduledTasks.put(uuid, taskId);
        }

        return true;
    }

    public boolean removeTemporaryLeader(UUID uuid, boolean broadcast) {
        boolean removed = temporaryLeaders.remove(uuid);
        if (removed) {
            Integer task = scheduledTasks.remove(uuid);
            if (task != null) Bukkit.getScheduler().cancelTask(task);

            if (broadcast) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                if (player.hasPlayedBefore()) {
                    String name = player.getName() != null ? player.getName() : uuid.toString();
                    Component chat = config.getMessage("messages.temp-leader-removed", Map.of(
                            "player", name
                    ));
                    Bukkit.getServer().sendMessage(chat);
                }
            }
        }
        return removed;
    }

    public Set<UUID> getTemporaryLeaders() {
        return Set.copyOf(temporaryLeaders);
    }

    public boolean addPermanentLeader(UUID uuid) {
        if (isLeader(uuid)) return false;

        permanentLeaders.add(uuid);

        List<String> list = plugin.getConfig().getStringList("permanent-leaders");
        list.add(uuid.toString());
        plugin.getConfig().set("permanent-leaders", list);
        plugin.saveConfig();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(MessageUtil.mm("<gold>You are now a permanent leader!"));
        }

        return true;
    }

    public boolean isPermanent(UUID uuid) {
        return permanentLeaders.contains(uuid);
    }

    public Set<UUID> getPermanentLeaders() {
        return Set.copyOf(permanentLeaders);
    }

    public boolean removePermanentLeader(UUID uuid, boolean broadcast) {
        boolean removed = permanentLeaders.remove(uuid);
        if (removed) {
            List<String> list = plugin.getConfig().getStringList("permanent-leaders");
            list.remove(uuid.toString());
            plugin.getConfig().set("permanent-leaders", list);
            plugin.saveConfig();

            if (broadcast) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                if (player.hasPlayedBefore()) {
                    String name = player.getName() != null ? player.getName() : uuid.toString();
                    Component chat = config.getMessage("messages.perm-leader-removed", Map.of(
                            "player", name
                    ));
                    Bukkit.getServer().sendMessage(chat);
                }
            }
        }
        return removed;
    }

    public void reloadFromConfig() {
        permanentLeaders.clear();
        List<String> list = plugin.getConfig().getStringList("permanent-leaders");
        for (String uuidStr : list) {
            try {
                permanentLeaders.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in config: " + uuidStr);
            }
        }
    }

    /**
     * Selects a random online player as the new leader.
     * @return the chosen Player, or null if no one is online
     */
    public Player selectRandomLeader(boolean allowRepeatLeader) {
        // Get all online players who are not in creative or spectator mode
        List<Player> candidates = Bukkit.getOnlinePlayers().stream()
                .filter(p -> {
                    GameMode gm = p.getGameMode();
                    return gm != GameMode.CREATIVE && gm != GameMode.SPECTATOR;
                })
                .collect(Collectors.toList());

        if (!allowRepeatLeader && autoLeaderUuid != null) {
            DebugLogger.debug("Removing current auto-leader from candidates: " + autoLeaderUuid);
            candidates.removeIf(p -> p.getUniqueId().equals(autoLeaderUuid));
        }

        if (candidates.isEmpty()) {
            plugin.getLogger().warning("No online players available for leader selection.");
            return null;
        }

        // Pick one at random
        Player chosen = candidates.get(random.nextInt(candidates.size()));

        plugin.getLogger().info("New leader selected: " + chosen.getName());
        return chosen;
    }

    public UUID getAutoLeaderUuid() {
        return autoLeaderUuid;
    }

    public Player getAutoLeaderPlayer() {
        return autoLeaderUuid != null ? Bukkit.getPlayer(autoLeaderUuid) : null;
    }

    public void setAutoLeader(UUID uuid) {
        this.autoLeaderUuid = uuid;
    }

    public void clearAutoLeader() {
        this.autoLeaderUuid = null;
    }
}
