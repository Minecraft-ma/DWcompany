package org.bukkit.command;

public interface Command {
    String getName();
    void setExecutor(CommandExecutor executor);
    void setTabCompleter(TabCompleter completer);
}
