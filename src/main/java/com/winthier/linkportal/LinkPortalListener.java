package com.winthier.linkportal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;

class LinkPortalListener implements Listener {
    final Map<UUID, Long> cooldowns = new HashMap<>();
    final Set<UUID> justTeleportedToPressurePlate = new HashSet<>();

    private boolean isOnCooldown(UUID uuid) {
        Long cooldown = cooldowns.get(uuid);
        long now = System.currentTimeMillis();
        if (cooldown == null) {
            return false;
        } else if (now - cooldown > 2000) {
            return false;
        } else {
            return true;
        }
    }
    
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
        if (isOnCooldown(player.getUniqueId())) return; // Do this late because we have to cancel the event if it's a Link Portal!
        if (portal.playerWalkThroughPortal(player)) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
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
        portal.entityWalkThroughPortal(entity);
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
        final String firstLine = event.getLine(0);
        if (Util.isLinkTag(firstLine)) {
            final Player player = event.getPlayer();
            if (!player.hasPermission("linkportal.create")) {
                Util.msg(player, "&4&lLinkPortal&r &cYou don't have permission!");
                event.setCancelled(true);
                return;
            }
            Portal portal = Portal.of(player, event.getBlock(), event.getLines());
            LinkPortalPlugin.instance.portals.addPortal(portal);
            LinkPortalPlugin.instance.portals.savePortals();
            String ringName = portal.getRingName();
            List<Portal> ring = LinkPortalPlugin.instance.portals.ringOfPortal(portal);
            String portalWord = ring.size() == 1 ? "Portal" : "Portals";
            if (ringName == null || ringName.isEmpty()) {
                Util.msg(player, "&3&lLinkPortal&r You created a Link Portal (Ring: %d %s)", ring.size(), portalWord);
            } else {
                Util.msg(player, "&3&lLinkPortal&r You created a Link Portal (\"%s\": %d %s)", ringName, ring.size(), portalWord);
            }
            event.setLine(0, Util.format("[&5&lLink&r]"));
            player.sendTitle("", Util.format("&aLink Portal created"));
        } else if (firstLine.equalsIgnoreCase("[portal]")) {
            event.setCancelled(true);
            if (!event.getPlayer().hasPermission("linkportal.portal")) return;
            final Block block = event.getBlock();
            BlockFace facing = Util.getSignFacing(block);
            if (facing == null) return;
            final Util.AxisAlignment alignment;
            switch (facing) {
            case EAST:
            case WEST:
            case EAST_NORTH_EAST:
            case EAST_SOUTH_EAST:
            case WEST_NORTH_WEST:
            case WEST_SOUTH_WEST:
                alignment = Util.AxisAlignment.Z;
                break;
            case NORTH:
            case SOUTH:
            case NORTH_NORTH_EAST:
            case NORTH_NORTH_WEST:
            case SOUTH_SOUTH_EAST:
            case SOUTH_SOUTH_WEST:
                alignment = Util.AxisAlignment.X;
                break;
            default: return;
            }
            Util.createNetherPortal(block, alignment);
        }
    }

    private void checkRemovedBlock(final Block block, boolean later) {
        if (!Util.isLinkSign(block)) return;
        Sign sign = (Sign)block.getState();
        final Portal portal = LinkPortalPlugin.instance.portals.portalWithSign(sign);
        if (portal == null) return;
        if (later) {
            new BukkitRunnable() {
                @Override public void run() {
                    if (block.getType() == Material.AIR) {
                        removeBrokenPortal(portal);
                    }
                }
            }.runTask(LinkPortalPlugin.instance);
        } else {
            removeBrokenPortal(portal);
        }
    }

    private void removeBrokenPortal(Portal portal) {
        LinkPortalPlugin.instance.portals.removePortal(portal);
        LinkPortalPlugin.instance.portals.savePortals();
        if (portal.getOwnerUuid() == null) return;
        Player player = Bukkit.getServer().getPlayer(portal.getOwnerUuid());
        if (player == null) return;
        String ringName = portal.getRingName();
        if (ringName == null || ringName.isEmpty()) {
            Util.msg(player, "&3&lLinkPortal&r Link Portal destroyed");
        } else {
            Util.msg(player, "&3&lLinkPortal&r Link Portal \"%s\" destroyed", ringName);
        }
        player.sendTitle("", Util.format("&cLink Portal destroyed"));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        checkRemovedBlock(event.getBlock(), false);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block: event.blockList()) {
            checkRemovedBlock(block, false);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block: event.blockList()) {
            checkRemovedBlock(block, false);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        checkRemovedBlock(event.getBlock(), true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Action action = event.getAction();
        final Block block = event.getClickedBlock();
        final Player player = event.getPlayer();
        Block attachedBlock = null;
        if (action == Action.RIGHT_CLICK_BLOCK) {
            switch (block.getType()) {
            case STONE_BUTTON:
            case WOOD_BUTTON:
                break;
            default:
                return;
            }
            if (!player.getLocation().getBlock().equals(block.getRelative(0, -1, 0))) return;
            attachedBlock = block.getRelative(0, 1, 0);
        } else if (action == Action.PHYSICAL) {
            switch (block.getType()) {
            case STONE_PLATE:
            case GOLD_PLATE:
            case IRON_PLATE:
            case WOOD_PLATE:
                if (justTeleportedToPressurePlate.remove(player.getUniqueId())) return;
                break;
            default:
                return;
            }
            if (isOnCooldown(player.getUniqueId())) return;
            attachedBlock = block.getRelative(0, 2, 0);
        } else {
            return;
        }
        Sign sign = Util.findAttachedLinkSign(attachedBlock);
        if (sign == null) return;
        Portal portal = LinkPortalPlugin.instance.portals.portalWithSign(sign);
        if (portal == null) return;
        if (portal.playerWalkThroughPortal(player)) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            event.setCancelled(true);
        }
    }

    // Deny creature spawning from custom link portals.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NETHER_PORTAL) return;
        if (event.getEntity().getLocation().getBlock().getRelative(0, -1, 0).getType() == Material.OBSIDIAN) return;
        event.setCancelled(true);
    }
}
