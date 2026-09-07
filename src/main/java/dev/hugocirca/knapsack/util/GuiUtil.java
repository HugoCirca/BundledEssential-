package dev.hugocirca.knapsack.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** DRY for the ~6 copies of makeItem() scattered across managers. */
public final class GuiUtil {

    private GuiUtil() {}

    public static ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> lines = new ArrayList<>();
                for (String l : lore) lines.add(l);
                meta.setLore(lines);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack filler(Material material) {
        return item(material, " ");
    }

    public static void fill( org.bukkit.inventory.Inventory inv, Material material) {
        ItemStack f = filler(material);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, f);
        }
    }
}
