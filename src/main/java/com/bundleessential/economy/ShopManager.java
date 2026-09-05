package com.bundleessential.economy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ShopManager implements Listener {

    private final BalanceManager balanceManager;
    private final PriceManager priceManager;
    private final SellManager sellManager;
    private final Map<UUID, ShopPage> playerPages = new HashMap<>();
    private final Map<String, Material[]> categories = new LinkedHashMap<>();
    private final Set<UUID> searching = new HashSet<>();
    private final Set<UUID> chatPending = new HashSet<>();

    private static final int ITEMS_PER_PAGE = 21;
    private static final int[] ITEM_SLOTS = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34
    };
    private static final String SEARCH_TITLE = "§b§lSearch items";

    public ShopManager(BalanceManager balanceManager, PriceManager priceManager, SellManager sellManager) {
        this.balanceManager = balanceManager;
        this.priceManager = priceManager;
        this.sellManager = sellManager;
        categories.put("Logs", logsItems());
        categories.put("Stone", stoneItems());
        categories.put("Ores", oresItems());
        categories.put("Crops", cropsItems());
        categories.put("Mob Drops", mobDropsItems());
        categories.put("Building", buildingItems());
        categories.put("Decoration", decorationItems());
        categories.put("Food", foodItems());
        categories.put("Tools", toolsItems());
        categories.put("Armor", armorItems());
        categories.put("Redstone", redstoneItems());
        categories.put("Nether", netherItems());
        categories.put("End", endItems());
        categories.put("New 1.21-26.2", latestItems());
    }

    /**
     * Version-safe resolver: works on 1.20.4 servers AND newer (1.21 -> 26.2) servers.
     * Unknown names on old servers are skipped instead of crashing compilation.
     */
    private Material[] mats(String... names) {
        List<Material> list = new ArrayList<>();
        for (String n : names) {
            try {
                Material m = Material.matchMaterial(n);
                if (m != null && m != Material.AIR && m.isItem()) {
                    list.add(m);
                }
            } catch (Exception ignored) {}
        }
        return list.toArray(new Material[0]);
    }

    private Material icon(String name, Material fallback) {
        try {
            Material m = Material.matchMaterial(name);
            if (m != null) return m;
        } catch (Exception ignored) {}
        return fallback;
    }

    private Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("BundledEssential");
    }

    public void openShop(Player player) {
        Inventory shop = Bukkit.createInventory(null, 54, "§6§lShop");

        shop.setItem(10, makeItem(Material.OAK_LOG, "§e§lLogs & Wood", "§7Logs, planks, leaves", "§7Click to browse"));
        shop.setItem(11, makeItem(Material.COBBLESTONE, "§e§lStone & Nature", "§7Stone, dirt, sand", "§7Click to browse"));
        shop.setItem(12, makeItem(Material.DIAMOND_ORE, "§e§lOres & Minerals", "§7Ores, ingots, blocks", "§7Click to browse"));
        shop.setItem(13, makeItem(Material.WHEAT, "§e§lCrops & Plants", "§7Farms, flowers, saplings", "§7Click to browse"));
        shop.setItem(14, makeItem(Material.BONE, "§e§lMob Drops", "§7Drops, heads, rare", "§7Click to browse"));
        shop.setItem(15, makeItem(Material.BRICKS, "§e§lBuilding", "§7Wool, concrete, glass", "§7Click to browse"));
        shop.setItem(16, makeItem(Material.CRAFTING_TABLE, "§e§lDecoration", "§7Furniture, lights", "§7Click to browse"));

        shop.setItem(19, makeItem(Material.COOKED_BEEF, "§e§lFood", "§7Raw + cooked", "§7Click to browse"));
        shop.setItem(20, makeItem(Material.IRON_SWORD, "§e§lTools & Weapons", "§7Swords, bows, mace, buckets", "§7Click to browse"));
        shop.setItem(21, makeItem(Material.IRON_CHESTPLATE, "§e§lArmor", "§7Armor, horse, harness", "§7Click to browse"));
        shop.setItem(22, makeItem(Material.REDSTONE, "§e§lRedstone", "§7Pistons, rails, crafter", "§7Click to browse"));
        shop.setItem(23, makeItem(Material.NETHERRACK, "§e§lNether", "§7Click to browse"));
        shop.setItem(24, makeItem(Material.END_STONE, "§e§lEnd", "§7Click to browse"));

        // NEW: 1.21 -> 26.2 items (Sulfur, Cinnabar, Pale, Resin, Copper, Happy Ghast...)
        shop.setItem(31, makeItem(icon("SULFUR", Material.NETHERITE_INGOT), "§d§lNew 1.21-26.2", "§7Copper, Tuff, Pale, Resin", "§7Sulfur, Cinnabar, Ghast...", "§7Click to browse"));
        shop.setItem(40, makeItem(Material.EMERALD, "§a§lSell Items", "§7Click to open sell menu"));
        shop.setItem(49, makeItem(Material.COMPASS, "§b§lSearch Items", "§7Type a name, jump to matches", "§7Click to search"));

        ItemStack glass = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (shop.getItem(i) == null) shop.setItem(i, glass);
        }

        player.openInventory(shop);
    }

    private void openCategoryPage(Player player, String category, Material[] materials, int page) {
        if (materials == null || materials.length == 0) {
            player.sendMessage("§cNothing available in " + category + " on this server version.");
            return;
        }
        int totalPages = (int) Math.ceil((double) materials.length / ITEMS_PER_PAGE);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        playerPages.put(player.getUniqueId(), new ShopPage(category, materials, page));

        Inventory inv = Bukkit.createInventory(null, 54, "§6§l" + category + " §7(Page " + (page + 1) + "/" + totalPages + ")");

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, materials.length);

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex >= ITEM_SLOTS.length) break;
            Material mat = materials[i];
            double buyPrice = priceManager.getBuyPrice(mat);
            inv.setItem(ITEM_SLOTS[slotIndex], makeItem(mat, "§a" + formatName(mat),
                    "§ePrice: $" + String.format("%.2f", buyPrice),
                    "§7Click to buy 1"));
        }

        inv.setItem(4, makeItem(Material.ARROW, "§cBack to Shop"));

        if (page > 0) {
            inv.setItem(48, makeItem(Material.ARROW, "§e§lPrevious Page", "§7Page " + page + "/" + totalPages));
        }
        if (page < totalPages - 1) {
            inv.setItem(50, makeItem(Material.ARROW, "§e§lNext Page", "§7Page " + (page + 2) + "/" + totalPages));
        }

        inv.setItem(49, makeItem(Material.PAPER, "§7" + (page + 1) + "/" + totalPages));

        ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        player.openInventory(inv);
    }

    private void openSearch(Player player) {
        searching.add(player.getUniqueId());
        Inventory anvil = Bukkit.createInventory(null, InventoryType.ANVIL, SEARCH_TITLE);
        anvil.setItem(0, makeItem(Material.PAPER, "§7Rename me to search..."));
        player.openInventory(anvil);
        player.sendMessage("§bType what to search in the anvil, then click the result. §7(Empty = type in chat instead)");
    }

    private Material[] searchItems(String query) {
        String q = query.toLowerCase().replace(" ", "").replace("_", "");
        Set<Material> out = new LinkedHashSet<>();
        if (q.isEmpty()) return new Material[0];
        for (Material[] arr : categories.values()) {
            for (Material m : arr) {
                String id = m.name().toLowerCase().replace("_", "");
                String nice = formatName(m).toLowerCase().replace(" ", "");
                if (id.contains(q) || nice.contains(q)) out.add(m);
            }
        }
        return out.toArray(new Material[0]);
    }

    private void showResults(Player player, String query) {
        Material[] matches = searchItems(query);
        if (matches.length == 0) {
            player.sendMessage("§cNo items found for '§e" + query + "§c'.");
            openShop(player);
            return;
        }
        String q = query.length() > 24 ? query.substring(0, 24) : query;
        openCategoryPage(player, "Search: " + q, matches, 0);
        player.sendMessage("§aFound §e" + matches.length + " §aitem(s) for '§e" + query + "§a'.");
    }

    /** Rename text of an anvil inventory via reflection (Paper-only API, null when unsupported). */
    private String getAnvilText(Inventory anvil) {
        try {
            java.lang.reflect.Method m = anvil.getClass().getMethod("getRenameText");
            Object o = m.invoke(anvil);
            return o == null ? null : o.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSearchAnvil(String title) {
        return SEARCH_TITLE.equals(title);
    }

    private Material[] logsItems() {
        return mats(
            "OAK_LOG","SPRUCE_LOG","BIRCH_LOG","JUNGLE_LOG","ACACIA_LOG","DARK_OAK_LOG",
            "MANGROVE_LOG","CHERRY_LOG","PALE_OAK_LOG",
            "CRIMSON_STEM","WARPED_STEM",
            "STRIPPED_OAK_LOG","STRIPPED_SPRUCE_LOG","STRIPPED_BIRCH_LOG","STRIPPED_JUNGLE_LOG",
            "STRIPPED_ACACIA_LOG","STRIPPED_DARK_OAK_LOG","STRIPPED_MANGROVE_LOG","STRIPPED_CHERRY_LOG",
            "STRIPPED_PALE_OAK_LOG","STRIPPED_CRIMSON_STEM","STRIPPED_WARPED_STEM",
            "OAK_WOOD","SPRUCE_WOOD","BIRCH_WOOD","JUNGLE_WOOD","ACACIA_WOOD","DARK_OAK_WOOD",
            "MANGROVE_WOOD","CHERRY_WOOD","PALE_OAK_WOOD","CRIMSON_HYPHAE","WARPED_HYPHAE",
            "STRIPPED_OAK_WOOD","STRIPPED_SPRUCE_WOOD","STRIPPED_BIRCH_WOOD","STRIPPED_JUNGLE_WOOD",
            "STRIPPED_ACACIA_WOOD","STRIPPED_DARK_OAK_WOOD","STRIPPED_MANGROVE_WOOD","STRIPPED_CHERRY_WOOD",
            "STRIPPED_PALE_OAK_WOOD","STRIPPED_CRIMSON_HYPHAE","STRIPPED_WARPED_HYPHAE",
            "OAK_PLANKS","SPRUCE_PLANKS","BIRCH_PLANKS","JUNGLE_PLANKS","ACACIA_PLANKS","DARK_OAK_PLANKS",
            "MANGROVE_PLANKS","CHERRY_PLANKS","PALE_OAK_PLANKS","CRIMSON_PLANKS","WARPED_PLANKS",
            "BAMBOO_PLANKS","BAMBOO_MOSAIC","BAMBOO_BLOCK","STRIPPED_BAMBOO_BLOCK","BAMBOO",
            "OAK_SAPLING","SPRUCE_SAPLING","BIRCH_SAPLING","JUNGLE_SAPLING","ACACIA_SAPLING",
            "DARK_OAK_SAPLING","MANGROVE_PROPAGULE","CHERRY_SAPLING","PALE_OAK_SAPLING",
            "OAK_LEAVES","SPRUCE_LEAVES","BIRCH_LEAVES","JUNGLE_LEAVES","ACACIA_LEAVES","DARK_OAK_LEAVES",
            "MANGROVE_LEAVES","CHERRY_LEAVES","PALE_OAK_LEAVES","AZALEA_LEAVES","FLOWERING_AZALEA_LEAVES",
            "MANGROVE_ROOTS","MUDDY_MANGROVE_ROOTS","AZALEA","FLOWERING_AZALEA","STICK"
        );
    }

    private void openLogsShop(Player player) {
        openCategoryPage(player, "Logs", logsItems(), 0);
    }

    private Material[] stoneItems() {
        return mats(
            "STONE","COBBLESTONE","MOSSY_COBBLESTONE","SMOOTH_STONE",
            "GRANITE","POLISHED_GRANITE","DIORITE","POLISHED_DIORITE","ANDESITE","POLISHED_ANDESITE",
            "DEEPSLATE","COBBLED_DEEPSLATE","POLISHED_DEEPSLATE","DEEPSLATE_BRICKS","CRACKED_DEEPSLATE_BRICKS",
            "DEEPSLATE_TILES","CRACKED_DEEPSLATE_TILES","CHISELED_DEEPSLATE",
            "TUFF","POLISHED_TUFF","TUFF_BRICKS","CHISELED_TUFF","CHISELED_TUFF_BRICKS",
            "TUFF_STAIRS","TUFF_SLAB","TUFF_WALL","POLISHED_TUFF_STAIRS","POLISHED_TUFF_SLAB","POLISHED_TUFF_WALL",
            "TUFF_BRICK_STAIRS","TUFF_BRICK_SLAB","TUFF_BRICK_WALL",
            "CALCITE","DRIPSTONE_BLOCK","POINTED_DRIPSTONE",
            "SANDSTONE","CHISELED_SANDSTONE","CUT_SANDSTONE","SMOOTH_SANDSTONE",
            "RED_SANDSTONE","CHISELED_RED_SANDSTONE","CUT_RED_SANDSTONE","SMOOTH_RED_SANDSTONE",
            "SAND","RED_SAND","GRAVEL","CLAY",
            "DIRT","COARSE_DIRT","ROOTED_DIRT","PODZOL","MYCELIUM","GRASS_BLOCK","MUD","PACKED_MUD",
            "STONE_BRICKS","MOSSY_STONE_BRICKS","CRACKED_STONE_BRICKS","CHISELED_STONE_BRICKS",
            "STONE_STAIRS","STONE_SLAB","COBBLESTONE_STAIRS","COBBLESTONE_SLAB","COBBLESTONE_WALL",
            "MOSSY_COBBLESTONE_STAIRS","MOSSY_COBBLESTONE_SLAB","MOSSY_COBBLESTONE_WALL",
            "SANDSTONE_STAIRS","SANDSTONE_SLAB","SANDSTONE_WALL",
            "RED_SANDSTONE_STAIRS","RED_SANDSTONE_SLAB","RED_SANDSTONE_WALL",
            "GRANITE_STAIRS","GRANITE_SLAB","GRANITE_WALL",
            "DIORITE_STAIRS","DIORITE_SLAB","DIORITE_WALL",
            "ANDESITE_STAIRS","ANDESITE_SLAB","ANDESITE_WALL",
            "DEEPSLATE_BRICK_STAIRS","DEEPSLATE_BRICK_SLAB","DEEPSLATE_BRICK_WALL",
            "DEEPSLATE_TILE_STAIRS","DEEPSLATE_TILE_SLAB","DEEPSLATE_TILE_WALL",
            "POLISHED_DEEPSLATE_STAIRS","POLISHED_DEEPSLATE_SLAB","POLISHED_DEEPSLATE_WALL",
            "COBBLED_DEEPSLATE_STAIRS","COBBLED_DEEPSLATE_SLAB","COBBLED_DEEPSLATE_WALL",
            "MOSS_BLOCK","MOSS_CARPET","PALE_MOSS_BLOCK","PALE_MOSS_CARPET",
            "OBSIDIAN","CRYING_OBSIDIAN","INFESTED_STONE","INFESTED_COBBLESTONE"
        );
    }

    private void openStoneShop(Player player) {
        openCategoryPage(player, "Stone", stoneItems(), 0);
    }

    private Material[] oresItems() {
        return mats(
            "COAL_ORE","DEEPSLATE_COAL_ORE","COAL","CHARCOAL","COAL_BLOCK",
            "IRON_ORE","DEEPSLATE_IRON_ORE","RAW_IRON","IRON_INGOT","IRON_NUGGET","IRON_BLOCK","RAW_IRON_BLOCK",
            "COPPER_ORE","DEEPSLATE_COPPER_ORE","RAW_COPPER","COPPER_INGOT","COPPER_BLOCK","RAW_COPPER_BLOCK",
            "CUT_COPPER","CUT_COPPER_STAIRS","CUT_COPPER_SLAB","CHISELED_COPPER",
            "EXPOSED_COPPER","WEATHERED_COPPER","OXIDIZED_COPPER","WAXED_COPPER_BLOCK",
            "GOLD_ORE","DEEPSLATE_GOLD_ORE","NETHER_GOLD_ORE","RAW_GOLD","GOLD_INGOT","GOLD_NUGGET",
            "GOLD_BLOCK","RAW_GOLD_BLOCK",
            "REDSTONE_ORE","DEEPSLATE_REDSTONE_ORE","REDSTONE","REDSTONE_BLOCK",
            "LAPIS_ORE","DEEPSLATE_LAPIS_ORE","LAPIS_LAZULI","LAPIS_BLOCK",
            "DIAMOND_ORE","DEEPSLATE_DIAMOND_ORE","DIAMOND","DIAMOND_BLOCK",
            "EMERALD_ORE","DEEPSLATE_EMERALD_ORE","EMERALD","EMERALD_BLOCK",
            "NETHER_QUARTZ_ORE","QUARTZ","QUARTZ_BLOCK","QUARTZ_BRICKS","QUARTZ_PILLAR",
            "CHISELED_QUARTZ_BLOCK","SMOOTH_QUARTZ","SMOOTH_QUARTZ_STAIRS","SMOOTH_QUARTZ_SLAB",
            "AMETHYST_BLOCK","AMETHYST_SHARD",
            "ANCIENT_DEBRIS","NETHERITE_SCRAP","NETHERITE_INGOT","NETHERITE_BLOCK",
            "GLOWSTONE","GLOWSTONE_DUST","QUARTZ",
            "RESIN_CLUMP","RESIN_BLOCK","RESIN_BRICKS","RESIN_BRICK_STAIRS","RESIN_BRICK_SLAB","RESIN_BRICK_WALL","CHISELED_RESIN_BRICKS",
            "HEAVY_CORE"
        );
    }

    private void openOresShop(Player player) {
        openCategoryPage(player, "Ores", oresItems(), 0);
    }

    private Material[] cropsItems() {
        return mats(
            "WHEAT","WHEAT_SEEDS","HAY_BLOCK",
            "CARROT","POTATO","POISONOUS_POTATO","BAKED_POTATO",
            "BEETROOT","BEETROOT_SEEDS","BEETROOT_SOUP",
            "MELON","MELON_SLICE","MELON_SEEDS","GLISTERING_MELON_SLICE",
            "PUMPKIN","CARVED_PUMPKIN","JACK_O_LANTERN","PUMPKIN_SEEDS","PUMPKIN_PIE",
            "SUGAR_CANE","PAPER","BAMBOO","COCOA_BEANS","COOKIE",
            "NETHER_WART","CHORUS_FRUIT","CHORUS_FLOWER","POPPED_CHORUS_FRUIT",
            "SWEET_BERRIES","GLOW_BERRIES","APPLE","GOLDEN_APPLE","ENCHANTED_GOLDEN_APPLE","GOLDEN_CARROT",
            "BROWN_MUSHROOM","RED_MUSHROOM","BROWN_MUSHROOM_BLOCK","RED_MUSHROOM_BLOCK","MUSHROOM_STEM","MUSHROOM_STEW",
            "SUSPICIOUS_STEW","RABBIT_STEW",
            "CACTUS","CACTUS_FLOWER","KELP","DRIED_KELP","DRIED_KELP_BLOCK","SEAGRASS","TALL_SEAGRASS",
            "LILY_PAD","VINE","GLOW_LICHEN","MOSS_BLOCK","MOSS_CARPET","PALE_MOSS_BLOCK","PALE_MOSS_CARPET",
            "HANGING_ROOTS","BIG_DRIPLEAF","SMALL_DRIPLEAF","SPORE_BLOSSOM",
            "TORCHFLOWER","TORCHFLOWER_SEEDS","PITCHER_PLANT","PITCHER_POD",
            "DANDELION","GOLDEN_DANDELION","POPPY","BLUE_ORCHID","ALLIUM","AZURE_BLUET",
            "RED_TULIP","ORANGE_TULIP","WHITE_TULIP","PINK_TULIP","OXEYE_DAISY","CORNFLOWER",
            "LILY_OF_THE_VALLEY","WITHER_ROSE","SUNFLOWER","LILAC","ROSE_BUSH","PEONY",
            "EYEBLOSSOM",
            "FIREFLY_BUSH","BUSH","SHORT_GRASS","TALL_GRASS","FERN","LARGE_FERN",
            "LEAF_LITTER","WILDFLOWERS","SHORT_DRY_GRASS","TALL_DRY_GRASS",
            "AZALEA","FLOWERING_AZALEA","PINK_PETALS","COCOA_BEANS"
        );
    }

    private void openCropsShop(Player player) {
        openCategoryPage(player, "Crops", cropsItems(), 0);
    }

    private Material[] mobDropsItems() {
        return mats(
            "ROTTEN_FLESH","BONE","BONE_MEAL","BONE_BLOCK",
            "ARROW","SPECTRAL_ARROW","STRING",
            "SPIDER_EYE","FERMENTED_SPIDER_EYE","GUNPOWDER",
            "ENDER_PEARL","ENDER_EYE","BLAZE_ROD","BLAZE_POWDER","BREEZE_ROD","WIND_CHARGE",
            "MAGMA_CREAM","GHAST_TEAR","SLIME_BALL","SLIME_BLOCK",
            "PRISMARINE_SHARD","PRISMARINE_CRYSTALS","NAUTILUS_SHELL","HEART_OF_THE_SEA",
            "PHANTOM_MEMBRANE","SHULKER_SHELL","DRAGON_BREATH","ECHO_SHARD","DISC_FRAGMENT_5",
            "ARMADILLO_SCUTE","SCUTE","TURTLE_EGG","SNIFFER_EGG",
            "INK_SAC","GLOW_INK_SAC","HONEYCOMB","HONEY_BOTTLE","HONEY_BLOCK","HONEYCOMB_BLOCK",
            "FEATHER","LEATHER","RABBIT_HIDE","RABBIT_FOOT","EGG","FLINT","CHARCOAL","COAL",
            "ZOMBIE_HEAD","SKELETON_SKULL","WITHER_SKELETON_SKULL","CREEPER_HEAD","DRAGON_HEAD","PIGLIN_HEAD","PLAYER_HEAD",
            "TOTEM_OF_UNDYING","NETHER_STAR","TRIDENT","ELYTRA",
            "SADDLE","NAME_TAG","LEAD",
            "SULFUR_CUBE_BUCKET","DRIED_GHAST"
        );
    }

    private void openMobDropsShop(Player player) {
        openCategoryPage(player, "Mob Drops", mobDropsItems(), 0);
    }

    private Material[] foodItems() {
        return mats(
            "BEEF","COOKED_BEEF","PORKCHOP","COOKED_PORKCHOP",
            "MUTTON","COOKED_MUTTON","CHICKEN","COOKED_CHICKEN",
            "RABBIT","COOKED_RABBIT","RABBIT_STEW",
            "COD","COOKED_COD","SALMON","COOKED_SALMON","TROPICAL_FISH","PUFFERFISH",
            "BREAD","COOKIE","CAKE","PUMPKIN_PIE",
            "MUSHROOM_STEW","BEETROOT_SOUP","SUSPICIOUS_STEW",
            "GOLDEN_CARROT","GLISTERING_MELON_SLICE","GOLDEN_APPLE","ENCHANTED_GOLDEN_APPLE",
            "BAKED_POTATO","POISONOUS_POTATO","DRIED_KELP","HONEY_BOTTLE","MILK_BUCKET",
            "MELON_SLICE","SWEET_BERRIES","GLOW_BERRIES","APPLE","CARROT","POTATO","BEETROOT",
            "CHORUS_FRUIT","POPPED_CHORUS_FRUIT","DRIED_KELP_BLOCK","HAY_BLOCK"
        );
    }

    private void openFoodShop(Player player) {
        openCategoryPage(player, "Food", foodItems(), 0);
    }

    private Material[] toolsItems() {
        return mats(
            "WOODEN_SWORD","WOODEN_PICKAXE","WOODEN_AXE","WOODEN_SHOVEL","WOODEN_HOE",
            "STONE_SWORD","STONE_PICKAXE","STONE_AXE","STONE_SHOVEL","STONE_HOE",
            "IRON_SWORD","IRON_PICKAXE","IRON_AXE","IRON_SHOVEL","IRON_HOE",
            "GOLDEN_SWORD","GOLDEN_PICKAXE","GOLDEN_AXE","GOLDEN_SHOVEL","GOLDEN_HOE",
            "DIAMOND_SWORD","DIAMOND_PICKAXE","DIAMOND_AXE","DIAMOND_SHOVEL","DIAMOND_HOE",
            "NETHERITE_SWORD","NETHERITE_PICKAXE","NETHERITE_AXE","NETHERITE_SHOVEL","NETHERITE_HOE",
            "NETHERITE_UPGRADE_SMITHING_TEMPLATE","MACE",
            "BOW","CROSSBOW","ARROW","SPECTRAL_ARROW","TRIDENT","SHIELD",
            "FISHING_ROD","CARROT_ON_A_STICK","WARPED_FUNGUS_ON_A_STICK",
            "SHEARS","FLINT_AND_STEEL","BRUSH","BUNDLE",
            "WHITE_BUNDLE","ORANGE_BUNDLE","MAGENTA_BUNDLE","LIGHT_BLUE_BUNDLE","YELLOW_BUNDLE",
            "LIME_BUNDLE","PINK_BUNDLE","GRAY_BUNDLE","LIGHT_GRAY_BUNDLE","CYAN_BUNDLE",
            "PURPLE_BUNDLE","BLUE_BUNDLE","BROWN_BUNDLE","GREEN_BUNDLE","RED_BUNDLE","BLACK_BUNDLE",
            "BUCKET","WATER_BUCKET","LAVA_BUCKET","MILK_BUCKET","POWDER_SNOW_BUCKET",
            "COD_BUCKET","SALMON_BUCKET","TROPICAL_FISH_BUCKET","PUFFERFISH_BUCKET",
            "AXOLOTL_BUCKET","TADPOLE_BUCKET","SULFUR_CUBE_BUCKET",
            "COMPASS","RECOVERY_COMPASS","CLOCK","MAP","LEAD","NAME_TAG","SADDLE",
            "MINECART","CHEST_MINECART","HOPPER_MINECART","TNT_MINECART",
            "OAK_BOAT","SPRUCE_BOAT","BIRCH_BOAT","JUNGLE_BOAT","ACACIA_BOAT","DARK_OAK_BOAT",
            "MANGROVE_BOAT","CHERRY_BOAT","PALE_OAK_BOAT","BAMBOO_RAFT",
            "OAK_CHEST_BOAT","SPRUCE_CHEST_BOAT","BIRCH_CHEST_BOAT","JUNGLE_CHEST_BOAT",
            "ACACIA_CHEST_BOAT","DARK_OAK_CHEST_BOAT","MANGROVE_CHEST_BOAT","CHERRY_CHEST_BOAT",
            "PALE_OAK_CHEST_BOAT","BAMBOO_CHEST_RAFT"
        );
    }

    private void openToolsShop(Player player) {
        openCategoryPage(player, "Tools", toolsItems(), 0);
    }

    private Material[] armorItems() {
        return mats(
            "LEATHER_HELMET","LEATHER_CHESTPLATE","LEATHER_LEGGINGS","LEATHER_BOOTS",
            "CHAINMAIL_HELMET","CHAINMAIL_CHESTPLATE","CHAINMAIL_LEGGINGS","CHAINMAIL_BOOTS",
            "IRON_HELMET","IRON_CHESTPLATE","IRON_LEGGINGS","IRON_BOOTS",
            "GOLDEN_HELMET","GOLDEN_CHESTPLATE","GOLDEN_LEGGINGS","GOLDEN_BOOTS",
            "DIAMOND_HELMET","DIAMOND_CHESTPLATE","DIAMOND_LEGGINGS","DIAMOND_BOOTS",
            "NETHERITE_HELMET","NETHERITE_CHESTPLATE","NETHERITE_LEGGINGS","NETHERITE_BOOTS",
            "NETHERITE_UPGRADE_SMITHING_TEMPLATE",
            "TURTLE_HELMET","SHIELD","ELYTRA",
            "LEATHER_HORSE_ARMOR","IRON_HORSE_ARMOR","GOLDEN_HORSE_ARMOR","DIAMOND_HORSE_ARMOR",
            "WOLF_ARMOR",
            "WHITE_HARNESS","ORANGE_HARNESS","MAGENTA_HARNESS","LIGHT_BLUE_HARNESS","YELLOW_HARNESS",
            "LIME_HARNESS","PINK_HARNESS","GRAY_HARNESS","LIGHT_GRAY_HARNESS","CYAN_HARNESS",
            "PURPLE_HARNESS","BLUE_HARNESS","BROWN_HARNESS","GREEN_HARNESS","RED_HARNESS","BLACK_HARNESS",
            "SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE","DUNE_ARMOR_TRIM_SMITHING_TEMPLATE",
            "COAST_ARMOR_TRIM_SMITHING_TEMPLATE","WILD_ARMOR_TRIM_SMITHING_TEMPLATE",
            "WARD_ARMOR_TRIM_SMITHING_TEMPLATE","TIDE_ARMOR_TRIM_SMITHING_TEMPLATE",
            "VEX_ARMOR_TRIM_SMITHING_TEMPLATE","SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE",
            "RIB_ARMOR_TRIM_SMITHING_TEMPLATE","EYE_ARMOR_TRIM_SMITHING_TEMPLATE",
            "SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE","FLOW_ARMOR_TRIM_SMITHING_TEMPLATE",
            "BOLT_ARMOR_TRIM_SMITHING_TEMPLATE"
        );
    }

    private void openArmorShop(Player player) {
        openCategoryPage(player, "Armor", armorItems(), 0);
    }

    private Material[] buildingItems() {
        return mats(
            "WHITE_WOOL","ORANGE_WOOL","MAGENTA_WOOL","LIGHT_BLUE_WOOL","YELLOW_WOOL","LIME_WOOL",
            "PINK_WOOL","GRAY_WOOL","LIGHT_GRAY_WOOL","CYAN_WOOL","PURPLE_WOOL","BLUE_WOOL",
            "BROWN_WOOL","GREEN_WOOL","RED_WOOL","BLACK_WOOL",
            "WHITE_CONCRETE","ORANGE_CONCRETE","MAGENTA_CONCRETE","LIGHT_BLUE_CONCRETE","YELLOW_CONCRETE","LIME_CONCRETE",
            "PINK_CONCRETE","GRAY_CONCRETE","LIGHT_GRAY_CONCRETE","CYAN_CONCRETE","PURPLE_CONCRETE","BLUE_CONCRETE",
            "BROWN_CONCRETE","GREEN_CONCRETE","RED_CONCRETE","BLACK_CONCRETE",
            "WHITE_CONCRETE_POWDER","ORANGE_CONCRETE_POWDER","MAGENTA_CONCRETE_POWDER","LIGHT_BLUE_CONCRETE_POWDER",
            "YELLOW_CONCRETE_POWDER","LIME_CONCRETE_POWDER","PINK_CONCRETE_POWDER","GRAY_CONCRETE_POWDER",
            "LIGHT_GRAY_CONCRETE_POWDER","CYAN_CONCRETE_POWDER","PURPLE_CONCRETE_POWDER","BLUE_CONCRETE_POWDER",
            "BROWN_CONCRETE_POWDER","GREEN_CONCRETE_POWDER","RED_CONCRETE_POWDER","BLACK_CONCRETE_POWDER",
            "TERRACOTTA","WHITE_TERRACOTTA","ORANGE_TERRACOTTA","MAGENTA_TERRACOTTA","LIGHT_BLUE_TERRACOTTA",
            "YELLOW_TERRACOTTA","LIME_TERRACOTTA","PINK_TERRACOTTA","GRAY_TERRACOTTA","LIGHT_GRAY_TERRACOTTA",
            "CYAN_TERRACOTTA","PURPLE_TERRACOTTA","BLUE_TERRACOTTA","BROWN_TERRACOTTA","GREEN_TERRACOTTA",
            "RED_TERRACOTTA","BLACK_TERRACOTTA",
            "GLASS","TINTED_GLASS","GLASS_PANE",
            "WHITE_STAINED_GLASS","ORANGE_STAINED_GLASS","MAGENTA_STAINED_GLASS","LIGHT_BLUE_STAINED_GLASS",
            "YELLOW_STAINED_GLASS","LIME_STAINED_GLASS","PINK_STAINED_GLASS","GRAY_STAINED_GLASS",
            "LIGHT_GRAY_STAINED_GLASS","CYAN_STAINED_GLASS","PURPLE_STAINED_GLASS","BLUE_STAINED_GLASS",
            "BROWN_STAINED_GLASS","GREEN_STAINED_GLASS","RED_STAINED_GLASS","BLACK_STAINED_GLASS",
            "WHITE_STAINED_GLASS_PANE","ORANGE_STAINED_GLASS_PANE","MAGENTA_STAINED_GLASS_PANE",
            "LIGHT_BLUE_STAINED_GLASS_PANE","YELLOW_STAINED_GLASS_PANE","LIME_STAINED_GLASS_PANE",
            "PINK_STAINED_GLASS_PANE","GRAY_STAINED_GLASS_PANE","LIGHT_GRAY_STAINED_GLASS_PANE",
            "CYAN_STAINED_GLASS_PANE","PURPLE_STAINED_GLASS_PANE","BLUE_STAINED_GLASS_PANE",
            "BROWN_STAINED_GLASS_PANE","GREEN_STAINED_GLASS_PANE","RED_STAINED_GLASS_PANE","BLACK_STAINED_GLASS_PANE",
            "OAK_STAIRS","OAK_SLAB","OAK_FENCE","OAK_FENCE_GATE","OAK_DOOR","OAK_TRAPDOOR",
            "SPRUCE_STAIRS","SPRUCE_SLAB","SPRUCE_FENCE","SPRUCE_FENCE_GATE","SPRUCE_DOOR","SPRUCE_TRAPDOOR",
            "BIRCH_STAIRS","BIRCH_SLAB","BIRCH_FENCE","BIRCH_FENCE_GATE","BIRCH_DOOR","BIRCH_TRAPDOOR",
            "BRICKS","BRICK_STAIRS","BRICK_SLAB","BRICK_WALL",
            "STONE_BRICKS","STONE_BRICK_STAIRS","STONE_BRICK_SLAB","STONE_BRICK_WALL",
            "MUD_BRICKS","MUD_BRICK_STAIRS","MUD_BRICK_SLAB","MUD_BRICK_WALL",
            "NETHER_BRICKS","NETHER_BRICK_STAIRS","NETHER_BRICK_SLAB","NETHER_BRICK_WALL","NETHER_BRICK_FENCE",
            "RED_NETHER_BRICKS","RED_NETHER_BRICK_STAIRS","RED_NETHER_BRICK_SLAB","RED_NETHER_BRICK_WALL",
            "END_STONE","END_STONE_BRICKS","END_STONE_BRICK_STAIRS","END_STONE_BRICK_SLAB","END_STONE_BRICK_WALL",
            "PURPUR_BLOCK","PURPUR_PILLAR","PURPUR_STAIRS","PURPUR_SLAB",
            "QUARTZ_BLOCK","QUARTZ_BRICKS","QUARTZ_PILLAR","CHISELED_QUARTZ_BLOCK","SMOOTH_QUARTZ",
            "PRISMARINE","PRISMARINE_BRICKS","DARK_PRISMARINE","SEA_LANTERN",
            "OBSIDIAN","CRYING_OBSIDIAN","GLOWSTONE","SHROOMLIGHT",
            "CUT_COPPER","CUT_COPPER_STAIRS","CUT_COPPER_SLAB","CHISELED_COPPER","COPPER_GRATE",
            "RESIN_BRICKS","RESIN_BRICK_STAIRS","RESIN_BRICK_SLAB","RESIN_BRICK_WALL","CHISELED_RESIN_BRICKS",
            "SULFUR","POLISHED_SULFUR","SULFUR_BRICKS","CHISELED_SULFUR",
            "CINNABAR","POLISHED_CINNABAR","CINNABAR_BRICKS","CHISELED_CINNABAR"
        );
    }

    private void openBuildingShop(Player player) {
        openCategoryPage(player, "Building", buildingItems(), 0);
    }

    private Material[] decorationItems() {
        return mats(
            "CRAFTING_TABLE","FURNACE","BLAST_FURNACE","SMOKER","BREWING_STAND","CAULDRON",
            "COMPOSTER","BARREL","CHEST","TRAPPED_CHEST","ENDER_CHEST",
            "SHULKER_BOX","WHITE_SHULKER_BOX","ORANGE_SHULKER_BOX","MAGENTA_SHULKER_BOX",
            "LIGHT_BLUE_SHULKER_BOX","YELLOW_SHULKER_BOX","LIME_SHULKER_BOX","PINK_SHULKER_BOX",
            "GRAY_SHULKER_BOX","LIGHT_GRAY_SHULKER_BOX","CYAN_SHULKER_BOX","PURPLE_SHULKER_BOX",
            "BLUE_SHULKER_BOX","BROWN_SHULKER_BOX","GREEN_SHULKER_BOX","RED_SHULKER_BOX","BLACK_SHULKER_BOX",
            "HOPPER","DROPPER","DISPENSER","OBSERVER","CRAFTER",
            "ENCHANTING_TABLE","ANVIL","CHIPPED_ANVIL","DAMAGED_ANVIL","GRINDSTONE","STONECUTTER",
            "SMITHING_TABLE","CARTOGRAPHY_TABLE","FLETCHING_TABLE","LOOM","LECTERN","BOOKSHELF","CHISELED_BOOKSHELF",
            "TORCH","SOUL_TORCH","LANTERN","SOUL_LANTERN","SEA_LANTERN","GLOWSTONE","SHROOMLIGHT","END_ROD",
            "REDSTONE_LAMP","COPPER_BULB","EXPOSED_COPPER_BULB","WEATHERED_COPPER_BULB","OXIDIZED_COPPER_BULB",
            "CAMPFIRE","SOUL_CAMPFIRE","BELL","BEACON","CONDUIT","LODESTONE","RESPAWN_ANCHOR",
            "JUKEBOX","NOTE_BLOCK",
            "MUSIC_DISC_13","MUSIC_DISC_CAT","MUSIC_DISC_BLOCKS","MUSIC_DISC_CHIRP","MUSIC_DISC_FAR",
            "MUSIC_DISC_MALL","MUSIC_DISC_MELLOHI","MUSIC_DISC_STAL","MUSIC_DISC_STRAD","MUSIC_DISC_WARD",
            "MUSIC_DISC_11","MUSIC_DISC_WAIT","MUSIC_DISC_OTHERSIDE","MUSIC_DISC_RELIC","MUSIC_DISC_5",
            "MUSIC_DISC_PIGSTEP","MUSIC_DISC_CREATOR","MUSIC_DISC_CREATOR_MUSIC_BOX","MUSIC_DISC_PRECIPICE",
            "MUSIC_DISC_BOUNCE","MUSIC_DISC_TEARS",
            "PAINTING","ITEM_FRAME","GLOW_ITEM_FRAME","ARMOR_STAND","FLOWER_POT","DECORATED_POT",
            "CANDLE","WHITE_CANDLE","ORANGE_CANDLE","MAGENTA_CANDLE","LIGHT_BLUE_CANDLE","YELLOW_CANDLE",
            "LIME_CANDLE","PINK_CANDLE","GRAY_CANDLE","LIGHT_GRAY_CANDLE","CYAN_CANDLE","PURPLE_CANDLE",
            "BLUE_CANDLE","BROWN_CANDLE","GREEN_CANDLE","RED_CANDLE","BLACK_CANDLE",
            "WHITE_BED","ORANGE_BED","MAGENTA_BED","LIGHT_BLUE_BED","YELLOW_BED","LIME_BED",
            "PINK_BED","GRAY_BED","LIGHT_GRAY_BED","CYAN_BED","PURPLE_BED","BLUE_BED",
            "BROWN_BED","GREEN_BED","RED_BED","BLACK_BED",
            "WHITE_BANNER","ORANGE_BANNER","MAGENTA_BANNER","LIGHT_BLUE_BANNER","YELLOW_BANNER",
            "LIME_BANNER","PINK_BANNER","GRAY_BANNER","LIGHT_GRAY_BANNER","CYAN_BANNER",
            "PURPLE_BANNER","BLUE_BANNER","BROWN_BANNER","GREEN_BANNER","RED_BANNER","BLACK_BANNER",
            "OAK_SIGN","SPRUCE_SIGN","BIRCH_SIGN","JUNGLE_SIGN","ACACIA_SIGN","DARK_OAK_SIGN",
            "MANGROVE_SIGN","CHERRY_SIGN","PALE_OAK_SIGN","BAMBOO_SIGN","CRIMSON_SIGN","WARPED_SIGN",
            "OAK_HANGING_SIGN","SPRUCE_HANGING_SIGN","BIRCH_HANGING_SIGN","CHERRY_HANGING_SIGN","PALE_OAK_HANGING_SIGN",
            "BAMBOO_HANGING_SIGN",
            "IRON_BARS","CHAIN","LADDER","SCAFFOLDING",
            "DRAGON_HEAD","ZOMBIE_HEAD","SKELETON_SKULL","WITHER_SKELETON_SKULL","CREEPER_HEAD","PIGLIN_HEAD","PLAYER_HEAD",
            "OAK_SHELF","SPRUCE_SHELF","BIRCH_SHELF","JUNGLE_SHELF","ACACIA_SHELF","DARK_OAK_SHELF",
            "MANGROVE_SHELF","CHERRY_SHELF","PALE_OAK_SHELF","BAMBOO_SHELF","CRIMSON_SHELF","WARPED_SHELF",
            "COPPER_CHEST","EXPOSED_COPPER_CHEST","WEATHERED_COPPER_CHEST","OXIDIZED_COPPER_CHEST","WAXED_COPPER_CHEST"
        );
    }

    private void openDecorationShop(Player player) {
        openCategoryPage(player, "Decoration", decorationItems(), 0);
    }

    private Material[] redstoneItems() {
        return mats(
            "REDSTONE","REDSTONE_BLOCK","REDSTONE_TORCH","REDSTONE_LAMP",
            "LEVER","STONE_BUTTON","OAK_BUTTON","SPRUCE_BUTTON","BIRCH_BUTTON","JUNGLE_BUTTON",
            "ACACIA_BUTTON","DARK_OAK_BUTTON","MANGROVE_BUTTON","CHERRY_BUTTON","PALE_OAK_BUTTON",
            "BAMBOO_BUTTON","CRIMSON_BUTTON","WARPED_BUTTON","POLISHED_BLACKSTONE_BUTTON",
            "STONE_PRESSURE_PLATE","OAK_PRESSURE_PLATE","SPRUCE_PRESSURE_PLATE","BIRCH_PRESSURE_PLATE",
            "LIGHT_WEIGHTED_PRESSURE_PLATE","HEAVY_WEIGHTED_PRESSURE_PLATE",
            "PISTON","STICKY_PISTON","OBSERVER","HOPPER","DROPPER","DISPENSER","CRAFTER",
            "COMPARATOR","REPEATER","DAYLIGHT_DETECTOR",
            "SCULK_SENSOR","CALIBRATED_SCULK_SENSOR","SCULK","SCULK_CATALYST","SCULK_SHRIEKER","SCULK_VEIN",
            "NOTE_BLOCK","JUKEBOX","TNT","TRIPWIRE_HOOK",
            "RAIL","POWERED_RAIL","DETECTOR_RAIL","ACTIVATOR_RAIL",
            "MINECART","CHEST_MINECART","HOPPER_MINECART","TNT_MINECART",
            "TARGET","LIGHTNING_ROD",
            "COPPER_BULB","EXPOSED_COPPER_BULB","WEATHERED_COPPER_BULB","OXIDIZED_COPPER_BULB",
            "WAXED_COPPER_BULB","WAXED_EXPOSED_COPPER_BULB","WAXED_WEATHERED_COPPER_BULB","WAXED_OXIDIZED_COPPER_BULB",
            "TRAPPED_CHEST","DAYLIGHT_DETECTOR","SCAFFOLDING"
        );
    }

    private void openRedstoneShop(Player player) {
        openCategoryPage(player, "Redstone", redstoneItems(), 0);
    }

    private Material[] netherItems() {
        return mats(
            "NETHERRACK","NETHER_BRICKS","RED_NETHER_BRICKS","CHISELED_NETHER_BRICKS","CRACKED_NETHER_BRICKS",
            "NETHER_BRICK_FENCE","NETHER_BRICK_STAIRS","NETHER_BRICK_SLAB","NETHER_BRICK_WALL",
            "RED_NETHER_BRICK_STAIRS","RED_NETHER_BRICK_SLAB","RED_NETHER_BRICK_WALL",
            "NETHER_WART","NETHER_WART_BLOCK","WARPED_WART_BLOCK",
            "SHROOMLIGHT","GLOWSTONE","GLOWSTONE_DUST",
            "BLACKSTONE","POLISHED_BLACKSTONE","POLISHED_BLACKSTONE_BRICKS",
            "CRACKED_POLISHED_BLACKSTONE_BRICKS","CHISELED_POLISHED_BLACKSTONE","GILDED_BLACKSTONE",
            "BLACKSTONE_STAIRS","BLACKSTONE_SLAB","BLACKSTONE_WALL",
            "POLISHED_BLACKSTONE_STAIRS","POLISHED_BLACKSTONE_SLAB","POLISHED_BLACKSTONE_WALL",
            "POLISHED_BLACKSTONE_BRICK_STAIRS","POLISHED_BLACKSTONE_BRICK_SLAB","POLISHED_BLACKSTONE_BRICK_WALL",
            "BASALT","SMOOTH_BASALT","POLISHED_BASALT",
            "SOUL_SAND","SOUL_SOIL","SOUL_TORCH","SOUL_LANTERN","SOUL_CAMPFIRE",
            "MAGMA_BLOCK","CRYING_OBSIDIAN","OBSIDIAN","RESPAWN_ANCHOR","LODESTONE",
            "CRIMSON_NYLIUM","WARPED_NYLIUM","CRIMSON_ROOTS","WARPED_ROOTS","NETHER_SPROUTS",
            "CRIMSON_FUNGUS","WARPED_FUNGUS","WEEPING_VINES","TWISTING_VINES",
            "CRIMSON_STEM","WARPED_STEM","CRIMSON_PLANKS","WARPED_PLANKS",
            "CRIMSON_DOOR","WARPED_DOOR","CRIMSON_TRAPDOOR","WARPED_TRAPDOOR","CRIMSON_FENCE","WARPED_FENCE",
            "NETHER_GOLD_ORE","NETHER_QUARTZ_ORE","QUARTZ",
            "ANCIENT_DEBRIS","NETHERITE_SCRAP","NETHERITE_INGOT","NETHERITE_BLOCK"
        );
    }

    private void openNetherShop(Player player) {
        openCategoryPage(player, "Nether", netherItems(), 0);
    }

    private Material[] endItems() {
        return mats(
            "END_STONE","END_STONE_BRICKS","END_STONE_BRICK_STAIRS","END_STONE_BRICK_SLAB","END_STONE_BRICK_WALL",
            "PURPUR_BLOCK","PURPUR_PILLAR","PURPUR_STAIRS","PURPUR_SLAB",
            "END_ROD","CHORUS_FLOWER","CHORUS_FRUIT","POPPED_CHORUS_FRUIT",
            "ENDER_PEARL","ENDER_EYE","ENDER_CHEST",
            "SHULKER_BOX","WHITE_SHULKER_BOX","ORANGE_SHULKER_BOX","MAGENTA_SHULKER_BOX",
            "LIGHT_BLUE_SHULKER_BOX","YELLOW_SHULKER_BOX","LIME_SHULKER_BOX","PINK_SHULKER_BOX",
            "GRAY_SHULKER_BOX","LIGHT_GRAY_SHULKER_BOX","CYAN_SHULKER_BOX","PURPLE_SHULKER_BOX",
            "BLUE_SHULKER_BOX","BROWN_SHULKER_BOX","GREEN_SHULKER_BOX","RED_SHULKER_BOX","BLACK_SHULKER_BOX",
            "DRAGON_EGG","DRAGON_BREATH","DRAGON_HEAD","END_CRYSTAL","ELYTRA"
        );
    }

    private void openEndShop(Player player) {
        openCategoryPage(player, "End", endItems(), 0);
    }

    private Material[] latestItems() {
        return mats(
            // 1.21 Tricky Trials
            "COPPER_DOOR","EXPOSED_COPPER_DOOR","WEATHERED_COPPER_DOOR","OXIDIZED_COPPER_DOOR",
            "WAXED_COPPER_DOOR","WAXED_EXPOSED_COPPER_DOOR","WAXED_WEATHERED_COPPER_DOOR","WAXED_OXIDIZED_COPPER_DOOR",
            "COPPER_TRAPDOOR","EXPOSED_COPPER_TRAPDOOR","WEATHERED_COPPER_TRAPDOOR","OXIDIZED_COPPER_TRAPDOOR",
            "WAXED_COPPER_TRAPDOOR","COPPER_GRATE","EXPOSED_COPPER_GRATE","WEATHERED_COPPER_GRATE","OXIDIZED_COPPER_GRATE",
            "WAXED_COPPER_GRATE","COPPER_BULB","EXPOSED_COPPER_BULB","WEATHERED_COPPER_BULB","OXIDIZED_COPPER_BULB",
            "WAXED_COPPER_BULB","CHISELED_COPPER","EXPOSED_CHISELED_COPPER","WEATHERED_CHISELED_COPPER","OXIDIZED_CHISELED_COPPER",
            "TUFF_STAIRS","TUFF_SLAB","TUFF_WALL","POLISHED_TUFF","POLISHED_TUFF_STAIRS","POLISHED_TUFF_SLAB","POLISHED_TUFF_WALL",
            "TUFF_BRICKS","TUFF_BRICK_STAIRS","TUFF_BRICK_SLAB","TUFF_BRICK_WALL","CHISELED_TUFF","CHISELED_TUFF_BRICKS",
            "CRAFTER","HEAVY_CORE","MACE","BREEZE_ROD","WIND_CHARGE",
            "TRIAL_KEY","OMINOUS_TRIAL_KEY","BUNDLE",
            "WHITE_BUNDLE","ORANGE_BUNDLE","MAGENTA_BUNDLE","LIGHT_BLUE_BUNDLE","YELLOW_BUNDLE",
            "LIME_BUNDLE","PINK_BUNDLE","GRAY_BUNDLE","LIGHT_GRAY_BUNDLE","CYAN_BUNDLE",
            "PURPLE_BUNDLE","BLUE_BUNDLE","BROWN_BUNDLE","GREEN_BUNDLE","RED_BUNDLE","BLACK_BUNDLE",
            // 1.21.4 Pale Garden
            "PALE_OAK_LOG","PALE_OAK_WOOD","STRIPPED_PALE_OAK_LOG","STRIPPED_PALE_OAK_WOOD",
            "PALE_OAK_PLANKS","PALE_OAK_STAIRS","PALE_OAK_SLAB","PALE_OAK_FENCE","PALE_OAK_FENCE_GATE",
            "PALE_OAK_DOOR","PALE_OAK_TRAPDOOR","PALE_OAK_BUTTON","PALE_OAK_PRESSURE_PLATE",
            "PALE_OAK_SAPLING","PALE_OAK_LEAVES","PALE_MOSS_BLOCK","PALE_MOSS_CARPET",
            "PALE_HANGING_MOSS","CREAKING_HEART",
            "EYEBLOSSOM",
            "RESIN_CLUMP","RESIN_BLOCK","RESIN_BRICKS","RESIN_BRICK_STAIRS","RESIN_BRICK_SLAB",
            "RESIN_BRICK_WALL","CHISELED_RESIN_BRICKS",
            // 1.21.5 Spring to Life
            "BUSH","FIREFLY_BUSH","LEAF_LITTER","WILDFLOWERS","SHORT_DRY_GRASS","TALL_DRY_GRASS","CACTUS_FLOWER",
            "OAK_SHELF","SPRUCE_SHELF","BIRCH_SHELF","JUNGLE_SHELF","ACACIA_SHELF","DARK_OAK_SHELF",
            "MANGROVE_SHELF","CHERRY_SHELF","PALE_OAK_SHELF","BAMBOO_SHELF","CRIMSON_SHELF","WARPED_SHELF",
            // 1.21.6 Happy Ghast
            "DRIED_GHAST",
            "WHITE_HARNESS","ORANGE_HARNESS","MAGENTA_HARNESS","LIGHT_BLUE_HARNESS","YELLOW_HARNESS",
            "LIME_HARNESS","PINK_HARNESS","GRAY_HARNESS","LIGHT_GRAY_HARNESS","CYAN_HARNESS",
            "PURPLE_HARNESS","BLUE_HARNESS","BROWN_HARNESS","GREEN_HARNESS","RED_HARNESS","BLACK_HARNESS",
            // Copper chest (1.21.8+)
            "COPPER_CHEST","EXPOSED_COPPER_CHEST","WEATHERED_COPPER_CHEST","OXIDIZED_COPPER_CHEST",
            "WAXED_COPPER_CHEST","WAXED_EXPOSED_COPPER_CHEST","WAXED_WEATHERED_COPPER_CHEST","WAXED_OXIDIZED_COPPER_CHEST",
            "COPPER_GOLEM_STATUE","EXPOSED_COPPER_GOLEM_STATUE","WEATHERED_COPPER_GOLEM_STATUE","OXIDIZED_COPPER_GOLEM_STATUE",
            // 26.1 Tiny Takeover
            "GOLDEN_DANDELION",
            // 26.2 Chaos Cubed
            "SULFUR","SULFUR_STAIRS","SULFUR_SLAB","SULFUR_WALL",
            "POLISHED_SULFUR","POLISHED_SULFUR_STAIRS","POLISHED_SULFUR_SLAB","POLISHED_SULFUR_WALL",
            "SULFUR_BRICKS","SULFUR_BRICK_STAIRS","SULFUR_BRICK_SLAB","SULFUR_BRICK_WALL",
            "CHISELED_SULFUR","POTENT_SULFUR","SULFUR_SPIKE",
            "CINNABAR","CINNABAR_STAIRS","CINNABAR_SLAB","CINNABAR_WALL",
            "POLISHED_CINNABAR","POLISHED_CINNABAR_STAIRS","POLISHED_CINNABAR_SLAB","POLISHED_CINNABAR_WALL",
            "CINNABAR_BRICKS","CINNABAR_BRICK_STAIRS","CINNABAR_BRICK_SLAB","CINNABAR_BRICK_WALL",
            "CHISELED_CINNABAR","SULFUR_CUBE_BUCKET",
            "MUSIC_DISC_BOUNCE","MUSIC_DISC_TEARS","MUSIC_DISC_CREATOR","MUSIC_DISC_CREATOR_MUSIC_BOX","MUSIC_DISC_PRECIPICE"
        );
    }

    private void openLatestShop(Player player) {
        openCategoryPage(player, "New 1.21-26.2", latestItems(), 0);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (isSearchAnvil(title)) {
            event.setCancelled(true);
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            if (event.getSlot() == 2) {
                String text = getAnvilText(event.getView().getTopInventory());
                UUID id = player.getUniqueId();
                if (text == null || text.trim().isEmpty()) {
                    // Empty rename (or Bedrock where rename may not work) -> type in chat instead
                    searching.remove(id);
                    chatPending.add(id);
                    player.closeInventory();
                    player.sendMessage("§bType your search in chat, or 'cancel'.");
                } else {
                    searching.remove(id);
                    player.closeInventory();
                    showResults(player, text.trim());
                }
            }
            return;
        }

        boolean isMain = isMainShop(title);
        boolean isCategory = isCategoryShop(title);

        if (isMain || isCategory) {
            event.setCancelled(true);
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        if (isMain) {
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
                case 31 -> openLatestShop(player);
                case 40 -> sellManager.openSellGui(player);
                case 49 -> openSearch(player);
            }
            return;
        }

        if (isCategory) {
            // Ignore clicks in the player's own inventory while a category page is open
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                return;
            }

            ShopPage pageData = playerPages.get(player.getUniqueId());
            if (pageData == null) {
                openShop(player);
                return;
            }

            if (event.getSlot() == 4) {
                playerPages.remove(player.getUniqueId());
                openShop(player);
                return;
            }

            if (event.getSlot() == 48) {
                openCategoryPage(player, pageData.category, pageData.materials, pageData.page - 1);
                return;
            }
            if (event.getSlot() == 50) {
                openCategoryPage(player, pageData.category, pageData.materials, pageData.page + 1);
                return;
            }
            if (event.getSlot() == 49) {
                return;
            }

            Material material = clicked.getType();
            double buyPrice = priceManager.getBuyPrice(material);

            for (int slot : ITEM_SLOTS) {
                if (event.getSlot() == slot) {
                    if (balanceManager.removeBalance(player, buyPrice)) {
                        player.getInventory().addItem(new ItemStack(material, 1));
                        player.sendMessage("§aBought 1x " + formatName(material) + " for $" + String.format("%.2f", buyPrice));
                    } else {
                        player.sendMessage("§cNot enough money! Need $" + String.format("%.2f", buyPrice));
                    }
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (searching.remove(player.getUniqueId())) {
            // Closed the anvil without submitting -> back to shop
            Bukkit.getScheduler().runTask(getPlugin(), () -> openShop(player));
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!chatPending.remove(player.getUniqueId())) return;
        event.setCancelled(true);
        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(getPlugin(), () -> openShop(player));
            return;
        }
        Bukkit.getScheduler().runTask(getPlugin(), () -> showResults(player, msg));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (isMainShop(title) || isCategoryShop(title) || isSearchAnvil(title)) {
            event.setCancelled(true);
        }
    }

    private boolean isMainShop(String title) {
        return title.equals("§6§lShop");
    }

    private boolean isCategoryShop(String title) {
        // Category pages are created as "§6§l<Category> §7(Page X/Y)"
        return title.startsWith("§6§l") && title.contains("§7(Page ");
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
        if (material == null) material = Material.STONE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String l : lore) loreList.add(l);
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static class ShopPage {
        final String category;
        final Material[] materials;
        final int page;

        ShopPage(String category, Material[] materials, int page) {
            this.category = category;
            this.materials = materials;
            this.page = page;
        }
    }
}
