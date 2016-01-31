package com.winthier.linkportal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class LinkPortals {
    final PortalStorage storage = new PortalStorage();
    List<Portal> portals = null;
    
    public List<Portal> getPortals() {
        if (portals == null) {
            portals = new LinkedList<>();
            portals.addAll(storage.getPortals());
        }
        return portals;
    }

    public void addPortal(Portal portal) {
        getPortals().add(portal);
    }

    public void removePortal(Portal portal) {
        getPortals().remove(portal);
    }

    public void savePortals() {
        if (portals == null) return;
        storage.setPortals(portals);
        storage.save();
    }

    public void reload() {
        portals = null;
    }

    public Portal portalWithSign(Sign sign) {
        final Block block = sign.getBlock();
        final String worldName = block.getWorld().getName();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        for (Portal portal: getPortals()) {
            if (portal.signLocationEquals(worldName, x, y ,z)) return portal;
        }
        return null;
    }

    public List<Portal> ringOfPortal(Portal portal) {
        List<Portal> result = new ArrayList<>();
        for (Portal entry: getPortals()) {
            if (entry.ownerAndRingEquals(portal)) {
                result.add(entry);
            }
        }
        return result;
    }
}