package com.bundleessential;

import com.bundleessential.back.BackManager;
import com.bundleessential.home.HomeManager;
import com.bundleessential.tpa.TpaManager;
import com.bundleessential.waypoint.WaypointManager;
import com.bundleessential.util.DataStorage;
import org.bukkit.plugin.java.JavaPlugin;

public class BundledEssential extends JavaPlugin {

    private static BundledEssential instance;
    private DataStorage dataStorage;
    private TpaManager tpaManager;
    private HomeManager homeManager;
    private BackManager backManager;
    private WaypointManager waypointManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        dataStorage = new DataStorage(this);
        tpaManager = new TpaManager(this);
        homeManager = new HomeManager(this);
        backManager = new BackManager(this);
        waypointManager = new WaypointManager(this);

        registerCommands();

        getLogger().info("BundledEssential has been enabled!");
    }

    @Override
    public void onDisable() {
        if (dataStorage != null) {
            dataStorage.saveAll();
        }
        getLogger().info("BundledEssential has been disabled!");
    }

    private void registerCommands() {
        getCommand("tpa").setExecutor(tpaManager);
        getCommand("tpaccept").setExecutor(tpaManager);
        getCommand("tpahere").setExecutor(tpaManager);

        getCommand("sethome").setExecutor(homeManager);
        getCommand("removehome").setExecutor(homeManager);
        getCommand("home").setExecutor(homeManager);

        getCommand("back").setExecutor(backManager);

        getCommand("waypoint").setExecutor(waypointManager);
        getCommand("waypoint").setTabCompleter(waypointManager);

        getCommand("bundlehelp").setExecutor((sender, command, label, args) -> {
            sender.sendMessage("§6§l=== BundledEssential Commands ===");
            sender.sendMessage("§e/tpa <player> §7- Send teleport request");
            sender.sendMessage("§e/tpaccept §7- Accept teleport request");
            sender.sendMessage("§e/tpahere <player> §7- Request player to teleport to you");
            sender.sendMessage("§e/sethome §7- Set your home");
            sender.sendMessage("§e/removehome §7- Remove your home");
            sender.sendMessage("§e/home §7- Teleport to home");
            sender.sendMessage("§e/back §7- Return to death location");
            sender.sendMessage("§e/waypoint §7- Open waypoint GUI");
            sender.sendMessage("§e/waypoint:new <name> §7- Create waypoint");
            sender.sendMessage("§e/waypoint:delete <name> §7- Delete waypoint");
            sender.sendMessage("§6§l===============================");
            return true;
        });
    }

    public static BundledEssential getInstance() {
        return instance;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }
}
