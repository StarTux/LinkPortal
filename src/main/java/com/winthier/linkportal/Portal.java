package com.winthier.linkportal;

import com.winthier.playercache.PlayerCache;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

@Value
public class Portal {
    String signWorld;
    int signX, signY, signZ;
    UUID ownerUuid;
    String ownerName;
    String ringName;

    public Block getSignBlock() {
        World world = Bukkit.getServer().getWorld(signWorld);
        if (world == null) {
            LinkPortalPlugin.instance.getLogger().warning("World not found: " + signWorld);
            return null;
        }
        Block block = world.getBlockAt(signX, signY, signZ);
        return block;
    }

    public Location findWarpLocation() {
        Block signBlock = getSignBlock();
        if (signBlock == null) return null;
        Set<Block> blocks = Util.findPortalBlocksNearSign(signBlock, Util.PortalBlockType.PORTAL);
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
        StringBuilder sb = new StringBuilder(lines[1]);
        String line2 = lines[2];
        String line3 = lines[3];
        if (!line2.isEmpty()) { sb.append(" ").append(line2); }
        if (!line3.isEmpty()) { sb.append(" ").append(line3); }
        return sb.toString();
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
}
