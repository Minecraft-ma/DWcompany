package fr.dominatuin.dwcompany;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;

/**
 * Company class representing a business entity in the game.
 * 
 * <p>This class manages company information including members, finances,
 * headquarters location, subsidiary relationships, and join requests.</p>
 * 
 * @author Dominatuin
 * @version 1.0
 * @since 1.0-SNAPSHOT
 */
public class Company {

    // ======== Basic Information ========
    /** Company name - unique identifier */
    private final String name;
    /** CEO UUID */
    private UUID ceoUUID;
    /** CEO display name */
    private String ceoName;

    // ======== Member Management ========
    /** Set of all company members including CEO */
    private final Set<UUID> members = new HashSet<>();
    /** Pending join requests */
    private final Set<UUID> pendingJoinRequests = new HashSet<>();

    // ======== Company Status ========
    /** Maximum number of members allowed */
    private int maxMembers = 5;
    /** Whether company is international */
    private boolean isInternational = false;
    /** Whether company is a subsidiary of another company */
    private boolean isSubsidiary = false;
    /** Parent company name if subsidiary */
    private String parentCompany;
    /** Set of subsidiary company names */
    private final Set<String> subsidiaries = new HashSet<>();

    // ======== Financial Information ========
    /** Current company balance */
    private double balance;
    /** Total money earned since creation */
    private double totalMoneyEarned;
    /** Company level based on earnings */
    private int level = 1;

    // ======== Headquarters Information ========
    /** Headquarters world name */
    private String headquartersWorld;
    /** Headquarters X coordinate */
    private double headquartersX;
    /** Headquarters Y coordinate */
    private double headquartersY;
    /** Headquarters Z coordinate */
    private double headquartersZ;
    /** Whether headquarters is set */
    private boolean hasHeadquarters;

    // ======== Constants ========
    /** Level thresholds for company progression */
    private static final double[] LEVEL_THRESHOLDS =
            {0, 10000, 50000, 100000, 250000, 500000, 1000000};
    /** National member limit */
    public static final int NATIONAL_MEMBER_LIMIT = 5;
    /** International member limit */
    public static final int INTERNATIONAL_MEMBER_LIMIT = 10;
    /** Cost to upgrade to international */
    public static final double INTERNATIONAL_UPGRADE_COST = 20000.0;

    /**
     * Creates a new Company instance.
     * 
     * @param name Company name (must not be null or empty)
     * @param ceoUUID CEO UUID (must not be null)
     * @param ceoName CEO display name (must not be null)
     * @throws IllegalArgumentException if any parameter is null or invalid
     */
    public Company(String name, UUID ceoUUID, String ceoName) {
        // Validate input parameters
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Company name cannot be null or empty");
        }
        if (ceoUUID == null) {
            throw new IllegalArgumentException("CEO UUID cannot be null");
        }
        if (ceoName == null || ceoName.trim().isEmpty()) {
            throw new IllegalArgumentException("CEO name cannot be null or empty");
        }

        // Initialize basic information
        this.name = name.trim();
        this.ceoUUID = ceoUUID;
        this.ceoName = ceoName.trim();

        // Add CEO as first member
        this.members.add(ceoUUID);
        
        // Initialize financial values
        this.balance = 0.0;
        this.totalMoneyEarned = 0.0;
    }

    // ======== Basic Getters ========

    /**
     * Gets the company name.
     * @return Company name (never null)
     */
    public String getName() { return name; }
    
    /**
     * Gets the CEO UUID.
     * @return CEO UUID (never null)
     */
    public UUID getCeoUUID() { return ceoUUID; }
    
    /**
     * Gets the CEO display name.
     * @return CEO name (never null)
     */
    public String getCeoName() { return ceoName; }
    
    /**
     * Gets an unmodifiable set of all company members.
     * @return Set of member UUIDs (never null)
     */
    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
    
    /**
     * Gets the current number of members.
     * @return Member count (always >= 1)
     */
    public int getMemberCount() { return members.size(); }
    
    /**
     * Gets the current company balance.
     * @return Balance (can be negative)
     */
    public double getBalance() { return balance; }
    
    /**
     * Gets the company level based on total earnings.
     * @return Level (1-7)
     */
    public int getLevel() { return level; }

    // ======== Member Management ========

    /**
     * Adds a new member to the company.
     * @param uuid Player UUID to add
     * @return true if member was added successfully, false otherwise
     */
    public boolean addMember(UUID uuid) {
        if (uuid == null || members.contains(uuid) || members.size() >= maxMembers) {
            return false;
        }
        members.add(uuid);
        return true;
    }

    public boolean addMember(UUID uuid, String playerName) {
        return addMember(uuid);
    }

    public boolean removeMember(UUID uuid) {
        if (uuid == null || uuid.equals(ceoUUID)) {
            return false;
        }
        return members.remove(uuid);
    }

    public boolean hasMember(UUID uuid) {
        return uuid != null && members.contains(uuid);
    }

    /**
     * Checks if a player is the CEO of the company.
     * @param uuid Player UUID to check
     * @return true if player is the CEO, false otherwise
     */
    public boolean isCEO(UUID uuid) {
        return uuid != null && uuid.equals(ceoUUID);
    }

    /**
     * Sets a new CEO for the company.
     * @param newCeoUUID New CEO UUID
     * @param newCeoName New CEO display name
     */
    public void setCeo(UUID newCeoUUID, String newCeoName) {
        if (newCeoUUID == null || newCeoName == null) {
            throw new IllegalArgumentException("CEO UUID and name cannot be null");
        }
        
        // Remove old CEO from members if different
        if (!this.ceoUUID.equals(newCeoUUID)) {
            members.remove(this.ceoUUID);
            members.add(newCeoUUID);
        }
        
        this.ceoUUID = newCeoUUID;
        this.ceoName = newCeoName.trim();
    }

    // ======== Financial Management ========

    /**
     * Deposits money into company account.
     * @param amount Amount to deposit (must be positive)
     */
    public void deposit(double amount) {
        if (amount <= 0) {
            return; // Invalid amount
        }
        this.balance += amount;
        this.totalMoneyEarned += amount;
        updateLevel();
    }

    /**
     * Withdraws money from company account.
     * @param amount Amount to withdraw (must be positive)
     * @return true if withdrawal was successful, false if insufficient funds
     */
    public boolean withdraw(double amount) {
        if (amount <= 0) {
            return false; // Invalid amount
        }
        if (this.balance < amount) {
            return false; // Insufficient funds
        }
        this.balance -= amount;
        return true;
    }

    /**
     * Gets total money earned by the company.
     * @return Total earnings since creation
     */
    public double getTotalMoneyEarned() {
        return totalMoneyEarned;
    }

    /**
     * Sets the balance directly (for loading from storage).
     * @param balance New balance
     */
    public void setBalance(double balance) {
        this.balance = balance;
    }

    /**
     * Sets total money earned directly (for loading from storage).
     * @param totalMoneyEarned Total money earned
     */
    public void setTotalMoneyEarned(double totalMoneyEarned) {
        this.totalMoneyEarned = totalMoneyEarned;
        updateLevel();
    }

    // ======== Level Management ========

    /**
     * Updates company level based on total earnings.
     * This method is called automatically when money is deposited.
     */
    private void updateLevel() {
        int newLevel = 1;
        for (int i = 0; i < LEVEL_THRESHOLDS.length; i++) {
            if (totalMoneyEarned >= LEVEL_THRESHOLDS[i]) {
                newLevel = i + 1;
            }
        }
        this.level = newLevel;
    }

    /**
     * Gets the material icon for the company level.
     * @return Material representing the level
     */
    public Material getLevelIcon() {
        switch (level) {
            case 1: return Material.COBBLESTONE;
            case 2: return Material.IRON_BLOCK;
            case 3: return Material.GOLD_BLOCK;
            case 4: return Material.DIAMOND_BLOCK;
            case 5: return Material.EMERALD_BLOCK;
            case 6: return Material.OBSIDIAN;
            default: return Material.NETHERITE_BLOCK;
        }
    }

    /**
     * Gets the color code for the company level.
     * @return Color code string for chat formatting
     */
    public String getLevelColor() {
        switch (level) {
            case 1: return "§8"; // Gray
            case 2: return "§7"; // Light Gray
            case 3: return "§6"; // Gold
            case 4: return "§b"; // Aqua
            case 5: return "§e"; // Yellow
            case 6: return "§5"; // Purple
            case 7: return "§c"; // Red
            default: return "§f"; // White
        }
    }

    // ======== Headquarters Management ========

    /**
     * Sets company headquarters using Location object.
     * @param loc Location to set as headquarters (can be null to clear)
     */
    public void setHeadquarters(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            clearHeadquarters();
            return;
        }

        World w = loc.getWorld();
        this.headquartersWorld = w.getName();
        this.headquartersX = loc.getX();
        this.headquartersY = loc.getY();
        this.headquartersZ = loc.getZ();
        this.hasHeadquarters = true;
    }

    /**
     * Sets company headquarters using coordinates.
     * @param world World name
     * @param x X coordinate
     * @param y Y coordinate  
     * @param z Z coordinate
     */
    public void setHeadquarters(String world, double x, double y, double z) {
        if (world == null || world.trim().isEmpty()) {
            clearHeadquarters();
            return;
        }

        this.headquartersWorld = world.trim();
        this.headquartersX = x;
        this.headquartersY = y;
        this.headquartersZ = z;
        this.hasHeadquarters = true;
    }

    /**
     * Clears the headquarters location.
     */
    private void clearHeadquarters() {
        this.hasHeadquarters = false;
        this.headquartersWorld = null;
        this.headquartersX = 0;
        this.headquartersY = 0;
        this.headquartersZ = 0;
    }

    /**
     * Gets the headquarters location.
     * @return Location object or null if not set
     */
    public Location getHeadquarters() {
        if (!hasHeadquarters || headquartersWorld == null) {
            return null;
        }
        World w = Bukkit.getWorld(headquartersWorld);
        if (w == null) {
            return null; // World not loaded
        }
        return new Location(w, headquartersX, headquartersY, headquartersZ);
    }

    /**
     * Gets the headquarters location (alias for getHeadquarters).
     * @return Location object or null if not set
     */
    public Location getHeadquartersLocation() {
        return getHeadquarters();
    }

    /**
     * Checks if company has headquarters set.
     * @return true if headquarters is set, false otherwise
     */
    public boolean hasHeadquarters() {
        return hasHeadquarters;
    }

    /**
     * Gets formatted string representation of headquarters location.
     * @return Formatted location string
     */
    public String getHeadquartersString() {
        if (!hasHeadquarters) {
            return "§7Not set";
        }
        return String.format("§7%s, %d, %d, %d", 
            headquartersWorld, 
            (int)headquartersX, 
            (int)headquartersY, 
            (int)headquartersZ);
    }

    // ======== Status Management ========

    /**
     * Checks if company is international.
     * @return true if international, false if national
     */
    public boolean isInternational() {
        return isInternational;
    }

    /**
     * Gets maximum number of members allowed.
     * @return Maximum member count
     */
    public int getMaxMembers() {
        return maxMembers;
    }

    /**
     * Gets formatted status display string.
     * @return Status string with color
     */
    public String getStatusDisplay() {
        return isInternational() ? "§6International" : "§aNational";
    }

    /**
     * Gets color code for company status.
     * @return Color code string
     */
    public String getStatusColor() {
        return isInternational() ? "§6" : "§a";
    }

    // ======== Subsidiary Management ========

    /**
     * Checks if company is a subsidiary.
     * @return true if subsidiary, false otherwise
     */
    public boolean isSubsidiary() {
        return isSubsidiary;
    }

    /**
     * Gets parent company name.
     * @return Parent company name or null if not subsidiary
     */
    public String getParentCompany() {
        return parentCompany;
    }

    /**
     * Sets parent company relationship.
     * @param parent Parent company name (null to clear)
     */
    public void setParentCompany(String parent) {
        this.parentCompany = parent;
        this.isSubsidiary = parent != null;
    }

    /**
     * Gets unmodifiable set of subsidiary companies.
     * @return Set of subsidiary names
     */
    public Set<String> getSubsidiaries() {
        return Collections.unmodifiableSet(subsidiaries);
    }

    public boolean addSubsidiary(String name) {
        if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase(this.name)) {
            return false;
        }
        return subsidiaries.add(name.trim());
    }

    public boolean removeSubsidiary(String name) {
        if (name == null) {
            return false;
        }
        return subsidiaries.remove(name);
    }

    /**
     * Checks if company has a specific subsidiary.
     * @param name Subsidiary name to check
     * @return true if subsidiary exists, false otherwise
     */
    public boolean hasSubsidiary(String name) {
        return name != null && subsidiaries.contains(name);
    }

    // ======== Join Request Management ========

    /**
     * Gets unmodifiable set of pending join requests.
     * @return Set of player UUIDs with pending requests
     */
    public Set<UUID> getPendingJoinRequests() {
        return Collections.unmodifiableSet(pendingJoinRequests);
    }

    public boolean addJoinRequest(UUID uuid) {
        if (uuid != null && !members.contains(uuid)) {
            return pendingJoinRequests.add(uuid);
        }
        return false;
    }

    public boolean removeJoinRequest(UUID uuid) {
        return pendingJoinRequests.remove(uuid);
    }

    public boolean hasJoinRequest(UUID uuid) {
        return uuid != null && pendingJoinRequests.contains(uuid);
    }

    /**
     * Gets player name for display purposes.
     * @param uuid Player UUID
     * @return Player name or UUID string as fallback
     */
    public String getMemberName(UUID uuid) {
        if (uuid == null) {
            return "Unknown";
        }
        if (uuid.equals(ceoUUID)) {
            return ceoName;
        }
        // In a real implementation, this would query a name cache
        return uuid.toString().substring(0, 8) + "...";
    }

    // ======== Company Status Management ========

    /**
     * Sets company to National status.
     * Reduces member limit to National limit.
     */
    public void setNational() {
        this.isInternational = false;
        this.maxMembers = NATIONAL_MEMBER_LIMIT;
    }

    /**
     * Upgrades company to international status.
     * @return true if upgrade was successful, false if already international
     */
    public boolean upgradeToInternational() {
        if (this.isInternational) {
            return false; // Already international
        }
        this.isInternational = true;
        this.maxMembers = INTERNATIONAL_MEMBER_LIMIT;
        return true;
    }

    // ======== Utility Methods ========

    /**
     * Checks if this company is equal to another object.
     * @param o Object to compare
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Company)) return false;
        Company c = (Company) o;
        return name != null && name.equalsIgnoreCase(c.name);
    }

    /**
     * Gets hash code for this company.
     * @return Hash code based on company name
     */
    @Override
    public int hashCode() {
        return name == null ? 0 : name.toLowerCase().hashCode();
    }

    /**
     * Gets string representation of this company.
     * @return Company name with status
     */
    @Override
    public String toString() {
        return String.format("Company{name=%s, level=%d, members=%d, balance=%.2f}", 
            name, level, members.size(), balance);
    }
}