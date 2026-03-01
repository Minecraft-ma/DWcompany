package org.bukkit.configuration;

public interface ConfigurationSection {
    String getString(String path);
    String getString(String path, String defaultValue);
    int getInt(String path, int defaultValue);
    double getDouble(String path, double defaultValue);
    boolean getBoolean(String path, boolean defaultValue);
}
