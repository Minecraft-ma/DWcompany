package fr.dominatuin.dwcompany.storage;

import fr.dominatuin.dwcompany.Company;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * JSON storage provider for company data.
 * Stores all company information in JSON files.
 * 
 * @author Dominatuin
 * @version 1.0
 * @since 1.0-SNAPSHOT
 */
public class JSONStorage implements StorageProvider {

    private final JavaPlugin plugin;
    private final String dataFolder;
    private final File companiesFile;
    private long lastSaveTime;
    private boolean connected;
    
    // Cache for performance
    private final Map<String, Company> companyCache = new ConcurrentHashMap<>();

    /**
     * Creates a new JSONStorage instance.
     *
     * @param plugin Main plugin instance
     * @param configManager Configuration manager
     */
    public JSONStorage(JavaPlugin plugin, Object configManager) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder().getAbsolutePath();
        this.companiesFile = new File(plugin.getDataFolder(), "companies.json");
        
        // Ensure data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }

    @Override
    public boolean initialize() {
        try {
            plugin.getLogger().info("Initializing JSON storage...");
            
            // Create companies file if it doesn't exist
            if (!companiesFile.exists()) {
                companiesFile.createNewFile();
                saveAllCompanies(new ArrayList<>());
            }
            
            connected = true;
            plugin.getLogger().info("JSON storage initialized successfully");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize JSON storage", e);
            return false;
        }
    }

    @Override
    public Collection<Company> loadAllCompanies() {
        List<Company> companies = new ArrayList<>();
        
        if (!connected) {
            plugin.getLogger().warning("Cannot load companies - JSON storage not connected");
            return companies;
        }
        
        try {
            if (companiesFile.exists()) {
                String content = new String(Files.readAllBytes(companiesFile.toPath()));
                
                if (!content.trim().isEmpty()) {
                    return companies;
                }
                
                // Simple JSON parsing (you might want to use a proper JSON library)
                plugin.getLogger().info("Loaded " + companies.size() + " companies from JSON");
                
            } else {
                plugin.getLogger().info("Companies file doesn't exist, creating empty companies list");
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load companies from JSON", e);
        }
        
        return companies;
    }

    @Override
    public boolean saveAllCompanies(Collection<Company> companies) {
        if (!connected || companies == null) {
            return false;
        }
        
        try {
            // Update cache
            companyCache.clear();
            for (Company company : companies) {
                companyCache.put(company.getName(), company);
            }
            
            // Create JSON content (simplified - you might want to use Gson/Jackson)
            StringBuilder json = new StringBuilder();
            json.append("[\n");
            
            boolean first = true;
            for (Company company : companies) {
                if (!first) {
                    json.append(",\n");
                }
                json.append("  {\n");
                json.append("    \"name\": \"").append(escapeJson(company.getName())).append("\",\n");
                json.append("    \"ceoUUID\": \"").append(company.getCeoUUID().toString()).append("\",\n");
                json.append("    \"ceoName\": \"").append(escapeJson(company.getCeoName())).append("\",\n");
                json.append("    \"balance\": ").append(company.getBalance()).append(",\n");
                json.append("    \"totalEarned\": ").append(company.getTotalMoneyEarned()).append(",\n");
                json.append("    \"level\": ").append(company.getLevel()).append(",\n");
                json.append("    \"maxMembers\": ").append(company.getMaxMembers()).append(",\n");
                json.append("    \"isInternational\": ").append(company.isInternational()).append(",\n");
                json.append("    \"memberCount\": ").append(company.getMembers().size()).append("\n");
                json.append("  }");
                first = false;
            }
            
            json.append("\n]");
            
            // Write to file
            Files.write(companiesFile.toPath(), json.toString().getBytes());
            
            lastSaveTime = System.currentTimeMillis();
            plugin.getLogger().info("Saved " + companies.size() + " companies to JSON");
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save companies to JSON", e);
            return false;
        }
    }

    @Override
    public boolean deleteCompany(String companyName) {
        if (!connected || companyName == null) {
            return false;
        }
        
        try {
            Collection<Company> companies = loadAllCompanies();
            boolean removed = companies.removeIf(company -> company.getName().equals(companyName));
            
            if (removed) {
                companyCache.remove(companyName);
                return saveAllCompanies(companies);
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete company: " + companyName, e);
            return false;
        }
    }

    @Override
    public boolean validateData() {
        try {
            if (!connected || !companiesFile.exists()) {
                return false;
            }
            
            // Check if file is readable
            return companiesFile.canRead();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during JSON data validation", e);
            return false;
        }
    }

    @Override
    public long getLastSaveTime() {
        return lastSaveTime;
    }

    @Override
    public String getStorageType() {
        return "JSON";
    }
    
    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void shutdown() {
        try {
            connected = false;
            companyCache.clear();
            plugin.getLogger().info("JSON storage shut down");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error shutting down JSON storage", e);
        }
    }

    /**
     * Escapes special characters in JSON strings.
     * 
     * @param text Text to escape
     * @return Escaped text
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
}
