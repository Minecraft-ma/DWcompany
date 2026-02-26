package fr.dominatuin.dwcompany;

import fr.dominatuin.dwcompany.storage.DataManager;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages all company business logic and delegates storage to DataManager.
 * This class handles company operations while DataManager handles persistence.
 */
public class CompanyManager {

    private final JavaPlugin plugin;
    private DataManager dataManager;

    /**
     * Creates a new CompanyManager instance.
     *
     * @param plugin The main plugin instance
     */
    public CompanyManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets the DataManager for this CompanyManager.
     *
     * @param dataManager The data manager
     */
    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Creates a new company.
     *
     * @param name    The company name
     * @param ceoUUID The CEO's UUID
     * @param ceoName The CEO's name
     * @return true if created successfully
     */
    public boolean createCompany(String name, UUID ceoUUID, String ceoName) {
        if (dataManager == null) return false;

        // Check if name exists
        if (dataManager.getCompany(name) != null) {
            return false;
        }

        // Check if player is already in a company
        if (dataManager.isInCompany(ceoUUID)) {
            return false;
        }

        Company company = new Company(name, ceoUUID, ceoName);
        return dataManager.createCompany(company);
    }

    /**
     * Deletes a company.
     *
     * @param name The company name
     * @return true if deleted successfully
     */
    public boolean deleteCompany(String name) {
        if (dataManager == null) return false;
        return dataManager.deleteCompany(name);
    }

    /**
     * Gets a company by name.
     *
     * @param name The company name
     * @return The Company, or null if not found
     */
    public Company getCompany(String name) {
        if (dataManager == null) return null;
        return dataManager.getCompany(name);
    }

    /**
     * Gets all companies.
     *
     * @return Collection of all companies
     */
    public Collection<Company> getAllCompanies() {
        if (dataManager == null) return new ArrayList<>();
        return dataManager.getAllCompanies();
    }

    /**
     * Gets a player's company.
     *
     * @param playerUUID The player's UUID
     * @return The company name, or null if not in a company
     */
    public String getPlayerCompany(UUID playerUUID) {
        if (dataManager == null) return null;
        return dataManager.getPlayerCompany(playerUUID);
    }

    /**
     * Checks if a player is in a company.
     *
     * @param playerUUID The player's UUID
     * @return true if the player is in a company
     */
    public boolean isInCompany(UUID playerUUID) {
        if (dataManager == null) return false;
        return dataManager.isInCompany(playerUUID);
    }

    /**
     * Gets the number of companies a player owns.
     *
     * @param playerUUID The player's UUID
     * @return Number of companies owned
     */
    public int getPlayerCompanyCount(UUID playerUUID) {
        if (dataManager == null) return 0;
        return dataManager.getPlayerCompanyCount(playerUUID);
    }

    /**
     * Checks if a player can create more companies.
     *
     * @param playerUUID The player's UUID
     * @return true if player can create another company
     */
    public boolean canCreateCompany(UUID playerUUID) {
        if (dataManager == null) return false;
        return dataManager.canCreateCompany(playerUUID);
    }

    /**
     * Adds a player to a company.
     *
     * @param companyName The company name
     * @param playerUUID  The player's UUID
     * @param playerName  The player's name
     * @return true if added successfully
     */
    public boolean addMemberToCompany(String companyName, UUID playerUUID, String playerName) {
        if (dataManager == null) return false;
        return dataManager.addMemberToCompany(companyName, playerUUID, playerName);
    }

    /**
     * Removes a player from their company.
     *
     * @param playerUUID The player's UUID
     * @return true if removed successfully
     */
    public boolean removeMemberFromCompany(UUID playerUUID) {
        if (dataManager == null) return false;
        return dataManager.removeMemberFromCompany(playerUUID);
    }

    /**
     * Transfers company ownership to a new CEO.
     *
     * @param companyName The company name
     * @param newCeoUUID  The new CEO's UUID
     * @param newCeoName  The new CEO's name
     * @return true if transferred successfully
     */
    public boolean transferOwnership(String companyName, UUID newCeoUUID, String newCeoName) {
        Company company = getCompany(companyName);
        if (company == null) return false;

        if (!company.hasMember(newCeoUUID)) return false;

        company.setCeo(newCeoUUID, newCeoName);
        saveData();
        return true;
    }

    /**
     * Creates a subsidiary relationship.
     *
     * @param parentName     The parent company name
     * @param subsidiaryName The subsidiary company name
     * @return true if successful
     */
    public boolean addSubsidiary(String parentName, String subsidiaryName) {
        Company parent = getCompany(parentName);
        Company subsidiary = getCompany(subsidiaryName);

        if (parent == null || subsidiary == null) return false;
        if (subsidiary.isSubsidiary()) return false;

        parent.addSubsidiary(subsidiaryName);
        subsidiary.setParentCompany(parentName);
        saveData();
        return true;
    }

    /**
     * Removes a subsidiary relationship.
     *
     * @param parentName     The parent company name
     * @param subsidiaryName The subsidiary company name
     * @return true if removed successfully
     */
    public boolean removeSubsidiary(String parentName, String subsidiaryName) {
        Company parent = getCompany(parentName);
        Company subsidiary = getCompany(subsidiaryName);

        if (parent == null || subsidiary == null) return false;

        parent.removeSubsidiary(subsidiaryName);
        subsidiary.setParentCompany(null);
        saveData();
        return true;
    }

    /**
     * Deposits money into a company's bank.
     *
     * @param companyName The company name
     * @param amount      The amount to deposit
     */
    public void depositToCompany(String companyName, double amount) {
        Company company = getCompany(companyName);
        if (company != null) {
            company.deposit(amount);
            saveData();
        }
    }

    /**
     * Withdraws money from a company's bank.
     *
     * @param companyName The company name
     * @param amount      The amount to withdraw
     * @return true if withdrawn successfully
     */
    public boolean withdrawFromCompany(String companyName, double amount) {
        Company company = getCompany(companyName);
        if (company != null && company.withdraw(amount)) {
            saveData();
            return true;
        }
        return false;
    }

    /**
     * Sets a company's headquarters location.
     *
     * @param companyName The company name
     * @param location    The location to set
     * @return true if set successfully
     */
    public boolean setCompanyHeadquarters(String companyName, Location location) {
        Company company = getCompany(companyName);
        if (company == null || location == null) return false;

        company.setHeadquarters(location);
        saveData();
        return true;
    }

    /**
     * Upgrades a company to International status.
     *
     * @param companyName The company name
     * @return true if upgraded successfully
     */
    public boolean upgradeToInternational(String companyName) {
        Company company = getCompany(companyName);
        if (company == null) return false;

        if (company.upgradeToInternational()) {
            saveData();
            return true;
        }
        return false;
    }

    /**
     * Gets companies sorted by total money earned.
     *
     * @return List of companies sorted by earnings
     */
    public List<Company> getCompaniesSortedByEarnings() {
        return getAllCompanies().stream()
                .sorted((c1, c2) -> Double.compare(c2.getTotalMoneyEarned(), c1.getTotalMoneyEarned()))
                .collect(Collectors.toList());
    }

    /**
     * Gets companies sorted by level.
     *
     * @return List of companies sorted by level
     */
    public List<Company> getCompaniesSortedByLevel() {
        return getAllCompanies().stream()
                .sorted((c1, c2) -> Integer.compare(c2.getLevel(), c1.getLevel()))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a company name is available.
     *
     * @param name The name to check
     * @return true if available
     */
    public boolean isNameAvailable(String name) {
        return getCompany(name) == null;
    }

    /**
     * Gets the total number of companies.
     *
     * @return Number of companies
     */
    public int getCompanyCount() {
        return getAllCompanies().size();
    }

    /**
     * Saves all data.
     */
    public void saveData() {
        if (dataManager != null) {
            dataManager.saveAllCompanies(true);
        }
    }

    /**
     * Saves all data asynchronously.
     */
    public void saveDataAsync() {
        if (dataManager != null) {
            dataManager.saveAllCompaniesAsync();
        }
    }
}
