package dev.hugocirca.knapsack.economy;

import dev.hugocirca.knapsack.level.LevelManager;
import dev.hugocirca.knapsack.common.Saveable;
import dev.hugocirca.knapsack.util.JsonStorage;
import dev.hugocirca.knapsack.util.Money;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class BalanceManager implements Listener, CommandExecutor, TabCompleter, Saveable {

    private final JavaPlugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path balancesFile;
    private final Path playtimeOptFile;
    private final JsonObject balances = new JsonObject();
    private final JsonObject playtimeOpt = new JsonObject();
    private final ServerBank serverBankService;
    private final Random random = new Random();
    private BountyManager bountyManager;
    private LevelManager levelManager;
    private dev.hugocirca.knapsack.loan.LoanManager loanManager;
    public void setLoanManager(dev.hugocirca.knapsack.loan.LoanManager lm) { this.loanManager = lm; }

    public static final double BALANCE_CAP = 1_000_000_000_000.0; // 1T fallback
    private static final double MAX_MOB_REWARD = 10.0;
    private static final double DEF_MIN_PLAYTIME_REWARD = 8.0;
    private static final double DEF_MAX_PLAYTIME_REWARD = 12.0;
    private static final long PLAYTIME_INTERVAL_TICKS = 6000L;

    // Scoreboard merge: remember last balance line per player to cleanly update merged boards.
    private final Map<UUID, String> lastBalanceLine = new HashMap<>();

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
        this.serverBankService = new ServerBank(plugin);
        loadBalances();
        loadPlaytimeOpt();
        startPlaytimeTask();
        startInterestTask();
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

    private static double clamp(double v) {
        // placeholder, real clamp uses config cap via instance method; static for compat
        if (v < -1_000_000) return -1_000_000;
        if (v > BALANCE_CAP) return BALANCE_CAP;
        return Math.round(v * 100.0) / 100.0;
    }

    private double cap() {
        try {
            double c = plugin.getConfig().getDouble("economy.balance-cap", BALANCE_CAP);
            if (c < 1000) c = BALANCE_CAP;
            return c;
        } catch (Exception e) {
            return BALANCE_CAP;
        }
    }

    public double getCapPublic() { return cap(); }

    private double clampCap(double v) {
        double c = cap();
        if (v < -1_000_000) return -1_000_000;
        if (v > c) return c;
        return Math.round(v * 100.0) / 100.0;
    }

    private void loadBalances() {
        JsonObject loaded = JsonStorage.load(plugin, "balances.json");
        if (loaded != null && loaded.size() > 0) {
            double c = cap();
            for (Map.Entry<String, com.google.gson.JsonElement> e : loaded.entrySet()) {
                try {
                    double v = e.getValue().getAsDouble();
                    if (v > c) v = c;
                    if (v < 0) v = 0;
                    balances.addProperty(e.getKey(), Math.round(v * 100.0) / 100.0);
                } catch (Exception ex) {
                    balances.add(e.getKey(), e.getValue());
                }
            }
        }
    }

    // Delegated to ServerBank service — keeps BalanceManager focused on player balances
    public double getServerBank() { return serverBankService.get(); }
    public void addServerBank(double amount) { serverBankService.add(amount); }
    public void addServerBank(double amount, String player, String reason) { serverBankService.add(amount, player, reason); }
    public void saveServerBank() { serverBankService.save(); }
    public void resetServerBank() { serverBankService.reset(); }
    public java.util.List<JsonObject> getBankHistory() { return serverBankService.history(); }
    public ItemStack historyBook() { return serverBankService.historyBook(); }

    public void saveBalances() {
        JsonStorage.save(plugin, "balances.json", balances);
    }

    @Override
    public void saveAll() { saveBalances(); savePlaytimeOpt(); saveServerBank(); }

    private void loadPlaytimeOpt() {
        JsonObject loaded = JsonStorage.load(plugin, "playtime_opt.json");
        if (loaded != null) loaded.entrySet().forEach(e -> playtimeOpt.add(e.getKey(), e.getValue()));
    }

    public void savePlaytimeOpt() {
        JsonStorage.save(plugin, "playtime_opt.json", playtimeOpt);
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
        amount = clampCap(amount);
        balances.addProperty(getBalanceKey(player), amount);
        saveBalances();
        updateScoreboard(player);
    }

    public void setBalance(UUID uuid, double amount) {
        amount = clampCap(amount);
        String key = uuid.toString();
        balances.addProperty(key, amount);
        saveBalances();
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) updateScoreboard(p);
    }

    public void addBalance(Player player, double amount) {
        double c = cap();
        double cur = getBalance(player);
        double next = cur + amount;
        double overflow = 0;
        if (next > c) {
            overflow = next - c;
            next = c;
        }
        if (overflow > 0) {
            addServerBank(Math.round(overflow * 100.0) / 100.0, player.getName(), "cap overflow");
            player.sendMessage("§6[Bank] §eCapped at §a$" + Money.format(c) + "§e! §a$" + Money.format(overflow) + " §7went to the Server Bank.");
        }
        if (amount > 0 && next <= cur && overflow <= 0) {
            // already at cap with no overflow? still cap msg
            player.sendMessage("§cBalance capped at §e$" + Money.format(c) + "§c — earnings now feed the Server Bank!");
            addServerBank(Math.round(amount * 100.0) / 100.0, player.getName(), "cap feed");
            return;
        }
        addBalance(player.getUniqueId(), amount, false);
        updateScoreboard(player);
    }

    /** Offline-safe credit (no scoreboard refresh — use for offline payouts). */
    public void addBalance(UUID uuid, double amount) {
        addBalance(uuid, amount, true);
    }

    private void addBalance(UUID uuid, double amount, boolean mayBank) {
        String key = uuid.toString();
        double current = balances.has(key) ? balances.get(key).getAsDouble() : 0.0;
        double c = cap();
        double next = current + amount;
        double overflow = 0;
        if (next > c) {
            overflow = next - c;
            next = c;
        }
        // allow negative via loans: clamp only upper, not lower 0 when debt-driven
        if (next < -1_000_000) next = -1_000_000; // sanity floor 1M negative
        next = Math.round(next * 100.0) / 100.0;
        balances.addProperty(key, next);
        saveBalances();
        if (mayBank && overflow > 0) {
            String pname = "SYSTEM";
            try { pname = Bukkit.getOfflinePlayer(uuid).getName(); if (pname==null) pname=uuid.toString().substring(0,8); } catch (Exception ignored) {}
            addServerBank(Math.round(overflow * 100.0) / 100.0, pname, "cap overflow");
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage("§6[Bank] §a$" + Money.format(overflow) + " §7overflow went to the Server Bank (cap $" + Money.format(c) + ").");
        } else if (overflow > 0) {
            // caller already banked, don't double
        }
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
                    if (loanManager != null) reward = loanManager.garnishSlow(player, reward);
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
        // Check if scoreboard is disabled in config
        boolean enabled = true;
        try { enabled = plugin.getConfig().getBoolean("economy.scoreboard.enabled", true); } catch (Exception ignored) {}
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = player.getScoreboard();
        boolean isMain = board == manager.getMainScoreboard();

        if (!enabled) {
            String old = lastBalanceLine.remove(player.getUniqueId());
            if (old != null) {
                try { board.resetScores(old); } catch (Exception ignored) {}
            }
            // If we own the sidebar, unregister it and revert to main if we created a new board
            Objective eb = board.getObjective("ebalance");
            if (eb != null && eb.getDisplaySlot() == DisplaySlot.SIDEBAR) {
                try { eb.unregister(); } catch (Exception ignored) {}
                if (!isMain && board.getObjective(DisplaySlot.SIDEBAR) == null && board.getObjectives().isEmpty()) {
                    try { player.setScoreboard(manager.getMainScoreboard()); } catch (Exception ignored) {}
                }
            }
            return;
        }

        boolean merge = true;
        try { merge = plugin.getConfig().getBoolean("economy.scoreboard.merge", true); } catch (Exception ignored) {}

        // Don't pollute the global main scoreboard — create a new one if player is on main
        if (isMain) {
            board = manager.getNewScoreboard();
        }

        Objective sidebar = board.getObjective(DisplaySlot.SIDEBAR);
        Objective obj = board.getObjective("ebalance");

        if (obj == null) {
            if (merge && sidebar != null && sidebar.getName() != null && !sidebar.getName().equals("ebalance")) {
                // Merge: reuse their existing sidebar objective
                obj = sidebar;
            } else {
                // No merge or no existing sidebar — we own it
                if (sidebar != null && !merge && sidebar.getName() != null && !sidebar.getName().equals("ebalance")) {
                    try { sidebar.unregister(); } catch (Exception ignored) {}
                    sidebar = null;
                }
                try {
                    obj = board.registerNewObjective("ebalance", Criteria.DUMMY, "§6§lE-balance");
                    obj.setDisplaySlot(DisplaySlot.SIDEBAR);
                    hideSidebarNumbers(obj);
                } catch (IllegalArgumentException e) {
                    // Already exists (race) — fetch it
                    obj = board.getObjective("ebalance");
                    if (obj == null) obj = board.getObjective(DisplaySlot.SIDEBAR);
                }
            }
        }

        if (obj == null) return;

        boolean weOwn = "ebalance".equals(obj.getName());
        if (weOwn) {
            try { obj.setDisplayName("§6§lE-balance"); } catch (Exception ignored) {}
            hideSidebarNumbers(obj);
        }

        // Remove previous balance line to update cleanly
        String oldLine = lastBalanceLine.get(player.getUniqueId());
        if (oldLine != null) {
            try { board.resetScores(oldLine); } catch (Exception ignored) {}
        }
        String newLine = "§fMoney: §a$" + Money.format(getBalance(player));
        lastBalanceLine.put(player.getUniqueId(), newLine);
        obj.getScore(newLine).setScore(2);

        if (weOwn) {
            // Only add our filler lines when we own the objective — don't pollute their board
            try { board.resetScores(" "); } catch (Exception ignored) {}
            try { board.resetScores("§7Kill mobs & play"); } catch (Exception ignored) {}
            obj.getScore(" ").setScore(1);
            obj.getScore("§7Kill mobs & play").setScore(0);
        }

        if (board != player.getScoreboard()) {
            try { player.setScoreboard(board); } catch (Exception ignored) {}
        }
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

    private void startInterestTask() {
        // Every ~20 min, 30% chance Bank pays interest to online players
        new BukkitRunnable() {
            @Override public void run() {
                try {
                    if (serverBankService.get() < 1000 || Bukkit.getOnlinePlayers().isEmpty()) return;
                    if (random.nextDouble() > 0.30) return; // 30% chance
                    double pct = 0.002 + random.nextDouble() * 0.003; // 0.2% - 0.5%
                    double payout = Math.round(serverBankService.get() * pct * 100.0) / 100.0;
                    payout = Math.min(payout, 5000); // cap per round to avoid hyperinflation
                    if (payout < 10) return;
                    List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                    // filter out capped players? they still get but feeds bank, so skip capped to avoid loop
                    double cap = cap();
                    online.removeIf(p -> getBalance(p) >= cap);
                    if (online.isEmpty()) return;
                    Collections.shuffle(online);
                    double per = Math.floor(payout / online.size() * 100.0) / 100.0;
                    double rem = Math.round((payout - per * online.size()) * 100.0) / 100.0;
                    double total = 0;
                    for (int i = 0; i < online.size(); i++) {
                        Player p = online.get(i);
                        double give = per + (i == 0 ? rem : 0);
                        if (give <= 0) continue;
                        addBalance(p, give);
                        total += give;
                        p.sendMessage("§6[Bank Interest] §a+$" + Money.format(give) + " §7from Server Bank!");
                    }
                    if (total > 0) {
                        serverBankService.deduct(total, "interest payout " + Money.format(total), online.size());
                        Bukkit.broadcastMessage("§6[Bank] §eInterest payout §a$" + Money.format(total) + " §7split to " + online.size() + " players!");
                        plugin.getLogger().info("Bank interest " + Money.format(total) + " to " + online.size() + " players, bank now " + Money.format(serverBankService.get()));
                    }
                } catch (Exception ignored) {}
            }
        }.runTaskTimer(plugin, 24000L, 24000L);
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
        lastBalanceLine.remove(id);
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
                    if (p != null && loanManager != null) kept = loanManager.garnishSlow(p, kept);
                    if (p != null) {
                        addBalance(p, kept);
                        p.sendMessage("§a[Kill] §e+$" + Money.format(kept) + " §7(split " + party.size() + " ways)");
                    } else {
                        addBalance(id, kept);
                    }
                }
            } else {
                if (bountyManager != null) reward = bountyManager.garnish(killer, reward);
                if (loanManager != null) reward = loanManager.garnishSlow(killer, reward);
                addBalance(killer, reward);
                killer.sendMessage("§a[Kill] §e+$" + Money.format(reward));
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("resetbal")) {
            if (!sender.isOp() && !sender.hasPermission("knapsack.admin") && !sender.hasPermission("knapsack.resetbal")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length < 1 || args.length > 2) {
                sender.sendMessage("§cUsage: /resetbal <player> [amount]");
                sender.sendMessage("§7No amount = reset to $0. With amount = set to that (max 1T).");
                return true;
            }
            String targetName = args[0];
            double amount = 0.0;
            boolean hasAmount = args.length == 2;
            if (hasAmount) {
                try {
                    amount = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cAmount must be a number!");
                    return true;
                }
                if (amount < 0) {
                    sender.sendMessage("§cAmount must be >= 0!");
                    return true;
                }
                amount = clamp(amount);
            }
            // Resolve target (online first, then offline via Mojang cache / balances file)
            Player online = Bukkit.getPlayerExact(targetName);
            UUID targetId = null;
            String displayName = targetName;
            if (online != null) {
                targetId = online.getUniqueId();
                displayName = online.getName();
            } else {
                // Try offline lookup
                try {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
                    if (off != null && off.getUniqueId() != null) {
                        // hasPlayedBefore or already has a balance entry counts as known
                        if (off.hasPlayedBefore() || off.isOnline() || balances.has(off.getUniqueId().toString())) {
                            targetId = off.getUniqueId();
                            if (off.getName() != null) displayName = off.getName();
                        } else {
                            // Fallback: search balances keys by matching last known? If no record, treat as not found
                            // Also check playtime names cache via Bukkit offline?
                            // Allow creating anyway if they typed a valid name but never joined? Warn.
                            targetId = off.getUniqueId();
                            if (off.getName() != null) displayName = off.getName();
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (targetId == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }
            setBalance(targetId, amount);
            if (hasAmount) {
                sender.sendMessage("§aSet §e" + displayName + "§a's balance to §e$" + Money.format(amount) + " §7(capped 1T)");
                Player tp = Bukkit.getPlayer(targetId);
                if (tp != null) tp.sendMessage("§eYour balance was set to §a$" + Money.format(amount) + " §eby " + sender.getName());
            } else {
                sender.sendMessage("§aReset §e" + displayName + "§a's balance to §e$0.00");
                Player tp = Bukkit.getPlayer(targetId);
                if (tp != null) tp.sendMessage("§cYour balance was reset to $0 by " + sender.getName());
            }
            return true;
        }
        // /balance
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can use this command! §7/balance <player>");
                return true;
            }
            player.sendMessage("§6Your balance: §a$" + Money.format(getBalance(player)) + " §7(cap 1T)");
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                // offline fallback
                try {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer off = Bukkit.getOfflinePlayer(args[0]);
                    if (off != null && balances.has(off.getUniqueId().toString())) {
                        sender.sendMessage("§6" + args[0] + "'s balance: §a$" + Money.format(getBalance(off.getUniqueId())) + " §7(cap 1T)");
                        return true;
                    }
                } catch (Exception ignored) {}
                sender.sendMessage("§cPlayer not found or offline!");
                return true;
            }
            sender.sendMessage("§6" + target.getName() + "'s balance: §a$" + Money.format(getBalance(target)) + " §7(cap 1T)");
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        java.util.List<String> out = new java.util.ArrayList<>();
        String name = command.getName().toLowerCase();
        if (name.equals("balance")) {
            if (args.length == 1) {
                for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            }
        } else if (name.equals("resetbal")) {
            if (args.length == 1) {
                for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            } else if (args.length == 2) {
                out.add("0");
                out.add("1000");
                out.add("1000000");
            }
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        out.removeIf(s -> !s.toLowerCase().startsWith(last));
        return out;
    }
}
