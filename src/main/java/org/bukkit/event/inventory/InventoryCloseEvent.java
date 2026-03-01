package org.bukkit.event.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;

public class InventoryCloseEvent extends Event {
    private Player player;
    private Inventory inventory;
    
    public Player getPlayer() { return player; }
    public Inventory getInventory() { return inventory; }
}
