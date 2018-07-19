package com.winthier.linkportal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

final class PortalStorage {
    private YamlConfiguration config = null;
    static final String PATH = "portals.yml";
    static final String LIST = "portals";
    final ConfigurationSection tmpConfig = new YamlConfiguration();

    File getSaveFile() {
        File folder = LinkPortalPlugin.getInstance().getDataFolder();
        folder.mkdirs();
        return new File(folder, PATH);
    }

    void load() {
        config = YamlConfiguration.loadConfiguration(getSaveFile());
    }

    void save() {
        if (config == null) return;
        try {
            config.save(getSaveFile());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    void reload() {
        config = null;
    }

    ConfigurationSection getConfig() {
        if (config == null) load();
        return config;
    }

    List<Portal> getPortals() {
        getConfig();
        List<Portal> result = new ArrayList<>();
        for (Map<?, ?> map: config.getMapList(LIST)) {
            ConfigurationSection section = tmpConfig.createSection("tmp", map);
            Portal portal = Portal.deserialize(section);
            result.add(portal);
        }
        return result;
    }

    void setPortals(List<Portal> portals) {
        List<Object> list = new ArrayList<>();
        for (Portal portal: portals) {
            list.add(portal.serialize());
        }
        getConfig().set(LIST, list);
    }
}
