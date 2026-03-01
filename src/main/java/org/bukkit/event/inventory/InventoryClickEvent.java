package org.bukkit.event.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryClickEvent extends Event {
    private Player whoClicked;
    private Inventory inventory;
    private ItemStack currentItem;
    private int slot;
    private InventoryView view;
    
    public Player getWhoClicked() { return whoClicked; }
    public Inventory getInventory() { return inventory; }
    public ItemStack getCurrentItem() { return currentItem; }
    public int getSlot() { return slot; }
    public void setCancelled(boolean cancel) { }
    public boolean isCancelled() { return false; }
    public InventoryView getView() { return view; }
    
    public static class InventoryView {
        private String title;
        public String getTitle() { return title; }
    }
}
