package com.winthier.linkportal;

import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

class LinkPortalListener implements Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() != TeleportCause.NETHER_PORTAL) return;
        final Player player = event.getPlayer();
        final Location loc = player.getLocation();
        Sign sign = Util.findPortalSignNear(loc);
        if (sign == null) return;
        Portal portal = LinkPortalPlugin.instance.portals.portalWithSign(sign);
        if (portal == null) return;
        event.setCancelled(true);
        if (Util.entityWalkThroughPortal(player, portal)) {
            final String ownerName;
            if (portal.getOwnerUuid() == player.getUniqueId()) {
                ownerName = "your";
            } else {
                if (portal.getOwnerName().endsWith("s") ||
                    portal.getOwnerName().endsWith("x") ||
                    portal.getOwnerName().endsWith("z")) {
                    ownerName = portal.getOwnerName() + "'";
                } else {
                    ownerName = portal.getOwnerName() + "'s";
                }
            }
            Util.msg(player, "&3&lLinkPortal&r You enter &a%s&r Link Portal &a%s", ownerName, portal.getRingName());
        } else {
            Util.msg(player, "&4&lLinkPortal&r &cThis Link Portal has no destination");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onEntityPortal(EntityPortalEvent event) {
        final Entity entity = event.getEntity();
        final Location loc = entity.getLocation();
        Sign sign = Util.findPortalSignNear(loc);
        if (sign == null) return;
        Portal portal = LinkPortalPlugin.instance.portals.portalWithSign(sign);
        if (portal == null) return;
        event.setCancelled(true);
        Util.entityWalkThroughPortal(entity, portal);
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
        if (!event.getLine(0).equalsIgnoreCase("[link]")) return;
        final Player player = event.getPlayer();
        if (!player.hasPermission("linkportal.create")) {
            Util.msg(player, "&4&lLinkPortal&r &cYou don't have permission!");
            event.setCancelled(true);
            return;
        }
        Set<Block> blocks = Util.findPortalBlocksNearSign(event.getBlock(), Util.PortalBlockType.PORTAL);
        if (blocks == null || blocks.isEmpty()) {
            Util.msg(player, "&4&lLinkPortal&r &cYour sign is not attached to a nether portal!");
            event.setCancelled(true);
            return;
        }
        Sign sign = Util.findPortalSignNear(blocks);
        if (sign != null) {
            Util.msg(player, "&4&lLinkPortal&r &cThis nether portal already has a link sign attached!");
            event.setCancelled(true);
            return;
        }
        Portal portal = Portal.of(player, event.getBlock(), event.getLines());
        LinkPortalPlugin.instance.portals.addPortal(portal);
        LinkPortalPlugin.instance.portals.savePortals();
        Util.msg(player, "&3&lLinkPortal&r You created a Link Portal");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        final Block block = event.getBlock();
        switch (block.getType()) {
        case SIGN:
        case SIGN_POST:
        case WALL_SIGN:
            break;
        default: return;
        }
        BlockState state = block.getState();
        if (!(state instanceof Sign)) return;
        Sign sign = (Sign)state;
        if (!sign.getLine(0).equalsIgnoreCase("[link]")) return;
        Portal portal = LinkPortalPlugin.instance.portals.portalWithSign(sign);
        if (portal == null) return;
        LinkPortalPlugin.instance.portals.removePortal(portal);
        LinkPortalPlugin.instance.portals.savePortals();
        Util.msg(event.getPlayer(), "&3&lLinkPortal&r Link Portal destroyed");
    }
}
