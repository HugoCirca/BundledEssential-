package com.bundleessential.back;

import com.bundleessential.BundledEssential;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackManager implements CommandExecutor, Listener {

    private final BundledEssential plugin;
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    public BackManager(BundledEssential plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (!deathLocations.containsKey(uuid)) {
            player.sendMessage("§cYou have no death location to return to!");
            return true;
        }

        player.teleport(deathLocations.get(uuid));
        deathLocations.remove(uuid);
        player.sendMessage("§aTeleported back to your death location!");
        return true;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        deathLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    public Location getDeathLocation(UUID uuid) {
        return deathLocations.get(uuid);
    }
}
