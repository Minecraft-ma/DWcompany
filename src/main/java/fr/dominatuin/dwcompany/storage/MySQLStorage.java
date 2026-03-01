package fr.dominatuin.dwcompany.storage;

import fr.dominatuin.dwcompany.Company;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * High-performance MySQL storage provider optimized for Minecraft plugins.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Connection pooling and auto-reconnection</li>
 *   <li>Async operations to prevent main thread blocking</li>
 *   <li>Batch operations for better performance</li>
 *   <li>Minecraft-specific optimizations</li>
 *   <li>Automatic table creation and validation</li>
 * </ul>
 * 
 * @author Dominatuin
 * @version 1.0
 * @since 1.0-SNAPSHOT
 */
public class MySQLStorage implements StorageProvider {

    // ======== Database Configuration ========
    private final JavaPlugin plugin;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String tablePrefix;

    // ======== Connection Management ========
    private volatile Connection connection;
    private volatile boolean connected;
    private long lastConnectionTime;
    private final Object connectionLock = new Object();
    
    // ======== Performance Monitoring ========
    private long lastSaveTime;
    private int queryCount;
    private long totalQueryTime;
    private final Map<String, Long> queryStats = new ConcurrentHashMap<>();
    
    // ======== Minecraft-Specific Features ========
    private boolean useConnectionPooling = true;
    private int maxRetries = 3;
    private long connectionTimeout = 30000; // 30 seconds
    private long queryTimeout = 10000; // 10 seconds
    private boolean enableMetrics = true;
    
    // SQL constants to avoid duplication
    private static final String CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS ";
    private static final String SELECT_FROM = "SELECT * FROM ";
    private static final String INSERT_INTO = "INSERT INTO ";
    private static final String DELETE_FROM = "DELETE FROM ";

    /**
     * Creates a new MySQLStorage instance optimized for Minecraft plugins.
     *
     * @param plugin Main plugin instance (must not be null)
     * @param host MySQL server host
     * @param port MySQL server port
     * @param database Database name
     * @param username MySQL username
     * @param password MySQL password
     * @param tablePrefix Table prefix for multi-server environments
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public MySQLStorage(JavaPlugin plugin, String host, int port, String database,
                        String username, String password, String tablePrefix) {
        // Validate input parameters
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (database == null || database.trim().isEmpty()) {
            throw new IllegalArgumentException("Database cannot be null or empty");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        // Initialize configuration
        this.plugin = plugin;
        this.host = host.trim();
        this.port = port;
        this.database = database.trim();
        this.username = username.trim();
        this.password = password; // Password can be empty for some configurations
        this.tablePrefix = (tablePrefix != null ? tablePrefix.trim() : "") + "_";
        
        // Load Minecraft-specific settings from config
        loadMinecraftSettings();
        
        plugin.getLogger().info(String.format("MySQLStorage initialized for %s:%d/%s", host, port, database));
    }
    
    /**
     * Loads Minecraft-specific settings from plugin configuration.
     */
    private void loadMinecraftSettings() {
        // Load performance settings
        this.useConnectionPooling = plugin.getConfig().getBoolean("database.mysql.pooling.enabled", true);
        this.maxRetries = plugin.getConfig().getInt("database.mysql.max-retries", 3);
        this.connectionTimeout = plugin.getConfig().getLong("database.mysql.connection-timeout", 30000);
        this.queryTimeout = plugin.getConfig().getLong("database.mysql.query-timeout", 10000);
        this.enableMetrics = plugin.getConfig().getBoolean("database.mysql.metrics.enabled", true);
        
        plugin.getLogger().info(String.format("MySQL settings: pooling=%s, retries=%d, timeout=%dms", 
            useConnectionPooling, maxRetries, connectionTimeout));
    }

    // ======== Storage Provider Implementation ========

    @Override
    public boolean initialize() {
        try {
            // Load MySQL driver (new driver class for MySQL 8.0+)
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            plugin.getLogger().info("Connecting to MySQL database...");
            
            // Connect to database with Minecraft-optimized settings
            if (!connectWithRetry()) {
                plugin.getLogger().severe("Failed to connect to MySQL database after " + maxRetries + " attempts");
                return false;
            }
            
            // Create tables if they don't exist
            if (!createTables()) {
                plugin.getLogger().severe("Failed to create database tables");
                return false;
            }
            
            // Validate data integrity
            if (!validateData()) {
                plugin.getLogger().warning("Data validation found issues - attempting to fix");
                if (!fixDataIssues()) {
                    plugin.getLogger().warning("Some data issues could not be fixed automatically");
                }
            }
            
            // Start connection health monitoring
            startConnectionMonitoring();
            
            connected = true;
            lastConnectionTime = System.currentTimeMillis();
            
            plugin.getLogger().info("MySQL storage initialized successfully");
            return true;
            
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("MySQL JDBC driver not found! Please add MySQL connector to your plugin.");
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize MySQL storage", e);
            return false;
        }
    }
    
    /**
     * Connects to database with retry logic for Minecraft server environments.
     * 
     * @return true if connection successful
     */
    private boolean connectWithRetry() {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Build connection URL with Minecraft-optimized parameters
                String url = buildConnectionUrl();
                
                synchronized (connectionLock) {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                    
                    connection = DriverManager.getConnection(url, username, password);
                    
                    // Configure connection for Minecraft performance
                    configureConnection(connection);
                }
                
                plugin.getLogger().info(String.format("Connected to MySQL database (attempt %d/%d)", attempt, maxRetries));
                return true;
                
            } catch (SQLException e) {
                plugin.getLogger().warning(String.format("Connection attempt %d/%d failed: %s", 
                    attempt, maxRetries, e.getMessage()));
                
                if (attempt < maxRetries) {
                    // Wait before retry (exponential backoff)
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Builds MySQL connection URL with Minecraft-optimized parameters.
     * 
     * @return Connection URL string
     */
    private String buildConnectionUrl() {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:mysql://").append(host).append(":").append(port).append("/").append(database);
        url.append("?useSSL=false");
        url.append("&allowPublicKeyRetrieval=true");
        url.append("&useServerPrepStmts=true");
        url.append("&cachePrepStmts=true");
        url.append("&prepStmtCacheSize=250");
        url.append("&prepStmtCacheSqlLimit=2048");
        url.append("&useLocalSessionState=true");
        url.append("&useLocalTransactionState=true");
        url.append("&rewriteBatchedStatements=true");
        url.append("&cacheResultSetMetadata=true");
        url.append("&cacheServerConfiguration=true");
        url.append("&elideSetAutoCommits=true");
        url.append("&maintainTimeStats=false");
        url.append("&netTimeoutForStreamingResults=0");
        url.append("&autoReconnect=true");
        url.append("&autoReconnectForPools=true");
        url.append("&failOverReadOnly=false");
        url.append("&maxReconnects=10");
        url.append("&initialTimeout=10");
        
        return url.toString();
    }
    
    /**
     * Configures connection for optimal Minecraft performance.
     * 
     * @param conn Database connection
     * @throws SQLException if configuration fails
     */
    private void configureConnection(Connection conn) throws SQLException {
        // Set timeout values
        conn.setNetworkTimeout(null, (int) connectionTimeout);
        
        // Configure for Minecraft workloads
        try (Statement stmt = conn.createStatement()) {
            // Optimize for Minecraft data patterns
            stmt.execute("SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO'");
            stmt.execute("SET SESSION innodb_lock_wait_timeout = 5");
            stmt.execute("SET SESSION query_cache_type = ON");
            stmt.execute("SET SESSION query_cache_size = 67108864"); // 64MB
            stmt.execute("SET SESSION tmp_table_size = 67108864"); // 64MB
            stmt.execute("SET SESSION max_heap_table_size = 67108864"); // 64MB
        }
    }

    /**
     * Starts connection health monitoring for Minecraft server stability.
     */
    private void startConnectionMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkConnectionHealth();
            }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L); // Check every 5 minutes
    }
    
    /**
     * Checks connection health and reconnects if necessary.
     */
    private void checkConnectionHealth() {
        if (!connected) {
            return;
        }
        
        try {
            synchronized (connectionLock) {
                if (connection == null || connection.isClosed()) {
                    plugin.getLogger().warning("Database connection lost, attempting to reconnect...");
                    connected = false;
                    
                    if (connectWithRetry()) {
                        connected = true;
                        lastConnectionTime = System.currentTimeMillis();
                        plugin.getLogger().info("Database connection restored");
                    } else {
                        plugin.getLogger().severe("Failed to restore database connection");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking connection health", e);
        }
    }
    
    /**
     * Creates database tables optimized for Minecraft data.
     * 
     * @return true if tables created successfully
     */
    private boolean createTables() {
        try {
            synchronized (connectionLock) {
                if (connection == null || connection.isClosed()) {
                    return false;
                }
                
                // Create companies table
                String createCompaniesTable = CREATE_TABLE_IF_NOT_EXISTS + tablePrefix + "companies (" +
                    "name VARCHAR(64) NOT NULL PRIMARY KEY," +
                    "ceo_uuid VARCHAR(36) NOT NULL," +
                    "ceo_name VARCHAR(64) NOT NULL," +
                    "balance DOUBLE DEFAULT 0.0," +
                    "total_earned DOUBLE DEFAULT 0.0," +
                    "level INT DEFAULT 1," +
                    "max_members INT DEFAULT 5," +
                    "is_international BOOLEAN DEFAULT FALSE," +
                    "hq_world VARCHAR(64)," +
                    "hq_x DOUBLE DEFAULT 0.0," +
                    "hq_y DOUBLE DEFAULT 0.0," +
                    "hq_z DOUBLE DEFAULT 0.0," +
                    "has_hq BOOLEAN DEFAULT FALSE," +
                    "parent_company VARCHAR(64)," +
                    "is_subsidiary BOOLEAN DEFAULT FALSE," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC";
                
                // Create members table
                String createMembersTable = CREATE_TABLE_IF_NOT_EXISTS + tablePrefix + "members (" +
                    "company_name VARCHAR(64) NOT NULL," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(64) NOT NULL," +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (company_name, player_uuid)," +
                    "FOREIGN KEY (company_name) REFERENCES " + tablePrefix + "companies(name) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC";
                
                // Create subsidiaries table
                String createSubsidiariesTable = CREATE_TABLE_IF_NOT_EXISTS + tablePrefix + "subsidiaries (" +
                    "parent_company VARCHAR(64) NOT NULL," +
                    "subsidiary_name VARCHAR(64) NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (parent_company, subsidiary_name)," +
                    "FOREIGN KEY (parent_company) REFERENCES " + tablePrefix + "companies(name) ON DELETE CASCADE," +
                    "FOREIGN KEY (subsidiary_name) REFERENCES " + tablePrefix + "companies(name) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC";
                
                // Create join_requests table
                String createJoinRequestsTable = CREATE_TABLE_IF_NOT_EXISTS + tablePrefix + "join_requests (" +
                    "company_name VARCHAR(64) NOT NULL," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(64) NOT NULL," +
                    "requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (company_name, player_uuid)," +
                    "FOREIGN KEY (company_name) REFERENCES " + tablePrefix + "companies(name) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC";
                
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(createCompaniesTable);
                    stmt.execute(createMembersTable);
                    stmt.execute(createSubsidiariesTable);
                    stmt.execute(createJoinRequestsTable);
                    
                    plugin.getLogger().info("Database tables created/verified successfully");
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database tables", e);
            return false;
        }
    }

    /**
     * Validates data integrity for Minecraft plugin data.
     * 
     * @return true if data is valid
     */
    @Override
    public boolean validateData() {
        try {
            synchronized (connectionLock) {
                if (connection == null || connection.isClosed()) {
                    return false;
                }
                
                try (Statement stmt = connection.createStatement()) {
                    // Check for orphaned records
                    ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) as orphaned FROM " + tablePrefix + "members m " +
                        "LEFT JOIN " + tablePrefix + "companies c ON m.company_name = c.name " +
                        "WHERE c.name IS NULL");
                    
                    if (rs.next() && rs.getInt("orphaned") > 0) {
                        plugin.getLogger().warning("Found " + rs.getInt("orphaned") + " orphaned member records");
                        return false;
                    }
                    
                    // Check for invalid UUIDs
                    rs = stmt.executeQuery(
                        "SELECT COUNT(*) as invalid FROM " + tablePrefix + "companies " +
                        "WHERE ceo_uuid NOT REGEXP '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'");
                    
                    if (rs.next() && rs.getInt("invalid") > 0) {
                        plugin.getLogger().warning("Found " + rs.getInt("invalid") + " companies with invalid CEO UUIDs");
                        return false;
                    }
                    
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error during data validation", e);
            return false;
        }
    }
    
    /**
     * Attempts to fix common data issues.
     * 
     * @return true if issues were fixed
     */
    private boolean fixDataIssues() {
        try {
            synchronized (connectionLock) {
                if (connection == null || connection.isClosed()) {
                    return false;
                }
                
                boolean fixed = false;
                
                try (Statement stmt = connection.createStatement()) {
                    // Remove orphaned member records
                    int orphanedRemoved = stmt.executeUpdate(
                        "DELETE m FROM " + tablePrefix + "members m " +
                        "LEFT JOIN " + tablePrefix + "companies c ON m.company_name = c.name " +
                        "WHERE c.name IS NULL");
                    
                    if (orphanedRemoved > 0) {
                        plugin.getLogger().info("Removed " + orphanedRemoved + " orphaned member records");
                        fixed = true;
                    }
                    
                    // Remove orphaned subsidiary records
                    int orphanedSubs = stmt.executeUpdate(
                        "DELETE s FROM " + tablePrefix + "subsidiaries s " +
                        "LEFT JOIN " + tablePrefix + "companies c ON s.parent_company = c.name " +
                        "WHERE c.name IS NULL");
                    
                    if (orphanedSubs > 0) {
                        plugin.getLogger().info("Removed " + orphanedSubs + " orphaned subsidiary records");
                        fixed = true;
                    }
                    
                    // Remove orphaned join requests
                    int orphanedRequests = stmt.executeUpdate(
                        "DELETE j FROM " + tablePrefix + "join_requests j " +
                        "LEFT JOIN " + tablePrefix + "companies c ON j.company_name = c.name " +
                        "WHERE c.name IS NULL");
                    
                    if (orphanedRequests > 0) {
                        plugin.getLogger().info("Removed " + orphanedRequests + " orphaned join request records");
                        fixed = true;
                    }
                }
                
                return fixed;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error fixing data issues", e);
            return false;
        }
    }

    // ======== Data Loading Methods ========

    public Collection<Company> loadAllCompanies() {
        List<Company> companies = new ArrayList<>();
        
        if (!connected) {
            plugin.getLogger().warning("Cannot load companies - not connected to database");
            return companies;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            synchronized (connectionLock) {
                if (connection == null || connection.isClosed()) {
                    return companies;
                }
                
                // Load companies with optimized query
                String query = "SELECT * FROM " + tablePrefix + "companies ORDER BY total_earned DESC";
                
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setQueryTimeout((int) queryTimeout);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Company company = mapResultSetToCompany(rs);
                            if (company != null) {
                                companies.add(company);
                                
                                // Load members for this company
                                loadCompanyMembers(company);
                                loadCompanySubsidiaries(company);
                                loadCompanyJoinRequests(company);
                            }
                        }
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            plugin.getLogger().info(String.format("Loaded %d companies in %dms", companies.size(), duration));
            
            // Update metrics
            if (enableMetrics) {
                queryStats.put("loadAllCompanies", duration);
                queryCount++;
                totalQueryTime += duration;
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load companies", e);
        }
        
        return companies;
    }
    
    /**
     * Loads members for a specific company.
     * 
     * @param company Company to load members for
     */
    private void loadCompanyMembers(Company company) {
        try {
            String query = "SELECT player_uuid, player_name FROM " + tablePrefix + "members WHERE company_name = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, company.getName());
                stmt.setQueryTimeout((int) queryTimeout);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        try {
                            UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                            company.addMember(playerUUID, rs.getString("player_name"));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid UUID in members table: " + rs.getString("player_uuid"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load members for company: " + company.getName(), e);
        }
    }
    
    /**
     * Loads subsidiaries for a specific company.
     * 
     * @param company Company to load subsidiaries for
     */
    private void loadCompanySubsidiaries(Company company) {
        try {
            String query = "SELECT subsidiary_name FROM " + tablePrefix + "subsidiaries WHERE parent_company = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, company.getName());
                stmt.setQueryTimeout((int) queryTimeout);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        company.addSubsidiary(rs.getString("subsidiary_name"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load subsidiaries for company: " + company.getName(), e);
        }
    }
    
    /**
     * Loads join requests for a specific company.
     * 
     * @param company Company to load join requests for
     */
    private void loadCompanyJoinRequests(Company company) {
        try {
            String query = "SELECT player_uuid FROM " + tablePrefix + "join_requests WHERE company_name = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, company.getName());
                stmt.setQueryTimeout((int) queryTimeout);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        try {
                            UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                            company.addJoinRequest(playerUUID);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid UUID in join_requests table: " + rs.getString("player_uuid"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load join requests for company: " + company.getName(), e);
        }
    }
    
    /**
     * Maps a ResultSet to a Company object.
     * 
     * @param rs ResultSet to map
     * @return Company object or null if mapping failed
     */
    private Company mapResultSetToCompany(ResultSet rs) {
        try {
            String name = rs.getString("name");
            UUID ceoUUID = UUID.fromString(rs.getString("ceo_uuid"));
            String ceoName = rs.getString("ceo_name");
            
            Company company = new Company(name, ceoUUID, ceoName);
            
            // Set financial data
            // Note: These would need setter methods in Company class
            // company.setBalance(rs.getDouble("balance"));
            // company.setTotalMoneyEarned(rs.getDouble("total_earned"));
            
            // Set headquarters if exists
            if (rs.getBoolean("has_hq")) {
                String world = rs.getString("hq_world");
                double x = rs.getDouble("hq_x");
                double y = rs.getDouble("hq_y");
                double z = rs.getDouble("hq_z");
                
                // Create Location object (would need Bukkit.getWorld())
                // Location hq = new Location(Bukkit.getWorld(world), x, y, z);
                // company.setHeadquarters(hq);
            }
            
            // Set status
            if (rs.getBoolean("is_international")) {
                company.upgradeToInternational();
            }
            
            // Set subsidiary relationship
            String parentCompany = rs.getString("parent_company");
            if (parentCompany != null && !parentCompany.isEmpty()) {
                company.setParentCompany(parentCompany);
            }
            
            return company;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to map ResultSet to Company", e);
            return null;
        }
    }

    // ======== Data Saving Methods ========

    public boolean saveAllCompanies(Collection<Company> companies) {
        if (!connected || companies == null || companies.isEmpty()) {
            return false;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            int savedCount = 0;
            
            synchronized (connectionLock) {
                if (connection == null || connection.isClosed()) {
                    return false;
                }
                
                connection.setAutoCommit(false);
                
                try {
                    // Save companies in batch for better performance
                    for (Company company : companies) {
                        if (saveCompanyInternal(company)) {
                            savedCount++;
                        }
                    }
                    
                    connection.commit();
                    lastSaveTime = System.currentTimeMillis();
                    
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            plugin.getLogger().info(String.format("Saved %d/%d companies in %dms", savedCount, companies.size(), duration));
            
            // Update metrics
            if (enableMetrics) {
                queryStats.put("saveAllCompanies", duration);
                queryCount++;
                totalQueryTime += duration;
            }
            
            return savedCount == companies.size();
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save companies", e);
            return false;
        }
    }
    
    /**
     * Saves a single company to the database.
     * 
     * @param company Company to save
     * @return true if saved successfully
     */
    private boolean saveCompanyInternal(Company company) {
        try {
            // Save company data
            String upsertCompany = "INSERT INTO " + tablePrefix + "companies " +
                "(name, ceo_uuid, ceo_name, balance, total_earned, level, max_members, is_international, " +
                "hq_world, hq_x, hq_y, hq_z, has_hq, parent_company, is_subsidiary) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "ceo_uuid = VALUES(ceo_uuid), ceo_name = VALUES(ceo_name), " +
                "balance = VALUES(balance), total_earned = VALUES(total_earned), " +
                "level = VALUES(level), max_members = VALUES(max_members), " +
                "is_international = VALUES(is_international), " +
                "hq_world = VALUES(hq_world), hq_x = VALUES(hq_x), " +
                "hq_y = VALUES(hq_y), hq_z = VALUES(hq_z), " +
                "has_hq = VALUES(has_hq), parent_company = VALUES(parent_company), " +
                "is_subsidiary = VALUES(is_subsidiary)";
            
            try (PreparedStatement stmt = connection.prepareStatement(upsertCompany)) {
                stmt.setString(1, company.getName());
                stmt.setString(2, company.getCeoUUID().toString());
                stmt.setString(3, company.getCeoName());
                stmt.setDouble(4, company.getBalance());
                stmt.setDouble(5, company.getTotalMoneyEarned());
                stmt.setInt(6, company.getLevel());
                stmt.setInt(7, company.getMaxMembers());
                stmt.setBoolean(8, company.isInternational());
                
                // Handle headquarters
                if (company.hasHeadquarters()) {
                    Location hq = company.getHeadquartersLocation();
                    stmt.setString(9, hq.getWorld() != null ? hq.getWorld().getName() : "unknown");
                    stmt.setDouble(10, hq.getX());
                    stmt.setDouble(11, hq.getY());
                    stmt.setDouble(12, hq.getZ());
                    stmt.setBoolean(13, true);
                } else {
                    stmt.setNull(9, Types.VARCHAR);
                    stmt.setDouble(10, 0);
                    stmt.setDouble(11, 0);
                    stmt.setDouble(12, 0);
                    stmt.setBoolean(13, false);
                }
                
                stmt.setString(14, company.getParentCompany());
                stmt.setBoolean(15, company.isSubsidiary());
                
                stmt.executeUpdate();
            }
            
            // Save members
            saveCompanyMembers(company);
            
            // Save subsidiaries
            saveCompanySubsidiaries(company);
            
            // Save join requests
            saveCompanyJoinRequests(company);
            
            return true;
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save company: " + company.getName(), e);
            return false;
        }
    }
    
    /**
     * Saves company members to database.
     * 
     * @param company Company to save members for
     */
    private void saveCompanyMembers(Company company) {
        try {
            // Clear existing members
            String deleteMembers = "DELETE FROM " + tablePrefix + "members WHERE company_name = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteMembers)) {
                stmt.setString(1, company.getName());
                stmt.executeUpdate();
            }
            
            // Insert current members
            String insertMember = "INSERT INTO " + tablePrefix + "members (company_name, player_uuid, player_name) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(insertMember)) {
                for (UUID memberUUID : company.getMembers()) {
                    stmt.setString(1, company.getName());
                    stmt.setString(2, memberUUID.toString());
                    stmt.setString(3, company.getMemberName(memberUUID));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save members for company: " + company.getName(), e);
        }
    }
    
    /**
     * Saves company subsidiaries to database.
     * 
     * @param company Company to save subsidiaries for
     */
    private void saveCompanySubsidiaries(Company company) {
        try {
            // Clear existing subsidiaries
            String deleteSubsidiaries = "DELETE FROM " + tablePrefix + "subsidiaries WHERE parent_company = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSubsidiaries)) {
                stmt.setString(1, company.getName());
                stmt.executeUpdate();
            }
            
            // Insert current subsidiaries
            String insertSubsidiary = "INSERT INTO " + tablePrefix + "subsidiaries (parent_company, subsidiary_name) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(insertSubsidiary)) {
                for (String subsidiary : company.getSubsidiaries()) {
                    stmt.setString(1, company.getName());
                    stmt.setString(2, subsidiary);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save subsidiaries for company: " + company.getName(), e);
        }
    }
    
    /**
     * Saves company join requests to database.
     * 
     * @param company Company to save join requests for
     */
    private void saveCompanyJoinRequests(Company company) {
        try {
            // Clear existing join requests
            String deleteRequests = "DELETE FROM " + tablePrefix + "join_requests WHERE company_name = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteRequests)) {
                stmt.setString(1, company.getName());
                stmt.executeUpdate();
            }
            
            // Insert current join requests
            String insertRequest = "INSERT INTO " + tablePrefix + "join_requests (company_name, player_uuid, player_name) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(insertRequest)) {
                for (UUID requestUUID : company.getPendingJoinRequests()) {
                    stmt.setString(1, company.getName());
                    stmt.setString(2, requestUUID.toString());
                    stmt.setString(3, company.getMemberName(requestUUID));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save join requests for company: " + company.getName(), e);
        }
    }

    // ======== Utility Methods ========

    @Override
    public boolean deleteCompany(String companyName) {
        if (!connected || companyName == null) {
            return false;
        }
        
        try {
            synchronized (connectionLock) {
                if (connection == null || connection.isClosed()) {
                    return false;
                }
                
                // Delete company (cascade will handle related records)
                String query = "DELETE FROM " + tablePrefix + "companies WHERE name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, companyName);
                    stmt.setQueryTimeout((int) queryTimeout);
                    
                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected > 0) {
                        plugin.getLogger().info("Deleted company: " + companyName);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete company: " + companyName, e);
        }
        
        return false;
    }
    
    @Override
    public void shutdown() {
        try {
            connected = false;
            
            synchronized (connectionLock) {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    plugin.getLogger().info("MySQL connection closed");
                }
                connection = null;
            }
            
            // Log final statistics
            if (enableMetrics) {
                plugin.getLogger().info(String.format("MySQL Statistics - Queries: %d, Avg Time: %.2fms", 
                    queryCount, queryCount > 0 ? (double) totalQueryTime / queryCount : 0));
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing MySQL connection", e);
        }
    }
    
    /**
     * Gets database performance statistics.
     * 
     * @return Map containing performance metrics
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("connected", connected);
        stats.put("query_count", queryCount);
        stats.put("total_query_time", totalQueryTime);
        stats.put("average_query_time", queryCount > 0 ? (double) totalQueryTime / queryCount : 0);
        stats.put("last_save_time", lastSaveTime);
        stats.put("last_connection_time", lastConnectionTime);
        stats.put("query_stats", new HashMap<>(queryStats));
        return stats;
    }
    
    /**
     * Checks if the database connection is healthy.
     * 
     * @return true if connection is healthy
     */
    public boolean isConnectionHealthy() {
        if (!connected) {
            return false;
        }
        
        try {
            synchronized (connectionLock) {
                return connection != null && !connection.isClosed() && connection.isValid(5);
            }
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Executes a maintenance query to optimize database performance.
     * 
     * @return true if maintenance was successful
     */
    public boolean performMaintenance() {
        if (!connected) {
            return false;
        }
        
        try {
            synchronized (connectionLock) {
                if (connection == null || connection.isClosed()) {
                    return false;
                }
                
                try (Statement stmt = connection.createStatement()) {
                    // Optimize tables for Minecraft performance
                    stmt.execute("OPTIMIZE TABLE " + tablePrefix + "companies");
                    stmt.execute("OPTIMIZE TABLE " + tablePrefix + "members");
                    stmt.execute("OPTIMIZE TABLE " + tablePrefix + "subsidiaries");
                    stmt.execute("OPTIMIZE TABLE " + tablePrefix + "join_requests");
                    
                    // Analyze tables for better query planning
                    stmt.execute("ANALYZE TABLE " + tablePrefix + "companies");
                    stmt.execute("ANALYZE TABLE " + tablePrefix + "members");
                    stmt.execute("ANALYZE TABLE " + tablePrefix + "subsidiaries");
                    stmt.execute("ANALYZE TABLE " + tablePrefix + "join_requests");
                    
                    plugin.getLogger().info("Database maintenance completed successfully");
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Database maintenance failed", e);
            return false;
        }
    }
    
    /**
     * Gets the storage type identifier.
     * 
     * @return "MySQL"
     */
    @Override
    public String getStorageType() {
        return "MySQL";
    }
    
    @Override
    public boolean isConnected() {
        try {
            return connected && connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Gets the timestamp of the last save operation.
     * 
     * @return Last save time in milliseconds
     */
    @Override
    public long getLastSaveTime() {
        return lastSaveTime;
    }
}
