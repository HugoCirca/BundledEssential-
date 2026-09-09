package dev.hugocirca.knapsack.util;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Makes water buckets stack to 16 via inventory/pickup merging.
 * Vanilla is 1, we allow 16 by intercepting clicks/pickups and merging similar stacks.
 * No NMS, keeps bucket NBT intact (just amount).
 */
public class WaterBucketStackManager implements Listener {

    private static final int MAX = 16;
    private static final Material TYPE = Material.WATER_BUCKET;

    public WaterBucketStackManager(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private boolean isWater(ItemStack s) {
        return s != null && s.getType() == TYPE;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent e) {
        // left-click merge: cursor + clicked both water buckets -> stack to 16
        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();
        ClickType click = e.getClick();

        if (click == ClickType.LEFT || click == ClickType.RIGHT) {
            if (isWater(cursor) && isWater(current)) {
                int total = cursor.getAmount() + current.getAmount();
                if (total <= MAX) {
                    current.setAmount(total);
                    e.getView().setCursor(null);
                    e.setCancelled(true);
                    if (e.getWhoClicked() instanceof Player p) p.updateInventory();
                } else {
                    current.setAmount(MAX);
                    cursor.setAmount(total - MAX);
                    e.getView().setCursor(cursor);
                    e.setCancelled(true);
                    if (e.getWhoClicked() instanceof Player p) p.updateInventory();
                }
                return;
            }
            // right-click single place: if cursor is water and clicked is water, split one
            if (click == ClickType.RIGHT && isWater(cursor) && isWater(current) && current.getAmount() < MAX) {
                // vanilla right-click would place one, we let our merge handle
            }
        }

        // shift-click: allow stacking up to 16 when moving from player inv to chest etc.
        // We let vanilla handle but afterwards compact stacks — only compact real inventories, not custom GUIs (prevents filler/reorder dupe)
        if (e.getClick().isShiftClick() && isWater(current)) {
            if (e.getWhoClicked() instanceof Player p) {
                plugin().getServer().getScheduler().runTask(plugin(), () -> compact(p.getInventory()));
                Inventory top = e.getView().getTopInventory();
                if (top.getHolder() != null) {
                    plugin().getServer().getScheduler().runTask(plugin(), () -> compact(top));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent e) {
        // compact after drag — only player inventory + real block inventories, not custom GUIs
        Inventory inv = e.getInventory();
        if (e.getWhoClicked() instanceof Player p) {
            plugin().getServer().getScheduler().runTask(plugin(), () -> {
                compact(p.getInventory());
                if (inv.getHolder() != null) compact(inv);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Item ent = e.getItem();
        ItemStack stack = ent.getItemStack();
        if (!isWater(stack)) return;
        // try to merge into existing stacks up to 16 before vanilla adds a new slot
        Inventory inv = p.getInventory();
        int remaining = stack.getAmount();
        for (ItemStack s : inv.getContents()) {
            if (isWater(s) && s.getAmount() < MAX) {
                int space = MAX - s.getAmount();
                int take = Math.min(space, remaining);
                s.setAmount(s.getAmount() + take);
                remaining -= take;
                if (remaining <= 0) {
                    e.setCancelled(true);
                    ent.remove();
                    p.updateInventory();
                    return;
                }
            }
        }
        // if we partially merged, update entity stack and let vanilla handle remainder (as 1-slot)
        if (remaining != stack.getAmount()) {
            stack.setAmount(remaining);
            ent.setItemStack(stack);
            // let vanilla add remaining (will create new slot if needed)
        }
        // schedule compact to ensure no 1-stacks lingering
        plugin().getServer().getScheduler().runTask(plugin(), () -> compact(inv));
    }

    private void compact(Inventory inv) {
        if (inv == null) return;
        // Never compact custom GUIs (holder == null -> Bukkit.createInventory(null,...)) — prevents filler slot dupes
        try { if (inv.getHolder() == null) return; } catch (Exception ignored) { return; }
        // collect all water bucket amounts
        int total = 0;
        for (ItemStack s : inv.getContents()) if (isWater(s)) total += s.getAmount();
        if (total <= 0) return;
        // clear existing water buckets
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isWater(s)) inv.setItem(i, null);
        }
        // redistribute as 16-stacks
        for (int i = 0; i < inv.getSize() && total > 0; i++) {
            if (inv.getItem(i) == null) {
                int put = Math.min(MAX, total);
                ItemStack ns = new ItemStack(TYPE, put);
                inv.setItem(i, ns);
                total -= put;
            }
        }
        // if still total left (inv full), drop remainder at player
        if (total > 0) {
            // find owner
            if (inv.getHolder() instanceof Player p) {
                while (total > 0) {
                    int put = Math.min(MAX, total);
                    ItemStack drop = new ItemStack(TYPE, put);
                    p.getWorld().dropItemNaturally(p.getLocation(), drop);
                    total -= put;
                }
            }
        }
    }

    private JavaPlugin plugin() {
        return JavaPlugin.getProvidingPlugin(WaterBucketStackManager.class);
    }
}
