package dev.hugocirca.knapsack.commands;

import dev.hugocirca.knapsack.economy.BalanceManager;
import dev.hugocirca.knapsack.util.Money;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Extracted from KnapsackPlugin.registerCommands() inline lambda.
 * Keeps economy repair logic out of the god plugin class.
 */
public class RepairCommand implements CommandExecutor {

    private final BalanceManager balance;

    public RepairCommand(BalanceManager balance) {
        this.balance = balance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        if (balance == null) {
            player.sendMessage("§cEconomy is disabled!");
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
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
            if (balance.removeBalance(player, cost)) {
                item.setDurability((short) 0);
                player.sendMessage("§aRepaired to full durability for §e$" + Money.format(cost));
            } else {
                player.sendMessage("§cNot enough money! Need $" + Money.format(cost));
            }
        } else {
            double singlePct = 1.0 / maxDur;
            double singleCost = Math.round(baseCost * singlePct * 100.0) / 100.0;
            if (singleCost < 0.10) singleCost = 0.10;
            if (balance.removeBalance(player, singleCost)) {
                item.setDurability((short) Math.max(0, dur - 1));
                player.sendMessage("§aRepaired 1% durability for §e$" + Money.format(singleCost));
            } else {
                player.sendMessage("§cNot enough money! Need $" + Money.format(singleCost));
            }
        }
        return true;
    }
}
