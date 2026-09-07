package dev.hugocirca.knapsack.commands;

import dev.hugocirca.knapsack.economy.BalanceManager;
import dev.hugocirca.knapsack.level.PlaytimeManager;
import dev.hugocirca.knapsack.util.Money;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Wraps PlaytimeManager vault logic extracted from KnapsackPlugin lambda. */
public class PlaytimeCommand implements CommandExecutor, TabCompleter {

    private final PlaytimeManager playtime;
    private final BalanceManager balance;

    public PlaytimeCommand(PlaytimeManager playtime, BalanceManager balance) {
        this.playtime = playtime;
        this.balance = balance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && sender instanceof Player p) {
            String sub = args[0].toLowerCase();
            if (sub.equals("optout") || sub.equals("opt-out") || sub.equals("out")) {
                if (balance != null && balance.isPlaytimeOptOut(p.getUniqueId())) {
                    p.sendMessage("§eAlready opted out. Vault: §a$" + Money.format(balance.getPlaytimeVault(p.getUniqueId())));
                    return true;
                }
                if (balance != null) {
                    balance.setPlaytimeOptOut(p, true);
                    p.sendMessage("§ePlaytime pay §copted OUT§e. Earnings go silently to your vault. §6/playtime optin §eto claim.");
                }
                return true;
            }
            if (sub.equals("optin") || sub.equals("opt-in") || sub.equals("in")) {
                if (balance != null) {
                    double claimed = balance.setPlaytimeOptOut(p, false);
                    if (claimed > 0) p.sendMessage("§aOpted IN! Claimed vault §e$" + Money.format(claimed));
                    else p.sendMessage("§aOpted IN! No vaulted earnings.");
                }
                return true;
            }
            if (sub.equals("vault")) {
                if (balance != null) {
                    p.sendMessage("§6Vault: §a$" + Money.format(balance.getPlaytimeVault(p.getUniqueId()))
                            + " §7(" + (balance.isPlaytimeOptOut(p.getUniqueId()) ? "opted out" : "opted in") + ")");
                }
                return true;
            }
        }
        return playtime.onCommand(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> s = new ArrayList<>();
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
    }
}
