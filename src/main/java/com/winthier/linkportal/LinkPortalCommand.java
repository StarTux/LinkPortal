package com.winthier.linkportal;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class LinkPortalCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        // if (args.length == 1 && args[0].equals("import")) {
        //     if (!sender.hasPermission("linkportal.admin")) {
        //         return false;
        //     }
        //     Legacy.importPortals();
        //     return true;
        // }
        if (args.length == 1 && args[0].equals("reload")) {
            if (!sender.hasPermission("linkportal.admin")) {
                return false;
            }
            LinkPortalPlugin.getInstance().getPortals().reload();
            sender.sendMessage("Link portals reloaded.");
            return true;
        }
        return true;
    }
}
