package com.winthier.linkportal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class LinkPortalCommand implements TabExecutor {
    private final LinkPortalPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        Player player = sender instanceof Player ? (Player)sender : null;
        if (args.length == 1 && args[0].equals("reload")) {
            if (!sender.hasPermission("linkportal.admin")) {
                return false;
            }
            plugin.getPortals().reload();
            plugin.loadConf();
            sender.sendMessage("Link portals and config reloaded.");
            return true;
        } else if (args.length == 1 && args[0].equals("server")) {
            if (player == null) {
                sender.sendMessage("Player expected");
                return true;
            }
            if (!player.hasPermission("linkportal.admin")) {
                return false;
            }
            UUID uuid = player.getUniqueId();
            if (plugin.serverPortal.remove(uuid)) {
                player.sendMessage(ChatColor.YELLOW + "Server portal creation disabled.");
            } else {
                plugin.serverPortal.add(uuid);
                player.sendMessage(ChatColor.YELLOW + "Server portal creation enabled.");
            }
            return true;
        } else if (args.length == 1 && args[0].equals("list")) {
            if (player == null) return false;
            UUID uuid = player.getUniqueId();
            Map<String, Integer> map = new HashMap<>();
            for (Portal portal : plugin.getPortals().getPortals()) {
                if (!uuid.equals(portal.getOwnerUuid())) continue;
                String name = portal.getRingName();
                if (name == null) name = "";
                Integer o = map.get(name);
                int count = o != null ? o + 1 : 1;
                map.put(name, count);
            }
            if (map.isEmpty()) {
                player.sendMessage(ChatColor.RED + "You don't have any portals");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Listing " +
                                   (map.size() == 1
                                    ? "1 Portal Ring:"
                                    : map.size() + " Portal Rings:"));
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    String name = entry.getKey();
                    int count = entry.getValue();
                    player.sendMessage(ChatColor.AQUA + name
                                       + ChatColor.GRAY + ": "
                                       + (count == 1
                                          ? "1 Portal"
                                          : count + " Portals"));
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (args.length == 0) return null;
        String arg = args[args.length - 1];
        if (args.length == 1) {
            if (sender.hasPermission("linkportal.admin")) {
                return Stream.of("list", "reload", "server")
                    .filter(s -> s.startsWith(arg))
                    .collect(Collectors.toList());
            }                
            return Stream.of("list")
                .filter(s -> s.startsWith(arg))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
