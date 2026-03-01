package org.bukkit.inventory.meta;

import java.util.List;

public interface ItemMeta {
    boolean hasDisplayName();
    String getDisplayName();
    void setDisplayName(String name);
    boolean hasLore();
    List<String> getLore();
    void setLore(List<String> lore);
}
