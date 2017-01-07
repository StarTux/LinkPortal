package com.winthier.linkportal;

import java.util.Arrays;
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
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

public class Util {
    static final BlockFace[] faces = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };
    static final BlockFace[] horizontalFaces = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    enum PortalBlockType {
        FRAME, PORTAL;
    }
    enum AxisAlignment {
        X, Z;
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
                    if (signHasLinkTag(sign)) return sign;
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

    static BlockFace getAttachedFace(Block block) {
        MaterialData data = block.getState().getData();
        if (!(data instanceof org.bukkit.material.Sign)) return null;
        org.bukkit.material.Sign matSign = (org.bukkit.material.Sign)data;
        return matSign.getAttachedFace();
    }

    static BlockFace getSignFacing(Block block) {
        MaterialData data = block.getState().getData();
        if (!(data instanceof org.bukkit.material.Sign)) return null;
        org.bukkit.material.Sign matSign = (org.bukkit.material.Sign)data;
        return matSign.getFacing();
    }

    static boolean isLinkSign(Block block) {
        switch (block.getType()) {
        case SIGN:
        case SIGN_POST:
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
        for (BlockFace dir: horizontalFaces) {
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
        Material blockType = block.getType();
        if (blockType == Material.AIR) {
            blocks.add(block);
            for (BlockFace face: searchDirections) {
                if (!checkBlockForPortalCreation(block.getRelative(face), blocks, searched, searchDirections)) {
                    return false;
                }
            }
            return true;
        } else if (blockType.isSolid()) {
            return true;
        } else {
            return false;
        }
    }

    static boolean createNetherPortal(Block block, AxisAlignment alignment) {
        final List<BlockFace> searchDirections;
        final List<BlockFace> alternateDirections;
        final int data;
        switch (alignment) {
        case X:
            searchDirections = Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.WEST, BlockFace.EAST);
            alternateDirections = Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH);
            data = 1;
            break;
        case Z:
            searchDirections = Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH);
            alternateDirections = Arrays.asList(BlockFace.EAST, BlockFace.WEST);
            data = 2;
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
                if (portal.getRelative(face).getType() == Material.PORTAL) return false;
            }
        }
        
        for (Block portal: blocks) {
            BlockState state = portal.getState();
            state.setType(Material.PORTAL);
            state.setRawData((byte)data);
            state.update(true, false);
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
