package fr.dominatuin.dwcompany;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages economy operations using Vault API.
 * Handles player-to-company and company-to-player transactions.
 */
public class EconomyManager {

    private final JavaPlugin plugin;
    private final CompanyManager companyManager;
    private Economy economy;
    private boolean vaultEnabled;

    /**
     * Creates a new EconomyManager instance.
     *
     * @param plugin         The main plugin instance
     * @param companyManager The company manager for accessing company data
     */
    public EconomyManager(JavaPlugin plugin, CompanyManager companyManager) {
        this.plugin = plugin;
        this.companyManager = companyManager;
        this.vaultEnabled = false;

        setupEconomy();
    }

    /**
     * Sets up the Vault economy service.
     *
     * @return true if economy was successfully set up
     */
    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found! Economy features will be disabled.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No economy provider found! Economy features will be disabled.");
            return false;
        }

        economy = rsp.getProvider();
        vaultEnabled = true;
        plugin.getLogger().info("Successfully hooked into Vault economy: " + economy.getName());
        return true;
    }

    /**
     * Checks if Vault economy is available.
     *
     * @return true if economy is enabled
     */
    public boolean isEconomyEnabled() {
        return vaultEnabled && economy != null;
    }

    /**
     * Gets the economy instance.
     *
     * @return The economy instance, or null if not available
     */
    public Economy getEconomy() {
        return economy;
    }

    /**
     * Gets a player's balance.
     *
     * @param player The player
     * @return The player's balance
     */
    public double getPlayerBalance(Player player) {
        if (!isEconomyEnabled()) {
            return 0.0;
        }
        return economy.getBalance(player);
    }

    /**
     * Checks if a player has enough money.
     *
     * @param player The player
     * @param amount The amount to check
     * @return true if player has enough
     */
    public boolean hasEnough(Player player, double amount) {
        if (!isEconomyEnabled()) {
            return false;
        }
        return economy.has(player, amount);
    }

    /**
     * Deposits money from player to company bank.
     *
     * @param player      The player depositing
     * @param companyName The company name
     * @param amount      The amount to deposit
     * @return The transaction result
     */
    public TransactionResult depositToCompany(Player player, String companyName, double amount) {
        if (!isEconomyEnabled()) {
            return TransactionResult.NO_ECONOMY;
        }

        if (amount <= 0) {
            return TransactionResult.INVALID_AMOUNT;
        }

        if (!hasEnough(player, amount)) {
            return TransactionResult.INSUFFICIENT_FUNDS;
        }

        // Withdraw from player
        economy.withdrawPlayer(player, amount);

        // Deposit to company
        companyManager.depositToCompany(companyName, amount);

        return TransactionResult.SUCCESS;
    }

    /**
     * Withdraws money from company bank to player.
     *
     * @param player      The player withdrawing
     * @param companyName The company name
     * @param amount      The amount to withdraw
     * @return The transaction result
     */
    public TransactionResult withdrawFromCompany(Player player, String companyName, double amount) {
        if (!isEconomyEnabled()) {
            return TransactionResult.NO_ECONOMY;
        }

        if (amount <= 0) {
            return TransactionResult.INVALID_AMOUNT;
        }

        // Check company balance
        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            return TransactionResult.COMPANY_NOT_FOUND;
        }

        if (company.getBalance() < amount) {
            return TransactionResult.COMPANY_INSUFFICIENT_FUNDS;
        }

        // Withdraw from company
        if (!companyManager.withdrawFromCompany(companyName, amount)) {
            return TransactionResult.COMPANY_INSUFFICIENT_FUNDS;
        }

        // Deposit to player
        economy.depositPlayer(player, amount);

        return TransactionResult.SUCCESS;
    }

    /**
     * Gets a formatted string of a balance.
     *
     * @param amount The amount
     * @return Formatted string
     */
    public String formatMoney(double amount) {
        if (!isEconomyEnabled()) {
            return String.format("$%.2f", amount);
        }
        return economy.format(amount);
    }

    /**
     * Transaction result enum for economy operations.
     */
    public enum TransactionResult {
        SUCCESS("Transaction successful!"),
        NO_ECONOMY("§cEconomy system is not available."),
        INSUFFICIENT_FUNDS("§cYou don't have enough money."),
        COMPANY_NOT_FOUND("§cCompany not found."),
        COMPANY_INSUFFICIENT_FUNDS("§cThe company doesn't have enough funds."),
        INVALID_AMOUNT("§cInvalid amount specified."),
        NO_PERMISSION("§cYou don't have permission to do this.");

        private final String message;

        TransactionResult(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }
}
