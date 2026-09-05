package com.bundleessential.economy;

import com.bundleessential.util.Money;
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
    private final Map<UUID, Long> taxSince = new HashMap<>();

    private static final double LATE_FEE_RATE = 0.10; // +10% debt per reminder cycle once overdue
    private static final double GARNISH_RATE = 0.25; // 25% of mob/playtime earnings seized while in debt
    private static final long GRACE_MILLIS = 24L * 60 * 60 * 1000; // 24h to pay before punishments

    private static final double BOUNTY_TAX_RATE = 0.20;
    private static final double PAY_TAX_RATE = 0.05;
    private static final long WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L;
    private static final long TAX_CHECK_INTERVAL = 140 * 60 * 1000L; // 7 Minecraft days = ~140 min real

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

        if (isOverdue(player.getUniqueId())) {
            player.sendMessage("§c§l[Taxes] §cYou're OVERDUE! Pay with §e/paytax §cbefore using /pay.");
            return;
        }

        double tax = Math.round(amount * PAY_TAX_RATE * 100.0) / 100.0;
        double afterTax = Math.round((amount - tax) * 100.0) / 100.0;

        if (balanceManager.removeBalance(player, amount)) {
            stampDebt(player.getUniqueId(), tax);
            balanceManager.addBalance(target, afterTax);
            player.sendMessage("§aYou paid §e$" + Money.format(amount) + " §ato §e" + target.getName());
            target.sendMessage("§aYou received §e$" + Money.format(afterTax) + " §afrom §e" + player.getName());
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
            taxSince.remove(player.getUniqueId());
            lastTaxTime.put(player.getUniqueId(), System.currentTimeMillis());
            player.sendMessage("§aYou paid §e$" + Money.format(owed) + " §ain taxes! You're clear.");
        } else {
            player.sendMessage("§cNot enough money! You owe §e$" + Money.format(owed) + "§c in taxes.");
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
            sender.sendMessage("§e" + target.getName() + " §chas a bounty of §a$" + Money.format(bounty));
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
        if (isOverdue(player.getUniqueId())) {
            player.sendMessage("§c§l[Taxes] §cYou're OVERDUE! Pay with §e/paytax §cbefore placing bounties.");
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
            player.sendMessage("§aYou placed a §e$" + Money.format(amount) + " §abounty on §e" + target.getName());
            target.sendMessage("§cA bounty of §e$" + Money.format(amount) + " §chas been placed on you!");
            Bukkit.broadcastMessage("§6[Bounty] §e" + player.getName() + " §cplaced a §e$" + Money.format(amount) + " §cbounty on §e" + target.getName());
        } else {
            player.sendMessage("§cNot enough money!");
        }
    }

    /** True when the player owes taxes older than the grace period. */
    public boolean isOverdue(UUID uuid) {
        double owed = unpaidTaxes.getOrDefault(uuid, 0.0);
        if (owed <= 0) return false;
        long since = taxSince.getOrDefault(uuid, System.currentTimeMillis());
        return System.currentTimeMillis() - since > GRACE_MILLIS;
    }

    private void stampDebt(UUID uuid, double added) {
        if (unpaidTaxes.getOrDefault(uuid, 0.0) <= 0) {
            taxSince.put(uuid, System.currentTimeMillis());
        }
        unpaidTaxes.merge(uuid, Math.round(added * 100.0) / 100.0, Double::sum);
    }

    /**
     * Seizes up to GARNISH_RATE of an earning toward unpaid taxes.
     * Returns what the player actually keeps.
     */
    public double garnish(Player player, double amount) {
        double owed = unpaidTaxes.getOrDefault(player.getUniqueId(), 0.0);
        if (owed <= 0 || amount <= 0) return amount;
        double cut = Math.round(Math.min(owed, amount * GARNISH_RATE) * 100.0) / 100.0;
        if (cut <= 0) return amount;
        double left = Math.round((owed - cut) * 100.0) / 100.0;
        if (left <= 0) {
            unpaidTaxes.remove(player.getUniqueId());
            taxSince.remove(player.getUniqueId());
        } else {
            unpaidTaxes.put(player.getUniqueId(), left);
        }
        player.sendMessage("§c[Taxes] §e$" + com.bundleessential.util.Money.format(cut) + " §cseized for unpaid taxes! §7(/paytax)");
        return Math.round((amount - cut) * 100.0) / 100.0;
    }

    public void claimBounty(Player killer, Player victim) {
        Double bounty = bounties.remove(victim.getUniqueId());
        if (bounty != null && bounty > 0) {
            double tax = Math.round(bounty * BOUNTY_TAX_RATE * 100.0) / 100.0;
            double payout = Math.round((bounty - tax) * 100.0) / 100.0;
            balanceManager.addBalance(killer, payout);
            stampDebt(killer.getUniqueId(), tax);
            killer.sendMessage("§aYou claimed the §e$" + Money.format(bounty) + " §abounty on §e" + victim.getName());
            Bukkit.broadcastMessage("§6[Bounty] §e" + killer.getName() + " §ahas claimed the §e$" + Money.format(bounty) + " §abounty on §e" + victim.getName());
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
                long now = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    double owed = unpaidTaxes.getOrDefault(uuid, 0.0);
                    if (owed > 0) {
                        long last = lastTaxTime.getOrDefault(uuid, 0L);
                        if (now - last >= TAX_CHECK_INTERVAL) {
                            if (isOverdue(uuid)) {
                                double fee = Math.round(owed * LATE_FEE_RATE * 100.0) / 100.0;
                                double grown = Math.round((owed + fee) * 100.0) / 100.0;
                                unpaidTaxes.put(uuid, grown);
                                player.sendMessage("§c§l[Taxes] §cOVERDUE! Late fee +$" + Money.format(fee) + " (now $" + Money.format(grown) + "). §a/paytax");
                            } else {
                                player.sendMessage("§c§l[Taxes] §eYou owe $" + Money.format(owed) + " §7in taxes. §a/paytax");
                            }
                            lastTaxTime.put(uuid, now);
                        }
                    }
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("BundledEssential"), 1200L, 1200L);
    }
}
