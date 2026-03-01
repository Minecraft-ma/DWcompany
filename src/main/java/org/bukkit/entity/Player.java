package org.bukkit.entity;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.inventory.Inventory;
import org.bukkit.OfflinePlayer;
import java.util.UUID;

public interface Player extends Entity, OfflinePlayer {
    String getName();
    UUID getUniqueId();
    Location getLocation();
    boolean isOnline();
    boolean hasPermission(String permission);
    void sendMessage(String message);
    void openInventory(Inventory inventory);
    void closeInventory();
    void playSound(Location location, Sound sound, float volume, float pitch);
    void performCommand(String command);
}
