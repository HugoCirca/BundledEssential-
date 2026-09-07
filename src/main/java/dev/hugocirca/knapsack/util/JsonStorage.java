package dev.hugocirca.knapsack.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Shared JSON persistence helper. Every manager previously copy-pasted this.
 * Centralizes pretty-printing Gson, folder creation, load-or-empty, and atomic save.
 */
public final class JsonStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonStorage() {}

    public static Gson gson() {
        return GSON;
    }

    public static Path file(JavaPlugin plugin, String name) {
        return plugin.getDataFolder().toPath().resolve(name);
    }

    /** Load a JsonObject or return empty if missing/corrupt. Never throws. */
    public static JsonObject load(JavaPlugin plugin, String name) {
        Path path = file(plugin, name);
        plugin.getDataFolder().mkdirs();
        if (!Files.exists(path)) return new JsonObject();
        try {
            String json = new String(Files.readAllBytes(path));
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            return obj != null ? obj : new JsonObject();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load " + name, e);
            return new JsonObject();
        }
    }

    /** Load into existing target (preserves its reference) — mirrors old `loaded.entrySet().forEach`. */
    public static void loadInto(JavaPlugin plugin, String name, JsonObject target) {
        JsonObject loaded = load(plugin, name);
        loaded.entrySet().forEach(e -> target.add(e.getKey(), e.getValue()));
    }

    public static void save(JavaPlugin plugin, String name, JsonObject data) {
        Path path = file(plugin, name);
        plugin.getDataFolder().mkdirs();
        try {
            Files.write(path, GSON.toJson(data).getBytes());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save " + name, e);
        }
    }

    /** Raw bytes variant for managers that write Gson-serialized data directly. */
    public static void saveRaw(JavaPlugin plugin, Path path, String json) {
        plugin.getDataFolder().mkdirs();
        try {
            Files.write(path, json.getBytes());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save " + path.getFileName(), e);
        }
    }
}
