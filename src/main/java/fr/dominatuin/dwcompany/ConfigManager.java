package fr.dominatuin.dwcompany;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages the main plugin configuration (config.yml).
 * Handles loading, saving, and providing access to configuration values.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    // Cached configuration values
    private final Map<String, Object> cache;

    /**
     * Creates a new ConfigManager instance.
     *
     * @param plugin The plugin instance
     */
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cache = new HashMap<>();
        loadConfig();
    }

    /**
     * Loads the configuration file.
     */
    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");

        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load defaults from jar
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            config.setDefaults(defaultConfig);
            config.options().copyDefaults(true);
        }

        // Clear cache
        cache.clear();

        plugin.getLogger().info("Configuration loaded");
    }

    /**
     * Saves the configuration file.
     *
     * @return true if saved successfully
     */
    public boolean saveConfig() {
        try {
            config.save(configFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", e);
            return false;
        }
    }

    /**
     * Reloads the configuration.
     */
    public void reload() {
        loadConfig();
        plugin.getLogger().info("Configuration reloaded");
    }

    /**
     * Gets the raw FileConfiguration.
     *
     * @return The configuration
     */
    public FileConfiguration getConfig() {
        return config;
    }

    // ==================== Helper Methods ====================

    /**
     * Gets a string value from config.
     *
     * @param path The path
     * @param def  Default value
     * @return The string value
     */
    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    /**
     * Gets an integer value from config.
     *
     * @param path The path
     * @param def  Default value
     * @return The integer value
     */
    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    /**
     * Gets a double value from config.
     *
     * @param path The path
     * @param def  Default value
     * @return The double value
     */
    public double getDouble(String path, double def) {
        return config.getDouble(path, def);
    }

    /**
     * Gets a boolean value from config.
     *
     * @param path The path
     * @param def  Default value
     * @return The boolean value
     */
    public boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    // ==================== Company Settings ====================

    /**
     * Gets the cost to create a first company.
     *
     * @return The cost
     */
    public double getFirstCompanyCost() {
        return getDouble("companies.creation-cost.first", 100000.0);
    }

    /**
     * Gets the cost to create a second company.
     *
     * @return The cost
     */
    public double getSecondCompanyCost() {
        return getDouble("companies.creation-cost.second", 500000.0);
    }

    /**
     * Gets the maximum companies per player.
     *
     * @return Maximum companies
     */
    public int getMaxCompaniesPerPlayer() {
        return getInt("companies.max-per-player", 2);
    }

    /**
     * Gets the maximum members for National companies.
     *
     * @return Maximum members
     */
    public int getNationalMemberLimit() {
        return getInt("companies.member-limits.national", 5);
    }

    /**
     * Gets the maximum members for International companies.
     *
     * @return Maximum members
     */
    public int getInternationalMemberLimit() {
        return getInt("companies.member-limits.international", 10);
    }

    /**
     * Gets the International upgrade cost.
     *
     * @return The cost
     */
    public double getInternationalUpgradeCost() {
        return getDouble("companies.international-upgrade-cost", 20000.0);
    }

    // ==================== Level Settings ====================

    /**
     * Gets the money required for a specific level.
     *
     * @param level The level
     * @return Money required
     */
    public double getLevelRequirement(int level) {
        String path = "levels.level-" + level + "-money";
        double[] defaults = {0, 10000, 50000, 100000, 250000, 500000, 1000000};
        if (level >= 1 && level <= 7) {
            return getDouble(path, defaults[level - 1]);
        }
        return getDouble(path, 0);
    }

    /**
     * Gets the material for a company level icon.
     *
     * @param level The level
     * @return Material name
     */
    public String getLevelMaterial(int level) {
        String[] defaults = {
                "COBBLESTONE",      // Level 1
                "IRON_BLOCK",       // Level 2
                "GOLD_BLOCK",       // Level 3
                "DIAMOND_BLOCK",    // Level 4
                "EMERALD_BLOCK",    // Level 5
                "OBSIDIAN",         // Level 6
                "NETHERITE_BLOCK"   // Level 7
        };

        if (level >= 1 && level <= 7) {
            return getString("levels.materials.level-" + level, defaults[level - 1]);
        }
        return "COBBLESTONE";
    }

    // ==================== Storage Settings ====================

    /**
     * Gets the storage type (YAML or MySQL).
     *
     * @return Storage type
     */
    public String getStorageType() {
        return getString("storage.type", "YAML");
    }

    /**
     * Gets MySQL host.
     *
     * @return MySQL host
     */
    public String getMySQLHost() {
        return getString("storage.mysql.host", "localhost");
    }

    /**
     * Gets MySQL port.
     *
     * @return MySQL port
     */
    public int getMySQLPort() {
        return getInt("storage.mysql.port", 3306);
    }

    /**
     * Gets MySQL database name.
     *
     * @return Database name
     */
    public String getMySQLDatabase() {
        return getString("storage.mysql.database", "dwcompany");
    }

    /**
     * Gets MySQL username.
     *
     * @return Username
     */
    public String getMySQLUsername() {
        return getString("storage.mysql.username", "root");
    }

    /**
     * Gets MySQL password.
     *
     * @return Password
     */
    public String getMySQLPassword() {
        return getString("storage.mysql.password", "");
    }

    // ==================== Auto-save Settings ====================

    /**
     * Checks if auto-save is enabled.
     *
     * @return true if enabled
     */
    public boolean isAutoSaveEnabled() {
        return getBoolean("autosave.enabled", true);
    }

    /**
     * Gets the auto-save interval in minutes.
     *
     * @return Interval in minutes
     */
    public int getAutoSaveInterval() {
        return getInt("autosave.interval-minutes", 15);
    }

    // ==================== Backup Settings ====================

    /**
     * Checks if backups are enabled.
     *
     * @return true if enabled
     */
    public boolean isBackupEnabled() {
        return getBoolean("backup.enabled", true);
    }

    /**
     * Gets the maximum number of backups.
     *
     * @return Maximum backups
     */
    public int getMaxBackups() {
        return getInt("backup.max-backups", 10);
    }

    // ==================== Sound Settings ====================

    /**
     * Checks if GUI sounds are enabled.
     *
     * @return true if enabled
     */
    public boolean areSoundsEnabled() {
        return getBoolean("sounds.enabled", true);
    }

    /**
     * Gets the sound for button clicks.
     *
     * @return Sound name
     */
    public String getButtonClickSound() {
        return getString("sounds.button-click", "UI_BUTTON_CLICK");
    }

    /**
     * Gets the sound for success actions.
     *
     * @return Sound name
     */
    public String getSuccessSound() {
        return getString("sounds.success", "ENTITY_PLAYER_LEVELUP");
    }

    /**
     * Gets the sound for error actions.
     *
     * @return Sound name
     */
    public String getErrorSound() {
        return getString("sounds.error", "ENTITY_VILLAGER_NO");
    }

    // ==================== Dynmap Settings ====================

    /**
     * Checks if Dynmap markers are enabled.
     *
     * @return true if enabled
     */
    public boolean isDynmapEnabled() {
        return getBoolean("dynmap.enabled", true);
    }

    /**
     * Gets the Dynmap marker icon.
     *
     * @return Icon name
     */
    public String getDynmapMarkerIcon() {
        return getString("dynmap.marker-icon", "building");
    }

    /**
     * Gets the Dynmap marker set label.
     *
     * @return Label
     */
    public String getDynmapMarkerSetLabel() {
        return getString("dynmap.marker-set-label", "Company Headquarters");
    }
}
