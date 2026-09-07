package dev.hugocirca.knapsack.home;

import dev.hugocirca.knapsack.KnapsackPlugin;
import dev.hugocirca.knapsack.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HomeManager implements CommandExecutor, TabCompleter, Listener {

    private final KnapsackPlugin plugin;
    private final Map<UUID, Location> homes = new HashMap<>();
    private final File homeFile;
    private final FileConfiguration homeConfig;
    private static final String GUI_TITLE = "§6Home";

    public HomeManager(KnapsackPlugin plugin) {
        this.plugin = plugin;
        this.homeFile = new File(plugin.getDataFolder(), "homes.yml");
        if (!homeFile.exists()) {
            try { homeFile.createNewFile(); } catch (IOException e) { plugin.getLogger().severe("Could not create homes.yml!"); }
        }
        this.homeConfig = YamlConfiguration.loadConfiguration(homeFile);
        loadHomes();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("home")) {
            if (args.length >= 1 && args[0].equalsIgnoreCase("set")) {
                setHome(player);
                return true;
            }
            openGui(player);
            return true;
        }
        if (cmd.equals("sethome")) {
            setHome(player);
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("home") && args.length == 1) out.add("set");
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        out.removeIf(s -> !s.toLowerCase().startsWith(last));
        return out;
    }

    private void openGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, GUI_TITLE);
        GuiUtil.fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        boolean has = homes.containsKey(player.getUniqueId());
        if (has) {
            inv.setItem(4, GuiUtil.item(Material.ENDER_PEARL, "§aTeleport Home", "§7Click to teleport", "§7Shift-click to remove home"));
        } else {
            inv.setItem(4, GuiUtil.item(Material.EMERALD_BLOCK, "§aSet Home", "§7Click to set home here", "§7Empty slot = set"));
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().getTitle().equals(GUI_TITLE)) return;
        e.setCancelled(true);
        if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;
        if (e.getSlot() != 4) return;
        boolean has = homes.containsKey(p.getUniqueId());
        if (has) {
            if (e.isShiftClick()) {
                removeHome(p);
                p.closeInventory();
            } else {
                p.closeInventory();
                teleportHome(p);
            }
        } else {
            setHome(p);
            p.closeInventory();
        }
    }

    private void setHome(Player player) {
        UUID uuid = player.getUniqueId();
        if (homes.containsKey(uuid)) { player.sendMessage("§cYou already have a home"); return; }
        homes.put(uuid, player.getLocation().clone());
        saveHome(uuid, player.getLocation());
        player.sendMessage("§aWelcome to your new home, §e" + player.getName() + "§a!");
    }

    private void removeHome(Player player) {
        UUID uuid = player.getUniqueId();
        if (!homes.containsKey(uuid)) { player.sendMessage("§cYou don't have a home to remove!"); return; }
        homes.remove(uuid);
        homeConfig.set(uuid.toString(), null);
        saveConfig();
        player.sendMessage("§aYour home has been removed!");
    }

    private void teleportHome(Player player) {
        UUID uuid = player.getUniqueId();
        if (!homes.containsKey(uuid)) { player.sendMessage("§cYou don't have a home! Use §6/home §cto set one."); return; }
        player.teleport(homes.get(uuid));
        player.sendMessage("§aTeleported to your home!");
    }

    public Location getHome(UUID uuid) { return homes.get(uuid); }

    private void loadHomes() {
        for (String key : homeConfig.getKeys(false)) {
            try { UUID uuid = UUID.fromString(key); Location loc = homeConfig.getLocation(key); if (loc != null) homes.put(uuid, loc); } catch (IllegalArgumentException ignored) {}
        }
    }

    private void saveHome(UUID uuid, Location loc) { homeConfig.set(uuid.toString(), loc); saveConfig(); }

    private void saveConfig() { try { homeConfig.save(homeFile); } catch (IOException e) { plugin.getLogger().severe("Could not save homes.yml!"); } }
}
