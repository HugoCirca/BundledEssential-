package com.bundleessential.economy;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PriceManager {

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final Map<Material, Double> basePrices = new HashMap<>();
    private final Map<Material, Double> multipliers = new HashMap<>();
    private final Map<String, Double> categoryVolatility = new HashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File shopFile;

    private long serverStartTime;
    private double inflationFactor = 1.0;

    public PriceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.shopFile = new File(plugin.getDataFolder(), "shop.json");
        serverStartTime = System.currentTimeMillis();
        initCategories();
        initPrices();
        loadShopPrices();
        startDailyUpdate();
    }

    private void initCategories() {
        categoryVolatility.put("wood", 0.08);
        categoryVolatility.put("stone", 0.06);
        categoryVolatility.put("ore", 0.15);
        categoryVolatility.put("crop", 0.10);
        categoryVolatility.put("mob_drop", 0.12);
        categoryVolatility.put("building", 0.07);
        categoryVolatility.put("decoration", 0.09);
        categoryVolatility.put("food", 0.10);
        categoryVolatility.put("tool", 0.11);
        categoryVolatility.put("armor", 0.13);
        categoryVolatility.put("redstone", 0.09);
        categoryVolatility.put("nether", 0.14);
        categoryVolatility.put("end", 0.16);
        categoryVolatility.put("misc", 0.11);
    }

    private void loadShopPrices() {
        plugin.getDataFolder().mkdirs();
        if (shopFile.exists()) {
            try {
                String json = new String(Files.readAllBytes(shopFile.toPath()));
                JsonObject data = JsonParser.parseString(json).getAsJsonObject();
                for (Map.Entry<String, com.google.gson.JsonElement> entry : data.entrySet()) {
                    try {
                        Material mat = Material.matchMaterial(entry.getKey());
                        if (mat != null) basePrices.put(mat, entry.getValue().getAsDouble());
                    } catch (IllegalArgumentException ignored) {}
                }
                plugin.getLogger().info("Loaded custom prices from shop.json");
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load shop.json", e);
            }
        } else {
            saveShopPrices();
        }
    }

    public void saveShopPrices() {
        plugin.getDataFolder().mkdirs();
        try {
            JsonObject data = new JsonObject();
            for (Map.Entry<Material, Double> entry : basePrices.entrySet()) {
                data.addProperty(entry.getKey().name(), entry.getValue());
            }
            Files.write(shopFile.toPath(), gson.toJson(data).getBytes());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save shop.json", e);
        }
    }

    public void reloadShopPrices() {
        loadShopPrices();
    }

    /** Version-safe put: works on 1.20.4 and newer (1.21 -> 26.2). Unknown names are skipped. */
    private void set(String name, double price) {
        try {
            Material m = Material.matchMaterial(name);
            if (m != null) basePrices.put(m, price);
        } catch (Exception ignored) {}
    }

    private void initPrices() {
        // ===== AFFORDABLE REBALANCE =====
        // Common blocks 0.25-5, mid 5-30, rare 30-300, ultra-rare 300-800.
        // Old insane prices (Elytra 7500, Netherite block 10000, Totem 5000...) slashed ~70-90%.

        // === WOOD / LOGS (1-5) ===
        set("OAK_LOG", 2.50); set("SPRUCE_LOG", 2.50); set("BIRCH_LOG", 2.50);
        set("JUNGLE_LOG", 3.00); set("ACACIA_LOG", 3.00); set("DARK_OAK_LOG", 3.00);
        set("MANGROVE_LOG", 3.50); set("CHERRY_LOG", 3.50); set("PALE_OAK_LOG", 3.50);
        set("CRIMSON_STEM", 3.50); set("WARPED_STEM", 3.50);
        set("STRIPPED_OAK_LOG", 3.00); set("STRIPPED_SPRUCE_LOG", 3.00); set("STRIPPED_BIRCH_LOG", 3.00);
        set("STRIPPED_JUNGLE_LOG", 3.50); set("STRIPPED_ACACIA_LOG", 3.50); set("STRIPPED_DARK_OAK_LOG", 3.50);
        set("STRIPPED_MANGROVE_LOG", 4.00); set("STRIPPED_CHERRY_LOG", 4.00); set("STRIPPED_PALE_OAK_LOG", 4.00);
        set("STRIPPED_CRIMSON_STEM", 4.00); set("STRIPPED_WARPED_STEM", 4.00);
        set("OAK_WOOD", 3.00); set("SPRUCE_WOOD", 3.00); set("BIRCH_WOOD", 3.00);
        set("JUNGLE_WOOD", 3.50); set("ACACIA_WOOD", 3.50); set("DARK_OAK_WOOD", 3.50);
        set("MANGROVE_WOOD", 4.00); set("CHERRY_WOOD", 4.00); set("PALE_OAK_WOOD", 4.00);
        set("CRIMSON_HYPHAE", 4.00); set("WARPED_HYPHAE", 4.00);
        set("STRIPPED_OAK_WOOD", 3.00); set("STRIPPED_SPRUCE_WOOD", 3.00); set("STRIPPED_BIRCH_WOOD", 3.00);
        set("STRIPPED_JUNGLE_WOOD", 3.50); set("STRIPPED_ACACIA_WOOD", 3.50); set("STRIPPED_DARK_OAK_WOOD", 3.50);
        set("STRIPPED_MANGROVE_WOOD", 4.00); set("STRIPPED_CHERRY_WOOD", 4.00); set("STRIPPED_PALE_OAK_WOOD", 4.00);
        set("STRIPPED_CRIMSON_HYPHAE", 4.00); set("STRIPPED_WARPED_HYPHAE", 4.00);
        set("OAK_PLANKS", 0.75); set("SPRUCE_PLANKS", 0.75); set("BIRCH_PLANKS", 0.75);
        set("JUNGLE_PLANKS", 1.00); set("ACACIA_PLANKS", 1.00); set("DARK_OAK_PLANKS", 1.00);
        set("MANGROVE_PLANKS", 1.00); set("CHERRY_PLANKS", 1.00); set("PALE_OAK_PLANKS", 1.00);
        set("CRIMSON_PLANKS", 1.25); set("WARPED_PLANKS", 1.25);
        set("BAMBOO_PLANKS", 0.60); set("BAMBOO_MOSAIC", 1.00);
        set("BAMBOO_BLOCK", 1.50); set("STRIPPED_BAMBOO_BLOCK", 1.50); set("BAMBOO", 0.40);
        set("OAK_SAPLING", 1.00); set("SPRUCE_SAPLING", 1.00); set("BIRCH_SAPLING", 1.00);
        set("JUNGLE_SAPLING", 1.50); set("ACACIA_SAPLING", 1.25); set("DARK_OAK_SAPLING", 1.25);
        set("MANGROVE_PROPAGULE", 1.50); set("CHERRY_SAPLING", 1.50); set("PALE_OAK_SAPLING", 1.50);
        set("OAK_LEAVES", 0.50); set("SPRUCE_LEAVES", 0.50); set("BIRCH_LEAVES", 0.50);
        set("JUNGLE_LEAVES", 0.50); set("ACACIA_LEAVES", 0.50); set("DARK_OAK_LEAVES", 0.50);
        set("MANGROVE_LEAVES", 0.60); set("CHERRY_LEAVES", 0.60); set("PALE_OAK_LEAVES", 0.60);
        set("AZALEA_LEAVES", 0.75); set("FLOWERING_AZALEA_LEAVES", 1.00);
        set("MANGROVE_ROOTS", 1.00); set("MUDDY_MANGROVE_ROOTS", 1.25);
        set("AZALEA", 1.50); set("FLOWERING_AZALEA", 2.00);
        set("STICK", 0.25);

        // === STONE / NATURE (0.25-4) ===
        set("STONE", 0.75); set("COBBLESTONE", 0.40); set("MOSSY_COBBLESTONE", 0.75); set("SMOOTH_STONE", 1.00);
        set("GRANITE", 0.75); set("POLISHED_GRANITE", 1.25); set("DIORITE", 0.75); set("POLISHED_DIORITE", 1.25);
        set("ANDESITE", 0.75); set("POLISHED_ANDESITE", 1.25);
        set("DEEPSLATE", 1.00); set("COBBLED_DEEPSLATE", 0.60); set("POLISHED_DEEPSLATE", 1.25);
        set("DEEPSLATE_BRICKS", 1.50); set("CRACKED_DEEPSLATE_BRICKS", 1.25);
        set("DEEPSLATE_TILES", 1.50); set("CRACKED_DEEPSLATE_TILES", 1.25); set("CHISELED_DEEPSLATE", 1.75);
        set("TUFF", 0.75); set("POLISHED_TUFF", 1.25); set("TUFF_BRICKS", 1.50);
        set("CHISELED_TUFF", 1.75); set("CHISELED_TUFF_BRICKS", 1.75);
        set("TUFF_STAIRS", 1.00); set("TUFF_SLAB", 0.50); set("TUFF_WALL", 0.75);
        set("POLISHED_TUFF_STAIRS", 1.50); set("POLISHED_TUFF_SLAB", 0.75); set("POLISHED_TUFF_WALL", 1.00);
        set("TUFF_BRICK_STAIRS", 1.75); set("TUFF_BRICK_SLAB", 0.85); set("TUFF_BRICK_WALL", 1.00);
        set("CALCITE", 1.50); set("DRIPSTONE_BLOCK", 1.25); set("POINTED_DRIPSTONE", 1.00);
        set("SANDSTONE", 0.75); set("CHISELED_SANDSTONE", 1.25); set("CUT_SANDSTONE", 1.00); set("SMOOTH_SANDSTONE", 1.00);
        set("RED_SANDSTONE", 0.85); set("CHISELED_RED_SANDSTONE", 1.25); set("CUT_RED_SANDSTONE", 1.00); set("SMOOTH_RED_SANDSTONE", 1.00);
        set("SAND", 0.30); set("RED_SAND", 0.35); set("GRAVEL", 0.30); set("CLAY", 0.75);
        set("DIRT", 0.25); set("COARSE_DIRT", 0.30); set("ROOTED_DIRT", 0.40);
        set("PODZOL", 0.60); set("MYCELIUM", 0.75); set("GRASS_BLOCK", 0.75);
        set("MUD", 0.50); set("PACKED_MUD", 0.75);
        set("STONE_BRICKS", 1.25); set("MOSSY_STONE_BRICKS", 1.75);
        set("CRACKED_STONE_BRICKS", 1.00); set("CHISELED_STONE_BRICKS", 1.50);
        set("STONE_STAIRS", 1.00); set("STONE_SLAB", 0.50);
        set("COBBLESTONE_STAIRS", 0.60); set("COBBLESTONE_SLAB", 0.30); set("COBBLESTONE_WALL", 0.40);
        set("MOSSY_COBBLESTONE_STAIRS", 1.00); set("MOSSY_COBBLESTONE_SLAB", 0.50); set("MOSSY_COBBLESTONE_WALL", 0.60);
        set("SANDSTONE_STAIRS", 1.00); set("SANDSTONE_SLAB", 0.50); set("SANDSTONE_WALL", 0.60);
        set("RED_SANDSTONE_STAIRS", 1.00); set("RED_SANDSTONE_SLAB", 0.50); set("RED_SANDSTONE_WALL", 0.60);
        set("GRANITE_STAIRS", 1.00); set("GRANITE_SLAB", 0.50); set("GRANITE_WALL", 0.60);
        set("DIORITE_STAIRS", 1.00); set("DIORITE_SLAB", 0.50); set("DIORITE_WALL", 0.60);
        set("ANDESITE_STAIRS", 1.00); set("ANDESITE_SLAB", 0.50); set("ANDESITE_WALL", 0.60);
        set("DEEPSLATE_BRICK_STAIRS", 1.75); set("DEEPSLATE_BRICK_SLAB", 0.85); set("DEEPSLATE_BRICK_WALL", 1.00);
        set("DEEPSLATE_TILE_STAIRS", 1.75); set("DEEPSLATE_TILE_SLAB", 0.85); set("DEEPSLATE_TILE_WALL", 1.00);
        set("POLISHED_DEEPSLATE_STAIRS", 1.50); set("POLISHED_DEEPSLATE_SLAB", 0.75); set("POLISHED_DEEPSLATE_WALL", 1.00);
        set("COBBLED_DEEPSLATE_STAIRS", 0.85); set("COBBLED_DEEPSLATE_SLAB", 0.40); set("COBBLED_DEEPSLATE_WALL", 0.50);
        set("MOSS_BLOCK", 1.50); set("MOSS_CARPET", 0.75);
        set("PALE_MOSS_BLOCK", 1.50); set("PALE_MOSS_CARPET", 0.75); set("PALE_HANGING_MOSS", 1.00);
        set("INFESTED_STONE", 0.75); set("INFESTED_COBBLESTONE", 0.60);
        set("OBSIDIAN", 50.00); set("CRYING_OBSIDIAN", 60.00);

        // === ORES / MINERALS (affordable) ===
        set("COAL_ORE", 4.00); set("DEEPSLATE_COAL_ORE", 4.50); set("COAL", 1.25); set("CHARCOAL", 1.00); set("COAL_BLOCK", 11.00);
        set("IRON_ORE", 7.00); set("DEEPSLATE_IRON_ORE", 8.00); set("RAW_IRON", 3.50);
        set("IRON_INGOT", 4.50); set("IRON_NUGGET", 0.50); set("IRON_BLOCK", 150.00); set("RAW_IRON_BLOCK", 120.00);
        set("COPPER_ORE", 5.00); set("DEEPSLATE_COPPER_ORE", 5.50); set("RAW_COPPER", 2.50);
        set("COPPER_INGOT", 3.00); set("COPPER_BLOCK", 27.00); set("RAW_COPPER_BLOCK", 22.00);
        set("CUT_COPPER", 3.25); set("CUT_COPPER_STAIRS", 3.50); set("CUT_COPPER_SLAB", 1.75);
        set("CHISELED_COPPER", 3.50); set("EXPOSED_CHISELED_COPPER", 3.50); set("WEATHERED_CHISELED_COPPER", 3.50); set("OXIDIZED_CHISELED_COPPER", 3.50);
        set("EXPOSED_COPPER", 3.00); set("WEATHERED_COPPER", 3.00); set("OXIDIZED_COPPER", 3.00); set("WAXED_COPPER_BLOCK", 3.50);
        set("COPPER_GRATE", 4.00); set("EXPOSED_COPPER_GRATE", 4.00); set("WEATHERED_COPPER_GRATE", 4.00); set("OXIDIZED_COPPER_GRATE", 4.00); set("WAXED_COPPER_GRATE", 4.50);
        set("COPPER_BULB", 6.00); set("EXPOSED_COPPER_BULB", 6.00); set("WEATHERED_COPPER_BULB", 6.00); set("OXIDIZED_COPPER_BULB", 6.00);
        set("WAXED_COPPER_BULB", 6.50); set("WAXED_EXPOSED_COPPER_BULB", 6.50); set("WAXED_WEATHERED_COPPER_BULB", 6.50); set("WAXED_OXIDIZED_COPPER_BULB", 6.50);
        set("GOLD_ORE", 50.00); set("DEEPSLATE_GOLD_ORE", 55.00); set("NETHER_GOLD_ORE", 55.00);
        set("RAW_GOLD", 6.00); set("GOLD_INGOT", 8.00); set("GOLD_NUGGET", 0.90);
        set("GOLD_BLOCK", 300.00); set("RAW_GOLD_BLOCK", 250.00);
        set("REDSTONE_ORE", 35.00); set("DEEPSLATE_REDSTONE_ORE", 38.00); set("REDSTONE", 0.75); set("REDSTONE_BLOCK", 6.75);
        set("LAPIS_ORE", 60.00); set("DEEPSLATE_LAPIS_ORE", 65.00); set("LAPIS_LAZULI", 10.00); set("LAPIS_BLOCK", 100.00);
        set("DIAMOND_ORE", 200.00); set("DEEPSLATE_DIAMOND_ORE", 220.00); set("DIAMOND", 250.00); set("DIAMOND_BLOCK", 2500.00);
        set("EMERALD_ORE", 250.00); set("DEEPSLATE_EMERALD_ORE", 275.00); set("EMERALD", 150.00); set("EMERALD_BLOCK", 1500.00);
        set("NETHER_QUARTZ_ORE", 7.00); set("QUARTZ", 2.00); set("QUARTZ_BLOCK", 4.00);
        set("QUARTZ_BRICKS", 4.50); set("QUARTZ_PILLAR", 4.25); set("CHISELED_QUARTZ_BLOCK", 4.50);
        set("SMOOTH_QUARTZ", 4.25); set("SMOOTH_QUARTZ_STAIRS", 4.50); set("SMOOTH_QUARTZ_SLAB", 2.25);
        set("AMETHYST_BLOCK", 6.00); set("AMETHYST_SHARD", 3.50);
        set("ANCIENT_DEBRIS", 500.00); set("NETHERITE_SCRAP", 300.00); set("NETHERITE_INGOT", 1000.00); set("NETHERITE_BLOCK", 10000.00);
        set("GLOWSTONE", 4.00); set("GLOWSTONE_DUST", 1.00);
        set("RESIN_CLUMP", 2.00); set("RESIN_BLOCK", 4.00); set("RESIN_BRICKS", 4.50);
        set("RESIN_BRICK_STAIRS", 4.75); set("RESIN_BRICK_SLAB", 2.25); set("RESIN_BRICK_WALL", 2.50);
        set("CHISELED_RESIN_BRICKS", 5.00);
        set("HEAVY_CORE", 300.00);

        // === CROPS / NATURE (0.5-6, rares higher) ===
        set("WHEAT", 0.75); set("WHEAT_SEEDS", 0.25); set("HAY_BLOCK", 6.00);
        set("CARROT", 0.85); set("POTATO", 0.75); set("POISONOUS_POTATO", 0.25); set("BAKED_POTATO", 1.25);
        set("BEETROOT", 0.75); set("BEETROOT_SEEDS", 0.25); set("BEETROOT_SOUP", 1.75);
        set("MELON", 1.75); set("MELON_SLICE", 0.50); set("MELON_SEEDS", 0.25); set("GLISTERING_MELON_SLICE", 4.00);
        set("PUMPKIN", 1.75); set("CARVED_PUMPKIN", 1.75); set("JACK_O_LANTERN", 2.50); set("PUMPKIN_SEEDS", 0.25); set("PUMPKIN_PIE", 2.00);
        set("SUGAR_CANE", 0.85); set("PAPER", 0.40); set("COCOA_BEANS", 1.25); set("COOKIE", 1.00);
        set("NETHER_WART", 2.50); set("CHORUS_FRUIT", 3.00); set("CHORUS_FLOWER", 5.00); set("POPPED_CHORUS_FRUIT", 1.50);
        set("SWEET_BERRIES", 0.75); set("GLOW_BERRIES", 1.50);
        set("APPLE", 1.50); set("GOLDEN_APPLE", 250.00); set("ENCHANTED_GOLDEN_APPLE", 2500.00); set("GOLDEN_CARROT", 50.00);
        set("BROWN_MUSHROOM", 0.60); set("RED_MUSHROOM", 0.60);
        set("BROWN_MUSHROOM_BLOCK", 1.00); set("RED_MUSHROOM_BLOCK", 1.00); set("MUSHROOM_STEM", 0.50); set("MUSHROOM_STEW", 2.00);
        set("SUSPICIOUS_STEW", 3.00); set("RABBIT_STEW", 2.50);
        set("CACTUS", 0.60); set("CACTUS_FLOWER", 1.50);
        set("KELP", 0.40); set("DRIED_KELP", 0.50); set("DRIED_KELP_BLOCK", 4.00);
        set("SEAGRASS", 0.40); set("TALL_SEAGRASS", 0.50); set("LILY_PAD", 0.75);
        set("VINE", 0.75); set("GLOW_LICHEN", 1.25);
        set("HANGING_ROOTS", 0.60); set("BIG_DRIPLEAF", 1.00); set("SMALL_DRIPLEAF", 0.75); set("SPORE_BLOSSOM", 2.50);
        set("TORCHFLOWER", 2.50); set("TORCHFLOWER_SEEDS", 1.50); set("PITCHER_PLANT", 2.50); set("PITCHER_POD", 1.50);
        set("DANDELION", 0.50); set("GOLDEN_DANDELION", 5.00); set("POPPY", 0.50); set("BLUE_ORCHID", 0.60);
        set("ALLIUM", 0.60); set("AZURE_BLUET", 0.50); set("RED_TULIP", 0.60); set("ORANGE_TULIP", 0.60);
        set("WHITE_TULIP", 0.60); set("PINK_TULIP", 0.60); set("OXEYE_DAISY", 0.50); set("CORNFLOWER", 0.60);
        set("LILY_OF_THE_VALLEY", 0.60); set("WITHER_ROSE", 3.00);
        set("SUNFLOWER", 0.75); set("LILAC", 0.75); set("ROSE_BUSH", 0.75); set("PEONY", 0.75);
        set("EYEBLOSSOM", 2.50);
        set("FIREFLY_BUSH", 2.00); set("BUSH", 0.75);
        set("SHORT_GRASS", 0.30); set("TALL_GRASS", 0.40); set("FERN", 0.40); set("LARGE_FERN", 0.50);
        set("LEAF_LITTER", 0.30); set("WILDFLOWERS", 0.60); set("SHORT_DRY_GRASS", 0.30); set("TALL_DRY_GRASS", 0.40);
        set("PINK_PETALS", 0.75);

        // === MOB DROPS (commons cheap, rares affordable) ===
        set("ROTTEN_FLESH", 0.25); set("BONE", 0.85); set("BONE_MEAL", 0.40); set("BONE_BLOCK", 2.50);
        set("ARROW", 0.40); set("SPECTRAL_ARROW", 0.75); set("STRING", 1.00);
        set("SPIDER_EYE", 1.25); set("FERMENTED_SPIDER_EYE", 2.50); set("GUNPOWDER", 1.75);
        set("ENDER_PEARL", 8.00); set("ENDER_EYE", 10.00);
        set("BLAZE_ROD", 10.00); set("BLAZE_POWDER", 5.00);
        set("BREEZE_ROD", 25.00); set("WIND_CHARGE", 3.00);
        set("MAGMA_CREAM", 3.50); set("GHAST_TEAR", 15.00);
        set("SLIME_BALL", 2.50); set("SLIME_BLOCK", 5.00);
        set("PRISMARINE_SHARD", 2.00); set("PRISMARINE_CRYSTALS", 2.50);
        set("NAUTILUS_SHELL", 12.00); set("HEART_OF_THE_SEA", 1000.00);
        set("PHANTOM_MEMBRANE", 6.00); set("SHULKER_SHELL", 80.00);
        set("DRAGON_BREATH", 100.00); set("ECHO_SHARD", 15.00); set("DISC_FRAGMENT_5", 12.00);
        set("ARMADILLO_SCUTE", 5.00); set("SCUTE", 4.50); set("TURTLE_EGG", 5.00); set("SNIFFER_EGG", 60.00);
        set("INK_SAC", 0.75); set("GLOW_INK_SAC", 1.50);
        set("HONEYCOMB", 2.50); set("HONEY_BOTTLE", 1.75); set("HONEY_BLOCK", 7.00); set("HONEYCOMB_BLOCK", 8.00);
        set("FEATHER", 0.50); set("LEATHER", 1.50); set("RABBIT_HIDE", 1.00); set("RABBIT_FOOT", 2.50);
        set("EGG", 0.40); set("FLINT", 0.60);
        set("ZOMBIE_HEAD", 15.00); set("SKELETON_SKULL", 12.00); set("WITHER_SKELETON_SKULL", 150.00);
        set("CREEPER_HEAD", 18.00); set("DRAGON_HEAD", 500.00); set("PIGLIN_HEAD", 20.00); set("PLAYER_HEAD", 25.00);
        set("TOTEM_OF_UNDYING", 5000.00); set("NETHER_STAR", 3000.00); set("TRIDENT", 500.00); set("ELYTRA", 7500.00);
        set("SADDLE", 12.00); set("NAME_TAG", 8.00); set("LEAD", 3.00);
        set("SULFUR_CUBE_BUCKET", 40.00); set("DRIED_GHAST", 25.00);

        // === FOOD (2-6 mostly) ===
        set("BEEF", 1.50); set("COOKED_BEEF", 3.00); set("PORKCHOP", 1.50); set("COOKED_PORKCHOP", 3.00);
        set("MUTTON", 1.25); set("COOKED_MUTTON", 2.50); set("CHICKEN", 1.25); set("COOKED_CHICKEN", 2.50);
        set("RABBIT", 1.25); set("COOKED_RABBIT", 2.00);
        set("COD", 1.00); set("COOKED_COD", 2.00); set("SALMON", 1.25); set("COOKED_SALMON", 2.50);
        set("TROPICAL_FISH", 1.50); set("PUFFERFISH", 1.25);
        set("BREAD", 1.75); set("CAKE", 8.00);
        set("MILK_BUCKET", 2.50);

        // === TOOLS / WEAPONS (affordable tiers) ===
        set("WOODEN_SWORD", 1.50); set("WOODEN_PICKAXE", 1.50); set("WOODEN_AXE", 1.50); set("WOODEN_SHOVEL", 0.85); set("WOODEN_HOE", 1.00);
        set("STONE_SWORD", 4.00); set("STONE_PICKAXE", 4.00); set("STONE_AXE", 4.00); set("STONE_SHOVEL", 2.00); set("STONE_HOE", 2.50);
        set("IRON_SWORD", 10.00); set("IRON_PICKAXE", 10.00); set("IRON_AXE", 10.00); set("IRON_SHOVEL", 5.00); set("IRON_HOE", 6.00);
        set("GOLDEN_SWORD", 8.00); set("GOLDEN_PICKAXE", 8.00); set("GOLDEN_AXE", 8.00); set("GOLDEN_SHOVEL", 4.00); set("GOLDEN_HOE", 5.00);
        set("DIAMOND_SWORD", 200.00); set("DIAMOND_PICKAXE", 200.00); set("DIAMOND_AXE", 200.00); set("DIAMOND_SHOVEL", 100.00); set("DIAMOND_HOE", 120.00);
        set("NETHERITE_SWORD", 800.00); set("NETHERITE_PICKAXE", 800.00); set("NETHERITE_AXE", 800.00); set("NETHERITE_SHOVEL", 400.00); set("NETHERITE_HOE", 500.00);
        set("NETHERITE_UPGRADE_SMITHING_TEMPLATE", 80.00); set("MACE", 500.00);
        set("BOW", 6.00); set("CROSSBOW", 10.00); set("SHIELD", 8.00);
        set("FISHING_ROD", 4.00); set("CARROT_ON_A_STICK", 3.00); set("WARPED_FUNGUS_ON_A_STICK", 3.00);
        set("SHEARS", 5.00); set("FLINT_AND_STEEL", 4.00); set("BRUSH", 5.00); set("BUNDLE", 6.00);
        set("WHITE_BUNDLE", 6.00); set("ORANGE_BUNDLE", 6.00); set("MAGENTA_BUNDLE", 6.00); set("LIGHT_BLUE_BUNDLE", 6.00);
        set("YELLOW_BUNDLE", 6.00); set("LIME_BUNDLE", 6.00); set("PINK_BUNDLE", 6.00); set("GRAY_BUNDLE", 6.00);
        set("LIGHT_GRAY_BUNDLE", 6.00); set("CYAN_BUNDLE", 6.00); set("PURPLE_BUNDLE", 6.00); set("BLUE_BUNDLE", 6.00);
        set("BROWN_BUNDLE", 6.00); set("GREEN_BUNDLE", 6.00); set("RED_BUNDLE", 6.00); set("BLACK_BUNDLE", 6.00);
        set("BUCKET", 3.00); set("WATER_BUCKET", 3.50); set("LAVA_BUCKET", 6.00); set("POWDER_SNOW_BUCKET", 4.00);
        set("COD_BUCKET", 5.00); set("SALMON_BUCKET", 5.00); set("TROPICAL_FISH_BUCKET", 6.00); set("PUFFERFISH_BUCKET", 5.00);
        set("AXOLOTL_BUCKET", 15.00); set("TADPOLE_BUCKET", 5.00);
        set("COMPASS", 5.00); set("RECOVERY_COMPASS", 12.00); set("CLOCK", 5.00); set("MAP", 4.00);
        set("TRIAL_KEY", 20.00); set("OMINOUS_TRIAL_KEY", 40.00);
        set("MINECART", 8.00); set("CHEST_MINECART", 10.00); set("HOPPER_MINECART", 12.00); set("TNT_MINECART", 14.00);
        set("OAK_BOAT", 3.00); set("SPRUCE_BOAT", 3.00); set("BIRCH_BOAT", 3.00); set("JUNGLE_BOAT", 3.50);
        set("ACACIA_BOAT", 3.50); set("DARK_OAK_BOAT", 3.50); set("MANGROVE_BOAT", 4.00); set("CHERRY_BOAT", 4.00);
        set("PALE_OAK_BOAT", 4.00); set("BAMBOO_RAFT", 2.50);
        set("OAK_CHEST_BOAT", 5.00); set("SPRUCE_CHEST_BOAT", 5.00); set("BIRCH_CHEST_BOAT", 5.00); set("JUNGLE_CHEST_BOAT", 5.50);
        set("ACACIA_CHEST_BOAT", 5.50); set("DARK_OAK_CHEST_BOAT", 5.50); set("MANGROVE_CHEST_BOAT", 6.00); set("CHERRY_CHEST_BOAT", 6.00);
        set("PALE_OAK_CHEST_BOAT", 6.00); set("BAMBOO_CHEST_RAFT", 4.50);

        // === ARMOR (affordable) ===
        set("LEATHER_HELMET", 5.00); set("LEATHER_CHESTPLATE", 8.00); set("LEATHER_LEGGINGS", 6.00); set("LEATHER_BOOTS", 4.00);
        set("CHAINMAIL_HELMET", 10.00); set("CHAINMAIL_CHESTPLATE", 15.00); set("CHAINMAIL_LEGGINGS", 12.00); set("CHAINMAIL_BOOTS", 8.00);
        set("IRON_HELMET", 12.00); set("IRON_CHESTPLATE", 20.00); set("IRON_LEGGINGS", 17.00); set("IRON_BOOTS", 10.00);
        set("GOLDEN_HELMET", 10.00); set("GOLDEN_CHESTPLATE", 15.00); set("GOLDEN_LEGGINGS", 12.00); set("GOLDEN_BOOTS", 8.00);
        set("DIAMOND_HELMET", 200.00); set("DIAMOND_CHESTPLATE", 350.00); set("DIAMOND_LEGGINGS", 300.00); set("DIAMOND_BOOTS", 150.00);
        set("NETHERITE_HELMET", 800.00); set("NETHERITE_CHESTPLATE", 1200.00); set("NETHERITE_LEGGINGS", 1000.00); set("NETHERITE_BOOTS", 600.00);
        set("TURTLE_HELMET", 100.00);
        set("LEATHER_HORSE_ARMOR", 8.00); set("IRON_HORSE_ARMOR", 15.00); set("GOLDEN_HORSE_ARMOR", 18.00); set("DIAMOND_HORSE_ARMOR", 60.00);
        set("WOLF_ARMOR", 12.00);
        set("WHITE_HARNESS", 20.00); set("ORANGE_HARNESS", 20.00); set("MAGENTA_HARNESS", 20.00); set("LIGHT_BLUE_HARNESS", 20.00);
        set("YELLOW_HARNESS", 20.00); set("LIME_HARNESS", 20.00); set("PINK_HARNESS", 20.00); set("GRAY_HARNESS", 20.00);
        set("LIGHT_GRAY_HARNESS", 20.00); set("CYAN_HARNESS", 20.00); set("PURPLE_HARNESS", 20.00); set("BLUE_HARNESS", 20.00);
        set("BROWN_HARNESS", 20.00); set("GREEN_HARNESS", 20.00); set("RED_HARNESS", 20.00); set("BLACK_HARNESS", 20.00);
        set("SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE", 25.00); set("DUNE_ARMOR_TRIM_SMITHING_TEMPLATE", 25.00);
        set("COAST_ARMOR_TRIM_SMITHING_TEMPLATE", 25.00); set("WILD_ARMOR_TRIM_SMITHING_TEMPLATE", 25.00);
        set("WARD_ARMOR_TRIM_SMITHING_TEMPLATE", 30.00); set("TIDE_ARMOR_TRIM_SMITHING_TEMPLATE", 30.00);
        set("VEX_ARMOR_TRIM_SMITHING_TEMPLATE", 30.00); set("SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE", 25.00);
        set("RIB_ARMOR_TRIM_SMITHING_TEMPLATE", 25.00); set("EYE_ARMOR_TRIM_SMITHING_TEMPLATE", 30.00);
        set("SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE", 30.00); set("FLOW_ARMOR_TRIM_SMITHING_TEMPLATE", 30.00);
        set("BOLT_ARMOR_TRIM_SMITHING_TEMPLATE", 30.00);

        // === BUILDING colors (0.5-2) ===
        set("WHITE_WOOL", 1.00); set("ORANGE_WOOL", 1.00); set("MAGENTA_WOOL", 1.00); set("LIGHT_BLUE_WOOL", 1.00);
        set("YELLOW_WOOL", 1.00); set("LIME_WOOL", 1.00); set("PINK_WOOL", 1.00); set("GRAY_WOOL", 1.00);
        set("LIGHT_GRAY_WOOL", 1.00); set("CYAN_WOOL", 1.00); set("PURPLE_WOOL", 1.00); set("BLUE_WOOL", 1.00);
        set("BROWN_WOOL", 1.00); set("GREEN_WOOL", 1.00); set("RED_WOOL", 1.00); set("BLACK_WOOL", 1.00);
        set("WHITE_CONCRETE", 1.00); set("ORANGE_CONCRETE", 1.00); set("MAGENTA_CONCRETE", 1.00); set("LIGHT_BLUE_CONCRETE", 1.00);
        set("YELLOW_CONCRETE", 1.00); set("LIME_CONCRETE", 1.00); set("PINK_CONCRETE", 1.00); set("GRAY_CONCRETE", 1.00);
        set("LIGHT_GRAY_CONCRETE", 1.00); set("CYAN_CONCRETE", 1.00); set("PURPLE_CONCRETE", 1.00); set("BLUE_CONCRETE", 1.00);
        set("BROWN_CONCRETE", 1.00); set("GREEN_CONCRETE", 1.00); set("RED_CONCRETE", 1.00); set("BLACK_CONCRETE", 1.00);
        set("WHITE_CONCRETE_POWDER", 0.75); set("ORANGE_CONCRETE_POWDER", 0.75); set("MAGENTA_CONCRETE_POWDER", 0.75);
        set("LIGHT_BLUE_CONCRETE_POWDER", 0.75); set("YELLOW_CONCRETE_POWDER", 0.75); set("LIME_CONCRETE_POWDER", 0.75);
        set("PINK_CONCRETE_POWDER", 0.75); set("GRAY_CONCRETE_POWDER", 0.75); set("LIGHT_GRAY_CONCRETE_POWDER", 0.75);
        set("CYAN_CONCRETE_POWDER", 0.75); set("PURPLE_CONCRETE_POWDER", 0.75); set("BLUE_CONCRETE_POWDER", 0.75);
        set("BROWN_CONCRETE_POWDER", 0.75); set("GREEN_CONCRETE_POWDER", 0.75); set("RED_CONCRETE_POWDER", 0.75); set("BLACK_CONCRETE_POWDER", 0.75);
        set("TERRACOTTA", 1.00);
        set("WHITE_TERRACOTTA", 1.00); set("ORANGE_TERRACOTTA", 1.00); set("MAGENTA_TERRACOTTA", 1.00); set("LIGHT_BLUE_TERRACOTTA", 1.00);
        set("YELLOW_TERRACOTTA", 1.00); set("LIME_TERRACOTTA", 1.00); set("PINK_TERRACOTTA", 1.00); set("GRAY_TERRACOTTA", 1.00);
        set("LIGHT_GRAY_TERRACOTTA", 1.00); set("CYAN_TERRACOTTA", 1.00); set("PURPLE_TERRACOTTA", 1.00); set("BLUE_TERRACOTTA", 1.00);
        set("BROWN_TERRACOTTA", 1.00); set("GREEN_TERRACOTTA", 1.00); set("RED_TERRACOTTA", 1.00); set("BLACK_TERRACOTTA", 1.00);
        set("GLASS", 0.85); set("TINTED_GLASS", 1.50); set("GLASS_PANE", 0.40);
        set("WHITE_STAINED_GLASS", 1.00); set("ORANGE_STAINED_GLASS", 1.00); set("MAGENTA_STAINED_GLASS", 1.00); set("LIGHT_BLUE_STAINED_GLASS", 1.00);
        set("YELLOW_STAINED_GLASS", 1.00); set("LIME_STAINED_GLASS", 1.00); set("PINK_STAINED_GLASS", 1.00); set("GRAY_STAINED_GLASS", 1.00);
        set("LIGHT_GRAY_STAINED_GLASS", 1.00); set("CYAN_STAINED_GLASS", 1.00); set("PURPLE_STAINED_GLASS", 1.00); set("BLUE_STAINED_GLASS", 1.00);
        set("BROWN_STAINED_GLASS", 1.00); set("GREEN_STAINED_GLASS", 1.00); set("RED_STAINED_GLASS", 1.00); set("BLACK_STAINED_GLASS", 1.00);
        set("WHITE_STAINED_GLASS_PANE", 0.50); set("ORANGE_STAINED_GLASS_PANE", 0.50); set("MAGENTA_STAINED_GLASS_PANE", 0.50);
        set("LIGHT_BLUE_STAINED_GLASS_PANE", 0.50); set("YELLOW_STAINED_GLASS_PANE", 0.50); set("LIME_STAINED_GLASS_PANE", 0.50);
        set("PINK_STAINED_GLASS_PANE", 0.50); set("GRAY_STAINED_GLASS_PANE", 0.50); set("LIGHT_GRAY_STAINED_GLASS_PANE", 0.50);
        set("CYAN_STAINED_GLASS_PANE", 0.50); set("PURPLE_STAINED_GLASS_PANE", 0.50); set("BLUE_STAINED_GLASS_PANE", 0.50);
        set("BROWN_STAINED_GLASS_PANE", 0.50); set("GREEN_STAINED_GLASS_PANE", 0.50); set("RED_STAINED_GLASS_PANE", 0.50); set("BLACK_STAINED_GLASS_PANE", 0.50);
        set("OAK_STAIRS", 1.00); set("OAK_SLAB", 0.50); set("OAK_FENCE", 0.85); set("OAK_FENCE_GATE", 1.00); set("OAK_DOOR", 1.50); set("OAK_TRAPDOOR", 1.25);
        set("SPRUCE_STAIRS", 1.00); set("SPRUCE_SLAB", 0.50); set("SPRUCE_FENCE", 0.85); set("SPRUCE_FENCE_GATE", 1.00); set("SPRUCE_DOOR", 1.50); set("SPRUCE_TRAPDOOR", 1.25);
        set("BIRCH_STAIRS", 1.00); set("BIRCH_SLAB", 0.50); set("BIRCH_FENCE", 0.85); set("BIRCH_FENCE_GATE", 1.00); set("BIRCH_DOOR", 1.50); set("BIRCH_TRAPDOOR", 1.25);
        set("BRICKS", 1.50); set("BRICK_STAIRS", 1.75); set("BRICK_SLAB", 0.85); set("BRICK_WALL", 1.00);
        set("MUD_BRICKS", 1.50); set("MUD_BRICK_STAIRS", 1.75); set("MUD_BRICK_SLAB", 0.85); set("MUD_BRICK_WALL", 1.00);
        set("PRISMARINE", 6.00); set("PRISMARINE_BRICKS", 7.00); set("DARK_PRISMARINE", 8.00); set("SEA_LANTERN", 6.00);

        // === DECORATION (cheap utilities, mid furniture) ===
        set("CRAFTING_TABLE", 2.00); set("FURNACE", 4.00); set("BLAST_FURNACE", 8.00); set("SMOKER", 6.00);
        set("BREWING_STAND", 10.00); set("CAULDRON", 6.00); set("COMPOSTER", 3.00);
        set("BARREL", 4.00); set("CHEST", 4.00); set("TRAPPED_CHEST", 5.00); set("ENDER_CHEST", 60.00);
        set("SHULKER_BOX", 100.00);
        set("WHITE_SHULKER_BOX", 42.00); set("ORANGE_SHULKER_BOX", 42.00); set("MAGENTA_SHULKER_BOX", 42.00);
        set("LIGHT_BLUE_SHULKER_BOX", 42.00); set("YELLOW_SHULKER_BOX", 42.00); set("LIME_SHULKER_BOX", 42.00);
        set("PINK_SHULKER_BOX", 42.00); set("GRAY_SHULKER_BOX", 42.00); set("LIGHT_GRAY_SHULKER_BOX", 42.00);
        set("CYAN_SHULKER_BOX", 42.00); set("PURPLE_SHULKER_BOX", 42.00); set("BLUE_SHULKER_BOX", 42.00);
        set("BROWN_SHULKER_BOX", 42.00); set("GREEN_SHULKER_BOX", 42.00); set("RED_SHULKER_BOX", 42.00); set("BLACK_SHULKER_BOX", 42.00);
        set("ENCHANTING_TABLE", 150.00); set("ANVIL", 50.00); set("CHIPPED_ANVIL", 40.00); set("DAMAGED_ANVIL", 25.00);
        set("GRINDSTONE", 5.00); set("STONECUTTER", 5.00); set("SMITHING_TABLE", 6.00);
        set("CARTOGRAPHY_TABLE", 5.00); set("FLETCHING_TABLE", 4.00); set("LOOM", 4.00); set("LECTERN", 5.00);
        set("BOOKSHELF", 5.00); set("CHISELED_BOOKSHELF", 6.00);
        set("TORCH", 0.40); set("SOUL_TORCH", 0.60); set("LANTERN", 2.00); set("SOUL_LANTERN", 2.50);
        set("END_ROD", 4.00); set("REDSTONE_LAMP", 4.00);
        set("CAMPFIRE", 3.00); set("SOUL_CAMPFIRE", 3.50); set("BELL", 15.00);
        set("BEACON", 300.00); set("CONDUIT", 150.00); set("LODESTONE", 20.00); set("RESPAWN_ANCHOR", 25.00);
        set("JUKEBOX", 12.00); set("NOTE_BLOCK", 6.00);
        set("MUSIC_DISC_13", 15.00); set("MUSIC_DISC_CAT", 15.00); set("MUSIC_DISC_BLOCKS", 15.00); set("MUSIC_DISC_CHIRP", 15.00);
        set("MUSIC_DISC_FAR", 15.00); set("MUSIC_DISC_MALL", 15.00); set("MUSIC_DISC_MELLOHI", 15.00); set("MUSIC_DISC_STAL", 15.00);
        set("MUSIC_DISC_STRAD", 15.00); set("MUSIC_DISC_WARD", 15.00); set("MUSIC_DISC_11", 20.00); set("MUSIC_DISC_WAIT", 15.00);
        set("MUSIC_DISC_OTHERSIDE", 25.00); set("MUSIC_DISC_RELIC", 25.00); set("MUSIC_DISC_5", 25.00);
        set("MUSIC_DISC_PIGSTEP", 30.00); set("MUSIC_DISC_CREATOR", 30.00); set("MUSIC_DISC_CREATOR_MUSIC_BOX", 30.00);
        set("MUSIC_DISC_PRECIPICE", 30.00); set("MUSIC_DISC_BOUNCE", 40.00); set("MUSIC_DISC_TEARS", 35.00);
        set("PAINTING", 4.00); set("ITEM_FRAME", 5.00); set("GLOW_ITEM_FRAME", 8.00); set("ARMOR_STAND", 5.00);
        set("FLOWER_POT", 1.25); set("DECORATED_POT", 4.00);
        set("CANDLE", 1.00); set("WHITE_CANDLE", 1.00); set("ORANGE_CANDLE", 1.00); set("MAGENTA_CANDLE", 1.00);
        set("LIGHT_BLUE_CANDLE", 1.00); set("YELLOW_CANDLE", 1.00); set("LIME_CANDLE", 1.00); set("PINK_CANDLE", 1.00);
        set("GRAY_CANDLE", 1.00); set("LIGHT_GRAY_CANDLE", 1.00); set("CYAN_CANDLE", 1.00); set("PURPLE_CANDLE", 1.00);
        set("BLUE_CANDLE", 1.00); set("BROWN_CANDLE", 1.00); set("GREEN_CANDLE", 1.00); set("RED_CANDLE", 1.00); set("BLACK_CANDLE", 1.00);
        set("WHITE_BED", 6.00); set("ORANGE_BED", 6.00); set("MAGENTA_BED", 6.00); set("LIGHT_BLUE_BED", 6.00);
        set("YELLOW_BED", 6.00); set("LIME_BED", 6.00); set("PINK_BED", 6.00); set("GRAY_BED", 6.00);
        set("LIGHT_GRAY_BED", 6.00); set("CYAN_BED", 6.00); set("PURPLE_BED", 6.00); set("BLUE_BED", 6.00);
        set("BROWN_BED", 6.00); set("GREEN_BED", 6.00); set("RED_BED", 6.00); set("BLACK_BED", 6.00);
        set("WHITE_BANNER", 3.00); set("ORANGE_BANNER", 3.00); set("MAGENTA_BANNER", 3.00); set("LIGHT_BLUE_BANNER", 3.00);
        set("YELLOW_BANNER", 3.00); set("LIME_BANNER", 3.00); set("PINK_BANNER", 3.00); set("GRAY_BANNER", 3.00);
        set("LIGHT_GRAY_BANNER", 3.00); set("CYAN_BANNER", 3.00); set("PURPLE_BANNER", 3.00); set("BLUE_BANNER", 3.00);
        set("BROWN_BANNER", 3.00); set("GREEN_BANNER", 3.00); set("RED_BANNER", 3.00); set("BLACK_BANNER", 3.00);
        set("OAK_SIGN", 1.00); set("SPRUCE_SIGN", 1.00); set("BIRCH_SIGN", 1.00); set("JUNGLE_SIGN", 1.25);
        set("ACACIA_SIGN", 1.25); set("DARK_OAK_SIGN", 1.25); set("MANGROVE_SIGN", 1.25); set("CHERRY_SIGN", 1.25);
        set("PALE_OAK_SIGN", 1.25); set("BAMBOO_SIGN", 1.00); set("CRIMSON_SIGN", 1.50); set("WARPED_SIGN", 1.50);
        set("OAK_HANGING_SIGN", 2.00); set("SPRUCE_HANGING_SIGN", 2.00); set("BIRCH_HANGING_SIGN", 2.00);
        set("CHERRY_HANGING_SIGN", 2.50); set("PALE_OAK_HANGING_SIGN", 2.50); set("BAMBOO_HANGING_SIGN", 2.00);
        set("IRON_BARS", 3.00); set("CHAIN", 3.50); set("LADDER", 0.85); set("SCAFFOLDING", 0.75);
        set("OAK_SHELF", 3.00); set("SPRUCE_SHELF", 3.00); set("BIRCH_SHELF", 3.00); set("JUNGLE_SHELF", 3.50);
        set("ACACIA_SHELF", 3.50); set("DARK_OAK_SHELF", 3.50); set("MANGROVE_SHELF", 3.50); set("CHERRY_SHELF", 3.50);
        set("PALE_OAK_SHELF", 3.50); set("BAMBOO_SHELF", 3.00); set("CRIMSON_SHELF", 4.00); set("WARPED_SHELF", 4.00);
        set("COPPER_CHEST", 8.00); set("EXPOSED_COPPER_CHEST", 8.00); set("WEATHERED_COPPER_CHEST", 8.00); set("OXIDIZED_COPPER_CHEST", 8.00);
        set("WAXED_COPPER_CHEST", 8.50); set("WAXED_EXPOSED_COPPER_CHEST", 8.50); set("WAXED_WEATHERED_COPPER_CHEST", 8.50); set("WAXED_OXIDIZED_COPPER_CHEST", 8.50);
        set("COPPER_GOLEM_STATUE", 10.00); set("EXPOSED_COPPER_GOLEM_STATUE", 10.00); set("WEATHERED_COPPER_GOLEM_STATUE", 10.00); set("OXIDIZED_COPPER_GOLEM_STATUE", 10.00);

        // === REDSTONE (1-12) ===
        set("REDSTONE_TORCH", 1.25); set("LEVER", 0.85);
        set("STONE_BUTTON", 0.40); set("OAK_BUTTON", 0.50); set("SPRUCE_BUTTON", 0.50); set("BIRCH_BUTTON", 0.50);
        set("JUNGLE_BUTTON", 0.60); set("ACACIA_BUTTON", 0.60); set("DARK_OAK_BUTTON", 0.60);
        set("MANGROVE_BUTTON", 0.60); set("CHERRY_BUTTON", 0.60); set("PALE_OAK_BUTTON", 0.60);
        set("BAMBOO_BUTTON", 0.50); set("CRIMSON_BUTTON", 0.75); set("WARPED_BUTTON", 0.75); set("POLISHED_BLACKSTONE_BUTTON", 0.60);
        set("STONE_PRESSURE_PLATE", 0.85); set("OAK_PRESSURE_PLATE", 1.00); set("SPRUCE_PRESSURE_PLATE", 1.00); set("BIRCH_PRESSURE_PLATE", 1.00);
        set("LIGHT_WEIGHTED_PRESSURE_PLATE", 4.00); set("HEAVY_WEIGHTED_PRESSURE_PLATE", 4.00);
        set("PISTON", 8.00); set("STICKY_PISTON", 10.00); set("OBSERVER", 8.00);
        set("HOPPER", 12.00); set("DROPPER", 4.00); set("DISPENSER", 5.00); set("CRAFTER", 15.00);
        set("COMPARATOR", 5.00); set("REPEATER", 5.00); set("DAYLIGHT_DETECTOR", 6.00);
        set("SCULK_SENSOR", 12.00); set("CALIBRATED_SCULK_SENSOR", 15.00);
        set("SCULK", 3.00); set("SCULK_CATALYST", 15.00); set("SCULK_SHRIEKER", 15.00); set("SCULK_VEIN", 1.50);
        set("TNT", 12.00); set("RAIL", 1.50); set("POWERED_RAIL", 3.00); set("DETECTOR_RAIL", 3.00); set("ACTIVATOR_RAIL", 3.00);
        set("TARGET", 4.00); set("LIGHTNING_ROD", 5.00);

        // === NETHER (0.5-15, netherite high) ===
        set("NETHERRACK", 0.40); set("NETHER_BRICKS", 2.00); set("RED_NETHER_BRICKS", 2.50);
        set("CHISELED_NETHER_BRICKS", 2.75); set("CRACKED_NETHER_BRICKS", 2.25);
        set("NETHER_BRICK_FENCE", 2.00); set("NETHER_BRICK_STAIRS", 2.25); set("NETHER_BRICK_SLAB", 1.00); set("NETHER_BRICK_WALL", 1.25);
        set("RED_NETHER_BRICK_STAIRS", 2.75); set("RED_NETHER_BRICK_SLAB", 1.25); set("RED_NETHER_BRICK_WALL", 1.50);
        set("NETHER_WART_BLOCK", 4.00); set("WARPED_WART_BLOCK", 3.50);
        set("BLACKSTONE", 0.85); set("POLISHED_BLACKSTONE", 1.25); set("POLISHED_BLACKSTONE_BRICKS", 1.75);
        set("CRACKED_POLISHED_BLACKSTONE_BRICKS", 1.50); set("CHISELED_POLISHED_BLACKSTONE", 2.00); set("GILDED_BLACKSTONE", 3.00);
        set("BLACKSTONE_STAIRS", 1.00); set("BLACKSTONE_SLAB", 0.50); set("BLACKSTONE_WALL", 0.60);
        set("POLISHED_BLACKSTONE_STAIRS", 1.50); set("POLISHED_BLACKSTONE_SLAB", 0.75); set("POLISHED_BLACKSTONE_WALL", 1.00);
        set("POLISHED_BLACKSTONE_BRICK_STAIRS", 2.00); set("POLISHED_BLACKSTONE_BRICK_SLAB", 1.00); set("POLISHED_BLACKSTONE_BRICK_WALL", 1.25);
        set("BASALT", 0.75); set("SMOOTH_BASALT", 1.00); set("POLISHED_BASALT", 1.25);
        set("SOUL_SAND", 1.25); set("SOUL_SOIL", 1.25); set("SOUL_TORCH", 1.00); set("SOUL_LANTERN", 2.50); set("SOUL_CAMPFIRE", 3.00);
        set("MAGMA_BLOCK", 2.00);
        set("CRIMSON_NYLIUM", 1.50); set("WARPED_NYLIUM", 1.50);
        set("CRIMSON_ROOTS", 0.75); set("WARPED_ROOTS", 0.75); set("NETHER_SPROUTS", 0.75);
        set("CRIMSON_FUNGUS", 1.00); set("WARPED_FUNGUS", 1.00);
        set("WEEPING_VINES", 1.00); set("TWISTING_VINES", 1.00);
        set("CRIMSON_DOOR", 2.00); set("WARPED_DOOR", 2.00); set("CRIMSON_TRAPDOOR", 1.75); set("WARPED_TRAPDOOR", 1.75);
        set("CRIMSON_FENCE", 1.25); set("WARPED_FENCE", 1.25);

        // === END (high-end kept original) ===
        set("END_STONE", 15.00); set("END_STONE_BRICKS", 18.00);
        set("END_STONE_BRICK_STAIRS", 19.00); set("END_STONE_BRICK_SLAB", 9.00); set("END_STONE_BRICK_WALL", 10.00);
        set("PURPUR_BLOCK", 20.00); set("PURPUR_PILLAR", 22.00); set("PURPUR_STAIRS", 22.00); set("PURPUR_SLAB", 10.00);
        set("END_ROD", 15.00);
        set("DRAGON_EGG", 5000.00); set("END_CRYSTAL", 50.00);
        set("CREAKING_HEART", 30.00);

        // === 26.2 SULFUR / CINNABAR (new, affordable 2-8) ===
        set("SULFUR", 2.50); set("SULFUR_STAIRS", 2.75); set("SULFUR_SLAB", 1.25); set("SULFUR_WALL", 1.50);
        set("POLISHED_SULFUR", 3.00); set("POLISHED_SULFUR_STAIRS", 3.25); set("POLISHED_SULFUR_SLAB", 1.50); set("POLISHED_SULFUR_WALL", 1.75);
        set("SULFUR_BRICKS", 3.50); set("SULFUR_BRICK_STAIRS", 3.75); set("SULFUR_BRICK_SLAB", 1.75); set("SULFUR_BRICK_WALL", 2.00);
        set("CHISELED_SULFUR", 4.00); set("POTENT_SULFUR", 8.00); set("SULFUR_SPIKE", 1.50);
        set("CINNABAR", 2.50); set("CINNABAR_STAIRS", 2.75); set("CINNABAR_SLAB", 1.25); set("CINNABAR_WALL", 1.50);
        set("POLISHED_CINNABAR", 3.00); set("POLISHED_CINNABAR_STAIRS", 3.25); set("POLISHED_CINNABAR_SLAB", 1.50); set("POLISHED_CINNABAR_WALL", 1.75);
        set("CINNABAR_BRICKS", 3.50); set("CINNABAR_BRICK_STAIRS", 3.75); set("CINNABAR_BRICK_SLAB", 1.75); set("CINNABAR_BRICK_WALL", 2.00);
        set("CHISELED_CINNABAR", 4.00);

        // Copper doors/trapdoors/grates (1.21)
        set("COPPER_DOOR", 5.00); set("EXPOSED_COPPER_DOOR", 5.00); set("WEATHERED_COPPER_DOOR", 5.00); set("OXIDIZED_COPPER_DOOR", 5.00);
        set("WAXED_COPPER_DOOR", 5.50); set("WAXED_EXPOSED_COPPER_DOOR", 5.50); set("WAXED_WEATHERED_COPPER_DOOR", 5.50); set("WAXED_OXIDIZED_COPPER_DOOR", 5.50);
        set("COPPER_TRAPDOOR", 4.00); set("EXPOSED_COPPER_TRAPDOOR", 4.00); set("WEATHERED_COPPER_TRAPDOOR", 4.00); set("OXIDIZED_COPPER_TRAPDOOR", 4.00); set("WAXED_COPPER_TRAPDOOR", 4.50);

        // Init multipliers to 1.0
        for (Material mat : basePrices.keySet()) {
            multipliers.put(mat, 1.0);
        }
    }

    public double getBasePrice(Material material) {
        return basePrices.getOrDefault(material, 1.00);
    }

    public double getCurrentPrice(Material material) {
        double base = getBasePrice(material);
        double mult = multipliers.getOrDefault(material, 1.0);
        return Math.round(base * mult * inflationFactor * 100.0) / 100.0;
    }

    public double getSellPrice(Material material) {
        return Math.round(getCurrentPrice(material) * 0.6 * 100.0) / 100.0;
    }

    public double getBuyPrice(Material material) {
        return getCurrentPrice(material);
    }

    public double getEnchantmentBonus(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0.0;
        double bonus = 0.0;
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            bonus += entry.getValue() * 5.0;
        }
        return Math.round(bonus * 100.0) / 100.0;
    }

    public double getSellPriceWithEnchants(ItemStack item) {
        return Math.round((getSellPrice(item.getType()) + getEnchantmentBonus(item)) * 100.0) / 100.0;
    }

    public double getInflationFactor() {
        return inflationFactor;
    }

    private void updateInflation() {
        long daysRunning = (System.currentTimeMillis() - serverStartTime) / (1000 * 60 * 20);
        inflationFactor = 1.0 + (daysRunning * 0.0005);
    }

    private void driftPrices() {
        for (Material mat : multipliers.keySet()) {
            String category = getCategory(mat);
            double volatility = categoryVolatility.getOrDefault(category, 0.10);

            double drift = (random.nextDouble() - 0.5) * 2 * volatility;
            double newMult = multipliers.get(mat) + drift;
            newMult = Math.max(0.85, Math.min(1.20, newMult));

            newMult += (1.0 - newMult) * 0.05;
            newMult = Math.max(0.85, Math.min(1.20, newMult));

            multipliers.put(mat, newMult);
        }
    }

    private String getCategory(Material mat) {
        String name = mat.name();
        if (name.contains("SULFUR") || name.contains("CINNABAR") || name.contains("POTENT")) return "stone";
        if (name.contains("PALE_") || name.contains("RESIN") || name.contains("CREAKING") || name.contains("EYEBLOSSOM")) return "wood";
        if (name.contains("COPPER_GOLEM") || name.contains("COPPER_CHEST") || name.contains("SHELF") || name.contains("HARNESS") || name.contains("BUNDLE") || name.contains("MACE") || name.contains("HEAVY_CORE") || name.contains("BREEZE") || name.contains("WIND_CHARGE") || name.contains("TRIAL") || name.contains("VAULT") || name.contains("CRAFTER")) return "misc";
        if (name.contains("DANDELION") || name.contains("BUSH") || name.contains("LITTER") || name.contains("WILDFLOWER") || name.contains("DRY_GRASS") || name.contains("CACTUS_FLOWER") || name.contains("FIREFLY")) return "crop";
        if (name.contains("_LOG") || name.contains("_PLANKS") || name.contains("_WOOD") || name.contains("STICK") || name.contains("_LEAVES") || name.contains("_SAPLING") || name.contains("PROPAGULE") || name.contains("HYPHAE") || name.contains("_STEM") || name.contains("BAMBOO") || name.contains("AZALEA") || name.contains("MOSS")) return "wood";
        if (name.contains("_ORE") || name.contains("_INGOT") || name.contains("RAW_") || name.contains("DIAMOND") || name.contains("EMERALD") || name.contains("LAPIS") || name.contains("REDSTONE") || name.contains("QUARTZ") || name.contains("AMETHYST") || name.contains("NETHERITE") || name.contains("OBSIDIAN") || name.contains("NUGGET") || name.contains("GLOWSTONE")) return "ore";
        if (name.contains("WHEAT") || name.contains("CARROT") || name.contains("POTATO") || name.contains("BEETROOT") || name.contains("MELON") || name.contains("PUMPKIN") || name.contains("SUGAR") || name.contains("BAMBOO") || name.contains("COCOA") || name.contains("WART") || name.contains("CHORUS") || name.contains("BERRY") || name.contains("APPLE") || name.contains("MUSHROOM") || name.contains("CACTUS") || name.contains("KELP") || name.contains("LILY") || name.contains("FLOWER") || name.contains("TULIP") || name.contains("DAISY") || name.contains("PETAL") || name.contains("GRASS") || name.contains("FERN") || name.contains("DRIPLEAF") || name.contains("TORCHFLOWER") || name.contains("PITCHER")) return "crop";
        if (name.contains("SWORD") || name.contains("PICKAXE") || name.contains("AXE") || name.contains("SHOVEL") || name.contains("HOE") || name.contains("BOW") || name.contains("CROSSBOW") || name.contains("SHEARS") || name.contains("FISHING") || name.contains("FLINT_AND") || name.contains("BRUSH") || name.contains("BUCKET") || name.contains("COMPASS") || name.contains("CLOCK") || name.contains("MAP") || name.contains("BOAT") || name.contains("RAFT") || name.contains("MINECART") || name.contains("MACE")) return "tool";
        if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS") || name.contains("SHIELD") || name.contains("ELYTRA") || name.contains("HORSE_ARMOR") || name.contains("HARNESS") || name.contains("TRIM") || name.contains("WOLF_ARMOR")) return "armor";
        if (name.contains("PISTON") || name.contains("OBSERVER") || name.contains("HOPPER") || name.contains("DROPPER") || name.contains("DISPENSER") || name.contains("REDSTONE") || name.contains("LEVER") || name.contains("BUTTON") || name.contains("PRESSURE") || name.contains("TRIPWIRE") || name.contains("TNT") || name.contains("NOTE") || name.contains("JUKEBOX") || name.contains("FARMLAND") || name.contains("SCAFFOLD") || name.contains("CRAFTER") || name.contains("BULB") || name.contains("SCULK") || name.contains("RAIL") || name.contains("TARGET") || name.contains("LIGHTNING")) return "redstone";
        if (name.contains("NETHER") || name.contains("BLACKSTONE") || name.contains("BASALT") || name.contains("WART") || name.contains("SHROOMLIGHT") || name.contains("GLOWSTONE") || name.contains("MAGMA") || name.contains("SOUL") || name.contains("CRIMSON") || name.contains("WARPED") || name.contains("NYLIUM") || name.contains("VINE")) return "nether";
        if (name.contains("END_") || name.contains("PURPUR") || name.contains("DRAGON") || name.contains("SHULKER") || name.contains("CHORUS") || name.contains("ENDER")) return "end";
        if (name.contains("ROTTEN") || name.contains("BONE") || name.contains("STRING") || name.contains("SPIDER") || name.contains("GUNPOWDER") || name.contains("BLAZE") || name.contains("GHAST") || name.contains("SKULL") || name.contains("PHANTOM") || name.contains("ELYTRA") || name.contains("TOTEM") || name.contains("STAR") || name.contains("ARROW") || name.contains("FLINT") || name.contains("LEATHER") || name.contains("FEATHER") || name.contains("RABBIT") || name.contains("SLIME") || name.contains("HONEY") || name.contains("SCUTE") || name.contains("EGG") || name.contains("INK") || name.contains("CHARCOAL") || name.contains("PRISMARINE") || name.contains("NAUTILUS") || name.contains("HEART_OF") || name.contains("ECHO") || name.contains("DISC_FRAGMENT")) return "mob_drop";
        if (name.contains("COOKED_") || name.contains("BREAD") || name.contains("PIE") || name.contains("COOKIE") || name.contains("CAKE") || name.contains("STEW") || name.contains("GOLDEN_CARROT") || name.contains("BAKED_") || name.contains("POISONOUS") || name.contains("BEEF") || name.contains("PORK") || name.contains("MUTTON") || name.contains("CHICKEN") || name.contains("RABBIT") || name.contains("COD") || name.contains("SALMON") || name.contains("TROPICAL") || name.contains("PUFFERFISH") || name.contains("MILK")) return "food";
        if (name.contains("STAIRS") || name.contains("SLAB") || name.contains("FENCE") || name.contains("DOOR") || name.contains("TRAPDOOR") || name.contains("WALL") || name.contains("GLASS") || name.contains("TERRACOTTA") || name.contains("BRICK") || name.contains("PRISMARINE") || name.contains("PACKED") || name.contains("MUD") || name.contains("WOOL") || name.contains("CONCRETE") || name.contains("RESIN")) return "building";
        if (name.contains("BANNER") || name.contains("BED") || name.contains("PAINTING") || name.contains("FRAME") || name.contains("FLOWER") || name.contains("POT") || name.contains("JUKEBOX") || name.contains("BOOK") || name.contains("ENCHANT") || name.contains("BELL") || name.contains("LOOM") || name.contains("BARREL") || name.contains("CHEST") || name.contains("ANVIL") || name.contains("CAULDRON") || name.contains("LADDER") || name.contains("TORCH") || name.contains("LANTERN") || name.contains("CAMPFIRE") || name.contains("CHAIN") || name.contains("BONE_BLOCK") || name.contains("IRON_BARS") || name.contains("CANDLE") || name.contains("BEACON") || name.contains("CONDUIT") || name.contains("SHELF") || name.contains("DISC")) return "decoration";
        return "misc";
    }

    private void startDailyUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateInflation();
                driftPrices();
            }
        }.runTaskTimer(plugin, 6000L, 6000L);
    }

    public Map<Material, Double> getAllPrices() {
        Map<Material, Double> prices = new HashMap<>();
        for (Material mat : basePrices.keySet()) {
            prices.put(mat, getCurrentPrice(mat));
        }
        return prices;
    }
}
