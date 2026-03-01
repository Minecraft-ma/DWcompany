package org.bukkit.scheduler;

import org.bukkit.plugin.Plugin;

public abstract class BukkitRunnable implements Runnable {
    public abstract void run();
    
    public BukkitRunnable runTaskTimer(Plugin plugin, long delay, long period) {
        return this; // Stub implementation
    }
    
    public BukkitRunnable runTaskTimerAsynchronously(Plugin plugin, long delay, long period) {
        return this; // Stub implementation
    }
    
    public void cancel() {
        // Stub implementation
    }
}
