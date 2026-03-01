package org.bukkit.command;

import org.bukkit.command.CommandSender;

public interface CommandExecutor {
    boolean onCommand(CommandSender sender, Command command, String label, String[] args);
}
