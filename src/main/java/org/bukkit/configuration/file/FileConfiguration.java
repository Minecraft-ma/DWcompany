package org.bukkit.configuration.file;

import org.bukkit.configuration.ConfigurationSection;
import java.io.File;

public interface FileConfiguration extends ConfigurationSection {
    boolean getBoolean(String path, boolean defaultValue);
    long getLong(String path, long defaultValue);
    void save(File file) throws Exception;
    void set(String path, Object value);
}
