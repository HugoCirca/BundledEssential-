package dev.hugocirca.knapsack.autosell;

import dev.hugocirca.knapsack.economy.BalanceManager;
import dev.hugocirca.knapsack.economy.PriceManager;
import dev.hugocirca.knapsack.common.Saveable;
import dev.hugocirca.knapsack.util.JsonStorage;
import dev.hugocirca.knapsack.util.Money;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Auto-sell chest (first Custom-category item).
 * Buy it in /shop, right-click air holding it to set the sell interval and the
 * players to pay (equal split, online or offline), then place it. Everything put
 * inside — by hand or by hopper — is sold automatically every interval.
 * Sneak + right-click a placed one to reconfigure it.
 */
public class AutoSellManager implements Listener, CommandExecutor, Saveable {

    public static final String GUI_TITLE = "§6Auto-Sell Chest";
    private static final long SAVE_INTERVAL_TICKS = 6000L;
    private static final double DEF_PRICE = 500.0;
    private static final int DEF_INTERVAL = 60;
    private static final int DEF_MAX_RECIPIENTS = 5;
    private static final List<Integer> DEF_INTERVALS = Arrays.asList(30, 60, 300, 600);
    private static final int[] HEAD_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private final JavaPlugin plugin;
    private final BalanceManager balance;
    private final PriceManager prices;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final JsonObject data = new JsonObject();
    private final Map<String, UUID> byLoc = new HashMap<>();
    private final Map<UUID, UUID> openConfigs = new HashMap<>();
    private final Map<UUID, Map<Integer, UUID>> openHeads = new HashMap<>();
    private final Set<UUID> guiSwitching = new HashSet<>();

    public AutoSellManager(JavaPlugin plugin, BalanceManager balance, PriceManager prices) {
        this.plugin = plugin;
        this.balance = balance;
        this.prices = prices;
        this.file = plugin.getDataFolder().toPath().resolve("autosell.json");
        loadAll();
        startSaveTask();
        startSellTask();
    }

    // ---------- static item helpers ----------

    public static NamespacedKey tagKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, "autosell");
    }

    public static NamespacedKey idKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, "autosell-id");
    }

    /** Fresh shop template: marker tag, no chest id yet (assigned on configure/place). */
    public static ItemStack template(JavaPlugin plugin) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§lAuto-Sell Chest");
        List<String> lore = new ArrayList<>();
        lore.add("§7Sells its contents automatically");
        lore.add("§7Right-click air to configure");
        lore.add("§7Place it, feed it (hoppers work)");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(tagKey(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isChestItem(JavaPlugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.CHEST || !item.hasItemMeta()) {
            return false;
        }
        try {
            return item.getItemMeta().getPersistentDataContainer()
                    .has(tagKey(plugin), PersistentDataType.BYTE);
        } catch (Exception e) {
            return false;
        }
    }

    public static UUID idOf(JavaPlugin plugin, ItemStack item) {
        try {
            String s = item.getItemMeta().getPersistentDataContainer()
                    .get(idKey(plugin), PersistentDataType.STRING);
            return s == null ? null : UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    /** Break/place return: same chest id so its config survives. */
    public static ItemStack tagged(JavaPlugin plugin, UUID chestId) {
        ItemStack item = template(plugin);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer()
                .set(idKey(plugin), PersistentDataType.STRING, chestId.toString());
        item.setItemMeta(meta);
        return item;
    }

    // ---------- config ----------

    private double cfg(String path, double def) {
        try {
            return plugin.getConfig().getDouble(path, def);
        } catch (Exception e) {
            return def;
        }
    }

    public double chestPrice() {
        double p = cfg("autosell.price", DEF_PRICE);
        return p > 0 ? Math.round(p * 100.0) / 100.0 : DEF_PRICE;
    }

    private int defaultInterval() {
        try {
            return Math.max(5, plugin.getConfig().getInt("autosell.default-interval-seconds", DEF_INTERVAL));
        } catch (Exception e) {
            return DEF_INTERVAL;
        }
    }

    private List<Integer> intervalOptions() {
        try {
            List<Integer> raw = plugin.getConfig().getIntegerList("autosell.intervals");
            List<Integer> out = new ArrayList<>();
            for (int i : raw) {
                if (i >= 5 && !out.contains(i)) {
                    out.add(i);
                }
            }
            if (!out.isEmpty()) {
                return out;
            }
        } catch (Exception ignored) {}
        return new ArrayList<>(DEF_INTERVALS);
    }

    private int maxRecipients() {
        try {
            return Math.max(1, plugin.getConfig().getInt("autosell.max-recipients", DEF_MAX_RECIPIENTS));
        } catch (Exception e) {
            return DEF_MAX_RECIPIENTS;
        }
    }

    // ---------- data ----------

    private JsonObject chests() {
        if (!data.has("chests") || !data.get("chests").isJsonObject()) {
            data.add("chests", new JsonObject());
        }
        return data.getAsJsonObject("chests");
    }

    private JsonObject pending() {
        if (!data.has("pending") || !data.get("pending").isJsonObject()) {
            data.add("pending", new JsonObject());
        }
        return data.getAsJsonObject("pending");
    }

    private void queueOffline(UUID recipient, String ownerName, double pay, int count) {
        JsonObject all = pending();
        String key = recipient.toString();
        JsonArray arr;
        if (all.has(key) && all.get(key).isJsonArray()) {
            arr = all.getAsJsonArray(key);
        } else {
            arr = new JsonArray();
            all.add(key, arr);
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("owner", ownerName);
        entry.addProperty("pay", pay);
        entry.addProperty("count", count);
        entry.addProperty("time", System.currentTimeMillis());
        arr.add(entry);
    }

    private static String locKey(Block block) {
        return block.getWorld().getUID() + "|" + block.getX() + "|" + block.getY() + "|" + block.getZ();
    }

    private JsonObject ensureEntry(UUID chestId, UUID fallbackOwner) {
        JsonObject all = chests();
        String key = chestId.toString();
        if (!all.has(key) || !all.get(key).isJsonObject()) {
            JsonObject e = new JsonObject();
            e.addProperty("interval", defaultInterval());
            e.addProperty("owner", fallbackOwner.toString());
            JsonArray rec = new JsonArray();
            rec.add(fallbackOwner.toString());
            e.add("recipients", rec);
            e.addProperty("lastSell", 0L);
            all.add(key, e);
        }
        return all.getAsJsonObject(key);
    }

    private List<UUID> recipientList(JsonObject e) {
        List<UUID> out = new ArrayList<>();
        if (e.has("recipients") && e.get("recipients").isJsonArray()) {
            for (JsonElement el : e.getAsJsonArray("recipients")) {
                try {
                    UUID id = UUID.fromString(el.getAsString());
                    if (!out.contains(id)) {
                        out.add(id);
                    }
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    private void writeRecipients(JsonObject e, List<UUID> recipients) {
        JsonArray arr = new JsonArray();
        for (UUID id : recipients) {
            arr.add(id.toString());
        }
        e.add("recipients", arr);
    }

    private void removeChest(UUID chestId) {
        chests().remove(chestId.toString());
        byLoc.entrySet().removeIf(en -> en.getValue().equals(chestId));
    }

    private UUID chestAt(Block block) {
        return byLoc.get(locKey(block));
    }

    private void unlocate(Block block) {
        byLoc.remove(locKey(block));
    }

    private void locate(Block block, UUID chestId) {
        byLoc.entrySet().removeIf(en -> en.getValue().equals(chestId));
        byLoc.put(locKey(block), chestId);
        JsonObject e = ensureEntry(chestId, chestId);
        e.addProperty("world", block.getWorld().getUID().toString());
        e.addProperty("x", block.getX());
        e.addProperty("y", block.getY());
        e.addProperty("z", block.getZ());
    }

    private static String nameOf(UUID id) {
        try {
            String name = Bukkit.getOfflinePlayer(id).getName();
            return name == null ? id.toString().substring(0, 8) : name;
        } catch (Exception e) {
            return id.toString().substring(0, 8);
        }
    }

    private static String formatSecs(int secs) {
        if (secs >= 60 && secs % 60 == 0) {
            return (secs / 60) + "m";
        }
        return secs + "s";
    }

    @Override
    public void saveAll() {
        JsonStorage.save(plugin, "autosell.json", data);
    }

    private void loadAll() {
        JsonObject loaded = JsonStorage.load(plugin, "autosell.json");
        if (loaded != null) {
            loaded.entrySet().forEach(e -> data.add(e.getKey(), e.getValue()));
        }
        byLoc.clear();
        for (Map.Entry<String, JsonElement> en : chests().entrySet()) {
            try {
                UUID chestId = UUID.fromString(en.getKey());
                JsonObject e = en.getValue().getAsJsonObject();
                String loc = e.get("world").getAsString() + "|"
                        + e.get("x").getAsInt() + "|" + e.get("y").getAsInt() + "|" + e.get("z").getAsInt();
                byLoc.put(loc, chestId);
            } catch (Exception ignored) {}
        }
    }

    private void startSaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAll();
            }
        }.runTaskTimer(plugin, SAVE_INTERVAL_TICKS, SAVE_INTERVAL_TICKS);
    }

    // ---------- commands ----------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
            if (!sender.isOp() && !sender.hasPermission("knapsack.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /autosell give <player> [amount]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found or offline!");
                return true;
            }
            int amount = 1;
            if (args.length >= 3) {
                try {
                    amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cAmount must be a number!");
                    return true;
                }
            }
            ItemStack give = template(plugin);
            give.setAmount(amount);
            target.getInventory().addItem(give);
            sender.sendMessage("§aGave " + amount + "x Auto-Sell Chest to §e" + target.getName());
            target.sendMessage("§aYou received " + amount + "x §6§lAuto-Sell Chest§a! Right-click air to configure it.");
            return true;
        }
        sender.sendMessage("§6§lAuto-Sell Chest §7— no command needed! Right-click air holding one to set it up.");
        sender.sendMessage("§7Buy it in §e/shop §7(Custom tab). Place it, feed it by hand or hopper.");
        sender.sendMessage("§7Place it, feed it by hand or hopper — contents sell every interval.");
        sender.sendMessage("§7Right-click air holding it to set interval + recipients (equal split).");
        sender.sendMessage("§7Sneak + right-click a placed one to reconfigure it.");
        sender.sendMessage("§7Price: §e$" + Money.format(chestPrice()));
        return true;
    }

    // ---------- place / break / explode ----------

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack hand = event.getItemInHand();
        if (!isChestItem(plugin, hand)) {
            return;
        }
        UUID chestId = idOf(plugin, hand);
        if (chestId == null) {
            chestId = UUID.randomUUID();
        }
        ensureEntry(chestId, event.getPlayer().getUniqueId());
        locate(event.getBlockPlaced(), chestId);
        JsonObject e = chests().getAsJsonObject(chestId.toString());
        e.addProperty("lastSell", System.currentTimeMillis());
        saveAll();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        UUID chestId = chestAt(event.getBlock());
        if (chestId == null) {
            return;
        }
        Block block = event.getBlock();
        unlocate(block);
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        event.setDropItems(false);
        BlockState state = block.getState();
        if (state instanceof Chest chest) {
            for (ItemStack content : chest.getBlockInventory().getContents()) {
                if (content != null && content.getType() != Material.AIR) {
                    block.getWorld().dropItemNaturally(
                            block.getLocation().add(0.5, 0.5, 0.5), content.clone());
                }
            }
        }
        block.getWorld().dropItemNaturally(
                block.getLocation().add(0.5, 0.5, 0.5), tagged(plugin, chestId));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : new ArrayList<>(event.blockList())) {
            if (chestAt(block) != null) {
                unlocate(block);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : new ArrayList<>(event.blockList())) {
            if (chestAt(block) != null) {
                unlocate(block);
            }
        }
    }

    // ---------- configure: right-click air (held) / sneak-right-click (placed) ----------

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (!isChestItem(plugin, held)) {
                held = player.getInventory().getItemInOffHand();
                if (!isChestItem(plugin, held)) {
                    return;
                }
            }
            if (held.getAmount() > 1) {
                player.sendMessage("§cSplit the stack first — configure one chest at a time.");
                return;
            }
            UUID chestId = idOf(plugin, held);
            if (chestId == null) {
                chestId = UUID.randomUUID();
                ItemMeta meta = held.getItemMeta();
                meta.getPersistentDataContainer()
                        .set(idKey(plugin), PersistentDataType.STRING, chestId.toString());
                held.setItemMeta(meta);
            }
            ensureEntry(chestId, player.getUniqueId());
            openConfig(player, chestId);
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && player.isSneaking()) {
            Block clicked = event.getClickedBlock();
            if (clicked == null) {
                return;
            }
            UUID chestId = chestAt(clicked);
            if (chestId == null) {
                return;
            }
            event.setCancelled(true);
            ensureEntry(chestId, player.getUniqueId());
            openConfig(player, chestId);
        }
    }

    private ItemStack makeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String l : lore) {
                loreList.add(l);
            }
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void openConfig(Player player, UUID chestId) {
        JsonObject e = ensureEntry(chestId, player.getUniqueId());
        int interval = e.has("interval") ? e.get("interval").getAsInt() : defaultInterval();
        List<UUID> recipients = recipientList(e);

        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        List<Integer> options = intervalOptions();
        int next = options.get((options.indexOf(interval) + 1) % options.size());
        inv.setItem(4, makeItem(Material.CLOCK, "§eInterval: §f" + formatSecs(interval),
                "§7Click for next (" + formatSecs(next) + ")"));
        inv.setItem(49, makeItem(Material.CHEST, "§6Auto-Sell Chest",
                "§7Sells contents every " + formatSecs(interval),
                "§7Split equally: §f" + recipients.size() + " player(s)"));
        inv.setItem(50, makeItem(Material.NAME_TAG, "§eAdd offline player",
                "§7Type a name that played here"));

        Set<UUID> seen = new LinkedHashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            seen.add(p.getUniqueId());
        }
        seen.addAll(recipients);
        Map<Integer, UUID> slotMap = new HashMap<>();
        int i = 0;
        for (UUID id : seen) {
            if (i >= HEAD_SLOTS.length) {
                break;
            }
            boolean sel = recipients.contains(id);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            try {
                sm.setOwningPlayer(Bukkit.getOfflinePlayer(id));
            } catch (Exception ignored) {}
            sm.setDisplayName((sel ? "§a" : "§7") + nameOf(id));
            List<String> lore = new ArrayList<>();
            if (sel) {
                lore.add("§a✓ Receives an equal share");
                lore.add("§7Click to remove");
            } else {
                lore.add("§7Click to add to the split");
            }
            sm.setLore(lore);
            head.setItemMeta(sm);
            inv.setItem(HEAD_SLOTS[i], head);
            slotMap.put(HEAD_SLOTS[i], id);
            i++;
        }

        openConfigs.put(player.getUniqueId(), chestId);
        openHeads.put(player.getUniqueId(), slotMap);
        guiSwitching.add(player.getUniqueId());
        player.openInventory(inv);
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        UUID chestId = openConfigs.get(player.getUniqueId());
        if (chestId == null) {
            player.closeInventory();
            return;
        }
        JsonObject e = ensureEntry(chestId, player.getUniqueId());
        int slot = event.getSlot();
        if (slot == 4) {
            List<Integer> options = intervalOptions();
            int cur = e.has("interval") ? e.get("interval").getAsInt() : defaultInterval();
            e.addProperty("interval", options.get((options.indexOf(cur) + 1) % options.size()));
            saveAll();
            openConfig(player, chestId);
            return;
        }
        if (slot == 50) {
            openAddPlayer(player, chestId);
            return;
        }
        Map<Integer, UUID> heads = openHeads.get(player.getUniqueId());
        UUID target = heads == null ? null : heads.get(slot);
        if (target == null) {
            return;
        }
        List<UUID> recipients = recipientList(e);
        if (recipients.contains(target)) {
            recipients.remove(target);
            player.sendMessage("§7Removed §f" + nameOf(target) + " §7from the split.");
        } else {
            if (recipients.size() >= maxRecipients()) {
                player.sendMessage("§cSplit is full (max " + maxRecipients() + ").");
                return;
            }
            recipients.add(target);
            player.sendMessage("§aAdded §f" + nameOf(target) + " §ato the split.");
        }
        writeRecipients(e, recipients);
        saveAll();
        openConfig(player, chestId);
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }
        if (guiSwitching.remove(player.getUniqueId())) {
            return;
        }
        openConfigs.remove(player.getUniqueId());
        openHeads.remove(player.getUniqueId());
        saveAll();
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        JsonObject all = pending();
        String key = player.getUniqueId().toString();
        if (!all.has(key) || !all.get(key).isJsonArray()) {
            return;
        }
        JsonArray arr = all.getAsJsonArray(key);
        if (arr.size() == 0) {
            return;
        }
        // Delay 1s so join messages don't drown it
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            double total = 0;
            int sales = 0;
            List<String> owners = new ArrayList<>();
            for (JsonElement el : arr) {
                try {
                    JsonObject o = el.getAsJsonObject();
                    total += o.get("pay").getAsDouble();
                    sales++;
                    String owner = o.get("owner").getAsString();
                    if (!owners.contains(owner)) owners.add(owner);
                } catch (Exception ignored) {}
            }
            if (sales > 0) {
                player.sendMessage("§a[AutoSell] §eYou received §a$" + Money.format(total) + " §7from " + String.join(", ", owners) + "'s chest(s) §7while offline §7(" + sales + " sale(s))");
            }
            all.remove(key);
            saveAll();
        }, 20L);
    }

    private void openAddPlayer(Player player, UUID chestId) {
        guiSwitching.add(player.getUniqueId());
        try {
            new AnvilGUI.Builder()
                    .onClose(state -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            openConfig(player, chestId);
                        }
                    }))
                    .onClick((slot, state) -> {
                        if (slot != AnvilGUI.Slot.OUTPUT) {
                            return Collections.emptyList();
                        }
                        String name = state.getText() == null ? "" : state.getText().trim();
                        if (name.isEmpty()) {
                            return Collections.singletonList(AnvilGUI.ResponseAction.close());
                        }
                        // Allow any name (including Geyser/Floodgate Bedrock players like ".Steve").
                        // hasPlayedBefore is unreliable for Bedrock accounts, so we accept even
                        // never-seen names and resolve their UUID via OfflinePlayer.
                        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
                        UUID target = op.getUniqueId();
                        Bukkit.getScheduler().runTask(plugin, () -> addRecipient(player, chestId, target));
                        return Collections.singletonList(AnvilGUI.ResponseAction.close());
                    })
                    .text("")
                    .title("Add player")
                    .plugin(plugin)
                    .open(player);
        } catch (Exception ex) {
            plugin.getLogger().warning("Auto-sell add-player unavailable: " + ex.getMessage());
            player.sendMessage("§cAdding offline players is unavailable on this server version.");
        }
    }

    private void addRecipient(Player player, UUID chestId, UUID target) {
        JsonObject e = ensureEntry(chestId, player.getUniqueId());
        List<UUID> recipients = recipientList(e);
        if (recipients.contains(target)) {
            player.sendMessage("§e" + nameOf(target) + " §7is already in the split.");
            return;
        }
        if (recipients.size() >= maxRecipients()) {
            player.sendMessage("§cSplit is full (max " + maxRecipients() + ").");
            return;
        }
        recipients.add(target);
        writeRecipients(e, recipients);
        saveAll();
        player.sendMessage("§aAdded §f" + nameOf(target) + " §ato the split.");
    }

    // ---------- sell task ----------

    private void startSellTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                tickSell();
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    private void tickSell() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, JsonElement> en : new ArrayList<>(chests().entrySet())) {
            UUID chestId;
            try {
                chestId = UUID.fromString(en.getKey());
            } catch (Exception e) {
                continue;
            }
            JsonObject e = en.getValue().getAsJsonObject();
            int interval = e.has("interval") ? e.get("interval").getAsInt() : defaultInterval();
            long last = e.has("lastSell") ? e.get("lastSell").getAsLong() : 0L;
            if (now - last < interval * 1000L) {
                continue;
            }
            World world;
            try {
                world = Bukkit.getWorld(UUID.fromString(e.get("world").getAsString()));
            } catch (Exception ex) {
                continue;
            }
            if (world == null) {
                continue;
            }
            int x = e.get("x").getAsInt();
            int y = e.get("y").getAsInt();
            int z = e.get("z").getAsInt();
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.AIR) {
                removeChest(chestId);
                continue;
            }
            BlockState state = block.getState();
            if (!(state instanceof Chest chest)) {
                continue;
            }
            List<UUID> recipients = recipientList(e);
            if (recipients.isEmpty()) {
                try {
                    recipients.add(UUID.fromString(e.get("owner").getAsString()));
                } catch (Exception ignored) {}
            }
            e.addProperty("lastSell", now);
            if (recipients.isEmpty()) {
                continue;
            }
            Inventory inv = chest.getInventory();
            double total = 0.0;
            int count = 0;
            List<Integer> toClear = new ArrayList<>();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                if (isChestItem(plugin, item)) {
                    continue;
                }
                total += Math.round(prices.getSellPriceWithEnchants(item) * item.getAmount() * 100.0) / 100.0;
                count += item.getAmount();
                toClear.add(i);
            }
            if (count == 0 || total <= 0) {
                continue;
            }
            total = Math.round(total * 100.0) / 100.0;
            for (int slot : toClear) {
                inv.setItem(slot, null);
            }
            int n = recipients.size();
            double share = Math.floor(total / n * 100.0) / 100.0;
            double remainder = Math.round((total - share * n) * 100.0) / 100.0;
            String ownerName;
            try {
                ownerName = nameOf(UUID.fromString(e.get("owner").getAsString()));
            } catch (Exception ex) {
                ownerName = "someone";
            }
            for (int i = 0; i < n; i++) {
                double pay = share + (i == 0 ? remainder : 0);
                if (pay <= 0) {
                    continue;
                }
                UUID rid = recipients.get(i);
                balance.addBalance(rid, pay);
                Player p = Bukkit.getPlayer(rid);
                if (p != null) {
                    p.sendMessage("§a[AutoSell] §e+$" + Money.format(pay) + " §7from " + ownerName + "'s chest §7(sold " + count + " items)");
                } else {
                    queueOffline(rid, ownerName, pay, count);
                }
            }
        }
    }
}
