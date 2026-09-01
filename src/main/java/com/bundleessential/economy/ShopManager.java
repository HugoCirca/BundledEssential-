package com.bundleessential.economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopManager implements Listener {

    private final BalanceManager balanceManager;

    public ShopManager(BalanceManager balanceManager) {
        this.balanceManager = balanceManager;
    }

    public void openShop(Player player) {
        Inventory shop = Bukkit.createInventory(null, 27, "§6§lShop");

        // Plank in the middle (slot 13) - click to go to logs
        ItemStack plank = makeItem(Material.OAK_PLANKS, "§e§lLogs Shop", "§7Click to browse logs");
        shop.setItem(13, plank);

        // Decorative glass panes
        ItemStack glass = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (i != 13) shop.setItem(i, glass);
        }

        player.openInventory(shop);
    }

    private void openLogsShop(Player player) {
        Inventory logs = Bukkit.createInventory(null, 27, "§6§lLogs Shop");

        // Buy items (left side)
        logs.setItem(10, makeItem(Material.OAK_LOG, "§aBuy Oak Log", "§ePrice: $5.00", "§7Click to buy"));
        logs.setItem(11, makeItem(Material.BIRCH_LOG, "§aBuy Birch Log", "§ePrice: $5.00", "§7Click to buy"));
        logs.setItem(12, makeItem(Material.SPRUCE_LOG, "§aBuy Spruce Log", "§ePrice: $5.00", "§7Click to buy"));
        logs.setItem(13, makeItem(Material.JUNGLE_LOG, "§aBuy Jungle Log", "§ePrice: $5.00", "§7Click to buy"));
        logs.setItem(14, makeItem(Material.ACACIA_LOG, "§aBuy Acacia Log", "§ePrice: $5.00", "§7Click to buy"));
        logs.setItem(15, makeItem(Material.DARK_OAK_LOG, "§aBuy Dark Oak Log", "§ePrice: $5.00", "§7Click to buy"));

        // Sell items (bottom row)
        logs.setItem(19, makeItem(Material.OAK_LOG, "§cSell Oak Log", "§eSell: $2.00", "§7Click to sell"));
        logs.setItem(20, makeItem(Material.BIRCH_LOG, "§cSell Birch Log", "§eSell: $2.00", "§7Click to sell"));
        logs.setItem(21, makeItem(Material.SPRUCE_LOG, "§cSell Spruce Log", "§eSell: $2.00", "§7Click to sell"));
        logs.setItem(22, makeItem(Material.JUNGLE_LOG, "§cSell Jungle Log", "§eSell: $2.00", "§7Click to sell"));
        logs.setItem(23, makeItem(Material.ACACIA_LOG, "§cSell Acacia Log", "§eSell: $2.00", "§7Click to sell"));
        logs.setItem(24, makeItem(Material.DARK_OAK_LOG, "§cSell Dark Oak Log", "§eSell: $2.00", "§7Click to sell"));

        // Back button
        logs.setItem(4, makeItem(Material.ARROW, "§cBack to Shop"));

        // Glass fill
        ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (logs.getItem(i) == null) logs.setItem(i, glass);
        }

        player.openInventory(logs);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        event.setCancelled(true);

        // Main shop
        if (title.equals("§6§lShop")) {
            if (event.getSlot() == 13) {
                openLogsShop(player);
            }
            return;
        }

        // Logs shop
        if (title.equals("§6§lLogs Shop")) {
            // Back button
            if (event.getSlot() == 4) {
                openShop(player);
                return;
            }

            double price = 5.0;
            double sellPrice = 2.0;

            // Buy logs (slots 10-15)
            if (event.getSlot() >= 10 && event.getSlot() <= 15) {
                if (balanceManager.removeBalance(player, price)) {
                    player.getInventory().addItem(new ItemStack(clicked.getType(), 1));
                    player.sendMessage("§aBought 1x " + clicked.getType().name().replace("_LOG", " Log") + " for $" + price);
                } else {
                    player.sendMessage("§cNot enough money!");
                }
                return;
            }

            // Sell logs (slots 19-24)
            if (event.getSlot() >= 19 && event.getSlot() <= 24) {
                Material logType = clicked.getType();
                if (player.getInventory().containsAtLeast(new ItemStack(logType), 1)) {
                    player.getInventory().removeItem(new ItemStack(logType, 1));
                    balanceManager.addBalance(player, sellPrice);
                    player.sendMessage("§aSold 1x " + logType.name().replace("_LOG", " Log") + " for $" + sellPrice);
                } else {
                    player.sendMessage("§cYou don't have any " + logType.name().replace("_LOG", " Log") + "!");
                }
            }
        }
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
