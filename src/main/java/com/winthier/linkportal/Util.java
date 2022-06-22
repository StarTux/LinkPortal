package com.winthier.linkportal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Axis;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
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

    /**
     * Helper function for findPortalBlocksNear().
     */
    private static void checkPortalBlock(final Block block, Set<Block> blocks, Set<Block> checked, PortalBlockType blockType) {
        if (checked.contains(block)) return;
        if (checked.size() > 4096) return;
        checked.add(block);
        Material type = block.getType();
        if (type == Material.NETHER_PORTAL
            || type == Material.END_GATEWAY) {
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

    /**
     * Flood fill to find all portal blocks, or the frames around them.
     */
    static Set<Block> findPortalBlocksNear(final Block block, PortalBlockType blockType) {
        Set<Block> blocks = new HashSet<Block>();
        Set<Block> checked = new HashSet<Block>();
        if (block.getType() == Material.NETHER_PORTAL || block.getType() == Material.END_GATEWAY) {
            checkPortalBlock(block, blocks, checked, blockType);
        }
        for (BlockFace face : FACES) {
            Block otherBlock = block.getRelative(face);
            Material otherMat = otherBlock.getType();
            if (otherMat == Material.NETHER_PORTAL || otherMat == Material.END_GATEWAY) {
                checkPortalBlock(otherBlock, blocks, checked, blockType);
            }
        }
        return blocks;
    }

    static Sign findPortalSignNear(final Set<Block> portalBlocks) {
        for (Block block : portalBlocks) {
            for (BlockFace face : FACES) {
                Block otherBlock = block.getRelative(face);
                BlockState state = otherBlock.getState();
                if (state instanceof Sign) {
                    Sign sign = (Sign) state;
                    if (signHasLinkTag(sign)) return sign;
                }
            }
        }
        return null;
    }

    static Sign findPortalSignNear(final Block loc) {
        return findPortalSignNear(findPortalBlocksNear(loc, PortalBlockType.FRAME));
    }

    /**
     * Scan the entity hitbox for overlapping nether portal blocks,
     * then find a link sign.
     */
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

    static boolean isSign(Block block) {
        if (block == null) return false;
        switch (block.getType()) {
        case ACACIA_SIGN:
        case ACACIA_WALL_SIGN:
        case BIRCH_SIGN:
        case BIRCH_WALL_SIGN:
        case DARK_OAK_SIGN:
        case DARK_OAK_WALL_SIGN:
        case JUNGLE_SIGN:
        case JUNGLE_WALL_SIGN:
        case OAK_SIGN:
        case OAK_WALL_SIGN:
        case SPRUCE_SIGN:
        case SPRUCE_WALL_SIGN:
            return true;
        default:
            return Tag.SIGNS.isTagged(block.getType());
        }
    }

    static boolean isWallSign(Block block) {
        switch (block.getType()) {
        case ACACIA_WALL_SIGN:
        case BIRCH_WALL_SIGN:
        case DARK_OAK_WALL_SIGN:
        case JUNGLE_WALL_SIGN:
        case OAK_WALL_SIGN:
        case SPRUCE_WALL_SIGN:
            return true;
        default:
            return Tag.WALL_SIGNS.isTagged(block.getType());
        }
    }

    static boolean isSignPost(Block block) {
        switch (block.getType()) {
        case ACACIA_SIGN:
        case BIRCH_SIGN:
        case DARK_OAK_SIGN:
        case JUNGLE_SIGN:
        case OAK_SIGN:
        case SPRUCE_SIGN:
            return true;
        default:
            return Tag.STANDING_SIGNS.isTagged(block.getType());
        }
    }

    static BlockFace getAttachedFace(Block block) {
        if (!isWallSign(block)) return null;
        return ((org.bukkit.block.data.type.WallSign) block.getBlockData()).getFacing().getOppositeFace();
    }

    static BlockFace getSignFacing(Block block) {
        if (isWallSign(block)) {
            return ((org.bukkit.block.data.type.WallSign) block.getBlockData()).getFacing();
        } else if (isSignPost(block)) {
            return ((org.bukkit.block.data.type.Sign) block.getBlockData()).getRotation();
        }
        return null;
    }

    static boolean isLinkSign(Block block) {
        if (block == null) return false;
        if (!isSign(block)) return false;
        BlockState state = block.getState();
        if (!(state instanceof Sign)) return false;
        Sign sign = (Sign) state;
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
            Sign sign = (Sign) state;
            if (!signHasLinkTag(sign)) continue;
            return sign;
        }
        return null;
    }

    private static boolean checkBlockForPortalCreation(final Block block,
                                                       Set<Block> blocks, Set<Block> searched, List<BlockFace> searchDirections,
                                                       Function<Block, Boolean> frameChecker, int maxSize) {
        if (blocks.size() > maxSize) return false;
        if (searched.contains(block)) return true;
        searched.add(block);
        if (block.isEmpty()) {
            blocks.add(block);
            for (BlockFace face: searchDirections) {
                if (!checkBlockForPortalCreation(block.getRelative(face), blocks, searched, searchDirections, frameChecker, maxSize)) {
                    return false;
                }
            }
            return true;
        } else {
            return frameChecker.apply(block);
        }
    }

    static boolean createNetherPortal(Block block, Axis axis, Function<Block, Boolean> frameChecker, int maxSize) {
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
            if (!checkBlockForPortalCreation(block.getRelative(face), blocks, searched, searchDirections, frameChecker, maxSize)) {
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
        ((org.bukkit.block.data.Orientable) portalData).setAxis(axis);
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
        String line = PlainTextComponentSerializer.plainText().serialize(sign.line(0));
        return isLinkTag(line);
    }
}
