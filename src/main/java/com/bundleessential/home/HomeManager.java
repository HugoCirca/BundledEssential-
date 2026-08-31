package com.bundleessential.home;

import com.bundleessential.BundledEssential;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeManager implements CommandExecutor {

    private final BundledEssential plugin;
    private final Map<UUID, Location> homes = new HashMap<>();
    private final File homeFile;
    private final FileConfiguration homeConfig;

    public HomeManager(BundledEssential plugin) {
        this.plugin = plugin;
        this.homeFile = new File(plugin.getDataFolder(), "homes.yml");
        if (!homeFile.exists()) {
            try {
                homeFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create homes.yml!");
            }
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

        switch (command.getName().toLowerCase()) {
            case "sethome" -> setHome(player);
            case "removehome" -> removeHome(player);
            case "home" -> teleportHome(player);
        }
        return true;
    }

    private void setHome(Player player) {
        UUID uuid = player.getUniqueId();

        if (homes.containsKey(uuid)) {
            player.sendMessage("§cError, You already have a home");
            return;
        }

        homes.put(uuid, player.getLocation().clone());
        saveHome(uuid, player.getLocation());
        player.sendMessage("§aWelcome to your new home, §e" + player.getName() + "§a!");
    }

    private void removeHome(Player player) {
        UUID uuid = player.getUniqueId();

        if (!homes.containsKey(uuid)) {
            player.sendMessage("§cYou don't have a home to remove!");
            return;
        }

        homes.remove(uuid);
        homeConfig.set(uuid.toString(), null);
        saveConfig();
        player.sendMessage("§aYour home has been removed!");
    }

    private void teleportHome(Player player) {
        UUID uuid = player.getUniqueId();

        if (!homes.containsKey(uuid)) {
            player.sendMessage("§cYou don't have a home! Use §6/sethome §cto set one.");
            return;
        }

        player.teleport(homes.get(uuid));
        player.sendMessage("§aTeleported to your home!");
    }

    public Location getHome(UUID uuid) {
        return homes.get(uuid);
    }

    private void loadHomes() {
        for (String key : homeConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                Location loc = homeConfig.getLocation(key);
                if (loc != null) {
                    homes.put(uuid, loc);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveHome(UUID uuid, Location loc) {
        homeConfig.set(uuid.toString(), loc);
        saveConfig();
    }

    private void saveConfig() {
        try {
            homeConfig.save(homeFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml!");
        }
    }
}
