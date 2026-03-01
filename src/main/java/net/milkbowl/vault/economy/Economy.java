package net.milkbowl.vault.economy;

import org.bukkit.OfflinePlayer;

public interface Economy {
    boolean hasPlayerAccount(String playerName);
    double getBalance(String playerName);
    boolean has(String playerName, double amount);
    boolean withdrawPlayer(String playerName, double amount);
    boolean depositPlayer(String playerName, double amount);
    boolean hasPlayer(OfflinePlayer player);
    double getBalance(OfflinePlayer player);
    boolean has(OfflinePlayer player, double amount);
    boolean withdrawPlayer(OfflinePlayer player, double amount);
    boolean depositPlayer(OfflinePlayer player, double amount);
}
