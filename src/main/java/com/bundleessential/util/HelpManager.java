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
        // Dark colors only: book pages are light parchment, white text is unreadable.
        List<String> pages = new ArrayList<>();
        pages.add("§4§lBundledEssential\n§0Command guide with what everything does.\n\n§0Run §1/bundledhelp §0anytime to get this book again.\n\n§0Covers: TPA, Home, Trade, Economy, Quests, Custom");
        pages.add("§4§lTPA\n§1/tpa <player>\n§0- ask to teleport to them\n§1/tpahere <player>\n§0- ask them to come to you\n§1/tpaccept\n§0- say yes to a request\n\n§0Requests expire in 30s.");
        pages.add("§4§lHome\n§1/sethome\n§0- save where you stand\n§1/home\n§0- teleport back there\n§1/removehome\n§0- delete your home\n\n§4§lBack\n§1/back\n§0- return to death spot");
        pages.add("§4§lWaypoints\n§1/waypoint\n§0- open the travel GUI\n§1/waypoint new <name>\n§0- save this spot\n§1/waypoint delete <name>\n§0- forget a spot\n§1/waypoint <name>\n§0- teleport to a spot");
        pages.add("§4§lTrade\n§1/trade <player>\n§0- offer a trade\n§1/tradeaccept\n§0- open the trade window\n§1/tradecancel\n§0- back out safely\n\n§0Both sides lock in green to swap items.");
        pages.add("§4§lEconomy\n§1/shop\n§0- browse and buy items\n§1/shop search <name>\n§0- jump straight to matches\n§1/sell\n§0- sell what you hold\n§1/sellgui\n§0- sell a whole GUI of stuff\n§1/balance [player]\n§0- yours or their money");
        pages.add("§4§lEconomy\n§1/pay <p> <amt>\n§0- send money (5% tax)\n§1/bounty <p> [amt]\n§0- put a price on heads\n§1/paytax\n§0- clear your tax debt\n§1/repair [full]\n§0- fix the held item");
        pages.add("§4§lQuests & Daily\n§1/quest\n§0- see the current task\n§1/quest claim\n§0- collect pay, get next task\n§1/daily\n§0- streak pay, auto on join\n\n§0Tasks: mine, farm, hunt, fish, breed, enchant and more.");
        pages.add("§4§lCustom Items\n§1/shop §0Custom tab:\n§1Auto-Sell Chest\n§0- chest that sells itself on a timer, split pay\n§1Zombie Spawner\n§0- right-click it to raise rate to 35x\n\n§1/autosell\n§0- chest help and prices\n§1/autosell give <p>\n§0- hand out chests (ops)");
        pages.add("§4§lOther\n§1/level [player]\n§0- XP level and bonus\n§1/playtime [top|name]\n§0- times and leaderboard\n§1/bundledhelp\n§0- this book again\n§1/bundledupdate\n§0- fetch plugin updates");
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
        sender.sendMessage("  §7/shop search <name> §f- Jump to matching items");
        sender.sendMessage("  §7/sell §f- Sell item in hand");
        sender.sendMessage("  §7/sellgui §f- Open sell GUI");
        sender.sendMessage("  §7/balance [player] §f- Check balance");
        sender.sendMessage("  §7/pay <player> <amount> §f- Pay a player");
        sender.sendMessage("  §7/bounty <player> [amount] §f- Set/check bounty");
        sender.sendMessage("  §7/paytax §f- Pay accumulated taxes");
        sender.sendMessage("  §7/repair [full] §f- Repair held item");
        sender.sendMessage("  §7/quest [claim] §f- Repeatable quest, new one instantly");
        sender.sendMessage("  §7/daily §f- Daily streak reward (auto on join)");
        sender.sendMessage("");
        sender.sendMessage("§e§lOther");
        sender.sendMessage("  §7/bundledhelp §f- Show this help");
        sender.sendMessage("  §7/bundledupdate §f- Check for updates");
        sender.sendMessage("");
        sender.sendMessage("§6§l================================");
    }
}
