package com.bundleessential.chunkload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
 * Named chunkloaders: /chunkload keeps the chunk you stand in force-loaded
 * (farms keep running, even while you are offline), /chunkdelete removes one,
 * /showchunk outlines them with particles. Free, 3 per player, no upkeep.
 * Force-loads reset on restart, so all stored loaders are re-applied on enable.
 */
public class ChunkLoadManager implements CommandExecutor {

    private static final long SAVE_INTERVAL_TICKS = 6000L;
    private static final int MAX_PER_PLAYER = 3;
    private static final long SHOW_PERIOD_TICKS = 20L;
    private static final int SHOW_RUNS = 10;

    private final JavaPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final JsonObject data = new JsonObject();

    public ChunkLoadManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("chunkloaders.json");
        loadAll();
        applyAll();
        startSaveTask();
    }

    // ---------- data ----------

    private JsonObject loaders() {
        if (!data.has("loaders") || !data.get("loaders").isJsonObject()) {
            data.add("loaders", new JsonObject());
        }
        return data.getAsJsonObject("loaders");
    }

    private JsonArray playerLoaders(UUID playerId) {
        JsonObject all = loaders();
        String key = playerId.toString();
        if (!all.has(key) || !all.get(key).isJsonArray()) {
            all.add(key, new JsonArray());
        }
        return all.getAsJsonArray(key);
    }

    private static String nameOf(JsonObject e) {
        try {
            return e.get("name").getAsString();
        } catch (Exception ex) {
            return "?";
        }
    }

    private static World worldOf(JsonObject e) {
        try {
            return Bukkit.getWorld(UUID.fromString(e.get("world").getAsString()));
        } catch (Exception ex) {
            return null;
        }
    }

    private static int intOf(JsonObject e, String key) {
        return e.get(key).getAsInt();
    }

    public void saveAll() {
        try {
            Files.write(file, gson.toJson(data).getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save chunkloaders.json");
        }
    }

    private void loadAll() {
        plugin.getDataFolder().mkdirs();
        try {
            if (Files.exists(file)) {
                JsonObject loaded = gson.fromJson(new String(Files.readAllBytes(file)), JsonObject.class);
                if (loaded != null) {
                    loaded.entrySet().forEach(e -> data.add(e.getKey(), e.getValue()));
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load chunkloaders.json");
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

    /** Re-apply every stored loader (force-loads do not survive restarts). */
    private void applyAll() {
        int count = 0;
        for (Map.Entry<String, JsonElement> en : new ArrayList<>(loaders().entrySet())) {
            if (!en.getValue().isJsonArray()) {
                continue;
            }
            for (JsonElement el : en.getValue().getAsJsonArray()) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject e = el.getAsJsonObject();
                World world = worldOf(e);
                if (world == null) {
                    continue;
                }
                try {
                    world.setChunkForceLoaded(intOf(e, "cx"), intOf(e, "cz"), true);
                    count++;
                } catch (Exception ignored) {}
            }
        }
        if (count > 0) {
            plugin.getLogger().info("Re-applied " + count + " chunkloader(s).");
        }
    }

    /** True if any stored loader (any player) still covers this chunk. */
    private boolean coveredByAnother(String worldId, int cx, int cz, String exceptPlayer, String exceptName) {
        for (Map.Entry<String, JsonElement> en : loaders().entrySet()) {
            if (!en.getValue().isJsonArray()) {
                continue;
            }
            for (JsonElement el : en.getValue().getAsJsonArray()) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject e = el.getAsJsonObject();
                try {
                    if (!e.get("world").getAsString().equals(worldId)) {
                        continue;
                    }
                    if (e.get("cx").getAsInt() != cx || e.get("cz").getAsInt() != cz) {
                        continue;
                    }
                    if (en.getKey().equals(exceptPlayer) && nameOf(e).equalsIgnoreCase(exceptName)) {
                        continue;
                    }
                    return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    // ---------- commands ----------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        switch (command.getName().toLowerCase()) {
            case "chunkload" -> handleLoad(player, args);
            case "chunkdelete" -> handleDelete(player, args);
            case "showchunk" -> handleShow(player, args);
            default -> player.sendMessage("§cUsage: /chunkload [name], /chunkdelete <name>, /showchunk [name]");
        }
        return true;
    }

    private void handleLoad(Player player, String[] args) {
        Chunk chunk = player.getLocation().getChunk();
        String worldId = player.getWorld().getUID().toString();
        int cx = chunk.getX();
        int cz = chunk.getZ();

        JsonArray mine = playerLoaders(player.getUniqueId());
        if (mine.size() >= MAX_PER_PLAYER) {
            player.sendMessage("§cChunkloader limit reached! §7Max " + MAX_PER_PLAYER + " — delete one with §e/chunkdelete <name>");
            return;
        }
        String name = args.length == 0 ? "loader-" + (mine.size() + 1) : String.join(" ", args).trim();
        if (name.isEmpty()) {
            name = "loader-" + (mine.size() + 1);
        }
        if (name.length() > 24) {
            name = name.substring(0, 24);
        }
        for (JsonElement el : mine) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject e = el.getAsJsonObject();
            try {
                if (e.get("world").getAsString().equals(worldId)
                        && e.get("cx").getAsInt() == cx && e.get("cz").getAsInt() == cz) {
                    player.sendMessage("§cThis chunk is already loaded as §e" + nameOf(e) + "§c!");
                    return;
                }
                if (nameOf(e).equalsIgnoreCase(name)) {
                    player.sendMessage("§cYou already have a loader named §e" + name + "§c!");
                    return;
                }
            } catch (Exception ignored) {}
        }

        JsonObject e = new JsonObject();
        e.addProperty("name", name);
        e.addProperty("world", worldId);
        e.addProperty("cx", cx);
        e.addProperty("cz", cz);
        mine.add(e);
        try {
            player.getWorld().setChunkForceLoaded(cx, cz, true);
        } catch (Exception ex) {
            player.sendMessage("§cCould not force-load this chunk on your server version.");
            mine.remove(e);
            return;
        }
        saveAll();
        player.sendMessage("§aChunk loaded! §e" + name + " §7(" + player.getWorld().getName()
                + " " + cx + ", " + cz + ") §7— " + mine.size() + "/" + MAX_PER_PLAYER + " used.");
        player.sendMessage("§7See it with §e/showchunk");
    }

    private void handleDelete(Player player, String[] args) {
        JsonArray mine = playerLoaders(player.getUniqueId());
        if (mine.size() == 0) {
            player.sendMessage("§cYou have no chunkloaders! Make one with §e/chunkload [name]");
            return;
        }
        String name = args.length == 0 ? null : String.join(" ", args).trim();
        JsonObject target = null;
        if (name == null || name.isEmpty()) {
            if (mine.size() == 1 && mine.get(0).isJsonObject()) {
                target = mine.get(0).getAsJsonObject();
            } else {
                player.sendMessage("§cUsage: /chunkdelete <name>");
                return;
            }
        } else {
            for (JsonElement el : mine) {
                if (el.isJsonObject() && nameOf(el.getAsJsonObject()).equalsIgnoreCase(name)) {
                    target = el.getAsJsonObject();
                    break;
                }
            }
            if (target == null) {
                player.sendMessage("§cNo loader named §e" + name + "§c! See them with §e/showchunk");
                return;
            }
        }

        String targetName = nameOf(target);
        String worldId;
        int cx;
        int cz;
        try {
            worldId = target.get("world").getAsString();
            cx = target.get("cx").getAsInt();
            cz = target.get("cz").getAsInt();
        } catch (Exception ex) {
            mine.remove(target);
            saveAll();
            player.sendMessage("§cRemoved a broken loader entry.");
            return;
        }
        mine.remove(target);
        saveAll();
        if (!coveredByAnother(worldId, cx, cz, player.getUniqueId().toString(), targetName)) {
            try {
                World world = Bukkit.getWorld(UUID.fromString(worldId));
                if (world != null) {
                    world.setChunkForceLoaded(cx, cz, false);
                }
            } catch (Exception ignored) {}
        }
        player.sendMessage("§aDeleted loader §e" + targetName + "§a! §7(" + mine.size() + "/" + MAX_PER_PLAYER + " used)");
    }

    private void handleShow(Player player, String[] args) {
        JsonArray mine = playerLoaders(player.getUniqueId());
        if (mine.size() == 0) {
            player.sendMessage("§cYou have no chunkloaders! Make one with §e/chunkload [name]");
            return;
        }
        List<JsonObject> targets = new ArrayList<>();
        if (args.length == 0) {
            for (JsonElement el : mine) {
                if (el.isJsonObject()) {
                    targets.add(el.getAsJsonObject());
                }
            }
        } else {
            String name = String.join(" ", args).trim();
            for (JsonElement el : mine) {
                if (el.isJsonObject() && nameOf(el.getAsJsonObject()).equalsIgnoreCase(name)) {
                    targets.add(el.getAsJsonObject());
                    break;
                }
            }
            if (targets.isEmpty()) {
                player.sendMessage("§cNo loader named §e" + name + "§c!");
                return;
            }
        }

        player.sendMessage("§6§lYour chunkloaders §7(" + targets.size() + "):");
        List<Outline> outlines = new ArrayList<>();
        for (JsonObject e : targets) {
            World world;
            int cx;
            int cz;
            try {
                world = Bukkit.getWorld(UUID.fromString(e.get("world").getAsString()));
                cx = e.get("cx").getAsInt();
                cz = e.get("cz").getAsInt();
            } catch (Exception ex) {
                continue;
            }
            if (world == null) {
                continue;
            }
            player.sendMessage("§e" + nameOf(e) + " §7— " + world.getName() + " §f" + cx + "§7, §f" + cz);
            if (world.equals(player.getWorld())) {
                outlines.add(new Outline(world, cx, cz));
            }
        }
        if (outlines.isEmpty()) {
            player.sendMessage("§7(none in this world to outline — list above has them all)");
            return;
        }
        int y = player.getLocation().getBlockY() + 1;
        new BukkitRunnable() {
            int runs = 0;

            @Override
            public void run() {
                if (!player.isOnline() || runs++ >= SHOW_RUNS) {
                    cancel();
                    return;
                }
                for (Outline o : outlines) {
                    outlineChunk(player, o.world, o.cx, o.cz, y);
                }
            }
        }.runTaskTimer(plugin, 0L, SHOW_PERIOD_TICKS);
    }

    private void outlineChunk(Player viewer, World world, int cx, int cz, int y) {
        try {
            int x0 = cx * 16;
            int z0 = cz * 16;
            Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1.0f);
            for (int i = 0; i <= 16; i++) {
                viewer.spawnParticle(Particle.REDSTONE, x0 + i + 0.5, y + 0.5, z0 + 0.5, 1, dust);
                viewer.spawnParticle(Particle.REDSTONE, x0 + i + 0.5, y + 0.5, z0 + 16 + 0.5, 1, dust);
                viewer.spawnParticle(Particle.REDSTONE, x0 + 0.5, y + 0.5, z0 + i + 0.5, 1, dust);
                viewer.spawnParticle(Particle.REDSTONE, x0 + 16 + 0.5, y + 0.5, z0 + i + 0.5, 1, dust);
            }
        } catch (Exception ignored) {}
    }

    private static class Outline {
        final World world;
        final int cx;
        final int cz;

        Outline(World world, int cx, int cz) {
            this.world = world;
            this.cx = cx;
            this.cz = cz;
        }
    }
}
