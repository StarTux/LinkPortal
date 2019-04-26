package com.winthier.linkportal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

final class LinkPortals {
    private LinkPortalPlugin plugin;
    private final PortalStorage storage;
    private List<Portal> portals = null;

    LinkPortals(LinkPortalPlugin plugin) {
        this.plugin = plugin;
        this.storage = new PortalStorage(plugin);
    }

    public List<Portal> getPortals() {
        if (portals == null) {
            portals = new LinkedList<>();
            portals.addAll(storage.getPortals());
        }
        return portals;
    }

    public void addPortal(Portal portal) {
        for (Iterator<Portal> it = getPortals().iterator(); it.hasNext();) {
            Portal po = it.next();
            if (po.signLocationEquals(portal)) {
                it.remove();
            }
        }
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
        storage.reload();
    }

    public Portal portalWithSign(Sign sign) {
        final Block block = sign.getBlock();
        final String worldName = block.getWorld().getName();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        for (Portal portal: getPortals()) {
            if (portal.signLocationEquals(worldName, x, y, z)) return portal;
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
