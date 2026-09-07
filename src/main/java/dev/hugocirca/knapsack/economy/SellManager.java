package dev.hugocirca.knapsack.economy;

import dev.hugocirca.knapsack.util.Money;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SellManager implements CommandExecutor, Listener {

    private final BalanceManager balanceManager;
    private final PriceManager priceManager;
    private static final String SELL_GUI_TITLE = "§6§lSell Shop";

    public SellManager(BalanceManager balanceManager, PriceManager priceManager) {
        this.balanceManager = balanceManager;
        this.priceManager = priceManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "sell" -> handleSell(player);
            case "sellgui" -> openSellGui(player);
        }
        return true;
    }

    private void handleSell(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§cYou are not holding anything!");
            return;
        }

        int amount = item.getAmount();
        double pricePerItem = priceManager.getSellPriceWithEnchants(item);
        double total = Math.round(pricePerItem * amount * 100.0) / 100.0;

        balanceManager.addBalance(player, total);
        player.getInventory().setItemInMainHand(null);

        player.sendMessage("§aSold §e" + amount + "x " + formatMaterialName(item.getType()) + " §afor §e$" + Money.format(total));
        if (priceManager.getEnchantmentBonus(item) > 0) {
            player.sendMessage("§7(Enchantment bonus: §a+$" + Money.format(priceManager.getEnchantmentBonus(item) * amount) + "§7)");
        }
    }

    public void openSellGui(Player player) {
        Inventory sellGui = Bukkit.createInventory(null, 54, SELL_GUI_TITLE);

        ItemStack info = makeItem(Material.EMERALD_BLOCK, "§a§lHow to sell",
                "§7Put items in any slot",
                "§7Close to sell everything",
                "§7Shift-click to sell all of one item",
                "§7Enchantments give bonus money!");
        sellGui.setItem(49, info);

        player.openInventory(sellGui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(SELL_GUI_TITLE)) return;

        // Always cancel by default, then allow only safe actions
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        ClickType click = event.getClick();

        // Block dangerous clicks that could dupe: number key, swap offhand, drop, etc. are already cancelled above
        if (click == ClickType.NUMBER_KEY || click == ClickType.SWAP_OFFHAND || click == ClickType.DROP || click == ClickType.CONTROL_DROP || click == ClickType.DOUBLE_CLICK) {
            return;
        }

        // Clicking info block does nothing
        if (clicked != null && clicked.getType() == Material.EMERALD_BLOCK) return;
        if (cursor != null && cursor.getType() == Material.EMERALD_BLOCK) return;

        // Shift-click on item in sell GUI: sell all of that type from player inventory (not GUI)
        if ((click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) && clicked != null && clicked.getType() != Material.AIR) {
            // count includes enchanted variants? Use type match only for now
            int count = 0;
            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem != null && invItem.getType() == clicked.getType()) {
                    count += invItem.getAmount();
                }
            }
            // also check offhand and armor? leggings might be equipped? Include armor
            for (ItemStack invItem : player.getInventory().getArmorContents()) {
                if (invItem != null && invItem.getType() == clicked.getType()) count += invItem.getAmount();
            }
            ItemStack off = player.getInventory().getItemInOffHand();
            if (off != null && off.getType() == clicked.getType()) count += off.getAmount();

            if (count > 0) {
                // Use actual items' prices (with enchants) for accurate total - sum each stack's price
                double total = 0;
                for (ItemStack invItem : player.getInventory().getContents()) {
                    if (invItem != null && invItem.getType() == clicked.getType()) {
                        total += priceManager.getSellPriceWithEnchants(invItem) * invItem.getAmount();
                    }
                }
                for (ItemStack invItem : player.getInventory().getArmorContents()) {
                    if (invItem != null && invItem.getType() == clicked.getType()) total += priceManager.getSellPriceWithEnchants(invItem) * invItem.getAmount();
                }
                if (off != null && off.getType() == clicked.getType()) total += priceManager.getSellPriceWithEnchants(off) * off.getAmount();
                total = Math.round(total * 100.0) / 100.0;
                // remove from all inventories
                player.getInventory().removeItem(new ItemStack(clicked.getType(), count));
                // also need to clear armor/offhand if they held it - removeItem above handles main, but armor needs manual
                // For armor leggings specifically, remove from armor if present
                ItemStack[] armor = player.getInventory().getArmorContents();
                for (int i=0;i<armor.length;i++) if (armor[i]!=null && armor[i].getType()==clicked.getType()) armor[i]=null;
                player.getInventory().setArmorContents(armor);
                if (off != null && off.getType() == clicked.getType()) player.getInventory().setItemInOffHand(null);
                if (total > 0) {
                    balanceManager.addBalance(player, total);
                    player.sendMessage("§aSold §e" + count + "x " + formatMaterialName(clicked.getType()) + " §afor §e$" + Money.format(total));
                }
                player.updateInventory();
            }
            return;
        }

        // Normal placement: allow placing cursor into empty sell slot, or picking back
        int slot = event.getSlot();
        int raw = event.getRawSlot();
        boolean inTop = raw < event.getView().getTopInventory().getSize();
        if (inTop) {
            if (slot == 49) return; // info slot
            // Allow pick up and place within sell GUI
            if (click == ClickType.LEFT || click == ClickType.RIGHT || click == ClickType.MIDDLE) {
                event.setCancelled(false);
            }
        } else {
            // Click in bottom (player inv) while sell GUI open: allow normal pickup, but shift is already handled
            event.setCancelled(false);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(SELL_GUI_TITLE)) return;
        // Only allow drags that stay within player inv or within sell GUI, not cross
        // For safety, cancel any drag that touches sell GUI top slots that would place
        // Check if any raw slot is in top inventory and is 49 (info)
        for (int raw : event.getRawSlots()) {
            if (raw == 49) { event.setCancelled(true); return; }
        }
        // allow otherwise but compact will handle; don't cancel to allow placing multiple
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(SELL_GUI_TITLE)) return;

        Inventory inventory = event.getInventory();
        double totalEarned = 0.0;
        int totalItems = 0;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            if (item.getType() == Material.EMERALD_BLOCK) continue;

            int amount = item.getAmount();
            double price = priceManager.getSellPriceWithEnchants(item);
            double itemTotal = Math.round(price * amount * 100.0) / 100.0;
            totalEarned += itemTotal;
            totalItems += amount;
        }

        if (totalItems > 0) {
            totalEarned = Math.round(totalEarned * 100.0) / 100.0;
            balanceManager.addBalance(player, totalEarned);
            player.sendMessage("§aSold §e" + totalItems + " items §afor §e$" + Money.format(totalEarned) + "§a!");
        }
        // Fix dupe: clear GUI and cursor so items don't return to player after credit
        inventory.clear();
        // also clear cursor if it holds a sellable item that was counted
        ItemStack cursor = event.getView().getCursor();
        if (cursor != null && cursor.getType() != Material.AIR && cursor.getType() != Material.EMERALD_BLOCK) {
            event.getView().setCursor(null);
        }
        player.updateInventory();
    }

    private String formatMaterialName(Material material) {
        String name = material.name().replace("_", " ").toLowerCase();
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
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
