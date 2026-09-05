package com.bundleessential.economy;

import com.bundleessential.util.Money;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SellManager implements CommandExecutor, Listener {

    private final BalanceManager balanceManager;
    private final PriceManager priceManager;
    private static final String SELL_GUI_TITLE = "§6§lSell Shop";

    public SellManager(BalanceManager balanceManager, PriceManager priceManager) {
        this.balanceManager = balanceManager;
        this.priceManager = priceManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "sell" -> handleSell(player);
            case "sellgui" -> openSellGui(player);
        }
        return true;
    }

    private void handleSell(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§cYou are not holding anything!");
            return;
        }

        int amount = item.getAmount();
        double pricePerItem = priceManager.getSellPriceWithEnchants(item);
        double total = Math.round(pricePerItem * amount * 100.0) / 100.0;

        balanceManager.addBalance(player, total);
        player.getInventory().setItemInMainHand(null);

        player.sendMessage("§aSold §e" + amount + "x " + formatMaterialName(item.getType()) + " §afor §e$" + Money.format(total));
        if (priceManager.getEnchantmentBonus(item) > 0) {
            player.sendMessage("§7(Enchantment bonus: §a+$" + Money.format(priceManager.getEnchantmentBonus(item) * amount) + "§7)");
        }
    }

    public void openSellGui(Player player) {
        Inventory sellGui = Bukkit.createInventory(null, 54, SELL_GUI_TITLE);

        ItemStack info = makeItem(Material.EMERALD_BLOCK, "§a§lHow to sell",
                "§7Put items in any slot",
                "§7Close to sell everything",
                "§7Shift-click to sell all of one item",
                "§7Enchantments give bonus money!");
        sellGui.setItem(49, info);

        player.openInventory(sellGui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(SELL_GUI_TITLE)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.EMERALD_BLOCK) return;

        // Shift-click: sell all of that item type from player inventory
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            int count = 0;
            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem != null && invItem.getType() == clicked.getType()) {
                    count += invItem.getAmount();
                }
            }
            if (count > 0) {
                double priceEach = priceManager.getSellPriceWithEnchants(new ItemStack(clicked.getType()));
                double total = Math.round(priceEach * count * 100.0) / 100.0;
                player.getInventory().removeItem(new ItemStack(clicked.getType(), count));
                balanceManager.addBalance(player, total);
                player.sendMessage("§aSold §e" + count + "x " + formatMaterialName(clicked.getType()) + " §afor §e$" + Money.format(total));
            }
            return;
        }

        // Normal click: allow placing items into the sell GUI
        if (event.getSlot() != 49) {
            event.setCancelled(false);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(SELL_GUI_TITLE)) return;

        Inventory inventory = event.getInventory();
        double totalEarned = 0.0;
        int totalItems = 0;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            if (item.getType() == Material.EMERALD_BLOCK) continue;

            int amount = item.getAmount();
            double price = priceManager.getSellPriceWithEnchants(item);
            double itemTotal = Math.round(price * amount * 100.0) / 100.0;
            totalEarned += itemTotal;
            totalItems += amount;
        }

        if (totalItems > 0) {
            totalEarned = Math.round(totalEarned * 100.0) / 100.0;
            balanceManager.addBalance(player, totalEarned);
            player.sendMessage("§aSold §e" + totalItems + " items §afor §e$" + Money.format(totalEarned) + "§a!");
        }
    }

    private String formatMaterialName(Material material) {
        String name = material.name().replace("_", " ").toLowerCase();
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private ItemStack makeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String l : lore) loreList.add(l);
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }
}
