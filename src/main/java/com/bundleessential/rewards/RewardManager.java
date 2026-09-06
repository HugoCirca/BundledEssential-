package com.bundleessential.rewards;

import com.bundleessential.economy.BalanceManager;
import com.bundleessential.economy.BountyManager;
import com.bundleessential.util.Money;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Replaces Jobs: no join, no grind meters, no jackpot spam.
 * - /daily: one random quest per calendar day, progress via normal play, claim once.
 * - /login: daily streak claim. Reward scales linearly so a year streak pays tons
 *   but never explodes: base + (streak-1)*perDay + random, plus weekly/monthly bonus.
 */
public class RewardManager implements Listener, CommandExecutor {

    private enum QuestType {
        MINE_STONE(48, 96, 40.0, 70.0, "Mine %d stone/cobble/deepslate"),
        MINE_ORE(8, 16, 80.0, 120.0, "Mine %d ores"),
        CHOP(24, 48, 40.0, 70.0, "Chop %d logs"),
        FARM(24, 48, 40.0, 70.0, "Harvest %d ripe crops"),
        HUNT(10, 20, 60.0, 100.0, "Kill %d hostile mobs"),
        FISH(3, 8, 60.0, 100.0, "Catch %d fish");

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
        plugin.getDataFolder().mkdirs();
        try {
            if (Files.exists(file)) {
                JsonObject loaded = gson.fromJson(new String(Files.readAllBytes(file)), JsonObject.class);
                if (loaded != null) {
                    loaded.entrySet().forEach(e -> data.add(e.getKey(), e.getValue()));
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load rewards.json");
        }
    }

    public void saveAll() {
        try {
            Files.write(file, gson.toJson(data).getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save rewards.json");
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
        JsonObject q = all.getAsJsonObject(key);
        // New calendar day -> old quest expired, generate fresh on demand.
        if (!today().equals(q.has("date") ? q.get("date").getAsString() : "")) {
            return null;
        }
        return q;
    }

    private JsonObject getOrCreateQuest(Player player) {
        JsonObject q = getQuest(player.getUniqueId());
        if (q != null) {
            return q;
        }
        QuestType type = QuestType.values()[random.nextInt(QuestType.values().length)];
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
        fresh.addProperty("date", today());
        fresh.addProperty("claimed", false);
        quests().add(player.getUniqueId().toString(), fresh);
        return fresh;
    }

    private void addProgress(Player player, QuestType type, int amount) {
        JsonObject q = getQuest(player.getUniqueId());
        if (q == null) {
            return; // lazy: quest created on /daily, no tracking until then
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
            player.sendMessage("§6§lDAILY COMPLETE! §e" + describe(q) + " §7— claim with §e/daily claim");
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
            player.sendMessage("§eAlready claimed today! §7Streak: §e" + streak + " day(s)§7. Come back tomorrow.");
            return;
        }
        String yesterday = LocalDate.now().minusDays(1).toString();
        int streak = yesterday.equals(last) ? e.get("streak").getAsInt() + 1 : 1;
        double reward = loginReward(streak);
        double kept = reward;
        if (bountyManager != null) {
            kept = bountyManager.garnish(player, reward);
        }
        balanceManager.addBalance(player, kept);
        e.addProperty("streak", streak);
        e.addProperty("last", now);
        saveAll();
        String bonus = "";
        double weekly = cfg("rewards.login.weekly-bonus", DEF_BONUS_WEEKLY);
        double monthly = cfg("rewards.login.monthly-bonus", DEF_BONUS_MONTHLY);
        if (streak % 30 == 0) {
            bonus = " §6§l+30-day bonus $" + Money.format(monthly) + "!";
        } else if (streak % 7 == 0) {
            bonus = " §6§l+7-day bonus $" + Money.format(weekly) + "!";
        }
        player.sendMessage("§a§l[Login] §eDay " + streak + " §a+$" + Money.format(kept) + bonus
                + " §7(keep streak: claim every day)");
        if (streak == 1 && !last.isEmpty() && !yesterday.equals(last)) {
            player.sendMessage("§7Streak reset — you missed a day.");
        }
    }

    private void handleDaily(Player player, String[] args) {
        boolean claimOnly = args.length >= 1 && args[0].equalsIgnoreCase("claim");
        JsonObject q = getOrCreateQuest(player);
        int progress = q.get("progress").getAsInt();
        int target = q.get("target").getAsInt();
        double reward = q.get("reward").getAsDouble();
        boolean claimed = q.get("claimed").getAsBoolean();

        if (claimOnly || (args.length == 0 && progress >= target && !claimed)) {
            if (claimed) {
                player.sendMessage("§eAlready claimed today's quest. §7New quest tomorrow.");
                return;
            }
            if (progress < target) {
                player.sendMessage("§cNot done yet: §e" + describe(q) + " §7(" + progress + "/" + target + ")");
                return;
            }
            double kept = reward;
            if (bountyManager != null) {
                kept = bountyManager.garnish(player, reward);
            }
            balanceManager.addBalance(player, kept);
            q.addProperty("claimed", true);
            saveAll();
            player.sendMessage("§a§l[Daily] §e+$" + Money.format(kept) + " §7for: " + describe(q));
            return;
        }
        // status view (default)
        String state = claimed ? "§aclaimed ✓"
                : progress >= target ? "§6done — /daily claim"
                : "§e" + progress + "/" + target;
        player.sendMessage("§6§l[Daily] §f" + describe(q) + " §7— $" + Money.format(reward));
        player.sendMessage("§7Progress: " + state + " §7(resets daily)");
    }

    // ---------- commands ----------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        String name = command.getName().toLowerCase();
        if (name.equals("login")) {
            handleLogin(player);
        } else {
            handleDaily(player, args);
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

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (getQuest(player.getUniqueId()) == null) {
            return;
        }
        Material type = event.getBlock().getType();
        if (isPlainStone(type)) {
            addProgress(player, QuestType.MINE_STONE, 1);
        } else if (ORES.contains(type)) {
            addProgress(player, QuestType.MINE_ORE, 1);
        } else if (isLog(type)) {
            addProgress(player, QuestType.CHOP, 1);
        } else {
            switch (type) {
                case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, COCOA,
                        SWEET_BERRY_BUSH, MELON, PUMPKIN, SUGAR_CANE, CACTUS -> {
                    if (isMature(event.getBlock())) {
                        addProgress(player, QuestType.FARM, 1);
                    }
                }
                default -> {
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (!(event.getCaught() instanceof Item)) {
            return;
        }
        addProgress(event.getPlayer(), QuestType.FISH, 1);
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
        if (HOSTILE.contains(event.getEntityType().name())) {
            addProgress(killer, QuestType.HUNT, 1);
        }
    }
}
