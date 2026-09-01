package com.bundleessential.economy;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BountyManager implements CommandExecutor {

    private final BalanceManager balanceManager;
    private final Map<UUID, Double> bounties = new HashMap<>();
    private final Map<UUID, Double> unpaidTaxes = new HashMap<>();
    private final Map<UUID, Long> lastTaxTime = new HashMap<>();

    private static final double BOUNTY_TAX_RATE = 0.20;
    private static final double PAY_TAX_RATE = 0.05;
    private static final long WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L;

    public BountyManager(BalanceManager balanceManager) {
        this.balanceManager = balanceManager;
        startTaxReminder();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "pay" -> {
                if (args.length != 2) {
                    sender.sendMessage("§cUsage: /pay <player> <amount>");
                    return true;
                }
                handlePay(sender, args[0], args[1]);
            }
            case "paytax" -> handlePayTax(sender);
            case "bounty" -> {
                if (args.length == 1) {
                    handleBountyCheck(sender, args[0]);
                } else if (args.length == 2) {
                    handleBountySet(sender, args[0], args[1]);
                } else {
                    sender.sendMessage("§cUsage: /bounty <player> [amount]");
                    return true;
                }
            }
        }
        return true;
    }

    private void handlePay(CommandSender sender, String targetName, String amountStr) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§cPlayer not found or offline!");
            return;
        }
        if (target.equals(player)) {
            player.sendMessage("§cYou cannot pay yourself!");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount!");
            return;
        }
        if (amount <= 0) {
            player.sendMessage("§cAmount must be positive!");
            return;
        }

        double tax = Math.round(amount * PAY_TAX_RATE * 100.0) / 100.0;
        double afterTax = Math.round((amount - tax) * 100.0) / 100.0;

        if (balanceManager.removeBalance(player, amount)) {
            balanceManager.addBalance(target, afterTax);
            unpaidTaxes.merge(player.getUniqueId(), tax, Double::sum);
            player.sendMessage("§aYou paid §e$" + String.format("%.2f", amount) + " §ato §e" + target.getName() + " §7(tax: $" + String.format("%.2f", tax) + ")");
            target.sendMessage("§aYou received §e$" + String.format("%.2f", afterTax) + " §afrom §e" + player.getName());
        } else {
            player.sendMessage("§cNot enough money!");
        }
    }

    private void handlePayTax(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        double owed = unpaidTaxes.getOrDefault(player.getUniqueId(), 0.0);
        if (owed <= 0) {
            player.sendMessage("§aYou have no taxes to pay!");
            return;
        }

        if (balanceManager.removeBalance(player, owed)) {
            unpaidTaxes.remove(player.getUniqueId());
            lastTaxTime.put(player.getUniqueId(), System.currentTimeMillis());
            player.sendMessage("§aYou paid §e$" + String.format("%.2f", owed) + " §ain taxes!");
        } else {
            player.sendMessage("§cNot enough money! You owe §e$" + String.format("%.2f", owed) + "§c in taxes.");
        }
    }

    private void handleBountyCheck(CommandSender sender, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found or offline!");
            return;
        }

        double bounty = bounties.getOrDefault(target.getUniqueId(), 0.0);
        if (bounty > 0) {
            sender.sendMessage("§e" + target.getName() + " §chas a bounty of §a$" + String.format("%.2f", bounty));
        } else {
            sender.sendMessage("§e" + target.getName() + " §7has no bounty.");
        }
    }

    private void handleBountySet(CommandSender sender, String targetName, String amountStr) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§cPlayer not found or offline!");
            return;
        }
        if (target.equals(player)) {
            player.sendMessage("§cYou cannot set a bounty on yourself!");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount!");
            return;
        }
        if (amount <= 0) {
            player.sendMessage("§cAmount must be positive!");
            return;
        }

        if (balanceManager.removeBalance(player, amount)) {
            bounties.merge(target.getUniqueId(), amount, Double::sum);
            player.sendMessage("§aYou placed a §e$" + String.format("%.2f", amount) + " §abounty on §e" + target.getName());
            target.sendMessage("§cA bounty of §e$" + String.format("%.2f", amount) + " §chas been placed on you!");
            Bukkit.broadcastMessage("§6[Bounty] §e" + player.getName() + " §cplaced a §e$" + String.format("%.2f", amount) + " §cbounty on §e" + target.getName());
        } else {
            player.sendMessage("§cNot enough money!");
        }
    }

    public void claimBounty(Player killer, Player victim) {
        double bounty = bounties.remove(victim.getUniqueId());
        if (bounty > 0) {
            double tax = Math.round(bounty * BOUNTY_TAX_RATE * 100.0) / 100.0;
            double payout = Math.round((bounty - tax) * 100.0) / 100.0;
            balanceManager.addBalance(killer, payout);
            killer.sendMessage("§aYou claimed the §e$" + String.format("%.2f", bounty) + " §abounty on §e" + victim.getName() + " §7(tax: $" + String.format("%.2f", tax) + ")");
            Bukkit.broadcastMessage("§6[Bounty] §e" + killer.getName() + " §ahas claimed the §e$" + String.format("%.2f", bounty) + " §abounty on §e" + victim.getName());
        }
    }

    public double getBounty(UUID uuid) {
        return bounties.getOrDefault(uuid, 0.0);
    }

    public double getUnpaidTaxes(UUID uuid) {
        return unpaidTaxes.getOrDefault(uuid, 0.0);
    }

    private void startTaxReminder() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    double owed = unpaidTaxes.getOrDefault(player.getUniqueId(), 0.0);
                    if (owed > 0) {
                        player.sendMessage("§c§l[Taxes] §ePay your taxes: $" + String.format("%.2f", owed) + " §7(/paytax)");
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("BundledEssential"), 1200L, 1200L);
    }
}
