package fr.dominatuin.dwcompany;

import org.bukkit.ChatColor;
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
 * Manages plugin messages (messages.yml).
 * Handles loading, saving, and providing access to customizable messages.
 */
public class MessageManager {

    private final JavaPlugin plugin;
    private File messagesFile;
    private FileConfiguration messages;

    // Message cache
    private final Map<String, String> messageCache;

    /**
     * Creates a new MessageManager instance.
     *
     * @param plugin The plugin instance
     */
    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messageCache = new HashMap<>();
        loadMessages();
    }

    /**
     * Loads the messages file.
     */
    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // Create default messages if it doesn't exist
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Load defaults from jar
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultMessages = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            messages.setDefaults(defaultMessages);
            messages.options().copyDefaults(true);
        }

        // Clear cache
        messageCache.clear();

        plugin.getLogger().info("Messages loaded");
    }

    /**
     * Saves the messages file.
     *
     * @return true if saved successfully
     */
    public boolean saveMessages() {
        try {
            messages.save(messagesFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save messages.yml", e);
            return false;
        }
    }

    /**
     * Reloads the messages.
     */
    public void reload() {
        loadMessages();
        plugin.getLogger().info("Messages reloaded");
    }

    /**
     * Gets a message from the configuration.
     *
     * @param path The message path
     * @param def  Default value if not found
     * @return The formatted message
     */
    public String getMessage(String path, String def) {
        // Check cache first
        if (messageCache.containsKey(path)) {
            return messageCache.get(path);
        }

        String message = messages.getString(path, def);
        if (message != null) {
            // Colorize and cache
            message = ChatColor.translateAlternateColorCodes('&', message);
            messageCache.put(path, message);
        }

        return message != null ? message : def;
    }

    /**
     * Gets a message with placeholders replaced.
     *
     * @param path         The message path
     * @param def          Default value
     * @param placeholders Placeholder map
     * @return The formatted message
     */
    public String getMessage(String path, String def, Map<String, String> placeholders) {
        String message = getMessage(path, def);

        // Replace placeholders
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return message;
    }

    /**
     * Gets a message with a single placeholder.
     *
     * @param path        The message path
     * @param def         Default value
     * @param placeholder The placeholder key
     * @param value       The placeholder value
     * @return The formatted message
     */
    public String getMessage(String path, String def, String placeholder, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder, value);
        return getMessage(path, def, placeholders);
    }

    // ==================== Common Messages ====================

    public String getPrefix() {
        return getMessage("prefix", "&6[DWcompany] ");
    }

    public String getNoPermission() {
        return getMessage("general.no-permission", "&cYou don't have permission to do this.");
    }

    public String getPlayerOnly() {
        return getMessage("general.player-only", "&cThis command can only be used by players.");
    }

    public String getInvalidArgs() {
        return getMessage("general.invalid-args", "&cInvalid arguments. Use /entreprise help for help.");
    }

    public String getSuccess() {
        return getMessage("general.success", "&aOperation successful!");
    }

    public String getError() {
        return getMessage("general.error", "&cAn error occurred.");
    }

    public String getNotInCompany() {
        return getMessage("general.not-in-company", "&cYou are not in a company.");
    }

    public String getAlreadyInCompany() {
        return getMessage("general.already-in-company", "&cYou are already in a company.");
    }

    public String getCompanyNotFound() {
        return getMessage("general.company-not-found", "&cCompany not found.");
    }

    public String getNotCEO() {
        return getMessage("general.not-ceo", "&cOnly the CEO can do this.");
    }

    // ==================== Company Creation ====================

    public String getCompanyCreated(String companyName) {
        return getMessage("company.created", "&aCompany &l{company} &ahas been created! You are now the CEO.", "company", companyName);
    }

    public String getCompanyCreateCost(double cost) {
        return getMessage("company.create-cost", "&7Creation cost: &c{cost}", "cost", String.valueOf(cost));
    }

    public String getCompanyAlreadyExists() {
        return getMessage("company.already-exists", "&cA company with this name already exists.");
    }

    public String getMaxCompaniesReached(int max) {
        return getMessage("company.max-companies", "&cYou can only own a maximum of {max} companies.", "max", String.valueOf(max));
    }

    public String getNotEnoughMoney(double required) {
        return getMessage("economy.not-enough-money", "&cYou need ${required} to do this.", "required", String.valueOf(required));
    }

    // ==================== Join/Leave ====================

    public String getJoinRequestSent(String company) {
        return getMessage("join.request-sent", "&aJoin request sent to &l{company}&a.", "company", company);
    }

    public String getJoinRequestReceived(String player) {
        return getMessage("join.request-received", "&e{player} &ewants to join your company.", "player", player);
    }

    public String getPlayerJoined(String player) {
        return getMessage("join.player-joined", "&a&l{player} &ahas joined your company!", "player", player);
    }

    public String getYouJoined(String company) {
        return getMessage("join.you-joined", "&aYou have joined &l{company}&a!", "company", company);
    }

    public String getCompanyFull() {
        return getMessage("join.company-full", "&cThis company is full.");
    }

    public String getLeftCompany(String company) {
        return getMessage("leave.left", "&aYou have left the company &l{company}&a.", "company", company);
    }

    public String getCEOTransferRequired() {
        return getMessage("leave.ceo-transfer-required", "&cAs CEO, you must transfer ownership before leaving.");
    }

    // ==================== Bank ====================

    public String getDepositSuccess(double amount, double balance) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(amount));
        placeholders.put("balance", String.valueOf(balance));
        return getMessage("bank.deposit-success", "&aDeposited &l${amount}&a. New balance: &a${balance}", placeholders);
    }

    public String getWithdrawSuccess(double amount, double balance) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(amount));
        placeholders.put("balance", String.valueOf(balance));
        return getMessage("bank.withdraw-success", "&aWithdrawn &l${amount}&a. New balance: &a${balance}", placeholders);
    }

    public String getNotEnoughBalance() {
        return getMessage("bank.not-enough-balance", "&cThe company doesn't have enough funds.");
    }

    // ==================== Status (National/International) ====================

    public String getUpgradedToInternational(String company) {
        return getMessage("status.upgraded-international", "&6&l{company} &6is now International!", "company", company);
    }

    public String getInternationalCost(double cost) {
        return getMessage("status.international-cost", "&7Cost: &c${cost}", "cost", String.valueOf(cost));
    }

    // ==================== Headquarters ====================

    public String getHQSet(String location) {
        return getMessage("hq.set", "&aHeadquarters set! Location: &f{location}", "location", location);
    }

    public String getHQMarkerAdded() {
        return getMessage("hq.marker-added", "&7Marker added to Dynmap.");
    }

    // ==================== GUI Titles ====================

    public String getGUITitleCompanyList() {
        return getMessage("gui.titles.company-list", "&6&lCompanies");
    }

    public String getGUITitleCompanyDetails() {
        return getMessage("gui.titles.company-details", "&6&lCompany Info");
    }

    public String getGUITitleBank() {
        return getMessage("gui.titles.bank", "&6&lCompany Bank");
    }

    public String getGUITitleManagement() {
        return getMessage("gui.titles.management", "&c&lManage Company");
    }

    // ==================== Reload ====================

    public String getReloadSuccess() {
        return getMessage("admin.reload-success", "&aPlugin reloaded successfully!");
    }

    public String getReloadError() {
        return getMessage("admin.reload-error", "&cError reloading plugin. Check console.");
    }

    /**
     * Gets the raw FileConfiguration.
     *
     * @return The messages configuration
     */
    public FileConfiguration getMessagesConfig() {
        return messages;
    }
}
