package fr.dominatuin.dwcompany;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import fr.dominatuin.dwcompany.Company;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive GUI system for company management.
 * 
 * <p>This class handles all inventory-based user interfaces including:</p>
 * <ul>
 *   <li>Company list and selection</li>
 *   <li>Company details and statistics</li>
 *   <li>Bank deposit/withdraw interface</li>
 *   <li>Member management</li>
 *   <li>Subsidiary management</li>
 *   <li>Company settings</li>
 * </ul>
 * 
 * @author Dominatuin
 * @version 1.0
 * @since 1.0-SNAPSHOT
 */
public class CompanyGUI implements Listener {

    private final DWcompany plugin;
    private final CompanyManager companyManager;
    private final EconomyManager economyManager;
    private final MessageManager messageManager;
    private final ConfigManager configManager;

    // ======== GUI State Management ========
    /** Tracks which company each player is currently viewing */
    private final Map<UUID, String> playerViewingCompany;
    /** Tracks current page for each player's GUI */
    private final Map<UUID, Integer> playerPage;
    /** Tracks GUI type for each player */
    private final Map<UUID, GUIType> playerGUIType;

    // ======== GUI Constants ========
    /** Standard GUI size (6 rows) */
    private static final int GUI_SIZE = 54;
    /** Companies to display per page */
    private final int companiesPerPage;
    /** Navigation slot positions */
    private static final int[] NAVIGATION_SLOTS = {45, 46, 47, 48, 49, 50, 51, 52, 53};
    /** Content slot positions (first 5 rows) */
    private static final int[] CONTENT_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    /** GUI type enumeration */
    private enum GUIType {
        COMPANY_LIST, COMPANY_DETAILS, BANK, MEMBERS, SUBSIDIARIES, SETTINGS
    }

    /**
     * Creates a new CompanyGUI instance.
     * 
     * @param plugin Main plugin instance
     * @param companyManager Company management system
     * @param economyManager Economy management system  
     * @param messageManager Message handling system
     * @param configManager Configuration management
     */
    public CompanyGUI(DWcompany plugin, CompanyManager companyManager, EconomyManager economyManager,
                      MessageManager messageManager, ConfigManager configManager) {
        // Initialize dependencies
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.companyManager = Objects.requireNonNull(companyManager, "Company manager cannot be null");
        this.economyManager = Objects.requireNonNull(economyManager, "Economy manager cannot be null");
        this.messageManager = Objects.requireNonNull(messageManager, "Message manager cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "Config manager cannot be null");
        
        // Initialize state tracking
        this.playerViewingCompany = new ConcurrentHashMap<>();
        this.playerPage = new ConcurrentHashMap<>();
        this.playerGUIType = new ConcurrentHashMap<>();
        
        // Load configuration
        this.companiesPerPage = configManager.getInt("gui.companies-per-page", 10);
        
        // Register events
        registerEvents();
    }

    /**
     * Registers this GUI handler with the plugin.
     * Called automatically during construction.
     */
    private void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Opens the company list GUI for a player.
     *
     * @param player The player
     * @param page   The page number (0-indexed)
     */
    public void openCompanyList(Player player, int page) {
        List<Company> companies = companyManager.getCompaniesSortedByEarnings();

        if (companies.isEmpty()) {
            player.sendMessage(messageManager.getMessage("general.not-in-company", "§cNo companies exist yet."));
            return;
        }

        int totalPages = (int) Math.ceil(companies.size() / (double) companiesPerPage);
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, messageManager.getGUITitleCompanyList() + " - Page " + (page + 1));

        // Add company icons
        int startIndex = page * companiesPerPage;
        int endIndex = Math.min(startIndex + companiesPerPage, companies.size());

        for (int i = startIndex; i < endIndex; i++) {
            Company company = companies.get(i);
            ItemStack icon = createCompanyIcon(company, i + 1);
            gui.setItem(i - startIndex, icon);
        }

        // Add navigation buttons
        if (page > 0) {
            gui.setItem(45, createNavigationItem(Material.ARROW, "§ePrevious Page", page - 1));
        }
        if (page < totalPages - 1) {
            gui.setItem(53, createNavigationItem(Material.ARROW, "§eNext Page", page + 1));
        }

        // Info item
        gui.setItem(49, createInfoItem("§6Your Company", player));

        playerPage.put(player.getUniqueId(), page);
        player.openInventory(gui);
    }

    /**
     * Opens the company details GUI.
     *
     * @param player      The player
     * @param companyName The company name to view
     */
    public void openCompanyDetails(Player player, String companyName) {
        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage("§cCompany not found.");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, "§6§l" + company.getName());

        // Company info - with status display
        gui.setItem(4, createCompanyIcon(company, 0));

        // CEO Info
        gui.setItem(10, createPlayerHeadItem(company.getCeoName(), "§6CEO", "§7The leader of this company"));

        // Member count with max
        gui.setItem(12, createStatItem(Material.PLAYER_HEAD, "§eMembers",
                "§7" + company.getMemberCount() + "/" + company.getMaxMembers()));

        // Total earned
        gui.setItem(14, createStatItem(Material.GOLD_INGOT, "§eTotal Earned",
                economyManager.formatMoney(company.getTotalMoneyEarned())));

        // Current balance
        gui.setItem(16, createStatItem(Material.EMERALD, "§eBalance",
                economyManager.formatMoney(company.getBalance())));

        // Level
        gui.setItem(18, createStatItem(Material.EXPERIENCE_BOTTLE, "§eLevel",
                company.getLevelColor() + "Level " + company.getLevel()));

        // Status (National/International)
        gui.setItem(20, createStatItem(Material.GLOBE_BANNER_PATTERN, "§eStatus",
                company.getStatusColor() + company.getStatusDisplay()));

        // Headquarters location
        gui.setItem(22, createStatItem(Material.COMPASS, "§eHeadquarters",
                "§7" + company.getHeadquartersString()));

        // Subsidiaries
        if (!company.getSubsidiaries().isEmpty()) {
            gui.setItem(24, createSubsidiariesItem(company));
        }

        // Actions
        Company playerCompany = companyManager.getPlayerCompany(player.getUniqueId());

        // Join request button (if not in company)
        if (playerCompany == null) {
            gui.setItem(25, createActionItem(Material.LIME_WOOL, "§aRequest to Join", "§7Click to send a join request"));
        }

        // Back button
        gui.setItem(26, createNavigationItem(Material.BARRIER, "§cBack to List", -1));

        playerViewingCompany.put(player.getUniqueId(), companyName);
        playSound(player, Sound.UI_BUTTON_CLICK);
        player.openInventory(gui);
    }

    /**
     * Opens the company bank GUI.
     *
     * @param player The player
     */
    public void openCompanyBank(Player player) {
        Company playerCompany = companyManager.getPlayerCompany(player.getUniqueId());
        if (playerCompany == null) {
            player.sendMessage("§cYou are not in a company.");
            return;
        }
        String companyName = playerCompany.getName();

        Company company = playerCompany;
        if (company == null) {
            player.sendMessage("§cCompany not found.");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, "§6§lCompany Bank - " + companyName);

        // Balance display
        gui.setItem(4, createStatItem(Material.EMERALD_BLOCK, "§aCurrent Balance", economyManager.formatMoney(company.getBalance())));

        // Deposit buttons
        gui.setItem(11, createActionItem(Material.LIME_WOOL, "§aDeposit $100", "§7Click to deposit $100"));
        gui.setItem(12, createActionItem(Material.LIME_WOOL, "§aDeposit $500", "§7Click to deposit $500"));
        gui.setItem(13, createActionItem(Material.LIME_WOOL, "§aDeposit $1000", "§7Click to deposit $1000"));
        gui.setItem(14, createActionItem(Material.LIME_WOOL, "§aDeposit $5000", "§7Click to deposit $5000"));

        // Withdraw buttons (only for CEO or members with permission)
        boolean canWithdraw = company.isCEO(player.getUniqueId()) ||
                player.hasPermission("dwcompany.bank.withdraw");

        if (canWithdraw) {
            gui.setItem(15, createActionItem(Material.RED_WOOL, "§cWithdraw $100", "§7Click to withdraw $100"));
            gui.setItem(16, createActionItem(Material.RED_WOOL, "§cWithdraw $500", "§7Click to withdraw $500"));
            gui.setItem(17, createActionItem(Material.RED_WOOL, "§cWithdraw $1000", "§7Click to withdraw $1000"));
        }

        // Custom amount
        gui.setItem(22, createActionItem(Material.PAPER, "§eCustom Amount", "§7Use /entreprise bank deposit/withdraw <amount>"));

        // Close button
        gui.setItem(26, createNavigationItem(Material.BARRIER, "§cClose", -2));

        playerViewingCompany.put(player.getUniqueId(), "BANK:" + companyName);
        player.openInventory(gui);
    }

    /**
     * Opens the company management GUI for CEO.
     *
     * @param player The CEO player
     */
    public void openCompanyManagement(Player player) {
        Company playerCompany = companyManager.getPlayerCompany(player.getUniqueId());
        if (playerCompany == null) {
            player.sendMessage("§cYou are not in a company.");
            return;
        }
        String companyName = playerCompany.getName();

        Company company = playerCompany;
        if (company == null || !company.isCEO(player.getUniqueId())) {
            player.sendMessage("§cOnly the CEO can manage the company.");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, "§c§lManage - " + companyName);

        // Member management
        gui.setItem(10, createActionItem(Material.PLAYER_HEAD, "§eManage Members", "§7Click to view and manage members"));

        // Transfer ownership
        gui.setItem(12, createActionItem(Material.GOLDEN_HELMET, "§6Transfer Ownership", "§7Transfer CEO role to another member"));

        // Create subsidiary
        gui.setItem(14, createActionItem(Material.CHEST, "§aCreate Subsidiary", "§7Create a new subsidiary company"));

        // Add existing company as subsidiary
        gui.setItem(16, createActionItem(Material.CHEST_MINECART, "§aAdd Subsidiary", "§7Add an existing company as subsidiary"));

        // Join requests
        if (!company.getPendingJoinRequests().isEmpty()) {
            gui.setItem(20, createActionItem(Material.BOOK, "§eJoin Requests", "§7" + company.getPendingJoinRequests().size() + " pending requests"));
        }

        // Delete company
        gui.setItem(24, createActionItem(Material.TNT, "§c§lDelete Company", "§7§lWARNING: This cannot be undone!"));

        // Back/close
        gui.setItem(26, createNavigationItem(Material.BARRIER, "§cClose", -2));

        playerViewingCompany.put(player.getUniqueId(), "MANAGE:" + companyName);
        player.openInventory(gui);
    }

    // ==================== Item Creation Methods ====================

    private ItemStack createCompanyIcon(Company company, int rank) {
        Material material = company.getLevelIcon();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String color = company.getLevelColor();
        meta.setDisplayName(color + "§l" + company.getName());

        List<String> lore = new ArrayList<>();
        if (rank > 0) {
            lore.add("§7Rank: §f#" + rank);
        }
        lore.add("§7Level: " + color + company.getLevel());
        lore.add("§7CEO: §f" + company.getCeoName());
        lore.add("§7Status: " + company.getStatusColor() + company.getStatusDisplay());
        lore.add("§7Members: §f" + company.getMemberCount() + "/" + company.getMaxMembers());
        lore.add("§7Total Earned: §a" + economyManager.formatMoney(company.getTotalMoneyEarned()));
        lore.add("§7Balance: §a" + economyManager.formatMoney(company.getBalance()));

        if (company.hasHeadquarters()) {
            lore.add("§7HQ: §f" + company.getHeadquartersString());
        }

        if (company.isSubsidiary()) {
            lore.add("§7Subsidiary of: §f" + company.getParentCompany());
        }

        if (!company.getSubsidiaries().isEmpty()) {
            lore.add("§7Subsidiaries: §f" + company.getSubsidiaries().size());
        }

        lore.add("");
        lore.add("§eClick for details");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavigationItem(Material material, String name, int targetPage) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add("§7Page: " + (targetPage + 1));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(String title, Player player) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);

        List<String> lore = new ArrayList<>();
        Company playerCompany = companyManager.getPlayerCompany(player.getUniqueId());
        if (playerCompany != null) {
            Company company = playerCompany;
            if (company != null) {
                lore.add("§7Company: §f" + company.getName());
                lore.add("§7Role: " + (company.isCEO(player.getUniqueId()) ? "§6CEO" : "§7Member"));
                lore.add("§7Balance: §a" + economyManager.formatMoney(company.getBalance()));
            }
        } else {
            lore.add("§7You are not in a company");
            lore.add("§e/entreprise create <name>");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerHeadItem(String playerName, String title, String description) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);

        List<String> lore = new ArrayList<>();
        lore.add("§7" + playerName);
        lore.add("");
        lore.add(description);

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatItem(Material material, String title, String value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);

        List<String> lore = new ArrayList<>();
        lore.add(value);

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSubsidiariesItem(Company company) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aSubsidiaries");

        List<String> lore = new ArrayList<>();
        for (String subName : company.getSubsidiaries()) {
            Company sub = companyManager.getCompany(subName);
            if (sub != null) {
                lore.add("§7- " + sub.getLevelColor() + subName);
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates an action item with click functionality.
     * 
     * @param material Material for the item
     * @param name Display name for the item
     * @param description Description text for lore
     * @return Configured ItemStack for actions
     */
    private ItemStack createActionItem(Material material, String name, String description) {
        if (material == null || name == null) {
            return new ItemStack(Material.BARRIER);
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        if (description != null && !description.trim().isEmpty()) {
            lore.add("§7" + description);
        }
        lore.add("§eClick to execute");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ==================== Event Handlers ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Check if this is one of our GUIs
        if (!title.startsWith("§6§l") && !title.startsWith("§c§l")) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        // Handle company list
        if (title.startsWith("§6§lCompanies - Page ")) {
            handleCompanyListClick(player, clicked, title);
            return;
        }

        // Handle company details
        if (title.startsWith("§6§l") && !title.contains("Bank") && !title.contains("Companies")) {
            handleCompanyDetailsClick(player, clicked, title.substring(6));
            return;
        }

        // Handle bank
        if (title.contains("Bank")) {
            handleBankClick(player, clicked);
            return;
        }

        // Handle management
        if (title.startsWith("§c§lManage")) {
            handleManagementClick(player, clicked);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            playerViewingCompany.remove(event.getPlayer().getUniqueId());
            playerPage.remove(event.getPlayer().getUniqueId());
        }
    }

    private void handleCompanyListClick(Player player, ItemStack clicked, String title) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String name = meta.getDisplayName();

        // Check for navigation
        if (name.equals("§ePrevious Page")) {
            int currentPage = playerPage.getOrDefault(player.getUniqueId(), 0);
            openCompanyList(player, currentPage - 1);
            return;
        }

        if (name.equals("§eNext Page")) {
            int currentPage = playerPage.getOrDefault(player.getUniqueId(), 0);
            openCompanyList(player, currentPage + 1);
            return;
        }

        // Check for company click (colored name)
        if (name.startsWith("§")) {
            // Extract company name from display name
            String cleanName = name.replaceAll("§[0-9a-fk-or]", "").replace("§l", "");
            openCompanyDetails(player, cleanName);
        }
    }

    private void handleCompanyDetailsClick(Player player, ItemStack clicked, String companyName) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String name = meta.getDisplayName();

        if (name.equals("§cBack to List")) {
            playSound(player, Sound.UI_BUTTON_CLICK);
            int page = playerPage.getOrDefault(player.getUniqueId(), 0);
            openCompanyList(player, page);
            return;
        }

        if (name.equals("§aRequest to Join")) {
            playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
            player.performCommand("entreprise join " + companyName);
            player.closeInventory();
        }
    }

    private void handleBankClick(Player player, ItemStack clicked) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String name = meta.getDisplayName();

        if (name.equals("§cClose")) {
            playSound(player, Sound.UI_BUTTON_CLICK);
            player.closeInventory();
            return;
        }

        // Handle deposits
        if (name.contains("Deposit")) {
            playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
            int amount = extractAmount(name);
            if (amount > 0) {
                player.performCommand("entreprise bank deposit " + amount);
                // Refresh the GUI
                openCompanyBank(player);
            }
            return;
        }

        // Handle withdrawals
        if (name.contains("Withdraw")) {
            playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
            int amount = extractAmount(name);
            if (amount > 0) {
                player.performCommand("entreprise bank withdraw " + amount);
                // Refresh the GUI
                openCompanyBank(player);
            }
        }
    }

    private void handleManagementClick(Player player, ItemStack clicked) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String name = meta.getDisplayName();
        Company playerCompany = companyManager.getPlayerCompany(player.getUniqueId());
        String companyName = playerCompany != null ? playerCompany.getName() : null;

        if (name.equals("§cClose")) {
            playSound(player, Sound.UI_BUTTON_CLICK);
            player.closeInventory();
            return;
        }

        if (name.equals("§eManage Members")) {
            playSound(player, Sound.UI_BUTTON_CLICK);
            player.sendMessage("§eUse /entreprise members to view and manage members.");
            player.closeInventory();
            return;
        }

        if (name.equals("§6Transfer Ownership")) {
            playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
            player.sendMessage("§eUse /entreprise transfer <player> to transfer ownership.");
            player.closeInventory();
            return;
        }

        if (name.equals("§aCreate Subsidiary")) {
            playSound(player, Sound.UI_BUTTON_CLICK);
            player.sendMessage("§eUse /entreprise filiale create <name> to create a subsidiary.");
            player.closeInventory();
            return;
        }

        if (name.equals("§aAdd Subsidiary")) {
            playSound(player, Sound.UI_BUTTON_CLICK);
            player.sendMessage("§eUse /entreprise filiale add <company> to add a subsidiary.");
            player.closeInventory();
            return;
        }

        if (name.equals("§eJoin Requests")) {
            playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL);
            player.sendMessage("§ePending join requests. Use /entreprise requests to manage them.");
            player.closeInventory();
            return;
        }

        if (name.equals("§c§lDelete Company")) {
            playSound(player, Sound.BLOCK_ANVIL_PLACE);
            player.sendMessage("§c§lUse /entreprise delete to delete your company. This cannot be undone!");
            player.closeInventory();
        }
    }

    private int extractAmount(String name) {
        try {
            String clean = name.replaceAll("[^0-9]", "");
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ==================== Sound Effects ====================

    /**
     * Plays a sound for the player.
     *
     * @param player The player to play sound for
     * @param sound  The sound to play
     */
    public void playSound(Player player, Sound sound) {
        if (player != null && player.isOnline() && configManager.areSoundsEnabled()) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    /**
     * Plays a success sound (Level Up).
     *
     * @param player The player
     */
    public void playSuccessSound(Player player) {
        if (player != null && player.isOnline() && configManager.areSoundsEnabled()) {
            try {
                Sound sound = Sound.valueOf(configManager.getSuccessSound());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }
    }

    /**
     * Plays an error sound.
     *
     * @param player The player
     */
    public void playErrorSound(Player player) {
        if (player != null && player.isOnline() && configManager.areSoundsEnabled()) {
            try {
                Sound sound = Sound.valueOf(configManager.getErrorSound());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    /**
     * Plays a button click sound.
     *
     * @param player The player
     */
    public void playButtonClickSound(Player player) {
        if (player != null && player.isOnline() && configManager.areSoundsEnabled()) {
            try {
                Sound sound = Sound.valueOf(configManager.getButtonClickSound());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }
    }

    // ==================== Public Utility Methods ====================

    /**
     * Gets the company name a player is currently viewing.
     *
     * @param playerUUID The player's UUID (must not be null)
     * @return The company name, or null if not viewing
     */
    public String getPlayerViewingCompany(UUID playerUUID) {
        if (playerUUID == null) {
            return null;
        }
        return playerViewingCompany.get(playerUUID);
    }

    /**
     * Safely closes a player's GUI and cleans up state.
     * 
     * @param player The player to clean up for
     */
    public void closePlayerGUI(Player player) {
        if (player == null) {
            return;
        }
        
        UUID playerUUID = player.getUniqueId();
        playerViewingCompany.remove(playerUUID);
        playerPage.remove(playerUUID);
        playerGUIType.remove(playerUUID);
        
        // Close any open inventory
        player.closeInventory();
    }

    /**
     * Validates if a slot is clickable in the GUI.
     * 
     * @param slot Slot number to check
     * @return true if slot is clickable, false otherwise
     */
    private boolean isClickableSlot(int slot) {
        if (slot < 0 || slot >= GUI_SIZE) {
            return false;
        }
        
        // Check if slot is in content area (first 5 rows)
        for (int contentSlot : CONTENT_SLOTS) {
            if (slot == contentSlot) {
                return true;
            }
        }
        
        // Check if slot is in navigation area (bottom row)
        for (int navSlot : NAVIGATION_SLOTS) {
            if (slot == navSlot) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Gets the current page for a player.
     * 
     * @param playerUUID The player's UUID
     * @return Current page number, or 0 if not set
     */
    public int getPlayerPage(UUID playerUUID) {
        if (playerUUID == null) {
            return 0;
        }
        return playerPage.getOrDefault(playerUUID, 0);
    }

    /**
     * Handles GUI errors gracefully with logging and user feedback.
     * 
     * @param player The player to notify
     * @param error Error message to display
     * @param context Context where error occurred
     */
    private void handleGUIError(Player player, String error, String context) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        // Log error for debugging
        plugin.getLogger().warning(String.format("GUI Error in %s: %s", context, error));
        
        // Notify player
        player.sendMessage("§cError: " + error);
        playErrorSound(player);
        
        // Close GUI to prevent further issues
        closePlayerGUI(player);
    }

    /**
     * Clears a player's viewing state.
     *
     * @param playerUUID The player's UUID
     */
    public void clearPlayerView(UUID playerUUID) {
        playerViewingCompany.remove(playerUUID);
        playerPage.remove(playerUUID);
    }
}
