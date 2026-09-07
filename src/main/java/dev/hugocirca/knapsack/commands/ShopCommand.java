package dev.hugocirca.knapsack.commands;

import dev.hugocirca.knapsack.economy.ShopManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShopCommand implements CommandExecutor, TabCompleter {

    private final ShopManager shop;

    public ShopCommand(ShopManager shop) {
        this.shop = shop;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("search")) {
            String query = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "";
            shop.searchCommand(player, query);
        } else {
            shop.openShop(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> s = new ArrayList<>();
        if (args.length == 1) s.add("search");
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        s.removeIf(x -> !x.toLowerCase().startsWith(last));
        return s;
    }
}
