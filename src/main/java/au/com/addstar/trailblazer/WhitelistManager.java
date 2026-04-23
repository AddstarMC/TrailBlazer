package au.com.addstar.trailblazer;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WhitelistManager {
    private final TrailblazerPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    private static final long SAVE_DEBOUNCE_TICKS = 100L; // 5 seconds
    private BukkitTask pendingSaveTask;
    private boolean dirty = false;

    private final Set<String> allowedBreak = new HashSet<>();
    private final Set<String> allowedPlace = new HashSet<>();
    private final Set<String> allowedCraft = new HashSet<>();
    private final Set<String> allowedUse = new HashSet<>();
    private final Set<String> allowedMobKills = new HashSet<>();

    public WhitelistManager(TrailblazerPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "whitelist.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void loadWhitelist() {
        this.config = YamlConfiguration.loadConfiguration(file);

        allowedBreak.clear();
        allowedPlace.clear();
        allowedCraft.clear();
        allowedUse.clear();
        allowedMobKills.clear();

        loadSet("break", allowedBreak);
        loadSet("place", allowedPlace);
        loadSet("craft", allowedCraft);
        loadSet("use", allowedUse);
        loadSet("mobs", allowedMobKills);
        dirty = false;
    }

    /**
     * Marks the whitelist as dirty and schedules a debounced save.
     * Actual disk write happens after SAVE_DEBOUNCE_TICKS of inactivity.
     */
    public void saveWhitelist() {
        dirty = true;
        if (pendingSaveTask != null) {
            pendingSaveTask.cancel();
        }
        pendingSaveTask = Bukkit.getScheduler().runTaskLater(plugin, this::flushSave, SAVE_DEBOUNCE_TICKS);
    }

    /**
     * Immediately writes the whitelist to disk. Called on plugin disable
     * and by the debounce timer.
     */
    public void flushSave() {
        if (pendingSaveTask != null) {
            pendingSaveTask.cancel();
            pendingSaveTask = null;
        }
        if (!dirty) return;

        saveSet("break", allowedBreak);
        saveSet("place", allowedPlace);
        saveSet("craft", allowedCraft);
        saveSet("use", allowedUse);
        saveSet("mobs", allowedMobKills);
        try {
            config.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save whitelist.yml");
            e.printStackTrace();
        }
    }

    private void loadSet(String key, Set<String> target) {
        target.addAll(config.getStringList(key));
    }

    private void saveSet(String key, Set<String> source) {
        config.set(key, new ArrayList<>(source));
    }

    // Break
    public boolean isBreakAllowed(Material mat) {
        return allowedBreak.contains(mat.name());
    }

    public boolean addBreak(Material mat) {
        return allowedBreak.add(mat.name());
    }

    // Place
    public boolean isPlaceAllowed(Material mat) {
        return allowedPlace.contains(mat.name());
    }

    public boolean addPlace(Material mat) {
        return allowedPlace.add(mat.name());
    }

    // Craft
    public boolean isCraftAllowed(Material mat) {
        return allowedCraft.contains(mat.name());
    }

    public boolean addCraft(Material mat) {
        return allowedCraft.add(mat.name());
    }

    // Use (items, blocks, entities)
    public boolean isUseAllowed(Material mat) {
        return allowedUse.contains(mat.name());
    }

    public boolean isUseAllowed(EntityType type) {
        return allowedUse.contains("ENTITY_" + type.name());
    }

    public boolean isUseAllowed(String raw) {
        return allowedUse.contains(raw);
    }

    public boolean addUse(Material mat) {
        return allowedUse.add(mat.name());
    }

    public boolean addUse(EntityType type) {
        return allowedUse.add("ENTITY_" + type.name());
    }

    public boolean addUse(String raw) {
        return allowedUse.add(raw);
    }

    // Mob Kills
    public boolean isMobKillAllowed(EntityType type) {
        return allowedMobKills.contains(type.name());
    }

    public boolean addMob(EntityType type) {
        return allowedMobKills.add(type.name());
    }

    // Stats / Progress
    public Set<Material> getAllWhitelistedMaterials() {
        Set<Material> result = new HashSet<>();
        for (String name : allowedBreak) addMaterial(name, result);
        for (String name : allowedPlace) addMaterial(name, result);
        for (String name : allowedCraft) addMaterial(name, result);
        for (String name : allowedUse) {
            if (!name.startsWith("ENTITY_")) addMaterial(name, result);
        }
        return result;
    }

    private void addMaterial(String name, Set<Material> set) {
        Material mat = Material.matchMaterial(name);
        if (mat != null) set.add(mat);
    }

    public Set<String> getAllowedBreak() {
        return Set.copyOf(allowedBreak);
    }

    public Set<String> getAllowedPlace() {
        return Set.copyOf(allowedPlace);
    }

    public Set<String> getAllowedCraft() {
        return Set.copyOf(allowedCraft);
    }

    public Set<String> getAllowedUse() {
        return Set.copyOf(allowedUse);
    }

    public Set<String> getAllowedMobKills() {
        return Set.copyOf(allowedMobKills);
    }
}
