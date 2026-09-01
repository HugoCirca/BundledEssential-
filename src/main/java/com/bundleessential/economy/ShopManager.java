package com.bundleessential.economy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopManager implements Listener {

    private final BalanceManager balanceManager;
    private final PriceManager priceManager;
    private final SellManager sellManager;

    public ShopManager(BalanceManager balanceManager, PriceManager priceManager, SellManager sellManager) {
        this.balanceManager = balanceManager;
        this.priceManager = priceManager;
        this.sellManager = sellManager;
    }

    public void openShop(Player player) {
        Inventory shop = Bukkit.createInventory(null, 54, "§6§lShop");

        shop.setItem(10, makeItem(Material.OAK_LOG, "§e§lLogs", "§7Click to browse"));
        shop.setItem(11, makeItem(Material.COBBLESTONE, "§e§lStone", "§7Click to browse"));
        shop.setItem(12, makeItem(Material.DIAMOND_ORE, "§e§lOres", "§7Click to browse"));
        shop.setItem(13, makeItem(Material.WHEAT, "§e§lCrops", "§7Click to browse"));
        shop.setItem(14, makeItem(Material.BONE, "§e§lMob Drops", "§7Click to browse"));
        shop.setItem(15, makeItem(Material.BRICKS, "§e§lBuilding", "§7Click to browse"));
        shop.setItem(16, makeItem(Material.CRAFTING_TABLE, "§e§lDecoration", "§7Click to browse"));

        shop.setItem(19, makeItem(Material.COOKED_BEEF, "§e§lFood", "§7Click to browse"));
        shop.setItem(20, makeItem(Material.IRON_SWORD, "§e§lTools", "§7Click to browse"));
        shop.setItem(21, makeItem(Material.IRON_CHESTPLATE, "§e§lArmor", "§7Click to browse"));
        shop.setItem(22, makeItem(Material.REDSTONE, "§e§lRedstone", "§7Click to browse"));
        shop.setItem(23, makeItem(Material.NETHERRACK, "§e§lNether", "§7Click to browse"));
        shop.setItem(24, makeItem(Material.END_STONE, "§e§lEnd", "§7Click to browse"));

        shop.setItem(40, makeItem(Material.EMERALD, "§a§lSell Items", "§7Click to open sell menu"));

        ItemStack glass = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (shop.getItem(i) == null) shop.setItem(i, glass);
        }

        player.openInventory(shop);
    }

    private void openCategory(Player player, String category, Material... materials) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§l" + category + " Shop");

        // Buy row 1: slots 10-16
        // Buy row 2: slots 19-25
        // Buy row 3: slots 28-34
        // Sell row 1: slots 37-43
        // Sell row 2: slots 46-52

        int buySlot = 10;
        int sellSlot = 37;

        for (int i = 0; i < materials.length; i++) {
            Material mat = materials[i];

            // Buy items - rows 1-3
            if (buySlot <= 34) {
                double buyPrice = priceManager.getBuyPrice(mat);
                inv.setItem(buySlot, makeItem(mat, "§aBuy " + formatName(mat),
                        "§ePrice: $" + String.format("%.2f", buyPrice),
                        "§7Click to buy 1"));
                buySlot++;
                if (buySlot == 17) buySlot = 19;
                if (buySlot == 26) buySlot = 28;
            }

            // Sell items - rows 4-5
            if (i < 14 && sellSlot <= 52) {
                double sellPrice = priceManager.getSellPrice(mat);
                inv.setItem(sellSlot, makeItem(mat, "§cSell " + formatName(mat),
                        "§eSell: $" + String.format("%.2f", sellPrice),
                        "§7Click to sell 1"));
                sellSlot++;
                if (sellSlot == 44) sellSlot = 46;
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
                Material.WARPED_STEM, Material.STRIPPED_OAK_LOG, Material.STRIPPED_BIRCH_LOG,
                Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_JUNGLE_LOG,
                Material.OAK_PLANKS, Material.BIRCH_PLANKS, Material.SPRUCE_PLANKS,
                Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
                Material.BAMBOO_PLANKS, Material.CRIMSON_PLANKS, Material.WARPED_PLANKS);
    }

    private void openStoneShop(Player player) {
        openCategory(player, "Stone",
                Material.COBBLESTONE, Material.STONE, Material.DEEPSLATE,
                Material.ANDESITE, Material.DIORITE, Material.GRANITE,
                Material.TUFF, Material.DRIPSTONE_BLOCK, Material.CALCITE,
                Material.SMOOTH_STONE, Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS,
                Material.CRACKED_STONE_BRICKS, Material.CHISELED_STONE_BRICKS,
                Material.COBBLED_DEEPSLATE, Material.DEEPSLATE_BRICKS, Material.POLISHED_DEEPSLATE,
                Material.POLISHED_ANDESITE, Material.POLISHED_DIORITE, Material.POLISHED_GRANITE,
                Material.MOSS_BLOCK, Material.MUD_BRICKS, Material.REINFORCED_DEEPSLATE);
    }

    private void openOresShop(Player player) {
        openCategory(player, "Ores",
                Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.COAL,
                Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT, Material.RAW_IRON,
                Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT, Material.RAW_COPPER,
                Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT, Material.RAW_GOLD,
                Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.REDSTONE,
                Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.LAPIS_LAZULI,
                Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DIAMOND,
                Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.EMERALD,
                Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE, Material.QUARTZ,
                Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP, Material.NETHERITE_INGOT,
                Material.AMETHYST_BLOCK, Material.AMETHYST_SHARD,
                Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.GOLD_BLOCK,
                Material.IRON_BLOCK, Material.COPPER_BLOCK, Material.LAPIS_BLOCK,
                Material.REDSTONE_BLOCK, Material.NETHERITE_BLOCK);
    }

    private void openCropsShop(Player player) {
        openCategory(player, "Crops",
                Material.WHEAT, Material.WHEAT_SEEDS, Material.HAY_BLOCK,
                Material.CARROT, Material.POTATO, Material.BAKED_POTATO,
                Material.BEETROOT, Material.BEETROOT_SEEDS,
                Material.MELON, Material.MELON_SLICE,
                Material.PUMPKIN, Material.CARVED_PUMPKIN, Material.JACK_O_LANTERN,
                Material.SUGAR_CANE, Material.BAMBOO,
                Material.COCOA_BEANS, Material.NETHER_WART,
                Material.CHORUS_FRUIT, Material.CHORUS_FLOWER,
                Material.SWEET_BERRIES, Material.GLOW_BERRIES,
                Material.APPLE, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE,
                Material.BROWN_MUSHROOM, Material.RED_MUSHROOM,
                Material.CACTUS, Material.KELP, Material.DRIED_KELP,
                Material.LILY_PAD, Material.VINE, Material.GLOW_LICHEN);
    }

    private void openMobDropsShop(Player player) {
        openCategory(player, "Mob Drops",
                Material.ROTTEN_FLESH, Material.BONE, Material.BONE_MEAL,
                Material.STRING, Material.SPIDER_EYE, Material.GUNPOWDER,
                Material.ENDER_PEARL, Material.BLAZE_ROD, Material.BLAZE_POWDER,
                Material.MAGMA_CREAM, Material.GHAST_TEAR,
                Material.WITHER_SKELETON_SKULL, Material.ZOMBIE_HEAD,
                Material.SKELETON_SKULL, Material.CREEPER_HEAD,
                Material.PHANTOM_MEMBRANE, Material.SHULKER_SHELL,
                Material.ELYTRA, Material.TOTEM_OF_UNDYING, Material.NETHER_STAR,
                Material.ARROW, Material.FLINT, Material.LEATHER,
                Material.FEATHER, Material.RABBIT_HIDE, Material.RABBIT_FOOT,
                Material.TRIDENT, Material.HEART_OF_THE_SEA,
                Material.SLIME_BALL, Material.SLIME_BLOCK,
                Material.HONEYCOMB, Material.HONEY_BOTTLE,
                Material.EGG, Material.INK_SAC, Material.GLOW_INK_SAC,
                Material.SCUTE, Material.CHARCOAL);
    }

    private void openFoodShop(Player player) {
        openCategory(player, "Food",
                Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_MUTTON,
                Material.COOKED_CHICKEN, Material.COOKED_RABBIT,
                Material.COOKED_COD, Material.COOKED_SALMON,
                Material.BREAD, Material.COOKIE, Material.PUMPKIN_PIE,
                Material.CAKE, Material.MUSHROOM_STEW,
                Material.BEETROOT_SOUP, Material.RABBIT_STEW,
                Material.SUSPICIOUS_STEW, Material.GOLDEN_CARROT,
                Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE,
                Material.BAKED_POTATO, Material.DRIED_KELP,
                Material.TROPICAL_FISH, Material.PUFFERFISH,
                Material.MELON_SLICE, Material.SWEET_BERRIES,
                Material.GLOW_BERRIES, Material.APPLE);
    }

    private void openToolsShop(Player player) {
        openCategory(player, "Tools",
                Material.WOODEN_SWORD, Material.WOODEN_PICKAXE, Material.WOODEN_AXE,
                Material.WOODEN_SHOVEL, Material.WOODEN_HOE,
                Material.STONE_SWORD, Material.STONE_PICKAXE, Material.STONE_AXE,
                Material.STONE_SHOVEL, Material.STONE_HOE,
                Material.IRON_SWORD, Material.IRON_PICKAXE, Material.IRON_AXE,
                Material.IRON_SHOVEL, Material.IRON_HOE,
                Material.GOLDEN_SWORD, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE,
                Material.GOLDEN_SHOVEL, Material.GOLDEN_HOE,
                Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE,
                Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE,
                Material.NETHERITE_SWORD, Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE,
                Material.NETHERITE_SHOVEL, Material.NETHERITE_HOE,
                Material.SHEARS, Material.FLINT_AND_STEEL,
                Material.BOW, Material.CROSSBOW, Material.FISHING_ROD);
    }

    private void openArmorShop(Player player) {
        openCategory(player, "Armor",
                Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
                Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
                Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE,
                Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE,
                Material.IRON_LEGGINGS, Material.IRON_BOOTS,
                Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE,
                Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
                Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
                Material.TURTLE_HELMET, Material.SHIELD, Material.ELYTRA);
    }

    private void openBuildingShop(Player player) {
        openCategory(player, "Building",
                Material.OAK_PLANKS, Material.OAK_FENCE, Material.OAK_STAIRS,
                Material.OAK_SLAB, Material.OAK_DOOR, Material.OAK_TRAPDOOR,
                Material.BIRCH_PLANKS, Material.BIRCH_FENCE, Material.BIRCH_STAIRS,
                Material.SPRUCE_PLANKS, Material.SPRUCE_FENCE, Material.SPRUCE_STAIRS,
                Material.BRICKS, Material.BRICK_STAIRS, Material.BRICK_SLAB,
                Material.NETHER_BRICKS, Material.NETHER_BRICK_STAIRS,
                Material.COBBLESTONE, Material.COBBLESTONE_STAIRS, Material.COBBLESTONE_SLAB,
                Material.STONE_BRICKS, Material.STONE_BRICK_STAIRS,
                Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
                Material.GLASS, Material.GLASS_PANE,
                Material.TERRACOTTA, Material.PRISMARINE,
                Material.END_STONE, Material.END_STONE_BRICKS,
                Material.PURPUR_BLOCK, Material.PURPUR_STAIRS,
                Material.BAMBOO_MOSAIC, Material.BAMBOO_FENCE, Material.BAMBOO_DOOR,
                Material.MUD_BRICKS, Material.MUD_BRICK_STAIRS);
    }

    private void openDecorationShop(Player player) {
        openCategory(player, "Decoration",
                Material.CRAFTING_TABLE, Material.FURNACE, Material.SMOKER,
                Material.BLAST_FURNACE, Material.STONECUTTER,
                Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
                Material.BREWING_STAND, Material.CAULDRON,
                Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST,
                Material.BARREL, Material.HOPPER,
                Material.DROPPER, Material.DISPENSER, Material.OBSERVER,
                Material.TORCH, Material.SOUL_TORCH,
                Material.LANTERN, Material.SOUL_LANTERN,
                Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
                Material.BELL, Material.LOOM,
                Material.JUKEBOX, Material.NOTE_BLOCK,
                Material.PAINTING, Material.ITEM_FRAME, Material.GLOW_ITEM_FRAME,
                Material.BOOKSHELF, Material.CHISELED_BOOKSHELF,
                Material.ENCHANTING_TABLE, Material.FLOWER_POT,
                Material.IRON_BARS, Material.CHAIN,
                Material.DRAGON_HEAD, Material.PLAYER_HEAD,
                Material.DECORATED_POT, Material.BONE_BLOCK);
    }

    private void openRedstoneShop(Player player) {
        openCategory(player, "Redstone",
                Material.REDSTONE, Material.REDSTONE_BLOCK, Material.REDSTONE_TORCH,
                Material.REDSTONE_LAMP, Material.PISTON, Material.STICKY_PISTON,
                Material.OBSERVER, Material.HOPPER, Material.DROPPER, Material.DISPENSER,
                Material.NOTE_BLOCK, Material.JUKEBOX,
                Material.DAYLIGHT_DETECTOR, Material.LEVER,
                Material.STONE_BUTTON, Material.OAK_BUTTON,
                Material.STONE_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE,
                Material.TNT, Material.TRIPWIRE_HOOK,
                Material.TRAPPED_CHEST, Material.SOUL_SAND,
                Material.SOUL_SOIL, Material.MAGMA_BLOCK, Material.SCAFFOLDING);
    }

    private void openNetherShop(Player player) {
        openCategory(player, "Nether",
                Material.NETHERRACK, Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS,
                Material.NETHER_BRICK_FENCE, Material.NETHER_BRICK_STAIRS,
                Material.NETHER_WART_BLOCK, Material.WARPED_WART_BLOCK,
                Material.SHROOMLIGHT, Material.GLOWSTONE,
                Material.BLACKSTONE, Material.POLISHED_BLACKSTONE,
                Material.POLISHED_BLACKSTONE_BRICKS, Material.GILDED_BLACKSTONE,
                Material.BASALT, Material.SMOOTH_BASALT, Material.POLISHED_BASALT,
                Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
                Material.SOUL_SOIL, Material.MAGMA_BLOCK,
                Material.CRYING_OBSIDIAN, Material.OBSIDIAN,
                Material.NETHERITE_BLOCK, Material.ANCIENT_DEBRIS,
                Material.CRIMSON_STEM, Material.WARPED_STEM,
                Material.CRIMSON_PLANKS, Material.WARPED_PLANKS);
    }

    private void openEndShop(Player player) {
        openCategory(player, "End",
                Material.END_STONE, Material.END_STONE_BRICKS,
                Material.PURPUR_BLOCK, Material.PURPUR_PILLAR,
                Material.PURPUR_STAIRS, Material.PURPUR_SLAB,
                Material.DRAGON_EGG, Material.DRAGON_BREATH,
                Material.END_CRYSTAL, Material.ENDER_EYE,
                Material.END_ROD, Material.CHORUS_FRUIT,
                Material.CHORUS_FLOWER, Material.POPPED_CHORUS_FRUIT,
                Material.SHULKER_BOX, Material.PURPLE_SHULKER_BOX,
                Material.ENDER_PEARL);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // Cancel ALL clicks in any shop inventory
        if (title.equals("§6§lShop") || title.endsWith(" Shop")) {
            event.setCancelled(true);
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

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
                case 19 -> openFoodShop(player);
                case 20 -> openToolsShop(player);
                case 21 -> openArmorShop(player);
                case 22 -> openRedstoneShop(player);
                case 23 -> openNetherShop(player);
                case 24 -> openEndShop(player);
                case 40 -> sellManager.openSellGui(player);
            }
            return;
        }

        // Category shops
        if (title.endsWith(" Shop")) {
            if (event.getSlot() == 4) {
                openShop(player);
                return;
            }

            Material material = clicked.getType();
            double buyPrice = priceManager.getBuyPrice(material);
            double sellPrice = priceManager.getSellPrice(material);

            // Buy items (rows 1-3: slots 10-16, 19-25, 28-34)
            if ((event.getSlot() >= 10 && event.getSlot() <= 16) ||
                (event.getSlot() >= 19 && event.getSlot() <= 25) ||
                (event.getSlot() >= 28 && event.getSlot() <= 34)) {
                if (balanceManager.removeBalance(player, buyPrice)) {
                    player.getInventory().addItem(new ItemStack(material, 1));
                    player.sendMessage("§aBought 1x " + formatName(material) + " for $" + String.format("%.2f", buyPrice));
                } else {
                    player.sendMessage("§cNot enough money! Need $" + String.format("%.2f", buyPrice));
                }
                return;
            }

            // Sell items (rows 4-5: slots 37-43, 46-52)
            if ((event.getSlot() >= 37 && event.getSlot() <= 43) ||
                (event.getSlot() >= 46 && event.getSlot() <= 52)) {
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

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals("§6§lShop") || title.endsWith(" Shop")) {
            event.setCancelled(true);
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
