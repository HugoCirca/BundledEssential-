package com.bundleessential.waypoint;

import com.bundleessential.BundledEssential;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WaypointManager implements CommandExecutor, TabCompleter, Listener {

    private final BundledEssential plugin;
    private final Map<UUID, Map<String, Waypoint>> waypoints = new HashMap<>();
    private final Map<UUID, Inventory> openGUIs = new HashMap<>();
    private static final int MAX_WAYPOINTS = 27;
    private static final String GUI_TITLE = "§6Waypoints";

    public WaypointManager(BundledEssential plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadWaypoints();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length == 0) {
            openWaypointGUI(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "new" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /waypoint:new <name>");
                    return true;
                }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                createWaypoint(player, name);
            }
            case "delete" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /waypoint:delete <name>");
                    return true;
                }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                deleteWaypoint(player, name);
            }
            default -> {
                // Try to teleport to waypoint by name
                String name = String.join(" ", args);
                teleportToWaypoint(player, name);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("new");
            completions.add("delete");
            if (sender instanceof Player player) {
                Map<String, Waypoint> playerWaypoints = waypoints.getOrDefault(player.getUniqueId(), new HashMap<>());
                completions.addAll(playerWaypoints.keySet());
            }
        }
        return completions;
    }

    private void createWaypoint(Player player, String name) {
        UUID uuid = player.getUniqueId();
        Map<String, Waypoint> playerWaypoints = waypoints.computeIfAbsent(uuid, k -> new HashMap<>());

        if (playerWaypoints.containsKey(name)) {
            player.sendMessage("§cYou already have a waypoint named §e" + name + "§c!");
            return;
        }

        if (playerWaypoints.size() >= MAX_WAYPOINTS) {
            player.sendMessage("§cYou have reached the maximum number of waypoints (" + MAX_WAYPOINTS + ")!");
            return;
        }

        playerWaypoints.put(name, new Waypoint(name, player.getLocation().clone()));
        saveWaypoints(uuid);
        player.sendMessage("§aWaypoint §e" + name + " §acreated!");
    }

    private void deleteWaypoint(Player player, String name) {
        UUID uuid = player.getUniqueId();
        Map<String, Waypoint> playerWaypoints = waypoints.getOrDefault(uuid, new HashMap<>());

        if (!playerWaypoints.containsKey(name)) {
            player.sendMessage("§cYou don't have a waypoint named §e" + name + "§c!");
            return;
        }

        playerWaypoints.remove(name);
        saveWaypoints(uuid);
        player.sendMessage("§aWaypoint §e" + name + " §adeleted!");
    }

    private void teleportToWaypoint(Player player, String name) {
        UUID uuid = player.getUniqueId();
        Map<String, Waypoint> playerWaypoints = waypoints.getOrDefault(uuid, new HashMap<>());

        if (!playerWaypoints.containsKey(name)) {
            player.sendMessage("§cYou don't have a waypoint named §e" + name + "§c!");
            return;
        }

        player.teleport(playerWaypoints.get(name).getLocation());
        player.sendMessage("§aTeleported to waypoint §e" + name + "§a!");
    }

    private void openWaypointGUI(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, Waypoint> playerWaypoints = waypoints.computeIfAbsent(uuid, k -> new HashMap<>());

        Inventory gui = Bukkit.createInventory(null, MAX_WAYPOINTS, GUI_TITLE);

        // Fill with glass for empty slots
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, "§7Empty Slot");
        for (int i = 0; i < MAX_WAYPOINTS; i++) {
            gui.setItem(i, glass);
        }

        // Fill occupied slots with colored wool
        Material[] woolColors = {
            Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL,
            Material.LIME_WOOL, Material.GREEN_WOOL, Material.CYAN_WOOL,
            Material.LIGHT_BLUE_WOOL, Material.BLUE_WOOL, Material.PURPLE_WOOL,
            Material.MAGENTA_WOOL, Material.PINK_WOOL, Material.WHITE_WOOL,
            Material.LIGHT_GRAY_WOOL, Material.GRAY_WOOL, Material.BROWN_WOOL
        };

        int index = 0;
        for (Map.Entry<String, Waypoint> entry : playerWaypoints.entrySet()) {
            if (index >= MAX_WAYPOINTS) break;

            Material wool = woolColors[index % woolColors.length];
            ItemStack item = createItem(wool, "§a" + entry.getKey(),
                "§7Click to teleport",
                "§7Location: §f" + formatLocation(entry.getValue().getLocation()));
            gui.setItem(index, item);
            index++;
        }

        openGUIs.put(uuid, gui);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // Check if it's a wool item (waypoint)
        if (clicked.getType().name().endsWith("_WOOL")) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || meta.getDisplayName() == null) return;

            String name = ChatColor.stripColor(meta.getDisplayName());
            UUID uuid = player.getUniqueId();
            Map<String, Waypoint> playerWaypoints = waypoints.getOrDefault(uuid, new HashMap<>());

            if (playerWaypoints.containsKey(name)) {
                player.closeInventory();
                player.teleport(playerWaypoints.get(name).getLocation());
                player.sendMessage("§aTeleported to waypoint §e" + name + "§a!");
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClickAny(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        // Cancel all clicks including number keys, shift clicks, etc.
        event.setCancelled(true);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatLocation(org.bukkit.Location loc) {
        return String.format("%s %d, %d, %d",
            loc.getWorld().getName(),
            loc.getBlockX(),
            loc.getBlockY(),
            loc.getBlockZ()
        );
    }

    private void loadWaypoints() {
        // Load from config - simplified for low resource usage
        var config = plugin.getConfig();
        if (config.contains("waypoints")) {
            var section = config.getConfigurationSection("waypoints");
            if (section != null) {
                for (String uuidStr : section.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        var wpSection = section.getConfigurationSection(uuidStr);
                        if (wpSection != null) {
                            Map<String, Waypoint> playerWps = new HashMap<>();
                            for (String wpName : wpSection.getKeys(false)) {
                                var loc = wpSection.getLocation(wpName);
                                if (loc != null) {
                                    playerWps.put(wpName, new Waypoint(wpName, loc));
                                }
                            }
                            waypoints.put(uuid, playerWps);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    private void saveWaypoints(UUID uuid) {
        var config = plugin.getConfig();
        Map<String, Waypoint> playerWps = waypoints.getOrDefault(uuid, new HashMap<>());

        String path = "waypoints." + uuid.toString();
        config.set(path, null);

        for (Map.Entry<String, Waypoint> entry : playerWps.entrySet()) {
            config.set(path + "." + entry.getKey(), entry.getValue().getLocation());
        }

        plugin.saveConfig();
    }

    private static class Waypoint {
        private final String name;
        private final org.bukkit.Location location;

        public Waypoint(String name, org.bukkit.Location location) {
            this.name = name;
            this.location = location;
        }

        public String getName() {
            return name;
        }

        public org.bukkit.Location getLocation() {
            return location;
        }
    }
}
