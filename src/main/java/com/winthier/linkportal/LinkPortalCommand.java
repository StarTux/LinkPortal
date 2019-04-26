package com.winthier.linkportal;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class LinkPortalCommand implements CommandExecutor {
    private final LinkPortalPlugin plugin;

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        if (args.length == 1 && args[0].equals("reload")) {
            if (!sender.hasPermission("linkportal.admin")) {
                return false;
            }
            plugin.getPortals().reload();
            plugin.loadConf();
            sender.sendMessage("Link portals and config reloaded.");
            return true;
        }
        return true;
    }
}
