package dev.hugocirca.knapsack.home;

import dev.hugocirca.knapsack.KnapsackPlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HomeManager implements CommandExecutor, TabCompleter {

    private final KnapsackPlugin plugin;
    private final Map<UUID, Location> homes = new HashMap<>();
    private final File homeFile;
    private final FileConfiguration homeConfig;

    public HomeManager(KnapsackPlugin plugin) {
        this.plugin = plugin;
        this.homeFile = new File(plugin.getDataFolder(), "homes.yml");
        if (!homeFile.exists()) {
            try { homeFile.createNewFile(); } catch (IOException e) { plugin.getLogger().severe("Could not create homes.yml!"); }
        }
        this.homeConfig = YamlConfiguration.loadConfiguration(homeFile);
        loadHomes();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("home") && args.length >= 1) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set")) { setHome(player); return true; }
            if (sub.equals("remove") || sub.equals("delete")) { removeHome(player); return true; }
        }
        switch (cmd) {
            case "sethome" -> setHome(player);
            case "removehome" -> removeHome(player);
            case "home" -> teleportHome(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("home") && args.length == 1) {
            out.add("set");
            out.add("remove");
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        out.removeIf(s -> !s.toLowerCase().startsWith(last));
        return out;
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
        if (!homes.containsKey(uuid)) { player.sendMessage("§cYou don't have a home! Use §6/sethome §cto set one."); return; }
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
