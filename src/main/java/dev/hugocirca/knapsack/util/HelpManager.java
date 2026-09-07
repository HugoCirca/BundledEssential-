package dev.hugocirca.knapsack.util;

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
        meta.setTitle("§6Knapsack Guide");
        meta.setAuthor("Server");
        List<String> pages = new ArrayList<>();
        pages.add("§4§lKnapsack\n§0Command guide.\n\n§0Run §1/KShelp §0anytime.\n\n§0Covers: TPA, Home, Waypoints, Trade, Money, Custom");
        pages.add("§4§lTPA\n§1/tpa <player>\n§0- ask to teleport\n§1/tpa here <player>\n§0- ask them to come\n§1/tpa\n§0- GUI: accept/deny, auto toggle\n\n§0Expire 30s, auto feeds via toggle.");
        pages.add("§4§lHome\n§1/home\n§0- teleport home\n§1/home set\n§0- save where you stand\n§1/sethome\n§0- alias to /home set\n§1/home remove\n§0- delete home\n\n§4§lBack\n§1/back\n§0- return to death spot");
        pages.add("§4§lWaypoints\n§1/waypoint\n§0- GUI: click=tp, shift=delete, empty=new\n§1/waypoint new <name>\n§0- shortcut save\n\n§0Capacity 27, shifts handle delete.");
        pages.add("§4§lTrade\n§1/trade <player>\n§0- send request\n§0Target gets GUI: Accept/Deny\n§0No more /tradeaccept or /tradecancel\n\n§0Both sides lock green to swap.");
        pages.add("§4§lMoney Hub\n§1/money §7(aliases /eco /wallet)\n§0- hub GUI: Balance, Pay, Shop, Sell, Bounty, Bank, Loan, Tax, Quest, Daily, Repair\n§1/shop §7kept shortcut\n§1/sell §7kept shortcut\n§1/balance [p] §7kept\n§1/pay <p> <amt> §7kept\n§1/resetbal <p> [amt] §7admin");
        pages.add("§4§lMoney Details\n§1Shop§0- browse buy, search compass\n§1Sell§0- held item, shift=sellgui\n§1Bounty/Pay§0- pay 5% tax, bounty 20%\n§1Bank§0- overflow at cap, interest\n§1Loan§0- 200-2500 GUI, 3/7/14d or slow 20%");
        pages.add("§4§lQuests & Daily\n§1Via /money hub\n§1/quest §0- now hub only\n§1/daily §0- now hub only\n§0Both still work if you type them directly for now but prefer hub.");
        pages.add("§4§lCustom Items\n§1/shop §0Custom tab:\n§1Auto-Sell Chest $500\n§0- sells timer split\n§1Zombie $250 / Skeleton $325\n§0- right-click boost to 35x\n\n§1/autosell give <p> §7ops");
        pages.add("§4§lOther\n§1/level [player]\n§0- XP level bonus\n§1/playtime [top|optin|vault]\n§0- times\n§1/resetbal <p> [amt] §7admin\n§1/bundledreload §7or /knapsackreload\n§1/KShelp §7this book\n§1/be <help|version|update>");
        meta.setPages(pages);
        book.setItemMeta(meta);
        return book;
    }

    private void sendChat(CommandSender sender) {
        sender.sendMessage("§6§l=== Knapsack Commands ===");
        sender.sendMessage("");
        sender.sendMessage("§e§lTPA");
        sender.sendMessage("  §7/tpa <player> §f- Send request");
        sender.sendMessage("  §7/tpa here <player> §f- Request them to you");
        sender.sendMessage("  §7/tpa §f- GUI: accept/deny + auto toggle");
        sender.sendMessage("");
        sender.sendMessage("§e§lHome");
        sender.sendMessage("  §7/home §f- Teleport home");
        sender.sendMessage("  §7/home set §f- Set home (sethome alias)");
        sender.sendMessage("  §7/home remove §f- Remove home (removehome alias)");
        sender.sendMessage("");
        sender.sendMessage("§e§lBack");
        sender.sendMessage("  §7/back §f- Return to death");
        sender.sendMessage("");
        sender.sendMessage("§e§lWaypoints");
        sender.sendMessage("  §7/waypoint §f- GUI: click tp, shift delete, empty new");
        sender.sendMessage("  §7/waypoint new <name> §f- Shortcut");
        sender.sendMessage("");
        sender.sendMessage("§e§lTrade");
        sender.sendMessage("  §7/trade <player> §f- Send request (GUI accept/deny)");
        sender.sendMessage("");
        sender.sendMessage("§e§lMoney Hub");
        sender.sendMessage("  §7/money §f(or /eco) - Hub GUI");
        sender.sendMessage("  §7/shop §f- Shortcut kept");
        sender.sendMessage("  §7/sell §f- Shortcut kept (shift=sellgui)");
        sender.sendMessage("  §7/balance [player] §f- Kept");
        sender.sendMessage("  §7/pay <player> <amount> §f- Kept (5% tax)");
        sender.sendMessage("  §7/resetbal <p> [amt] §f- Admin console ok");
        sender.sendMessage("  §7(Bank/Loan/Bounty/Tax/Quest/Daily/Repair via /money)");
        sender.sendMessage("");
        sender.sendMessage("§e§lOther");
        sender.sendMessage("  §7/KShelp §f- Show this help (bundledhelp alias)");
        sender.sendMessage("  §7/bundledreload §f- Reload config.yml (console)");
        sender.sendMessage("  §7/bundledupdate §f- Check for updates");
        sender.sendMessage("");
        sender.sendMessage("§6§l================================");
    }
}
