package com.bundleessential.jobs;

import com.bundleessential.economy.BalanceManager;
import com.bundleessential.util.Money;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Jobs system: pick one job at a time with /jobs join, get paid per action.
 * Player-placed ores/logs never pay (no place-and-break farming).
 * Stone/cobble pay nothing (no cobble-generator AFK farms).
 */
public class JobsManager implements Listener, CommandExecutor {

    public static final List<String> JOBS = Arrays.asList("miner", "woodcutter", "farmer", "fisher", "hunter");
    private static final int PLACED_CAP = 20000;
    private static final long SAVE_INTERVAL_TICKS = 6000L;
    private static final double WOODCUTTER_PAY = 0.50;
    // Stone meter: every STONE_MIN-MAX stone mined hits a payout of $STONE_PAY_MIN-MAX
    private static final int STONE_MIN = 64;
    private static final int STONE_MAX = 128;
    private static final double STONE_PAY_MIN = 4.00;
    private static final double STONE_PAY_MAX = 10.00;

    private final JavaPlugin plugin;
    private final java.util.Random random = new java.util.Random();
    private final BalanceManager balanceManager;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path jobsFile;
    private final Path placedFile;
    private final JsonObject jobs = new JsonObject();
    private final LinkedHashMap<String, Boolean> placed = new LinkedHashMap<String, Boolean>(1024, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > PLACED_CAP;
        }
    };

    private final Map<Material, Double> minerPay = new HashMap<>();
    private final Map<Material, Double> cavePay = new HashMap<>();
    private final Map<Material, Double> farmerPay = new HashMap<>();
    private final Map<Material, Double> fisherPay = new HashMap<>();
    private final Map<String, Double> hunterPay = new HashMap<>();

    public JobsManager(JavaPlugin plugin, BalanceManager balanceManager) {
        this.plugin = plugin;
        this.balanceManager = balanceManager;
        this.jobsFile = plugin.getDataFolder().toPath().resolve("jobs.json");
        this.placedFile = plugin.getDataFolder().toPath().resolve("jobs_placed.json");
        initTables();
        loadAll();
        startSaveTask();
    }

    // ---------- data ----------

    private JsonObject entry(UUID uuid) {
        String key = uuid.toString();
        if (!jobs.has(key)) {
            JsonObject e = new JsonObject();
            e.addProperty("job", "");
            e.add("earned", new JsonObject());
            jobs.add(key, e);
        }
        return jobs.getAsJsonObject(key);
    }

    public String getJob(UUID uuid) {
        String job = entry(uuid).get("job").getAsString();
        return job.isEmpty() ? null : job;
    }

    public double getEarned(UUID uuid, String job) {
        JsonObject earned = entry(uuid).getAsJsonObject("earned");
        if (!earned.has(job)) return 0.0;
        return Math.round(earned.get(job).getAsDouble() * 100.0) / 100.0;
    }

    private void pay(Player player, String job, double amount) {
        if (amount <= 0) return;
        balanceManager.addBalance(player, amount);
        JsonObject earned = entry(player.getUniqueId()).getAsJsonObject("earned");
        double total = earned.has(job) ? earned.get(job).getAsDouble() : 0.0;
        earned.addProperty(job, Math.round((total + amount) * 100.0) / 100.0);
    }

    private static String key(Block block) {
        return block.getWorld().getUID() + "|" + block.getX() + "|" + block.getY() + "|" + block.getZ();
    }

    private static boolean isLog(Material type) {
        String n = type.name();
        return n.endsWith("_LOG") || n.endsWith("_STEM") || n.endsWith("_WOOD") || n.endsWith("_HYPHAE");
    }

    private static boolean isPlainStone(Material type) {
        return type == Material.STONE || type == Material.COBBLESTONE
                || type == Material.DEEPSLATE || type == Material.COBBLED_DEEPSLATE;
    }

    private int randomStoneTarget() {
        return STONE_MIN + random.nextInt(STONE_MAX - STONE_MIN + 1);
    }

    private void stoneProgress(Player player) {
        JsonObject e = entry(player.getUniqueId());
        int count = (e.has("stoneCount") ? e.get("stoneCount").getAsInt() : 0) + 1;
        int target = e.has("stoneTarget") ? e.get("stoneTarget").getAsInt() : 0;
        if (target <= 0) target = randomStoneTarget();
        if (count >= target) {
            double payout = Math.round((STONE_PAY_MIN + random.nextDouble() * (STONE_PAY_MAX - STONE_PAY_MIN)) * 100.0) / 100.0;
            pay(player, "miner", payout);
            player.sendMessage("§6§lLUCKY BREAK! §e+$" + Money.format(payout) + " §7for mining " + target + " stone.");
            count = 0;
            target = randomStoneTarget();
        }
        e.addProperty("stoneCount", count);
        e.addProperty("stoneTarget", target);
    }

    // ---------- commands (/jobs list|join|leave|info) ----------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        if (args.length == 0) {
            sendInfo(player, player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list" -> sendList(player);
            case "join" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /jobs join <miner|woodcutter|farmer|fisher|hunter>");
                } else {
                    joinJob(player, args[1].toLowerCase());
                }
            }
            case "leave" -> leaveJob(player);
            case "info" -> {
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) player.sendMessage("§cPlayer not found or offline!");
                    else sendInfo(player, target);
                } else {
                    sendInfo(player, player);
                }
            }
            default -> player.sendMessage("§cUsage: /jobs <list|join|leave|info>");
        }
        return true;
    }

    private void sendList(Player player) {
        player.sendMessage("§6§lJobs §7— pick one with §e/jobs join <name>");
        player.sendMessage("§eMiner §7- ores (diamond $12, gold $4...) + cave blocks + stone meter ($4-10 breaks)");
        player.sendMessage("§eWoodcutter §7- logs $0.50 each");
        player.sendMessage("§eFarmer §7- ripe crops (wheat $1, melon/pumpkin $1.50...)");
        player.sendMessage("§eFisher §7- catches (fish $1-2, treasure up to $10)");
        player.sendMessage("§eHunter §7- hostile mobs (zombie $1, enderman $3, dragon $500...)");
    }

    private void sendInfo(Player viewer, Player target) {
        String job = getJob(target.getUniqueId());
        viewer.sendMessage("§6" + target.getName() + "'s job: " + (job == null ? "§7none §7(/jobs join)" : "§e" + job));
        StringBuilder sb = new StringBuilder("§7Earned: ");
        boolean first = true;
        for (String j : JOBS) {
            if (!first) sb.append("§7, ");
            first = false;
            sb.append("§e").append(j).append(" $").append(String.format("%.2f", getEarned(target.getUniqueId(), j)));
        }
        viewer.sendMessage(sb.toString());
    }

    private void joinJob(Player player, String job) {
        if (!JOBS.contains(job)) {
            player.sendMessage("§cUnknown job! Choose: §eminer, woodcutter, farmer, fisher, hunter");
            return;
        }
        entry(player.getUniqueId()).addProperty("job", job);
        saveAll();
        player.sendMessage("§aYou joined the §e" + job + " §ajob! Earnings stack with other rewards.");
    }

    private void leaveJob(Player player) {
        if (getJob(player.getUniqueId()) == null) {
            player.sendMessage("§cYou don't have a job. See §e/jobs list");
            return;
        }
        entry(player.getUniqueId()).addProperty("job", "");
        saveAll();
        player.sendMessage("§eYou left your job. Your earnings are kept.");
    }

    // ---------- events ----------

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Material type = event.getBlock().getType();
        if (minerPay.containsKey(type) || cavePay.containsKey(type) || isLog(type) || isPlainStone(type)) {
            placed.put(key(event.getBlock()), Boolean.TRUE);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String job = getJob(player.getUniqueId());
        if (job == null) return;
        Material type = event.getBlock().getType();
        String k = key(event.getBlock());

        if (job.equals("miner")) {
            Double amount = minerPay.get(type);
            if (amount != null) {
                if (placed.remove(k) != null) return; // player-placed: no pay
                pay(player, "miner", amount);
                return;
            }
            if (isPlainStone(type)) {
                if (placed.remove(k) != null) return; // player-placed: no progress
                stoneProgress(player);
                return;
            }
            Double cave = cavePay.get(type);
            if (cave != null) {
                if (placed.remove(k) != null) return; // player-placed: no pay
                pay(player, "miner", cave);
            }
        } else if (job.equals("woodcutter")) {
            if (!isLog(type)) return;
            if (placed.remove(k) != null) return; // player-placed: no pay
            pay(player, "woodcutter", WOODCUTTER_PAY);
        } else if (job.equals("farmer")) {
            Double amount = farmerPay.get(type);
            if (amount == null) return;
            if (!isMature(event.getBlock())) return;
            pay(player, "farmer", amount);
        }
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
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        if (!"fisher".equals(getJob(player.getUniqueId()))) return;
        if (!(event.getCaught() instanceof Item item)) return;
        pay(player, "fisher", fisherPay.getOrDefault(item.getItemStack().getType(), 0.50));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!"hunter".equals(getJob(killer.getUniqueId()))) return;
        Double amount = hunterPay.get(event.getEntityType().name());
        if (amount == null || amount <= 0) return;
        pay(killer, "hunter", amount);
    }

    // ---------- persistence ----------

    private void startSaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAll();
            }
        }.runTaskTimer(plugin, SAVE_INTERVAL_TICKS, SAVE_INTERVAL_TICKS);
    }

    public void saveAll() {
        try {
            Files.write(jobsFile, gson.toJson(jobs).getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save jobs.json");
        }
        try {
            JsonArray arr = new JsonArray();
            for (String k : placed.keySet()) arr.add(k);
            Files.write(placedFile, gson.toJson(arr).getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save jobs_placed.json");
        }
    }

    private void loadAll() {
        plugin.getDataFolder().mkdirs();
        try {
            if (Files.exists(jobsFile)) {
                JsonObject loaded = gson.fromJson(new String(Files.readAllBytes(jobsFile)), JsonObject.class);
                if (loaded != null) loaded.entrySet().forEach(e -> jobs.add(e.getKey(), e.getValue()));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load jobs.json");
        }
        try {
            if (Files.exists(placedFile)) {
                JsonArray arr = gson.fromJson(new String(Files.readAllBytes(placedFile)), JsonArray.class);
                if (arr != null) {
                    for (JsonElement el : arr) {
                        if (placed.size() >= PLACED_CAP) break;
                        placed.put(el.getAsString(), Boolean.TRUE);
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load jobs_placed.json");
        }
    }

    private void initTables() {
        minerPay.put(Material.COAL_ORE, 1.00);
        minerPay.put(Material.DEEPSLATE_COAL_ORE, 1.25);
        minerPay.put(Material.IRON_ORE, 2.50);
        minerPay.put(Material.DEEPSLATE_IRON_ORE, 3.00);
        minerPay.put(Material.COPPER_ORE, 1.50);
        minerPay.put(Material.DEEPSLATE_COPPER_ORE, 1.75);
        minerPay.put(Material.GOLD_ORE, 4.00);
        minerPay.put(Material.DEEPSLATE_GOLD_ORE, 4.50);
        minerPay.put(Material.NETHER_GOLD_ORE, 4.00);
        minerPay.put(Material.REDSTONE_ORE, 2.00);
        minerPay.put(Material.DEEPSLATE_REDSTONE_ORE, 2.25);
        minerPay.put(Material.LAPIS_ORE, 3.00);
        minerPay.put(Material.DEEPSLATE_LAPIS_ORE, 3.50);
        minerPay.put(Material.DIAMOND_ORE, 12.00);
        minerPay.put(Material.DEEPSLATE_DIAMOND_ORE, 14.00);
        minerPay.put(Material.EMERALD_ORE, 10.00);
        minerPay.put(Material.DEEPSLATE_EMERALD_ORE, 12.00);
        minerPay.put(Material.NETHER_QUARTZ_ORE, 1.50);
        minerPay.put(Material.ANCIENT_DEBRIS, 25.00);
        minerPay.put(Material.OBSIDIAN, 2.00);
        minerPay.put(Material.GLOWSTONE, 1.00);

        // Natural cave blocks: small instant pay (player-placed never pays)
        cavePay.put(Material.ANDESITE, 0.25);
        cavePay.put(Material.DIORITE, 0.25);
        cavePay.put(Material.GRANITE, 0.25);
        cavePay.put(Material.TUFF, 0.30);
        cavePay.put(Material.CALCITE, 0.35);
        cavePay.put(Material.DRIPSTONE_BLOCK, 0.30);
        cavePay.put(Material.POINTED_DRIPSTONE, 0.20);
        cavePay.put(Material.DIRT, 0.10);
        cavePay.put(Material.GRAVEL, 0.15);
        cavePay.put(Material.CLAY, 0.25);
        cavePay.put(Material.MOSS_BLOCK, 0.30);
        cavePay.put(Material.INFESTED_STONE, 0.50);
        cavePay.put(Material.INFESTED_COBBLESTONE, 0.40);

        farmerPay.put(Material.WHEAT, 1.00);
        farmerPay.put(Material.CARROTS, 1.00);
        farmerPay.put(Material.POTATOES, 1.00);
        farmerPay.put(Material.BEETROOTS, 0.75);
        farmerPay.put(Material.NETHER_WART, 1.00);
        farmerPay.put(Material.COCOA, 0.75);
        farmerPay.put(Material.SWEET_BERRY_BUSH, 0.50);
        farmerPay.put(Material.MELON, 1.50);
        farmerPay.put(Material.PUMPKIN, 1.50);
        farmerPay.put(Material.SUGAR_CANE, 0.50);
        farmerPay.put(Material.CACTUS, 0.50);

        fisherPay.put(Material.COD, 1.00);
        fisherPay.put(Material.SALMON, 1.50);
        fisherPay.put(Material.TROPICAL_FISH, 2.00);
        fisherPay.put(Material.PUFFERFISH, 1.50);
        fisherPay.put(Material.BOW, 3.00);
        fisherPay.put(Material.ENCHANTED_BOOK, 8.00);
        fisherPay.put(Material.NAME_TAG, 6.00);
        fisherPay.put(Material.NAUTILUS_SHELL, 10.00);
        fisherPay.put(Material.SADDLE, 5.00);
        fisherPay.put(Material.FISHING_ROD, 0.50);
        fisherPay.put(Material.LEATHER, 0.25);
        fisherPay.put(Material.LEATHER_BOOTS, 0.50);
        fisherPay.put(Material.ROTTEN_FLESH, 0.25);
        fisherPay.put(Material.STICK, 0.25);
        fisherPay.put(Material.STRING, 0.25);
        fisherPay.put(Material.BONE, 0.25);
        fisherPay.put(Material.TRIPWIRE_HOOK, 0.25);
        fisherPay.put(Material.INK_SAC, 0.50);
        fisherPay.put(Material.LILY_PAD, 0.25);
        fisherPay.put(Material.BOWL, 0.25);

        hunterPay.put("ZOMBIE", 1.00);
        hunterPay.put("HUSK", 1.00);
        hunterPay.put("DROWNED", 1.50);
        hunterPay.put("SKELETON", 1.50);
        hunterPay.put("STRAY", 1.50);
        hunterPay.put("BOGGED", 2.00);
        hunterPay.put("SPIDER", 1.00);
        hunterPay.put("CAVE_SPIDER", 1.50);
        hunterPay.put("CREEPER", 2.00);
        hunterPay.put("ENDERMAN", 3.00);
        hunterPay.put("WITCH", 3.00);
        hunterPay.put("BLAZE", 3.00);
        hunterPay.put("BREEZE", 4.00);
        hunterPay.put("GHAST", 5.00);
        hunterPay.put("MAGMA_CUBE", 1.00);
        hunterPay.put("SLIME", 0.75);
        hunterPay.put("PHANTOM", 2.00);
        hunterPay.put("PILLAGER", 2.00);
        hunterPay.put("VINDICATOR", 2.50);
        hunterPay.put("EVOKER", 4.00);
        hunterPay.put("RAVAGER", 6.00);
        hunterPay.put("VEX", 2.00);
        hunterPay.put("PIGLIN", 2.00);
        hunterPay.put("PIGLIN_BRUTE", 5.00);
        hunterPay.put("HOGLIN", 3.00);
        hunterPay.put("ZOGLIN", 4.00);
        hunterPay.put("WITHER_SKELETON", 4.00);
        hunterPay.put("ZOMBIFIED_PIGLIN", 1.50);
        hunterPay.put("GUARDIAN", 2.50);
        hunterPay.put("ELDER_GUARDIAN", 25.00);
        hunterPay.put("ENDERMITE", 2.00);
        hunterPay.put("SILVERFISH", 1.00);
        hunterPay.put("SHULKER", 3.00);
        hunterPay.put("WITHER", 200.00);
        hunterPay.put("ENDER_DRAGON", 500.00);
        hunterPay.put("WARDEN", 50.00);
    }
}
