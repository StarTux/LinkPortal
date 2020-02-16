package com.winthier.linkportal;

import java.util.HashSet;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class LinkPortalPlugin extends JavaPlugin {
    private final LinkPortalListener listener = new LinkPortalListener(this);
    private final LinkPortals portals = new LinkPortals(this);
    private boolean debugMode;
    HashSet<UUID> serverPortal = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConf();
        getServer().getPluginManager().registerEvents(listener, this);
        getCommand("linkportal").setExecutor(new LinkPortalCommand(this));
    }

    void loadConf() {
        reloadConfig();
        this.debugMode = getConfig().getBoolean("debug");
        if (this.debugMode) {
            getLogger().info("Debug mode enabled in config.yml!");
        }
    }
}
