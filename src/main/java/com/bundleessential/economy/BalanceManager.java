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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.UUID;

public class BalanceManager implements Listener, CommandExecutor {

    private final JavaPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path balancesFile;
    private final JsonObject balances = new JsonObject();
    private final Random random = new Random();
    private BountyManager bountyManager;
    private LevelManager levelManager;

    private static final double MAX_MOB_REWARD = 10.0;
    private static final double MIN_PLAYTIME_REWARD = 1.0;
    private static final double MAX_PLAYTIME_REWARD = 3.0;
    private static final long PLAYTIME_INTERVAL_TICKS = 6000L;

    public BalanceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.balancesFile = plugin.getDataFolder().toPath().resolve("balances.json");
        loadBalances();
        startPlaytimeTask();
    }

    public void setBountyManager(BountyManager bountyManager) {
        this.bountyManager = bountyManager;
    }

    public void setLevelManager(LevelManager levelManager) {
        this.levelManager = levelManager;
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
        setBalance(player, getBalance(player) + amount);
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
                    double reward = Math.round((MIN_PLAYTIME_REWARD + random.nextDouble() * (MAX_PLAYTIME_REWARD - MIN_PLAYTIME_REWARD)) * 100.0) / 100.0;
                    double mult = 1.0;
                    int level = 0;
                    if (levelManager != null) {
                        mult = levelManager.getPlaytimeMultiplier(player);
                        level = levelManager.getLevel(player);
                        reward = Math.round(reward * mult * 100.0) / 100.0;
                    }
                    if (bountyManager != null) reward = bountyManager.garnish(player, reward);
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

        Score line1 = obj.getScore("=============");
        line1.setScore(3);
        Score line2 = obj.getScore("§fMoney: §a$" + Money.format(getBalance(player)));
        line2.setScore(2);
        Score line3 = obj.getScore(" ");
        line3.setScore(1);
        Score line4 = obj.getScore("§7Kill mobs & play");
        line4.setScore(0);

        player.setScoreboard(board);
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
            if (bountyManager != null) reward = bountyManager.garnish(killer, reward);
            addBalance(killer, reward);
            killer.sendMessage("§a[Kill] §e+$" + Money.format(reward));
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
