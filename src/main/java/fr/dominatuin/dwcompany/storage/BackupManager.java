package fr.dominatuin.dwcompany.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

/**
 * Manages automatic backups of company data.
 * Creates timestamped backup files in a backup folder.
 */
public class BackupManager {

    private final JavaPlugin plugin;
    private final File backupFolder;
    private final File dataFolder;
    private final SimpleDateFormat dateFormat;

    private boolean enabled;
    private int maxBackups;

    /**
     * Creates a new BackupManager instance.
     *
     * @param plugin         The plugin instance
     * @param enabled        Whether backups are enabled
     * @param maxBackups     Maximum number of backups to keep
     */
    public BackupManager(JavaPlugin plugin, boolean enabled, int maxBackups) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.backupFolder = new File(dataFolder, "backups");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        this.enabled = enabled;
        this.maxBackups = maxBackups;

        if (enabled) {
            createBackupFolder();
        }
    }

    /**
     * Creates the backup folder if it doesn't exist.
     */
    private void createBackupFolder() {
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
            plugin.getLogger().info(String.format("Created backup folder: %s", backupFolder.getPath()));
        }
    }

    /**
     * Creates a backup of the current data.
     *
     * @return true if backup was successful
     */
    public boolean createBackup() {
        if (!enabled) {
            return false;
        }

        try {
            String timestamp = dateFormat.format(new Date());
            File backupDir = new File(backupFolder, timestamp);
            backupDir.mkdirs();

            // Backup companies.yml
            File companiesFile = new File(dataFolder, "companies.yml");
            if (companiesFile.exists()) {
                Files.copy(companiesFile.toPath(),
                        new File(backupDir, "companies.yml").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            // Backup config.yml
            File configFile = new File(dataFolder, "config.yml");
            if (configFile.exists()) {
                Files.copy(configFile.toPath(),
                        new File(backupDir, "config.yml").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            // Backup messages.yml
            File messagesFile = new File(dataFolder, "messages.yml");
            if (messagesFile.exists()) {
                Files.copy(messagesFile.toPath(),
                        new File(backupDir, "messages.yml").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            plugin.getLogger().info(String.format("Created backup: %s", timestamp));

            // Clean old backups
            cleanOldBackups();

            return true;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create backup", e);
            return false;
        }
    }

    /**
     * Removes old backups exceeding the maximum limit.
     */
    private void cleanOldBackups() {
        File[] backups = backupFolder.listFiles(File::isDirectory);
        if (backups == null || backups.length <= maxBackups) {
            return;
        }

        // Sort by last modified (oldest first)
        java.util.Arrays.sort(backups, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));

        // Delete oldest backups
        int toDelete = backups.length - maxBackups;
        for (int i = 0; i < toDelete; i++) {
            deleteDirectory(backups[i]);
            plugin.getLogger().info(String.format("Deleted old backup: %s", backups[i].getName()));
        }
    }

    /**
     * Recursively deletes a directory.
     *
     * @param directory Directory to delete
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * Restores data from a backup.
     *
     * @param backupName Name of the backup folder
     * @return true if restored successfully
     */
    public boolean restoreBackup(String backupName) {
        File backupDir = new File(backupFolder, backupName);
        if (!backupDir.exists()) {
            plugin.getLogger().warning(String.format("Backup not found: %s", backupName));
            return false;
        }

        try {
            // Restore companies.yml
            File backupCompanies = new File(backupDir, "companies.yml");
            if (backupCompanies.exists()) {
                Files.copy(backupCompanies.toPath(),
                        new File(dataFolder, "companies.yml").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            plugin.getLogger().info(String.format("Restored backup: %s", backupName));
            return true;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to restore backup", e);
            return false;
        }
    }

    /**
     * Gets a list of available backups.
     *
     * @return Array of backup folder names
     */
    public String[] getAvailableBackups() {
        File[] backups = backupFolder.listFiles(File::isDirectory);
        if (backups == null) {
            return new String[0];
        }

        String[] names = new String[backups.length];
        for (int i = 0; i < backups.length; i++) {
            names[i] = backups[i].getName();
        }

        // Sort newest first
        java.util.Arrays.sort(names, java.util.Collections.reverseOrder());
        return names;
    }

    /**
     * Checks if backups are enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether backups are enabled.
     *
     * @param enabled Enabled state
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            createBackupFolder();
        }
    }

    /**
     * Gets the maximum number of backups.
     *
     * @return Maximum backups
     */
    public int getMaxBackups() {
        return maxBackups;
    }

    /**
     * Sets the maximum number of backups.
     *
     * @param maxBackups Maximum backups
     */
    public void setMaxBackups(int maxBackups) {
        this.maxBackups = maxBackups;
    }
}
