package dev.hugocirca.knapsack.commands;

import dev.hugocirca.knapsack.economy.BalanceManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ResetServerBankCommand implements CommandExecutor {

    private final BalanceManager balance;

    public ResetServerBankCommand(BalanceManager balance) {
        this.balance = balance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("knapsack.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        balance.resetServerBank();
        sender.sendMessage("§aServer Bank reset to §e$0 §7(history cleared).");
        Bukkit.broadcastMessage("§6[Bank] §eServer Bank was reset by " + sender.getName());
        return true;
    }
}
