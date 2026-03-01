package org.bukkit.inventory;

import org.bukkit.inventory.ItemStack;

public interface Inventory {
    void setItem(int slot, ItemStack item);
    ItemStack getItem(int slot);
    String getTitle();
    int getSize();
}
