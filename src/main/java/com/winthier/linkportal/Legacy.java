// package com.winthier.linkportal;

// import com.winthier.playercache.PlayerCache;
// import java.io.File;
// import java.util.List;
// import java.util.UUID;
// import org.bukkit.Bukkit;
// import org.bukkit.World;
// import org.bukkit.block.Block;
// import org.bukkit.block.BlockState;
// import org.bukkit.block.Sign;
// import org.bukkit.configuration.ConfigurationSection;
// import org.bukkit.configuration.file.YamlConfiguration;

// class Legacy {
//     static void importPortals() {
//         int count = 0;
//         File file = new File(LinkPortalPlugin.instance.getDataFolder(), "import.yml");
//         ConfigurationSection config = YamlConfiguration.loadConfiguration(file);
//         for (String signWorld: config.getKeys(false)) {
//             World world = Bukkit.getWorld(signWorld);
//             if (world == null) {
//                     LinkPortalPlugin.instance.getLogger().warning("World not found: " + signWorld);
//                     continue;
//             }
//             ConfigurationSection worldSection = config.getConfigurationSection(signWorld);
//         playerLoop:
//             for (String playerName: worldSection.getKeys(false)) {
//                 UUID playerUuid = PlayerCache.uuidForName(playerName);
//                 if (playerUuid == null) {
//                     LinkPortalPlugin.instance.getLogger().warning("Player not found: " + playerName);
//                     continue playerLoop;
//                 }
//                 List<Integer> coords = worldSection.getIntegerList(playerName);
//                 int x = coords.get(0);
//                 int y = coords.get(1);
//                 int z = coords.get(2);
//                 Block block = world.getBlockAt(x, y, z);
//                 BlockState state = block.getState();
//                 if (!(state instanceof Sign)) continue playerLoop;
//                 Sign sign = (Sign)state;
//                 if (sign.getLine(0).equals("[link]") ||
//                     sign.getLine(0).equals("link")) {
//                     sign.setLine(0, "[link]");
//                     sign.setLine(1, "");
//                     sign.setLine(2, "");
//                     sign.setLine(3, "");
//                     sign.update(true, false);
//                     Portal portal = Portal.of(playerUuid, sign);
//                     LinkPortalPlugin.instance.portals.addPortal(portal);
//                     count += 1;
//                 }
//             }
//         }
//         LinkPortalPlugin.instance.portals.savePortals();
//         LinkPortalPlugin.instance.getLogger().info("Imported " + count + " link portals");
//     }
// }
