package fr.dominatuin.dwcompany;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;

/**
 * Represents a Company in the plugin.
 * Stores all company data including members, CEO, bank balance, subsidiaries, and level.
 */
public class Company {

    // Company identification
    private final String name;
    private UUID ceoUUID;
    private String ceoName;

    // Members
    private final Set<UUID> members;
    private final Map<UUID, String> memberNames;
    private int maxMembers; // 5 for National, 10 for International

    // Economy
    private double balance;
    private double totalMoneyEarned;

    // Subsidiaries
    private final Set<String> subsidiaries;
    private String parentCompany;

    // Level tracking
    private int level;

    // Pending join requests
    private final Set<UUID> pendingJoinRequests;

    // Pending subsidiary requests
    private final Map<String, UUID> pendingSubsidiaryRequests;

    // Headquarters location
    private String headquartersWorld;
    private double headquartersX;
    private double headquartersY;
    private double headquartersZ;
    private boolean hasHeadquarters;

    // National / International status
    private boolean isInternational;

    // Constants
    public static final int MAX_COMPANIES_PER_PLAYER = 2;
    public static final double FIRST_COMPANY_COST = 100000.0;
    public static final double SECOND_COMPANY_COST = 500000.0;
    public static final double INTERNATIONAL_UPGRADE_COST = 20000.0;
    public static final int NATIONAL_MEMBER_LIMIT = 5;
    public static final int INTERNATIONAL_MEMBER_LIMIT = 10;

    /**
     * Creates a new Company with the given name and CEO.
     *
     * @param name   The unique name of the company
     * @param ceoUUID The UUID of the CEO
     * @param ceoName The name of the CEO
     */
    public Company(String name, UUID ceoUUID, String ceoName) {
        this.name = name;
        this.ceoUUID = ceoUUID;
        this.ceoName = ceoName;
        this.members = new HashSet<>();
        this.memberNames = new HashMap<>();
        this.maxMembers = NATIONAL_MEMBER_LIMIT; // Default: National
        this.balance = 0.0;
        this.totalMoneyEarned = 0.0;
        this.subsidiaries = new HashSet<>();
        this.parentCompany = null;
        this.level = 1;
        this.pendingJoinRequests = new HashSet<>();
        this.pendingSubsidiaryRequests = new HashMap<>();
        this.hasHeadquarters = false;
        this.headquartersWorld = null;
        this.headquartersX = 0;
        this.headquartersY = 0;
        this.headquartersZ = 0;
        this.isInternational = false; // Default: National

        // CEO is automatically a member
        this.members.add(ceoUUID);
        this.memberNames.put(ceoUUID, ceoName);
    }

    // ==================== Getters ====================

    public String getName() {
        return name;
    }

    public UUID getCeoUUID() {
        return ceoUUID;
    }

    public String getCeoName() {
        return ceoName;
    }

    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }

    public int getMemberCount() {
        return members.size();
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public double getBalance() {
        return balance;
    }

    public double getTotalMoneyEarned() {
        return totalMoneyEarned;
    }

    public Set<String> getSubsidiaries() {
        return new HashSet<>(subsidiaries);
    }

    public String getParentCompany() {
        return parentCompany;
    }

    public int getLevel() {
        return level;
    }

    public boolean isInternational() {
        return isInternational;
    }

    public String getStatusDisplay() {
        return isInternational ? "International" : "National";
    }

    public String getStatusColor() {
        return isInternational ? "§6" : "§a"; // Gold for International, Green for National
    }

    public boolean hasHeadquarters() {
        return hasHeadquarters;
    }

    public Location getHeadquartersLocation() {
        if (!hasHeadquarters || headquartersWorld == null) {
            return null;
        }
        World world = Bukkit.getWorld(headquartersWorld);
        if (world == null) {
            return null;
        }
        return new Location(world, headquartersX, headquartersY, headquartersZ);
    }

    public String getHeadquartersString() {
        if (!hasHeadquarters) {
            return "Not set";
        }
        return String.format("%s (%.0f, %.0f, %.0f)", headquartersWorld, headquartersX, headquartersY, headquartersZ);
    }

    // ==================== Setters ====================

    public void setCeo(UUID newCeoUUID, String newCeoName) {
        this.ceoUUID = newCeoUUID;
        this.ceoName = newCeoName;
        // Ensure CEO is a member
        if (!members.contains(newCeoUUID)) {
            members.add(newCeoUUID);
            memberNames.put(newCeoUUID, newCeoName);
        }
    }

    public void setParentCompany(String parentCompany) {
        this.parentCompany = parentCompany;
    }

    public void setHeadquarters(Location location) {
        if (location == null) {
            this.hasHeadquarters = false;
            return;
        }
        this.headquartersWorld = location.getWorld().getName();
        this.headquartersX = location.getX();
        this.headquartersY = location.getY();
        this.headquartersZ = location.getZ();
        this.hasHeadquarters = true;
    }

    public void setHeadquarters(String world, double x, double y, double z) {
        this.headquartersWorld = world;
        this.headquartersX = x;
        this.headquartersY = y;
        this.headquartersZ = z;
        this.hasHeadquarters = true;
    }

    /**
     * Upgrades the company to International status.
     *
     * @return true if upgrade was successful
     */
    public boolean upgradeToInternational() {
        if (isInternational) {
            return false; // Already international
        }
        this.isInternational = true;
        this.maxMembers = INTERNATIONAL_MEMBER_LIMIT;
        return true;
    }

    /**
     * Downgrades to National (mainly for admin purposes).
     */
    public void setNational() {
        this.isInternational = false;
        this.maxMembers = NATIONAL_MEMBER_LIMIT;
    }

    // ==================== Member Management ====================

    /**
     * Checks if the company can accept more members.
     *
     * @return true if there's space for more members
     */
    public boolean canAddMember() {
        return members.size() < maxMembers;
    }

    /**
     * Gets the remaining member slots.
     *
     * @return number of available slots
     */
    public int getRemainingSlots() {
        return maxMembers - members.size();
    }

    public void addMember(UUID playerUUID, String playerName) {
        members.add(playerUUID);
        memberNames.put(playerUUID, playerName);
    }

    public void removeMember(UUID playerUUID) {
        members.remove(playerUUID);
        memberNames.remove(playerUUID);
    }

    public boolean hasMember(UUID playerUUID) {
        return members.contains(playerUUID);
    }

    public String getMemberName(UUID playerUUID) {
        return memberNames.getOrDefault(playerUUID, "Unknown");
    }

    public boolean isCEO(UUID playerUUID) {
        return ceoUUID.equals(playerUUID);
    }

    // ==================== Economy Management ====================

    public void deposit(double amount) {
        this.balance += amount;
        this.totalMoneyEarned += amount;
        updateLevel();
    }

    public boolean withdraw(double amount) {
        if (balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    // ==================== Level System ====================

    /**
     * Updates the company level based on total money earned.
     * Level tiers:
     * - Level 1: < 10,000$
     * - Level 2: 10,000$ - 50,000$
     * - Level 3: 50,000$ - 100,000$
     * - Level 4: 100,000$ - 250,000$
     * - Level 5: 250,000$ - 500,000$
     * - Level 6: 500,000$ - 1,000,000$
     * - Level 7: 1,000,000$+ (Netherite)
     */
    private void updateLevel() {
        if (totalMoneyEarned < 10000) {
            level = 1;
        } else if (totalMoneyEarned < 50000) {
            level = 2;
        } else if (totalMoneyEarned < 100000) {
            level = 3;
        } else if (totalMoneyEarned < 250000) {
            level = 4;
        } else if (totalMoneyEarned < 500000) {
            level = 5;
        } else if (totalMoneyEarned < 1000000) {
            level = 6;
        } else {
            level = 7;
        }
    }

    /**
     * Gets the material icon for this company's level.
     *
     * @return Material representing the company level
     */
    public Material getLevelIcon() {
        switch (level) {
            case 1:
                return Material.COBBLESTONE;
            case 2:
                return Material.IRON_BLOCK;
            case 3:
                return Material.GOLD_BLOCK;
            case 4:
                return Material.DIAMOND_BLOCK;
            case 5:
                return Material.EMERALD_BLOCK;
            case 6:
                return Material.OBSIDIAN;
            case 7:
                return Material.NETHERITE_BLOCK;
            default:
                return Material.COBBLESTONE;
        }
    }

    public String getLevelColor() {
        switch (level) {
            case 1:
                return "§7"; // Gray
            case 2:
                return "§f"; // White
            case 3:
                return "§e"; // Yellow
            case 4:
                return "§b"; // Aqua
            case 5:
                return "§a"; // Green
            case 6:
                return "§5"; // Dark Purple
            case 7:
                return "§4"; // Dark Red
            default:
                return "§7";
        }
    }

    // ==================== Subsidiary Management ====================

    public void addSubsidiary(String companyName) {
        subsidiaries.add(companyName);
    }

    public void removeSubsidiary(String companyName) {
        subsidiaries.remove(companyName);
    }

    public boolean hasSubsidiary(String companyName) {
        return subsidiaries.contains(companyName);
    }

    public boolean isSubsidiary() {
        return parentCompany != null;
    }

    // ==================== Join Request Management ====================

    public void addJoinRequest(UUID playerUUID) {
        pendingJoinRequests.add(playerUUID);
    }

    public void removeJoinRequest(UUID playerUUID) {
        pendingJoinRequests.remove(playerUUID);
    }

    public boolean hasJoinRequest(UUID playerUUID) {
        return pendingJoinRequests.contains(playerUUID);
    }

    public Set<UUID> getPendingJoinRequests() {
        return new HashSet<>(pendingJoinRequests);
    }

    // ==================== Subsidiary Request Management ====================

    public void addSubsidiaryRequest(String companyName, UUID requesterUUID) {
        pendingSubsidiaryRequests.put(companyName, requesterUUID);
    }

    public void removeSubsidiaryRequest(String companyName) {
        pendingSubsidiaryRequests.remove(companyName);
    }

    public boolean hasSubsidiaryRequest(String companyName) {
        return pendingSubsidiaryRequests.containsKey(companyName);
    }

    public UUID getSubsidiaryRequester(String companyName) {
        return pendingSubsidiaryRequests.get(companyName);
    }

    public Map<String, UUID> getPendingSubsidiaryRequests() {
        return new HashMap<>(pendingSubsidiaryRequests);
    }

    // ==================== Utility ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Company company = (Company) o;
        return Objects.equals(name, company.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Company{" +
                "name='" + name + '\'' +
                ", ceoName='" + ceoName + '\'' +
                ", level=" + level +
                ", status=" + getStatusDisplay() +
                ", members=" + members.size() + "/" + maxMembers +
                ", balance=" + balance +
                '}';
    }
}
