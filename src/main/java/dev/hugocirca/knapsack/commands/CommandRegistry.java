package dev.hugocirca.knapsack.commands;

import dev.hugocirca.knapsack.KnapsackPlugin;
import dev.hugocirca.knapsack.economy.BalanceManager;
import dev.hugocirca.knapsack.economy.BountyManager;
import dev.hugocirca.knapsack.economy.SellManager;
import dev.hugocirca.knapsack.economy.ShopManager;
import dev.hugocirca.knapsack.level.LevelManager;
import dev.hugocirca.knapsack.level.PlaytimeManager;
import dev.hugocirca.knapsack.rewards.RewardManager;
import dev.hugocirca.knapsack.util.HelpManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralizes the 200+ lines of inline lambda command wiring that lived in KnapsackPlugin.
 * Each command is now a named class for testability and readability.
 */
public final class CommandRegistry {

    private CommandRegistry() {}

    public static void register(KnapsackPlugin plugin,
                                BalanceManager balance, BountyManager bounty,
                                ShopManager shop, SellManager sell,
                                LevelManager level, RewardManager rewards,
                                PlaytimeManager playtime,
                                dev.hugocirca.knapsack.autosell.AutoSellManager autosell,
                                dev.hugocirca.knapsack.loan.LoanManager loan,
                                dev.hugocirca.knapsack.giveaway.GiveawayManager giveaway,
                                dev.hugocirca.knapsack.home.HomeManager home,
                                dev.hugocirca.knapsack.back.BackManager back,
                                dev.hugocirca.knapsack.trade.TradeManager trade,
                                dev.hugocirca.knapsack.tpa.TpaManager tpa,
                                dev.hugocirca.knapsack.waypoint.WaypointManager waypoints,
                                dev.hugocirca.knapsack.updater.UpdateManager updater,
                                HelpManager help) {

        if (tpa != null) {
            plugin.getCommand("tpa").setExecutor(tpa);
            plugin.getCommand("tpa").setTabCompleter(tpa);
            plugin.getCommand("tpahere").setExecutor(tpa);
            plugin.getCommand("tpahere").setTabCompleter(tpa);
        }
        if (home != null) {
            plugin.getCommand("sethome").setExecutor(home);
            plugin.getCommand("removehome").setExecutor(home);
            plugin.getCommand("home").setExecutor(home);
            plugin.getCommand("home").setTabCompleter(home);
        }
        if (back != null) plugin.getCommand("back").setExecutor(back);
        if (trade != null) {
            plugin.getCommand("trade").setExecutor(trade);
            plugin.getCommand("trade").setTabCompleter(trade);
            plugin.getCommand("tradeaccept").setExecutor(trade);
            plugin.getCommand("tradecancel").setExecutor(trade);
        }
        if (waypoints != null) {
            plugin.getCommand("waypoint").setExecutor(waypoints);
            plugin.getCommand("waypoint").setTabCompleter(waypoints);
        }
        if (shop != null) {
            ShopCommand sc = new ShopCommand(shop);
            plugin.getCommand("shop").setExecutor(sc);
            plugin.getCommand("shop").setTabCompleter(sc);
        }
        if (sell != null) {
            plugin.getCommand("sell").setExecutor(sell);
            plugin.getCommand("sellgui").setExecutor(sell);
        }
        if (bounty != null && plugin.getFeatures().isEnabled("pay")) {
            plugin.getCommand("pay").setExecutor(bounty);
            plugin.getCommand("pay").setTabCompleter(new PayCommand());
            plugin.getCommand("paytax").setExecutor(bounty);
        }
        if (bounty != null && plugin.getFeatures().isEnabled("bounty")) {
            plugin.getCommand("bounty").setExecutor(bounty);
            plugin.getCommand("bounty").setTabCompleter(new BountyTab());
        }
        if (balance != null) {
            plugin.getCommand("balance").setExecutor(balance);
            plugin.getCommand("balance").setTabCompleter(balance);
            plugin.getCommand("resetbal").setExecutor(balance);
            plugin.getCommand("resetbal").setTabCompleter(balance);
            plugin.getCommand("resetserverbal").setExecutor(new ResetServerBankCommand(balance));
            ServerBankCommand sbc = new ServerBankCommand(balance);
            plugin.getCommand("serverbank").setExecutor(sbc);
            plugin.getCommand("serverbank").setTabCompleter(sbc);
            plugin.getCommand("repair").setExecutor(new RepairCommand(balance));
        }
        if (level != null) {
            plugin.getCommand("level").setExecutor(level);
            plugin.getCommand("level").setTabCompleter((sender, cmd, alias, args) -> {
                List<String> s = new ArrayList<>();
                if (args.length == 1) for (Player pl : Bukkit.getOnlinePlayers()) s.add(pl.getName());
                String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
                s.removeIf(x -> !x.toLowerCase().startsWith(last));
                return s;
            });
        }
        if (rewards != null) {
            plugin.getCommand("quest").setExecutor(rewards);
            plugin.getCommand("quest").setTabCompleter((sender, cmd, alias, args) -> {
                List<String> s = new ArrayList<>();
                if (args.length == 1) { s.add("claim"); s.add("skip"); }
                String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
                s.removeIf(x -> !x.toLowerCase().startsWith(last));
                return s;
            });
            plugin.getCommand("daily").setExecutor(rewards);
        }
        if (autosell != null) {
            plugin.getCommand("autosell").setExecutor(autosell);
            plugin.getCommand("autosell").setTabCompleter((sender, cmd, alias, args) -> {
                List<String> s = new ArrayList<>();
                if (args.length == 1) s.add("give");
                else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                    for (Player pl : Bukkit.getOnlinePlayers()) s.add(pl.getName());
                } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) s.add("<amount>");
                String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
                s.removeIf(x -> x.startsWith("<") ? false : !x.toLowerCase().startsWith(last));
                return s;
            });
        }
        if (loan != null) {
            plugin.getCommand("loan").setExecutor(loan);
            plugin.getCommand("loan").setTabCompleter(loan);
        }
        if (giveaway != null) {
            plugin.getCommand("giveaway").setExecutor(giveaway);
            plugin.getCommand("giveaway").setTabCompleter((sender, cmd, alias, args) -> {
                List<String> s = new ArrayList<>();
                if (args.length == 1) s.add("<amount>");
                else if (args.length == 2) { s.add("all"); s.add("<count>"); for (Player pl : Bukkit.getOnlinePlayers()) s.add(pl.getName()); }
                String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
                s.removeIf(x -> x.startsWith("<") ? false : !x.toLowerCase().startsWith(last));
                return s;
            });
        }
        if (playtime != null) {
            PlaytimeCommand pc = new PlaytimeCommand(playtime, balance);
            plugin.getCommand("playtime").setExecutor(pc);
            plugin.getCommand("playtime").setTabCompleter(pc);
        }
        if (updater != null) plugin.getCommand("bundledupdate").setExecutor(updater);

        plugin.getCommand("bundledhelp").setExecutor(help);
        plugin.getCommand("bundleversion").setExecutor((sender, cmd, alias, args) -> {
            sender.sendMessage("§6§lBundledEssential §e v" + plugin.getDescription().getVersion());
            return true;
        });

        try {
            if (plugin.getCommand("be") != null) {
                BeCommand be = new BeCommand(help, updater, plugin.getDescription().getVersion());
                plugin.getCommand("be").setExecutor(be);
                plugin.getCommand("be").setTabCompleter(be);
            }
        } catch (Exception ignored) {}

        try {
            if (plugin.getCommand("craft") != null) plugin.getCommand("craft").setExecutor(new CraftCommand());
        } catch (Exception ignored) {}

        try {
            if (plugin.getCommand("bundledreload") != null) {
                plugin.getCommand("bundledreload").setExecutor(new BundledReloadCommand(plugin, balance));
            }
        } catch (Exception ignored) {}
    }
}
