package com.bundleessential;

import com.bundleessential.back.BackManager;
import com.bundleessential.cosmetics.CosmeticsManager;
import com.bundleessential.economy.BalanceManager;
import com.bundleessential.economy.BountyManager;
import com.bundleessential.economy.PriceManager;
import com.bundleessential.economy.SellManager;
import com.bundleessential.economy.ShopManager;
import com.bundleessential.level.LevelManager;
import com.bundleessential.level.PlaytimeManager;
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
    private LevelManager levelManager;
    private PlaytimeManager playtimeManager;
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
            if (getConfig().getBoolean("leveling.enabled", true)) {
                levelManager = new LevelManager(this);
                balanceManager.setLevelManager(levelManager);
            }
            if (getConfig().getBoolean("playtime.enabled", true)) {
                playtimeManager = new PlaytimeManager(this);
            }
        }
        if (getConfig().getBoolean("cosmetics.enabled", false) && balanceManager != null) {
            cosmeticsManager = new CosmeticsManager(this, balanceManager);
        }

        updateManager = new UpdateManager(this);
        helpManager = new HelpManager();

        if (balanceManager != null) Bukkit.getPluginManager().registerEvents(balanceManager, this);
        if (shopManager != null) Bukkit.getPluginManager().registerEvents(shopManager, this);
        if (levelManager != null) Bukkit.getPluginManager().registerEvents(levelManager, this);
        if (playtimeManager != null) Bukkit.getPluginManager().registerEvents(playtimeManager, this);
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
        if (levelManager != null) {
            levelManager.saveLevels();
        }
        if (playtimeManager != null) {
            playtimeManager.savePlaytime();
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
            getCommand("repair").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can use this command!");
                    return true;
                }
                if (balanceManager == null) {
                    player.sendMessage("§cEconomy is disabled!");
                    return true;
                }
                org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == org.bukkit.Material.AIR) {
                    player.sendMessage("§cYou are not holding anything!");
                    return true;
                }
                if (item.getType().getMaxDurability() <= 0) {
                    player.sendMessage("§cThis item cannot be repaired!");
                    return true;
                }
                int maxDur = item.getType().getMaxDurability();
                int dur = item.getDurability();
                if (dur == 0) {
                    player.sendMessage("§aItem is already at full durability!");
                    return true;
                }
                double durabilityPct = (double) dur / maxDur;
                double baseCost = 5.0;
                double cost = Math.round(baseCost * durabilityPct * 100.0) / 100.0;
                if (cost < 0.50) cost = 0.50;
                if (args.length > 0 && args[0].equalsIgnoreCase("full")) {
                    if (balanceManager.removeBalance(player, cost)) {
                        item.setDurability((short) 0);
                        player.sendMessage("§aRepaired to full durability for §e$" + String.format("%.2f", cost));
                    } else {
                        player.sendMessage("§cNot enough money! Need $" + String.format("%.2f", cost));
                    }
                } else {
                    double singlePct = 1.0 / maxDur;
                    double singleCost = Math.round(baseCost * singlePct * 100.0) / 100.0;
                    if (singleCost < 0.10) singleCost = 0.10;
                    if (balanceManager.removeBalance(player, singleCost)) {
                        item.setDurability((short) Math.max(0, dur - 1));
                        player.sendMessage("§aRepaired 1% durability for §e$" + String.format("%.2f", singleCost));
                    } else {
                        player.sendMessage("§cNot enough money! Need $" + String.format("%.2f", singleCost));
                    }
                }
                return true;
            });
        }
        if (levelManager != null) {
            getCommand("level").setExecutor(levelManager);
        }
        if (playtimeManager != null) {
            getCommand("playtime").setExecutor(playtimeManager);
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
