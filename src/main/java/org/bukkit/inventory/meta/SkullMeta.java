package org.bukkit.inventory.meta;

import org.bukkit.inventory.ItemMeta;

public interface SkullMeta extends ItemMeta {
    void setOwningPlayer(org.bukkit.OfflinePlayer player);
    org.bukkit.OfflinePlayer getOwningPlayer();
}
