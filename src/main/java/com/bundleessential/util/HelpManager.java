package com.bundleessential.util;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class HelpManager implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            ItemStack book = buildBook();
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), book);
                player.sendMessage("§eInventory full — help book dropped at your feet!");
            } else {
                player.getInventory().addItem(book);
                player.sendMessage("§aOpened the help book in your inventory!");
            }
        } else {
            sendChat(sender);
        }
        return true;
    }

    private ItemStack buildBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("§6BundledEssential Guide");
        meta.setAuthor("Server");
        List<String> pages = new ArrayList<>();
        pages.add("§6§lBundledEssential\n§7Command guide\n\n§fRun §e/bundledhelp §fanytime to get this book again.\n\n§7Pages: TPA, Home, Trade, Economy, Daily, Other");
        pages.add("§e§lTPA\n§7/tpa <player>\n§f- request teleport\n§7/tpahere <player>\n§f- summon a player\n§7/tpaccept\n§f- accept request\n\n§7Requests expire in 30s.");
        pages.add("§e§lHome\n§7/sethome\n§f- set home\n§7/home\n§f- go home\n§7/removehome\n§f- delete home\n\n§e§lBack\n§7/back\n§f- last death spot");
        pages.add("§e§lWaypoints\n§7/waypoint\n§f- open GUI\n§7/waypoint new <name>\n§f- save spot\n§7/waypoint delete <name>\n§f- remove it\n§7/waypoint <name>\n§f- teleport");
        pages.add("§e§lTrade\n§7/trade <player>\n§f- send request\n§7/tradeaccept\n§f- open trade GUI\n§7/tradecancel\n§f- cancel trade\n\n§7Both sides accept in the GUI to swap items.");
        pages.add("§e§lEconomy\n§7/shop\n§f- buy items\n§7/shop search <name>\n§f- find items\n§7/sell\n§f- sell held item\n§7/sellgui\n§f- sell GUI\n§7/balance\n§f- check money");
        pages.add("§e§lEconomy\n§7/pay <p> <amt>\n§f- pay (5% tax)\n§7/bounty <p> [amt]\n§f- set/check bounty\n§7/paytax\n§f- clear taxes\n§7/repair [full]\n§f- fix held item");
        pages.add("§e§lDaily & Login\n§7/daily\n§f- random quest, claim $40-120\n§7/login\n§f- streak reward, grows daily\n\n§7/level [player]\n§f- XP level\n§7/playtime\n§f- online time + top");
        pages.add("§e§lOther\n§7/bundledhelp\n§f- this book\n§7/bundledupdate\n§f- check updates\n§7/bundleversion\n§f- plugin version");
        meta.setPages(pages);
        book.setItemMeta(meta);
        return book;
    }

    private void sendChat(CommandSender sender) {
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
        sender.sendMessage("  §7/paytax §f- Pay accumulated taxes");
        sender.sendMessage("  §7/repair [full] §f- Repair held item");
        sender.sendMessage("  §7/daily [claim] §f- Daily random quest");
        sender.sendMessage("  §7/login §f- Daily streak reward (scales)");
        sender.sendMessage("");
        sender.sendMessage("§e§lOther");
        sender.sendMessage("  §7/bundledhelp §f- Show this help");
        sender.sendMessage("  §7/bundledupdate §f- Check for updates");
        sender.sendMessage("");
        sender.sendMessage("§6§l================================");
    }
}
