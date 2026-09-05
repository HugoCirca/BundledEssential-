package com.bundleessential.level;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Server leveling system driven by collected XP orbs.
 * Each level needs more XP than the last: need(level) = baseXp * multiplier^(level-1).
 */
public class LevelManager implements Listener, CommandExecutor {

    private final JavaPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path levelsFile;
    private final JsonObject data = new JsonObject();

    private final double baseXp;
    private final double multiplier;
    private final double playtimeBonusPerLevel;

    public LevelManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.levelsFile = plugin.getDataFolder().toPath().resolve("levels.json");
        this.baseXp = Math.max(1.0, plugin.getConfig().getDouble("leveling.base-xp", 100.0));
        double mult = plugin.getConfig().getDouble("leveling.multiplier", 1.5);
        this.multiplier = mult < 1.01 ? 1.01 : mult;
        this.playtimeBonusPerLevel = Math.max(0.0, plugin.getConfig().getDouble("leveling.playtime-bonus-per-level", 0.10));
        loadLevels();
    }

    /** XP orbs needed to go from the given level to the next one. */
    public int getXpNeeded(int level) {
        if (level < 1) level = 1;
        return (int) Math.round(baseXp * Math.pow(multiplier, level - 1));
    }

    public int getLevel(UUID uuid) {
        return getEntry(uuid).get("level").getAsInt();
    }

    public int getLevel(Player player) {
        return getLevel(player.getUniqueId());
    }

    public int getXp(UUID uuid) {
        return getEntry(uuid).get("xp").getAsInt();
    }

    public int getTotalXp(UUID uuid) {
        return getEntry(uuid).get("total").getAsInt();
    }

    /** Playtime money multiplier for a player, e.g. level 5 at 0.10 bonus = 1.4x. */
    public double getPlaytimeMultiplier(UUID uuid) {
        return 1.0 + (getLevel(uuid) - 1) * playtimeBonusPerLevel;
    }

    public double getPlaytimeMultiplier(Player player) {
        return getPlaytimeMultiplier(player.getUniqueId());
    }

    public void addXp(Player player, int amount) {
        if (amount <= 0) return;
        UUID uuid = player.getUniqueId();
        JsonObject entry = getEntry(uuid);
        int level = entry.get("level").getAsInt();
        int xp = entry.get("xp").getAsInt() + amount;
        int total = entry.get("total").getAsInt() + amount;

        int needed = getXpNeeded(level);
        while (xp >= needed) {
            xp -= needed;
            level++;
            needed = getXpNeeded(level);
            player.sendMessage("§6§lLEVEL UP! §eYou are now level §6" + level);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        entry.addProperty("level", level);
        entry.addProperty("xp", xp);
        entry.addProperty("total", total);
        saveLevels();
    }

    @EventHandler
    public void onExpPickup(PlayerExpChangeEvent event) {
        if (event.getAmount() <= 0) return;
        addXp(event.getPlayer(), event.getAmount());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        getEntry(event.getPlayer().getUniqueId());
        saveLevels();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cUsage: /level <player>");
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found or offline!");
                return true;
            }
        }

        UUID uuid = target.getUniqueId();
        int level = getLevel(uuid);
        int xp = getXp(uuid);
        int needed = getXpNeeded(level);
        sender.sendMessage("§6" + target.getName() + "'s level: §e" + level);
        sender.sendMessage("§7XP: §a" + xp + "§7/§e" + needed + " §7(total collected: " + getTotalXp(uuid) + ")");
        sender.sendMessage("§7Next level needs §e" + needed + "§7 XP (" + progressBar(xp, needed) + "§7)");
        sender.sendMessage("§7Playtime prize bonus: §a+" + (int) Math.round((getPlaytimeMultiplier(uuid) - 1.0) * 100) + "%");
        return true;
    }

    private String progressBar(int xp, int needed) {
        int bars = 20;
        int filled = needed <= 0 ? bars : (int) Math.round((double) xp / needed * bars);
        if (filled > bars) filled = bars;
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? "§a|" : "§7|");
        }
        return sb.append("§8]").toString();
    }

    private JsonObject getEntry(UUID uuid) {
        String key = uuid.toString();
        if (!data.has(key)) {
            JsonObject entry = new JsonObject();
            entry.addProperty("level", 1);
            entry.addProperty("xp", 0);
            entry.addProperty("total", 0);
            data.add(key, entry);
        }
        return data.getAsJsonObject(key);
    }

    private void loadLevels() {
        plugin.getDataFolder().mkdirs();
        if (Files.exists(levelsFile)) {
            try {
                String json = new String(Files.readAllBytes(levelsFile));
                JsonObject loaded = gson.fromJson(json, JsonObject.class);
                if (loaded != null) {
                    loaded.entrySet().forEach(e -> data.add(e.getKey(), e.getValue()));
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load levels.json");
            }
        }
    }

    public void saveLevels() {
        try {
            Files.write(levelsFile, gson.toJson(data).getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save levels.json");
        }
    }
}
