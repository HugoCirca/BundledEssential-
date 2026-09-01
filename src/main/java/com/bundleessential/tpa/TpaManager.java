package com.bundleessential.tpa;

import com.bundleessential.BundledEssential;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaManager implements CommandExecutor {

    private final BundledEssential plugin;
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private final Map<UUID, Boolean> tpaHereRequests = new HashMap<>();
    private static final long REQUEST_EXPIRE_TICKS = 600L;

    public TpaManager(BundledEssential plugin) {
        this.plugin = plugin;
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
        }
        return true;
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

        if (isTpaHere) {
            target.teleport(sender.getLocation());
            target.sendMessage("§aYou have been teleported to §e" + sender.getName() + "§a!");
            sender.sendMessage("§aTeleported §e" + target.getName() + " §ato you!");
        } else {
            sender.teleport(target.getLocation());
            sender.sendMessage("§aYou have been teleported to §e" + target.getName() + "§a!");
            target.sendMessage("§e" + sender.getName() + " §ahas accepted your teleport request!");
        }
    }
}
