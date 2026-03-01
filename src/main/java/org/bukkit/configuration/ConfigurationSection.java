package org.bukkit.configuration;

import java.util.List;
import java.util.Set;

public interface ConfigurationSection {
    String getString(String path);
    String getString(String path, String defaultValue);
    int getInt(String path, int defaultValue);
    double getDouble(String path, double defaultValue);
    boolean getBoolean(String path, boolean defaultValue);
    List<String> getStringList(String path);
    ConfigurationSection getConfigurationSection(String path);
    Set<String> getKeys(boolean deep);
    void set(String path, Object value);
}
