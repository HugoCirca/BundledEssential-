package com.bundleessential;

import com.bundleessential.back.BackManager;
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

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        dataStorage = new DataStorage(this);
        tpaManager = new TpaManager(this);
        homeManager = new HomeManager(this);
        backManager = new BackManager(this);
        tradeManager = new TradeManager(this);
        updateManager = new UpdateManager(this);
        waypointManager = new WaypointManager(this);
        balanceManager = new BalanceManager(this);
        priceManager = new PriceManager(this);
        bountyManager = new BountyManager(balanceManager);
        balanceManager.setBountyManager(bountyManager);
        shopManager = new ShopManager(balanceManager, priceManager);
        sellManager = new SellManager(balanceManager, priceManager);
        helpManager = new HelpManager();

        Bukkit.getPluginManager().registerEvents(balanceManager, this);
        Bukkit.getPluginManager().registerEvents(shopManager, this);
        Bukkit.getPluginManager().registerEvents(sellManager, this);

        registerCommands();
        updateManager.startup();

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

        getCommand("trade").setExecutor(tradeManager);
        getCommand("tradeaccept").setExecutor(tradeManager);
        getCommand("tradecancel").setExecutor(tradeManager);

        getCommand("waypoint").setExecutor(waypointManager);
        getCommand("waypoint").setTabCompleter(waypointManager);

        getCommand("shop").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                shopManager.openShop(player);
            } else {
                sender.sendMessage("§cOnly players can use this command!");
            }
            return true;
        });

        getCommand("sell").setExecutor(sellManager);
        getCommand("sellgui").setExecutor(sellManager);

        getCommand("pay").setExecutor(bountyManager);
        getCommand("bounty").setExecutor(bountyManager);
        getCommand("balance").setExecutor(balanceManager);

        getCommand("bundledhelp").setExecutor(helpManager);
    }

    public static BundledEssential getInstance() {
        return instance;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }
}
