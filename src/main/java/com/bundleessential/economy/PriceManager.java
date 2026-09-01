package com.bundleessential.economy;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PriceManager {

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final Map<Material, Double> basePrices = new HashMap<>();
    private final Map<Material, Double> multipliers = new HashMap<>();
    private final Map<String, Double> categoryVolatility = new HashMap<>();

    private long serverStartTime;
    private double inflationFactor = 1.0;

    public PriceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        serverStartTime = System.currentTimeMillis();
        initCategories();
        initPrices();
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

    private void initPrices() {
        // === WOOD ===
        basePrices.put(Material.OAK_LOG, 8.00);
        basePrices.put(Material.BIRCH_LOG, 10.00);
        basePrices.put(Material.SPRUCE_LOG, 12.00);
        basePrices.put(Material.JUNGLE_LOG, 15.00);
        basePrices.put(Material.ACACIA_LOG, 18.00);
        basePrices.put(Material.DARK_OAK_LOG, 20.00);
        basePrices.put(Material.MANGROVE_LOG, 22.00);
        basePrices.put(Material.CHERRY_LOG, 25.00);
        basePrices.put(Material.CRIMSON_STEM, 30.00);
        basePrices.put(Material.WARPED_STEM, 30.00);
        basePrices.put(Material.OAK_PLANKS, 2.00);
        basePrices.put(Material.BIRCH_PLANKS, 2.50);
        basePrices.put(Material.SPRUCE_PLANKS, 3.00);
        basePrices.put(Material.JUNGLE_PLANKS, 3.75);
        basePrices.put(Material.ACACIA_PLANKS, 4.50);
        basePrices.put(Material.DARK_OAK_PLANKS, 5.00);
        basePrices.put(Material.MANGROVE_PLANKS, 5.50);
        basePrices.put(Material.CHERRY_PLANKS, 6.25);
        basePrices.put(Material.CRIMSON_PLANKS, 7.50);
        basePrices.put(Material.WARPED_PLANKS, 7.50);
        basePrices.put(Material.BAMBOO_BLOCK, 4.00);
        basePrices.put(Material.BAMBOO_PLANKS, 1.50);
        basePrices.put(Material.STRIPPED_OAK_LOG, 9.00);
        basePrices.put(Material.STRIPPED_BIRCH_LOG, 11.00);
        basePrices.put(Material.STRIPPED_SPRUCE_LOG, 13.00);
        basePrices.put(Material.STRIPPED_JUNGLE_LOG, 16.00);
        basePrices.put(Material.STRIPPED_ACACIA_LOG, 19.00);
        basePrices.put(Material.STRIPPED_DARK_OAK_LOG, 21.00);
        basePrices.put(Material.OAK_WOOD, 10.00);
        basePrices.put(Material.BIRCH_WOOD, 12.00);
        basePrices.put(Material.SPRUCE_WOOD, 14.00);
        basePrices.put(Material.JUNGLE_WOOD, 17.00);
        basePrices.put(Material.ACACIA_WOOD, 20.00);
        basePrices.put(Material.DARK_OAK_WOOD, 22.00);
        basePrices.put(Material.OAK_LEAVES, 1.00);
        basePrices.put(Material.BIRCH_LEAVES, 1.00);
        basePrices.put(Material.SPRUCE_LEAVES, 1.00);
        basePrices.put(Material.JUNGLE_LEAVES, 1.00);
        basePrices.put(Material.ACACIA_LEAVES, 1.00);
        basePrices.put(Material.DARK_OAK_LEAVES, 1.00);
        basePrices.put(Material.OAK_SAPLING, 5.00);
        basePrices.put(Material.BIRCH_SAPLING, 5.00);
        basePrices.put(Material.SPRUCE_SAPLING, 5.00);
        basePrices.put(Material.JUNGLE_SAPLING, 8.00);
        basePrices.put(Material.ACACIA_SAPLING, 6.00);
        basePrices.put(Material.DARK_OAK_SAPLING, 6.00);
        basePrices.put(Material.STICK, 0.50);
        basePrices.put(Material.OAK_SIGN, 2.00);
        basePrices.put(Material.OAK_BOAT, 8.00);

        // === STONE ===
        basePrices.put(Material.COBBLESTONE, 1.00);
        basePrices.put(Material.STONE, 3.00);
        basePrices.put(Material.DEEPSLATE, 4.00);
        basePrices.put(Material.ANDESITE, 3.50);
        basePrices.put(Material.DIORITE, 3.50);
        basePrices.put(Material.GRANITE, 3.50);
        basePrices.put(Material.TUFF, 5.00);
        basePrices.put(Material.DRIPSTONE_BLOCK, 6.00);
        basePrices.put(Material.CALCITE, 8.00);
        basePrices.put(Material.SMOOTH_STONE, 4.00);
        basePrices.put(Material.STONE_BRICKS, 5.00);
        basePrices.put(Material.MOSSY_STONE_BRICKS, 8.00);
        basePrices.put(Material.CRACKED_STONE_BRICKS, 4.00);
        basePrices.put(Material.CHISELED_STONE_BRICKS, 6.00);
        basePrices.put(Material.COBBLED_DEEPSLATE, 2.00);
        basePrices.put(Material.DEEPSLATE_BRICKS, 6.00);
        basePrices.put(Material.POLISHED_DEEPSLATE, 5.00);
        basePrices.put(Material.POLISHED_ANDESITE, 5.00);
        basePrices.put(Material.POLISHED_DIORITE, 5.00);
        basePrices.put(Material.POLISHED_GRANITE, 5.00);
        basePrices.put(Material.INFESTED_STONE, 2.00);
        basePrices.put(Material.INFESTED_COBBLESTONE, 1.50);
        basePrices.put(Material.MOSS_BLOCK, 8.00);
        basePrices.put(Material.MOSS_CARPET, 3.00);
        basePrices.put(Material.MUD, 3.00);
        basePrices.put(Material.PACKED_MUD, 4.00);
        basePrices.put(Material.MUD_BRICKS, 6.00);
        basePrices.put(Material.REINFORCED_DEEPSLATE, 50.00);

        // === ORES ===
        basePrices.put(Material.COAL_ORE, 12.00);
        basePrices.put(Material.DEEPSLATE_COAL_ORE, 14.00);
        basePrices.put(Material.IRON_ORE, 25.00);
        basePrices.put(Material.DEEPSLATE_IRON_ORE, 28.00);
        basePrices.put(Material.COPPER_ORE, 20.00);
        basePrices.put(Material.DEEPSLATE_COPPER_ORE, 22.00);
        basePrices.put(Material.GOLD_ORE, 50.00);
        basePrices.put(Material.DEEPSLATE_GOLD_ORE, 55.00);
        basePrices.put(Material.REDSTONE_ORE, 35.00);
        basePrices.put(Material.DEEPSLATE_REDSTONE_ORE, 38.00);
        basePrices.put(Material.LAPIS_ORE, 60.00);
        basePrices.put(Material.DEEPSLATE_LAPIS_ORE, 65.00);
        basePrices.put(Material.DIAMOND_ORE, 200.00);
        basePrices.put(Material.DEEPSLATE_DIAMOND_ORE, 220.00);
        basePrices.put(Material.EMERALD_ORE, 250.00);
        basePrices.put(Material.DEEPSLATE_EMERALD_ORE, 275.00);
        basePrices.put(Material.NETHER_GOLD_ORE, 55.00);
        basePrices.put(Material.NETHER_QUARTZ_ORE, 30.00);
        basePrices.put(Material.ANCIENT_DEBRIS, 500.00);
        basePrices.put(Material.COAL, 3.00);
        basePrices.put(Material.RAW_IRON, 12.00);
        basePrices.put(Material.RAW_COPPER, 10.00);
        basePrices.put(Material.RAW_GOLD, 25.00);
        basePrices.put(Material.IRON_INGOT, 15.00);
        basePrices.put(Material.GOLD_INGOT, 30.00);
        basePrices.put(Material.COPPER_INGOT, 8.00);
        basePrices.put(Material.DIAMOND, 250.00);
        basePrices.put(Material.EMERALD, 150.00);
        basePrices.put(Material.LAPIS_LAZULI, 10.00);
        basePrices.put(Material.REDSTONE, 2.00);
        basePrices.put(Material.AMETHYST_SHARD, 15.00);
        basePrices.put(Material.NETHERITE_INGOT, 1000.00);
        basePrices.put(Material.NETHERITE_SCRAP, 300.00);
        basePrices.put(Material.QUARTZ, 8.00);
        basePrices.put(Material.AMETHYST_BLOCK, 20.00);
        basePrices.put(Material.COPPER_BLOCK, 80.00);
        basePrices.put(Material.IRON_BLOCK, 150.00);
        basePrices.put(Material.GOLD_BLOCK, 300.00);
        basePrices.put(Material.DIAMOND_BLOCK, 2500.00);
        basePrices.put(Material.EMERALD_BLOCK, 1500.00);
        basePrices.put(Material.LAPIS_BLOCK, 100.00);
        basePrices.put(Material.REDSTONE_BLOCK, 20.00);
        basePrices.put(Material.NETHERITE_BLOCK, 10000.00);
        basePrices.put(Material.RAW_IRON_BLOCK, 120.00);
        basePrices.put(Material.RAW_GOLD_BLOCK, 250.00);
        basePrices.put(Material.RAW_COPPER_BLOCK, 100.00);
        basePrices.put(Material.CRYING_OBSIDIAN, 60.00);
        basePrices.put(Material.OBSIDIAN, 50.00);

        // === CROPS ===
        basePrices.put(Material.WHEAT, 2.00);
        basePrices.put(Material.WHEAT_SEEDS, 0.50);
        basePrices.put(Material.CARROT, 3.00);
        basePrices.put(Material.POTATO, 2.50);
        basePrices.put(Material.BEETROOT, 2.00);
        basePrices.put(Material.BEETROOT_SEEDS, 0.50);
        basePrices.put(Material.MELON, 5.00);
        basePrices.put(Material.MELON_SLICE, 1.50);
        basePrices.put(Material.PUMPKIN, 6.00);
        basePrices.put(Material.PUMPKIN_SEEDS, 0.50);
        basePrices.put(Material.SUGAR_CANE, 3.00);
        basePrices.put(Material.BAMBOO, 1.00);
        basePrices.put(Material.COCOA_BEANS, 4.00);
        basePrices.put(Material.NETHER_WART, 8.00);
        basePrices.put(Material.CHORUS_FRUIT, 12.00);
        basePrices.put(Material.CHORUS_FLOWER, 20.00);
        basePrices.put(Material.SWEET_BERRIES, 2.00);
        basePrices.put(Material.GLOW_BERRIES, 6.00);
        basePrices.put(Material.APPLE, 5.00);
        basePrices.put(Material.GOLDEN_APPLE, 250.00);
        basePrices.put(Material.ENCHANTED_GOLDEN_APPLE, 2500.00);
        basePrices.put(Material.BROWN_MUSHROOM, 2.00);
        basePrices.put(Material.RED_MUSHROOM, 2.00);
        basePrices.put(Material.MUSHROOM_STEM, 1.00);
        basePrices.put(Material.CACTUS, 2.00);
        basePrices.put(Material.VINE, 3.00);
        basePrices.put(Material.GLOW_LICHEN, 5.00);
        basePrices.put(Material.KELP, 1.00);
        basePrices.put(Material.DRIED_KELP, 1.50);
        basePrices.put(Material.SEAGRASS, 1.00);
        basePrices.put(Material.TALL_SEAGRASS, 1.50);
        basePrices.put(Material.LILY_PAD, 2.00);

        // === MOB DROPS ===
        basePrices.put(Material.ROTTEN_FLESH, 0.50);
        basePrices.put(Material.BONE, 3.00);
        basePrices.put(Material.BONE_MEAL, 1.00);
        basePrices.put(Material.STRING, 4.00);
        basePrices.put(Material.SPIDER_EYE, 5.00);
        basePrices.put(Material.GUNPOWDER, 6.00);
        basePrices.put(Material.ENDER_PEARL, 25.00);
        basePrices.put(Material.BLAZE_ROD, 30.00);
        basePrices.put(Material.BLAZE_POWDER, 15.00);
        basePrices.put(Material.MAGMA_CREAM, 12.00);
        basePrices.put(Material.GHAST_TEAR, 40.00);
        basePrices.put(Material.WITHER_SKELETON_SKULL, 150.00);
        basePrices.put(Material.ZOMBIE_HEAD, 50.00);
        basePrices.put(Material.SKELETON_SKULL, 40.00);
        basePrices.put(Material.CREEPER_HEAD, 60.00);
        basePrices.put(Material.PLAYER_HEAD, 100.00);
        basePrices.put(Material.PHANTOM_MEMBRANE, 20.00);
        basePrices.put(Material.SHULKER_SHELL, 80.00);
        basePrices.put(Material.ELYTRA, 750.00);
        basePrices.put(Material.TOTEM_OF_UNDYING, 1500.00);
        basePrices.put(Material.NETHER_STAR, 3000.00);
        basePrices.put(Material.ARROW, 1.00);
        basePrices.put(Material.FLINT, 2.00);
        basePrices.put(Material.LEATHER, 5.00);
        basePrices.put(Material.FEATHER, 1.50);
        basePrices.put(Material.RABBIT_HIDE, 3.00);
        basePrices.put(Material.RABBIT_FOOT, 8.00);
        basePrices.put(Material.TRIDENT, 500.00);
        basePrices.put(Material.HEART_OF_THE_SEA, 1000.00);
        basePrices.put(Material.SLIME_BALL, 8.00);
        basePrices.put(Material.SLIME_BLOCK, 12.00);
        basePrices.put(Material.HONEYCOMB, 10.00);
        basePrices.put(Material.HONEY_BOTTLE, 5.00);
        basePrices.put(Material.EGG, 1.00);
        basePrices.put(Material.INK_SAC, 2.00);
        basePrices.put(Material.GLOW_INK_SAC, 5.00);
        basePrices.put(Material.SCUTE, 15.00);
        basePrices.put(Material.TURTLE_EGG, 10.00);
        basePrices.put(Material.SHROOMLIGHT, 12.00);
        basePrices.put(Material.CHARCOAL, 1.50);

        // === FOOD ===
        basePrices.put(Material.COOKED_BEEF, 8.00);
        basePrices.put(Material.COOKED_PORKCHOP, 8.00);
        basePrices.put(Material.COOKED_MUTTON, 6.00);
        basePrices.put(Material.COOKED_CHICKEN, 6.00);
        basePrices.put(Material.COOKED_RABBIT, 5.00);
        basePrices.put(Material.COOKED_COD, 5.00);
        basePrices.put(Material.COOKED_SALMON, 6.00);
        basePrices.put(Material.BREAD, 5.00);
        basePrices.put(Material.PUMPKIN_PIE, 6.00);
        basePrices.put(Material.COOKIE, 3.00);
        basePrices.put(Material.CAKE, 12.00);
        basePrices.put(Material.MUSHROOM_STEW, 6.00);
        basePrices.put(Material.BEETROOT_SOUP, 5.00);
        basePrices.put(Material.RABBIT_STEW, 8.00);
        basePrices.put(Material.SUSPICIOUS_STEW, 10.00);
        basePrices.put(Material.HONEY_BOTTLE, 5.00);
        basePrices.put(Material.GOLDEN_CARROT, 50.00);
        basePrices.put(Material.GOLDEN_APPLE, 250.00);
        basePrices.put(Material.ENCHANTED_GOLDEN_APPLE, 2500.00);
        basePrices.put(Material.DRIED_KELP, 1.50);
        basePrices.put(Material.TROPICAL_FISH, 5.00);
        basePrices.put(Material.PUFFERFISH, 4.00);
        basePrices.put(Material.POISONOUS_POTATO, 0.50);
        basePrices.put(Material.MELON_SLICE, 1.50);
        basePrices.put(Material.CARROT, 3.00);
        basePrices.put(Material.POTATO, 2.50);
        basePrices.put(Material.BAKED_POTATO, 4.00);
        basePrices.put(Material.BEETROOT, 2.00);
        basePrices.put(Material.SWEET_BERRIES, 2.00);
        basePrices.put(Material.GLOW_BERRIES, 6.00);

        // === TOOLS ===
        basePrices.put(Material.WOODEN_SWORD, 3.00);
        basePrices.put(Material.WOODEN_PICKAXE, 3.00);
        basePrices.put(Material.WOODEN_AXE, 3.00);
        basePrices.put(Material.WOODEN_SHOVEL, 1.50);
        basePrices.put(Material.WOODEN_HOE, 2.00);
        basePrices.put(Material.STONE_SWORD, 8.00);
        basePrices.put(Material.STONE_PICKAXE, 8.00);
        basePrices.put(Material.STONE_AXE, 8.00);
        basePrices.put(Material.STONE_SHOVEL, 4.00);
        basePrices.put(Material.STONE_HOE, 5.00);
        basePrices.put(Material.IRON_SWORD, 25.00);
        basePrices.put(Material.IRON_PICKAXE, 25.00);
        basePrices.put(Material.IRON_AXE, 25.00);
        basePrices.put(Material.IRON_SHOVEL, 12.00);
        basePrices.put(Material.IRON_HOE, 15.00);
        basePrices.put(Material.GOLDEN_SWORD, 20.00);
        basePrices.put(Material.GOLDEN_PICKAXE, 20.00);
        basePrices.put(Material.GOLDEN_AXE, 20.00);
        basePrices.put(Material.GOLDEN_SHOVEL, 10.00);
        basePrices.put(Material.GOLDEN_HOE, 12.00);
        basePrices.put(Material.DIAMOND_SWORD, 200.00);
        basePrices.put(Material.DIAMOND_PICKAXE, 200.00);
        basePrices.put(Material.DIAMOND_AXE, 200.00);
        basePrices.put(Material.DIAMOND_SHOVEL, 100.00);
        basePrices.put(Material.DIAMOND_HOE, 120.00);
        basePrices.put(Material.NETHERITE_SWORD, 800.00);
        basePrices.put(Material.NETHERITE_PICKAXE, 800.00);
        basePrices.put(Material.NETHERITE_AXE, 800.00);
        basePrices.put(Material.NETHERITE_SHOVEL, 400.00);
        basePrices.put(Material.NETHERITE_HOE, 500.00);
        basePrices.put(Material.SHEARS, 10.00);
        basePrices.put(Material.FLINT_AND_STEEL, 8.00);
        basePrices.put(Material.BOW, 15.00);
        basePrices.put(Material.CROSSBOW, 25.00);
        basePrices.put(Material.FISHING_ROD, 8.00);
        basePrices.put(Material.TRIDENT, 500.00);
        basePrices.put(Material.WARPED_FUNGUS_ON_A_STICK, 5.00);
        basePrices.put(Material.CARROT_ON_A_STICK, 5.00);

        // === ARMOR ===
        basePrices.put(Material.LEATHER_HELMET, 10.00);
        basePrices.put(Material.LEATHER_CHESTPLATE, 15.00);
        basePrices.put(Material.LEATHER_LEGGINGS, 12.00);
        basePrices.put(Material.LEATHER_BOOTS, 8.00);
        basePrices.put(Material.CHAINMAIL_HELMET, 20.00);
        basePrices.put(Material.CHAINMAIL_CHESTPLATE, 30.00);
        basePrices.put(Material.CHAINMAIL_LEGGINGS, 25.00);
        basePrices.put(Material.CHAINMAIL_BOOTS, 15.00);
        basePrices.put(Material.IRON_HELMET, 25.00);
        basePrices.put(Material.IRON_CHESTPLATE, 40.00);
        basePrices.put(Material.IRON_LEGGINGS, 35.00);
        basePrices.put(Material.IRON_BOOTS, 20.00);
        basePrices.put(Material.GOLDEN_HELMET, 20.00);
        basePrices.put(Material.GOLDEN_CHESTPLATE, 30.00);
        basePrices.put(Material.GOLDEN_LEGGINGS, 25.00);
        basePrices.put(Material.GOLDEN_BOOTS, 15.00);
        basePrices.put(Material.DIAMOND_HELMET, 200.00);
        basePrices.put(Material.DIAMOND_CHESTPLATE, 350.00);
        basePrices.put(Material.DIAMOND_LEGGINGS, 300.00);
        basePrices.put(Material.DIAMOND_BOOTS, 150.00);
        basePrices.put(Material.NETHERITE_HELMET, 800.00);
        basePrices.put(Material.NETHERITE_CHESTPLATE, 1200.00);
        basePrices.put(Material.NETHERITE_LEGGINGS, 1000.00);
        basePrices.put(Material.NETHERITE_BOOTS, 600.00);
        basePrices.put(Material.TURTLE_HELMET, 100.00);
        basePrices.put(Material.SHIELD, 15.00);
        basePrices.put(Material.ELYTRA, 750.00);

        // === REDSTONE ===
        basePrices.put(Material.REDSTONE, 2.00);
        basePrices.put(Material.REDSTONE_BLOCK, 20.00);
        basePrices.put(Material.REDSTONE_TORCH, 3.00);
        basePrices.put(Material.REDSTONE_LAMP, 10.00);
        basePrices.put(Material.PISTON, 15.00);
        basePrices.put(Material.STICKY_PISTON, 20.00);
        basePrices.put(Material.OBSERVER, 20.00);
        basePrices.put(Material.HOPPER, 25.00);
        basePrices.put(Material.DROPPER, 10.00);
        basePrices.put(Material.DISPENSER, 12.00);
        basePrices.put(Material.NOTE_BLOCK, 15.00);
        basePrices.put(Material.JUKEBOX, 30.00);
        basePrices.put(Material.DAYLIGHT_DETECTOR, 15.00);
        basePrices.put(Material.LEVER, 2.00);
        basePrices.put(Material.STONE_BUTTON, 1.00);
        basePrices.put(Material.OAK_BUTTON, 1.50);
        basePrices.put(Material.STONE_PRESSURE_PLATE, 2.00);
        basePrices.put(Material.OAK_PRESSURE_PLATE, 3.00);
        basePrices.put(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, 10.00);
        basePrices.put(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, 10.00);
        basePrices.put(Material.TNT, 30.00);
        basePrices.put(Material.TRIPWIRE_HOOK, 3.00);
        basePrices.put(Material.TRIPWIRE, 1.00);
        basePrices.put(Material.TRAPPED_CHEST, 10.00);
        basePrices.put(Material.FARMLAND, 2.00);
        basePrices.put(Material.SOUL_SAND, 5.00);
        basePrices.put(Material.SOUL_SOIL, 5.00);
        basePrices.put(Material.MAGMA_BLOCK, 8.00);
        basePrices.put(Material.SCAFFOLDING, 2.00);

        // === NETHER ===
        basePrices.put(Material.NETHERRACK, 2.00);
        basePrices.put(Material.NETHER_BRICKS, 8.00);
        basePrices.put(Material.RED_NETHER_BRICKS, 10.00);
        basePrices.put(Material.NETHER_BRICK_FENCE, 8.00);
        basePrices.put(Material.NETHER_BRICK_STAIRS, 9.00);
        basePrices.put(Material.NETHER_WART_BLOCK, 15.00);
        basePrices.put(Material.WARPED_WART_BLOCK, 12.00);
        basePrices.put(Material.SHROOMLIGHT, 12.00);
        basePrices.put(Material.GLOWSTONE, 10.00);
        basePrices.put(Material.BLACKSTONE, 3.00);
        basePrices.put(Material.POLISHED_BLACKSTONE, 5.00);
        basePrices.put(Material.POLISHED_BLACKSTONE_BRICKS, 7.00);
        basePrices.put(Material.CRACKED_POLISHED_BLACKSTONE_BRICKS, 5.00);
        basePrices.put(Material.CHISELED_POLISHED_BLACKSTONE, 8.00);
        basePrices.put(Material.GILDED_BLACKSTONE, 12.00);
        basePrices.put(Material.BASALT, 3.00);
        basePrices.put(Material.SMOOTH_BASALT, 4.00);
        basePrices.put(Material.POLISHED_BASALT, 5.00);
        basePrices.put(Material.NETHER_GOLD_ORE, 55.00);
        basePrices.put(Material.NETHER_QUARTZ_ORE, 30.00);
        basePrices.put(Material.SOUL_SOIL, 5.00);
        basePrices.put(Material.MAGMA_BLOCK, 8.00);
        basePrices.put(Material.CRYING_OBSIDIAN, 60.00);
        basePrices.put(Material.OBSIDIAN, 50.00);
        basePrices.put(Material.NETHERITE_BLOCK, 10000.00);
        basePrices.put(Material.ANCIENT_DEBRIS, 500.00);

        // === END ===
        basePrices.put(Material.END_STONE, 15.00);
        basePrices.put(Material.END_STONE_BRICKS, 18.00);
        basePrices.put(Material.PURPUR_BLOCK, 20.00);
        basePrices.put(Material.PURPUR_PILLAR, 22.00);
        basePrices.put(Material.PURPUR_STAIRS, 22.00);
        basePrices.put(Material.PURPUR_SLAB, 10.00);
        basePrices.put(Material.DRAGON_EGG, 5000.00);
        basePrices.put(Material.DRAGON_BREATH, 100.00);
        basePrices.put(Material.END_CRYSTAL, 50.00);
        basePrices.put(Material.ENDER_EYE, 20.00);
        basePrices.put(Material.END_ROD, 15.00);
        basePrices.put(Material.CHORUS_FRUIT, 12.00);
        basePrices.put(Material.CHORUS_FLOWER, 20.00);
        basePrices.put(Material.POPPED_CHORUS_FRUIT, 5.00);
        basePrices.put(Material.SHULKER_BOX, 100.00);
        basePrices.put(Material.PURPLE_SHULKER_BOX, 120.00);

        // === BUILDING ===
        basePrices.put(Material.OAK_FENCE, 3.00);
        basePrices.put(Material.BIRCH_FENCE, 3.50);
        basePrices.put(Material.SPRUCE_FENCE, 4.00);
        basePrices.put(Material.JUNGLE_FENCE, 5.00);
        basePrices.put(Material.ACACIA_FENCE, 6.00);
        basePrices.put(Material.DARK_OAK_FENCE, 7.00);
        basePrices.put(Material.OAK_STAIRS, 3.00);
        basePrices.put(Material.BIRCH_STAIRS, 3.50);
        basePrices.put(Material.SPRUCE_STAIRS, 4.00);
        basePrices.put(Material.OAK_SLAB, 1.50);
        basePrices.put(Material.BIRCH_SLAB, 1.75);
        basePrices.put(Material.SPRUCE_SLAB, 2.00);
        basePrices.put(Material.OAK_DOOR, 5.00);
        basePrices.put(Material.BIRCH_DOOR, 5.50);
        basePrices.put(Material.SPRUCE_DOOR, 6.00);
        basePrices.put(Material.OAK_TRAPDOOR, 4.00);
        basePrices.put(Material.BIRCH_TRAPDOOR, 4.50);
        basePrices.put(Material.STONE_BRICK_STAIRS, 6.00);
        basePrices.put(Material.STONE_BRICK_SLAB, 2.50);
        basePrices.put(Material.COBBLESTONE_STAIRS, 1.50);
        basePrices.put(Material.COBBLESTONE_SLAB, 0.75);
        basePrices.put(Material.COBBLESTONE_WALL, 1.00);
        basePrices.put(Material.BRICKS, 5.00);
        basePrices.put(Material.BRICK_STAIRS, 6.00);
        basePrices.put(Material.BRICK_SLAB, 2.50);
        basePrices.put(Material.NETHER_BRICKS, 8.00);
        basePrices.put(Material.RED_NETHER_BRICKS, 10.00);
        basePrices.put(Material.NETHER_BRICK_STAIRS, 9.00);
        basePrices.put(Material.NETHER_BRICK_FENCE, 8.00);
        basePrices.put(Material.END_STONE, 15.00);
        basePrices.put(Material.END_STONE_BRICKS, 18.00);
        basePrices.put(Material.PURPUR_BLOCK, 20.00);
        basePrices.put(Material.GLASS, 3.00);
        basePrices.put(Material.GLASS_PANE, 1.50);
        basePrices.put(Material.TERRACOTTA, 4.00);
        basePrices.put(Material.PRISMARINE, 25.00);
        basePrices.put(Material.PRISMARINE_BRICKS, 30.00);
        basePrices.put(Material.DARK_PRISMARINE, 35.00);
        basePrices.put(Material.SEA_LANTERN, 20.00);
        basePrices.put(Material.GLOWSTONE, 10.00);
        basePrices.put(Material.JACK_O_LANTERN, 8.00);
        basePrices.put(Material.CARVED_PUMPKIN, 5.00);
        basePrices.put(Material.HAY_BLOCK, 15.00);
        basePrices.put(Material.DRIED_KELP_BLOCK, 5.00);
        basePrices.put(Material.SNOW_BLOCK, 4.00);
        basePrices.put(Material.PACKED_ICE, 8.00);
        basePrices.put(Material.BLUE_ICE, 25.00);
        basePrices.put(Material.HONEY_BLOCK, 10.00);
        basePrices.put(Material.HONEYCOMB_BLOCK, 12.00);
        basePrices.put(Material.SMOOTH_STONE_SLAB, 2.00);
        basePrices.put(Material.CUT_COPPER, 9.00);
        basePrices.put(Material.CUT_COPPER_SLAB, 4.50);
        basePrices.put(Material.CUT_COPPER_STAIRS, 9.00);
        basePrices.put(Material.WAXED_COPPER_BLOCK, 85.00);
        basePrices.put(Material.MUD_BRICKS, 6.00);
        basePrices.put(Material.MUD_BRICK_STAIRS, 7.00);
        basePrices.put(Material.MUD_BRICK_SLAB, 3.00);
        basePrices.put(Material.BAMBOO_MOSAIC, 6.00);
        basePrices.put(Material.BAMBOO_MOSAIC_SLAB, 3.00);
        basePrices.put(Material.BAMBOO_MOSAIC_STAIRS, 6.00);
        basePrices.put(Material.BAMBOO_FENCE, 3.00);
        basePrices.put(Material.BAMBOO_FENCE_GATE, 4.00);
        basePrices.put(Material.BAMBOO_DOOR, 5.00);
        basePrices.put(Material.BAMBOO_TRAPDOOR, 4.00);
        basePrices.put(Material.BEDROCK, 9999.00);
        basePrices.put(Material.BARRIER, 9999.00);
        basePrices.put(Material.END_PORTAL_FRAME, 9999.00);

        // === DECORATION ===
        basePrices.put(Material.CRAFTING_TABLE, 5.00);
        basePrices.put(Material.FURNACE, 10.00);
        basePrices.put(Material.SMOKER, 15.00);
        basePrices.put(Material.BLAST_FURNACE, 20.00);
        basePrices.put(Material.STONECUTTER, 12.00);
        basePrices.put(Material.ANVIL, 50.00);
        basePrices.put(Material.CHIPPED_ANVIL, 40.00);
        basePrices.put(Material.DAMAGED_ANVIL, 25.00);
        basePrices.put(Material.BREWING_STAND, 30.00);
        basePrices.put(Material.CAULDRON, 15.00);
        basePrices.put(Material.LADDER, 2.00);
        basePrices.put(Material.BELL, 40.00);
        basePrices.put(Material.LOOM, 10.00);
        basePrices.put(Material.BARREL, 12.00);
        basePrices.put(Material.CHEST, 12.00);
        basePrices.put(Material.TRAPPED_CHEST, 14.00);
        basePrices.put(Material.ENDER_CHEST, 60.00);
        basePrices.put(Material.HOPPER, 25.00);
        basePrices.put(Material.DROPPER, 10.00);
        basePrices.put(Material.DISPENSER, 12.00);
        basePrices.put(Material.OBSERVER, 20.00);
        basePrices.put(Material.PISTON, 15.00);
        basePrices.put(Material.STICKY_PISTON, 20.00);
        basePrices.put(Material.TNT, 30.00);
        basePrices.put(Material.REDSTONE_LAMP, 10.00);
        basePrices.put(Material.TORCH, 1.00);
        basePrices.put(Material.SOUL_TORCH, 2.00);
        basePrices.put(Material.LANTERN, 5.00);
        basePrices.put(Material.SOUL_LANTERN, 6.00);
        basePrices.put(Material.CAMPFIRE, 8.00);
        basePrices.put(Material.SOUL_CAMPFIRE, 9.00);
        basePrices.put(Material.BONE_BLOCK, 10.00);
        basePrices.put(Material.IRON_BARS, 8.00);
        basePrices.put(Material.CHAIN, 10.00);
        basePrices.put(Material.END_ROD, 15.00);
        basePrices.put(Material.DRAGON_HEAD, 500.00);
        basePrices.put(Material.DRAGON_EGG, 5000.00);
        basePrices.put(Material.PLAYER_HEAD, 100.00);
        basePrices.put(Material.ZOMBIE_HEAD, 50.00);
        basePrices.put(Material.SKELETON_SKULL, 40.00);
        basePrices.put(Material.CREEPER_HEAD, 60.00);
        basePrices.put(Material.WITHER_SKELETON_SKULL, 150.00);
        basePrices.put(Material.PIGLIN_HEAD, 80.00);
        basePrices.put(Material.SHULKER_BOX, 100.00);
        basePrices.put(Material.JUKEBOX, 30.00);
        basePrices.put(Material.NOTE_BLOCK, 15.00);
        basePrices.put(Material.PAINTING, 10.00);
        basePrices.put(Material.ITEM_FRAME, 15.00);
        basePrices.put(Material.GLOW_ITEM_FRAME, 25.00);
        basePrices.put(Material.FLOWER_POT, 3.00);
        basePrices.put(Material.DECORATED_POT, 12.00);
        basePrices.put(Material.SUSPICIOUS_GRAVEL, 5.00);
        basePrices.put(Material.SUSPICIOUS_SAND, 5.00);
        basePrices.put(Material.CHISELED_BOOKSHELF, 15.00);
        basePrices.put(Material.BOOKSHELF, 12.00);
        basePrices.put(Material.ENCHANTING_TABLE, 150.00);
        basePrices.put(Material.PAINTING, 10.00);
        basePrices.put(Material.WHITE_BANNER, 5.00);
        basePrices.put(Material.ORANGE_BANNER, 5.00);
        basePrices.put(Material.MAGENTA_BANNER, 5.00);
        basePrices.put(Material.LIGHT_BLUE_BANNER, 5.00);
        basePrices.put(Material.YELLOW_BANNER, 5.00);
        basePrices.put(Material.LIME_BANNER, 5.00);
        basePrices.put(Material.PINK_BANNER, 5.00);
        basePrices.put(Material.GRAY_BANNER, 5.00);
        basePrices.put(Material.LIGHT_GRAY_BANNER, 5.00);
        basePrices.put(Material.CYAN_BANNER, 5.00);
        basePrices.put(Material.PURPLE_BANNER, 5.00);
        basePrices.put(Material.BLUE_BANNER, 5.00);
        basePrices.put(Material.BROWN_BANNER, 5.00);
        basePrices.put(Material.GREEN_BANNER, 5.00);
        basePrices.put(Material.RED_BANNER, 5.00);
        basePrices.put(Material.BLACK_BANNER, 5.00);
        basePrices.put(Material.WHITE_BED, 15.00);
        basePrices.put(Material.RED_BED, 15.00);
        basePrices.put(Material.BLUE_BED, 15.00);
        basePrices.put(Material.GREEN_BED, 15.00);
        basePrices.put(Material.PAINTING, 10.00);

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
        if (name.contains("_LOG") || name.contains("_PLANKS") || name.contains("_WOOD") || name.contains("STICK") || name.contains("_LEAVES") || name.contains("_SAPLING")) return "wood";
        if (name.contains("_ORE") || name.contains("_INGOT") || name.contains("RAW_") || name.contains("DIAMOND") || name.contains("EMERALD") || name.contains("LAPIS") || name.contains("REDSTONE") || name.contains("QUARTZ") || name.contains("AMETHYST") || name.contains("NETHERITE") || name.contains("OBSIDIAN")) return "ore";
        if (name.contains("WHEAT") || name.contains("CARROT") || name.contains("POTATO") || name.contains("BEETROOT") || name.contains("MELON") || name.contains("PUMPKIN") || name.contains("SUGAR") || name.contains("BAMBOO") || name.contains("COCOA") || name.contains("WART") || name.contains("CHORUS") || name.contains("BERRY") || name.contains("APPLE") || name.contains("MUSHROOM") || name.contains("CACTUS") || name.contains("KELP") || name.contains("LILY")) return "crop";
        if (name.contains("SWORD") || name.contains("PICKAXE") || name.contains("AXE") || name.contains("SHOVEL") || name.contains("HOE") || name.contains("BOW") || name.contains("CROSSBOW") || name.contains("SHEARS") || name.contains("FISHING") || name.contains("FLINT_AND")) return "tool";
        if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS") || name.contains("SHIELD") || name.contains("ELYTRA")) return "armor";
        if (name.contains("PISTON") || name.contains("OBSERVER") || name.contains("HOPPER") || name.contains("DROPPER") || name.contains("DISPENSER") || name.contains("REDSTONE") || name.contains("LEVER") || name.contains("BUTTON") || name.contains("PRESSURE") || name.contains("TRIPWIRE") || name.contains("TNT") || name.contains("NOTE") || name.contains("JUKEBOX") || name.contains("FARMLAND") || name.contains("SCAFFOLD")) return "redstone";
        if (name.contains("NETHER") || name.contains("BLACKSTONE") || name.contains("BASALT") || name.contains("WART") || name.contains("SHROOMLIGHT") || name.contains("GLOWSTONE") || name.contains("MAGMA") || name.contains("SOUL") || name.contains("CRIMSON") || name.contains("WARPED")) return "nether";
        if (name.contains("END_") || name.contains("PURPUR") || name.contains("DRAGON") || name.contains("SHULKER") || name.contains("CHORUS") || name.contains("ENDER")) return "end";
        if (name.contains("ROTTEN") || name.contains("BONE") || name.contains("STRING") || name.contains("SPIDER") || name.contains("GUNPOWDER") || name.contains("BLAZE") || name.contains("GHAST") || name.contains("SKULL") || name.contains("PHANTOM") || name.contains("ELYTRA") || name.contains("TOTEM") || name.contains("STAR") || name.contains("ARROW") || name.contains("FLINT") || name.contains("LEATHER") || name.contains("FEATHER") || name.contains("RABBIT") || name.contains("SLIME") || name.contains("HONEY") || name.contains("SCUTE") || name.contains("EGG") || name.contains("INK") || name.contains("CHARCOAL")) return "mob_drop";
        if (name.contains("COOKED_") || name.contains("BREAD") || name.contains("PIE") || name.contains("COOKIE") || name.contains("CAKE") || name.contains("STEW") || name.contains("GOLDEN_CARROT") || name.contains("BAKED_") || name.contains("POISONOUS")) return "food";
        if (name.contains("STAIRS") || name.contains("SLAB") || name.contains("FENCE") || name.contains("DOOR") || name.contains("TRAPDOOR") || name.contains("WALL") || name.contains("GLASS") || name.contains("TERRACOTTA") || name.contains("BRICK") || name.contains("PRISMARINE") || name.contains("PACKED") || name.contains("MUD") || name.contains("BEDROCK") || name.contains("BARRIER")) return "building";
        if (name.contains("BANNER") || name.contains("BED") || name.contains("PAINTING") || name.contains("FRAME") || name.contains("FLOWER") || name.contains("POT") || name.contains("JUKEBOX") || name.contains("BOOK") || name.contains("ENCHANT") || name.contains("BELL") || name.contains("LOOM") || name.contains("BARREL") || name.contains("CHEST") || name.contains("ANVIL") || name.contains("CAULDRON") || name.contains("LADDER") || name.contains("TORCH") || name.contains("LANTERN") || name.contains("CAMPFIRE") || name.contains("CHAIN") || name.contains("BONE_BLOCK") || name.contains("IRON_BARS")) return "decoration";
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
