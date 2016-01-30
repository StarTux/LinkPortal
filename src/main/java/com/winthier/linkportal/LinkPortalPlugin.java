package com.winthier.linkportal;

import org.bukkit.plugin.java.JavaPlugin;
import lombok.Getter;

public class LinkPortalPlugin extends JavaPlugin {
    static LinkPortalPlugin instance = null;
    final LinkPortalListener listener = new LinkPortalListener();
    final LinkPortals portals = new LinkPortals();
    
    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(listener, this);
        getCommand("linkportal").setExecutor(new LinkPortalCommand());
    }

    @Override
    public void onDisable() {
    }
}
