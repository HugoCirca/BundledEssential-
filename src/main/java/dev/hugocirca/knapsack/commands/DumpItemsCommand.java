package dev.hugocirca.knapsack.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Temporary command to dump every current, obtainable item Material
 * (matching this server's exact Paper/Spigot build) to a plain txt file.
 *
 * Usage: register this in your plugin, then run /dumpitems in-game or console.
 * Output: plugins/Knapsack/items.txt (plugin.getDataFolder()/items.txt)
 *
 * Remove this command once you've generated the list — it's a one-time tool.
 */
public class DumpItemsCommand implements CommandExecutor {

    private final org.bukkit.plugin.Plugin plugin;

    public DumpItemsCommand(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<String> names = new ArrayList<>();

        for (Material m : Material.values()) {
            // isItem() = it can exist as an ItemStack
            // isLegacy() = pre-1.13 numeric-id material, not usable/spawnable on modern servers — skip
            if (m.isItem() && !m.isLegacy()) {
                names.add(m.name());
            }
        }

        Collections.sort(names);

        File outFile = new File(plugin.getDataFolder(), "items.txt");
        try {
            plugin.getDataFolder().mkdirs();
            try (PrintWriter writer = new PrintWriter(new FileWriter(outFile))) {
                for (String name : names) {
                    writer.println(name);
                }
            }
            sender.sendMessage("§aDumped " + names.size() + " items to " + outFile.getPath());
            plugin.getLogger().info("Dumped " + names.size() + " items to " + outFile.getPath());
        } catch (IOException e) {
            sender.sendMessage("§cFailed to write items.txt: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}
