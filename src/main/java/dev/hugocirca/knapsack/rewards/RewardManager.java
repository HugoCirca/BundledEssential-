package dev.hugocirca.knapsack.rewards;

import dev.hugocirca.knapsack.economy.BalanceManager;
import dev.hugocirca.knapsack.economy.BountyManager;
import dev.hugocirca.knapsack.common.Saveable;
import dev.hugocirca.knapsack.util.JsonStorage;
import dev.hugocirca.knapsack.util.Money;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Cocoa;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Replaces Jobs: no join, no grind meters, no jackpot spam.
 * - /quest: repeatable task, progress via normal play, claiming rolls a new one instantly.
 * - /daily: manual daily streak claim (one missed day freezes it). Reward scales linearly so a year streak pays tons
 *   but never explodes: base + (streak-1)*perDay + random, plus weekly/monthly bonus.
 */
public class RewardManager implements Listener, CommandExecutor, Saveable {

    // CHOP stays completable for anyone holding one, but is out of the pool
    // for now (tree-feller plugins eat the break events).
    private static final QuestType[] QUEST_POOL = {
        QuestType.MINE_STONE, QuestType.MINE_ORE, QuestType.FARM, QuestType.HUNT,
        QuestType.FISH, QuestType.BREED, QuestType.TAME, QuestType.ENCHANT,
        QuestType.SMELT, QuestType.BREW, QuestType.EAT, QuestType.SHEAR,
        QuestType.SLEEP, QuestType.EXPLORE, QuestType.LEVELS
    };

    private enum QuestType {
        MINE_STONE(48, 96, 30.0, 50.0, "Mine %d stone/cobble/deepslate"),
        MINE_ORE(8, 16, 35.0, 55.0, "Mine %d ores"),
        CHOP(24, 48, 30.0, 50.0, "Chop %d logs"),
        FARM(24, 48, 30.0, 50.0, "Harvest %d ripe crops"),
        HUNT(10, 20, 45.0, 75.0, "Kill %d hostile mobs"),
        FISH(4, 8, 25.0, 40.0, "Catch %d fish"),
        BREED(3, 6, 40.0, 60.0, "Breed %d animals"),
        TAME(1, 2, 40.0, 60.0, "Tame %d animals"),
        ENCHANT(2, 4, 45.0, 65.0, "Enchant %d items"),
        SMELT(16, 32, 30.0, 50.0, "Smelt %d items"),
        BREW(3, 6, 40.0, 60.0, "Brew %d potions"),
        EAT(8, 12, 30.0, 45.0, "Eat %d food"),
        SHEAR(3, 6, 30.0, 45.0, "Shear %d sheep"),
        SLEEP(1, 1, 20.0, 30.0, "Sleep %d night(s)"),
        EXPLORE(1, 1, 25.0, 35.0, "Travel to %d dimension(s)"),
        LEVELS(3, 5, 40.0, 60.0, "Gain %d XP levels");

        final int minTarget;
        final int maxTarget;
        final double minReward;
        final double maxReward;
        final String desc;

        QuestType(int minTarget, int maxTarget, double minReward, double maxReward, String desc) {
            this.minTarget = minTarget;
            this.maxTarget = maxTarget;
            this.minReward = minReward;
            this.maxReward = maxReward;
            this.desc = desc;
        }
    }

    // Login defaults (overridable in config.yml under rewards.login).
    // Day1 ~$20-30, day30 ~$165-175 (+weekly bonuses), day365 ~$1840-1850.
    // Total for a full year of daily claims ~= $340k. Linear, no exponential blowup.
    private static final double DEF_LOGIN_BASE = 20.0;
    private static final double DEF_LOGIN_PER_DAY = 5.0;
    private static final double DEF_LOGIN_RANDOM = 10.0;
    private static final double DEF_BONUS_WEEKLY = 50.0;
    private static final double DEF_BONUS_MONTHLY = 200.0;
    private static final double DEF_DAILY_MULT = 1.0;

    // Anti-farm: player-placed blocks never count toward quests (covers shop-bought
    // ores, silk-touch recycle, place-and-break loops). Keyed by coords, LRU-capped at 100k (was 20k — too easy to evict).
    private static final int PLACED_CAP = 100000;
    private final LinkedHashMap<String, Boolean> placed = new LinkedHashMap<String, Boolean>(4096, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > PLACED_CAP;
        }
    };

    private static final long SAVE_INTERVAL_TICKS = 6000L;

    private static final Set<String> HOSTILE = new HashSet<>(Arrays.asList(
            "ZOMBIE", "HUSK", "DROWNED", "SKELETON", "STRAY", "BOGGED", "SPIDER", "CAVE_SPIDER",
            "CREEPER", "ENDERMAN", "WITCH", "BLAZE", "BREEZE", "GHAST", "MAGMA_CUBE", "SLIME",
            "PHANTOM", "PILLAGER", "VINDICATOR", "EVOKER", "RAVAGER", "VEX", "PIGLIN", "PIGLIN_BRUTE",
            "HOGLIN", "ZOGLIN", "WITHER_SKELETON", "ZOMBIFIED_PIGLIN", "GUARDIAN", "ELDER_GUARDIAN",
            "ENDERMITE", "SILVERFISH", "SHULKER", "WITHER", "ENDER_DRAGON", "WARDEN"));

    private static final Set<Material> ORES = new HashSet<>(Arrays.asList(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.NETHER_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS));

    private final JavaPlugin plugin;
    private final BalanceManager balanceManager;
    private BountyManager bountyManager;
    private final java.util.Random random = new java.util.Random();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final JsonObject data = new JsonObject();

    public RewardManager(JavaPlugin plugin, BalanceManager balanceManager, BountyManager bountyManager) {
        this.plugin = plugin;
        this.balanceManager = balanceManager;
        this.bountyManager = bountyManager;
        this.file = plugin.getDataFolder().toPath().resolve("rewards.json");
        load();
        startSaveTask();
    }

    public void setBountyManager(BountyManager bountyManager) {
        this.bountyManager = bountyManager;
    }

    /** Live config read so edits apply after restart (old configs missing keys fall back to defaults). */
    private double cfg(String path, double def) {
        try {
            return plugin.getConfig().getDouble(path, def);
        } catch (Exception e) {
            return def;
        }
    }

    // ---------- persistence ----------

    private JsonObject section(String name) {
        if (!data.has(name) || !data.get(name).isJsonObject()) {
            data.add(name, new JsonObject());
        }
        return data.getAsJsonObject(name);
    }

    private void load() {
        JsonObject loaded = JsonStorage.load(plugin, "rewards.json");
        if (loaded != null) loaded.entrySet().forEach(e -> data.add(e.getKey(), e.getValue()));
        if (data.has("placed") && data.get("placed").isJsonArray()) {
            for (JsonElement el : data.getAsJsonArray("placed")) {
                if (placed.size() >= PLACED_CAP) {
                    break;
                }
                try {
                    placed.put(el.getAsString(), Boolean.TRUE);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public void saveAll() {
        JsonArray arr = new JsonArray();
        for (String k : placed.keySet()) arr.add(k);
        data.add("placed", arr);
        JsonStorage.save(plugin, "rewards.json", data);
    }

    private void startSaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAll();
            }
        }.runTaskTimer(plugin, SAVE_INTERVAL_TICKS, SAVE_INTERVAL_TICKS);
    }

    private String today() {
        return LocalDate.now().toString();
    }

    // ---------- quests ----------

    private JsonObject quests() {
        return section("quests");
    }

    private JsonObject getQuest(UUID uuid) {
        JsonObject all = quests();
        String key = uuid.toString();
        if (!all.has(key) || !all.get(key).isJsonObject()) {
            return null;
        }
        return all.getAsJsonObject(key);
    }

    private JsonObject newQuest(Player player) {
        QuestType type = QUEST_POOL[random.nextInt(QUEST_POOL.length)];
        int target = type.minTarget + random.nextInt(type.maxTarget - type.minTarget + 1);
        double mult = cfg("rewards.daily.reward-multiplier", DEF_DAILY_MULT);
        if (mult <= 0) {
            mult = 1.0;
        }
        double reward = Math.round((type.minReward + random.nextDouble()
                * (type.maxReward - type.minReward)) * mult * 100.0) / 100.0;
        JsonObject fresh = new JsonObject();
        fresh.addProperty("type", type.name());
        fresh.addProperty("target", target);
        fresh.addProperty("progress", 0);
        fresh.addProperty("reward", reward);
        fresh.addProperty("claimed", false);
        quests().add(player.getUniqueId().toString(), fresh);
        saveAll();
        return fresh;
    }

    private JsonObject getOrCreateQuest(Player player) {
        JsonObject q = getQuest(player.getUniqueId());
        if (q != null) {
            if (q.has("claimed") && q.get("claimed").getAsBoolean()) {
                return newQuest(player); // legacy claimed quest: roll fresh
            }
            return q;
        }
        return newQuest(player);
    }

    private void addProgress(Player player, QuestType type, int amount) {
        JsonObject q = getQuest(player.getUniqueId());
        if (q == null) {
            return; // lazy: quest created on /quest, no tracking until then
        }
        if (!type.name().equals(q.get("type").getAsString())) {
            return;
        }
        if (q.get("claimed").getAsBoolean()) {
            return;
        }
        int progress = q.get("progress").getAsInt();
        int target = q.get("target").getAsInt();
        if (progress >= target) {
            return;
        }
        progress = Math.min(target, progress + amount);
        q.addProperty("progress", progress);
        if (progress >= target) {
            player.sendMessage("§6§lQUEST COMPLETE! §e" + describe(q) + " §7— claim with §e/quest claim");
        }
    }

    private String describe(JsonObject q) {
        QuestType type = QuestType.valueOf(q.get("type").getAsString());
        return String.format(type.desc, q.get("target").getAsInt());
    }

    // ---------- login streak ----------

    private JsonObject logins() {
        return section("logins");
    }

    private JsonObject loginEntry(UUID uuid) {
        JsonObject all = logins();
        String key = uuid.toString();
        if (!all.has(key) || !all.get(key).isJsonObject()) {
            JsonObject e = new JsonObject();
            e.addProperty("streak", 0);
            e.addProperty("last", "");
            all.add(key, e);
        }
        return all.getAsJsonObject(key);
    }

    private double loginReward(int streak) {
        double base = cfg("rewards.login.base", DEF_LOGIN_BASE);
        double perDay = cfg("rewards.login.per-day", DEF_LOGIN_PER_DAY);
        double randomMax = cfg("rewards.login.random-max", DEF_LOGIN_RANDOM);
        double weekly = cfg("rewards.login.weekly-bonus", DEF_BONUS_WEEKLY);
        double monthly = cfg("rewards.login.monthly-bonus", DEF_BONUS_MONTHLY);
        double reward = base + (streak - 1) * perDay + random.nextDouble() * randomMax;
        if (streak % 30 == 0) {
            reward += monthly;
        } else if (streak % 7 == 0) {
            reward += weekly;
        }
        return Math.round(reward * 100.0) / 100.0;
    }

    private void handleLogin(Player player) {
        JsonObject e = loginEntry(player.getUniqueId());
        String last = e.has("last") ? e.get("last").getAsString() : "";
        String now = today();
        if (now.equals(last)) {
            int streak = e.get("streak").getAsInt();
            double lastAmount = e.has("lastAmount") ? e.get("lastAmount").getAsDouble() : 0.0;
            player.sendMessage("§eAlready claimed today! §7(Day " + streak + ": +$"
                    + Money.format(lastAmount) + ") Come back tomorrow.");
            return;
        }
        String yesterday = LocalDate.now().minusDays(1).toString();
        String dayBefore = LocalDate.now().minusDays(2).toString();
        int streak;
        String frozen = "";
        if (yesterday.equals(last)) {
            streak = e.get("streak").getAsInt() + 1;
        } else if (dayBefore.equals(last)) {
            // One-day grace: streak frozen, not incremented, not reset.
            streak = e.get("streak").getAsInt();
            frozen = " §7(grace day used — streak frozen)";
        } else {
            streak = 1;
        }
        double reward = loginReward(streak);
        double kept = reward;
        if (bountyManager != null) {
            kept = bountyManager.garnish(player, reward);
        }
        balanceManager.addBalance(player, kept);
        e.addProperty("streak", streak);
        e.addProperty("last", now);
        e.addProperty("lastAmount", kept);
        saveAll();
        String bonus = "";
        double weekly = cfg("rewards.login.weekly-bonus", DEF_BONUS_WEEKLY);
        double monthly = cfg("rewards.login.monthly-bonus", DEF_BONUS_MONTHLY);
        if (streak % 30 == 0) {
            bonus = " §6§l+30-day bonus $" + Money.format(monthly) + "!";
        } else if (streak % 7 == 0) {
            bonus = " §6§l+7-day bonus $" + Money.format(weekly) + "!";
        }
        player.sendMessage("§a§l[Login] §eDay " + streak + " §a+$" + Money.format(kept) + bonus + frozen
                + " §7(claim /daily every day)");
        if (streak == 1 && !last.isEmpty() && !yesterday.equals(last) && !dayBefore.equals(last)) {
            player.sendMessage("§7Streak reset — you missed more than a day.");
        }
    }

    private void handleQuest(Player player, String[] args) {
        if (args.length >= 1 && (args[0].equalsIgnoreCase("skip") || args[0].equalsIgnoreCase("reroll"))) {
            JsonObject next = newQuest(player);
            player.sendMessage("§eQuest skipped! §6§lNew quest: §f" + describe(next)
                    + " §7— $" + Money.format(next.get("reward").getAsDouble()));
            return;
        }
        boolean claimOnly = args.length >= 1 && args[0].equalsIgnoreCase("claim");
        JsonObject q = getOrCreateQuest(player);
        int progress = q.get("progress").getAsInt();
        int target = q.get("target").getAsInt();
        double reward = q.get("reward").getAsDouble();
        boolean claimed = q.has("claimed") && q.get("claimed").getAsBoolean();

        if (claimOnly || (args.length == 0 && progress >= target && !claimed)) {
            if (progress < target) {
                player.sendMessage("§cNot done yet: §e" + describe(q) + " §7(" + progress + "/" + target + ")");
                return;
            }
            double kept = reward;
            if (bountyManager != null) {
                kept = bountyManager.garnish(player, reward);
            }
            balanceManager.addBalance(player, kept);
            player.sendMessage("§a§l[Quest] §e+$" + Money.format(kept) + " §7for: " + describe(q));
            JsonObject next = newQuest(player);
            player.sendMessage("§6§l[Quest] §fNew quest: " + describe(next)
                    + " §7— $" + Money.format(next.get("reward").getAsDouble()));
            return;
        }
        // status view (default)
        String state = claimed ? "§aclaimed ✓"
                : progress >= target ? "§6done — /quest claim"
                : "§e" + progress + "/" + target;
        player.sendMessage("§6§l[Quest] §f" + describe(q) + " §7— $" + Money.format(reward));
        player.sendMessage("§7Progress: " + state + " §7(new quest the moment you claim)");
    }

    // ---------- commands ----------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        String name = command.getName().toLowerCase();
        if (name.equals("daily") || name.equals("login")) {
            handleLogin(player);
        } else {
            handleQuest(player, args);
        }
        return true;
    }

    // ---------- events ----------

    private static boolean isLog(Material type) {
        String n = type.name();
        return n.endsWith("_LOG") || n.endsWith("_STEM") || n.endsWith("_WOOD") || n.endsWith("_HYPHAE");
    }

    private static boolean isPlainStone(Material type) {
        return type == Material.STONE || type == Material.COBBLESTONE
                || type == Material.DEEPSLATE || type == Material.COBBLED_DEEPSLATE;
    }

    private boolean isMature(Block block) {
        Material type = block.getType();
        if (type == Material.MELON || type == Material.PUMPKIN
                || type == Material.SUGAR_CANE || type == Material.CACTUS) {
            return true;
        }
        try {
            if (type == Material.COCOA && block.getBlockData() instanceof Cocoa cocoa) {
                return cocoa.getAge() >= cocoa.getMaximumAge();
            }
        } catch (Exception ignored) {
            return false;
        }
        if (type == Material.SWEET_BERRY_BUSH && block.getBlockData() instanceof Ageable berry) {
            return berry.getAge() >= 2;
        }
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }

    private static String key(Block block) {
        return block.getWorld().getUID() + "|" + block.getX() + "|" + block.getY() + "|" + block.getZ();
    }

    private static boolean isQuestPlaceBlock(Material type) {
        if (isPlainStone(type) || ORES.contains(type) || isLog(type)) {
            return true;
        }
        switch (type) {
            case MELON, PUMPKIN, SUGAR_CANE, CACTUS -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (isQuestPlaceBlock(event.getBlock().getType())) {
            placed.put(key(event.getBlock()), Boolean.TRUE);
        }
    }

    private final Set<String> countedBreaks = new HashSet<>();

    private static QuestType questTypeForBreak(Material type) {
        if (isPlainStone(type)) {
            return QuestType.MINE_STONE;
        }
        if (ORES.contains(type)) {
            return QuestType.MINE_ORE;
        }
        if (isLog(type)) {
            return QuestType.CHOP;
        }
        switch (type) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, COCOA,
                    SWEET_BERRY_BUSH, MELON, PUMPKIN, SUGAR_CANE, CACTUS -> {
                return QuestType.FARM;
            }
            default -> {
                return null;
            }
        }
    }

    private static String locKey(Location loc) {
        return loc.getWorld().getUID() + "|" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ();
    }

    /**
     * MONITOR + verify-by-outcome instead of trusting cancellation state.
     * Tree-fellers (SmoothTimber) cancel the original break and fell the tree
     * animated over ticks; protection plugins cancel with nothing breaking.
     * Counting only blocks that actually changed is correct for both cases.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        JsonObject q = getQuest(player.getUniqueId());
        if (q == null || (q.has("claimed") && q.get("claimed").getAsBoolean())) {
            return;
        }
        QuestType need;
        try {
            need = QuestType.valueOf(q.get("type").getAsString());
        } catch (Exception e) {
            return;
        }
        Block block = event.getBlock();
        Material type = block.getType();
        if (questTypeForBreak(type) != need) {
            return;
        }
        if (need == QuestType.FARM && !isMature(block)) {
            return;
        }
        if (placed.remove(key(block)) != null) {
            return; // player-placed: never counts (shop-bought, silk-touch, etc.)
        }
        Location loc = block.getLocation().clone();
        UUID pid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> verifyBreak(pid, need, loc, type, true), 1L);
    }

    private void verifyBreak(UUID pid, QuestType need, Location loc, Material before, boolean retry) {
        Player player = Bukkit.getPlayer(pid);
        if (player == null) {
            return;
        }
        Block after;
        try {
            after = loc.getBlock();
        } catch (Exception e) {
            return;
        }
        // Only count if block actually became air (not replaced with sand/other block within 1 tick)
        if (after.getType().isAir()) {
            String ck = locKey(loc);
            if (countedBreaks.add(ck)) {
                addProgress(player, need, 1);
                Bukkit.getScheduler().runTaskLater(plugin, () -> countedBreaks.remove(ck), 70L);
            }
            return;
        }
        if (after.getType() != before) {
            // Block changed to different solid (e.g., sand placed) — not a valid break, don't count
            return;
        }
        if (retry) {
            // Animated fellers break over ticks: one slow re-check catches those.
            Bukkit.getScheduler().runTaskLater(plugin, () -> verifyBreak(pid, need, loc, before, false), 60L);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (!(event.getCaught() instanceof Item item)) {
            return;
        }
        // Junk (bowls, sticks, leather...) and treasure don't count — only real fish.
        switch (item.getItemStack().getType()) {
            case COD, SALMON, TROPICAL_FISH, PUFFERFISH ->
                    addProgress(event.getPlayer(), QuestType.FISH, 1);
            default -> {
            }
        }
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            return;
        }
        if (!HOSTILE.contains(event.getEntityType().name())) {
            return;
        }
        // Team kills: killer plus everyone who hit it progress their HUNT quest.
        for (UUID id : balanceManager.contributors(event.getEntity(), killer.getUniqueId())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                addProgress(p, QuestType.HUNT, 1);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) {
            addProgress(player, QuestType.BREED, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (event.getOwner() instanceof Player player) {
            addProgress(player, QuestType.TAME, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        addProgress(event.getEnchanter(), QuestType.ENCHANT, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent event) {
        addProgress(event.getPlayer(), QuestType.SMELT, event.getItemAmount());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        int potions = 0;
        try {
            for (org.bukkit.inventory.ItemStack result : event.getResults()) {
                if (result != null && result.getType() != Material.AIR) {
                    potions += result.getAmount();
                }
            }
        } catch (Exception ignored) {}
        if (potions <= 0) {
            return;
        }
        final int brewed = potions;
        event.getContents().getViewers().stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .findFirst()
                .ifPresent(player -> addProgress(player, QuestType.BREW, brewed));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent event) {
        if (event.getItem().getType().isEdible()) {
            addProgress(event.getPlayer(), QuestType.EAT, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        addProgress(event.getPlayer(), QuestType.SHEAR, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSleep(PlayerBedEnterEvent event) {
        addProgress(event.getPlayer(), QuestType.SLEEP, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        addProgress(event.getPlayer(), QuestType.EXPLORE, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLevelGain(PlayerLevelChangeEvent event) {
        int gain = event.getNewLevel() - event.getOldLevel();
        if (gain > 0) {
            addProgress(event.getPlayer(), QuestType.LEVELS, gain);
        }
    }
}
