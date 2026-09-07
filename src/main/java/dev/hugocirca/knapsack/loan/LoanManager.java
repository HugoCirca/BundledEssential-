package dev.hugocirca.knapsack.loan;

import dev.hugocirca.knapsack.economy.BalanceManager;
import dev.hugocirca.knapsack.common.Saveable;
import dev.hugocirca.knapsack.util.JsonStorage;
import dev.hugocirca.knapsack.util.Money;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /loan chest GUI: bedrock buttons for 200/500/1000/1500/2000/2500 + compass custom (anvil).
 * Then pick: 3/7/14/30 days lump OR slow auto-deduct.
 * Lump: must /loan pay before due; overdue = +5%/day and balance can go negative.
 * Slow: 20% of each earning auto-pays until cleared.
 * On-time full repay = +$25 bonus. Data in loans.json.
 */
public class LoanManager implements CommandExecutor, TabCompleter, Listener, Saveable {

    private static final String TITLE_AMT = "§6Loan - Pick Amount";
    private static final String TITLE_REPAY = "§6Loan - Repayment";
    private static final List<Integer> AMOUNTS = Arrays.asList(200, 500, 1000, 1500, 2000, 2500);
    private static final double REWARD = 25.0;
    private static final double LATE_DAILY = 0.05;
    private static final double SLOW_RATE = 0.20;

    private final JavaPlugin plugin;
    private final BalanceManager balance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final JsonObject data = new JsonObject();
    private final Map<UUID, Integer> pendingAmt = new HashMap<>();

    public LoanManager(JavaPlugin plugin, BalanceManager balance) {
        this.plugin = plugin;
        this.balance = balance;
        this.file = plugin.getDataFolder().toPath().resolve("loans.json");
        loadAll();
        startLateTask();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private JsonArray loans() {
        if (!data.has("loans") || !data.get("loans").isJsonArray()) data.add("loans", new JsonArray());
        return data.getAsJsonArray("loans");
    }

    private void loadAll() {
        JsonObject loaded = JsonStorage.load(plugin, "loans.json");
        if (loaded != null) loaded.entrySet().forEach(e -> data.add(e.getKey(), e.getValue()));
    }

    @Override
    public void saveAll() {
        JsonStorage.save(plugin, "loans.json", data);
    }

    private JsonObject newLoan(UUID player, int principal, int days, String mode) {
        JsonObject o = new JsonObject();
        o.addProperty("id", UUID.randomUUID().toString().substring(0, 8));
        o.addProperty("player", player.toString());
        o.addProperty("principal", principal);
        o.addProperty("debt", principal);
        o.addProperty("days", days);
        o.addProperty("mode", mode);
        o.addProperty("created", System.currentTimeMillis());
        long due = mode.equals("SLOW") ? 0 : System.currentTimeMillis() + days * 24L * 60 * 60 * 1000;
        o.addProperty("due", due);
        o.addProperty("repaid", false);
        return o;
    }

    private List<JsonObject> playerLoans(UUID id) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement el : loans()) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            if (o.has("player") && o.get("player").getAsString().equals(id.toString()) && !o.get("repaid").getAsBoolean())
                out.add(o);
        }
        return out;
    }

    private double totalDebt(UUID id) {
        double t = 0;
        for (JsonObject o : playerLoans(id)) t += o.get("debt").getAsDouble();
        return Math.round(t * 100.0) / 100.0;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length >= 1 && args[0].equalsIgnoreCase("pay")) sender.sendMessage("§cOnly players can use /loan pay");
            else sender.sendMessage("§cOnly players can use /loan");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("pay")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("all")) {
                payAll(player);
            } else if (args.length >= 2) {
                payOne(player, args[1]);
            } else {
                // pay smallest first
                List<JsonObject> ls = playerLoans(player.getUniqueId());
                if (ls.isEmpty()) { player.sendMessage("§aNo active loans!"); return true; }
                ls.sort((a,b)-> Double.compare(a.get("debt").getAsDouble(), b.get("debt").getAsDouble()));
                payOne(player, ls.get(0).get("id").getAsString());
            }
            return true;
        }
        if (args.length >= 1 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("list"))) {
            List<JsonObject> ls = playerLoans(player.getUniqueId());
            if (ls.isEmpty()) { player.sendMessage("§aNo active loans. Total debt $0"); return true; }
            player.sendMessage("§6Loans (" + ls.size() + ") total §c$" + Money.format(totalDebt(player.getUniqueId())));
            for (JsonObject o : ls) {
                String id = o.get("id").getAsString();
                double debt = o.get("debt").getAsDouble();
                String mode = o.get("mode").getAsString();
                long due = o.get("due").getAsLong();
                String dueStr = mode.equals("SLOW") ? "slow 20% garnish" : (due < System.currentTimeMillis() ? "§cOVERDUE" : ((due - System.currentTimeMillis())/86400000) + "d left");
                player.sendMessage(" §7" + id + " §f$" + Money.format(debt) + " §7" + mode + " " + dueStr);
            }
            player.sendMessage("§7/loan pay [id|all] to repay early (+$25 bonus if on time)");
            return true;
        }
        openAmountGui(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) { out.add("pay"); out.add("info"); out.add("list"); }
        else if (args.length == 2 && args[0].equalsIgnoreCase("pay")) {
            out.add("all");
            if (sender instanceof Player p) for (JsonObject o : playerLoans(p.getUniqueId())) out.add(o.get("id").getAsString());
        }
        String last = args.length==0?"":args[args.length-1].toLowerCase();
        out.removeIf(s -> !s.toLowerCase().startsWith(last));
        return out;
    }

    private void openAmountGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_AMT);
        ItemStack filler = make(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i=0;i<27;i++) inv.setItem(i, filler);
        int[] slots = {10,11,12,13,14,15};
        for (int i=0;i<AMOUNTS.size();i++) {
            int amt = AMOUNTS.get(i);
            inv.setItem(slots[i], make(Material.BEDROCK, "§e$" + amt, "§7Click to borrow §a$" + amt, "§7Then pick repayment"));
        }
        inv.setItem(22, make(Material.COMPASS, "§bCustom Amount", "§7Open anvil to type amount", "§750 - 10000"));
        // pending loans as paper to click-pay
        List<JsonObject> pending = playerLoans(player.getUniqueId());
        int pSlot = 18;
        for (JsonObject o : pending) {
            if (pSlot > 25) break;
            String id = o.get("id").getAsString();
            double debt = o.get("debt").getAsDouble();
            String mode = o.get("mode").getAsString();
            long due = o.get("due").getAsLong();
            String dueStr = mode.equals("SLOW") ? "slow 20%" : (due < System.currentTimeMillis() ? "§cOVERDUE" : ((due - System.currentTimeMillis())/86400000) + "d left");
            inv.setItem(pSlot++, make(Material.PAPER, "§e" + id + " §f$" + Money.format(debt), "§7" + mode + " " + dueStr, "§aClick to pay (+$25 if on time)"));
        }
        inv.setItem(26, make(Material.BARRIER, "§cClose", "§7Your debt: §c$" + Money.format(totalDebt(player.getUniqueId()))));
        player.openInventory(inv);
    }

    private void openRepayGui(Player player, int amount) {
        pendingAmt.put(player.getUniqueId(), amount);
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_REPAY);
        ItemStack filler = make(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i=0;i<27;i++) inv.setItem(i, filler);
        inv.setItem(10, make(Material.CLOCK, "§e3 Days", "§7Lump pay in 3 days", "§7Overdue +5%/day, can go negative"));
        inv.setItem(11, make(Material.CLOCK, "§e7 Days", "§7Lump pay in 7 days"));
        inv.setItem(12, make(Material.CLOCK, "§e14 Days", "§7Lump pay in 14 days (max)", "§72 weeks max"));
        inv.setItem(16, make(Material.HOPPER, "§aSlow Deduct", "§720% of earnings auto-pay", "§7No due date, no late fee"));
        inv.setItem(22, make(Material.EMERALD, "§aBorrow §e$" + amount, "§7Pick a repayment plan above"));
        player.openInventory(inv);
    }

    private void openCustomAnvil(Player player) {
        try {
            new AnvilGUI.Builder().onClose(s -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) openAmountGui(player);
            })).onClick((slot, state) -> {
                if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                String txt = state.getText()==null?"":state.getText().trim();
                int amt;
                try { amt = Integer.parseInt(txt); } catch (Exception e) { player.sendMessage("§cNot a number!"); return Collections.singletonList(AnvilGUI.ResponseAction.close()); }
                if (amt < 50 || amt > 10000) { player.sendMessage("§cCustom must be 50 - 10000!"); return Collections.singletonList(AnvilGUI.ResponseAction.close()); }
                Bukkit.getScheduler().runTask(plugin, () -> openRepayGui(player, amt));
                return Collections.singletonList(AnvilGUI.ResponseAction.close());
            }).text("").title("Custom loan amount").plugin(plugin).open(player);
        } catch (Exception ex) {
            player.sendMessage("§cAnvil unavailable on this version.");
            plugin.getLogger().warning("Loan anvil unavailable: " + ex.getMessage());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String title = e.getView().getTitle();
        boolean amt = title.equals(TITLE_AMT);
        boolean rep = title.equals(TITLE_REPAY);
        if (!amt && !rep) return;
        e.setCancelled(true);
        if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;
        ItemStack cur = e.getCurrentItem();
        if (cur == null || cur.getType() == Material.AIR || cur.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
        if (amt) {
            if (cur.getType() == Material.PAPER) {
                String id = cur.getItemMeta().getDisplayName().replaceAll("§.", "").split(" ")[0].trim();
                // paper pay
                player.closeInventory();
                payOne(player, id);
                return;
            }
            if (cur.getType() == Material.BEDROCK) {
                String name = cur.getItemMeta().getDisplayName();
                int val = Integer.parseInt(name.replaceAll("[^0-9]", ""));
                openRepayGui(player, val);
            } else if (cur.getType() == Material.COMPASS) {
                player.closeInventory();
                openCustomAnvil(player);
            } else if (cur.getType() == Material.BARRIER) player.closeInventory();
        } else {
            Integer pending = pendingAmt.get(player.getUniqueId());
            if (pending == null) { player.closeInventory(); return; }
            int amtVal = pending;
            if (cur.getType() == Material.CLOCK) {
                String dn = cur.getItemMeta().getDisplayName();
                int days = Integer.parseInt(dn.replaceAll("[^0-9]", ""));
                createAndGive(player, amtVal, days, "LUMP");
                player.closeInventory();
            } else if (cur.getType() == Material.HOPPER) {
                createAndGive(player, amtVal, 0, "SLOW");
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        // keep pendingAmt until repayment picked or new /loan; no auto clear needed
    }

    private void createAndGive(Player player, int amount, int days, String mode) {
        double capDebt = totalDebt(player.getUniqueId());
        if (capDebt >= 10000) { player.sendMessage("§cToo much debt (max total $10000). Pay some first via /loan pay"); return; }
        if (capDebt + amount > 10000) { player.sendMessage("§cThat would exceed $10000 total debt cap!"); return; }
        JsonObject loan = newLoan(player.getUniqueId(), amount, days, mode);
        loans().add(loan);
        saveAll();
        balance.addBalance(player, amount);
        pendingAmt.remove(player.getUniqueId());
        String id = loan.get("id").getAsString();
        if (mode.equals("SLOW")) player.sendMessage("§aLoan §e$" + amount + " §agiven! ID §e" + id + " §a— Slow deduct 20% of earnings until paid. §7/loan pay " + id);
        else player.sendMessage("§aLoan §e$" + amount + " §agiven! ID §e" + id + " §aDue in §e" + days + "d§a. §7/loan pay " + id + " §7to repay (+$25 bonus if on time)");
    }

    private void payOne(Player player, String id) {
        JsonObject target = null;
        for (JsonElement el : loans()) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            if (o.get("id").getAsString().equalsIgnoreCase(id) && o.get("player").getAsString().equals(player.getUniqueId().toString()) && !o.get("repaid").getAsBoolean()) { target = o; break; }
        }
        if (target == null) { player.sendMessage("§cLoan id not found: " + id); return; }
        double debt = target.get("debt").getAsDouble();
        boolean overdue = !target.get("mode").getAsString().equals("SLOW") && target.get("due").getAsLong() < System.currentTimeMillis();
        // allow negative: directly manipulate balance via setBalance
        double bal = balance.getBalance(player);
        if (bal < debt) {
            // allow going negative after overdue or if player confirms? Spec says balance go to negatives if don't pay, so allow negative via loan pay
            // For manual pay we require funds, but overdue we allow negative
            if (!overdue) { player.sendMessage("§cNeed §e$" + Money.format(debt) + "§c you have §e$" + Money.format(bal)); return; }
        }
        // deduct (may go negative)
        double after = bal - debt;
        balance.setBalance(player.getUniqueId(), after);
        target.addProperty("repaid", true);
        target.addProperty("debt", 0.0);
        saveAll();
        if (!overdue) {
            balance.addBalance(player, REWARD);
            player.sendMessage("§aPaid loan §e" + id + " §a$" + Money.format(debt) + " + §e$25 bonus §afor on-time!");
        } else {
            player.sendMessage("§ePaid overdue loan §c" + id + " §f$" + Money.format(debt) + " §7(no bonus, was overdue)");
        }
    }

    private void payAll(Player player) {
        List<JsonObject> ls = playerLoans(player.getUniqueId());
        ls.sort((a,b)-> Double.compare(a.get("due").getAsLong(), b.get("due").getAsLong()));
        for (JsonObject o : new ArrayList<>(ls)) payOne(player, o.get("id").getAsString());
    }

    private ItemStack make(Material m, String name, String... lore) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length >0) {
                List<String> l = new ArrayList<>();
                for (String s : lore) l.add(s);
                meta.setLore(l);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Called by BalanceManager on each earning for SLOW loans */
    public double garnishSlow(Player player, double amount) {
        List<JsonObject> ls = playerLoans(player.getUniqueId());
        JsonObject slow = null;
        for (JsonObject o : ls) if (o.get("mode").getAsString().equals("SLOW")) { slow=o; break; }
        if (slow == null || amount <= 0) return amount;
        double debt = slow.get("debt").getAsDouble();
        if (debt <= 0) return amount;
        double cut = Math.round(Math.min(debt, amount * SLOW_RATE) * 100.0)/100.0;
        double kept = Math.round((amount - cut)*100.0)/100.0;
        double nextDebt = Math.round((debt - cut)*100.0)/100.0;
        slow.addProperty("debt", nextDebt);
        if (nextDebt <= 0.01) {
            slow.addProperty("repaid", true);
            slow.addProperty("debt", 0.0);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage("§aSlow loan repaid! +§e$25 bonus");
                    balance.addBalance(player, REWARD);
                }
            });
        } else {
            player.sendMessage("§6[Loan] §e$" + Money.format(cut) + " §7auto-paid to loan (remaining §c$" + Money.format(nextDebt) + "§7)");
        }
        saveAll();
        return kept;
    }

    private void startLateTask() {
        new BukkitRunnable() {
            @Override public void run() {
                long now = System.currentTimeMillis();
                boolean changed=false;
                for (JsonElement el : loans()) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    if (o.get("repaid").getAsBoolean()) continue;
                    if (o.get("mode").getAsString().equals("SLOW")) continue;
                    long due = o.get("due").getAsLong();
                    if (now <= due) continue;
                    long daysOver = (now - due) / 86400000;
                    if (daysOver < 1) daysOver=1;
                    // apply once per day: use stored lastLate
                    long last = o.has("lastLate") ? o.get("lastLate").getAsLong() : due;
                    long shouldDays = (now - due) / 86400000;
                    long doneDays = o.has("appliedDays") ? o.get("appliedDays").getAsLong() : 0;
                    if (shouldDays <= doneDays) continue;
                    long toApply = shouldDays - doneDays;
                    double debt = o.get("debt").getAsDouble();
                    double mult = Math.pow(1 + LATE_DAILY, toApply);
                    double next = Math.round(debt * mult * 100.0)/100.0;
                    o.addProperty("debt", next);
                    o.addProperty("appliedDays", shouldDays);
                    o.addProperty("lastLate", now);
                    changed=true;
                    try {
                        UUID pid = UUID.fromString(o.get("player").getAsString());
                        Player p = Bukkit.getPlayer(pid);
                        if (p != null) p.sendMessage("§c[Loan] §cOverdue " + o.get("id").getAsString() + " +5%/day → $" + Money.format(next));
                    } catch (Exception ignored) {}
                }
                if (changed) saveAll();
            }
        }.runTaskTimer(plugin, 1200L, 72000L); // every hour
    }
}
