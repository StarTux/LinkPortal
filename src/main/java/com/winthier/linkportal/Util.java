package com.winthier.linkportal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

public class Util {
    static final BlockFace[] faces = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };
    enum PortalBlockType {
        FRAME, PORTAL;
    }

    private static void checkPortalBlock(final Block block, Set<Block> blocks, Set<Block> checked, PortalBlockType blockType) {
        if (checked.contains(block)) return;
        checked.add(block);
        Material type = block.getType();
        if (type.isSolid()) {
            if (blockType == PortalBlockType.FRAME) {
                blocks.add(block);
            }
        }
        if (type == Material.PORTAL) {
            if (blockType == PortalBlockType.PORTAL) {
                blocks.add(block);
            }
            for (BlockFace face : faces) {
                Block otherBlock = block.getRelative(face);
                checkPortalBlock(otherBlock, blocks, checked, blockType);
            }
        }
    }

    static Set<Block> findPortalBlocksNear(final Block block, PortalBlockType blockType) {
        Set<Block> blocks = new HashSet<Block>();
        Set<Block> checked = new HashSet<Block>();
        if (block.getType() == Material.PORTAL) checkPortalBlock(block, blocks, checked, blockType);
        for (BlockFace face : faces) {
            Block otherBlock = block.getRelative(face);
            if (otherBlock.getType() == Material.PORTAL) checkPortalBlock(otherBlock, blocks, checked, blockType);
        }
        return blocks;
    }

    static Sign findPortalSignNear(final Set<Block> portalBlocks) {
        for (Block block : portalBlocks) {
            for (BlockFace face : faces) {
                Block otherBlock = block.getRelative(face);
                BlockState state = otherBlock.getState();
                if (state instanceof Sign) {
                    Sign sign = (Sign)state;
                    if (sign.getLine(0).equalsIgnoreCase("[link]")) return sign;
                }
            }
        }
        return null;
    }

    static Sign findPortalSignNear(final Block loc) {
        return findPortalSignNear(findPortalBlocksNear(loc, PortalBlockType.FRAME));
    }

    static Sign findPortalSignNear(final Location loc) {
        return findPortalSignNear(loc.getBlock());
    }

    static Set<Block> findPortalBlocksNearSign(final Block block, PortalBlockType blockType) {
        MaterialData data = block.getState().getData();
        if (!(data instanceof org.bukkit.material.Sign)) return null;
        org.bukkit.material.Sign sign = (org.bukkit.material.Sign)data;
        return findPortalBlocksNear(block.getRelative(sign.getAttachedFace()), blockType);
    }

    static String format(String string, Object... args) {
        string = ChatColor.translateAlternateColorCodes('&', string);
        if (args.length > 0) string = String.format(string, args);
        return string;
    }

    static void msg(Player player, String msg, Object... args) {
        player.sendMessage(format(msg, args));
    }

    static boolean entityWalkThroughPortal(Entity entity, Portal portal) {
        List<Portal> ring = LinkPortalPlugin.instance.portals.ringOfPortal(portal);
        if (ring == null || ring.isEmpty()) return false;
        int startIndex = ring.indexOf(portal);
        for (int i = 1; i < ring.size(); ++i) {
            int index = (startIndex + i) % ring.size();
            Portal aPortal = ring.get(index);
            if (entityWarpToPortal(entity, aPortal)) {
                return true;
            }
        }
        return false;
    }

    static boolean entityWarpToPortal(Entity entity, Portal portal) {
        Location loc = portal.findWarpLocation();
        if (loc == null) {
            LinkPortalPlugin.instance.getLogger().info("Deleting portal of "+portal.getOwnerName()+" ("+portal.getOwnerUuid()+") at "+portal.describeLocation()+" because portal blocks cannot be found.");
            LinkPortalPlugin.instance.portals.removePortal(portal);
            return false;
        }
        Location eLoc = entity.getLocation();
        loc.setYaw(eLoc.getYaw());
        loc.setPitch(eLoc.getPitch());
        entity.teleport(loc);
        return true;
    }
}
