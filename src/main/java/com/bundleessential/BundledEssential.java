package com.bundleessential;

import com.bundleessential.autosell.AutoSellManager;
import com.bundleessential.back.BackManager;
import com.bundleessential.economy.BalanceManager;
import com.bundleessential.economy.BountyManager;
import com.bundleessential.economy.PriceManager;
import com.bundleessential.economy.SellManager;
import com.bundleessential.economy.ShopManager;
import com.bundleessential.giveaway.GiveawayManager;
import com.bundleessential.level.LevelManager;
import com.bundleessential.level.PlaytimeManager;
import com.bundleessential.light.DynamicLightManager;
import com.bundleessential.home.HomeManager;
import com.bundleessential.loan.LoanManager;
import com.bundleessential.rewards.RewardManager;
import com.bundleessential.spawner.SpawnerManager;
import com.bundleessential.tpa.TpaManager;
import com.bundleessential.trade.TradeManager;
import com.bundleessential.updater.UpdateManager;
import com.bundleessential.util.DataStorage;
import com.bundleessential.util.Features;
import com.bundleessential.util.HelpManager;
import com.bundleessential.util.Money;
import com.bundleessential.waypoint.WaypointManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BundledEssential extends JavaPlugin {

    private static BundledEssential instance;
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
        getLogger().info("BundledEssential has been enabled!");
    }

    @Override
    public void onDisable() {
        if (dataStorage != null) {
            dataStorage.saveAll();
        }
        if (levelManager != null) {
            levelManager.saveLevels();
        }
        if (rewardManager != null) {
            rewardManager.saveAll();
        }
        if (autosellManager != null) {
            autosellManager.saveAll();
        }
        if (spawnerManager != null) {
            spawnerManager.saveAll();
        }
        if (loanManager != null) loanManager.saveAll();
        if (balanceManager != null) {
            balanceManager.saveServerBank();
        }
        if (playtimeManager != null) {
            playtimeManager.savePlaytime();
        }
        if (dynamicLightManager != null) {
            dynamicLightManager.removeAll();
        }
        getLogger().info("BundledEssential has been disabled!");
    }

    private void registerCommands() {
        if (tpaManager != null) {
            getCommand("tpa").setExecutor(tpaManager);
            getCommand("tpa").setTabCompleter(tpaManager);
            getCommand("tpaccept").setExecutor(tpaManager);
            getCommand("tpahere").setExecutor(tpaManager);
            getCommand("tpahere").setTabCompleter(tpaManager);
            getCommand("tpaauto").setExecutor(tpaManager);
            getCommand("tpaauto").setTabCompleter(tpaManager);
        }
        if (homeManager != null) {
            getCommand("sethome").setExecutor(homeManager);
            getCommand("removehome").setExecutor(homeManager);
            getCommand("home").setExecutor(homeManager);
            getCommand("home").setTabCompleter(homeManager);
        }
        if (backManager != null) {
            getCommand("back").setExecutor(backManager);
        }
        if (tradeManager != null) {
            getCommand("trade").setExecutor(tradeManager);
            getCommand("trade").setTabCompleter(tradeManager);
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
                    if (args.length >= 1 && args[0].equalsIgnoreCase("search")) {
                        String query = args.length >= 2
                                ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                                : "";
                        shopManager.searchCommand(player, query);
                    } else {
                        shopManager.openShop(player);
                    }
                } else {
                    sender.sendMessage("§cOnly players can use this command!");
                }
                return true;
            });
            getCommand("shop").setTabCompleter((sender, cmd, alias, args) -> {
                java.util.List<String> s = new java.util.ArrayList<>();
                if (args.length == 1) s.add("search");
                String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
                s.removeIf(x -> !x.toLowerCase().startsWith(last));
                return s;
            });
        }
        if (sellManager != null) {
            getCommand("sell").setExecutor(sellManager);
            getCommand("sellgui").setExecutor(sellManager);
        }
        if (bountyManager != null && features.isEnabled("pay")) {
            getCommand("pay").setExecutor(bountyManager);
            getCommand("pay").setTabCompleter((sender, cmd, alias, args) -> {
                java.util.List<String> s = new java.util.ArrayList<>();
                if (args.length == 1) {
                    for (Player pl : Bukkit.getOnlinePlayers()) {
                        if (sender instanceof Player self && pl.equals(self)) continue;
                        s.add(pl.getName());
                    }
                } else if (args.length == 2) {
                    s.add("<amount>");
                }
                String last = args[args.length - 1].toLowerCase();
                s.removeIf(x -> x.startsWith("<") ? false : !x.toLowerCase().startsWith(last));
                return s;
            });
            getCommand("paytax").setExecutor(bountyManager);
        }
        if (bountyManager != null && features.isEnabled("bounty")) {
            getCommand("bounty").setExecutor(bountyManager);
            getCommand("bounty").setTabCompleter((sender, cmd, alias, args) -> {
                java.util.List<String> s = new java.util.ArrayList<>();
                if (args.length == 1) {
                    for (Player pl : Bukkit.getOnlinePlayers()) s.add(pl.getName());
                } else if (args.length == 2) {
                    s.add("<amount>");
                }
                String last = args[args.length - 1].toLowerCase();
                s.removeIf(x -> x.startsWith("<") ? false : !x.toLowerCase().startsWith(last));
                return s;
            });
        }
        if (balanceManager != null) {
            getCommand("balance").setExecutor(balanceManager);
            getCommand("balance").setTabCompleter(balanceManager);
            getCommand("resetbal").setExecutor(balanceManager);
            getCommand("resetbal").setTabCompleter(balanceManager);
            getCommand("resetserverbal").setExecutor((sender, cmd, alias, args) -> {
                if (!sender.isOp() && !sender.hasPermission("bundleessential.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                balanceManager.resetServerBank();
                sender.sendMessage("§aServer Bank reset to §e$0 §7(history cleared).");
                Bukkit.broadcastMessage("§6[Bank] §eServer Bank was reset by " + sender.getName());
                return true;
            });
            getCommand("serverbank").setExecutor((sender, cmd, alias, args) -> {
                if (args.length >= 1 && args[0].equalsIgnoreCase("history")) {
                    if (sender instanceof Player player) {
                        org.bukkit.inventory.ItemStack book = balanceManager.historyBook();
                        if (player.getInventory().firstEmpty() == -1) {
                            player.getWorld().dropItemNaturally(player.getLocation(), book);
                            player.sendMessage("§eInventory full — history book dropped!");
                        } else {
                            player.getInventory().addItem(book);
                            player.sendMessage("§aOpened Server Bank history book!");
                        }
                    } else {
                        sender.sendMessage("§6§lServer Bank: §a$" + Money.format(balanceManager.getServerBank()) + " §7(" + balanceManager.getBankHistory().size() + " txns)");
                        int shown = 0;
                        for (int i = balanceManager.getBankHistory().size() - 1; i >= 0 && shown < 10; i--) {
                            com.google.gson.JsonObject e = balanceManager.getBankHistory().get(i);
                            String t = new java.text.SimpleDateFormat("MM-dd HH:mm").format(new java.util.Date(e.get("time").getAsLong()));
                            sender.sendMessage(" " + t + " " + e.get("player").getAsString() + " $" + Money.format(e.get("amount").getAsDouble()) + " " + e.get("reason").getAsString());
                            shown++;
                        }
                    }
                    return true;
                }
                sender.sendMessage("§6§lServer Bank: §a$" + Money.format(balanceManager.getServerBank()));
                sender.sendMessage("§7History: §e/serverbank history §7(book, also works for console)");
                if (sender.isOp() || sender.hasPermission("bundleessential.admin")) {
                    sender.sendMessage("§7Cap: §e$" + Money.format(balanceManager.getCapPublic()));
                    sender.sendMessage("§7Overflow + garnish + bounty/pay taxes feed the bank. Config: economy.balance-cap");
                    sender.sendMessage("§7Random interest: 30% every 20 min, 0.2-0.5% of bank (max $5000) → online");
                }
                return true;
            });
            getCommand("serverbank").setTabCompleter((sender, c, a, args) -> {
                java.util.List<String> s = new java.util.ArrayList<>();
                if (args.length == 1) s.add("history");
                String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
                s.removeIf(x -> !x.toLowerCase().startsWith(last));
                return s;
            });
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
                        player.sendMessage("§aRepaired to full durability for §e$" + Money.format(cost));
                    } else {
                        player.sendMessage("§cNot enough money! Need $" + Money.format(cost));
                    }
                } else {
                    double singlePct = 1.0 / maxDur;
                    double singleCost = Math.round(baseCost * singlePct * 100.0) / 100.0;
                    if (singleCost < 0.10) singleCost = 0.10;
                    if (balanceManager.removeBalance(player, singleCost)) {
                        item.setDurability((short) Math.max(0, dur - 1));
                        player.sendMessage("§aRepaired 1% durability for §e$" + Money.format(singleCost));
                    } else {
                        player.sendMessage("§cNot enough money! Need $" + Money.format(singleCost));
                    }
                }
                return true;
            });
        }
        if (levelManager != null) {
            getCommand("level").setExecutor(levelManager);
            getCommand("level").setTabCompleter((sender, cmd, alias, args) -> {
                java.util.List<String> s = new java.util.ArrayList<>();
                if (args.length == 1) {
                    for (Player pl : Bukkit.getOnlinePlayers()) s.add(pl.getName());
                }
                String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
                s.removeIf(x -> !x.toLowerCase().startsWith(last));
                return s;
            });
        }
        if (rewardManager != null) {
            getCommand("quest").setExecutor(rewardManager);
            getCommand("quest").setTabCompleter((sender, cmd, alias, args) -> {
                java.util.List<String> s = new java.util.ArrayList<>();
                if (args.length == 1) {
                    s.add("claim");
                    s.add("skip");
                }
                String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
                s.removeIf(x -> !x.toLowerCase().startsWith(last));
                return s;
            });
            getCommand("daily").setExecutor(rewardManager);
        }
        if (autosellManager != null) {
            getCommand("autosell").setExecutor(autosellManager);
            getCommand("autosell").setTabCompleter((sender, cmd, alias, args) -> {
                java.util.List<String> s = new java.util.ArrayList<>();
                if (args.length == 1) s.add("give");
                else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                    for (Player pl : Bukkit.getOnlinePlayers()) s.add(pl.getName());
                } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                    s.add("<amount>");
                }
                String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
                s.removeIf(x -> x.startsWith("<") ? false : !x.toLowerCase().startsWith(last));
                return s;
            });
        }
        if (loanManager != null) {
            getCommand("loan").setExecutor(loanManager);
            getCommand("loan").setTabCompleter(loanManager);
        }
        if (giveawayManager != null) {
            getCommand("giveaway").setExecutor(giveawayManager);
            getCommand("giveaway").setTabCompleter((sender, cmd, alias, args) -> {
                java.util.List<String> s = new java.util.ArrayList<>();
                if (args.length == 1) s.add("<amount>");
                else if (args.length == 2) {
                    s.add("all");
                    s.add("<count>");
                    for (Player pl : Bukkit.getOnlinePlayers()) s.add(pl.getName());
                }
                String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
                s.removeIf(x -> x.startsWith("<") ? false : !x.toLowerCase().startsWith(last));
                return s;
            });
        }
        if (playtimeManager != null) {
            getCommand("playtime").setExecutor((sender, command, label, args) -> {
                if (args.length >= 1 && sender instanceof Player p) {
                    String sub = args[0].toLowerCase();
                    if (sub.equals("optout") || sub.equals("opt-out") || sub.equals("out")) {
                        if (balanceManager != null && balanceManager.isPlaytimeOptOut(p.getUniqueId())) {
                            p.sendMessage("§eAlready opted out. Vault: §a$"
                                    + Money.format(balanceManager.getPlaytimeVault(p.getUniqueId())));
                            return true;
                        }
                        if (balanceManager != null) {
                            balanceManager.setPlaytimeOptOut(p, true);
                            p.sendMessage("§ePlaytime pay §copted OUT§e. Earnings go silently to your vault. §6/playtime optin §eto claim.");
                        }
                        return true;
                    }
                    if (sub.equals("optin") || sub.equals("opt-in") || sub.equals("in")) {
                        if (balanceManager != null) {
                            double claimed = balanceManager.setPlaytimeOptOut(p, false);
                            if (claimed > 0) {
                                p.sendMessage("§aOpted IN! Claimed vault §e$" + Money.format(claimed));
                            } else {
                                p.sendMessage("§aOpted IN! No vaulted earnings.");
                            }
                        }
                        return true;
                    }
                    if (sub.equals("vault")) {
                        if (balanceManager != null) {
                            p.sendMessage("§6Vault: §a$" + Money.format(balanceManager.getPlaytimeVault(p.getUniqueId()))
                                    + " §7(" + (balanceManager.isPlaytimeOptOut(p.getUniqueId()) ? "opted out" : "opted in") + ")");
                        }
                        return true;
                    }
                }
                return playtimeManager.onCommand(sender, command, label, args);
            });
            getCommand("playtime").setTabCompleter((sender, cmd, alias, args) -> {
                java.util.List<String> s = new java.util.ArrayList<>();
                if (args.length == 1) {
                    s.add("leaderboard");
                    s.add("top");
                    s.add("optin");
                    s.add("optout");
                    s.add("vault");
                    for (Player pl : Bukkit.getOnlinePlayers()) s.add(pl.getName());
                }
                String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
                s.removeIf(x -> !x.toLowerCase().startsWith(last));
                return s;
            });
        }
        if (updateManager != null) {
            getCommand("bundledupdate").setExecutor(updateManager);
        }
        getCommand("bundledhelp").setExecutor(helpManager);
        getCommand("bundleversion").setExecutor((sender, command, label, args) -> {
            sender.sendMessage("§6§lBundledEssential §e v" + getDescription().getVersion());
            return true;
        });
        // Consolidated hub: /be help|version|update (old roots still work)
        try {
            if (getCommand("be") != null) {
                getCommand("be").setExecutor((sender, command, label, args) -> {
                    if (args.length == 0) {
                        sender.sendMessage("§cUsage: /be <help|version|update>");
                        return true;
                    }
                    String sub = args[0].toLowerCase();
                    if (sub.equals("help")) {
                        return helpManager.onCommand(sender, getCommand("bundledhelp"), "bundledhelp", new String[0]);
                    }
                    if (sub.equals("version")) {
                        sender.sendMessage("§6§lBundledEssential §e v" + getDescription().getVersion());
                        return true;
                    }
                    if (sub.equals("update")) {
                        if (updateManager != null) {
                            return updateManager.onCommand(sender, getCommand("bundledupdate"), "bundledupdate",
                                    args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[0]);
                        }
                        sender.sendMessage("§cUpdater is disabled!");
                        return true;
                    }
                    sender.sendMessage("§cUsage: /be <help|version|update>");
                    return true;
                });
                getCommand("be").setTabCompleter((sender, cmd, alias, args) -> {
                    java.util.List<String> s = new java.util.ArrayList<>();
                    if (args.length == 1) {
                        s.add("help");
                        s.add("version");
                        s.add("update");
                    }
                    String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
                    s.removeIf(x -> !x.toLowerCase().startsWith(last));
                    return s;
                });
            }
        } catch (Exception ignored) {}
        // /craft portable workbench
        try {
            if (getCommand("craft") != null) {
                getCommand("craft").setExecutor((sender, command, label, args) -> {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("§cOnly players can use /craft");
                        return true;
                    }
                    player.openWorkbench(null, true);
                    return true;
                });
            }
        } catch (Exception ignored) {}
        // Reload config.yml + features.yml live (console + admins, no restart)
        try {
            if (getCommand("bundledreload") != null) {
                getCommand("bundledreload").setExecutor((sender, command, label, args) -> {
                    if (!sender.isOp() && !sender.hasPermission("bundleessential.admin") && !sender.hasPermission("bundleessential.reload") && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
                        sender.sendMessage("§cNo permission.");
                        return true;
                    }
                    try {
                        reloadConfig();
                        // re-save defaults so new keys like economy.balance-cap appear
                        saveDefaultConfig();
                        // Features is file-backed; reload by recreating (toggles are isEnabled checks)
                        features = new com.bundleessential.util.Features(this);
                        sender.sendMessage("§aBundledEssential config reloaded!");
                        sender.sendMessage("§7balance-cap: §e$" + com.bundleessential.util.Money.format(getConfig().getDouble("economy.balance-cap", 1_000_000_000_000.0)));
                        sender.sendMessage("§7Server Bank: §a$" + com.bundleessential.util.Money.format(balanceManager != null ? balanceManager.getServerBank() : 0));
                    } catch (Exception e) {
                        sender.sendMessage("§cReload failed: " + e.getMessage());
                    }
                    return true;
                });
            }
        } catch (Exception ignored) {}
    }

    public static BundledEssential getInstance() {
        return instance;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }
}
