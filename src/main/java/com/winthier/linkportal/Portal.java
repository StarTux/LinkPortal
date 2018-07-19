package com.winthier.linkportal;

import com.winthier.playercache.PlayerCache;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@Value
@RequiredArgsConstructor
public final class Portal {
    final String signWorld;
    final int signX, signY, signZ;
    final UUID ownerUuid;
    final String ownerName;
    final String ringName;

    public Block getSignBlock() {
        World world = Bukkit.getServer().getWorld(signWorld);
        if (world == null) {
            LinkPortalPlugin.getInstance().getLogger().warning("World not found: " + signWorld);
            return null;
        }
        return world.getBlockAt(signX, signY, signZ);
    }

    public Block getAttachedBlock() {
        Block signBlock = getSignBlock();
        if (!Util.isLinkSign(signBlock)) return null;
        if (signBlock == null) return null;
        BlockFace attachedFace = Util.getAttachedFace(signBlock);
        if (attachedFace == null) return null;
        return signBlock.getRelative(attachedFace);
    }

    public Location findWarpLocation() {
        if (isBlocky()) {
            return getAttachedBlock().getRelative(0, -2, 0).getLocation().add(0.5, 0.0, 0.5);
        } else {
            Block attachedBlock = getAttachedBlock();
            if (attachedBlock == null) return null;
            Set<Block> blocks = Util.findPortalBlocksNear(attachedBlock, Util.PortalBlockType.PORTAL);
            if (blocks == null || blocks.isEmpty()) return null;
            int x = 0, y = Integer.MAX_VALUE, z = 0;
            for (Block block: blocks) {
                x += block.getX();
                y = Math.min(y, block.getY());
                z += block.getZ();
            }
            return new Location(
                attachedBlock.getWorld(),
                (double)x / (double)blocks.size() + 0.5,
                (double)y,
                (double)z / (double)blocks.size() + 0.5);
        }
    }

    public boolean isBlocky() {
        Block attachedBlock = getAttachedBlock();
        if (attachedBlock == null) return false;
        Block chestBlock = attachedBlock.getRelative(0, -1, 0);
        Block footBlock = attachedBlock.getRelative(0, -2, 0);
        Block floorBlock = attachedBlock.getRelative(0, -3, 0);
        if (!floorBlock.getType().isSolid()) return false;
        if (chestBlock.getType().isOccluding()) return false;
        if (chestBlock.getType() == Material.NETHER_PORTAL) return false;
        if (footBlock.getType().isOccluding()) return false;
        if (footBlock.getType() == Material.NETHER_PORTAL) return false;
        return true;
    }

    void serialize(ConfigurationSection config) {
        config.set("sign.World", signWorld);
        config.set("sign.Coordinates", Arrays.asList(signX, signY, signZ));
        config.set("owner.UUID", ownerUuid.toString());
        config.set("owner.Name", ownerName);
        config.set("ring.Name", ringName);
    }

    Map<?, ?> serialize() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> sign = new HashMap<>();
        Map<String, Object> owner = new HashMap<>();
        Map<String, Object> ring = new HashMap<>();
        sign.put("World", signWorld);
        sign.put("Coordinates", Arrays.asList(signX, signY, signZ));
        owner.put("Name", ownerName);
        owner.put("UUID", ownerUuid.toString());
        ring.put("Name", ringName);
        result.put("sign", sign);
        result.put("owner", owner);
        result.put("ring", ring);
        return result;
    }

    static Portal deserialize(ConfigurationSection config) {
        String signWorld = config.getString("sign.World");
        List<Integer> coordinates = config.getIntegerList("sign.Coordinates");
        int signX = coordinates.get(0);
        int signY = coordinates.get(1);
        int signZ = coordinates.get(2);
        UUID ownerUuid = UUID.fromString(config.getString("owner.UUID"));
        String ownerName = config.getString("owner.Name");
        String ringName = config.getString("ring.Name");
        return new Portal(signWorld, signX, signY, signZ, ownerUuid, ownerName, ringName);
    }

    static String ringNameOf(String[] lines) {
        return lines[1];
    }

    static String ringNameOf(Sign sign) {
        return ringNameOf(sign.getLines());
    }

    static Portal of(UUID ownerUuid, Sign sign) {
        Block block = sign.getBlock();
        String signWorld = block.getWorld().getName();
        int signX = block.getX();
        int signY = block.getY();
        int signZ = block.getZ();
        String ownerName = PlayerCache.nameForUuid(ownerUuid);
        if (ownerName == null) ownerName = "";
        String ringName = ringNameOf(sign);
        return new Portal(signWorld, signX, signY, signZ, ownerUuid, ownerName, ringName);
    }

    static Portal of(Player player, Block block, String[] lines) {
        String signWorld = block.getWorld().getName();
        int signX = block.getX();
        int signY = block.getY();
        int signZ = block.getZ();
        UUID ownerUuid = player.getUniqueId();
        String ownerName = player.getName();
        if (ownerName == null) ownerName = "";
        String ringName = ringNameOf(lines);
        return new Portal(signWorld, signX, signY, signZ, ownerUuid, ownerName, ringName);
    }

    public boolean signLocationEquals(Portal other) {
        return signLocationEquals(other.getSignWorld(), other.getSignX(), other.getSignY(), other.getSignZ());
    }

    public boolean signLocationEquals(String worldName, int x, int y, int z) {
        return signWorld.equals(this.signWorld) && x == signX && y == signY && z == signZ;
    }

    public boolean ownerAndRingEquals(Portal portal) {
        return ownerUuid.equals(portal.ownerUuid) && ringName.equals(portal.ringName);
    }

    public String describeLocation() {
        return String.format("%s,%d,%d,%d", signWorld, signX, signY, signZ);
    }

    boolean entityWarpToPortal(Entity entity) {
        final Location loc = findWarpLocation();
        if (loc == null) {
            boolean signIsThere;
            if (isBlocky()) {
                LinkPortalPlugin.getInstance().getLogger().info("Deleting portal \"" + ringName + "\" (blocky) of " + ownerName + " (" + ownerUuid + ") at " + describeLocation() + " because portal blocks cannot be found.");
            } else {
                LinkPortalPlugin.getInstance().getLogger().info("Deleting portal \"" + ringName + "\" of " + ownerName + " (" + ownerUuid + ") at " + describeLocation() + " because portal blocks cannot be found.");
            }
            LinkPortalPlugin.getInstance().getPortals().removePortal(this);
            return false;
        }
        Location eLoc = entity.getLocation();
        loc.setYaw(eLoc.getYaw());
        loc.setPitch(eLoc.getPitch());
        new BukkitRunnable() {
            @Override public void run() {
                entity.teleport(loc);
            }
        }.runTask(LinkPortalPlugin.getInstance());
        switch (loc.getBlock().getType()) {
        case ACACIA_PRESSURE_PLATE:
        case BIRCH_PRESSURE_PLATE:
        case DARK_OAK_PRESSURE_PLATE:
        case HEAVY_WEIGHTED_PRESSURE_PLATE:
        case JUNGLE_PRESSURE_PLATE:
        case LIGHT_WEIGHTED_PRESSURE_PLATE:
        case OAK_PRESSURE_PLATE:
        case SPRUCE_PRESSURE_PLATE:
        case STONE_PRESSURE_PLATE:
                if (entity instanceof Player) {
                    LinkPortalPlugin.getInstance().getListener().justTeleportedToPressurePlate.add(((Player)entity).getUniqueId());
                }
                break;
        default: break;
        }
        return true;
    }

    public boolean entityWalkThroughPortal(Entity entity) {
        List<Portal> ring = LinkPortalPlugin.getInstance().getPortals().ringOfPortal(this);
        if (ring == null || ring.isEmpty()) return false;
        int startIndex = ring.indexOf(this);
        for (int i = 1; i < ring.size(); ++i) {
            int index = (startIndex + i) % ring.size();
            Portal aPortal = ring.get(index);
            if (aPortal.entityWarpToPortal(entity)) {
                return true;
            }
        }
        return false;
    }

    public boolean playerWalkThroughPortal(Player player) {
        boolean result = entityWalkThroughPortal(player);
        if (result) {
            final String displayName;
            if (ownerUuid == player.getUniqueId()) {
                displayName = "your";
            } else {
                if (ownerName.endsWith("s")) {
                    displayName = ownerName + "'";
                } else {
                    displayName = ownerName + "'s";
                }
            }
            Util.msg(player, "&3&lLinkPortal&r You enter &a%s&r Link Portal &a%s", displayName, ringName);
        } else {
            Util.msg(player, "&4&lLinkPortal&r &cThis Link Portal has no destination");
        }
        return result;
    }
}
