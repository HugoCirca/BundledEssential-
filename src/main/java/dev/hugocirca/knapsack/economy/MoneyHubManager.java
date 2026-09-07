package dev.hugocirca.knapsack.economy;

import dev.hugocirca.knapsack.KnapsackPlugin;
import dev.hugocirca.knapsack.util.Features;
import dev.hugocirca.knapsack.util.GuiUtil;
import dev.hugocirca.knapsack.util.Money;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MoneyHubManager implements CommandExecutor, Listener {

    private static final String TITLE = "§6Money Hub";
    private final KnapsackPlugin plugin;
    private final BalanceManager balance;
    private final BountyManager bounty;
    private final ShopManager shop;
    private final SellManager sell;
    private final PriceManager prices;
    private final dev.hugocirca.knapsack.loan.LoanManager loan;
    private final dev.hugocirca.knapsack.rewards.RewardManager rewards;

    public MoneyHubManager(KnapsackPlugin plugin, BalanceManager balance, BountyManager bounty, ShopManager shop, SellManager sell, PriceManager prices, dev.hugocirca.knapsack.loan.LoanManager loan, dev.hugocirca.knapsack.rewards.RewardManager rewards) {
        this.plugin = plugin;
        this.balance = balance;
        this.bounty = bounty;
        this.shop = shop;
        this.sell = sell;
        this.prices = prices;
        this.loan = loan;
        this.rewards = rewards;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private boolean isEnabled(String feature) {
        try { return plugin.getFeatures().isEnabled(feature); } catch (Exception e) { return true; }
    }

    private void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        GuiUtil.fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        Features f = plugin.getFeatures();
        int slot = 10;
        if (balance != null) {
            inv.setItem(slot++, GuiUtil.item(Material.EMERALD, "§aBalance", "§7$" + Money.format(balance.getBalance(p)), "§7Click to refresh"));
        }
        if (bounty != null && isEnabled("pay")) {
            inv.setItem(slot++, GuiUtil.item(Material.GOLD_INGOT, "§ePay Player", "§7Click to pay", "§7Opens anvil"));
        }
        if (shop != null && isEnabled("shop")) {
            inv.setItem(slot++, GuiUtil.item(Material.CHEST, "§6Shop", "§7Browse & buy", "§7Click to open"));
        }
        if (sell != null && isEnabled("sell")) {
            inv.setItem(slot++, GuiUtil.item(Material.EMERALD_BLOCK, "§aSell", "§7Sell held item", "§7Click: /sell | Shift: /sellgui"));
        }
        if (bounty != null && isEnabled("bounty")) {
            inv.setItem(slot++, GuiUtil.item(Material.SKELETON_SKULL, "§cBounty", "§7Place/check bounty", "§7Click to set"));
        }
        if (balance != null) {
            inv.setItem(slot++, GuiUtil.item(Material.GOLD_BLOCK, "§6Server Bank", "§a$" + Money.format(balance.getServerBank()), "§7Click for history book"));
        }
        if (loan != null) {
            inv.setItem(slot++, GuiUtil.item(Material.PAPER, "§eLoan", "§7Borrow GUI", "§7Click to open"));
        }
        if (bounty != null) {
            inv.setItem(29, GuiUtil.item(Material.REDSTONE, "§cPay Tax", "§7Owed: $" + Money.format(bounty.getUnpaidTaxes(p.getUniqueId())), "§7Click to /paytax"));
        }
        if (rewards != null) {
            inv.setItem(30, GuiUtil.item(Material.BOOK, "§dQuest", "§7/quest", "§7Click to view"));
            inv.setItem(31, GuiUtil.item(Material.CLOCK, "§bDaily", "§7/daily streak", "§7Click to claim"));
        }
        inv.setItem(32, GuiUtil.item(Material.ANVIL, "§7Repair", "§7Repair held item", "§7Click: 1% | Shift: full"));
        // hide disabled slots? Already gated.
        p.openInventory(inv);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cOnly players can use /money hub");
            return true;
        }
        // Collision fallback: if /eco alias used, same hub
        open(p);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;
        ItemStack cur = e.getCurrentItem();
        if (cur == null || cur.getType() == Material.GRAY_STAINED_GLASS_PANE || cur.getType() == Material.AIR) return;
        Material t = cur.getType();
        if (t == Material.EMERALD && balance != null) {
            p.sendMessage("§6Balance: §a$" + Money.format(balance.getBalance(p)));
            p.closeInventory();
        } else if (t == Material.GOLD_INGOT) {
            p.closeInventory();
            p.performCommand("pay");
        } else if (t == Material.CHEST && shop != null) {
            p.closeInventory();
            shop.openShop(p);
        } else if (t == Material.EMERALD_BLOCK && sell != null) {
            p.closeInventory();
            if (e.isShiftClick()) p.performCommand("sellgui");
            else p.performCommand("sell");
        } else if (t == Material.SKELETON_SKULL && bounty != null) {
            p.closeInventory();
            p.sendMessage("§cUse: /bounty <player> [amount] — hub click only shows shortcut");
        } else if (t == Material.GOLD_BLOCK && balance != null) {
            p.closeInventory();
            p.performCommand("serverbank");
        } else if (t == Material.PAPER && loan != null) {
            p.closeInventory();
            p.performCommand("loan");
        } else if (t == Material.REDSTONE) {
            p.closeInventory();
            p.performCommand("paytax");
        } else if (t == Material.BOOK) {
            p.closeInventory();
            p.performCommand("quest");
        } else if (t == Material.CLOCK) {
            p.closeInventory();
            p.performCommand("daily");
        } else if (t == Material.ANVIL && balance != null) {
            p.closeInventory();
            if (e.isShiftClick()) p.performCommand("repair full");
            else p.performCommand("repair");
        }
    }
}
