package com.bundleessential;

import com.bundleessential.back.BackManager;
import com.bundleessential.cosmetics.CosmeticsManager;
import com.bundleessential.economy.BalanceManager;
import com.bundleessential.economy.BountyManager;
import com.bundleessential.economy.PriceManager;
import com.bundleessential.economy.SellManager;
import com.bundleessential.economy.ShopManager;
import com.bundleessential.home.HomeManager;
import com.bundleessential.tpa.TpaManager;
import com.bundleessential.trade.TradeManager;
import com.bundleessential.updater.UpdateManager;
import com.bundleessential.util.DataStorage;
import com.bundleessential.util.HelpManager;
import com.bundleessential.waypoint.WaypointManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BundledEssential extends JavaPlugin {

    private static BundledEssential instance;
    private DataStorage dataStorage;
    private TpaManager tpaManager;
    private HomeManager homeManager;
    private BackManager backManager;
    private TradeManager tradeManager;
    private UpdateManager updateManager;
    private WaypointManager waypointManager;
    private BalanceManager balanceManager;
    private PriceManager priceManager;
    private ShopManager shopManager;
    private BountyManager bountyManager;
    private HelpManager helpManager;
    private SellManager sellManager;
    private CosmeticsManager cosmeticsManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        dataStorage = new DataStorage(this);

        if (getConfig().getBoolean("tpa.enabled", true)) {
            tpaManager = new TpaManager(this);
        }
        if (getConfig().getBoolean("home.enabled", true)) {
            homeManager = new HomeManager(this);
        }
        if (getConfig().getBoolean("back.enabled", true)) {
            backManager = new BackManager(this);
        }
        if (getConfig().getBoolean("trade.enabled", true)) {
            tradeManager = new TradeManager(this);
        }
        if (getConfig().getBoolean("waypoints.enabled", true)) {
            waypointManager = new WaypointManager(this);
        }
        if (getConfig().getBoolean("economy.enabled", true)) {
            balanceManager = new BalanceManager(this);
            priceManager = new PriceManager(this);
            bountyManager = new BountyManager(balanceManager);
            balanceManager.setBountyManager(bountyManager);
            sellManager = new SellManager(balanceManager, priceManager);
            shopManager = new ShopManager(balanceManager, priceManager, sellManager);
        }
        if (getConfig().getBoolean("cosmetics.enabled", false) && balanceManager != null) {
            cosmeticsManager = new CosmeticsManager(this, balanceManager);
        }

        updateManager = new UpdateManager(this);
        helpManager = new HelpManager();

        if (balanceManager != null) Bukkit.getPluginManager().registerEvents(balanceManager, this);
        if (shopManager != null) Bukkit.getPluginManager().registerEvents(shopManager, this);
        if (sellManager != null) Bukkit.getPluginManager().registerEvents(sellManager, this);
        if (cosmeticsManager != null) Bukkit.getPluginManager().registerEvents(cosmeticsManager, this);

        registerCommands();
        updateManager.startup();

        getLogger().info("BundledEssential has been enabled!");
    }

    @Override
    public void onDisable() {
        if (dataStorage != null) {
            dataStorage.saveAll();
        }
        if (cosmeticsManager != null) {
            cosmeticsManager.saveCosmetics();
        }
        getLogger().info("BundledEssential has been disabled!");
    }

    private void registerCommands() {
        if (tpaManager != null) {
            getCommand("tpa").setExecutor(tpaManager);
            getCommand("tpaccept").setExecutor(tpaManager);
            getCommand("tpahere").setExecutor(tpaManager);
        }
        if (homeManager != null) {
            getCommand("sethome").setExecutor(homeManager);
            getCommand("removehome").setExecutor(homeManager);
            getCommand("home").setExecutor(homeManager);
        }
        if (backManager != null) {
            getCommand("back").setExecutor(backManager);
        }
        if (tradeManager != null) {
            getCommand("trade").setExecutor(tradeManager);
            getCommand("tradeaccept").setExecutor(tradeManager);
            getCommand("tradecancel").setExecutor(tradeManager);
        }
        if (waypointManager != null) {
            getCommand("waypoint").setExecutor(waypointManager);
            getCommand("waypoint").setTabCompleter(waypointManager);
        }
        if (shopManager != null) {
            getCommand("shop").setExecutor((sender, command, label, args) -> {
                if (sender instanceof Player player) {
                    shopManager.openShop(player);
                } else {
                    sender.sendMessage("§cOnly players can use this command!");
                }
                return true;
            });
        }
        if (sellManager != null) {
            getCommand("sell").setExecutor(sellManager);
            getCommand("sellgui").setExecutor(sellManager);
        }
        if (bountyManager != null) {
            getCommand("pay").setExecutor(bountyManager);
            getCommand("paytax").setExecutor(bountyManager);
            getCommand("bounty").setExecutor(bountyManager);
        }
        if (balanceManager != null) {
            getCommand("balance").setExecutor(balanceManager);
        }
        if (cosmeticsManager != null) {
            getCommand("cosmetics").setExecutor(cosmeticsManager);
        }
        getCommand("bundledupdate").setExecutor(updateManager);
        getCommand("bundledhelp").setExecutor(helpManager);
    }

    public static BundledEssential getInstance() {
        return instance;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }
}
