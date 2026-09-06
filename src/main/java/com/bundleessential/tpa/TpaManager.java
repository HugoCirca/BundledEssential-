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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TpaManager implements CommandExecutor {

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
        loadAutoAccept();
    }

    private void loadAutoAccept() {
        plugin.getDataFolder().mkdirs();
        try {
            if (Files.exists(autoFile)) {
                JsonArray arr = gson.fromJson(new String(Files.readAllBytes(autoFile)), JsonArray.class);
                if (arr != null) {
                    for (JsonElement el : arr) {
                        try {
                            autoAccept.add(UUID.fromString(el.getAsString()));
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load tpa.json");
        }
    }

    private void saveAutoAccept() {
        try {
            JsonArray arr = new JsonArray();
            for (UUID id : autoAccept) {
                arr.add(id.toString());
            }
            Files.write(autoFile, gson.toJson(arr).getBytes());
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
                if (args.length != 1) {
                    player.sendMessage("§cUsage: /tpa <player>");
                    return true;
                }
                handleTpa(player, args[0]);
            }
            case "tpaccept" -> handleTpAccept(player);
            case "tpahere" -> {
                if (args.length != 1) {
                    player.sendMessage("§cUsage: /tpahere <player>");
                    return true;
                }
                handleTpaHere(player, args[0]);
            }
            case "tpaautoaccept" -> handleAutoAccept(player, args);
        }
        return true;
    }

    private void handleAutoAccept(Player player, String[] args) {
        UUID id = player.getUniqueId();
        boolean enable;
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("on")) {
                enable = true;
            } else if (args[0].equalsIgnoreCase("off")) {
                enable = false;
            } else {
                player.sendMessage("§cUsage: /tpaautoaccept [on|off]");
                return;
            }
        } else {
            enable = !autoAccept.contains(id);
        }
        if (enable) {
            autoAccept.add(id);
            player.sendMessage("§aTPA auto-accept §lON§a! Requests teleport you instantly.");
        } else {
            autoAccept.remove(id);
            player.sendMessage("§eTPA auto-accept §lOFF§e. Use §6/tpaccept §emanually.");
        }
        saveAutoAccept();
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
        target.sendMessage("§e" + sender.getName() + " §awants to teleport to you. §6/tpaccept §7to accept.");

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
        target.sendMessage("§e" + sender.getName() + " §awants you to teleport to them. §6/tpaccept §7to accept.");

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
