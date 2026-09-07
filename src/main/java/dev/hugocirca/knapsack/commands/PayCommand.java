package dev.hugocirca.knapsack.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Thin tab-complete wrapper for BountyManager /pay. Logic stays in BountyManager. */
public class PayCommand implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> s = new ArrayList<>();
        if (args.length == 1) {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player self && pl.equals(self)) continue;
                s.add(pl.getName());
            }
        } else if (args.length == 2) {
            s.add("<amount>");
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        s.removeIf(x -> x.startsWith("<") ? false : !x.toLowerCase().startsWith(last));
        return s;
    }
}
