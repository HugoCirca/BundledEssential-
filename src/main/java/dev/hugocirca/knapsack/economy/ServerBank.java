package dev.hugocirca.knapsack.economy;

import dev.hugocirca.knapsack.util.Money;
import dev.hugocirca.knapsack.common.Saveable;
import dev.hugocirca.knapsack.util.JsonStorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Extracted from BalanceManager — owns serverbank.json, history (500 cap), and the 2B bank cap.
 * BalanceManager now delegates to this; public API is preserved for other managers.
 */
public class ServerBank implements Saveable {

    private static final double BANK_CAP = 2_000_000_000.0;
    private static final int HISTORY_CAP = 500;

    private final JavaPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private double serverBank = 0.0;
    private final List<JsonObject> history = new ArrayList<>();

    public ServerBank(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("serverbank.json");
        load();
    }

    private void load() {
        JsonObject obj = JsonStorage.load(plugin, "serverbank.json");
        try {
            if (obj != null && obj.has("balance")) serverBank = Math.max(0, obj.get("balance").getAsDouble());
            if (obj != null && obj.has("history") && obj.get("history").isJsonArray()) {
                history.clear();
                for (JsonElement el : obj.getAsJsonArray("history")) if (el.isJsonObject()) history.add(el.getAsJsonObject());
                while (history.size() > HISTORY_CAP) history.remove(0);
            }
            if (serverBank > BANK_CAP) {
                plugin.getLogger().info("Server Bank " + serverBank + " exceeds 2B cap — resetting to 0 and clearing history.");
                serverBank = 0;
                history.clear();
                save();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load serverbank.json");
        }
    }

    public double get() { return Math.round(serverBank * 100.0) / 100.0; }

    public List<JsonObject> history() { return new ArrayList<>(history); }

    public void add(double amount, String player, String reason) {
        if (amount <= 0) return;
        double next = serverBank + amount;
        if (next >= BANK_CAP || !Double.isFinite(next)) {
            serverBank = BANK_CAP;
            plugin.getLogger().warning("Server Bank capped at 2B (overflow discarded)!");
        } else {
            serverBank = Math.round(next * 100.0) / 100.0;
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("time", System.currentTimeMillis());
        entry.addProperty("player", player == null ? "SYSTEM" : player);
        entry.addProperty("amount", Math.round(amount * 100.0) / 100.0);
        entry.addProperty("reason", reason);
        entry.addProperty("bankAfter", get());
        history.add(entry);
        while (history.size() > HISTORY_CAP) history.remove(0);
        save();
    }

    public void add(double amount) { add(amount, "SYSTEM", "unknown"); }

    public void save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("balance", Math.round(serverBank * 100.0) / 100.0);
        JsonArray arr = new JsonArray();
        for (JsonObject e : history) arr.add(e);
        obj.add("history", arr);
        JsonStorage.save(plugin, "serverbank.json", obj);
    }

    @Override
    public void saveAll() { save(); }

    public void reset() {
        serverBank = 0;
        history.clear();
        save();
    }

    /** Used by interest payout (deduct without history spam double-save). */
    public boolean deduct(double amount, String reason, int recipients) {
        if (amount <= 0) return false;
        serverBank = Math.round((serverBank - amount) * 100.0) / 100.0;
        if (serverBank < 0) serverBank = 0;
        JsonObject entry = new JsonObject();
        entry.addProperty("time", System.currentTimeMillis());
        entry.addProperty("player", "BANK→" + recipients + " players");
        entry.addProperty("amount", -Math.round(amount * 100.0) / 100.0);
        entry.addProperty("reason", reason);
        entry.addProperty("bankAfter", get());
        history.add(entry);
        while (history.size() > HISTORY_CAP) history.remove(0);
        save();
        return true;
    }

    public ItemStack historyBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return book;
        meta.setTitle("§6Server Bank History");
        meta.setAuthor("Bank");
        List<String> pages = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm");
        if (history.isEmpty()) {
            pages.add("§4§lServer Bank\n§0No transactions yet.\n\n§0Balance: $" + Money.format(get()));
        } else {
            StringBuilder cur = new StringBuilder("§4§lServer Bank §0$" + Money.format(get()) + "\n\n");
            int count = 0;
            for (int i = history.size() - 1; i >= 0; i--) {
                JsonObject e = history.get(i);
                String t = fmt.format(new Date(e.get("time").getAsLong()));
                String who = e.has("player") ? e.get("player").getAsString() : "?";
                String rsn = e.has("reason") ? e.get("reason").getAsString() : "?";
                String amt = Money.format(e.get("amount").getAsDouble());
                String line = "§0" + t + " §1" + who + " §a$" + amt + " §7" + rsn + "\n";
                if (cur.length() + line.length() > 240) {
                    pages.add(cur.toString());
                    cur = new StringBuilder();
                }
                cur.append(line);
                count++;
                if (count >= 80) break;
                if (pages.size() >= 12) break;
            }
            if (cur.length() > 0) pages.add(cur.toString());
        }
        if (pages.isEmpty()) pages.add("§0Empty");
        meta.setPages(pages);
        book.setItemMeta(meta);
        return book;
    }

    public double getBankCap() { return BANK_CAP; }
}
