package au.com.addstar.trailblazer.ui;

import au.com.addstar.trailblazer.TrailblazerPlugin;
import au.com.addstar.trailblazer.config.PluginConfig;
import au.com.addstar.trailblazer.util.DebugLogger;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class AutoLeaderBar {
    private final TrailblazerPlugin plugin;
    private final PluginConfig config;
    private final BossBar bossBar;
    private final Random random = new Random();

    // configurable bounds, in seconds
    private int minTime = 60;
    private int maxTime = 600;

    // state for the running countdown
    private int totalTime;
    private int remainingTime;
    private BukkitTask task;
    private BukkitTask selectionTask;
    private boolean isSelecting = false;

    public AutoLeaderBar(TrailblazerPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
        this.bossBar = BossBar.bossBar(Component.empty(), 1f, Color.BLUE, Overlay.PROGRESS);
        this.bossBar.progress(1f);
    }

    public boolean isRunning() {
        return task != null && !task.isCancelled();
    }

    /**
     * Start the repeating auto-leader timer.
     * @param minTimeSecs minimum interval (seconds)
     * @param maxTimeSecs maximum interval (seconds)
     * @return false if it was already running
     */
    public boolean startTimer(int minTimeSecs, int maxTimeSecs) {
        DebugLogger.debug("startTimer() was called");
        if (isRunning() || isSelecting) return false;

        if (minTimeSecs > maxTimeSecs) {
            int temp = minTimeSecs;
            minTimeSecs = maxTimeSecs;
            maxTimeSecs = temp;
        }
        this.minTime = Math.max(1, minTimeSecs);
        this.maxTime = Math.max(this.minTime, maxTimeSecs);
        chooseNewLeader();
        return true;
    }

    /**
     * Stop the auto-leader timer and hide the bar.
     * @return false if it wasn't running
     */
    public boolean stopTimer() {
        DebugLogger.debug("stopTimer() was called");
        if (!isRunning() && !isSelecting) return false;

        if (task != null) {
            task.cancel();
            task = null;
        }
        if (selectionTask != null) {
            selectionTask.cancel();
            selectionTask = null;
        }
        isSelecting = false;

        Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(bossBar));

        DebugLogger.debug("Removing current auto-leader");
        UUID autoLeaderUuid = plugin.getLeaderManager().getAutoLeaderUuid();
        if (autoLeaderUuid != null) {
            plugin.getLeaderManager().removeTemporaryLeader(autoLeaderUuid, false);
        }
        plugin.getLeaderManager().clearAutoLeader();

        return true;
    }

    // pick the next random interval and kick off a per-second task
    private void scheduleNextRun() {
        DebugLogger.debug("scheduleNextRun() was called");
        if (isRunning()) {
            task.cancel();
            task = null;
        }
        this.totalTime = (minTime == maxTime) ? minTime : random.nextInt(maxTime - minTime + 1) + minTime;
        this.remainingTime = totalTime;
        updateBar();

        // show to everyone right away
        Bukkit.getOnlinePlayers().forEach(p -> p.showBossBar(bossBar));

        // schedule tick every 20 ticks = 1s
        this.task = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            this::tick,
            20L,
            20L
        );
    }

    // called once per second
    private void tick() {
        if (!isRunning()) {
            // task was cancelled, so stop the countdown
            plugin.getLogger().warning("AutoLeaderBar task was cancelled, stopping countdown.");
            return;
        }

        remainingTime--;
        if (remainingTime <= 0) {
            DebugLogger.debug("Timer countdown has reached 0");
            chooseNewLeader();
        } else {
            // we just need to update the bossbar
            updateBar();
        }
    }

    private void updateBar() {
        if (!isSelecting) {
            Player leader = plugin.getLeaderManager().getAutoLeaderPlayer();
            String leaderName = leader != null ? leader.getName() : "None";
            float progress = totalTime > 0 ? (float) remainingTime / totalTime : 0f;
            bossBar.progress(Math.max(0f, Math.min(1f, progress)));
            bossBar.name(
                    config.getMessage("messages.auto-leader-bossbar", Map.of(
                            "player", leaderName,
                            "remaining", formatDuration(remainingTime)
                    ))
            );
        }
        Bukkit.getOnlinePlayers().forEach(p -> p.showBossBar(bossBar));
    }

    public void chooseNewLeader() {
        if (isSelecting) {
            DebugLogger.debug("chooseNewLeader() called while already selecting — ignoring");
            return;
        }

        if (task != null) {
            task.cancel();
            task = null;
        }
        if (selectionTask != null) {
            selectionTask.cancel();
            selectionTask = null;
        }

        isSelecting = true;
        UUID prevLeaderUuid = plugin.getLeaderManager().getAutoLeaderUuid();
        bossBar.progress(0f);
        bossBar.name(config.getMessage("messages.auto-leader-selecting"));
        updateBar();

        selectionTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            selectionTask = null;

            if (!isSelecting) {
                DebugLogger.debug("Selection task fired but isSelecting is false (timer was stopped)");
                return;
            }

            DebugLogger.debug("Timer countdown has reached 0, choosing new leader");
            UUID currentAutoUuid = plugin.getLeaderManager().getAutoLeaderUuid();
            if (currentAutoUuid != null) {
                DebugLogger.debug("Removing current auto-leader");
                plugin.getLeaderManager().removeTemporaryLeader(currentAutoUuid, false);
            }

            Player newLeader = plugin.getLeaderManager().selectRandomLeader(
                    plugin.getConfig().getBoolean("allow-repeat-leader", true)
            );
            if (newLeader == null) {
                plugin.getLogger().warning("No eligible leaders found - cancelling auto-leader.");
                isSelecting = false;
                stopTimer();
                bossBar.name(config.getMessage("messages.auto-leader-none"));
                Bukkit.getOnlinePlayers().forEach(p -> p.showBossBar(bossBar));
                return;
            }

            plugin.getLeaderManager().setAutoLeader(newLeader.getUniqueId());
            plugin.getLeaderManager().addTemporaryLeader(newLeader.getUniqueId(), maxTime + 10, null);
            isSelecting = false;

            Bukkit.getServer().sendMessage(
                config.getMessage("messages.auto-leader-chosen", Map.of(
                        "player", newLeader.getName()
                ))
            );

            if (newLeader.isOnline()) {
                boolean sameLeader = newLeader.getUniqueId().equals(prevLeaderUuid);
                if (!sameLeader) {
                    Player prevPlayer = prevLeaderUuid != null ? Bukkit.getPlayer(prevLeaderUuid) : null;
                    if (prevPlayer != null && prevPlayer.isOnline()) {
                        prevPlayer.showTitle(Title.title(
                                Component.text("Leader role lost!", NamedTextColor.RED),
                                Component.text("You are no longer the leader", NamedTextColor.DARK_RED),
                                Title.Times.times(
                                        Duration.ofMillis(800),
                                        Duration.ofSeconds(3),
                                        Duration.ofMillis(600)
                                )
                        ));
                    }

                    newLeader.playSound(
                            newLeader.getLocation(),
                            "minecraft:block.note_block.chime",
                            1.0f, 1.0f
                    );
                    newLeader.showTitle(Title.title(
                            Component.text("Leader role gained", NamedTextColor.GREEN),
                            Component.text("You are now the new leader", NamedTextColor.DARK_GREEN),
                            Title.Times.times(
                                    Duration.ofMillis(800),
                                    Duration.ofSeconds(3),
                                    Duration.ofMillis(600)
                            )
                    ));
                } else {
                    newLeader.playSound(
                            newLeader.getLocation(),
                            "minecraft:block.note_block.chime",
                            1.0f, 1.0f
                    );
                    newLeader.showTitle(Title.title(
                            Component.text("Continue leading", NamedTextColor.AQUA),
                            Component.text("You have retained the leader role", NamedTextColor.DARK_AQUA),
                            Title.Times.times(
                                    Duration.ofMillis(800),
                                    Duration.ofSeconds(3),
                                    Duration.ofMillis(600)
                            )
                    ));
                }
            }

            scheduleNextRun();
        }, 30L);
    }

    /**
     * Formats a duration as:
     *  - "45s"           for durations under 60 seconds
     *  - "1m 22s"        for durations with minutes + seconds
     *  - "2m"            for exact multiples of a minute
     */
    private String formatDuration(int totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (seconds > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return minutes + "m";
        }
    }
}