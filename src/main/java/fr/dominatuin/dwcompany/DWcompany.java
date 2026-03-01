package fr.dominatuin.dwcompany;

import fr.dominatuin.dwcompany.storage.DataManager;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for DWcompany.
 * Initializes all managers and registers commands and events.
 */
public final class DWcompany extends JavaPlugin {

    private static DWcompany instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private DataManager dataManager;
    private CompanyManager companyManager;
    private EconomyManager economyManager;
    private CompanyGUI companyGUI;
    private MainMenuGUI mainMenuGUI;
    private DynmapManager dynmapManager;

    /**
     * Gets the plugin instance.
     *
     * @return The DWcompany instance
     */
    public static DWcompany getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Log startup message
        getLogger().info("===================================");
        getLogger().info("DWcompany Plugin Starting...");
        getLogger().info(String.format("Version: %s", getDescription().getVersion()));
        getLogger().info("===================================");

        // Initialize managers
        if (!initializeManagers()) {
            getLogger().severe("Failed to initialize plugin! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        registerCommands();

        getLogger().info("DWcompany enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Shutdown data manager
        if (dataManager != null) {
            dataManager.shutdown();
        }

        getLogger().info("DWcompany disabled.");
        instance = null;
    }

    /**
     * Initializes all manager classes.
     *
     * @return true if initialization was successful
     */
    private boolean initializeManagers() {
        try {
            // Config manager - handles config.yml
            configManager = new ConfigManager(this);

            // Message manager - handles messages.yml
            messageManager = new MessageManager(this);

            // Company manager - handles business logic
            companyManager = new CompanyManager(this);

            // Data manager - handles storage (YAML or MySQL)
            dataManager = new DataManager(this, configManager);

            if (!dataManager.initialize()) {
                getLogger().severe("Failed to initialize data manager!");
                return false;
            }

            // Link data manager to company manager
            companyManager.setDataManager(dataManager);

            // Economy manager - handles Vault integration
            economyManager = new EconomyManager(this, companyManager);

            // GUI manager - handles inventory interfaces
            companyGUI = new CompanyGUI(this, companyManager, economyManager, messageManager, configManager);

            // Main menu GUI - clickable command menu
            mainMenuGUI = new MainMenuGUI(this, companyManager, companyGUI, configManager);

            // Dynmap manager - handles map markers (optional)
            if (configManager.isDynmapEnabled()) {
                dynmapManager = new DynmapManager(this, companyManager);
            }

            return true;

        } catch (Exception e) {
            getLogger().severe(String.format("Error initializing managers: %s", e.getMessage()));
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Registers all commands.
     */
    private void registerCommands() {
        CompanyCommandExecutor commandExecutor = new CompanyCommandExecutor(
                this, companyManager, economyManager, companyGUI,
                mainMenuGUI, dynmapManager);
        getCommand("entreprise").setExecutor(commandExecutor);
        getCommand("entreprise").setTabCompleter(commandExecutor);
    }

    /**
     * Reloads the plugin configuration and managers.
     *
     * @return true if reload was successful
     */
    public boolean reload() {
        try {
            // Reload configs
            configManager.reload();
            messageManager.reload();

            // Reload data manager
            dataManager.reload();

            getLogger().info("Plugin reloaded successfully!");
            return true;
        } catch (Exception e) {
            getLogger().severe(String.format("Error reloading plugin: %s", e.getMessage()));
            e.printStackTrace();
            return false;
        }
    }

    // ==================== Getter Methods ====================

    /**
     * Gets the ConfigManager instance.
     *
     * @return The config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the MessageManager instance.
     *
     * @return The message manager
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Gets the DataManager instance.
     *
     * @return The data manager
     */
    public DataManager getDataManager() {
        return dataManager;
    }

    /**
     * Gets the CompanyManager instance.
     *
     * @return The company manager
     */
    public CompanyManager getCompanyManager() {
        return companyManager;
    }

    /**
     * Gets the EconomyManager instance.
     *
     * @return The economy manager
     */
    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    /**
     * Gets the MainMenuGUI instance.
     *
     * @return The main menu GUI
     */
    public MainMenuGUI getMainMenuGUI() {
        return mainMenuGUI;
    }

    /**
     * Gets the CompanyGUI instance.
     *
     * @return The GUI manager
     */
    public CompanyGUI getCompanyGUI() {
        return companyGUI;
    }

    /**
     * Gets the DynmapManager instance.
     *
     * @return The Dynmap manager
     */
    public DynmapManager getDynmapManager() {
        return dynmapManager;
    }
    
    @Override
    public Server getServer() {
        return super.getServer();
    }
}
