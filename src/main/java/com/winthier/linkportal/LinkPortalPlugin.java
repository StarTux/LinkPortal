package com.winthier.linkportal;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class LinkPortalPlugin extends JavaPlugin {
    @Getter private static LinkPortalPlugin instance = null;
    private final LinkPortalListener listener = new LinkPortalListener(this);
    private final LinkPortals portals = new LinkPortals();

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(listener, this);
        getCommand("linkportal").setExecutor(new LinkPortalCommand());
    }
}
