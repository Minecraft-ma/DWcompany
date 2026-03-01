package org.bukkit.inventory;

import org.bukkit.Material;

public class ItemStack {
    private Material material;
    private int amount;
    
    public ItemStack(Material material) {
        this.material = material;
        this.amount = 1;
    }
    
    public ItemStack(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }
    
    public Material getType() { return material; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    
    public org.bukkit.inventory.meta.ItemMeta getItemMeta() {
        return null; // Stub implementation
    }
    
    public void setItemMeta(org.bukkit.inventory.meta.ItemMeta meta) {
        // Stub implementation
    }
}
