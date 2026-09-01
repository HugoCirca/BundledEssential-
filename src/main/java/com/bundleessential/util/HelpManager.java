package com.bundleessential.util;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HelpManager implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("§6§l=== BundledEssential Commands ===");
        sender.sendMessage("");
        sender.sendMessage("§e§lTPA");
        sender.sendMessage("  §7/tpa <player> §f- Send teleport request");
        sender.sendMessage("  §7/tpahere <player> §f- Request player to teleport to you");
        sender.sendMessage("  §7/tpaccept §f- Accept teleport request");
        sender.sendMessage("");
        sender.sendMessage("§e§lHome");
        sender.sendMessage("  §7/sethome §f- Set your home");
        sender.sendMessage("  §7/removehome §f- Remove your home");
        sender.sendMessage("  §7/home §f- Teleport to home");
        sender.sendMessage("");
        sender.sendMessage("§e§lBack");
        sender.sendMessage("  §7/back §f- Return to last death location");
        sender.sendMessage("");
        sender.sendMessage("§e§lWaypoints");
        sender.sendMessage("  §7/waypoint §f- Open waypoint GUI");
        sender.sendMessage("  §7/waypoint new <name> §f- Create waypoint");
        sender.sendMessage("  §7/waypoint delete <name> §f- Delete waypoint");
        sender.sendMessage("");
        sender.sendMessage("§e§lTrade");
        sender.sendMessage("  §7/trade <player> §f- Send trade request");
        sender.sendMessage("  §7/tradeaccept §f- Accept trade request");
        sender.sendMessage("  §7/tradecancel §f- Cancel trade requests");
        sender.sendMessage("");
        sender.sendMessage("§e§lEconomy");
        sender.sendMessage("  §7/shop §f- Open the shop");
        sender.sendMessage("  §7/sell §f- Sell item in hand");
        sender.sendMessage("  §7/sellgui §f- Open sell GUI");
        sender.sendMessage("  §7/balance [player] §f- Check balance");
        sender.sendMessage("  §7/pay <player> <amount> §f- Pay a player");
        sender.sendMessage("  §7/bounty <player> [amount] §f- Set/check bounty");
        sender.sendMessage("");
        sender.sendMessage("§6§l================================");
        return true;
    }
}
