package au.com.addstar.trailblazer.commands;

import au.com.addstar.trailblazer.LeaderManager;
import au.com.addstar.trailblazer.TrailblazerPlugin;
import au.com.addstar.trailblazer.WhitelistManager;
import au.com.addstar.trailblazer.ui.AutoLeaderBar;
import au.com.addstar.trailblazer.util.DebugLogger;
import au.com.addstar.trailblazer.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TrailblazerCommand implements CommandExecutor {
    private final TrailblazerPlugin plugin;

    public TrailblazerCommand(TrailblazerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(MessageUtil.mm("<gray>Usage:</gray> /trailblazer <unlock|stats|addleader|removeleader|autoleader>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "unlock" -> handleUnlock(sender, args);
            case "stats" -> handleStats(sender);
            case "addleader" -> handleAddLeader(sender, args);
            case "removeleader" -> handleRemoveLeader(sender, args);
            case "autoleader" -> handleAutoLeader(sender, args);
            case "reload" -> handleReload(sender);
            case "debug" -> handleDebugToggle(sender, args);
            case "leaders" -> handleListLeaders(sender);
            default -> {
                sender.sendMessage(MessageUtil.mm("<red>Unknown subcommand. Use /trailblazer <unlock|stats|addleader|removeleader|autoleader></red>"));
            }
        }

        return true;
    }

    private void handleUnlock(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtil.mm("<red>Usage: /trailblazer unlock <break|place|craft> <material>"));
            return;
        }

        String type = args[1].toLowerCase();
        Material material = Material.matchMaterial(args[2]);

        if (material == null) {
            sender.sendMessage(MessageUtil.mm("<red>Invalid material: <gray>" + args[2] + "</gray>"));
            return;
        }

        WhitelistManager whitelist = plugin.getWhitelistManager();
        boolean added = switch (type) {
            case "break" -> whitelist.addBreak(material);
            case "place" -> whitelist.addPlace(material);
            case "craft" -> whitelist.addCraft(material);
            default -> {
                sender.sendMessage(MessageUtil.mm("<red>Invalid type: must be break, place, or craft."));
                yield false;
            }
        };

        if (added) {
            whitelist.saveWhitelist();
            plugin.getProgressBar().showUnlock(sender.getName(), type, material);
            sender.sendMessage(MessageUtil.mm("<green>Unlocked <gray>" + type + "</gray> for <aqua>" + material.name().toLowerCase() + "</aqua>"));
        } else {
            sender.sendMessage(MessageUtil.mm("<yellow>Already unlocked <gray>" + type + "</gray> for <aqua>" + material.name().toLowerCase() + "</aqua>"));
        }
    }

    private void handleStats(CommandSender sender) {
        WhitelistManager whitelist = plugin.getWhitelistManager();

        int totalMaterials = Material.values().length;
        int breakCount = whitelist.getAllowedBreak().size();
        int placeCount = whitelist.getAllowedPlace().size();
        int craftCount = whitelist.getAllowedCraft().size();

        long useMaterials = whitelist.getAllowedUse().stream().filter(name -> !name.startsWith("ENTITY_")).count();
        long useEntities = whitelist.getAllowedUse().stream().filter(name -> name.startsWith("ENTITY_")).count();

        int mobKills = whitelist.getAllowedMobKills().size();
        int totalEntityTypes = (int) Arrays.stream(EntityType.values())
                .filter(EntityType::isAlive)
                .count();

        sender.sendMessage(MessageUtil.mm("<aqua>--- Trailblazer Unlock Stats ---</aqua>"));
        sender.sendMessage(MessageUtil.mm("<gray>Breaks:</gray> <green>" + breakCount + "</green> / <gray>" + totalMaterials + "</gray>"));
        sender.sendMessage(MessageUtil.mm("<gray>Places:</gray> <green>" + placeCount + "</green> / <gray>" + totalMaterials + "</gray>"));
        sender.sendMessage(MessageUtil.mm("<gray>Crafts:</gray> <green>" + craftCount + "</green> / <gray>" + totalMaterials + "</gray>"));
        sender.sendMessage(MessageUtil.mm("<gray>Uses:</gray> <green>" + useMaterials + "</green> items/blocks, <green>" + useEntities + "</green> entities"));
        sender.sendMessage(MessageUtil.mm("<gray>Mob Kills:</gray> <green>" + mobKills + "</green> / <gray>" + totalEntityTypes + "</gray> entities"));
    }

    private void handleAddLeader(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.mm("<red>Usage: /trailblazer addleader <player> [timeInSeconds] [reason...]"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(MessageUtil.mm("<red>Player not found."));
            return;
        }

        UUID uuid = target.getUniqueId();
        LeaderManager leaderManager = plugin.getLeaderManager();

        if (args.length >= 3) {
            try {
                long timeSecs = Long.parseLong(args[2]);

                String reason = args.length > 3
                        ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
                        : "Unknown reason";

                DebugLogger.debug("Command/addLeader", "Player " + target.getName() + " temp added for " + timeSecs + " seconds with reason: " + reason);
                boolean added = leaderManager.addTemporaryLeader(uuid, timeSecs, reason);
                if (added) {
                    Component chat = plugin.getPluginConfig().getMessage("messages.temp-leader-added", Map.of(
                            "player", target.getName(),
                            "reason", reason,
                            "duration", MessageUtil.formatDurationShort(timeSecs)
                    ));
                    Bukkit.getServer().sendMessage(chat);
                } else {
                    sender.sendMessage(MessageUtil.mm("<yellow>" + target.getName() + " is already a leader."));
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(MessageUtil.mm("<red>Invalid number: <gray>" + args[2] + "</gray>"));
            }
        } else {
            DebugLogger.debug("Command/addLeader", "Player " + target.getName() + " perm added");
            boolean added = leaderManager.addPermanentLeader(uuid);
            if (added) {
                sender.sendMessage(MessageUtil.mm("<green>Added <aqua>" + target.getName() + "</aqua> as a permanent leader."));
            } else {
                sender.sendMessage(MessageUtil.mm("<yellow>" + target.getName() + " is already a leader."));
            }
        }
    }

    private void handleRemoveLeader(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.mm("<red>Usage: /trailblazer removeleader <player>"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId() == null) {
            sender.sendMessage(MessageUtil.mm("<red>Player not found."));
            return;
        }

        LeaderManager leaderManager = plugin.getLeaderManager();
        if (!leaderManager.isPermanent(target.getUniqueId()) && !leaderManager.isLeader(target.getUniqueId())) {
            sender.sendMessage(MessageUtil.mm("<red>Player is not a leader.</red>"));
            return;
        }

        DebugLogger.debug("Command/removeLeader", "Player " + target.getName() + " removed");
        leaderManager.removeTemporaryLeader(target.getUniqueId(), false);
        leaderManager.removePermanentLeader(target.getUniqueId(), false);
        String targetName = target.getName() != null ? target.getName() : args[1];
        sender.sendMessage(MessageUtil.mm("<green>Removed leader: <aqua>" + targetName + "</aqua>"));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("trailblazer.admin")) {
            sender.sendMessage(MessageUtil.mm("<red>You don't have permission to reload the plugin.</red>"));
            return;
        }

        plugin.reload();
        sender.sendMessage(MessageUtil.mm("<green>Trailblazer config and whitelist reloaded.</green>"));
    }

    private void handleDebugToggle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("trailblazer.admin")) {
            sender.sendMessage(MessageUtil.mm("<red>You don't have permission to toggle debug mode.</red>"));
            return;
        }

        if (args.length == 1) {
            boolean current = plugin.isDebugEnabled();
            sender.sendMessage(MessageUtil.mm("<aqua>Debug mode is currently <bold>" + (current ? "ON" : "OFF") + "</bold>"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "on" -> {
                plugin.setDebugEnabled(true);
                sender.sendMessage(MessageUtil.mm("<green>Debug logging is now ON.</green>"));
            }
            case "off" -> {
                plugin.setDebugEnabled(false);
                sender.sendMessage(MessageUtil.mm("<gray>Debug logging is now OFF.</gray>"));
            }
            default -> sender.sendMessage(MessageUtil.mm("<red>Usage: /trailblazer debug [on|off]</red>"));
        }
    }

    private void handleListLeaders(CommandSender sender) {
        LeaderManager leaderManager = plugin.getLeaderManager();

        Set<UUID> permanent = leaderManager.getPermanentLeaders();
        Set<UUID> temporary = leaderManager.getTemporaryLeaders();

        sender.sendMessage(MessageUtil.mm("<aqua>--- Trailblazer Leaders ---</aqua>"));

        if (permanent.isEmpty()) {
            sender.sendMessage(MessageUtil.mm("<gray>No permanent leaders.</gray>"));
        } else {
            sender.sendMessage(MessageUtil.mm("<yellow>Permanent Leaders:</yellow>"));
            for (UUID uuid : permanent) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                String name = player.getName() != null ? player.getName() : uuid.toString();
                sender.sendMessage(MessageUtil.mm("<gray>-</gray> <green>" + name + "</green>"));
            }
        }

        if (temporary.isEmpty()) {
            sender.sendMessage(MessageUtil.mm("<gray>No temporary leaders.</gray>"));
        } else {
            sender.sendMessage(MessageUtil.mm("<yellow>Temporary Leaders:</yellow>"));
            for (UUID uuid : temporary) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                String name = player.getName() != null ? player.getName() : uuid.toString();
                sender.sendMessage(MessageUtil.mm("<gray>-</gray> <gold>" + name + "</gold>"));
            }
        }
    }

    private void handleAutoLeader(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.mm("<red>Usage: /trailblazer autoleader <start|stop|set> ..."));
            return;
        }
        AutoLeaderBar autoLeaderBar = plugin.getAutoLeaderBar();
        switch (args[1].toLowerCase()) {
            case "start" -> {
                if (args.length < 4) {
                    sender.sendMessage(MessageUtil.mm("<red>Usage: /trailblazer autoleader start <min> <max></red>"));
                    return;
                }
                try {
                    int min = Integer.parseInt(args[2]);
                    int max = Integer.parseInt(args[3]);
                    if (!autoLeaderBar.startTimer(min, max)) {
                        sender.sendMessage(MessageUtil.mm("<red>Cannot start: leader selection is already in progress.</red>"));
                        return;
                    }
                    sender.sendMessage(MessageUtil.mm("<green>Auto leader started with min: " + min + " and max: " + max + "</green>"));
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtil.mm("<red>Invalid number: <gray>" + args[2] + "</gray> or <gray>" + args[3] + "</gray>"));
                }
            }
            case "stop" -> {
                autoLeaderBar.stopTimer();
                sender.sendMessage(MessageUtil.mm("<green>Auto leader stopped.</green>"));
            }
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(MessageUtil.mm("<red>Usage: /trailblazer autoleader set <player> [reason...]</red>"));
                    return;
                }
                if (!autoLeaderBar.isRunning() && !autoLeaderBar.isSelecting()) {
                    sender.sendMessage(MessageUtil.mm("<red>Auto leader is not running.</red>"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(MessageUtil.mm("<red>Player not found or not online.</red>"));
                    return;
                }
                String reason = args.length > 3
                        ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
                        : null;
                autoLeaderBar.setSpecificLeader(target, reason);
                sender.sendMessage(MessageUtil.mm("<green>Set <aqua>" + target.getName() + "</aqua> as leader.</green>"));
            }
            default -> sender.sendMessage(MessageUtil.mm("<red>Invalid command. Use /trailblazer autoleader start|stop|set</red>"));
        }
    }
}
