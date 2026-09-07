package com.bundleessential.economy;

import com.bundleessential.level.LevelManager;
import com.bundleessential.util.Money;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class BalanceManager implements Listener, CommandExecutor {

    private final JavaPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path balancesFile;
    private final Path playtimeOptFile;
    private final JsonObject balances = new JsonObject();
    private final JsonObject playtimeOpt = new JsonObject();
    private final Random random = new Random();
    private BountyManager bountyManager;
    private LevelManager levelManager;

    private static final double MAX_MOB_REWARD = 10.0;
    private static final double DEF_MIN_PLAYTIME_REWARD = 8.0;
    private static final double DEF_MAX_PLAYTIME_REWARD = 12.0;
    private static final long PLAYTIME_INTERVAL_TICKS = 6000L;

    // Assist tracking: who hit each mob (for split kill payouts + quest credit).
    private static final int DAMAGERS_CAP = 2000;
    private final Map<UUID, Set<UUID>> damagers = new LinkedHashMap<UUID, Set<UUID>>(256, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Set<UUID>> eldest) {
            return size() > DAMAGERS_CAP;
        }
    };

    public BalanceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.balancesFile = plugin.getDataFolder().toPath().resolve("balances.json");
        this.playtimeOptFile = plugin.getDataFolder().toPath().resolve("playtime_opt.json");
        loadBalances();
        loadPlaytimeOpt();
        startPlaytimeTask();
    }

    public void setBountyManager(BountyManager bountyManager) {
        this.bountyManager = bountyManager;
    }

    public void setLevelManager(LevelManager levelManager) {
        this.levelManager = levelManager;
    }

    /** Playtime base range, tunable in config.yml (economy.playtime-min/max-reward). */
    private double playtimeMin() {
        try {
            return Math.max(0.0, plugin.getConfig().getDouble("economy.playtime-min-reward", DEF_MIN_PLAYTIME_REWARD));
        } catch (Exception e) {
            return DEF_MIN_PLAYTIME_REWARD;
        }
    }

    private double playtimeMax() {
        try {
            return Math.max(0.0, plugin.getConfig().getDouble("economy.playtime-max-reward", DEF_MAX_PLAYTIME_REWARD));
        } catch (Exception e) {
            return DEF_MAX_PLAYTIME_REWARD;
        }
    }

    private void loadBalances() {
        plugin.getDataFolder().mkdirs();
        if (Files.exists(balancesFile)) {
            try {
                String json = new String(Files.readAllBytes(balancesFile));
                JsonObject loaded = gson.fromJson(json, JsonObject.class);
                if (loaded != null) {
                    loaded.entrySet().forEach(e -> balances.add(e.getKey(), e.getValue()));
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load balances.json");
            }
        }
    }

    public void saveBalances() {
        try {
            Files.write(balancesFile, gson.toJson(balances).getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save balances.json");
        }
    }

    private void loadPlaytimeOpt() {
        plugin.getDataFolder().mkdirs();
        if (Files.exists(playtimeOptFile)) {
            try {
                String json = new String(Files.readAllBytes(playtimeOptFile));
                JsonObject loaded = gson.fromJson(json, JsonObject.class);
                if (loaded != null) {
                    loaded.entrySet().forEach(e -> playtimeOpt.add(e.getKey(), e.getValue()));
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load playtime_opt.json");
            }
        }
    }

    public void savePlaytimeOpt() {
        try {
            Files.write(playtimeOptFile, gson.toJson(playtimeOpt).getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save playtime_opt.json");
        }
    }

    private JsonObject playtimeEntry(UUID uuid) {
        String key = uuid.toString();
        if (!playtimeOpt.has(key) || !playtimeOpt.get(key).isJsonObject()) {
            JsonObject e = new JsonObject();
            e.addProperty("optOut", false);
            e.addProperty("vault", 0.0);
            playtimeOpt.add(key, e);
        }
        return playtimeOpt.getAsJsonObject(key);
    }

    public boolean isPlaytimeOptOut(UUID uuid) {
        try {
            return playtimeEntry(uuid).get("optOut").getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    public double getPlaytimeVault(UUID uuid) {
        try {
            return playtimeEntry(uuid).get("vault").getAsDouble();
        } catch (Exception e) {
            return 0.0;
        }
    }

    /** true = silent farm to vault, false = normal pay + message. Returns vaulted total. */
    public double setPlaytimeOptOut(Player player, boolean optOut) {
        JsonObject e = playtimeEntry(player.getUniqueId());
        e.addProperty("optOut", optOut);
        savePlaytimeOpt();
        if (!optOut) {
            double vault = getPlaytimeVault(player.getUniqueId());
            if (vault > 0) {
                e.addProperty("vault", 0.0);
                savePlaytimeOpt();
                addBalance(player, vault);
                return vault;
            }
        }
        return 0.0;
    }

    private String getBalanceKey(Player player) {
        return player.getUniqueId().toString();
    }

    public double getBalance(Player player) {
        String key = getBalanceKey(player);
        if (balances.has(key)) {
            return balances.get(key).getAsDouble();
        }
        return 0.0;
    }

    public double getBalance(UUID uuid) {
        String key = uuid.toString();
        if (balances.has(key)) {
            return balances.get(key).getAsDouble();
        }
        return 0.0;
    }

    public void setBalance(Player player, double amount) {
        balances.addProperty(getBalanceKey(player), amount);
        saveBalances();
        updateScoreboard(player);
    }

    public void addBalance(Player player, double amount) {
        addBalance(player.getUniqueId(), amount);
        updateScoreboard(player);
    }

    /** Offline-safe credit (no scoreboard refresh — use for offline payouts). */
    public void addBalance(UUID uuid, double amount) {
        String key = uuid.toString();
        double current = balances.has(key) ? balances.get(key).getAsDouble() : 0.0;
        balances.addProperty(key, current + amount);
        saveBalances();
    }

    public boolean removeBalance(Player player, double amount) {
        double current = getBalance(player);
        if (current < amount) return false;
        setBalance(player, current - amount);
        return true;
    }

    public void transfer(Player from, Player to, double amount) {
        if (removeBalance(from, amount)) {
            addBalance(to, amount);
        }
    }

    private void startPlaytimeTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    double min = playtimeMin();
                    double max = Math.max(min, playtimeMax());
                    double reward = Math.round((min + random.nextDouble() * (max - min)) * 100.0) / 100.0;
                    double mult = 1.0;
                    int level = 0;
                    if (levelManager != null) {
                        mult = levelManager.getPlaytimeMultiplier(player);
                        level = levelManager.getLevel(player);
                        reward = Math.round(reward * mult * 100.0) / 100.0;
                    }
                    if (bountyManager != null) reward = bountyManager.garnish(player, reward);
                    if (isPlaytimeOptOut(player.getUniqueId())) {
                        JsonObject e = playtimeEntry(player.getUniqueId());
                        double vault = getPlaytimeVault(player.getUniqueId()) + reward;
                        vault = Math.round(vault * 100.0) / 100.0;
                        e.addProperty("vault", vault);
                        savePlaytimeOpt();
                        continue; // silent farm, no message
                    }
                    addBalance(player, reward);
                    if (level > 1) {
                        player.sendMessage("§a[Playtime] §e+$" + Money.format(reward) + " §7(Lv " + level + " bonus +" + (int) Math.round((mult - 1.0) * 100) + "%)");
                    } else {
                        player.sendMessage("§a[Playtime] §e+$" + Money.format(reward));
                    }
                }
            }
        }.runTaskTimer(plugin, PLAYTIME_INTERVAL_TICKS, PLAYTIME_INTERVAL_TICKS);
    }

    private void updateScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective obj = board.registerNewObjective("ebalance", Criteria.DUMMY, "§6§lE-balance");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        hideSidebarNumbers(obj);

        Score line2 = obj.getScore("§fMoney: §a$" + Money.format(getBalance(player)));
        line2.setScore(2);
        Score line3 = obj.getScore(" ");
        line3.setScore(1);
        Score line4 = obj.getScore("§7Kill mobs & play");
        line4.setScore(0);

        player.setScoreboard(board);
    }

    /**
     * Hides the red sidebar numbers on Paper 1.20.5+ via blank number format.
     * Done by reflection so the plugin still compiles/runs on older Spigot —
     * there the numbers simply stay visible (vanilla forces them).
     */
    private void hideSidebarNumbers(Objective obj) {
        try {
            Class<?> formatClass = Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat");
            Object blank = formatClass.getMethod("blank").invoke(null);
            obj.getClass().getMethod("numberFormat", formatClass).invoke(obj, blank);
        } catch (Exception ignored) {
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!balances.has(getBalanceKey(player))) {
            balances.addProperty(getBalanceKey(player), 0.0);
            saveBalances();
        }
        updateScoreboard(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        Player p = null;
        if (event.getDamager() instanceof Player pl) {
            p = pl;
        } else if (event.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player shooter) {
            p = shooter;
        }
        if (p == null) {
            return;
        }
        damagers.computeIfAbsent(event.getEntity().getUniqueId(), k -> new HashSet<>()).add(p.getUniqueId());
    }

    /** Everyone who hit this mob plus the killer (assist-split payouts/credit). */
    public Set<UUID> contributors(Entity entity, UUID killerId) {
        Set<UUID> out = new LinkedHashSet<>();
        Set<UUID> hit = damagers.get(entity.getUniqueId());
        if (hit != null) {
            out.addAll(hit);
        }
        if (killerId != null) {
            out.add(killerId);
        }
        return out;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        for (Set<UUID> set : damagers.values()) {
            set.remove(id);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeathCleanup(EntityDeathEvent event) {
        damagers.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (event.getEntity() instanceof Player victim) {
            if (bountyManager != null) {
                bountyManager.claimBounty(killer, victim);
            }
        } else {
            double reward = Math.round(random.nextDouble() * MAX_MOB_REWARD * 100.0) / 100.0;
            if (reward < 0.01) reward = 0.01;
            Set<UUID> party = contributors(event.getEntity(), killer.getUniqueId());
            if (party.size() > 1) {
                double share = Math.floor(reward / party.size() * 100.0) / 100.0;
                double remainder = Math.round((reward - share * party.size()) * 100.0) / 100.0;
                for (UUID id : party) {
                    double pay = share + (id.equals(killer.getUniqueId()) ? remainder : 0);
                    if (pay <= 0) continue;
                    Player p = Bukkit.getPlayer(id);
                    double kept = pay;
                    if (p != null && bountyManager != null) kept = bountyManager.garnish(p, pay);
                    if (p != null) {
                        addBalance(p, kept);
                        p.sendMessage("§a[Kill] §e+$" + Money.format(kept) + " §7(split " + party.size() + " ways)");
                    } else {
                        addBalance(id, kept);
                    }
                }
            } else {
                if (bountyManager != null) reward = bountyManager.garnish(killer, reward);
                addBalance(killer, reward);
                killer.sendMessage("§a[Kill] §e+$" + Money.format(reward));
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can use this command!");
                return true;
            }
            player.sendMessage("§6Your balance: §a$" + Money.format(getBalance(player)));
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found or offline!");
                return true;
            }
            sender.sendMessage("§6" + target.getName() + "'s balance: §a$" + Money.format(getBalance(target)));
        }
        return true;
    }
}
