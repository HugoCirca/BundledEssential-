package dev.hugocirca.knapsack;

import dev.hugocirca.knapsack.autosell.AutoSellManager;
import dev.hugocirca.knapsack.back.BackManager;
import dev.hugocirca.knapsack.economy.BalanceManager;
import dev.hugocirca.knapsack.economy.BountyManager;
import dev.hugocirca.knapsack.economy.PriceManager;
import dev.hugocirca.knapsack.economy.SellManager;
import dev.hugocirca.knapsack.economy.ShopManager;
import dev.hugocirca.knapsack.giveaway.GiveawayManager;
import dev.hugocirca.knapsack.level.LevelManager;
import dev.hugocirca.knapsack.level.PlaytimeManager;
import dev.hugocirca.knapsack.light.DynamicLightManager;
import dev.hugocirca.knapsack.home.HomeManager;
import dev.hugocirca.knapsack.loan.LoanManager;
import dev.hugocirca.knapsack.rewards.RewardManager;
import dev.hugocirca.knapsack.spawner.SpawnerManager;
import dev.hugocirca.knapsack.tpa.TpaManager;
import dev.hugocirca.knapsack.trade.TradeManager;
import dev.hugocirca.knapsack.updater.UpdateManager;
import dev.hugocirca.knapsack.util.DataStorage;
import dev.hugocirca.knapsack.util.Features;
import dev.hugocirca.knapsack.util.HelpManager;
import dev.hugocirca.knapsack.util.Money;
import dev.hugocirca.knapsack.waypoint.WaypointManager;
import dev.hugocirca.knapsack.commands.CommandRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class KnapsackPlugin extends JavaPlugin {

    private static KnapsackPlugin instance;
    private DataStorage dataStorage;
    private Features features;
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
    private RewardManager rewardManager;
    private AutoSellManager autosellManager;
    private SpawnerManager spawnerManager;
    private GiveawayManager giveawayManager;
    private PlaytimeManager playtimeManager;
    private DynamicLightManager dynamicLightManager;
    private SellManager sellManager;
    private LoanManager loanManager;

    @Override
    public void onEnable() {
        instance = this;
        migrateLegacyData(); // P7B: silent copy BundledEssential → Knapsack
        saveDefaultConfig();

        dataStorage = new DataStorage(this);
        features = new Features(this);

        if (features.isEnabled("tpa")) {
            tpaManager = new TpaManager(this);
        }
        if (features.isEnabled("home")) {
            homeManager = new HomeManager(this);
        }
        if (features.isEnabled("back")) {
            backManager = new BackManager(this);
        }
        if (features.isEnabled("trade")) {
            tradeManager = new TradeManager(this);
        }
        if (features.isEnabled("waypoints")) {
            waypointManager = new WaypointManager(this);
        }
        if (features.isEnabled("economy")) {
            balanceManager = new BalanceManager(this);
            priceManager = new PriceManager(this);
            if (features.isEnabled("bounty") || features.isEnabled("pay")) {
                bountyManager = new BountyManager(balanceManager);
                balanceManager.setBountyManager(bountyManager);
            }
            if (features.isEnabled("sell")) {
                sellManager = new SellManager(balanceManager, priceManager);
            }
            if (features.isEnabled("shop")) {
                shopManager = new ShopManager(balanceManager, priceManager, sellManager);
            }
            if (features.isEnabled("leveling")) {
                levelManager = new LevelManager(this);
                balanceManager.setLevelManager(levelManager);
            }
            if (features.isEnabled("rewards")) {
                rewardManager = new RewardManager(this, balanceManager, bountyManager);
            }
            if (features.isEnabled("autosell")) {
                autosellManager = new AutoSellManager(this, balanceManager, priceManager);
            }
            if (features.isEnabled("spawner")) {
                spawnerManager = new SpawnerManager(this);
            }
            if (balanceManager != null) {
                loanManager = new LoanManager(this, balanceManager);
                balanceManager.setLoanManager(loanManager);
                giveawayManager = new GiveawayManager(balanceManager);
            }
            if (features.isEnabled("playtime")) {
                playtimeManager = new PlaytimeManager(this);
            }
            if (features.isEnabled("dynamic-light")) {
                dynamicLightManager = new DynamicLightManager(this);
            }
        }
        helpManager = new HelpManager();
        if (features.isEnabled("updater")) {
            updateManager = new UpdateManager(this);
        }

        if (balanceManager != null) Bukkit.getPluginManager().registerEvents(balanceManager, this);
        if (shopManager != null) Bukkit.getPluginManager().registerEvents(shopManager, this);
        if (levelManager != null) Bukkit.getPluginManager().registerEvents(levelManager, this);
        if (rewardManager != null) Bukkit.getPluginManager().registerEvents(rewardManager, this);
        if (autosellManager != null) Bukkit.getPluginManager().registerEvents(autosellManager, this);
        if (spawnerManager != null) Bukkit.getPluginManager().registerEvents(spawnerManager, this);
        if (playtimeManager != null) Bukkit.getPluginManager().registerEvents(playtimeManager, this);
        if (sellManager != null) Bukkit.getPluginManager().registerEvents(sellManager, this);
        if (tradeManager != null) Bukkit.getPluginManager().registerEvents(tradeManager, this);
        if (dynamicLightManager != null) Bukkit.getPluginManager().registerEvents(dynamicLightManager, this);

        registerCommands();
        if (updateManager != null) updateManager.startup();

        getLogger().info("Registered commands: " + String.join(", ", getDescription().getCommands().keySet()));
        getLogger().info("KnapsackPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (dataStorage != null) dataStorage.saveAll();
        // All persistence now goes through Saveable + JsonStorage; one call is enough
        if (levelManager != null) levelManager.saveAll();
        if (rewardManager != null) rewardManager.saveAll();
        if (autosellManager != null) autosellManager.saveAll();
        if (spawnerManager != null) spawnerManager.saveAll();
        if (loanManager != null) loanManager.saveAll();
        if (balanceManager != null) balanceManager.saveAll();
        if (playtimeManager != null) playtimeManager.saveAll();
        if (tpaManager != null) tpaManager.saveAll();
        if (priceManager != null) priceManager.saveAll();
        if (dynamicLightManager != null) dynamicLightManager.removeAll();
        getLogger().info("KnapsackPlugin has been disabled!");
    }

    private void registerCommands() {
        CommandRegistry.register(this,
                balanceManager, bountyManager, shopManager, sellManager,
                levelManager, rewardManager, playtimeManager,
                autosellManager, loanManager, giveawayManager,
                homeManager, backManager, tradeManager, tpaManager,
                waypointManager, updateManager, helpManager);
    }

    private void migrateLegacyData() {
        try {
            java.io.File newFolder = getDataFolder(); // plugins/Knapsack
            java.io.File legacy = new java.io.File(newFolder.getParentFile(), "BundledEssential");
            if (!legacy.exists() || !legacy.isDirectory()) return;
            if (newFolder.exists()) {
                String[] existing = newFolder.list();
                if (existing != null && existing.length > 0) return; // already has data, don't overwrite
            }
            getLogger().info("Migrating legacy data from BundledEssential → Knapsack...");
            newFolder.mkdirs();
            java.nio.file.Files.walk(legacy.toPath()).forEach(src -> {
                try {
                    java.nio.file.Path dest = newFolder.toPath().resolve(legacy.toPath().relativize(src));
                    if (java.nio.file.Files.isDirectory(src)) {
                        java.nio.file.Files.createDirectories(dest);
                    } else {
                        java.nio.file.Files.copy(src, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception ignored) {}
            });
            getLogger().info("Legacy data migration complete (kept " + legacy.getName() + " intact).");
        } catch (Exception e) {
            getLogger().warning("Legacy migration failed: " + e.getMessage());
        }
    }

    public Features getFeatures() { return features; }
    public void setFeatures(Features f) { this.features = f; }

    public static KnapsackPlugin getInstance() {
        return instance;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }
}
