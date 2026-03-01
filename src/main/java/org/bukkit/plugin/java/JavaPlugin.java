package org.bukkit.plugin.java;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescription;
import java.util.logging.Logger;

public abstract class JavaPlugin implements Plugin {
    private Logger logger;
    private Server server;
    
    public Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(getClass().getName());
        }
        return logger;
    }
    
    @Override
    public Server getServer() {
        return server;
    }
    
    public Command getCommand(String name) {
        return null; // Stub implementation
    }
    
    public PluginDescription getDescription() {
        return new PluginDescription() {
            @Override
            public String getVersion() {
                return "1.0.0"; // Stub implementation
            }
        };
    }
}
