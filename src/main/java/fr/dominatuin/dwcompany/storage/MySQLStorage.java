package fr.dominatuin.dwcompany.storage;

import fr.dominatuin.dwcompany.Company;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * MySQL storage provider for company data.
 * Stores all company information in a MySQL database.
 */
public class MySQLStorage implements StorageProvider {

    private final JavaPlugin plugin;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String tablePrefix;

    private Connection connection;
    private long lastSaveTime;
    private boolean connected;

    /**
     * Creates a new MySQLStorage instance.
     *
     * @param plugin   The plugin instance
     * @param host     MySQL host
     * @param port     MySQL port
     * @param database Database name
     * @param username MySQL username
     * @param password MySQL password
     * @param tablePrefix Table prefix
     */
    public MySQLStorage(JavaPlugin plugin, String host, int port, String database,
                        String username, String password, String tablePrefix) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.tablePrefix = tablePrefix;
        this.lastSaveTime = 0;
        this.connected = false;
    }

    @Override
    public boolean initialize() {
        try {
            // Load MySQL driver
            Class.forName("com.mysql.jdbc.Driver");

            // Connect to database
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
            connection = DriverManager.getConnection(url, username, password);

            // Create tables
            createTables();

            connected = true;
            plugin.getLogger().info("MySQL storage initialized successfully.");
            return true;

        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "MySQL driver not found", e);
            return false;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL database", e);
            return false;
        }
    }

    /**
     * Creates necessary database tables.
     */
    private void createTables() throws SQLException {
        // Main companies table
        String createCompaniesTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "companies (" +
                "name VARCHAR(64) PRIMARY KEY," +
                "ceo_uuid VARCHAR(36) NOT NULL," +
                "ceo_name VARCHAR(64) NOT NULL," +
                "balance DOUBLE DEFAULT 0," +
                "total_earned DOUBLE DEFAULT 0," +
                "hq_world VARCHAR(64)," +
                "hq_x DOUBLE DEFAULT 0," +
                "hq_y DOUBLE DEFAULT 0," +
                "hq_z DOUBLE DEFAULT 0," +
                "has_hq BOOLEAN DEFAULT FALSE," +
                "is_international BOOLEAN DEFAULT FALSE," +
                "parent_company VARCHAR(64)," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        // Members table
        String createMembersTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "members (" +
                "company_name VARCHAR(64) NOT NULL," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "player_name VARCHAR(64) NOT NULL," +
                "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "PRIMARY KEY (company_name, player_uuid)," +
                "FOREIGN KEY (company_name) REFERENCES " + tablePrefix + "companies(name) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        // Subsidiaries table
        String createSubsidiariesTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "subsidiaries (" +
                "parent_name VARCHAR(64) NOT NULL," +
                "subsidiary_name VARCHAR(64) NOT NULL," +
                "PRIMARY KEY (parent_name, subsidiary_name)," +
                "FOREIGN KEY (parent_name) REFERENCES " + tablePrefix + "companies(name) ON DELETE CASCADE," +
                "FOREIGN KEY (subsidiary_name) REFERENCES " + tablePrefix + "companies(name) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createCompaniesTable);
            stmt.execute(createMembersTable);
            stmt.execute(createSubsidiariesTable);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing MySQL connection", e);
        }
        connected = false;
    }

    @Override
    public List<Company> loadCompanies() {
        List<Company> companies = new ArrayList<>();

        if (!connected || connection == null) {
            plugin.getLogger().warning("Cannot load companies - MySQL not connected");
            return companies;
        }

        try {
            // Load all companies
            String query = "SELECT * FROM " + tablePrefix + "companies";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    try {
                        Company company = loadCompanyFromResultSet(rs);
                        if (company != null) {
                            companies.add(company);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to load company from MySQL", e);
                    }
                }
            }

            plugin.getLogger().info("Loaded " + companies.size() + " companies from MySQL");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load companies from MySQL", e);
        }

        return companies;
    }

    /**
     * Loads a company from a database result set.
     */
    private Company loadCompanyFromResultSet(ResultSet rs) throws SQLException {
        String name = rs.getString("name");
        String ceoUUIDStr = rs.getString("ceo_uuid");
        String ceoName = rs.getString("ceo_name");

        UUID ceoUUID = UUID.fromString(ceoUUIDStr);
        Company company = new Company(name, ceoUUID, ceoName);

        // Load balance
        double balance = rs.getDouble("balance");
        if (balance > 0) {
            company.deposit(balance);
        }

        // Load headquarters
        if (rs.getBoolean("has_hq")) {
            String world = rs.getString("hq_world");
            double x = rs.getDouble("hq_x");
            double y = rs.getDouble("hq_y");
            double z = rs.getDouble("hq_z");
            company.setHeadquarters(world, x, y, z);
        }

        // Load international status
        if (rs.getBoolean("is_international")) {
            company.upgradeToInternational();
        }

        // Load parent company
        String parent = rs.getString("parent_company");
        if (parent != null) {
            company.setParentCompany(parent);
        }

        // Load members
        loadMembers(company);

        // Load subsidiaries
        loadSubsidiaries(company);

        return company;
    }

    /**
     * Loads members for a company.
     */
    private void loadMembers(Company company) throws SQLException {
        String query = "SELECT * FROM " + tablePrefix + "members WHERE company_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, company.getName());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String uuidStr = rs.getString("player_uuid");
                    String name = rs.getString("player_name");
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        if (!uuid.equals(company.getCeoUUID())) {
                            company.addMember(uuid, name);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid member UUID for company " + company.getName());
                    }
                }
            }
        }
    }

    /**
     * Loads subsidiaries for a company.
     */
    private void loadSubsidiaries(Company company) throws SQLException {
        String query = "SELECT * FROM " + tablePrefix + "subsidiaries WHERE parent_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, company.getName());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String subName = rs.getString("subsidiary_name");
                    company.addSubsidiary(subName);
                }
            }
        }
    }

    @Override
    public boolean saveCompany(Company company) {
        if (!connected || connection == null) {
            return false;
        }

        try {
            // Insert or update company
            String query = "INSERT INTO " + tablePrefix + "companies " +
                    "(name, ceo_uuid, ceo_name, balance, total_earned, hq_world, hq_x, hq_y, hq_z, has_hq, is_international, parent_company) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "ceo_uuid=?, ceo_name=?, balance=?, total_earned=?, hq_world=?, hq_x=?, hq_y=?, hq_z=?, has_hq=?, is_international=?, parent_company=?";

            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                // Insert values
                stmt.setString(1, company.getName());
                stmt.setString(2, company.getCeoUUID().toString());
                stmt.setString(3, company.getCeoName());
                stmt.setDouble(4, company.getBalance());
                stmt.setDouble(5, company.getTotalMoneyEarned());

                if (company.hasHeadquarters()) {
                    Location hq = company.getHeadquartersLocation();
                    stmt.setString(6, hq.getWorld().getName());
                    stmt.setDouble(7, hq.getX());
                    stmt.setDouble(8, hq.getY());
                    stmt.setDouble(9, hq.getZ());
                    stmt.setBoolean(10, true);
                } else {
                    stmt.setNull(6, Types.VARCHAR);
                    stmt.setDouble(7, 0);
                    stmt.setDouble(8, 0);
                    stmt.setDouble(9, 0);
                    stmt.setBoolean(10, false);
                }

                stmt.setBoolean(11, company.isInternational());
                stmt.setString(12, company.getParentCompany());

                // Update values (same as insert)
                stmt.setString(13, company.getCeoUUID().toString());
                stmt.setString(14, company.getCeoName());
                stmt.setDouble(15, company.getBalance());
                stmt.setDouble(16, company.getTotalMoneyEarned());

                if (company.hasHeadquarters()) {
                    Location hq = company.getHeadquartersLocation();
                    stmt.setString(17, hq.getWorld().getName());
                    stmt.setDouble(18, hq.getX());
                    stmt.setDouble(19, hq.getY());
                    stmt.setDouble(20, hq.getZ());
                    stmt.setBoolean(21, true);
                } else {
                    stmt.setNull(17, Types.VARCHAR);
                    stmt.setDouble(18, 0);
                    stmt.setDouble(19, 0);
                    stmt.setDouble(20, 0);
                    stmt.setBoolean(21, false);
                }

                stmt.setBoolean(22, company.isInternational());
                stmt.setString(23, company.getParentCompany());

                stmt.executeUpdate();
            }

            // Save members
            saveMembers(company);

            // Save subsidiaries
            saveSubsidiaries(company);

            return true;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save company to MySQL", e);
            return false;
        }
    }

    /**
     * Saves members for a company.
     */
    private void saveMembers(Company company) throws SQLException {
        // Delete existing members
        String deleteQuery = "DELETE FROM " + tablePrefix + "members WHERE company_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteQuery)) {
            stmt.setString(1, company.getName());
            stmt.executeUpdate();
        }

        // Insert current members
        String insertQuery = "INSERT INTO " + tablePrefix + "members (company_name, player_uuid, player_name) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
            for (UUID memberUUID : company.getMembers()) {
                stmt.setString(1, company.getName());
                stmt.setString(2, memberUUID.toString());
                stmt.setString(3, company.getMemberName(memberUUID));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    /**
     * Saves subsidiaries for a company.
     */
    private void saveSubsidiaries(Company company) throws SQLException {
        // Delete existing subsidiaries
        String deleteQuery = "DELETE FROM " + tablePrefix + "subsidiaries WHERE parent_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteQuery)) {
            stmt.setString(1, company.getName());
            stmt.executeUpdate();
        }

        // Insert current subsidiaries
        String insertQuery = "INSERT INTO " + tablePrefix + "subsidiaries (parent_name, subsidiary_name) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
            for (String subName : company.getSubsidiaries()) {
                stmt.setString(1, company.getName());
                stmt.setString(2, subName);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public boolean saveCompanies(Collection<Company> companies) {
        if (!connected || connection == null) {
            return false;
        }

        boolean allSuccess = true;
        for (Company company : companies) {
            if (!saveCompany(company)) {
                allSuccess = false;
            }
        }

        if (allSuccess) {
            lastSaveTime = System.currentTimeMillis();
        }

        return allSuccess;
    }

    @Override
    public boolean deleteCompany(String companyName) {
        if (!connected || connection == null) {
            return false;
        }

        try {
            String query = "DELETE FROM " + tablePrefix + "companies WHERE name = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, companyName);
                stmt.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete company from MySQL", e);
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return connected && connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "MySQL";
    }

    @Override
    public long getLastSaveTime() {
        return lastSaveTime;
    }

    @Override
    public boolean validateData() {
        if (!isConnected()) {
            return false;
        }

        try {
            String query = "SELECT COUNT(*) as count FROM " + tablePrefix + "companies";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    plugin.getLogger().info("MySQL data validation: " + count + " companies found");
                }
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "MySQL data validation failed", e);
            return false;
        }
    }

    /**
     * Tests the database connection.
     *
     * @return true if connection is working
     */
    public boolean testConnection() {
        try {
            return connection != null && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
}
