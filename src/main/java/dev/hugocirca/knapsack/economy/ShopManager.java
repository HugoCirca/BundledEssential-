package dev.hugocirca.knapsack.economy;

import dev.hugocirca.knapsack.autosell.AutoSellManager;
import dev.hugocirca.knapsack.spawner.SpawnerManager;
import dev.hugocirca.knapsack.util.Money;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
    private final Map<UUID, Long> lastBuyTime = new HashMap<>();
    // Bedrock/Geyser can fire one tap twice (dupes land <50ms apart),
    // so ignore re-buys inside a short window without eating spam-clicks.
    private static final long BUY_DEBOUNCE_MS = 150L;
    private final Map<String, Material[]> categories = new LinkedHashMap<>();
    private final Set<UUID> searchSubmitted = new HashSet<>();
    private final Map<UUID, PendingBuy> pendingBuys = new HashMap<>();
    private final Set<UUID> switching = new HashSet<>();

    private static class PendingBuy {
        final Material material;
        final int amount;
        final Double unitOverride;
        final ItemStack product;

        PendingBuy(Material material, int amount) {
            this(material, amount, null, null);
        }

        PendingBuy(Material material, int amount, Double unitOverride, ItemStack product) {
            this.material = material;
            this.amount = amount;
            this.unitOverride = unitOverride;
            this.product = product;
        }
    }

    private static final int ITEMS_PER_PAGE = 21;
    private static final int[] ITEM_SLOTS = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34
    };
    // Plain title on purpose: Paper 1.21.4+ kicks players when an anvil title
    // sent via packets contains legacy color codes.
    private static final String SEARCH_TITLE = "Search shop items";
    private static final String BUY_TITLE = "§6§lBuy ";
    // Never sold: admin/creative-only (command execution, world editing).
    // SPAWNER is also excluded — the only spawner sold is the Custom
    // Zombie Spawner ($500) which stacks via right-click. The vanilla $1
    // one has no stacking and breaks the system.
    // Everything else — even unobtainable blocks — is listed.
    /** Kept for compat; canonical list is in ShopCatalog. */
    private static final String[] SHOP_EXCLUDED = ShopCatalog.SHOP_EXCLUDED;

    public ShopManager(BalanceManager balanceManager, PriceManager priceManager, SellManager sellManager) {
        this.balanceManager = balanceManager;
        this.priceManager = priceManager;
        this.sellManager = sellManager;
        categories.putAll(ShopCatalog.categories());
    }

    private Material[] miscItems() { return ShopCatalog.misc(categories); }
    private Material[] mats(String... names) { return ShopCatalog.mats(names); }
    private Material icon(String name, Material fallback) { return ShopCatalog.icon(name, fallback); }

    private Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("Knapsack");
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
        shop.setItem(25, makeItem(Material.CHEST, "§e§lMisc & More", "§7Everything else", "§7Click to browse"));

        // NEW: 1.21 -> 26.2 items (Sulfur, Cinnabar, Pale, Resin, Copper, Happy Ghast...)
        shop.setItem(31, makeItem(icon("SULFUR", Material.NETHERITE_INGOT), "§d§lNew 1.21-26.2", "§7Copper, Tuff, Pale, Resin", "§7Sulfur, Cinnabar, Ghast...", "§7Click to browse"));
        if (sellManager != null) {
            shop.setItem(40, makeItem(Material.EMERALD, "§a§lSell Items", "§7Click to open sell menu"));
        }
        shop.setItem(49, makeItem(Material.COMPASS, "§b§lSearch Items", "§7Type a name, jump to matches", "§7Click to search"));
        shop.setItem(53, makeItem(Material.BARRIER, "§c§lCustom", "§7Special items", "§7Click to look"));

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
                    "§ePrice: $" + Money.format(buyPrice),
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

    private double autosellPrice() {
        try {
            Plugin plugin = getPlugin();
            if (plugin instanceof JavaPlugin jp) {
                double p = jp.getConfig().getDouble("autosell.price", 500.0);
                if (p > 0) {
                    return Math.round(p * 100.0) / 100.0;
                }
            }
        } catch (Exception ignored) {}
        return 500.0;
    }

    private void openBuyGui(Player player, Material material, int amount) {
        openBuyGui(player, material, amount, null, null);
    }

    private void openBuyGui(Player player, Material material, int amount, Double unitOverride, ItemStack product) {
        int max = Math.max(1, material.getMaxStackSize());
        amount = Math.max(1, Math.min(max, amount));
        pendingBuys.put(player.getUniqueId(), new PendingBuy(material, amount, unitOverride, product));
        double unit = unitOverride != null ? unitOverride : priceManager.getBuyPrice(material);
        double total = Math.round(unit * amount * 100.0) / 100.0;

        Inventory inv = Bukkit.createInventory(null, 27, BUY_TITLE + formatName(material));
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(10, makeItem(Material.ARROW, "§cBack"));
        inv.setItem(12, makeItem(Material.RED_STAINED_GLASS_PANE, "§c§l-1", "§7Shift-click: -10"));
        ItemStack shown = product != null ? product.clone() : new ItemStack(material);
        shown.setAmount(Math.max(1, Math.min(shown.getMaxStackSize(), amount)));
        ItemMeta shownMeta = shown.getItemMeta();
        List<String> lore = new ArrayList<>();
        if (shownMeta != null && shownMeta.hasLore()) {
            lore.addAll(shownMeta.getLore());
        }
        lore.add("§eAmount: §f" + amount + "§7/§f" + max);
        lore.add("§eUnit: §a$" + Money.format(unit));
        lore.add("§eTotal: §a$" + Money.format(total));
        shownMeta.setLore(lore);
        shown.setItemMeta(shownMeta);
        inv.setItem(13, shown);
        inv.setItem(14, makeItem(Material.LIME_STAINED_GLASS_PANE, "§a§l+1", "§7Shift-click: +10"));
        inv.setItem(16, makeItem(Material.EMERALD_BLOCK, "§a§lConfirm: $" + Money.format(total),
                "§7Buy " + amount + "x " + formatName(material)));
        inv.setItem(19, makeItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lBuy 10",
                "§7Total: §a$" + Money.format(Math.round(unit * Math.min(10, max) * 100.0) / 100.0),
                "§7Instant buy"));
        inv.setItem(21, makeItem(Material.ORANGE_STAINED_GLASS_PANE, "§6§lBuy 25",
                "§7Total: §a$" + Money.format(Math.round(unit * Math.min(25, max) * 100.0) / 100.0),
                "§7Instant buy"));
        inv.setItem(23, makeItem(Material.MAGENTA_STAINED_GLASS_PANE, "§d§lBuy 50",
                "§7Total: §a$" + Money.format(Math.round(unit * Math.min(50, max) * 100.0) / 100.0),
                "§7Instant buy"));

        switching.add(player.getUniqueId());
        player.openInventory(inv);
    }

    private void confirmBuy(Player player, PendingBuy pending) {
        // Bedrock/Geyser can fire one tap twice: ignore re-buys inside the window
        long now = System.currentTimeMillis();
        if (now - lastBuyTime.getOrDefault(player.getUniqueId(), 0L) < BUY_DEBOUNCE_MS) return;
        lastBuyTime.put(player.getUniqueId(), now);
        double unit = pending.unitOverride != null ? pending.unitOverride
                : priceManager.getBuyPrice(pending.material);
        double total = Math.round(unit * pending.amount * 100.0) / 100.0;
        if (balanceManager.removeBalance(player, total)) {
            ItemStack deliver = pending.product != null ? pending.product.clone() : new ItemStack(pending.material);
            deliver.setAmount(pending.amount);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(deliver);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage("§aBought " + pending.amount + "x " + formatName(pending.material) + " for $" + Money.format(total));
            if (!leftover.isEmpty()) {
                player.sendMessage("§eNo room — extras dropped at your feet.");
            }
        } else {
            player.sendMessage("§cNot enough money! Need $" + Money.format(total));
        }
        openBuyGui(player, pending.material, pending.amount, pending.unitOverride, pending.product);
    }

    /** Instant-buy pane: buys a fixed bulk amount (clamped to max stack). */
    private void buyBulk(Player player, PendingBuy pending, int preset) {
        int amount = Math.max(1, Math.min(preset, Math.max(1, pending.material.getMaxStackSize())));
        confirmBuy(player, new PendingBuy(pending.material, amount, pending.unitOverride, pending.product));
    }

    private void openSearch(Player player) {
        Plugin plugin = JavaPlugin.getProvidingPlugin(ShopManager.class);
        try {
            new AnvilGUI.Builder()
                    .onClose(state -> {
                        Player p = state.getPlayer();
                        // Submit already scheduled showResults -> don't reopen shop over it.
                        if (searchSubmitted.remove(p.getUniqueId())) {
                            return;
                        }
                        if (!p.isOnline()) {
                            return;
                        }
                        Bukkit.getScheduler().runTask(plugin, () -> openShop(p));
                    })
                    .onClick((slot, state) -> {
                        if (slot != AnvilGUI.Slot.OUTPUT) {
                            return Collections.emptyList();
                        }
                        String text = state.getText() == null ? "" : state.getText().trim();
                        Player p = state.getPlayer();
                        if (text.isEmpty()) {
                            return Collections.singletonList(AnvilGUI.ResponseAction.close());
                        }
                        searchSubmitted.add(p.getUniqueId());
                        Bukkit.getScheduler().runTask(plugin, () -> showResults(p, text));
                        return Collections.singletonList(AnvilGUI.ResponseAction.close());
                    })
                    .text("Type item name...")
                    .title(SEARCH_TITLE)
                    .plugin(plugin)
                    .open(player);
        } catch (Exception e) {
            // Unsupported version etc: fall back to the shop instead of a dead screen.
            plugin.getLogger().warning("Shop search unavailable: " + e.getMessage());
            openShop(player);
            player.sendMessage("§cSearch is unavailable on this server version.");
        }
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

    /** Text fallback for /shop search <name> (Bedrock/Geyser players, quick typed search). */
    public void searchCommand(Player player, String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            player.sendMessage("§cUsage: /shop search <item name>");
            return;
        }
        showResults(player, q);
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

    private Material[] logsItems() { return ShopCatalog.logs(); }

    private void openLogsShop(Player player) {
        openCategoryPage(player, "Logs", logsItems(), 0);
    }

    private Material[] stoneItems() { return ShopCatalog.stone(); }

    private void openStoneShop(Player player) {
        openCategoryPage(player, "Stone", stoneItems(), 0);
    }

    private Material[] oresItems() { return ShopCatalog.ores(); }

    private void openOresShop(Player player) {
        openCategoryPage(player, "Ores", oresItems(), 0);
    }

    private Material[] cropsItems() { return ShopCatalog.crops(); }

    private void openCropsShop(Player player) {
        openCategoryPage(player, "Crops", cropsItems(), 0);
    }

    private Material[] mobDropsItems() { return ShopCatalog.mobDrops(); }

    private void openMobDropsShop(Player player) {
        openCategoryPage(player, "Mob Drops", mobDropsItems(), 0);
    }

    private Material[] foodItems() { return ShopCatalog.food(); }

    private void openFoodShop(Player player) {
        openCategoryPage(player, "Food", foodItems(), 0);
    }

    private Material[] toolsItems() { return ShopCatalog.tools(); }

    private void openToolsShop(Player player) {
        openCategoryPage(player, "Tools", toolsItems(), 0);
    }

    private Material[] armorItems() { return ShopCatalog.armor(); }

    private void openArmorShop(Player player) {
        openCategoryPage(player, "Armor", armorItems(), 0);
    }

    private Material[] buildingItems() { return ShopCatalog.building(); }

    private void openBuildingShop(Player player) {
        openCategoryPage(player, "Building", buildingItems(), 0);
    }

    private Material[] decorationItems() { return ShopCatalog.decoration(); }

    private void openDecorationShop(Player player) {
        openCategoryPage(player, "Decoration", decorationItems(), 0);
    }

    private Material[] redstoneItems() { return ShopCatalog.redstone(); }

    private void openRedstoneShop(Player player) {
        openCategoryPage(player, "Redstone", redstoneItems(), 0);
    }

    private Material[] netherItems() { return ShopCatalog.nether(); }

    private void openNetherShop(Player player) {
        openCategoryPage(player, "Nether", netherItems(), 0);
    }

    private Material[] endItems() { return ShopCatalog.end(); }

    private void openEndShop(Player player) {
        openCategoryPage(player, "End", endItems(), 0);
    }

    private Material[] latestItems() { return ShopCatalog.latest(); }

    private void openLatestShop(Player player) {
        openCategoryPage(player, "New 1.21-26.2", latestItems(), 0);
    }

    private void openMiscShop(Player player) {
        openCategoryPage(player, "Misc", categories.get("Misc"), 0);
    }

    private void openCustomShop(Player player) {
        playerPages.put(player.getUniqueId(), new ShopPage("Custom", new Material[0], 0));
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lCustom");
        inv.setItem(4, makeItem(Material.ARROW, "§cBack to Shop"));
        JavaPlugin shopPlugin = (JavaPlugin) JavaPlugin.getProvidingPlugin(ShopManager.class);
        ItemStack display = AutoSellManager.template(shopPlugin);
        ItemMeta displayMeta = display.getItemMeta();
        List<String> displayLore = new ArrayList<>(displayMeta.getLore());
        displayLore.add("§ePrice: §a$" + Money.format(autosellPrice()));
        displayLore.add("§7Click to buy");
        displayMeta.setLore(displayLore);
        display.setItemMeta(displayMeta);
        inv.setItem(22, display);
        ItemStack zombieDisplay = SpawnerManager.template(shopPlugin, EntityType.ZOMBIE);
        ItemMeta zombieMeta = zombieDisplay.getItemMeta();
        List<String> zombieLore = new ArrayList<>(zombieMeta.getLore());
        zombieLore.add("§ePrice: §a$" + Money.format(zombiePrice()));
        zombieLore.add("§7Click to buy");
        zombieMeta.setLore(zombieLore);
        zombieDisplay.setItemMeta(zombieMeta);
        inv.setItem(24, zombieDisplay);
        ItemStack skeletonDisplay = SpawnerManager.template(shopPlugin, EntityType.SKELETON);
        ItemMeta skeletonMeta = skeletonDisplay.getItemMeta();
        List<String> skeletonLore = new ArrayList<>(skeletonMeta.getLore());
        skeletonLore.add("§ePrice: §a$" + Money.format(skeletonPrice()));
        skeletonLore.add("§7Click to buy");
        skeletonMeta.setLore(skeletonLore);
        skeletonDisplay.setItemMeta(skeletonMeta);
        inv.setItem(25, skeletonDisplay);
        ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }
        player.openInventory(inv);
    }

    private void openAutosellBuyGui(Player player) {
        JavaPlugin shopPlugin = (JavaPlugin) JavaPlugin.getProvidingPlugin(ShopManager.class);
        openBuyGui(player, Material.CHEST, 1, autosellPrice(), AutoSellManager.template(shopPlugin));
    }

    private double spawnerPrice() {
        return zombiePrice();
    }

    private double zombiePrice() {
        try {
            Plugin plugin = getPlugin();
            if (plugin instanceof JavaPlugin jp) {
                if (jp.getConfig().contains("spawner.zombie-price")) {
                    double p = jp.getConfig().getDouble("spawner.zombie-price", 250.0);
                    if (p > 0) return Math.round(p * 100.0) / 100.0;
                }
                double p = jp.getConfig().getDouble("spawner.price", 250.0);
                if (p > 0) {
                    return Math.round(p * 100.0) / 100.0;
                }
            }
        } catch (Exception ignored) {}
        return 250.0;
    }

    private double skeletonPrice() {
        try {
            Plugin plugin = getPlugin();
            if (plugin instanceof JavaPlugin jp) {
                double p = jp.getConfig().getDouble("spawner.skeleton-price", 325.0);
                if (p > 0) {
                    return Math.round(p * 100.0) / 100.0;
                }
            }
        } catch (Exception ignored) {}
        return 325.0;
    }

    private void openSpawnerBuyGui(Player player) {
        openZombieBuyGui(player);
    }

    private void openZombieBuyGui(Player player) {
        JavaPlugin shopPlugin = (JavaPlugin) JavaPlugin.getProvidingPlugin(ShopManager.class);
        openBuyGui(player, Material.SPAWNER, 1, zombiePrice(), SpawnerManager.template(shopPlugin, EntityType.ZOMBIE));
    }

    private void openSkeletonBuyGui(Player player) {
        JavaPlugin shopPlugin = (JavaPlugin) JavaPlugin.getProvidingPlugin(ShopManager.class);
        openBuyGui(player, Material.SPAWNER, 1, skeletonPrice(), SpawnerManager.template(shopPlugin, EntityType.SKELETON));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (title.startsWith(BUY_TITLE)) {
            event.setCancelled(true);
            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
            PendingBuy pending = pendingBuys.get(player.getUniqueId());
            if (pending == null) {
                openShop(player);
                return;
            }
            switch (event.getSlot()) {
                case 10 -> {
                    pendingBuys.remove(player.getUniqueId());
                    ShopPage back = playerPages.get(player.getUniqueId());
                    if (back != null && back.category.equals("Custom")) {
                        openCustomShop(player);
                    } else if (back != null) {
                        openCategoryPage(player, back.category, back.materials, back.page);
                    } else {
                        openShop(player);
                    }
                }
                case 12 -> openBuyGui(player, pending.material,
                        pending.amount - (event.isShiftClick() ? 10 : 1),
                        pending.unitOverride, pending.product);
                case 14 -> openBuyGui(player, pending.material,
                        pending.amount + (event.isShiftClick() ? 10 : 1),
                        pending.unitOverride, pending.product);
                case 16 -> confirmBuy(player, pending);
                case 19 -> buyBulk(player, pending, 10);
                case 21 -> buyBulk(player, pending, 25);
                case 23 -> buyBulk(player, pending, 50);
                default -> {
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
                case 25 -> openMiscShop(player);
                case 31 -> openLatestShop(player);
                case 40 -> {
                    if (sellManager != null) sellManager.openSellGui(player);
                }
                case 49 -> openSearch(player);
                case 53 -> openCustomShop(player);
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

            if (title.equals("§6§lCustom") && event.getSlot() == 22) {
                openAutosellBuyGui(player);
                return;
            }

            if (title.equals("§6§lCustom") && event.getSlot() == 24) {
                openZombieBuyGui(player);
                return;
            }

            if (title.equals("§6§lCustom") && event.getSlot() == 25) {
                openSkeletonBuyGui(player);
                return;
            }

            Material material = clicked.getType();

            for (int slot : ITEM_SLOTS) {
                if (event.getSlot() == slot) {
                    if (material.getMaxStackSize() <= 1) {
                        // Unstackable (tools/weapons): keep instant buy-1
                        double buyPrice = priceManager.getBuyPrice(material);
                        // Bedrock/Geyser can fire one tap twice: ignore re-buys inside the window
                        long now = System.currentTimeMillis();
                        if (now - lastBuyTime.getOrDefault(player.getUniqueId(), 0L) < BUY_DEBOUNCE_MS) return;
                        lastBuyTime.put(player.getUniqueId(), now);
                        if (balanceManager.removeBalance(player, buyPrice)) {
                            player.getInventory().addItem(new ItemStack(material, 1));
                            player.sendMessage("§aBought 1x " + formatName(material) + " for $" + Money.format(buyPrice));
                        } else {
                            player.sendMessage("§cNot enough money! Need $" + Money.format(buyPrice));
                        }
                    } else {
                        openBuyGui(player, material, 1);
                    }
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        // Programmatic GUI switches (picker refresh) also fire close: skip those.
        if (switching.remove(player.getUniqueId())) {
            return;
        }
        // E / close (incl. Bedrock) = exit. Only the go-back arrow reopens.
        pendingBuys.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (isMainShop(title) || isCategoryShop(title) || title.startsWith(BUY_TITLE)) {
            event.setCancelled(true);
        }
    }

    private boolean isMainShop(String title) {
        return title.equals("§6§lShop");
    }

    private boolean isCategoryShop(String title) {
        // Category pages are created as "§6§l<Category> §7(Page X/Y)"
        return title.equals("§6§lCustom")
                || (title.startsWith("§6§l") && title.contains("§7(Page "));
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
