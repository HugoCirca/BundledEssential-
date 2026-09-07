package dev.hugocirca.knapsack.commands;

import dev.hugocirca.knapsack.KnapsackPlugin;
import dev.hugocirca.knapsack.economy.BalanceManager;
import dev.hugocirca.knapsack.util.Features;
import dev.hugocirca.knapsack.util.Money;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class BundledReloadCommand implements CommandExecutor {

    private final KnapsackPlugin plugin;
    private final BalanceManager balance;

    public BundledReloadCommand(KnapsackPlugin plugin, BalanceManager balance) {
        this.plugin = plugin;
        this.balance = balance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("knapsack.admin")
                && !sender.hasPermission("knapsack.reload")
                && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        try {
            plugin.reloadConfig();
            plugin.saveDefaultConfig();
            // Features is file-backed; reload by recreating
            plugin.setFeatures(new Features(plugin));
            sender.sendMessage("§aBundledEssential config reloaded!");
            sender.sendMessage("§7balance-cap: §e$" + Money.format(plugin.getConfig().getDouble("economy.balance-cap", 1_000_000_000_000.0)));
            sender.sendMessage("§7Server Bank: §a$" + Money.format(balance != null ? balance.getServerBank() : 0));
        } catch (Exception e) {
            sender.sendMessage("§cReload failed: " + e.getMessage());
        }
        return true;
    }
}
