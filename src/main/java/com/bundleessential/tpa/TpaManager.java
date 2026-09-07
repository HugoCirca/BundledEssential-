package com.bundleessential.tpa;

import com.bundleessential.BundledEssential;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TpaManager implements CommandExecutor, TabCompleter {

    private final BundledEssential plugin;
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private final Map<UUID, Boolean> tpaHereRequests = new HashMap<>();
    private final Set<UUID> autoAccept = new HashSet<>();
    private static final long REQUEST_EXPIRE_TICKS = 600L;
    private final Gson gson = new GsonBuilder().create();
    private final Path autoFile;

    public TpaManager(BundledEssential plugin) {
        this.plugin = plugin;
        this.autoFile = plugin.getDataFolder().toPath().resolve("tpa.json");
        loadAuto();
    }

    private void loadAuto() {
        plugin.getDataFolder().mkdirs();
        try {
            if (Files.exists(autoFile)) {
                String raw = new String(Files.readAllBytes(autoFile));
                com.google.gson.JsonElement root = gson.fromJson(raw, com.google.gson.JsonElement.class);
                if (root != null && root.isJsonObject()) {
                    com.google.gson.JsonObject obj = root.getAsJsonObject();
                    JsonArray acc = obj.has("autoAccept") && obj.get("autoAccept").isJsonArray() ? obj.getAsJsonArray("autoAccept") : new JsonArray();
                    for (JsonElement el : acc) try { autoAccept.add(UUID.fromString(el.getAsString())); } catch (Exception ignored) {}
                    // legacy autoCancel ignored — removed, defaults to manual
                } else if (root != null && root.isJsonArray()) {
                    // legacy: plain array was autoAccept
                    JsonArray arr = root.getAsJsonArray();
                    for (JsonElement el : arr) try { autoAccept.add(UUID.fromString(el.getAsString())); } catch (Exception ignored) {}
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load tpa.json");
        }
    }

    private void saveAuto() {
        try {
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            JsonArray acc = new JsonArray();
            for (UUID id : autoAccept) acc.add(id.toString());
            obj.add("autoAccept", acc);
            Files.write(autoFile, gson.toJson(obj).getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save tpa.json");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "tpa" -> {
                if (args.length == 0) {
                    player.sendMessage("§cUsage: /tpa <player> | /tpa here <player> | /tpa accept | /tpa auto [on|off]");
                    return true;
                }
                String sub = args[0].toLowerCase();
                if (sub.equals("accept")) {
                    handleTpAccept(player);
                } else if (sub.equals("here")) {
                    if (args.length != 2) {
                        player.sendMessage("§cUsage: /tpa here <player>");
                        return true;
                    }
                    handleTpaHere(player, args[1]);
                } else if (sub.equals("auto")) {
                    String[] rest = args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[0];
                    handleAuto(player, rest);
                } else if (args.length == 1) {
                    handleTpa(player, args[0]);
                } else {
                    player.sendMessage("§cUsage: /tpa <player> | /tpa here <player> | /tpa accept | /tpa auto [on|off]");
                }
            }
            case "tpaccept" -> handleTpAccept(player);
            case "tpahere" -> {
                if (args.length != 1) {
                    player.sendMessage("§cUsage: /tpahere <player>");
                    return true;
                }
                handleTpaHere(player, args[0]);
            }
            case "tpaauto" -> handleAuto(player, args);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String name = command.getName().toLowerCase();
        if (name.equals("tpa")) {
            if (args.length == 1) {
                out.add("here");
                out.add("accept");
                out.add("auto");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (sender instanceof Player self && p.equals(self)) continue;
                    out.add(p.getName());
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("here")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (sender instanceof Player self && p.equals(self)) continue;
                    out.add(p.getName());
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("auto")) {
                out.add("on");
                out.add("off");
            }
        } else if (name.equals("tpahere")) {
            if (args.length == 1) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (sender instanceof Player self && p.equals(self)) continue;
                    out.add(p.getName());
                }
            }
        } else if (name.equals("tpaauto")) {
            if (args.length == 1) {
                out.add("on");
                out.add("off");
            }
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        out.removeIf(s -> !s.toLowerCase().startsWith(last));
        return out;
    }

    private void handleAuto(Player player, String[] args) {
        UUID id = player.getUniqueId();
        boolean enable;
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("on")) {
                enable = true;
            } else if (args[0].equalsIgnoreCase("off")) {
                enable = false;
            } else {
                player.sendMessage("§cUsage: /tpaauto [on|off]");
                return;
            }
        } else {
            enable = !autoAccept.contains(id);
        }
        if (enable) {
            autoAccept.add(id);
            player.sendMessage("§aTPA auto-accept §lON§a! Requests teleport instantly. Run §e/tpaauto §aagain for manual.");
        } else {
            autoAccept.remove(id);
            player.sendMessage("§eTPA auto-accept §lOFF§e. Use §6/tpa accept §emanually.");
        }
        saveAuto();
    }

    private void handleTpa(Player sender, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found or offline!");
            return;
        }
        if (target.equals(sender)) {
            sender.sendMessage("§cYou cannot send a TPA to yourself!");
            return;
        }

        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();

        if (autoAccept.contains(targetId)) {
            sender.sendMessage("§aTeleport request sent to §e" + target.getName() + "§a!");
            doTeleport(sender, target, false, true);
            return;
        }

        pendingRequests.put(targetId, senderId);
        tpaHereRequests.put(targetId, false);

        sender.sendMessage("§aTeleport request sent to §e" + target.getName() + "§a!");
        target.sendMessage("§e" + sender.getName() + " §awants to teleport to you. §6/tpa accept §7to accept.");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRequests.containsKey(targetId) && pendingRequests.get(targetId).equals(senderId)) {
                    pendingRequests.remove(targetId);
                    tpaHereRequests.remove(targetId);
                    if (sender.isOnline()) {
                        sender.sendMessage("§cYour teleport request to §e" + target.getName() + " §cexpired.");
                    }
                    if (target.isOnline()) {
                        target.sendMessage("§cThe teleport request from §e" + sender.getName() + " §cexpired.");
                    }
                }
            }
        }.runTaskLater(plugin, REQUEST_EXPIRE_TICKS);
    }

    private void handleTpaHere(Player sender, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found or offline!");
            return;
        }
        if (target.equals(sender)) {
            sender.sendMessage("§cYou cannot send a TPA here to yourself!");
            return;
        }

        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();

        if (autoAccept.contains(targetId)) {
            sender.sendMessage("§aRequest sent to §e" + target.getName() + " §a to teleport to you.");
            doTeleport(sender, target, true, true);
            return;
        }

        pendingRequests.put(targetId, senderId);
        tpaHereRequests.put(targetId, true);

        sender.sendMessage("§aRequest sent to §e" + target.getName() + " §a to teleport to you.");
        target.sendMessage("§e" + sender.getName() + " §awants you to teleport to them. §6/tpa accept §7to accept.");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRequests.containsKey(targetId) && pendingRequests.get(targetId).equals(senderId)) {
                    pendingRequests.remove(targetId);
                    tpaHereRequests.remove(targetId);
                    if (sender.isOnline()) {
                        sender.sendMessage("§cYour teleport request to §e" + target.getName() + " §cexpired.");
                    }
                    if (target.isOnline()) {
                        target.sendMessage("§cThe teleport request from §e" + sender.getName() + " §cexpired.");
                    }
                }
            }
        }.runTaskLater(plugin, REQUEST_EXPIRE_TICKS);
    }

    private void handleTpAccept(Player target) {
        UUID targetId = target.getUniqueId();

        if (!pendingRequests.containsKey(targetId)) {
            target.sendMessage("§cYou have no pending teleport requests!");
            return;
        }

        UUID senderId = pendingRequests.get(targetId);
        boolean isTpaHere = tpaHereRequests.getOrDefault(targetId, false);

        Player sender = Bukkit.getPlayer(senderId);
        pendingRequests.remove(targetId);
        tpaHereRequests.remove(targetId);

        if (sender == null) {
            target.sendMessage("§cThat player is no longer online!");
            return;
        }

        doTeleport(sender, target, isTpaHere, false);
    }

    /** Shared teleport for manual accepts and auto-accepts. */
    private void doTeleport(Player sender, Player target, boolean isTpaHere, boolean auto) {
        String tag = auto ? "§b[Auto] " : "";
        if (isTpaHere) {
            target.teleport(sender.getLocation());
            target.sendMessage(tag + "§aYou have been teleported to §e" + sender.getName() + "§a!");
            sender.sendMessage(tag + "§aTeleported §e" + target.getName() + " §ato you!");
        } else {
            sender.teleport(target.getLocation());
            sender.sendMessage(tag + "§aYou have been teleported to §e" + target.getName() + "§a!");
            target.sendMessage(tag + "§e" + sender.getName() + " §ahas accepted your teleport request!");
        }
    }
}
