package com.bundleessential.giveaway;

import com.bundleessential.economy.BalanceManager;
import com.bundleessential.util.Money;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Console giveaway: /giveaway <amount> [count|all|<player>]
 * Gives money to random online winners. Console-friendly.
 */
public class GiveawayManager implements CommandExecutor {

    private final BalanceManager balance;

    public GiveawayManager(BalanceManager balance) {
        this.balance = balance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("bundleessential.giveaway") && !isConsole(sender)) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (balance == null) {
            sender.sendMessage("§cEconomy is off — giveaway unavailable.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /giveaway <amount> [count|all|<player>]");
            sender.sendMessage("§7Examples: §f/giveaway 100 §7— one random winner gets $100");
            sender.sendMessage("§7          §f/giveaway 50 5 §7— five random winners get $50 each");
            sender.sendMessage("§7          §f/giveaway 25 all §7— everyone online gets $25");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cAmount must be a number!");
            return true;
        }
        if (amount <= 0) {
            sender.sendMessage("§cAmount must be above 0!");
            return true;
        }
        amount = Math.round(amount * 100.0) / 100.0;

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) {
            sender.sendMessage("§cNo players online to give to!");
            return true;
        }

        List<Player> winners = new ArrayList<>();

        if (args.length >= 2) {
            String target = args[1];
            if (target.equalsIgnoreCase("all")) {
                winners.addAll(online);
            } else {
                // Try count first
                try {
                    int count = Integer.parseInt(target);
                    count = Math.max(1, Math.min(count, online.size()));
                    Collections.shuffle(online);
                    winners.addAll(online.subList(0, count));
                } catch (NumberFormatException ex) {
                    // Treat as player name
                    Player p = Bukkit.getPlayer(target);
                    if (p == null || !p.isOnline()) {
                        sender.sendMessage("§cPlayer '" + target + "' not found or offline!");
                        return true;
                    }
                    winners.add(p);
                }
            }
        } else {
            Collections.shuffle(online);
            winners.add(online.get(0));
        }

        for (Player p : winners) {
            balance.addBalance(p, amount);
            p.sendMessage("§6§l[GIVEAWAY] §aYou received §e$" + Money.format(amount) + "§a!");
        }

        String winnerNames = String.join(", ", winners.stream().map(Player::getName).toList());
        sender.sendMessage("§aGave §e$" + Money.format(amount) + " §asilently to " + winners.size() + " player(s): §f" + winnerNames);
        return true;
    }

    private static boolean isConsole(CommandSender sender) {
        return !(sender instanceof Player);
    }
}
