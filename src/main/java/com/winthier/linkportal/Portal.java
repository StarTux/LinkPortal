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
import lombok.experimental.NonFinal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;

@Value
@RequiredArgsConstructor
public class Portal {
    String signWorld;
    int signX, signY, signZ;
    UUID ownerUuid;
    String ownerName;
    String ringName;
    // Cache
    @NonFinal Block signBlock = null, attachedBlock = null;

    public Block getSignBlock() {
        if (signBlock == null) {
            World world = Bukkit.getServer().getWorld(signWorld);
            if (world == null) {
                LinkPortalPlugin.instance.getLogger().warning("World not found: " + signWorld);
                return null;
            }
            signBlock = world.getBlockAt(signX, signY, signZ);
        }
        return signBlock;
    }

    public Block getAttachedBlock() {
        if (attachedBlock == null) {
            Block signBlock = getSignBlock();
            if (signBlock == null) return null;
            BlockFace attachedFace = Util.getAttachedFace(signBlock);
            if (attachedFace == null) return null;
            return signBlock.getRelative(attachedFace);
        }
        return attachedBlock;
    }

    public Location findWarpLocation() {
        if (isBlocky()) {
            return getAttachedBlock().getRelative(0, -2, 0).getLocation().add(0.5, 0.0, 0.5);
        } else {
            Block attachedBlock = getAttachedBlock();
            if (signBlock == null) return null;
            Set<Block> blocks = Util.findPortalBlocksNear(attachedBlock, Util.PortalBlockType.PORTAL);
            if (blocks == null || blocks.isEmpty()) return null;
            int x = 0, y = Integer.MAX_VALUE, z = 0;
            for (Block block: blocks) {
                x += block.getX();
                y = Math.min(y, block.getY());
                z += block.getZ();
            }
            return new Location(
                signBlock.getWorld(),
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
        switch (chestBlock.getType()) {
        case AIR:
        case STONE_BUTTON:
        case WOOD_BUTTON:
            break;
        default:
            return false;
        }
        switch (footBlock.getType()) {
        case AIR:
        case STONE_PLATE:
        case GOLD_PLATE:
        case IRON_PLATE:
        case WOOD_PLATE:
            break;
        default:
            return false;
        }
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
        Location loc = findWarpLocation();
        if (loc == null) {
            LinkPortalPlugin.instance.getLogger().info("Deleting portal of "+ownerName+" ("+ownerUuid+") at "+describeLocation()+" because portal blocks cannot be found.");
            LinkPortalPlugin.instance.portals.removePortal(this);
            return false;
        }
        Location eLoc = entity.getLocation();
        loc.setYaw(eLoc.getYaw());
        loc.setPitch(eLoc.getPitch());
        entity.teleport(loc);
        return true;
    }

    public boolean entityWalkThroughPortal(Entity entity) {
        List<Portal> ring = LinkPortalPlugin.instance.portals.ringOfPortal(this);
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
                if (ownerName.endsWith("s") ||
                    ownerName.endsWith("x") ||
                    ownerName.endsWith("z")) {
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
