package fr.dominatuin.dwcompany;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import fr.dominatuin.dwcompany.Company;

import java.util.ArrayList;
import java.util.List;

/**
 * Main menu GUI that opens when players type /entreprise.
 * Provides clickable buttons for all main commands.
 */
public class MainMenuGUI implements Listener {

    private final DWcompany plugin;
    private final CompanyManager companyManager;
    private final CompanyGUI companyGUI;
    private final ConfigManager configManager;

    /**
     * Creates a new MainMenuGUI instance.
     *
     * @param plugin         The main plugin instance
     * @param companyManager The company manager
     * @param companyGUI     The company GUI manager
     * @param configManager  The config manager
     */
    public MainMenuGUI(DWcompany plugin, CompanyManager companyManager,
                        CompanyGUI companyGUI, ConfigManager configManager) {
        this.plugin = plugin;
        this.companyManager = companyManager;
        this.companyGUI = companyGUI;
        this.configManager = configManager;

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Opens the main menu GUI for a player.
     *
     * @param player The player
     */
    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6§lDWcompany - Main Menu");

        Company playerCompany = companyManager.getPlayerCompany(player.getUniqueId());
        boolean isInCompany = playerCompany != null;
        boolean isCEO = false;

        if (isInCompany) {
            isCEO = playerCompany.isCEO(player.getUniqueId());
        }

        // Row 1 - Company Actions
        if (!isInCompany) {
            // Not in company - show create and join
            gui.setItem(10, createMenuItem(Material.GRASS_BLOCK, "§aCreate Company",
                    "§7Click to create a new company", "§eCost: §f100k-500k"));
            gui.setItem(12, createMenuItem(Material.OAK_DOOR, "§eJoin Company",
                    "§7Click to browse and join a company"));
        } else {
            // In company - show leave and company info
            gui.setItem(10, createMenuItem(Material.BOOK, "§6My Company",
                    "§7View your company details", "§e" + playerCompany));
            gui.setItem(12, createMenuItem(Material.BARRIER, "§cLeave Company",
                    "§7Click to leave your company"));
        }

        // Row 2 - Company Features
        gui.setItem(14, createMenuItem(Material.GOLD_INGOT, "§eCompany Bank",
                "§7Access your company bank", "§7Deposit and withdraw money"));

        gui.setItem(16, createMenuItem(Material.BOOK, "§bCompany List",
                "§7View all companies", "§7Sorted by level"));

        // Row 3 - CEO/Advanced Features
        if (isCEO) {
            gui.setItem(18, createMenuItem(Material.CHEST, "§aSubsidiaries",
                    "§7Manage company subsidiaries"));

            gui.setItem(20, createMenuItem(Material.COMPASS, "§5Headquarters",
                    "§7Set company headquarters", "§7Creates Dynmap marker"));

            gui.setItem(22, createMenuItem(Material.GLOBE_BANNER_PATTERN, "§6International",
                    "§7Upgrade to International status", "§eCost: §f20k"));

            gui.setItem(24, createMenuItem(Material.ANVIL, "§cManagement",
                    "§7Manage company members", "§7Transfer, kick, etc."));
        }

        // Close button
        gui.setItem(26, createMenuItem(Material.BARRIER, "§cClose", "§7Click to close menu"));

        playOpenSound(player);
        player.openInventory(gui);
    }

    /**
     * Creates a menu item with name and lore.
     *
     * @param material The material
     * @param name     The display name
     * @param lore     The lore lines
     * @return The created ItemStack
     */
    private ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);

        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(line);
        }
        loreList.add("");
        loreList.add("§eClick to use");

        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (!title.equals("§6§lDWcompany - Main Menu")) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String name = meta.getDisplayName();
        playClickSound(player);

        switch (name) {
            case "§aCreate Company":
                player.closeInventory();
                player.sendMessage("§eTo create a company, type: §f/entreprise create <name>");
                player.sendMessage("§7Example: §f/entreprise create MyCompany");
                break;

            case "§eJoin Company":
                player.closeInventory();
                companyGUI.openCompanyList(player, 0);
                break;

            case "§6My Company":
                player.closeInventory();
                Company playerCompany = companyManager.getPlayerCompany(player.getUniqueId());
                if (playerCompany != null) {
                    companyGUI.openCompanyDetails(player, playerCompany.getName());
                }
                break;

            case "§cLeave Company":
                player.closeInventory();
                player.performCommand("entreprise leave");
                break;

            case "§eCompany Bank":
                player.closeInventory();
                player.performCommand("entreprise bank");
                break;

            case "§bCompany List":
                player.closeInventory();
                companyGUI.openCompanyList(player, 0);
                break;

            case "§aSubsidiaries":
                player.closeInventory();
                player.sendMessage("§eSubsidiary commands:");
                player.sendMessage("§f/entreprise filiale create <name> §7- Create subsidiary");
                player.sendMessage("§f/entreprise filiale add <company> §7- Add existing company");
                player.sendMessage("§f/entreprise filiale list §7- List subsidiaries");
                break;

            case "§5Headquarters":
                player.closeInventory();
                player.performCommand("entreprise batiment");
                break;

            case "§6International":
                player.closeInventory();
                player.performCommand("entreprise international");
                break;

            case "§cManagement":
                player.closeInventory();
                player.performCommand("entreprise manage");
                break;

            case "§cClose":
                player.closeInventory();
                break;
        }
    }

    private void playOpenSound(Player player) {
        if (configManager.areSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        }
    }

    private void playClickSound(Player player) {
        if (configManager.areSoundsEnabled()) {
            try {
                Sound sound = Sound.valueOf(configManager.getButtonClickSound());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }
    }
}
