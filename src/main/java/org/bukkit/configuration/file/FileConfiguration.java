package org.bukkit.configuration.file;

import org.bukkit.configuration.ConfigurationSection;

public interface FileConfiguration extends ConfigurationSection {
    boolean getBoolean(String path, boolean defaultValue);
    long getLong(String path, long defaultValue);
}
