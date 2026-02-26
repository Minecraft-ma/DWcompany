package fr.dominatuin.dwcompany.storage;

import fr.dominatuin.dwcompany.Company;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * YAML storage provider for company data.
 * Stores all company information in companies.yml file.
 */
public class YamlStorage implements StorageProvider {

    private final JavaPlugin plugin;
    private final File dataFolder;
    private final File companiesFile;
    private FileConfiguration companiesConfig;

    private long lastSaveTime;
    private boolean connected;

    /**
     * Creates a new YamlStorage instance.
     *
     * @param plugin The plugin instance
     */
    public YamlStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.companiesFile = new File(dataFolder, "companies.yml");
        this.lastSaveTime = 0;
        this.connected = false;
    }

    @Override
    public boolean initialize() {
        try {
            // Create data folder if needed
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            // Create companies file if needed
            if (!companiesFile.exists()) {
                companiesFile.createNewFile();
            }

            companiesConfig = YamlConfiguration.loadConfiguration(companiesFile);
            connected = true;
            plugin.getLogger().info("YAML storage initialized successfully.");
            return true;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize YAML storage", e);
            connected = false;
            return false;
        }
    }

    @Override
    public void shutdown() {
        if (companiesConfig != null) {
            saveConfig();
        }
        connected = false;
    }

    @Override
    public List<Company> loadCompanies() {
        List<Company> companies = new ArrayList<>();

        if (!connected || companiesConfig == null) {
            plugin.getLogger().warning("Cannot load companies - storage not connected");
            return companies;
        }

        ConfigurationSection companiesSection = companiesConfig.getConfigurationSection("companies");
        if (companiesSection == null) {
            return companies;
        }

        for (String companyName : companiesSection.getKeys(false)) {
            try {
                ConfigurationSection companySection = companiesSection.getConfigurationSection(companyName);
                if (companySection == null) continue;

                Company company = loadCompanyFromSection(companyName, companySection);
                if (company != null) {
                    companies.add(company);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load company: " + companyName, e);
            }
        }

        plugin.getLogger().info("Loaded " + companies.size() + " companies from YAML");
        return companies;
    }

    /**
     * Loads a single company from a configuration section.
     */
    private Company loadCompanyFromSection(String name, ConfigurationSection section) {
        String ceoUUIDStr = section.getString("ceo.uuid");
        String ceoName = section.getString("ceo.name");

        if (ceoUUIDStr == null || ceoName == null) {
            plugin.getLogger().warning("Missing CEO data for company: " + name);
            return null;
        }

        UUID ceoUUID;
        try {
            ceoUUID = UUID.fromString(ceoUUIDStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid CEO UUID for company: " + name);
            return null;
        }

        Company company = new Company(name, ceoUUID, ceoName);

        // Load balance
        double balance = section.getDouble("balance", 0.0);
        if (balance > 0) {
            company.deposit(balance);
        }

        // Load members
        List<String> memberUUIDs = section.getStringList("members.uuids");
        List<String> memberNames = section.getStringList("members.names");

        for (int i = 0; i < memberUUIDs.size(); i++) {
            try {
                UUID memberUUID = UUID.fromString(memberUUIDs.get(i));
                String memberName = i < memberNames.size() ? memberNames.get(i) : "Unknown";
                if (!memberUUID.equals(ceoUUID)) {
                    company.addMember(memberUUID, memberName);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid member UUID in company " + name + ": " + memberUUIDs.get(i));
            }
        }

        // Load subsidiaries
        List<String> subsidiaries = section.getStringList("subsidiaries");
        for (String subsidiary : subsidiaries) {
            company.addSubsidiary(subsidiary);
        }

        // Load parent company
        String parentCompany = section.getString("parentCompany");
        if (parentCompany != null) {
            company.setParentCompany(parentCompany);
        }

        // Load headquarters
        String hqWorld = section.getString("headquarters.world");
        if (hqWorld != null) {
            double hqX = section.getDouble("headquarters.x", 0);
            double hqY = section.getDouble("headquarters.y", 0);
            double hqZ = section.getDouble("headquarters.z", 0);
            company.setHeadquarters(hqWorld, hqX, hqY, hqZ);
        }

        // Load international status
        boolean isInternational = section.getBoolean("international", false);
        if (isInternational) {
            company.upgradeToInternational();
        }

        return company;
    }

    @Override
    public boolean saveCompany(Company company) {
        if (!connected || companiesConfig == null) {
            return false;
        }

        String path = "companies." + company.getName();

        // CEO info
        companiesConfig.set(path + ".ceo.uuid", company.getCeoUUID().toString());
        companiesConfig.set(path + ".ceo.name", company.getCeoName());

        // Economy
        companiesConfig.set(path + ".balance", company.getBalance());
        companiesConfig.set(path + ".totalEarned", company.getTotalMoneyEarned());

        // Members
        List<String> memberUUIDs = new ArrayList<>();
        List<String> memberNames = new ArrayList<>();

        for (UUID memberUUID : company.getMembers()) {
            memberUUIDs.add(memberUUID.toString());
            memberNames.add(company.getMemberName(memberUUID));
        }

        companiesConfig.set(path + ".members.uuids", memberUUIDs);
        companiesConfig.set(path + ".members.names", memberNames);

        // Subsidiaries
        companiesConfig.set(path + ".subsidiaries", new ArrayList<>(company.getSubsidiaries()));

        // Parent company
        if (company.isSubsidiary()) {
            companiesConfig.set(path + ".parentCompany", company.getParentCompany());
        } else {
            companiesConfig.set(path + ".parentCompany", null);
        }

        // Headquarters
        if (company.hasHeadquarters()) {
            Location hq = company.getHeadquartersLocation();
            if (hq != null) {
                companiesConfig.set(path + ".headquarters.world", hq.getWorld().getName());
                companiesConfig.set(path + ".headquarters.x", hq.getX());
                companiesConfig.set(path + ".headquarters.y", hq.getY());
                companiesConfig.set(path + ".headquarters.z", hq.getZ());
            }
        } else {
            companiesConfig.set(path + ".headquarters", null);
        }

        // International status
        companiesConfig.set(path + ".international", company.isInternational());

        return saveConfig();
    }

    @Override
    public boolean saveCompanies(Collection<Company> companies) {
        if (!connected || companiesConfig == null) {
            return false;
        }

        // Clear existing companies
        companiesConfig.set("companies", null);

        // Save all companies
        for (Company company : companies) {
            saveCompany(company);
        }

        boolean success = saveConfig();
        if (success) {
            lastSaveTime = System.currentTimeMillis();
        }
        return success;
    }

    @Override
    public boolean deleteCompany(String companyName) {
        if (!connected || companiesConfig == null) {
            return false;
        }

        String path = "companies." + companyName;
        companiesConfig.set(path, null);
        return saveConfig();
    }

    /**
     * Saves the configuration to file.
     *
     * @return true if saved successfully
     */
    private boolean saveConfig() {
        try {
            companiesConfig.save(companiesFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save companies.yml", e);
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String getStorageType() {
        return "YAML";
    }

    @Override
    public long getLastSaveTime() {
        return lastSaveTime;
    }

    @Override
    public boolean validateData() {
        if (!companiesFile.exists()) {
            return true; // No data to validate
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(companiesFile);
            ConfigurationSection companiesSection = config.getConfigurationSection("companies");

            if (companiesSection == null) {
                return true;
            }

            int validCount = 0;
            int invalidCount = 0;

            for (String companyName : companiesSection.getKeys(false)) {
                ConfigurationSection section = companiesSection.getConfigurationSection(companyName);
                if (section == null) continue;

                String ceoUUID = section.getString("ceo.uuid");
                String ceoName = section.getString("ceo.name");

                if (ceoUUID == null || ceoName == null) {
                    invalidCount++;
                    plugin.getLogger().warning("Data validation: Company '" + companyName + "' has missing CEO data");
                } else {
                    validCount++;
                }
            }

            plugin.getLogger().info("Data validation: " + validCount + " valid companies, " + invalidCount + " invalid");
            return invalidCount == 0;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Data validation failed", e);
            return false;
        }
    }
}
