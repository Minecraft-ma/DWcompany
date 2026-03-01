package org.bukkit.plugin;

import org.bukkit.Server;
import java.util.logging.Logger;

public interface Plugin {
    void onEnable();
    void onDisable();
    Logger getLogger();
    Server getServer();
}
