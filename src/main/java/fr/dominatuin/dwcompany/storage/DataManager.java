package fr.dominatuin.dwcompany.storage;

import fr.dominatuin.dwcompany.Company;
import fr.dominatuin.dwcompany.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Central data manager that orchestrates storage, saving, and backups.
 * Handles async saving and auto-save functionality.
 */
public class DataManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private StorageProvider storageProvider;
    private BackupManager backupManager;

    // In-memory cache of companies
    private final Map<String, Company> companies;
    private final Map<UUID, String> playerCompanyMap;
    private final Map<UUID, Integer> playerCompanyCounts;

    private boolean autoSaveEnabled;
    private int autoSaveInterval;
    private BukkitRunnable autoSaveTask;

    /**
     * Creates a new DataManager instance.
     *
     * @param plugin        The plugin instance
     * @param configManager The config manager
     */
    public DataManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.companies = new ConcurrentHashMap<>();
        this.playerCompanyMap = new ConcurrentHashMap<>();
        this.playerCompanyCounts = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the data manager and storage.
     *
     * @return true if initialized successfully
     */
    public boolean initialize() {
        // Initialize backup manager
        boolean backupEnabled = configManager.getConfig().getBoolean("backup.enabled", true);
        int maxBackups = configManager.getConfig().getInt("backup.max-backups", 10);
        backupManager = new BackupManager(plugin, backupEnabled, maxBackups);

        // Create backup before loading
        if (backupEnabled) {
            backupManager.createBackup();
        }

        // Initialize storage provider
        String storageType = configManager.getConfig().getString("storage.type", "YAML").toUpperCase();

        if (storageType.equals("MYSQL")) {
            String host = configManager.getConfig().getString("storage.mysql.host", "localhost");
            int port = configManager.getConfig().getInt("storage.mysql.port", 3306);
            String database = configManager.getConfig().getString("storage.mysql.database", "dwcompany");
            String username = configManager.getConfig().getString("storage.mysql.username", "root");
            String password = configManager.getConfig().getString("storage.mysql.password", "");
            String tablePrefix = configManager.getConfig().getString("storage.mysql.table-prefix", "dwc_");

            storageProvider = new MySQLStorage(plugin, host, port, database, username, password, tablePrefix);
        } else {
            storageProvider = new YamlStorage(plugin);
        }

        // Initialize storage
        if (!storageProvider.initialize()) {
            plugin.getLogger().severe("Failed to initialize storage provider!");
            return false;
        }

        // Validate data
        if (!storageProvider.validateData()) {
            plugin.getLogger().warning("Data validation found issues with existing data.");
        }

        // Load companies
        loadAllCompanies();

        // Start auto-save
        setupAutoSave();

        plugin.getLogger().info("DataManager initialized with " + storageProvider.getStorageType() + " storage");
        return true;
    }

    /**
     * Shuts down the data manager.
     */
    public void shutdown() {
        // Cancel auto-save
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        // Final save
        saveAllCompanies(true);

        // Shutdown storage
        if (storageProvider != null) {
            storageProvider.shutdown();
        }
    }

    /**
     * Sets up auto-save task.
     */
    private void setupAutoSave() {
        autoSaveEnabled = configManager.getConfig().getBoolean("autosave.enabled", true);
        autoSaveInterval = configManager.getConfig().getInt("autosave.interval-minutes", 15);

        if (!autoSaveEnabled || autoSaveInterval <= 0) {
            return;
        }

        long ticks = autoSaveInterval * 60 * 20; // Convert minutes to ticks

        autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Auto-saving company data...");
                saveAllCompaniesAsync();
            }
        };

        autoSaveTask.runTaskTimerAsynchronously(plugin, ticks, ticks);
        plugin.getLogger().info("Auto-save enabled (interval: " + autoSaveInterval + " minutes)");
    }

    /**
     * Loads all companies from storage.
     */
    public void loadAllCompanies() {
        companies.clear();
        playerCompanyMap.clear();
        playerCompanyCounts.clear();

        List<Company> loadedCompanies = storageProvider.loadCompanies();

        for (Company company : loadedCompanies) {
            companies.put(company.getName().toLowerCase(), company);

            // Update player mappings
            for (UUID memberUUID : company.getMembers()) {
                playerCompanyMap.put(memberUUID, company.getName());
            }

            // Update CEO count
            UUID ceoUUID = company.getCeoUUID();
            playerCompanyCounts.put(ceoUUID, playerCompanyCounts.getOrDefault(ceoUUID, 0) + 1);
        }

        plugin.getLogger().info("Loaded " + companies.size() + " companies into memory");
    }

    /**
     * Saves all companies to storage.
     *
     * @param sync Whether to save synchronously
     * @return true if saved successfully
     */
    public boolean saveAllCompanies(boolean sync) {
        if (sync) {
            return performSave();
        } else {
            saveAllCompaniesAsync();
            return true;
        }
    }

    /**
     * Saves all companies asynchronously.
     */
    public void saveAllCompaniesAsync() {
        new BukkitRunnable() {
            @Override
            public void run() {
                performSave();
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Performs the actual save operation.
     *
     * @return true if saved successfully
     */
    private boolean performSave() {
        try {
            long startTime = System.currentTimeMillis();
            boolean success = storageProvider.saveCompanies(companies.values());
            long duration = System.currentTimeMillis() - startTime;

            if (success) {
                plugin.getLogger().info("Saved " + companies.size() + " companies in " + duration + "ms");
            } else {
                plugin.getLogger().warning("Failed to save some companies!");
            }

            return success;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during save operation", e);
            return false;
        }
    }

    /**
     * Saves a single company.
     *
     * @param company The company to save
     * @param async   Whether to save asynchronously
     */
    public void saveCompany(Company company, boolean async) {
        if (async) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    storageProvider.saveCompany(company);
                }
            }.runTaskAsynchronously(plugin);
        } else {
            storageProvider.saveCompany(company);
        }
    }

    /**
     * Creates a new company.
     *
     * @param company The company to create
     * @return true if created successfully
     */
    public boolean createCompany(Company company) {
        if (companies.containsKey(company.getName().toLowerCase())) {
            return false;
        }

        companies.put(company.getName().toLowerCase(), company);

        // Update mappings
        playerCompanyMap.put(company.getCeoUUID(), company.getName());
        playerCompanyCounts.put(company.getCeoUUID(),
                playerCompanyCounts.getOrDefault(company.getCeoUUID(), 0) + 1);

        // Save immediately
        saveCompany(company, true);
        return true;
    }

    /**
     * Deletes a company.
     *
     * @param companyName The company name to delete
     * @return true if deleted successfully
     */
    public boolean deleteCompany(String companyName) {
        Company company = companies.get(companyName.toLowerCase());
        if (company == null) {
            return false;
        }

        // Remove from storage
        storageProvider.deleteCompany(companyName);

        // Remove from memory
        companies.remove(companyName.toLowerCase());

        // Update player mappings
        for (UUID memberUUID : company.getMembers()) {
            playerCompanyMap.remove(memberUUID);
        }

        // Update CEO count
        UUID ceoUUID = company.getCeoUUID();
        int count = playerCompanyCounts.getOrDefault(ceoUUID, 0);
        if (count > 0) {
            playerCompanyCounts.put(ceoUUID, count - 1);
        }

        return true;
    }

    /**
     * Gets a company by name.
     *
     * @param name The company name
     * @return The company, or null if not found
     */
    public Company getCompany(String name) {
        return companies.get(name.toLowerCase());
    }

    /**
     * Gets all companies.
     *
     * @return Collection of all companies
     */
    public Collection<Company> getAllCompanies() {
        return companies.values();
    }

    /**
     * Gets a player's company.
     *
     * @param playerUUID The player's UUID
     * @return Company name, or null if not in a company
     */
    public String getPlayerCompany(UUID playerUUID) {
        return playerCompanyMap.get(playerUUID);
    }

    /**
     * Checks if a player is in a company.
     *
     * @param playerUUID The player's UUID
     * @return true if in a company
     */
    public boolean isInCompany(UUID playerUUID) {
        return playerCompanyMap.containsKey(playerUUID);
    }

    /**
     * Gets the number of companies a player owns.
     *
     * @param playerUUID The player's UUID
     * @return Number of companies owned
     */
    public int getPlayerCompanyCount(UUID playerUUID) {
        return playerCompanyCounts.getOrDefault(playerUUID, 0);
    }

    /**
     * Checks if a player can create more companies.
     *
     * @param playerUUID The player's UUID
     * @return true if they can create more
     */
    public boolean canCreateCompany(UUID playerUUID) {
        int maxCompanies = configManager.getConfig().getInt("companies.max-per-player", 2);
        return getPlayerCompanyCount(playerUUID) < maxCompanies;
    }

    /**
     * Adds a member to a company.
     *
     * @param companyName The company name
     * @param playerUUID  The player's UUID
     * @param playerName  The player's name
     * @return true if added successfully
     */
    public boolean addMemberToCompany(String companyName, UUID playerUUID, String playerName) {
        Company company = getCompany(companyName);
        if (company == null) {
            return false;
        }

        if (company.hasMember(playerUUID)) {
            return false;
        }

        company.addMember(playerUUID, playerName);
        playerCompanyMap.put(playerUUID, companyName);

        saveCompany(company, true);
        return true;
    }

    /**
     * Removes a member from their company.
     *
     * @param playerUUID The player's UUID
     * @return true if removed successfully
     */
    public boolean removeMemberFromCompany(UUID playerUUID) {
        String companyName = playerCompanyMap.get(playerUUID);
        if (companyName == null) {
            return false;
        }

        Company company = getCompany(companyName);
        if (company == null) {
            return false;
        }

        company.removeMember(playerUUID);
        playerCompanyMap.remove(playerUUID);

        saveCompany(company, true);
        return true;
    }

    /**
     * Creates a backup of the current data.
     *
     * @return true if backup was successful
     */
    public boolean createBackup() {
        if (backupManager == null) {
            return false;
        }
        return backupManager.createBackup();
    }

    /**
     * Gets the storage provider.
     *
     * @return The storage provider
     */
    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    /**
     * Gets the backup manager.
     *
     * @return The backup manager
     */
    public BackupManager getBackupManager() {
        return backupManager;
    }

    /**
     * Reloads the data manager configuration.
     */
    public void reload() {
        // Cancel current auto-save
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }

        // Reload auto-save settings
        setupAutoSave();

        // Reload backup settings
        if (backupManager != null) {
            boolean backupEnabled = configManager.getConfig().getBoolean("backup.enabled", true);
            int maxBackups = configManager.getConfig().getInt("backup.max-backups", 10);
            backupManager.setEnabled(backupEnabled);
            backupManager.setMaxBackups(maxBackups);
        }

        plugin.getLogger().info("DataManager reloaded");
    }
}
