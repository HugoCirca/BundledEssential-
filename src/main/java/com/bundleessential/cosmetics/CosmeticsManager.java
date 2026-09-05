package com.bundleessential.cosmetics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.bundleessential.economy.BalanceManager;
import com.bundleessential.util.Money;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CosmeticsManager implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final BalanceManager balanceManager;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path cosmeticsFile;
    private final JsonObject cosmeticsData = new JsonObject();
    private final Map<UUID, String> prefixes = new HashMap<>();
    private final Map<UUID, String> joinMessages = new HashMap<>();
    private final Map<UUID, String> deathMessages = new HashMap<>();
    private final Map<UUID, String> killEffects = new HashMap<>();
    private final Set<UUID> unlockedPrefix = new HashSet<>();
    private final Set<UUID> unlockedJoinDeath = new HashSet<>();
    private final Set<UUID> unlockedKillEffect = new HashSet<>();
    private final Map<UUID, String> pendingInput = new HashMap<>();

    private static final Map<String, String[]> PREFIX_OPTIONS = new LinkedHashMap<>();
    static {
        PREFIX_OPTIONS.put("white", new String[]{"§f", "500"});
        PREFIX_OPTIONS.put("yellow", new String[]{"§e", "500"});
        PREFIX_OPTIONS.put("aqua", new String[]{"§b", "500"});
        PREFIX_OPTIONS.put("green", new String[]{"§a", "500"});
        PREFIX_OPTIONS.put("red", new String[]{"§c", "500"});
        PREFIX_OPTIONS.put("light_purple", new String[]{"§d", "750"});
        PREFIX_OPTIONS.put("blue", new String[]{"§9", "750"});
        PREFIX_OPTIONS.put("gold", new String[]{"§6", "1000"});
        PREFIX_OPTIONS.put("dark_purple", new String[]{"§5", "1000"});
        PREFIX_OPTIONS.put("dark_aqua", new String[]{"§3", "1000"});
        PREFIX_OPTIONS.put("rainbow", new String[]{"§c§l", "2000"});
        PREFIX_OPTIONS.put("gradient", new String[]{"§6§l", "2000"});
    }

    private static final Map<String, String[]> KILL_EFFECTS = new LinkedHashMap<>();
    static {
        KILL_EFFECTS.put("lightning", new String[]{"Lightning", "1500"});
        KILL_EFFECTS.put("fire", new String[]{"Fire", "1000"});
        KILL_EFFECTS.put("smoke", new String[]{"Smoke", "800"});
        KILL_EFFECTS.put("heart", new String[]{"Hearts", "1200"});
        KILL_EFFECTS.put("note", new String[]{"Music Notes", "1000"});
    }

    public CosmeticsManager(JavaPlugin plugin, BalanceManager balanceManager) {
        this.plugin = plugin;
        this.balanceManager = balanceManager;
        this.cosmeticsFile = plugin.getDataFolder().toPath().resolve("cosmetics.json");
        loadCosmetics();
    }

    private void loadCosmetics() {
        plugin.getDataFolder().mkdirs();
        if (Files.exists(cosmeticsFile)) {
            try {
                String json = new String(Files.readAllBytes(cosmeticsFile));
                JsonObject loaded = gson.fromJson(json, JsonObject.class);
                if (loaded != null) {
                    if (loaded.has("prefixes")) {
                        loaded.getAsJsonObject("prefixes").entrySet().forEach(e -> {
                            prefixes.put(UUID.fromString(e.getKey()), e.getValue().getAsString());
                        });
                    }
                    if (loaded.has("joinMessages")) {
                        loaded.getAsJsonObject("joinMessages").entrySet().forEach(e -> {
                            joinMessages.put(UUID.fromString(e.getKey()), e.getValue().getAsString());
                        });
                    }
                    if (loaded.has("deathMessages")) {
                        loaded.getAsJsonObject("deathMessages").entrySet().forEach(e -> {
                            deathMessages.put(UUID.fromString(e.getKey()), e.getValue().getAsString());
                        });
                    }
                    if (loaded.has("killEffects")) {
                        loaded.getAsJsonObject("killEffects").entrySet().forEach(e -> {
                            killEffects.put(UUID.fromString(e.getKey()), e.getValue().getAsString());
                        });
                    }
                    if (loaded.has("unlockedPrefix")) {
                        loaded.getAsJsonArray("unlockedPrefix").forEach(e -> unlockedPrefix.add(UUID.fromString(e.getAsString())));
                    }
                    if (loaded.has("unlockedJoinDeath")) {
                        loaded.getAsJsonArray("unlockedJoinDeath").forEach(e -> unlockedJoinDeath.add(UUID.fromString(e.getAsString())));
                    }
                    if (loaded.has("unlockedKillEffect")) {
                        loaded.getAsJsonArray("unlockedKillEffect").forEach(e -> unlockedKillEffect.add(UUID.fromString(e.getAsString())));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load cosmetics.json");
            }
        }
    }

    public void saveCosmetics() {
        try {
            JsonObject data = new JsonObject();
            JsonObject prefixObj = new JsonObject();
            prefixes.forEach((k, v) -> prefixObj.addProperty(k.toString(), v));
            data.add("prefixes", prefixObj);
            JsonObject joinObj = new JsonObject();
            joinMessages.forEach((k, v) -> joinObj.addProperty(k.toString(), v));
            data.add("joinMessages", joinObj);
            JsonObject deathObj = new JsonObject();
            deathMessages.forEach((k, v) -> deathObj.addProperty(k.toString(), v));
            data.add("deathMessages", deathObj);
            JsonObject killObj = new JsonObject();
            killEffects.forEach((k, v) -> killObj.addProperty(k.toString(), v));
            data.add("killEffects", killObj);
            com.google.gson.JsonArray up = new com.google.gson.JsonArray();
            unlockedPrefix.forEach(u -> up.add(u.toString()));
            data.add("unlockedPrefix", up);
            com.google.gson.JsonArray ujd = new com.google.gson.JsonArray();
            unlockedJoinDeath.forEach(u -> ujd.add(u.toString()));
            data.add("unlockedJoinDeath", ujd);
            com.google.gson.JsonArray uke = new com.google.gson.JsonArray();
            unlockedKillEffect.forEach(u -> uke.add(u.toString()));
            data.add("unlockedKillEffect", uke);
            Files.write(cosmeticsFile, gson.toJson(data).getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save cosmetics.json");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length == 0) {
            openCosmeticsGui(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "prefix" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /cosmetics prefix <name|none>");
                    return true;
                }
                handlePrefix(player, args[1]);
            }
            case "joinmessage" -> {
                if (args.length < 2) {
                    openAnvilInput(player, "joinmessage");
                    return true;
                }
                String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                handleJoinMessage(player, msg);
            }
            case "deathmessage" -> {
                if (args.length < 2) {
                    openAnvilInput(player, "deathmessage");
                    return true;
                }
                String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                handleDeathMessage(player, msg);
            }
            case "killeffect" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /cosmetics killeffect <effect|none>");
                    return true;
                }
                handleKillEffect(player, args[1]);
            }
            default -> openCosmeticsGui(player);
        }
        return true;
    }

    private void openAnvilInput(Player player, String type) {
        if (!unlockedJoinDeath.contains(player.getUniqueId())) {
            player.sendMessage("§cYou don't own this cosmetic! Buy it from /cosmetics ($2000)");
            return;
        }

        Inventory anvil = Bukkit.createInventory(null, InventoryType.ANVIL, "§6§lEnter " + (type.equals("joinmessage") ? "Join" : "Death") + " Message");

        ItemStack input = new ItemStack(Material.PAPER);
        ItemMeta meta = input.getItemMeta();
        String current = type.equals("joinmessage") ? joinMessages.get(player.getUniqueId()) : deathMessages.get(player.getUniqueId());
        meta.setDisplayName(current != null ? current : "Type your message");
        input.setItemMeta(meta);
        anvil.setItem(0, input);

        ItemStack result = new ItemStack(Material.PAPER);
        ItemMeta resultMeta = result.getItemMeta();
        resultMeta.setDisplayName("§aClick to confirm");
        result.setItemMeta(resultMeta);
        anvil.setItem(2, result);

        pendingInput.put(player.getUniqueId(), type);
        player.openInventory(anvil);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // Anvil input
        if (title.startsWith("§6§lEnter ")) {
            event.setCancelled(true);
            if (event.getSlot() == 2) {
                ItemStack input = event.getView().getTopInventory().getItem(0);
                if (input != null && input.hasItemMeta() && input.getItemMeta().hasDisplayName()) {
                    String text = input.getItemMeta().getDisplayName();
                    String type = pendingInput.remove(player.getUniqueId());
                    if (type != null && !text.equals("Type your message")) {
                        if (type.equals("joinmessage")) {
                            handleJoinMessage(player, text);
                        } else if (type.equals("deathmessage")) {
                            handleDeathMessage(player, text);
                        }
                    }
                }
                player.closeInventory();
            }
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        // Main cosmetics shop
        if (title.equals("§6§lCosmetics Shop")) {
            event.setCancelled(true);
            switch (event.getSlot()) {
                case 10 -> openPrefixGui(player);
                case 12 -> {
                    if (unlockedJoinDeath.contains(player.getUniqueId())) {
                        player.sendMessage("§aYou already own this! Use /cosmetics joinmessage");
                    } else if (balanceManager.removeBalance(player, 2000)) {
                        unlockedJoinDeath.add(player.getUniqueId());
                        saveCosmetics();
                        player.sendMessage("§aYou unlocked Join/Death Messages! Use /cosmetics joinmessage");
                    } else {
                        player.sendMessage("§cNot enough money! Need $2000");
                    }
                }
                case 14 -> openKillEffectGui(player);
            }
            return;
        }

        // Prefix shop
        if (title.equals("§6§lPrefix Shop")) {
            event.setCancelled(true);
            if (event.getSlot() == 4) {
                openCosmeticsGui(player);
                return;
            }
            if (event.getSlot() >= 10 && event.getSlot() <= 15) {
                String code = new ArrayList<>(PREFIX_OPTIONS.keySet()).get(event.getSlot() - 10);
                double price = Double.parseDouble(PREFIX_OPTIONS.get(code)[1]);
                if (unlockedPrefix.contains(player.getUniqueId())) {
                    prefixes.put(player.getUniqueId(), code);
                    saveCosmetics();
                    player.sendMessage("§aPrefix set to §e" + code);
                } else if (balanceManager.removeBalance(player, price)) {
                    unlockedPrefix.add(player.getUniqueId());
                    prefixes.put(player.getUniqueId(), code);
                    saveCosmetics();
                    player.sendMessage("§aYou bought the §e" + code + " §aprefix!");
                } else {
                    player.sendMessage("§cNot enough money! Need $" + Money.format(price));
                }
            }
            return;
        }

        // Kill effect shop
        if (title.equals("§6§lKill Effect Shop")) {
            event.setCancelled(true);
            if (event.getSlot() == 4) {
                openCosmeticsGui(player);
                return;
            }
            if (event.getSlot() >= 10 && event.getSlot() <= 15) {
                String code = new ArrayList<>(KILL_EFFECTS.keySet()).get(event.getSlot() - 10);
                double price = Double.parseDouble(KILL_EFFECTS.get(code)[1]);
                if (unlockedKillEffect.contains(player.getUniqueId())) {
                    killEffects.put(player.getUniqueId(), code);
                    saveCosmetics();
                    player.sendMessage("§aKill effect set to §e" + code);
                } else if (balanceManager.removeBalance(player, price)) {
                    unlockedKillEffect.add(player.getUniqueId());
                    killEffects.put(player.getUniqueId(), code);
                    saveCosmetics();
                    player.sendMessage("§aYou bought the §e" + code + " §akill effect!");
                } else {
                    player.sendMessage("§cNot enough money! Need $" + Money.format(price));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals("§6§lCosmetics Shop") || title.equals("§6§lPrefix Shop") || title.equals("§6§lKill Effect Shop") || title.startsWith("§6§lEnter ")) {
            event.setCancelled(true);
        }
    }

    private void openCosmeticsGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6§lCosmetics Shop");
        gui.setItem(10, makeItem(Material.NAME_TAG, "§e§lChat Prefix", "§7Custom name color", "§7One-time purchase, permanent", "§eClick to browse"));
        gui.setItem(12, makeItem(Material.BOOK, "§e§lJoin Message", "§7Custom join message", "§7One-time purchase, permanent", "§eClick to browse"));
        gui.setItem(14, makeItem(Material.WITHER_SKELETON_SKULL, "§e§lKill Effect", "§7Effects on kills", "§7One-time purchase, permanent", "§eClick to browse"));
        ItemStack glass = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) { if (gui.getItem(i) == null) gui.setItem(i, glass); }
        player.openInventory(gui);
    }

    private void openPrefixGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6§lPrefix Shop");
        int slot = 10;
        for (Map.Entry<String, String[]> entry : PREFIX_OPTIONS.entrySet()) {
            if (slot >= 16) break;
            String code = entry.getKey();
            String display = entry.getValue()[0];
            double price = Double.parseDouble(entry.getValue()[1]);
            boolean owned = unlockedPrefix.contains(player.getUniqueId());
            String status = owned ? "§aOwned" : "§ePrice: $" + Money.format(price);
            gui.setItem(slot, makeItem(Material.NAME_TAG, display + code + " Prefix", status, owned ? "§7Click to set" : "§7Click to buy"));
            slot++;
        }
        gui.setItem(4, makeItem(Material.ARROW, "§cBack"));
        ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) { if (gui.getItem(i) == null) gui.setItem(i, glass); }
        player.openInventory(gui);
    }

    private void openKillEffectGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6§lKill Effect Shop");
        int slot = 10;
        for (Map.Entry<String, String[]> entry : KILL_EFFECTS.entrySet()) {
            if (slot >= 16) break;
            String code = entry.getKey();
            String name = entry.getValue()[0];
            double price = Double.parseDouble(entry.getValue()[1]);
            boolean owned = unlockedKillEffect.contains(player.getUniqueId());
            String status = owned ? "§aOwned" : "§ePrice: $" + Money.format(price);
            gui.setItem(slot, makeItem(Material.FIREWORK_ROCKET, "§e" + name, status, owned ? "§7Click to set" : "§7Click to buy"));
            slot++;
        }
        gui.setItem(4, makeItem(Material.ARROW, "§cBack"));
        ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) { if (gui.getItem(i) == null) gui.setItem(i, glass); }
        player.openInventory(gui);
    }

    private void handlePrefix(Player player, String code) {
        if (code.equalsIgnoreCase("none")) {
            prefixes.remove(player.getUniqueId());
            saveCosmetics();
            player.sendMessage("§aPrefix removed!");
            return;
        }
        if (!PREFIX_OPTIONS.containsKey(code)) {
            player.sendMessage("§cUnknown prefix. Options: " + String.join(", ", PREFIX_OPTIONS.keySet()));
            return;
        }
        if (!unlockedPrefix.contains(player.getUniqueId())) {
            player.sendMessage("§cYou don't own this prefix! Buy it from /cosmetics");
            return;
        }
        prefixes.put(player.getUniqueId(), code);
        saveCosmetics();
        player.sendMessage("§aPrefix set to §e" + code);
    }

    private void handleJoinMessage(Player player, String msg) {
        if (!unlockedJoinDeath.contains(player.getUniqueId())) {
            player.sendMessage("§cYou don't own this cosmetic! Buy it from /cosmetics ($2000)");
            return;
        }
        if (msg.equalsIgnoreCase("none")) {
            joinMessages.remove(player.getUniqueId());
            saveCosmetics();
            player.sendMessage("§aJoin message reset!");
            return;
        }
        joinMessages.put(player.getUniqueId(), msg);
        saveCosmetics();
        player.sendMessage("§aJoin message set to: §e" + msg);
    }

    private void handleDeathMessage(Player player, String msg) {
        if (!unlockedJoinDeath.contains(player.getUniqueId())) {
            player.sendMessage("§cYou don't own this cosmetic! Buy it from /cosmetics ($2000)");
            return;
        }
        if (msg.equalsIgnoreCase("none")) {
            deathMessages.remove(player.getUniqueId());
            saveCosmetics();
            player.sendMessage("§aDeath message reset!");
            return;
        }
        deathMessages.put(player.getUniqueId(), msg);
        saveCosmetics();
        player.sendMessage("§aDeath message set to: §e" + msg);
    }

    private void handleKillEffect(Player player, String code) {
        if (code.equalsIgnoreCase("none")) {
            killEffects.remove(player.getUniqueId());
            saveCosmetics();
            player.sendMessage("§aKill effect removed!");
            return;
        }
        if (!KILL_EFFECTS.containsKey(code)) {
            player.sendMessage("§cUnknown effect. Options: " + String.join(", ", KILL_EFFECTS.keySet()));
            return;
        }
        if (!unlockedKillEffect.contains(player.getUniqueId())) {
            player.sendMessage("§cYou don't own this effect! Buy it from /cosmetics");
            return;
        }
        killEffects.put(player.getUniqueId(), code);
        saveCosmetics();
        player.sendMessage("§aKill effect set to §e" + code);
    }

    public String getPrefix(Player player) {
        String code = prefixes.get(player.getUniqueId());
        if (code == null) return "";
        String[] parts = PREFIX_OPTIONS.get(code);
        if (parts == null) return "";
        return parts[0] + player.getName() + "§r";
    }

    public String getCustomJoinMessage(Player player) {
        return joinMessages.get(player.getUniqueId());
    }

    public String getCustomDeathMessage(Player player) {
        return deathMessages.get(player.getUniqueId());
    }

    public String getKillEffect(Player player) {
        return killEffects.get(player.getUniqueId());
    }

    public void playKillEffect(Player killer, Player victim) {
        String effect = killEffects.get(killer.getUniqueId());
        if (effect == null) return;
        org.bukkit.Location loc = victim.getLocation();
        switch (effect) {
            case "lightning" -> loc.getWorld().strikeLightningEffect(loc);
            case "fire" -> loc.getWorld().createExplosion(loc, 0F, false, false);
            case "smoke" -> {
                for (int i = 0; i < 10; i++) {
                    loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.01);
                }
            }
            case "heart" -> loc.getWorld().spawnParticle(org.bukkit.Particle.HEART, loc.clone().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0.01);
            case "note" -> loc.getWorld().spawnParticle(org.bukkit.Particle.NOTE, loc.clone().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 1.0);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String custom = joinMessages.get(player.getUniqueId());
        if (custom != null) {
            event.setJoinMessage("§a" + custom.replace("{player}", player.getName()));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String custom = deathMessages.get(player.getUniqueId());
        if (custom != null) {
            event.setDeathMessage("§c" + custom.replace("{player}", player.getName()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingInput.remove(event.getPlayer().getUniqueId());
    }

    private ItemStack makeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String l : lore) loreList.add(l);
            meta.setLore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }
}
