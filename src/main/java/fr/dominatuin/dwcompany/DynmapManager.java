package fr.dominatuin.dwcompany;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

/**
 * Manages Dynmap integration for displaying company headquarters markers.
 * Requires Dynmap plugin to be installed on the server.
 */
public class DynmapManager {

    private final JavaPlugin plugin;
    private final CompanyManager companyManager;

    private DynmapAPI dynmapAPI;
    private MarkerAPI markerAPI;
    private MarkerSet companyMarkerSet;

    private boolean dynmapEnabled;

    // Marker icon name - uses default Dynmap icons
    private static final String MARKER_ICON = "building";
    private static final String MARKER_SET_ID = "dwcompany.hq";
    private static final String MARKER_SET_LABEL = "Company Headquarters";

    /**
     * Creates a new DynmapManager instance.
     *
     * @param plugin         The main plugin instance
     * @param companyManager The company manager for accessing company data
     */
    public DynmapManager(JavaPlugin plugin, CompanyManager companyManager) {
        this.plugin = plugin;
        this.companyManager = companyManager;
        this.dynmapEnabled = false;

        setupDynmap();
    }

    /**
     * Sets up the Dynmap API integration.
     *
     * @return true if Dynmap was successfully initialized
     */
    private boolean setupDynmap() {
        // Check if Dynmap is installed
        if (plugin.getServer().getPluginManager().getPlugin("dynmap") == null) {
            plugin.getLogger().info("Dynmap not found. Map markers will be disabled.");
            return false;
        }

        // Get Dynmap API
        dynmapAPI = (DynmapAPI) plugin.getServer().getPluginManager().getPlugin("dynmap");
        if (dynmapAPI == null) {
            plugin.getLogger().warning("Failed to get Dynmap API.");
            return false;
        }

        // Get Marker API
        markerAPI = dynmapAPI.getMarkerAPI();
        if (markerAPI == null) {
            plugin.getLogger().warning("Failed to get Dynmap Marker API.");
            return false;
        }

        // Create or get marker set
        companyMarkerSet = markerAPI.getMarkerSet(MARKER_SET_ID);
        if (companyMarkerSet == null) {
            companyMarkerSet = markerAPI.createMarkerSet(
                    MARKER_SET_ID,
                    MARKER_SET_LABEL,
                    null, // Use all icons
                    false // Not persistent, we manage it
            );
        }

        if (companyMarkerSet == null) {
            plugin.getLogger().warning("Failed to create Dynmap marker set.");
            return false;
        }

        // Set default marker icon if not exists
        if (markerAPI.getMarkerIcon(MARKER_ICON) == null) {
            // Use default icon if custom doesn't exist
            plugin.getLogger().info("Using default marker icon for companies.");
        }

        dynmapEnabled = true;
        plugin.getLogger().info("Successfully hooked into Dynmap!");

        // Load existing company markers
        loadAllCompanyMarkers();

        return true;
    }

    /**
     * Checks if Dynmap integration is enabled.
     *
     * @return true if Dynmap is available
     */
    public boolean isDynmapEnabled() {
        return dynmapEnabled && dynmapAPI != null && markerAPI != null;
    }

    /**
     * Adds or updates a company headquarters marker on Dynmap.
     *
     * @param company The company to add marker for
     * @return true if marker was added/updated
     */
    public boolean addOrUpdateCompanyMarker(Company company) {
        if (!isDynmapEnabled()) {
            return false;
        }

        if (!company.hasHeadquarters()) {
            // Remove marker if HQ was removed
            removeCompanyMarker(company.getName());
            return false;
        }

        Location hq = company.getHeadquartersLocation();
        if (hq == null) {
            return false;
        }

        String markerId = getMarkerId(company.getName());
        String label = buildMarkerLabel(company);
        String worldName = hq.getWorld().getName();

        // Check if marker already exists
        Marker existingMarker = companyMarkerSet.findMarker(markerId);

        if (existingMarker != null) {
            // Update existing marker
            existingMarker.setLocation(worldName, hq.getX(), hq.getY(), hq.getZ());
            existingMarker.setLabel(label);
        } else {
            // Create new marker
            Marker marker = companyMarkerSet.createMarker(
                    markerId,
                    label,
                    worldName,
                    hq.getX(),
                    hq.getY(),
                    hq.getZ(),
                    markerAPI.getMarkerIcon(MARKER_ICON),
                    false // Not persistent
            );

            if (marker == null) {
                plugin.getLogger().warning("Failed to create marker for company: " + company.getName());
                return false;
            }
        }

        plugin.getLogger().fine("Updated Dynmap marker for: " + company.getName());
        return true;
    }

    /**
     * Removes a company headquarters marker from Dynmap.
     *
     * @param companyName The name of the company
     * @return true if marker was removed or didn't exist
     */
    public boolean removeCompanyMarker(String companyName) {
        if (!isDynmapEnabled()) {
            return false;
        }

        String markerId = getMarkerId(companyName);
        Marker marker = companyMarkerSet.findMarker(markerId);

        if (marker != null) {
            marker.deleteMarker();
            plugin.getLogger().fine("Removed Dynmap marker for: " + companyName);
        }

        return true;
    }

    /**
     * Loads markers for all companies that have headquarters set.
     */
    public void loadAllCompanyMarkers() {
        if (!isDynmapEnabled()) {
            return;
        }

        int count = 0;
        for (Company company : companyManager.getAllCompanies()) {
            if (company.hasHeadquarters()) {
                if (addOrUpdateCompanyMarker(company)) {
                    count++;
                }
            }
        }

        plugin.getLogger().info("Loaded " + count + " company markers on Dynmap.");
    }

    /**
     * Clears all company markers from Dynmap.
     */
    public void clearAllMarkers() {
        if (!isDynmapEnabled() || companyMarkerSet == null) {
            return;
        }

        for (Marker marker : companyMarkerSet.getMarkers()) {
            marker.deleteMarker();
        }

        plugin.getLogger().info("Cleared all company markers from Dynmap.");
    }

    /**
     * Reloads all markers (clears and re-adds).
     */
    public void reloadMarkers() {
        clearAllMarkers();
        loadAllCompanyMarkers();
    }

    /**
     * Builds the HTML label for a company marker.
     *
     * @param company The company
     * @return HTML formatted label
     */
    private String buildMarkerLabel(Company company) {
        StringBuilder sb = new StringBuilder();

        // Company name with status
        sb.append("<div style='font-weight:bold;font-size:14px;'>");
        sb.append(company.getName());
        sb.append("</div>");

        // Status badge
        String statusColor = company.isInternational() ? "#FFA500" : "#32CD32"; // Orange or Green
        sb.append("<div style='color:").append(statusColor).append(";font-size:11px;'>");
        sb.append(company.getStatusDisplay());
        sb.append("</div>");

        // CEO
        sb.append("<div style='font-size:10px;margin-top:4px;'>");
        sb.append("CEO: ").append(company.getCeoName());
        sb.append("</div>");

        // Level
        sb.append("<div style='font-size:10px;'>");
        sb.append("Level: ").append(company.getLevel());
        sb.append("</div>");

        // Members
        sb.append("<div style='font-size:10px;'>");
        sb.append("Members: ").append(company.getMemberCount()).append("/").append(company.getMaxMembers());
        sb.append("</div>");

        return sb.toString();
    }

    /**
     * Generates a safe marker ID from company name.
     *
     * @param companyName The company name
     * @return Safe marker ID
     */
    private String getMarkerId(String companyName) {
        // Replace spaces and special characters to make it safe for marker ID
        return "company_" + companyName.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    /**
     * Gets the Dynmap API instance.
     *
     * @return The Dynmap API, or null if not available
     */
    public DynmapAPI getDynmapAPI() {
        return dynmapAPI;
    }

    /**
     * Gets the Marker API instance.
     *
     * @return The Marker API, or null if not available
     */
    public MarkerAPI getMarkerAPI() {
        return markerAPI;
    }
}
