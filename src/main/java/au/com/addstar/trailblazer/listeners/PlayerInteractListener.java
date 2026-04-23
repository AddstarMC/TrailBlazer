package au.com.addstar.trailblazer.listeners;

import au.com.addstar.trailblazer.TrailblazerPlugin;
import au.com.addstar.trailblazer.WhitelistManager;
import au.com.addstar.trailblazer.util.DebugLogger;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.EnumSet;
import java.util.Set;

public class PlayerInteractListener implements Listener {
    private final TrailblazerPlugin plugin;

    private static final Set<Material> INTERACTABLE_BLOCKS = EnumSet.of(
            Material.CRAFTING_TABLE, Material.BEACON,
            Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST,
            Material.VAULT,
            Material.BARREL, Material.DISPENSER, Material.DROPPER,
            Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
            Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
            Material.SMITHING_TABLE, Material.CARTOGRAPHY_TABLE,
            Material.COMPOSTER, Material.FLETCHING_TABLE,
            Material.LECTERN, Material.GRINDSTONE, Material.STONECUTTER,
            Material.BREWING_STAND, Material.ENCHANTING_TABLE,
            Material.BELL, Material.NOTE_BLOCK, Material.JUKEBOX,
            Material.REPEATER, Material.COMPARATOR, Material.DAYLIGHT_DETECTOR,
            Material.HOPPER,
            // 1.20.4
            Material.CHISELED_BOOKSHELF,
            // 1.21 (Tricky Trials)
            Material.CRAFTER, Material.TRIAL_SPAWNER
    );

    public PlayerInteractListener(TrailblazerPlugin plugin) {
        this.plugin = plugin;
    }

    /*
        This event is triggered when a player interacts with a block or item (in hand or in the world).
        Example actions this covers:
          - Opening a chest, door, trapdoor, etc.
          - Lighting a fire/portal.
          - Placing block (although this is ignored and handled in BlockPlaceEvent).
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        boolean isLeader = plugin.getLeaderManager().isLeader(player.getUniqueId());

        Material clickedBlockType = event.getClickedBlock() != null ? event.getClickedBlock().getType() : Material.AIR;
        Material inHandType = event.getItem() != null ? event.getItem().getType() : Material.AIR;

        Material usedMaterial = determineUsedMaterial(clickedBlockType, inHandType);

        if (usedMaterial == inHandType && usedMaterial.isBlock()) return;

        WhitelistManager whitelist = plugin.getWhitelistManager();
        if (!whitelist.isUseAllowed(usedMaterial)) {
            if (isLeader) {
                if (whitelist.addUse(usedMaterial)) {
                    whitelist.saveWhitelist();
                    plugin.getProgressBar().showUnlock(player.getName(), "use", usedMaterial);
                }
            } else {
                event.setCancelled(true);
                player.sendActionBar(plugin.getPluginConfig().getMessage("messages.interact-not-allowed"));
                DebugLogger.debug("PlayerInteract/block", "Player " + player.getName() + " tried to use block: " + usedMaterial);
                return;
            }
        }
    }

    /*
        This event is triggered when a player interacts with an entity (e.g. right-clicking a cow).
        Example actions this covers:
          - Feeding animals
          - Using a bucket on a cow
          - Using a fishing rod
          - Using a name tag
          - Mounting an animal
          - Getting in a boat
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        EntityType type = event.getRightClicked().getType();
        if (type == EntityType.PLAYER) return;

        boolean isLeader = plugin.getLeaderManager().isLeader(player.getUniqueId());
        WhitelistManager whitelist = plugin.getWhitelistManager();
        if (!whitelist.isUseAllowed(type)) {
            if (isLeader) {
                if (whitelist.addUse(type)) {
                    whitelist.saveWhitelist();
                    plugin.getProgressBar().showUnlock(player.getName(), "use", type);
                }
            } else {
                event.setCancelled(true);
                player.sendActionBar(plugin.getPluginConfig().getMessage("messages.interact-not-allowed"));
                DebugLogger.debug("PlayerInteract/entity", "Player " + player.getName() + " tried to use entity: " + type);
                return;
            }
        }
    }

    public static boolean isInteractableBlock(Material material) {
        return Tag.BUTTONS.isTagged(material)
                || Tag.WOODEN_DOORS.isTagged(material)
                || Tag.TRAPDOORS.isTagged(material)
                || Tag.FENCE_GATES.isTagged(material)
                || Tag.SIGNS.isTagged(material)
                || Tag.BEDS.isTagged(material)
                || Tag.SHULKER_BOXES.isTagged(material)
                || INTERACTABLE_BLOCKS.contains(material);
    }

    private static Material determineUsedMaterial(Material clickedBlock, Material inHand) {
        if (inHand == Material.AIR && clickedBlock != Material.AIR) {
            return clickedBlock;
        }

        if (clickedBlock == Material.AIR && inHand != Material.AIR) {
            return inHand;
        }

        if (isInteractableBlock(clickedBlock)) {
            return clickedBlock;
        }

        return inHand;
    }
}
