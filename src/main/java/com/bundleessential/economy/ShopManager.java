package com.bundleessential.economy;

import org.bukkit.Bukkit;
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
    private final PriceManager priceManager;

    public ShopManager(BalanceManager balanceManager, PriceManager priceManager) {
        this.balanceManager = balanceManager;
        this.priceManager = priceManager;
    }

    public void openShop(Player player) {
        Inventory shop = Bukkit.createInventory(null, 54, "§6§lShop");

        // Categories
        shop.setItem(10, makeItem(Material.OAK_LOG, "§e§lLogs", "§7Click to browse"));
        shop.setItem(11, makeItem(Material.COBBLESTONE, "§e§lStone", "§7Click to browse"));
        shop.setItem(12, makeItem(Material.DIAMOND_ORE, "§e§lOres", "§7Click to browse"));
        shop.setItem(13, makeItem(Material.WHEAT, "§e§lCrops", "§7Click to browse"));
        shop.setItem(14, makeItem(Material.BONE, "§e§lMob Drops", "§7Click to browse"));
        shop.setItem(15, makeItem(Material.BRICKS, "§e§lBuilding", "§7Click to browse"));
        shop.setItem(16, makeItem(Material.CRAFTING_TABLE, "§e§lDecoration", "§7Click to browse"));

        // Sell section
        shop.setItem(40, makeItem(Material.EMERALD, "§a§lSell Items", "§7Click to open sell menu"));

        ItemStack glass = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (shop.getItem(i) == null) shop.setItem(i, glass);
        }

        player.openInventory(shop);
    }

    private void openCategory(Player player, String category, Material... materials) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§l" + category + " Shop");

        int buySlot = 10;
        int sellSlot = 37;

        for (Material mat : materials) {
            if (buySlot <= 16) {
                double buyPrice = priceManager.getBuyPrice(mat);
                inv.setItem(buySlot, makeItem(mat, "§aBuy " + formatName(mat),
                        "§ePrice: $" + String.format("%.2f", buyPrice),
                        "§7Click to buy 1"));
                buySlot++;
            }

            if (sellSlot <= 43) {
                double sellPrice = priceManager.getSellPrice(mat);
                inv.setItem(sellSlot, makeItem(mat, "§cSell " + formatName(mat),
                        "§eSell: $" + String.format("%.2f", sellPrice),
                        "§7Click to sell 1"));
                sellSlot++;
            }
        }

        inv.setItem(4, makeItem(Material.ARROW, "§cBack to Shop"));

        ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        player.openInventory(inv);
    }

    private void openLogsShop(Player player) {
        openCategory(player, "Logs",
                Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
                Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
                Material.MANGROVE_LOG, Material.CHERRY_LOG, Material.CRIMSON_STEM,
                Material.WARPED_STEM);
    }

    private void openStoneShop(Player player) {
        openCategory(player, "Stone",
                Material.COBBLESTONE, Material.STONE, Material.DEEPSLATE,
                Material.ANDESITE, Material.DIORITE, Material.GRANITE,
                Material.TUFF, Material.DRIPSTONE_BLOCK, Material.CALCITE,
                Material.SMOOTH_STONE, Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS);
    }

    private void openOresShop(Player player) {
        openCategory(player, "Ores",
                Material.COAL_ORE, Material.IRON_ORE, Material.COPPER_ORE,
                Material.GOLD_ORE, Material.REDSTONE_ORE, Material.LAPIS_ORE,
                Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.NETHER_GOLD_ORE,
                Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS);
    }

    private void openCropsShop(Player player) {
        openCategory(player, "Crops",
                Material.WHEAT, Material.CARROT, Material.POTATO,
                Material.BEETROOT, Material.MELON, Material.PUMPKIN,
                Material.SUGAR_CANE, Material.BAMBOO, Material.NETHER_WART,
                Material.CHORUS_FRUIT, Material.APPLE);
    }

    private void openMobDropsShop(Player player) {
        openCategory(player, "Mob Drops",
                Material.ROTTEN_FLESH, Material.BONE, Material.STRING,
                Material.SPIDER_EYE, Material.GUNPOWDER, Material.ENDER_PEARL,
                Material.BLAZE_ROD, Material.MAGMA_CREAM, Material.GHAST_TEAR,
                Material.PHANTOM_MEMBRANE, Material.SHULKER_SHELL);
    }

    private void openBuildingShop(Player player) {
        openCategory(player, "Building",
                Material.OAK_PLANKS, Material.OAK_FENCE, Material.OAK_STAIRS,
                Material.OAK_SLAB, Material.OAK_DOOR, Material.OAK_TRAPDOOR,
                Material.BRICKS, Material.BRICK_STAIRS, Material.BRICK_SLAB,
                Material.NETHER_BRICKS, Material.OBSIDIAN, Material.GLASS);
    }

    private void openDecorationShop(Player player) {
        openCategory(player, "Decoration",
                Material.CRAFTING_TABLE, Material.FURNACE, Material.ANVIL,
                Material.BREWING_STAND, Material.CHEST, Material.ENDER_CHEST,
                Material.HOPPER, Material.TORCH, Material.LANTERN,
                Material.CAMPFIRE, Material.BELL, Material.JUKEBOX);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        event.setCancelled(true);

        // Main shop
        if (title.equals("§6§lShop")) {
            switch (event.getSlot()) {
                case 10 -> openLogsShop(player);
                case 11 -> openStoneShop(player);
                case 12 -> openOresShop(player);
                case 13 -> openCropsShop(player);
                case 14 -> openMobDropsShop(player);
                case 15 -> openBuildingShop(player);
                case 16 -> openDecorationShop(player);
                case 40 -> new SellManager(balanceManager, priceManager).openSellGui(player);
            }
            return;
        }

        // Category shops
        if (title.endsWith(" Shop")) {
            // Back button
            if (event.getSlot() == 4) {
                openShop(player);
                return;
            }

            Material material = clicked.getType();
            double buyPrice = priceManager.getBuyPrice(material);
            double sellPrice = priceManager.getSellPrice(material);

            // Buy (slots 10-16)
            if (event.getSlot() >= 10 && event.getSlot() <= 16) {
                if (balanceManager.removeBalance(player, buyPrice)) {
                    player.getInventory().addItem(new ItemStack(material, 1));
                    player.sendMessage("§aBought 1x " + formatName(material) + " for $" + String.format("%.2f", buyPrice));
                } else {
                    player.sendMessage("§cNot enough money! Need $" + String.format("%.2f", buyPrice));
                }
                return;
            }

            // Sell (slots 37-43)
            if (event.getSlot() >= 37 && event.getSlot() <= 43) {
                if (player.getInventory().containsAtLeast(new ItemStack(material), 1)) {
                    player.getInventory().removeItem(new ItemStack(material, 1));
                    balanceManager.addBalance(player, sellPrice);
                    player.sendMessage("§aSold 1x " + formatName(material) + " for $" + String.format("%.2f", sellPrice));
                } else {
                    player.sendMessage("§cYou don't have any " + formatName(material) + "!");
                }
            }
        }
    }

    private String formatName(Material material) {
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
