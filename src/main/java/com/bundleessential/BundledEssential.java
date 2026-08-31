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
    }

    public static BundledEssential getInstance() {
        return instance;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }
}
