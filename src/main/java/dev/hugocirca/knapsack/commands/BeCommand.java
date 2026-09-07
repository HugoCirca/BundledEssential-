package dev.hugocirca.knapsack.commands;

import dev.hugocirca.knapsack.updater.UpdateManager;
import dev.hugocirca.knapsack.util.HelpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BeCommand implements CommandExecutor, TabCompleter {

    private final HelpManager help;
    private final UpdateManager updater;
    private final String version;

    public BeCommand(HelpManager help, UpdateManager updater, String version) {
        this.help = help;
        this.updater = updater;
        this.version = version;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /be <help|version|update>");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("help")) {
            return help.onCommand(sender, command, "bundledhelp", new String[0]);
        }
        if (sub.equals("version")) {
            sender.sendMessage("§6§lBundledEssential §e v" + version);
            return true;
        }
        if (sub.equals("update")) {
            if (updater != null) {
                return updater.onCommand(sender, command, "bundledupdate",
                        args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]);
            }
            sender.sendMessage("§cUpdater is disabled!");
            return true;
        }
        sender.sendMessage("§cUsage: /be <help|version|update>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> s = new ArrayList<>();
        if (args.length == 1) {
            s.add("help");
            s.add("version");
            s.add("update");
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        s.removeIf(x -> !x.toLowerCase().startsWith(last));
        return s;
    }
}
