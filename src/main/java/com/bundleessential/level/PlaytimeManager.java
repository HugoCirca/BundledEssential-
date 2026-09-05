package com.bundleessential.level;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks how long each player has been online.
 * /playtime shows your time, /playtime leaderboard shows the top 10.
 */
public class PlaytimeManager implements Listener, CommandExecutor {

    private static final long TICK_INTERVAL = 1200L; // 1 minute

    private final JavaPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path playtimeFile;
    private final JsonObject data = new JsonObject();

    public PlaytimeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playtimeFile = plugin.getDataFolder().toPath().resolve("playtime.json");
        loadPlaytime();
        startTask();
    }

    public int getMinutes(UUID uuid) {
        return getEntry(uuid).get("minutes").getAsInt();
    }

    public int getMinutes(Player player) {
        return getMinutes(player.getUniqueId());
    }

    public static String format(int minutes) {
        if (minutes < 1) return "less than a minute";
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours <= 0) return mins + "m";
        return hours + "h " + mins + "m";
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        JsonObject entry = getEntry(event.getPlayer().getUniqueId());
        entry.addProperty("name", event.getPlayer().getName());
        savePlaytime();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && (args[0].equalsIgnoreCase("leaderboard") || args[0].equalsIgnoreCase("top"))) {
            List<Map.Entry<String, JsonObject>> entries = new ArrayList<>();
            for (Map.Entry<String, com.google.gson.JsonElement> e : data.entrySet()) {
                if (e.getValue().isJsonObject()) entries.add(Map.entry(e.getKey(), e.getValue().getAsJsonObject()));
            }
            entries.sort((a, b) -> Integer.compare(
                    b.getValue().has("minutes") ? b.getValue().get("minutes").getAsInt() : 0,
                    a.getValue().has("minutes") ? a.getValue().get("minutes").getAsInt() : 0));

            sender.sendMessage("§6§lPlaytime Leaderboard");
            int shown = Math.min(10, entries.size());
            if (shown == 0) {
                sender.sendMessage("§7No playtime recorded yet.");
                return true;
            }
            for (int i = 0; i < shown; i++) {
                JsonObject entry = entries.get(i).getValue();
                String name = entry.has("name") ? entry.get("name").getAsString() : entries.get(i).getKey();
                int minutes = entry.has("minutes") ? entry.get("minutes").getAsInt() : 0;
                sender.sendMessage("§e" + (i + 1) + ". §f" + name + " §7- §a" + format(minutes));
            }
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cUsage: /playtime [leaderboard|<player>]");
                return true;
            }
            sender.sendMessage("§6Your playtime: §a" + format(getMinutes(player)));
            return true;
        }

        Player online = Bukkit.getPlayer(args[0]);
        if (online != null) {
            sender.sendMessage("§6" + online.getName() + "'s playtime: §a" + format(getMinutes(online)));
            return true;
        }
        for (Map.Entry<String, com.google.gson.JsonElement> e : data.entrySet()) {
            JsonObject entry = e.getValue().getAsJsonObject();
            if (entry.has("name") && entry.get("name").getAsString().equalsIgnoreCase(args[0])) {
                sender.sendMessage("§6" + entry.get("name").getAsString() + "'s playtime: §a" + format(entry.get("minutes").getAsInt()));
                return true;
            }
        }
        try {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
            if (offline.hasPlayedBefore() || offline.isOnline()) {
                UUID uuid = offline.getUniqueId();
                if (data.has(uuid.toString())) {
                    sender.sendMessage("§6" + args[0] + "'s playtime: §a" + format(getMinutes(uuid)));
                } else {
                    sender.sendMessage("§6" + args[0] + "'s playtime: §a" + format(0));
                }
                return true;
            }
        } catch (Exception ignored) {}
        sender.sendMessage("§cPlayer not found!");
        return true;
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean changed = false;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    JsonObject entry = getEntry(player.getUniqueId());
                    entry.addProperty("minutes", entry.get("minutes").getAsInt() + 1);
                    entry.addProperty("name", player.getName());
                    changed = true;
                }
                if (changed) savePlaytime();
            }
        }.runTaskTimer(plugin, TICK_INTERVAL, TICK_INTERVAL);
    }

    private JsonObject getEntry(UUID uuid) {
        String key = uuid.toString();
        if (!data.has(key)) {
            JsonObject entry = new JsonObject();
            entry.addProperty("minutes", 0);
            entry.addProperty("name", key);
            data.add(key, entry);
        }
        return data.getAsJsonObject(key);
    }

    private void loadPlaytime() {
        plugin.getDataFolder().mkdirs();
        if (Files.exists(playtimeFile)) {
            try {
                String json = new String(Files.readAllBytes(playtimeFile));
                JsonObject loaded = gson.fromJson(json, JsonObject.class);
                if (loaded != null) {
                    loaded.entrySet().forEach(e -> data.add(e.getKey(), e.getValue()));
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load playtime.json");
            }
        }
    }

    public void savePlaytime() {
        try {
            Files.write(playtimeFile, gson.toJson(data).getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save playtime.json");
        }
    }
}
