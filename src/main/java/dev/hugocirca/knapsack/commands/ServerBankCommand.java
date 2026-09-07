package dev.hugocirca.knapsack.commands;

import dev.hugocirca.knapsack.economy.BalanceManager;
import dev.hugocirca.knapsack.util.Money;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Extracted from KnapsackPlugin inline /serverbank lambda. */
public class ServerBankCommand implements CommandExecutor, TabCompleter {

    private final BalanceManager balance;

    public ServerBankCommand(BalanceManager balance) {
        this.balance = balance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("history")) {
            if (sender instanceof Player player) {
                ItemStack book = balance.historyBook();
                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItemNaturally(player.getLocation(), book);
                    player.sendMessage("§eInventory full — history book dropped!");
                } else {
                    player.getInventory().addItem(book);
                    player.sendMessage("§aOpened Server Bank history book!");
                }
            } else {
                sender.sendMessage("§6§lServer Bank: §a$" + Money.format(balance.getServerBank()) + " §7(" + balance.getBankHistory().size() + " txns)");
                int shown = 0;
                for (int i = balance.getBankHistory().size() - 1; i >= 0 && shown < 10; i--) {
                    JsonObject e = balance.getBankHistory().get(i);
                    String t = new SimpleDateFormat("MM-dd HH:mm").format(new Date(e.get("time").getAsLong()));
                    sender.sendMessage(" " + t + " " + e.get("player").getAsString() + " $" + Money.format(e.get("amount").getAsDouble()) + " " + e.get("reason").getAsString());
                    shown++;
                }
            }
            return true;
        }
        sender.sendMessage("§6§lServer Bank: §a$" + Money.format(balance.getServerBank()));
        sender.sendMessage("§7History: §e/serverbank history §7(book, also works for console)");
        if (sender.isOp() || sender.hasPermission("knapsack.admin")) {
            sender.sendMessage("§7Cap: §e$" + Money.format(balance.getCapPublic()));
            sender.sendMessage("§7Overflow + garnish + bounty/pay taxes feed the bank. Config: economy.balance-cap");
            sender.sendMessage("§7Random interest: 30% every 20 min, 0.2-0.5% of bank (max $5000) → online");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> s = new ArrayList<>();
        if (args.length == 1) s.add("history");
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        s.removeIf(x -> !x.toLowerCase().startsWith(last));
        return s;
    }
}
