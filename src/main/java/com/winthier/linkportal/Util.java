package com.winthier.linkportal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Axis;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

final class Util {
    static final BlockFace[] FACES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
    static final BlockFace[] HORIZONTAL_FACES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    enum PortalBlockType {
        FRAME, PORTAL;
    }

    private Util() { }

    private static void checkPortalBlock(final Block block, Set<Block> blocks, Set<Block> checked, PortalBlockType blockType) {
        if (checked.contains(block)) return;
        checked.add(block);
        Material type = block.getType();
        if (type == Material.NETHER_PORTAL) {
            if (blockType == PortalBlockType.PORTAL) {
                blocks.add(block);
            }
            for (BlockFace face : FACES) {
                Block otherBlock = block.getRelative(face);
                checkPortalBlock(otherBlock, blocks, checked, blockType);
            }
        } else if (!block.isEmpty()) {
            if (blockType == PortalBlockType.FRAME) {
                blocks.add(block);
            }
        }
    }

    static Set<Block> findPortalBlocksNear(final Block block, PortalBlockType blockType) {
        Set<Block> blocks = new HashSet<Block>();
        Set<Block> checked = new HashSet<Block>();
        if (block.getType() == Material.NETHER_PORTAL) checkPortalBlock(block, blocks, checked, blockType);
        for (BlockFace face : FACES) {
            Block otherBlock = block.getRelative(face);
            if (otherBlock.getType() == Material.NETHER_PORTAL) checkPortalBlock(otherBlock, blocks, checked, blockType);
        }
        return blocks;
    }

    static Sign findPortalSignNear(final Set<Block> portalBlocks) {
        for (Block block : portalBlocks) {
            for (BlockFace face : FACES) {
                Block otherBlock = block.getRelative(face);
                BlockState state = otherBlock.getState();
                if (state instanceof Sign) {
                    Sign sign = (Sign)state;
                    if (signHasLinkTag(sign)) return sign;
                }
            }
        }
        return null;
    }

    static Sign findPortalSignNear(final Block loc) {
        return findPortalSignNear(findPortalBlocksNear(loc, PortalBlockType.FRAME));
    }

    static Sign findPortalSignNearNetherPortal(final Entity ent) {
        Location entLoc = ent.getLocation();
        double w = ent.getWidth() * 0.5;
        double h = ent.getHeight();
        Block min = entLoc.clone().add(-w, 0, -w).getBlock();
        Block max = entLoc.clone().add(w, h, w).getBlock();
        int my = max.getY() - min.getY() + 1;
        int mx = max.getX() - min.getX() + 1;
        int mz = max.getZ() - min.getZ() + 1;
        for (int y = 0; y < my; y += 1) {
            for (int z = 0; z < mz; z += 1) {
                for (int x = 0; x < mx; x += 1) {
                    Block aBlock = min.getRelative(x, y, z);
                    if (aBlock.getType() == Material.NETHER_PORTAL) {
                        return findPortalSignNear(aBlock);
                    }
                }
            }
        }
        return null;
    }

    static String format(String string, Object... args) {
        string = ChatColor.translateAlternateColorCodes('&', string);
        if (args.length > 0) string = String.format(string, args);
        return string;
    }

    static void msg(Player player, String msg, Object... args) {
        player.sendMessage(format(msg, args));
    }

    static BlockFace getAttachedFace(Block block) {
        if (block.getType() != Material.WALL_SIGN) return null;
        return ((org.bukkit.block.data.type.WallSign)block.getBlockData()).getFacing().getOppositeFace();
    }

    static BlockFace getSignFacing(Block block) {
        if (block.getType() == Material.WALL_SIGN) {
            return ((org.bukkit.block.data.type.WallSign)block.getBlockData()).getFacing();
        } else if (block.getType() == Material.SIGN) {
            return ((org.bukkit.block.data.type.Sign)block.getBlockData()).getRotation();
        }
        return null;
    }

    static boolean isLinkSign(Block block) {
        switch (block.getType()) {
        case SIGN:
        case WALL_SIGN:
            break;
        default: return false;
        }
        BlockState state = block.getState();
        if (!(state instanceof Sign)) return false;
        Sign sign = (Sign)state;
        return signHasLinkTag(sign);
    }

    static Sign findAttachedLinkSign(Block block) {
        for (BlockFace dir: HORIZONTAL_FACES) {
            Block nbor = block.getRelative(dir);
            BlockFace attachedFace = getAttachedFace(nbor);
            if (attachedFace == null) continue;
            if (attachedFace != dir.getOppositeFace()) continue;
            BlockState state = nbor.getState();
            if (!(state instanceof Sign)) continue;
            Sign sign = (Sign)state;
            if (!signHasLinkTag(sign)) continue;
            return sign;
        }
        return null;
    }

    private static boolean checkBlockForPortalCreation(final Block block, Set<Block> blocks, Set<Block> searched, List<BlockFace> searchDirections) {
        if (blocks.size() > 441) return false;
        if (searched.contains(block)) return true;
        searched.add(block);
        if (block.isEmpty()) {
            blocks.add(block);
            for (BlockFace face: searchDirections) {
                if (!checkBlockForPortalCreation(block.getRelative(face), blocks, searched, searchDirections)) {
                    return false;
                }
            }
            return true;
        } else {
            return block.getType().isSolid();
        }
    }

    static boolean createNetherPortal(Block block, Axis axis) {
        final List<BlockFace> searchDirections;
        final List<BlockFace> alternateDirections;
        switch (axis) {
        case X:
            searchDirections = Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.WEST, BlockFace.EAST);
            alternateDirections = Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH);
            break;
        case Z:
            searchDirections = Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH);
            alternateDirections = Arrays.asList(BlockFace.EAST, BlockFace.WEST);
            break;
        default:
            return false;
        }
        final Set<Block> blocks = new HashSet<>();
        final Set<Block> searched = new HashSet<>();
        blocks.add(block);
        searched.add(block);
        for (BlockFace face: searchDirections) {
            if (!checkBlockForPortalCreation(block.getRelative(face), blocks, searched, searchDirections)) {
                return false;
            }
        }
        if (blocks.isEmpty()) return false;
        for (Block portal: blocks) {
            for (BlockFace face: alternateDirections) {
                if (portal.getRelative(face).getType() == Material.NETHER_PORTAL) return false;
            }
        }
        BlockData portalData = Material.NETHER_PORTAL.createBlockData();
        ((org.bukkit.block.data.Orientable)portalData).setAxis(axis);
        for (Block portal: blocks) {
            portal.setBlockData(portalData, false);
        }
        return true;
    }

    static boolean isLinkTag(String line) {
        if (line == null) return false;
        return ChatColor.stripColor(line).equalsIgnoreCase("[link]");
    }

    static boolean signHasLinkTag(Sign sign) {
        if (sign == null) return false;
        return isLinkTag(sign.getLine(0));
    }
}
