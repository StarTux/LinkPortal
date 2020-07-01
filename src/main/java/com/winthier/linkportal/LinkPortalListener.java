package com.winthier.linkportal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
final class LinkPortalListener implements Listener {
    private final LinkPortalPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    final Set<UUID> justTeleportedToPressurePlate = new HashSet<>();
    private static final int COOLDOWN = 5;
    Map<UUID, Session> sessions = new HashMap<>();

    final class Session {
        int x;
        int y;
        int z;
    }

    private boolean isOnCooldown(UUID uuid) {
        Long cooldown = cooldowns.get(uuid);
        long now = System.currentTimeMillis();
        if (cooldown == null) return false;
        if (now - cooldown > COOLDOWN * 1000) return false;
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() != TeleportCause.NETHER_PORTAL) return;
        final Player player = event.getPlayer();
        Sign sign = Util.findPortalSignNearNetherPortal(player);
        if (sign == null) return;
        Portal portal = plugin.getPortals().portalWithSign(sign);
        if (portal == null) return;
        event.setCancelled(true);
        // Do this late because we have to cancel the event if it's a
        // Link Portal!
        if (isOnCooldown(player.getUniqueId())) return;
        boolean success = portal.playerWalkThroughPortal(player);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("" + event.getEventName() + ":"
                                    + " " + player.getName()
                                    + " walk through nether portal: "
                                    + portal.debugString()
                                    + " => " + success);
        }
        if (success) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            player.setPortalCooldown(COOLDOWN * 20);
        }
    }

    void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Session session = sessions.get(uuid);
            if (session == null) {
                session = new Session();
                sessions.put(uuid, session);
            }
            Location loc = player.getLocation();
            final int x = loc.getBlockX();
            final int y = loc.getBlockY();
            final int z = loc.getBlockZ();
            if (x != session.x || y != session.y || z != session.z) {
                session.x = x;
                session.y = y;
                session.z = z;
                Block block = loc.getBlock();
                if (block.getType() == Material.END_GATEWAY) {
                    onPlayerEndGateway(player, block);
                }
            }
        }
    }

    /**
     * Called by LinkPortalPlugin::tick.
     */
    public void onPlayerEndGateway(Player player, Block block) {
        Sign sign = Util.findPortalSignNear(block);
        if (sign == null) return;
        Portal portal = plugin.getPortals().portalWithSign(sign);
        if (portal == null) return;
        if (isOnCooldown(player.getUniqueId())) return;
        boolean success = portal.playerWalkThroughPortal(player);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("onPlayerEndGateway:"
                                    + " " + player.getName()
                                    + " walk through end gateway: "
                                    + portal.debugString()
                                    + " => " + success);
        }
        if (success) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            player.setPortalCooldown(COOLDOWN * 20);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        final Entity entity = event.getEntity();
        if (entity instanceof Player) return;
        if (entity.getPortalCooldown() > 0) return;
        Sign sign = Util.findPortalSignNearNetherPortal(entity);
        if (sign == null) {
            entity.setPortalCooldown(COOLDOWN * 20);
            return;
        }
        Portal portal = plugin.getPortals().portalWithSign(sign);
        if (portal == null) {
            entity.setPortalCooldown(COOLDOWN * 20);
            return;
        }
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("" + event.getEventName() + ":"
                                    + " " + entity.getType().name().toLowerCase()
                                    + " (" + entity.getUniqueId() + ")"
                                    + " walk through portal: "
                                    + portal.debugString()
                                    + ".");
        }
        boolean success = portal.entityWalkThroughPortal(entity);
        if (success) {
            entity.setPortalCooldown(COOLDOWN * 20);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onEntityPortal(EntityPortalEvent event) {
        final Entity entity = event.getEntity();
        Sign sign = Util.findPortalSignNearNetherPortal(entity);
        if (sign == null) return;
        Portal portal = plugin.getPortals().portalWithSign(sign);
        if (portal == null) return;
        event.setCancelled(true);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("" + event.getEventName() + ":"
                                    + " " + entity.getType().name().toLowerCase()
                                    + " (" + entity.getUniqueId() + ")"
                                    + " walk through portal: "
                                    + portal.debugString()
                                    + ".");
        }
        boolean success = portal.entityWalkThroughPortal(entity);
        if (success) {
            entity.setPortalCooldown(COOLDOWN * 20);
        }
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
            Portal portal;
            boolean adminPortal;
            if (plugin.serverPortal.remove(player.getUniqueId())) {
                adminPortal = true;
                portal = Portal.of(plugin, event.getBlock(), event.getLines());
            } else {
                adminPortal = false;
                portal = Portal.of(plugin, player, event.getBlock(), event.getLines());
            }
            plugin.getPortals().addPortal(portal);
            plugin.getPortals().savePortals();
            String ringName = portal.getRingName();
            List<Portal> ring = plugin.getPortals().ringOfPortal(portal);
            String portalWord = ring.size() == 1 ? "Portal" : "Portals";
            if (ringName == null || ringName.isEmpty()) {
                Util.msg(player, "&3&lLinkPortal&r You created a Link Portal (Ring: %d %s)",
                         ring.size(), portalWord);
            } else {
                Util.msg(player, "&3&lLinkPortal&r You created a Link Portal (\"%s\": %d %s)",
                         ringName, ring.size(), portalWord);
            }
            if (adminPortal) {
                Util.msg(player, "&eCreated as server portal."
                         + " Server portal creation now disabled.");
            }
            event.setLine(0, Util.format("[&5&lLink&r]"));
            player.sendTitle("", Util.format("&aLink Portal created"));
        } else if (firstLine.equalsIgnoreCase("[portal]")) {
            event.setCancelled(true);
            if (!event.getPlayer().hasPermission("linkportal.portal")) return;
            final Block block = event.getBlock();
            BlockFace facing = Util.getSignFacing(block);
            if (facing == null) return;
            final Axis axis;
            switch (facing) {
            case EAST:
            case WEST:
            case EAST_NORTH_EAST:
            case EAST_SOUTH_EAST:
            case WEST_NORTH_WEST:
            case WEST_SOUTH_WEST:
                axis = Axis.Z;
                break;
            case NORTH:
            case SOUTH:
            case NORTH_NORTH_EAST:
            case NORTH_NORTH_WEST:
            case SOUTH_SOUTH_EAST:
            case SOUTH_SOUTH_WEST:
                axis = Axis.X;
                break;
            default: return;
            }
            Util.createNetherPortal(block, axis);
        }
    }

    private void checkRemovedBlock(final Block block, boolean later) {
        if (!Util.isLinkSign(block)) return;
        Sign sign = (Sign) block.getState();
        final Portal portal = plugin.getPortals().portalWithSign(sign);
        if (portal == null) return;
        if (later) {
            new BukkitRunnable() {
                @Override public void run() {
                    if (block.isEmpty()) {
                        removeBrokenPortal(portal);
                    }
                }
            }.runTask(plugin);
        } else {
            removeBrokenPortal(portal);
        }
    }

    private void removeBrokenPortal(Portal portal) {
        plugin.getPortals().removePortal(portal);
        plugin.getPortals().savePortals();
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
            case ACACIA_BUTTON:
            case BIRCH_BUTTON:
            case DARK_OAK_BUTTON:
            case JUNGLE_BUTTON:
            case OAK_BUTTON:
            case SPRUCE_BUTTON:
            case STONE_BUTTON:
                break;
            default:
                if (Tag.BUTTONS.isTagged(block.getType())) break;
                return;
            }
            if (!player.getLocation().getBlock().equals(block.getRelative(0, -1, 0))) return;
            attachedBlock = block.getRelative(0, 1, 0);
        } else if (action == Action.PHYSICAL) {
            switch (block.getType()) {
            case ACACIA_PRESSURE_PLATE:
            case BIRCH_PRESSURE_PLATE:
            case DARK_OAK_PRESSURE_PLATE:
            case HEAVY_WEIGHTED_PRESSURE_PLATE:
            case JUNGLE_PRESSURE_PLATE:
            case LIGHT_WEIGHTED_PRESSURE_PLATE:
            case OAK_PRESSURE_PLATE:
            case SPRUCE_PRESSURE_PLATE:
            case STONE_PRESSURE_PLATE:
                if (justTeleportedToPressurePlate.remove(player.getUniqueId())) return;
                break;
            default:
                if (Tag.PRESSURE_PLATES.isTagged(block.getType())) break;
                return;
            }
            if (isOnCooldown(player.getUniqueId())) return;
            attachedBlock = block.getRelative(0, 2, 0);
        } else {
            return;
        }
        Sign sign = Util.findAttachedLinkSign(attachedBlock);
        if (sign == null) return;
        Portal portal = plugin.getPortals().portalWithSign(sign);
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
        final Block block = event.getEntity().getLocation().getBlock().getRelative(0, -1, 0);
        if (block.getType() == Material.OBSIDIAN) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        cooldowns.remove(uuid);
        sessions.remove(uuid);
    }
}
