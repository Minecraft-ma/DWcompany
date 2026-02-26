package fr.dominatuin.dwcompany;

import fr.dominatuin.dwcompany.storage.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Handles all /entreprise commands.
 * Implements CommandExecutor for command handling and TabCompleter for tab completion.
 */
public class CompanyCommandExecutor implements CommandExecutor, TabCompleter {

    private final DWcompany plugin;
    private final CompanyManager companyManager;
    private final EconomyManager economyManager;
    private final CompanyGUI companyGUI;
    private final MainMenuGUI mainMenuGUI;
    private final DynmapManager dynmapManager;

    // Message constants
    private static final String MSG_NOT_IN_COMPANY = "§cYou are not in a company.";
    private static final String MSG_COMPANY_NOT_FOUND = "§cCompany not found.";
    private static final String MSG_ONLY_CEO = "§cOnly the CEO can do this.";
    private static final String MSG_NO_PERMISSION = "§cYou don't have permission.";
    private static final String CMD_ADMIN = "admin";
    private static final String CMD_CREATE = "create";
    private static final String CMD_DELETE = "delete";
    private static final String CMD_FILIALE = "filiale";
    private static final String CMD_TRANSFER = "transfer";
    private static final String CMD_CONFIRM = "confirm";

    // Cooldown map for join requests
    private final Map<UUID, Long> joinRequestCooldowns;
    private static final long JOIN_COOLDOWN_MS = 60000; // 60 seconds

    /**
     * Creates a new command executor.
     *
     * @param plugin         The main plugin instance
     * @param companyManager The company manager
     * @param economyManager The economy manager
     * @param companyGUI     The GUI manager
     * @param mainMenuGUI    The main menu GUI
     * @param dynmapManager  The Dynmap manager
     */
    public CompanyCommandExecutor(DWcompany plugin, CompanyManager companyManager,
                                   EconomyManager economyManager, CompanyGUI companyGUI,
                                   MainMenuGUI mainMenuGUI, DynmapManager dynmapManager) {
        this.plugin = plugin;
        this.companyManager = companyManager;
        this.economyManager = economyManager;
        this.companyGUI = companyGUI;
        this.mainMenuGUI = mainMenuGUI;
        this.dynmapManager = dynmapManager;
        this.joinRequestCooldowns = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length > 0 && CMD_ADMIN.equalsIgnoreCase(args[0])) {
                handleAdminCommand(sender, args);
                return true;
            }
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            if (!checkPermission(player, "dwcompany.use")) return true;
            mainMenuGUI.openMainMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create": return handleCreate(player, args);
            case "join": return handleJoin(player, args);
            case "leave": return handleLeave(player, args);
            case "bank": return handleBank(player, args);
            case "filiale": return handleFiliale(player, args);
            case "batiment": return handleBatiment(player);
            case "national": return handleStatus(player, args);
            case "international": return handleStatus(player, args);
            case "list": return handleList(player);
            case "info": return handleInfo(player, args);
            case "delete": return handleDelete(player, args);
            case "transfer": return handleTransfer(player, args);
            case "members": return handleMembers(player);
            case "accept": return handleAccept(player, args);
            case "deny": return handleDeny(player, args);
            case "kick": return handleKick(player, args);
            case "manage": return handleManage(player);
            case "requests": return handleRequests(player);
            case "reload": return handleReload(player);
            case "help": return handleHelp(player);
            default:
                player.sendMessage("§cUnknown command. Use /entreprise help for help.");
                return true;
        }
    }

    // ==================== Command Handlers ====================

    private boolean handleCreate(Player player, String[] args) {
        if (!checkPermission(player, "dwcompany.create")) return true;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /entreprise create <name>");
            return true;
        }

        String name = args[1];

        // Validate name
        if (!isValidCompanyName(name)) {
            player.sendMessage("§cInvalid company name. Use 3-20 alphanumeric characters.");
            return true;
        }

        // Check if already in company
        if (companyManager.isInCompany(player.getUniqueId())) {
            player.sendMessage("§cYou are already in a company. Leave it first with /entreprise leave");
            return true;
        }

        // Create company
        if (companyManager.createCompany(name, player.getUniqueId(), player.getName())) {
            player.sendMessage("§aCompany §l" + name + " §ahas been created! You are now the CEO.");
            player.sendMessage("§eUse /entreprise manage to open the management menu.");
        } else {
            player.sendMessage("§cA company with this name already exists or the name is invalid.");
        }

        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        if (!checkPermission(player, "dwcompany.join")) return true;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /entreprise join <company>");
            return true;
        }

        // Check if already in company
        if (companyManager.isInCompany(player.getUniqueId())) {
            player.sendMessage("§cYou are already in a company.");
            return true;
        }

        String companyName = args[1];
        Company company = companyManager.getCompany(companyName);

        if (company == null) {
            player.sendMessage("§cCompany not found.");
            return true;
        }

        // Check cooldown
        if (isOnCooldown(player.getUniqueId())) {
            long remaining = getCooldownRemaining(player.getUniqueId()) / 1000;
            player.sendMessage("§cPlease wait " + remaining + " seconds before sending another request.");
            return true;
        }

        // Add join request
        company.addJoinRequest(player.getUniqueId());
        setCooldown(player.getUniqueId());

        player.sendMessage("§aJoin request sent to §l" + company.getName() + "§a.");

        // Notify CEO
        Player ceo = Bukkit.getPlayer(company.getCeoUUID());
        if (ceo != null && ceo.isOnline()) {
            ceo.sendMessage("§e" + player.getName() + " §ewants to join your company.");
            ceo.sendMessage("§eUse /entreprise accept " + player.getName() + " to accept or /entreprise deny " + player.getName() + " to deny.");
        }

        return true;
    }

    private boolean handleLeave(Player player, String[] args) {
        if (!checkPermission(player, "dwcompany.leave")) return true;

        String companyName = companyManager.getPlayerCompany(player.getUniqueId());
        if (companyName == null) {
            player.sendMessage("§cYou are not in a company.");
            return true;
        }

        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage("§cCompany not found.");
            return true;
        }

        // Check if CEO
        if (company.isCEO(player.getUniqueId())) {
            if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
                player.sendMessage("§c§lWARNING: You are the CEO of this company!");
                player.sendMessage("§cTo leave, you must either:");
                player.sendMessage("§71. Transfer ownership: §e/entreprise transfer <player>");
                player.sendMessage("§72. Delete the company: §e/entreprise delete");
                player.sendMessage("§cOr type §e/entreprise leave confirm §cto leave (company will be deleted if no other members).");

                if (company.getMemberCount() > 1) {
                    player.sendMessage("§c§lNote: The company has " + company.getMemberCount() + " members.");
                    player.sendMessage("§cYou must transfer ownership first!");
                } else {
                    player.sendMessage("§cYou are the only member. Leaving will §ldelete§c the company.");
                }
                return true;
            }

            // Confirm delete if only member
            if (company.getMemberCount() == 1) {
                companyManager.deleteCompany(companyName);
                player.sendMessage("§cYou left and the company §l" + companyName + " §chas been deleted.");
                return true;
            } else {
                player.sendMessage("§cYou cannot leave as CEO with other members. Transfer ownership first!");
                return true;
            }
        }

        // Regular member leave
        if (companyManager.removeMemberFromCompany(player.getUniqueId())) {
            player.sendMessage("§aYou have left the company §l" + companyName + "§a.");

            // Notify CEO
            Player ceo = Bukkit.getPlayer(company.getCeoUUID());
            if (ceo != null && ceo.isOnline()) {
                ceo.sendMessage("§e" + player.getName() + " §ehas left your company.");
            }
        } else {
            player.sendMessage("§cFailed to leave the company.");
        }

        return true;
    }

    private boolean handleBank(Player player, String[] args) {
        // No args - open GUI
        if (args.length == 1) {
            companyGUI.openCompanyBank(player);
            return true;
        }

        String action = args[1].toLowerCase();
        String companyName = companyManager.getPlayerCompany(player.getUniqueId());

        if (companyName == null) {
            player.sendMessage("§cYou are not in a company.");
            return true;
        }

        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage("§cCompany not found.");
            return true;
        }

        switch (action) {
            case "deposit":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /entreprise bank deposit <amount>");
                    return true;
                }

                double depositAmount;
                try {
                    depositAmount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid amount.");
                    return true;
                }

                if (!checkPermission(player, "dwcompany.bank.deposit")) return true;

                EconomyManager.TransactionResult result = economyManager.depositToCompany(player, companyName, depositAmount);
                player.sendMessage(result.getMessage());

                if (result.isSuccess()) {
                    player.sendMessage("§aDeposited §l" + economyManager.formatMoney(depositAmount) + " §ainto the company bank.");
                    player.sendMessage("§7New balance: §a" + economyManager.formatMoney(companyManager.getCompany(companyName).getBalance()));
                }
                break;

            case "withdraw":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /entreprise bank withdraw <amount>");
                    return true;
                }

                double withdrawAmount;
                try {
                    withdrawAmount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid amount.");
                    return true;
                }

                // Check permission
                if (!player.hasPermission("dwcompany.bank.withdraw") && !company.isCEO(player.getUniqueId())) {
                    player.sendMessage("§cOnly the CEO or members with permission can withdraw.");
                    return true;
                }

                EconomyManager.TransactionResult withdrawResult = economyManager.withdrawFromCompany(player, companyName, withdrawAmount);
                player.sendMessage(withdrawResult.getMessage());

                if (withdrawResult.isSuccess()) {
                    player.sendMessage("§aWithdrawn §l" + economyManager.formatMoney(withdrawAmount) + " §afrom the company bank.");
                    player.sendMessage("§7New balance: §a" + economyManager.formatMoney(companyManager.getCompany(companyName).getBalance()));
                }
                break;

            case "balance":
                player.sendMessage("§6Company Bank Balance");
                player.sendMessage("§7Company: §f" + companyName);
                player.sendMessage("§7Balance: §a" + economyManager.formatMoney(company.getBalance()));
                player.sendMessage("§7Total Earned: §a" + economyManager.formatMoney(company.getTotalMoneyEarned()));
                break;

            default:
                player.sendMessage("§cUnknown bank command. Use: deposit, withdraw, balance");
        }

        return true;
    }

    private boolean handleFiliale(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /entreprise filiale <create|add>");
            return true;
        }

        String subAction = args[1].toLowerCase();
        String companyName = companyManager.getPlayerCompany(player.getUniqueId());

        if (companyName == null) {
            player.sendMessage("§cYou are not in a company.");
            return true;
        }

        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage("§cCompany not found.");
            return true;
        }

        // Check if CEO
        if (!company.isCEO(player.getUniqueId()) && !player.hasPermission("dwcompany.filiale.create")) {
            player.sendMessage("§cOnly the CEO can manage subsidiaries.");
            return true;
        }

        switch (subAction) {
            case "create":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /entreprise filiale create <name>");
                    return true;
                }

                String newFilialeName = args[2];

                if (!isValidCompanyName(newFilialeName)) {
                    player.sendMessage("§cInvalid subsidiary name. Use 3-20 alphanumeric characters.");
                    return true;
                }

                // Create subsidiary as a new company
                if (companyManager.createCompany(newFilialeName, player.getUniqueId(), player.getName())) {
                    // Set parent relationship
                    companyManager.addSubsidiary(companyName, newFilialeName);

                    player.sendMessage("§aSubsidiary §l" + newFilialeName + " §ahas been created!");
                    player.sendMessage("§7Parent company: §f" + companyName);
                } else {
                    player.sendMessage("§cA company with this name already exists.");
                }
                break;

            case "add":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /entreprise filiale add <company>");
                    return true;
                }

                String targetCompanyName = args[2];
                Company targetCompany = companyManager.getCompany(targetCompanyName);

                if (targetCompany == null) {
                    player.sendMessage("§cCompany not found.");
                    return true;
                }

                if (targetCompany.isSubsidiary()) {
                    player.sendMessage("§cThis company is already a subsidiary of another company.");
                    return true;
                }

                if (targetCompany.getName().equalsIgnoreCase(companyName)) {
                    player.sendMessage("§cA company cannot be its own subsidiary.");
                    return true;
                }

                // Add subsidiary relationship
                if (companyManager.addSubsidiary(companyName, targetCompanyName)) {
                    player.sendMessage("§a§l" + targetCompanyName + " §ais now a subsidiary of §l" + companyName + "§a!");

                    // Notify target CEO
                    Player targetCEO = Bukkit.getPlayer(targetCompany.getCeoUUID());
                    if (targetCEO != null && targetCEO.isOnline()) {
                        targetCEO.sendMessage("§eYour company is now a subsidiary of §l" + companyName + "§e.");
                    }
                } else {
                    player.sendMessage("§cFailed to add subsidiary.");
                }
                break;

            case "remove":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /entreprise filiale remove <company>");
                    return true;
                }

                String removeName = args[2];
                if (!company.hasSubsidiary(removeName)) {
                    player.sendMessage("§cThis company is not your subsidiary.");
                    return true;
                }

                if (companyManager.removeSubsidiary(companyName, removeName)) {
                    player.sendMessage("§aRemoved §l" + removeName + " §aas your subsidiary.");
                } else {
                    player.sendMessage("§cFailed to remove subsidiary.");
                }
                break;

            case "list":
                player.sendMessage("§6Subsidiaries of §l" + companyName);
                if (company.getSubsidiaries().isEmpty()) {
                    player.sendMessage("§7No subsidiaries.");
                } else {
                    for (String sub : company.getSubsidiaries()) {
                        player.sendMessage("§7- §f" + sub);
                    }
                }
                break;

            default:
                player.sendMessage("§cUnknown filiale command. Use: create, add, remove, list");
        }

        return true;
    }

    private boolean handleBatiment(Player player) {
        String companyName = companyManager.getPlayerCompany(player.getUniqueId());
        if (companyName == null) {
            player.sendMessage(MSG_NOT_IN_COMPANY);
            return true;
        }

        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage(MSG_COMPANY_NOT_FOUND);
            return true;
        }

        if (!company.isCEO(player.getUniqueId())) {
            player.sendMessage(MSG_ONLY_CEO);
            return true;
        }

        org.bukkit.Location loc = player.getLocation();

        if (companyManager.setCompanyHeadquarters(companyName, loc)) {
            if (dynmapManager != null && dynmapManager.isDynmapEnabled()) {
                dynmapManager.addOrUpdateCompanyMarker(company);
            }
            player.sendMessage("§aHeadquarters set for §l" + companyName + "§a!");
            player.sendMessage("§7Location: §f" + company.getHeadquartersString());
        } else {
            player.sendMessage("§cFailed to set headquarters.");
        }
        return true;
    }

    private boolean handleStatus(Player player, String[] args) {
        String companyName = companyManager.getPlayerCompany(player.getUniqueId());
        if (companyName == null) {
            player.sendMessage("§cYou are not in a company.");
            return true;
        }

        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage("§cCompany not found.");
            return true;
        }

        // Check if CEO
        if (!company.isCEO(player.getUniqueId())) {
            player.sendMessage("§cOnly the CEO can change the company status.");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("national")) {
            if (!company.isInternational()) {
                player.sendMessage("§cYour company is already National.");
                return true;
            }

            company.setNational();
            player.sendMessage("§aCompany status changed to §lNational§a.");
            player.sendMessage("§7Member limit reduced to " + Company.NATIONAL_MEMBER_LIMIT + ".");
            return true;
        }

        if (subCommand.equals("international")) {
            if (company.isInternational()) {
                player.sendMessage("§cYour company is already International.");
                return true;
            }

            // Check if player has enough money for upgrade
            if (!economyManager.hasEnough(player, Company.INTERNATIONAL_UPGRADE_COST)) {
                player.sendMessage("§cYou need §l" + economyManager.formatMoney(Company.INTERNATIONAL_UPGRADE_COST) + " §cto upgrade to International.");
                return true;
            }

            // Confirm upgrade
            if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
                player.sendMessage("§eYou are about to upgrade §l" + companyName + " §eto §6§lInternational§e.");
                player.sendMessage("§7Cost: §c" + economyManager.formatMoney(Company.INTERNATIONAL_UPGRADE_COST));
                player.sendMessage("§7Member limit will increase from " + Company.NATIONAL_MEMBER_LIMIT + " to " + Company.INTERNATIONAL_MEMBER_LIMIT + ".");
                player.sendMessage("§cType §e/entreprise international confirm §cto proceed.");
                return true;
            }

            // Withdraw upgrade fee
            economyManager.getEconomy().withdrawPlayer(player, Company.INTERNATIONAL_UPGRADE_COST);

            // Upgrade to international
            if (companyManager.upgradeToInternational(companyName)) {
                player.sendMessage("§6§l" + companyName + " §6is now an International company!");
                player.sendMessage("§7Cost: §c" + economyManager.formatMoney(Company.INTERNATIONAL_UPGRADE_COST));
                player.sendMessage("§aMember limit increased to " + Company.INTERNATIONAL_MEMBER_LIMIT + "!");

                // Update Dynmap marker
                if (dynmapManager != null && dynmapManager.isDynmapEnabled() && company.hasHeadquarters()) {
                    dynmapManager.addOrUpdateCompanyMarker(companyManager.getCompany(companyName));
                }
            } else {
                // Refund if upgrade failed
                economyManager.getEconomy().depositPlayer(player, Company.INTERNATIONAL_UPGRADE_COST);
                player.sendMessage("§cFailed to upgrade company.");
            }
        }

        return true;
    }

    private boolean handleList(Player player) {
        if (!checkPermission(player, "dwcompany.use")) return true;
        companyGUI.openCompanyList(player, 0);
        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        String targetCompany;

        if (args.length < 2) {
            // Show own company info
            targetCompany = companyManager.getPlayerCompany(player.getUniqueId());
            if (targetCompany == null) {
                player.sendMessage("§cYou are not in a company. Specify a company name.");
                return true;
            }
        } else {
            targetCompany = args[1];
        }

        Company company = companyManager.getCompany(targetCompany);
        if (company == null) {
            player.sendMessage("§cCompany not found.");
            return true;
        }

        // Display info
        player.sendMessage("§6======== §l" + company.getName() + " §6========");
        player.sendMessage("§7Level: " + company.getLevelColor() + company.getLevel());
        player.sendMessage("§7CEO: §f" + company.getCeoName());
        player.sendMessage("§7Members: §f" + company.getMemberCount());
        player.sendMessage("§7Balance: §a" + economyManager.formatMoney(company.getBalance()));
        player.sendMessage("§7Total Earned: §a" + economyManager.formatMoney(company.getTotalMoneyEarned()));

        if (company.isSubsidiary()) {
            player.sendMessage("§7Subsidiary of: §f" + company.getParentCompany());
        }

        if (!company.getSubsidiaries().isEmpty()) {
            player.sendMessage("§7Subsidiaries: §f" + String.join(", ", company.getSubsidiaries()));
        }

        return true;
    }

    private boolean handleDelete(Player player, String[] args) {
        String companyName = companyManager.getPlayerCompany(player.getUniqueId());

        if (companyName == null) {
            player.sendMessage("§cYou are not in a company.");
            return true;
        }

        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage("§cCompany not found.");
            return true;
        }

        // Check if CEO or admin
        if (!company.isCEO(player.getUniqueId()) && !player.hasPermission("dwcompany.admin")) {
            player.sendMessage("§cOnly the CEO can delete the company.");
            return true;
        }

        // Confirm deletion
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            player.sendMessage("§c§lWARNING: This will permanently delete your company!");
            player.sendMessage("§cAll data including bank balance will be lost.");
            player.sendMessage("§cType §e/entreprise delete confirm §cto proceed.");
            return true;
        }

        // Delete company
        if (companyManager.deleteCompany(companyName)) {
            player.sendMessage("§cCompany §l" + companyName + " §chas been deleted.");

            // Notify all online members
            for (UUID memberUUID : company.getMembers()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline() && !member.getUniqueId().equals(player.getUniqueId())) {
                    member.sendMessage("§cYour company §l" + companyName + " §chas been deleted by the CEO.");
                }
            }
        } else {
            player.sendMessage("§cFailed to delete company.");
        }

        return true;
    }

    private boolean handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /entreprise transfer <player>");
            return true;
        }

        String companyName = companyManager.getPlayerCompany(player.getUniqueId());
        if (companyName == null) {
            player.sendMessage("§cYou are not in a company.");
            return true;
        }

        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage("§cCompany not found.");
            return true;
        }

        // Check if CEO
        if (!company.isCEO(player.getUniqueId())) {
            player.sendMessage("§cOnly the CEO can transfer ownership.");
            return true;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            player.sendMessage("§cPlayer not found or offline.");
            return true;
        }

        // Check if target is member
        if (!company.hasMember(target.getUniqueId())) {
            player.sendMessage("§cThis player is not a member of your company.");
            return true;
        }

        // Confirm transfer
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            player.sendMessage("§eYou are about to transfer ownership of §l" + companyName + " §eto §l" + target.getName() + "§e.");
            player.sendMessage("§c§lWARNING: You will lose CEO status!");
            player.sendMessage("§cType §e/entreprise transfer " + target.getName() + " confirm §cto proceed.");
            return true;
        }

        // Transfer ownership
        if (companyManager.transferOwnership(companyName, target.getUniqueId(), target.getName())) {
            player.sendMessage("§aYou have transferred ownership of §l" + companyName + " §ato §l" + target.getName() + "§a.");
            target.sendMessage("§aYou are now the CEO of §l" + companyName + "§a!");

            // Notify other members
            for (UUID memberUUID : company.getMembers()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    if (!member.getUniqueId().equals(player.getUniqueId()) &&
                            !member.getUniqueId().equals(target.getUniqueId())) {
                        member.sendMessage("§e" + target.getName() + " is now the CEO of your company.");
                    }
                }
            }
        } else {
            player.sendMessage("§cFailed to transfer ownership.");
        }

        return true;
    }

    private boolean handleMembers(Player player) {
        String companyName = companyManager.getPlayerCompany(player.getUniqueId());
        if (companyName == null) {
            player.sendMessage(MSG_NOT_IN_COMPANY);
            return true;
        }

        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage(MSG_COMPANY_NOT_FOUND);
            return true;
        }

        player.sendMessage("§6Members of §l" + companyName);
        player.sendMessage("§7CEO: §6" + company.getCeoName());

        for (UUID memberUUID : company.getMembers()) {
            if (!memberUUID.equals(company.getCeoUUID())) {
                String memberName = company.getMemberName(memberUUID);
                player.sendMessage("§7- §f" + memberName);
            }
        }

        player.sendMessage("§7Total: §f" + company.getMemberCount() + " members");
        return true;
    }

    private boolean handleAccept(Player player, String[] args) {
        String companyName = companyManager.getPlayerCompany(player.getUniqueId());
        if (companyName == null) {
            player.sendMessage("§cYou are not in a company.");
            return true;
        }

        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage("§cCompany not found.");
            return true;
        }

        // Check if CEO
        if (!company.isCEO(player.getUniqueId())) {
            player.sendMessage("§cOnly the CEO can accept join requests.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /entreprise accept <player>");
            return true;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            // Try to find by name in pending requests
            UUID pendingUUID = null;
            for (UUID uuid : company.getPendingJoinRequests()) {
                if (company.getMemberName(uuid).equalsIgnoreCase(targetName)) {
                    pendingUUID = uuid;
                    break;
                }
            }

            if (pendingUUID == null) {
                player.sendMessage("§cNo pending join request from this player.");
                return true;
            }

            // Accept the offline/unknown player
            company.removeJoinRequest(pendingUUID);
            companyManager.addMemberToCompany(companyName, pendingUUID, targetName);
            player.sendMessage("§aAccepted join request from §l" + targetName + "§a.");
            return true;
        }

        // Check if request exists
        if (!company.hasJoinRequest(target.getUniqueId())) {
            player.sendMessage("§cNo pending join request from this player.");
            return true;
        }

        // Check if player is already in a company
        if (companyManager.isInCompany(target.getUniqueId())) {
            player.sendMessage("§cThis player is already in a company.");
            company.removeJoinRequest(target.getUniqueId());
            return true;
        }

        // Accept the request
        company.removeJoinRequest(target.getUniqueId());
        if (companyManager.addMemberToCompany(companyName, target.getUniqueId(), target.getName())) {
            player.sendMessage("§a§l" + target.getName() + " §ahas joined your company!");
            target.sendMessage("§aYou have joined §l" + companyName + "§a!");

            // Notify other members
            for (UUID memberUUID : company.getMembers()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline() &&
                        !member.getUniqueId().equals(player.getUniqueId()) &&
                        !member.getUniqueId().equals(target.getUniqueId())) {
                    member.sendMessage("§e" + target.getName() + " has joined your company.");
                }
            }
        } else {
            player.sendMessage("§cFailed to add member.");
        }

        return true;
    }

    private boolean handleDeny(Player player, String[] args) {
        String companyName = companyManager.getPlayerCompany(player.getUniqueId());
        if (companyName == null) {
            player.sendMessage("§cYou are not in a company.");
            return true;
        }

        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage("§cCompany not found.");
            return true;
        }

        // Check if CEO
        if (!company.isCEO(player.getUniqueId())) {
            player.sendMessage("§cOnly the CEO can deny join requests.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /entreprise deny <player>");
            return true;
        }

        String targetName = args[1];

        // Find player UUID from pending requests
        UUID targetUUID = null;
        for (UUID uuid : company.getPendingJoinRequests()) {
            String name = company.getMemberName(uuid);
            if (name.equalsIgnoreCase(targetName)) {
                targetUUID = uuid;
                break;
            }
        }

        if (targetUUID == null) {
            // Try to find online player
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                targetUUID = target.getUniqueId();
            } else {
                player.sendMessage("§cNo pending join request from this player.");
                return true;
            }
        }

        if (!company.hasJoinRequest(targetUUID)) {
            player.sendMessage("§cNo pending join request from this player.");
            return true;
        }

        // Deny the request
        company.removeJoinRequest(targetUUID);
        player.sendMessage("§cDenied join request from §l" + targetName + "§c.");

        // Notify player if online
        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null && target.isOnline()) {
            target.sendMessage("§cYour join request to §l" + companyName + " §cwas denied.");
        }

        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /entreprise kick <player>");
            return true;
        }

        String companyName = companyManager.getPlayerCompany(player.getUniqueId());
        if (companyName == null) {
            player.sendMessage("§cYou are not in a company.");
            return true;
        }

        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage("§cCompany not found.");
            return true;
        }

        // Check if CEO or has admin permission
        if (!company.isCEO(player.getUniqueId()) && !player.hasPermission("dwcompany.admin")) {
            player.sendMessage("§cOnly the CEO can kick members.");
            return true;
        }

        String targetName = args[1];

        // Find the member UUID
        UUID targetUUID = null;
        for (UUID memberUUID : company.getMembers()) {
            if (company.getMemberName(memberUUID).equalsIgnoreCase(targetName)) {
                targetUUID = memberUUID;
                break;
            }
        }

        if (targetUUID == null) {
            player.sendMessage("§cPlayer is not a member of your company.");
            return true;
        }

        // Can't kick CEO
        if (company.isCEO(targetUUID)) {
            player.sendMessage("§cYou cannot kick the CEO. Transfer ownership first.");
            return true;
        }

        // Kick the member
        if (companyManager.removeMemberFromCompany(targetUUID)) {
            player.sendMessage("§aKicked §l" + targetName + " §afrom your company.");

            // Notify the kicked player
            Player target = Bukkit.getPlayer(targetUUID);
            if (target != null && target.isOnline()) {
                target.sendMessage("§cYou have been kicked from §l" + companyName + "§c.");
            }
        } else {
            player.sendMessage("§cFailed to kick member.");
        }

        return true;
    }

    private boolean handleManage(Player player) {
        String companyName = companyManager.getPlayerCompany(player.getUniqueId());
        if (companyName == null) {
            player.sendMessage(MSG_NOT_IN_COMPANY);
            return true;
        }

        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage(MSG_COMPANY_NOT_FOUND);
            return true;
        }

        if (!company.isCEO(player.getUniqueId()) && !player.hasPermission("dwcompany.admin")) {
            player.sendMessage(MSG_ONLY_CEO);
            return true;
        }

        companyGUI.openCompanyManagement(player);
        return true;
    }

    private boolean handleRequests(Player player) {
        String companyName = companyManager.getPlayerCompany(player.getUniqueId());
        if (companyName == null) {
            player.sendMessage(MSG_NOT_IN_COMPANY);
            return true;
        }

        Company company = companyManager.getCompany(companyName);
        if (company == null) {
            player.sendMessage(MSG_COMPANY_NOT_FOUND);
            return true;
        }

        if (!company.isCEO(player.getUniqueId())) {
            player.sendMessage(MSG_ONLY_CEO);
            return true;
        }

        Set<UUID> requests = company.getPendingJoinRequests();
        if (requests.isEmpty()) {
            player.sendMessage("§7No pending join requests.");
            return true;
        }

        player.sendMessage("§6Pending Join Requests");
        for (UUID uuid : requests) {
            player.sendMessage("§7- §f" + company.getMemberName(uuid) + " §7(accept/deny)");
        }
        return true;
    }

    private boolean handleReload(Player player) {
        if (!player.hasPermission("dwcompany.admin")) {
            player.sendMessage(MSG_NO_PERMISSION);
            return true;
        }

        player.sendMessage("§eReloading plugin...");

        if (plugin.reload()) {
            player.sendMessage("§aPlugin reloaded successfully!");
        } else {
            player.sendMessage("§cError reloading plugin. Check console for details.");
        }
        return true;
    }

    private boolean handleHelp(Player player) {
        player.sendMessage("§6===== DWcompany Help =====");
        player.sendMessage("§e/entreprise §7- Open company list GUI");
        player.sendMessage("§e/entreprise create <name> §7- Create a new company (100k-500k)");
        player.sendMessage("§e/entreprise join <company> §7- Request to join a company");
        player.sendMessage("§e/entreprise leave §7- Leave your company");
        player.sendMessage("§e/entreprise bank §7- Open bank GUI");
        player.sendMessage("§e/entreprise bank deposit <amount> §7- Deposit money");
        player.sendMessage("§e/entreprise bank withdraw <amount> §7- Withdraw money (CEO only)");
        player.sendMessage("§e/entreprise batiment §7- Set company headquarters");
        player.sendMessage("§e/entreprise international §7- Upgrade to International (20k)");
        player.sendMessage("§e/entreprise filiale create <name> §7- Create a subsidiary");
        player.sendMessage("§e/entreprise filiale add <company> §7- Add a subsidiary");
        player.sendMessage("§e/entreprise info [company] §7- View company info");
        player.sendMessage("§e/entreprise members §7- List company members");
        player.sendMessage("§e/entreprise transfer <player> §7- Transfer CEO role");
        player.sendMessage("§e/entreprise manage §7- Open management GUI (CEO)");
        player.sendMessage("§e/entreprise delete §7- Delete your company (CEO)");
        player.sendMessage("§e/entreprise help §7- Show this help");

        if (player.hasPermission("dwcompany.admin")) {
            player.sendMessage("§c§lAdmin Commands:");
            player.sendMessage("§c/entreprise reload §7- Reload plugin configuration");
            player.sendMessage("§c/entreprise admin delete <company> §7- Delete any company");
        }

        return true;
    }

    private void handleAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dwcompany.admin")) {
            sender.sendMessage("§cYou don't have permission.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /entreprise admin <delete>");
            return;
        }

        String adminAction = args[1].toLowerCase();

        if (adminAction.equals("delete")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /entreprise admin delete <company>");
                return;
            }

            String companyName = args[2];
            if (companyManager.deleteCompany(companyName)) {
                sender.sendMessage("§cCompany §l" + companyName + " §chas been deleted by an admin.");
            } else {
                sender.sendMessage("§cCompany not found.");
            }
        }
    }

    // ==================== Utility Methods ====================

    private boolean checkPermission(Player player, String permission) {
        if (!player.hasPermission(permission)) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        return true;
    }

    private boolean isValidCompanyName(String name) {
        // 3-20 characters, alphanumeric only
        return name.matches("^[a-zA-Z0-9]{3,20}$");
    }

    private boolean isOnCooldown(UUID playerUUID) {
        if (!joinRequestCooldowns.containsKey(playerUUID)) {
            return false;
        }
        long lastRequest = joinRequestCooldowns.get(playerUUID);
        return System.currentTimeMillis() - lastRequest < JOIN_COOLDOWN_MS;
    }

    private long getCooldownRemaining(UUID playerUUID) {
        if (!joinRequestCooldowns.containsKey(playerUUID)) {
            return 0;
        }
        long lastRequest = joinRequestCooldowns.get(playerUUID);
        long remaining = JOIN_COOLDOWN_MS - (System.currentTimeMillis() - lastRequest);
        return Math.max(0, remaining);
    }

    private void setCooldown(UUID playerUUID) {
        joinRequestCooldowns.put(playerUUID, System.currentTimeMillis());
    }

    // ==================== Tab Completer ====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("create");
            completions.add("join");
            completions.add("leave");
            completions.add("bank");
            completions.add("filiale");
            completions.add("list");
            completions.add("info");
            completions.add("delete");
            completions.add("transfer");
            completions.add("members");
            completions.add("accept");
            completions.add("deny");
            completions.add("kick");
            completions.add("manage");
            completions.add("requests");
            completions.add("reload");
            completions.add("help");

            if (sender.hasPermission("dwcompany.admin")) {
                completions.add("admin");
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "join":
                case "info":
                    // Suggest company names
                    for (Company company : companyManager.getAllCompanies()) {
                        completions.add(company.getName());
                    }
                    break;

                case "bank":
                    completions.add("deposit");
                    completions.add("withdraw");
                    completions.add("balance");
                    break;

                case "filiale":
                    completions.add("create");
                    completions.add("add");
                    completions.add("remove");
                    completions.add("list");
                    break;

                case "accept":
                case "deny":
                case "kick":
                    // Suggest pending requests or members
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        String companyName = companyManager.getPlayerCompany(player.getUniqueId());
                        if (companyName != null) {
                            Company company = companyManager.getCompany(companyName);
                            if (company != null) {
                                if (subCommand.equals("accept") || subCommand.equals("deny")) {
                                    for (UUID uuid : company.getPendingJoinRequests()) {
                                        completions.add(company.getMemberName(uuid));
                                    }
                                } else if (subCommand.equals("kick")) {
                                    for (UUID uuid : company.getMembers()) {
                                        if (!company.isCEO(uuid)) {
                                            completions.add(company.getMemberName(uuid));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;

                case "transfer":
                    // Suggest members
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        String companyName = companyManager.getPlayerCompany(player.getUniqueId());
                        if (companyName != null) {
                            Company company = companyManager.getCompany(companyName);
                            if (company != null) {
                                for (UUID uuid : company.getMembers()) {
                                    if (!company.isCEO(uuid)) {
                                        completions.add(company.getMemberName(uuid));
                                    }
                                }
                            }
                        }
                    }
                    break;

                case "admin":
                    if (sender.hasPermission("dwcompany.admin")) {
                        completions.add("delete");
                    }
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String subAction = args[1].toLowerCase();

            if (subCommand.equals("bank") && (subAction.equals("deposit") || subAction.equals("withdraw"))) {
                completions.add("100");
                completions.add("500");
                completions.add("1000");
                completions.add("5000");
            }

            if (subCommand.equals("filiale") && subAction.equals("add")) {
                // Suggest all companies
                for (Company company : companyManager.getAllCompanies()) {
                    completions.add(company.getName());
                }
            }

            if (subCommand.equals("filiale") && subAction.equals("remove")) {
                // Suggest subsidiaries
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    String companyName = companyManager.getPlayerCompany(player.getUniqueId());
                    if (companyName != null) {
                        Company company = companyManager.getCompany(companyName);
                        if (company != null) {
                            completions.addAll(company.getSubsidiaries());
                        }
                    }
                }
            }

            if (subCommand.equals("admin") && subAction.equals("delete")) {
                // Suggest all companies for admin delete
                for (Company company : companyManager.getAllCompanies()) {
                    completions.add(company.getName());
                }
            }
        }

        // Filter completions based on input
        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));

        return completions;
    }
}
