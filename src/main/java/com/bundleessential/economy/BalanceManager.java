package com.bundleessential.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
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

public class BalanceManager implements Listener {

    private final JavaPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path balancesFile;
    private final JsonObject balances = new JsonObject();
    private BountyManager bountyManager;

    private static final double MOB_KILL_REWARD = 5.0;
    private static final double PLAYTIME_REWARD = 15.0;
    private static final long PLAYTIME_INTERVAL_TICKS = 6000L; // 5 minutes

    public BalanceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.balancesFile = plugin.getDataFolder().toPath().resolve("balances.json");
        loadBalances();
        startPlaytimeTask();
    }

    public void setBountyManager(BountyManager bountyManager) {
        this.bountyManager = bountyManager;
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
                    addBalance(player, PLAYTIME_REWARD);
                    player.sendMessage("§a[Playtime] §e+$" + PLAYTIME_REWARD);
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
        Score line2 = obj.getScore("§fMoney: §a$" + String.format("%.2f", getBalance(player)));
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
            addBalance(killer, MOB_KILL_REWARD);
            killer.sendMessage("§a[Kill] §e+$" + MOB_KILL_REWARD);
        }
    }
}
