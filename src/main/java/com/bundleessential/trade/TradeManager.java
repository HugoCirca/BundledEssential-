package com.bundleessential.trade;

import com.bundleessential.BundledEssential;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TradeManager implements CommandExecutor, Listener {

    private final BundledEssential plugin;
    private final Map<UUID, UUID> pendingTrades = new HashMap<>();
    private final Map<UUID, Trade> activeTrades = new HashMap<>();
    private static final long REQUEST_EXPIRE_TICKS = 600L;

    // Left offer (player A), right offer (player B), column 4 is the divider
    private static final int[] LEFT_SLOTS = {0,1,2,3, 9,10,11,12, 18,19,20,21, 27,28,29,30};
    private static final int[] RIGHT_SLOTS = {5,6,7,8, 14,15,16,17, 23,24,25,26, 32,33,34,35};
    private static final int[] DIVIDER_SLOTS = {4,13,22,31,40,49};
    private static final int[] FILLER_SLOTS = {37,38,42,43, 45,46,47,48,50,51,52,53};
    private static final int HEAD_A = 36;
    private static final int ACCEPT_A = 39;
    private static final int CANCEL = 40;
    private static final int ACCEPT_B = 41;
    private static final int HEAD_B = 44;

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
        if (activeTrades.containsKey(sender.getUniqueId()) || activeTrades.containsKey(target.getUniqueId())) {
            sender.sendMessage("§cOne of you is already in a trade!");
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
        if (activeTrades.containsKey(senderId) || activeTrades.containsKey(targetId)) {
            target.sendMessage("§cOne of you is already in a trade!");
            return;
        }

        openTradeGui(sender, target);
    }

    private void handleTradeCancel(Player sender) {
        UUID senderId = sender.getUniqueId();

        Trade trade = activeTrades.get(senderId);
        if (trade != null) {
            cancelTrade(trade, "§cTrade cancelled by §e" + sender.getName() + "§c.");
            return;
        }

        if (pendingTrades.containsValue(senderId)) {
            pendingTrades.values().removeIf(id -> id.equals(senderId));
            sender.sendMessage("§cYour pending trade requests have been cancelled.");
        } else {
            sender.sendMessage("§cYou have no pending trade requests to cancel.");
        }
    }

    private void openTradeGui(Player a, Player b) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lTrade §e" + a.getName() + " §7<-> §e" + b.getName());

        ItemStack divider = named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot : DIVIDER_SLOTS) inv.setItem(slot, divider);
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot : FILLER_SLOTS) inv.setItem(slot, filler);

        Trade trade = new Trade(a.getUniqueId(), b.getUniqueId(), inv);
        activeTrades.put(a.getUniqueId(), trade);
        activeTrades.put(b.getUniqueId(), trade);
        refreshChrome(trade);

        a.openInventory(inv);
        b.openInventory(inv);
        a.sendMessage("§aTrade opened! Put your items on §eyour side §a(left).");
        b.sendMessage("§aTrade opened! Put your items on §eyour side §a(right).");
    }

    private void refreshChrome(Trade trade) {
        Player a = Bukkit.getPlayer(trade.a);
        Player b = Bukkit.getPlayer(trade.b);
        String nameA = a != null ? a.getName() : "???";
        String nameB = b != null ? b.getName() : "???";

        trade.inv.setItem(HEAD_A, head(a, "§e§l" + nameA, trade.aAccepted ? "§aAccepted ✓" : "§7Choosing items..."));
        trade.inv.setItem(HEAD_B, head(b, "§e§l" + nameB, trade.bAccepted ? "§aAccepted ✓" : "§7Choosing items..."));
        trade.inv.setItem(ACCEPT_A, named(Material.GREEN_STAINED_GLASS_PANE,
                trade.aAccepted ? "§a§lACCEPTED (click to undo)" : "§a§lClick to Accept",
                "§7" + nameA + "'s confirm button"));
        trade.inv.setItem(ACCEPT_B, named(Material.GREEN_STAINED_GLASS_PANE,
                trade.bAccepted ? "§a§lACCEPTED (click to undo)" : "§a§lClick to Accept",
                "§7" + nameB + "'s confirm button"));
        trade.inv.setItem(CANCEL, named(Material.RED_STAINED_GLASS_PANE, "§c§lCancel Trade", "§7Returns everyone's items"));
    }

    private ItemStack head(Player owner, String name, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            if (owner != null) meta.setOwningPlayer(owner);
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String l : lore) loreList.add(l);
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String l : lore) loreList.add(l);
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isLeft(int slot) {
        for (int s : LEFT_SLOTS) if (s == slot) return true;
        return false;
    }

    private boolean isRight(int slot) {
        for (int s : RIGHT_SLOTS) if (s == slot) return true;
        return false;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Trade trade = activeTrades.get(player.getUniqueId());
        if (trade == null || trade.finished) return;
        if (!event.getView().getTopInventory().equals(trade.inv)) return;

        // Only own offer slots may be modified, and only with plain clicks.
        // Shift-click, number keys and double-click collect could pull from the other side.
        if (event.getRawSlot() >= trade.inv.getSize()) {
            if (event.getAction().name().contains("MOVE_TO_OTHER_INVENTORY")
                    || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
            }
            return;
        }

        int slot = event.getSlot();
        boolean isA = player.getUniqueId().equals(trade.a);
        boolean ownSide = isA ? isLeft(slot) : isRight(slot);

        if (ownSide) {
            switch (event.getAction()) {
                case PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE,
                     PLACE_ALL, PLACE_SOME, PLACE_ONE,
                     DROP_ALL_SLOT, DROP_ONE_SLOT, DROP_ALL_CURSOR, DROP_ONE_CURSOR,
                     SWAP_WITH_CURSOR -> {
                    // Allowed: any change resets both accepts (anti switch-scam)
                    if (trade.aAccepted || trade.bAccepted) {
                        trade.aAccepted = false;
                        trade.bAccepted = false;
                        refreshChrome(trade);
                        trade.message("§eTrade offer changed — both players must accept again.");
                    }
                    return; // let vanilla handle the item move
                }
                default -> {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        event.setCancelled(true);

        if (slot == CANCEL) {
            cancelTrade(trade, "§cTrade cancelled by §e" + player.getName() + "§c.");
            return;
        }

        if ((isA && slot == ACCEPT_A) || (!isA && slot == ACCEPT_B)) {
            if (isA) trade.aAccepted = !trade.aAccepted;
            else trade.bAccepted = !trade.bAccepted;
            refreshChrome(trade);
            if (trade.aAccepted && trade.bAccepted) {
                completeTrade(trade);
            } else {
                trade.message("§e" + player.getName() + (isA ? (trade.aAccepted ? " §aaccepted." : " §7un-accepted.")
                        : (trade.bAccepted ? " §aaccepted." : " §7un-accepted.")));
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Trade trade = activeTrades.get(player.getUniqueId());
        if (trade == null || trade.finished) return;
        if (!event.getView().getTopInventory().equals(trade.inv)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Trade trade = activeTrades.get(player.getUniqueId());
        if (trade == null || trade.finished) return;
        if (!event.getInventory().equals(trade.inv)) return;
        cancelTrade(trade, "§cTrade closed — items returned.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Trade trade = activeTrades.get(event.getPlayer().getUniqueId());
        if (trade == null || trade.finished) return;
        cancelTrade(trade, "§cTrade ended — a player left. Items returned.");
    }

    private void completeTrade(Trade trade) {
        trade.finished = true;
        Player a = Bukkit.getPlayer(trade.a);
        Player b = Bukkit.getPlayer(trade.b);

        List<ItemStack> left = takeAll(trade.inv, LEFT_SLOTS);
        List<ItemStack> right = takeAll(trade.inv, RIGHT_SLOTS);
        clearAll(trade.inv);

        activeTrades.remove(trade.a);
        activeTrades.remove(trade.b);

        if (a != null && a.isOnline()) {
            giveAll(a, right);
            a.closeInventory();
            a.sendMessage("§a§lTrade complete!");
            a.playSound(a.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        }
        if (b != null && b.isOnline()) {
            giveAll(b, left);
            b.closeInventory();
            b.sendMessage("§a§lTrade complete!");
            b.playSound(b.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        }
    }

    private void cancelTrade(Trade trade, String reason) {
        if (trade.finished) return;
        trade.finished = true;

        List<ItemStack> left = takeAll(trade.inv, LEFT_SLOTS);
        List<ItemStack> right = takeAll(trade.inv, RIGHT_SLOTS);
        clearAll(trade.inv);

        activeTrades.remove(trade.a);
        activeTrades.remove(trade.b);

        Player a = Bukkit.getPlayer(trade.a);
        Player b = Bukkit.getPlayer(trade.b);
        returnAll(a, b, left);
        returnAll(b, a, right);

        if (a != null && a.isOnline()) {
            a.closeInventory();
            a.sendMessage(reason);
        }
        if (b != null && b.isOnline()) {
            b.closeInventory();
            b.sendMessage(reason);
        }
    }

    private List<ItemStack> takeAll(Inventory inv, int[] slots) {
        List<ItemStack> out = new ArrayList<>();
        for (int slot : slots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) out.add(item.clone());
        }
        return out;
    }

    private void clearAll(Inventory inv) {
        for (int s : LEFT_SLOTS) inv.setItem(s, null);
        for (int s : RIGHT_SLOTS) inv.setItem(s, null);
    }

    private void giveAll(Player player, List<ItemStack> items) {
        if (items.isEmpty()) return;
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(items.toArray(new ItemStack[0]));
        for (ItemStack rest : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
    }

    private void returnAll(Player owner, Player fallback, List<ItemStack> items) {
        if (items.isEmpty()) return;
        if (owner != null && owner.isOnline()) {
            giveAll(owner, items);
            return;
        }
        if (fallback != null && fallback.isOnline()) {
            giveAll(fallback, items);
            fallback.sendMessage("§eSome items were dropped at your feet (owner offline).");
        }
    }

    private static class Trade {
        final UUID a;
        final UUID b;
        final Inventory inv;
        boolean aAccepted = false;
        boolean bAccepted = false;
        boolean finished = false;

        Trade(UUID a, UUID b, Inventory inv) {
            this.a = a;
            this.b = b;
            this.inv = inv;
        }

        void message(String msg) {
            Player pa = Bukkit.getPlayer(a);
            Player pb = Bukkit.getPlayer(b);
            if (pa != null && pa.isOnline()) pa.sendMessage(msg);
            if (pb != null && pb.isOnline()) pb.sendMessage(msg);
        }
    }
}
