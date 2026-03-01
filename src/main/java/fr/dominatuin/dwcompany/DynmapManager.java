package fr.dominatuin.dwcompany;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

public class DynmapManager {

    private final JavaPlugin plugin;
    private final CompanyManager companyManager;
    private DynmapAPI dynmapAPI;
    private MarkerAPI markerAPI;
    private MarkerSet companyMarkerSet;
    private boolean dynmapEnabled;

    private static final String MARKER_ICON = "building";
    private static final String MARKER_SET_ID = "dwcompany.hq";
    private static final String MARKER_SET_LABEL = "Company Headquarters";

    public DynmapManager(JavaPlugin plugin, CompanyManager companyManager) {
        this.plugin = plugin;
        this.companyManager = companyManager;
        this.dynmapEnabled = false;
        setupDynmap();
    }

    private boolean setupDynmap() {
        Plugin dynmapPlugin = plugin.getServer().getPluginManager().getPlugin("dynmap");
        if (dynmapPlugin == null) {
            plugin.getLogger().info("Dynmap not found. Map markers disabled.");
            return false;
        }

        dynmapAPI = (DynmapAPI) dynmapPlugin;
        if (dynmapAPI == null) {
            plugin.getLogger().warning("Failed to get Dynmap API.");
            return false;
        }

        markerAPI = dynmapAPI.getMarkerAPI();
        if (markerAPI == null) {
            plugin.getLogger().warning("Failed to get Dynmap Marker API.");
            return false;
        }

        companyMarkerSet = markerAPI.getMarkerSet(MARKER_SET_ID);
        if (companyMarkerSet == null) {
            companyMarkerSet = markerAPI.createMarkerSet(MARKER_SET_ID, MARKER_SET_LABEL, null, false);
        }

        if (companyMarkerSet == null) {
            plugin.getLogger().warning("Failed to create Dynmap marker set.");
            return false;
        }

        dynmapEnabled = true;
        plugin.getLogger().info("Successfully hooked into Dynmap!");
        loadAllCompanyMarkers();
        return true;
    }

    public boolean isDynmapEnabled() {
        return dynmapEnabled && dynmapAPI != null && markerAPI != null;
    }

    public boolean addOrUpdateCompanyMarker(Company company) {
        if (!isDynmapEnabled() || !company.hasHeadquarters()) {
            return false;
        }

        Location hq = company.getHeadquartersLocation();
        if (hq == null) return false;

        String markerId = getMarkerId(company.getName());
        String label = buildMarkerLabel(company);
        String worldName = hq.getWorld().getName();

        Marker existingMarker = companyMarkerSet.findMarker(markerId);
        if (existingMarker != null) {
            existingMarker.setLocation(worldName, hq.getX(), hq.getY(), hq.getZ());
            existingMarker.setLabel(label);
        } else {
            companyMarkerSet.createMarker(markerId, label, worldName, hq.getX(), hq.getY(), hq.getZ(), 
                markerAPI.getMarkerIcon(MARKER_ICON), false);
        }
        return true;
    }

    public boolean removeCompanyMarker(String companyName) {
        if (!isDynmapEnabled()) return false;

        Marker marker = companyMarkerSet.findMarker(getMarkerId(companyName));
        if (marker != null) {
            marker.deleteMarker();
        }
        return true;
    }

    public void loadAllCompanyMarkers() {
        if (!isDynmapEnabled()) return;

        int count = 0;
        for (Company company : companyManager.getAllCompanies()) {
            if (company.hasHeadquarters() && addOrUpdateCompanyMarker(company)) {
                count++;
            }
        }
        plugin.getLogger().info(String.format("Loaded %d company markers on Dynmap.", count));
    }

    private String buildMarkerLabel(Company company) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-weight:bold;font-size:14px;'>").append(company.getName()).append("</div>");
        
        String statusColor = company.isInternational() ? "#FFA500" : "#32CD32";
        sb.append("<div style='color:").append(statusColor).append(";font-size:11px;'>")
          .append(company.getStatusDisplay()).append("</div>");
        
        sb.append("<div style='font-size:10px;margin-top:4px;'>CEO: ").append(company.getCeoName()).append("</div>");
        sb.append("<div style='font-size:10px;'>Level: ").append(company.getLevel()).append("</div>");
        sb.append("<div style='font-size:10px;'>Members: ").append(company.getMemberCount())
          .append("/").append(company.getMaxMembers()).append("</div>");
        
        return sb.toString();
    }

    private String getMarkerId(String companyName) {
        return "company_" + companyName.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
}
