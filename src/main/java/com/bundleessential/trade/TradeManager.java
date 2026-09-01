package com.bundleessential.trade;

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

public class TradeManager implements CommandExecutor {

    private final BundledEssential plugin;
    private final Map<UUID, UUID> pendingTrades = new HashMap<>();
    private static final long REQUEST_EXPIRE_TICKS = 600L;

    public TradeManager(BundledEssential plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "trade" -> {
                if (args.length != 1) {
                    player.sendMessage("§cUsage: /trade <player>");
                    return true;
                }
                handleTrade(player, args[0]);
            }
            case "tradeaccept" -> handleTradeAccept(player);
            case "tradecancel" -> handleTradeCancel(player);
        }
        return true;
    }

    private void handleTrade(Player sender, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found or offline!");
            return;
        }
        if (target.equals(sender)) {
            sender.sendMessage("§cYou cannot send a trade request to yourself!");
            return;
        }

        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();

        pendingTrades.put(targetId, senderId);

        sender.sendMessage("§aTrade request sent to §e" + target.getName() + "§a!");
        target.sendMessage("§e" + sender.getName() + " §awants to trade with you. §6/tradeaccept §7to accept.");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingTrades.containsKey(targetId) && pendingTrades.get(targetId).equals(senderId)) {
                    pendingTrades.remove(targetId);
                    if (sender.isOnline()) {
                        sender.sendMessage("§cYour trade request to §e" + target.getName() + " §cexpired.");
                    }
                    if (target.isOnline()) {
                        target.sendMessage("§cThe trade request from §e" + sender.getName() + " §cexpired.");
                    }
                }
            }
        }.runTaskLater(plugin, REQUEST_EXPIRE_TICKS);
    }

    private void handleTradeAccept(Player target) {
        UUID targetId = target.getUniqueId();

        if (!pendingTrades.containsKey(targetId)) {
            target.sendMessage("§cYou have no pending trade requests!");
            return;
        }

        UUID senderId = pendingTrades.get(targetId);
        Player sender = Bukkit.getPlayer(senderId);
        pendingTrades.remove(targetId);

        if (sender == null) {
            target.sendMessage("§cThat player is no longer online!");
            return;
        }

        sender.sendMessage("§e" + target.getName() + " §aaccepted your trade request!");
        target.sendMessage("§aYou accepted the trade request from §e" + sender.getName() + "§a!");
    }

    private void handleTradeCancel(Player sender) {
        UUID senderId = sender.getUniqueId();

        if (pendingTrades.containsValue(senderId)) {
            pendingTrades.values().removeIf(id -> id.equals(senderId));
            sender.sendMessage("§cYour pending trade requests have been cancelled.");
        } else {
            sender.sendMessage("§cYou have no pending trade requests to cancel.");
        }
    }
}
