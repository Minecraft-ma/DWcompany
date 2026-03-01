package fr.dominatuin.dwcompany.storage;

import fr.dominatuin.dwcompany.Company;
import fr.dominatuin.dwcompany.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * High-performance data manager for DWCompany plugin.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Thread-safe data structures</li>
 *   <li>Asynchronous save/load operations</li>
 *   <li>Automatic backup management</li>
 *   <li>Player-company relationship tracking</li>
 *   <li>Data validation and integrity checks</li>
 * </ul>
 * 
 * @author Dominatuin
 * @version 1.0
 * @since 1.0-SNAPSHOT
 */
public class DataManager {
    
    // ======== Constants ========
    private static final String DEFAULT_TABLE_PREFIX = "dwcompany";
    
    // ======== Core Components ========
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private StorageProvider storageProvider;
    private BackupManager backupManager;

    // ======== Data Storage ========
    private final Map<String, Company> companies;
    private final Map<UUID, String> playerCompanyMap;
    private final Map<UUID, Integer> playerCompanyCounts;
    
    // ======== Auto-save Configuration ========
    private boolean autoSaveEnabled;
    private long autoSaveInterval;
    private BukkitRunnable autoSaveTask;
    private final AtomicBoolean isSaving = new AtomicBoolean(false);

    /**
     * Creates a new DataManager instance.
     * 
     * @param plugin Main plugin instance
     * @param configManager Configuration manager
     */
    public DataManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "ConfigManager cannot be null");
        
        // Initialize thread-safe collections
        this.companies = new ConcurrentHashMap<>();
        this.playerCompanyMap = new ConcurrentHashMap<>();
        this.playerCompanyCounts = new ConcurrentHashMap<>();
        
        // Load configuration
        loadConfiguration();
        
        // Initialize storage and backup
        initializeStorageProvider();
        initializeBackupManager();
    }

    /**
     * Loads configuration settings.
     */
    private void loadConfiguration() {
        this.autoSaveEnabled = configManager.getConfig().getBoolean("data.auto-save.enabled", true);
        this.autoSaveInterval = configManager.getConfig().getLong("data.auto-save.interval", 300000L); // 5 minutes
    }

    /**
     * Initializes the storage provider based on configuration.
     */
    private void initializeStorageProvider() {
        String storageType = configManager.getConfig().getString("data.storage.type", "mysql").toLowerCase();
        
        try {
            switch (storageType) {
                case "mysql":
                    String host = configManager.getString("database.mysql.host", "localhost");
                    int port = configManager.getInt("database.mysql.port", 3306);
                    String database = configManager.getString("database.mysql.database", DEFAULT_TABLE_PREFIX);
                    String username = configManager.getString("database.mysql.username", "root");
                    String password = configManager.getString("database.mysql.password", "");
                    String tablePrefix = configManager.getString("database.mysql.table-prefix", DEFAULT_TABLE_PREFIX);
                    
                    this.storageProvider = new MySQLStorage(plugin, host, port, database, username, password, tablePrefix);
                    break;
                case "json":
                    this.storageProvider = new JSONStorage(plugin, configManager);
                    break;
                default:
                    plugin.getLogger().warning("Unknown storage type: " + storageType + ", falling back to MySQL");
                    String defaultHost = configManager.getString("database.mysql.host", "localhost");
                    int defaultPort = configManager.getInt("database.mysql.port", 3306);
                    String defaultDatabase = configManager.getString("database.mysql.database", DEFAULT_TABLE_PREFIX);
                    String defaultUsername = configManager.getString("database.mysql.username", "root");
                    String defaultPassword = configManager.getString("database.mysql.password", "");
                    String defaultTablePrefix = configManager.getString("database.mysql.table-prefix", DEFAULT_TABLE_PREFIX);
                    
                    this.storageProvider = new MySQLStorage(plugin, defaultHost, defaultPort, defaultDatabase, defaultUsername, defaultPassword, defaultTablePrefix);
                    break;
            }
            
            plugin.getLogger().info("Storage provider initialized: " + storageProvider.getClass().getSimpleName());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize storage provider", e);
            throw new RuntimeException("Storage initialization failed", e);
        }
    }

    /**
     * Initializes the backup manager.
     */
    private void initializeBackupManager() {
        boolean backupEnabled = configManager.getConfig().getBoolean("backup.enabled", true);
        
        if (backupEnabled) {
            try {
                boolean autoBackup = configManager.getConfig().getBoolean("backup.auto-backup.enabled", true);
                int maxBackups = configManager.getConfig().getInt("backup.max-backups", 10);
                
                this.backupManager = new BackupManager(plugin, autoBackup, maxBackups);
                plugin.getLogger().info("Backup manager initialized");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to initialize backup manager", e);
                this.backupManager = null;
            }
        } else {
            plugin.getLogger().info("Backup manager disabled");
        }
    }

    /**
     * Initializes the data manager.
     * 
     * @return true if initialization was successful
     */
    public boolean initialize() {
        try {
            if (!storageProvider.initialize()) {
                plugin.getLogger().severe("Failed to initialize storage provider");
                return false;
            }
            
            loadData();
            
            if (autoSaveEnabled) {
                startAutoSave();
            }
            
            plugin.getLogger().info("DataManager initialized successfully");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize DataManager", e);
            return false;
        }
    }

    /**
     * Loads all company data from storage.
     */
    private void loadData() {
        try {
            plugin.getLogger().info("Loading company data...");
            
            Collection<Company> loadedCompanies = storageProvider.loadAllCompanies();
            companies.clear();
            
            for (Company company : loadedCompanies) {
                companies.put(company.getName(), company);
                
                for (UUID memberUUID : company.getMembers()) {
                    playerCompanyMap.put(memberUUID, company.getName());
                }
            }
            
            plugin.getLogger().info("Loaded " + companies.size() + " companies");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load company data", e);
            throw new RuntimeException("Data loading failed", e);
        }
    }

    /**
     * Starts the auto-save task.
     */
    private void startAutoSave() {
        autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                saveDataAsync();
            }
        };
        
        autoSaveTask.runTaskTimerAsynchronously(plugin, autoSaveInterval, autoSaveInterval);
        plugin.getLogger().info("Auto-save started with interval: " + autoSaveInterval + "ms");
    }

    /**
     * Shuts down the data manager.
     */
    public void shutdown() {
        try {
            if (autoSaveTask != null) {
                autoSaveTask.cancel();
                autoSaveTask = null;
            }
            
            saveData();
            
            if (storageProvider != null) {
                storageProvider.shutdown();
            }
            
            plugin.getLogger().info("DataManager shut down successfully");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during shutdown", e);
        }
    }

    /**
     * Saves data asynchronously.
     */
    public void saveDataAsync() {
        if (!isSaving.compareAndSet(false, true)) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                saveData();
            } finally {
                isSaving.set(false);
            }
        });
    }

    /**
     * Saves all data synchronously.
     * 
     * @return true if save was successful
     */
    public boolean saveData() {
        try {
            long startTime = System.currentTimeMillis();
            
            boolean success = storageProvider.saveAllCompanies(companies.values());
            long duration = System.currentTimeMillis() - startTime;
            
            if (success) {
                plugin.getLogger().info("Data saved successfully in " + duration + "ms");
            } else {
                plugin.getLogger().warning("Failed to save data!");
            }

            return success;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during save operation", e);
            return false;
        }
    }

    // ======== Company Management Methods ========
    
    /**
     * Gets a company by name.
     * 
     * @param name Company name
     * @return Company or null if not found
     */
    public Company getCompany(String name) {
        return companies.get(name);
    }
    
    /**
     * Gets all companies.
     * 
     * @return Unmodifiable collection of all companies
     */
    public Collection<Company> getAllCompanies() {
        return Collections.unmodifiableCollection(companies.values());
    }
    
    /**
     * Gets the company a player belongs to.
     * 
     * @param playerUUID Player UUID
     * @return Company or null if not in a company
     */
    public Company getPlayerCompany(UUID playerUUID) {
        String companyName = playerCompanyMap.get(playerUUID);
        return companyName != null ? companies.get(companyName) : null;
    }
    
    /**
     * Checks if a player is in a company.
     * 
     * @param playerUUID Player UUID
     * @return true if player is in a company
     */
    public boolean isInCompany(UUID playerUUID) {
        return playerCompanyMap.containsKey(playerUUID);
    }
    
    /**
     * Creates a new company.
     * 
     * @param company Company to create
     * @return true if company was created successfully
     */
    public boolean createCompany(Company company) {
        if (company == null) {
            return false;
        }
        
        try {
            companies.put(company.getName(), company);
            
            for (UUID memberUUID : company.getMembers()) {
                playerCompanyMap.put(memberUUID, company.getName());
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create company: " + company.getName(), e);
            return false;
        }
    }
    
    /**
     * Deletes a company.
     * 
     * @param companyName Company name to delete
     * @return true if company was deleted successfully
     */
    public boolean deleteCompany(String companyName) {
        if (companyName == null || companyName.trim().isEmpty()) {
            return false;
        }
        
        try {
            Company company = companies.remove(companyName);
            if (company != null) {
                for (UUID memberUUID : company.getMembers()) {
                    playerCompanyMap.remove(memberUUID);
                }
                
                return storageProvider.deleteCompany(companyName);
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete company: " + companyName, e);
            return false;
        }
    }
    
    /**
     * Checks if a company name is available.
     * 
     * @param name Company name to check
     * @return true if name is available
     */
    public boolean isCompanyNameAvailable(String name) {
        return !companies.containsKey(name);
    }
    
    /**
     * Checks if a player can create a company.
     * 
     * @param playerUUID Player UUID
     * @return true if player can create a company
     */
    public boolean canCreateCompany(UUID playerUUID) {
        if (playerUUID == null) {
            return false;
        }
        
        if (isInCompany(playerUUID)) {
            return false;
        }
        
        int maxCompanies = configManager.getConfig().getInt("companies.max-per-player", 1);
        int currentCount = getPlayerCompanyCount(playerUUID);
        
        return currentCount < maxCompanies;
    }
    
    /**
     * Gets the number of companies a player owns.
     * 
     * @param playerUUID Player UUID
     * @return Number of companies owned
     */
    public int getPlayerCompanyCount(UUID playerUUID) {
        if (playerUUID == null) {
            return 0;
        }
        
        return (int) companies.values().stream()
            .filter(company -> company.getCeoUUID().equals(playerUUID))
            .count();
    }
    
    /**
     * Adds a member to a company.
     * 
     * @param companyName Company name
     * @param playerUUID Player UUID
     * @param playerName Player name
     * @return true if member was added successfully
     */
    public boolean addMemberToCompany(String companyName, UUID playerUUID, String playerName) {
        if (companyName == null || playerUUID == null || playerName == null) {
            return false;
        }
        
        try {
            Company company = companies.get(companyName);
            if (company != null) {
                boolean added = company.addMember(playerUUID, playerName);
                if (added) {
                    playerCompanyMap.put(playerUUID, companyName);
                }
                return added;
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to add member to company: " + companyName, e);
            return false;
        }
    }
    
    /**
     * Removes a member from their company.
     * 
     * @param playerUUID Player UUID
     * @return true if member was removed successfully
     */
    public boolean removeMemberFromCompany(UUID playerUUID) {
        if (playerUUID == null) {
            return false;
        }
        
        try {
            String companyName = playerCompanyMap.get(playerUUID);
            if (companyName != null) {
                Company company = companies.get(companyName);
                if (company != null) {
                    boolean removed = company.removeMember(playerUUID);
                    if (removed) {
                        playerCompanyMap.remove(playerUUID);
                    }
                    return removed;
                }
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to remove member from company", e);
            return false;
        }
    }
    
    /**
     * Gets statistics about the data.
     * 
     * @return Map containing statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_companies", companies.size());
        stats.put("total_players", playerCompanyMap.size());
        stats.put("storage_type", storageProvider.getStorageType());
        stats.put("auto_save_enabled", autoSaveEnabled);
        stats.put("last_save_time", storageProvider.getLastSaveTime());
        return stats;
    }
    
    /**
     * Validates data integrity.
     * 
     * @return true if data is valid
     */
    public boolean validateDataIntegrity() {
        try {
            // Check for orphaned player mappings
            for (Map.Entry<UUID, String> entry : playerCompanyMap.entrySet()) {
                UUID playerUUID = entry.getKey();
                String companyName = entry.getValue();
                
                Company company = companies.get(companyName);
                if (company == null || !company.getMembers().contains(playerUUID)) {
                    plugin.getLogger().warning("Found orphaned player mapping: " + playerUUID + " -> " + companyName);
                    return false;
                }
            }
            
            // Validate storage data
            return storageProvider.validateData();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during data integrity validation", e);
            return false;
        }
    }
    
    /**
     * Reloads the data manager.
     * 
     * @return true if reload was successful
     */
    public boolean reload() {
        try {
            plugin.getLogger().info("Reloading DataManager...");
            
            companies.clear();
            playerCompanyMap.clear();
            playerCompanyCounts.clear();
            
            loadConfiguration();
            initializeStorageProvider();
            loadData();
            
            plugin.getLogger().info("DataManager reloaded successfully");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload DataManager", e);
            return false;
        }
    }
}
