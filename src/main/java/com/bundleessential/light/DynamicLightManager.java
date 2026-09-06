package com.bundleessential.light;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Built-in dynamic lighting: held items with light properties
 * (torch, lantern, lava bucket, glowstone, end rod, ...) place a
 * real invisible Light block at the player's feet. No extra plugin needed.
 * Only AIR is ever replaced, and lights are removed on quit/disable.
 */
public class DynamicLightManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<Material, Integer> emission = new HashMap<>();
    private final Map<UUID, Tracked> lights = new HashMap<>();
    private final long interval;

    public DynamicLightManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.interval = Math.max(2L, plugin.getConfig().getLong("dynamic-light.interval-ticks", 10L));
        put("LANTERN", 15);
        put("TORCH", 14);
        put("JACK_O_LANTERN", 15);
        put("GLOWSTONE", 15);
        put("SHROOMLIGHT", 15);
        put("SEA_LANTERN", 15);
        put("OCHRE_FROGLIGHT", 15);
        put("VERDANT_FROGLIGHT", 15);
        put("PEARLESCENT_FROGLIGHT", 15);
        put("END_ROD", 14);
        put("LAVA_BUCKET", 15);
        put("REDSTONE_LAMP", 15);
        put("BEACON", 15);
        put("CONDUIT", 15);
        put("CAMPFIRE", 15);
        put("SOUL_LANTERN", 10);
        put("SOUL_TORCH", 10);
        put("SOUL_CAMPFIRE", 10);
        put("CRYING_OBSIDIAN", 10);
        put("REDSTONE_TORCH", 7);
        put("ENDER_CHEST", 7);
        put("GLOW_LICHEN", 7);
        put("AMETHYST_CLUSTER", 5);
        put("LARGE_AMETHYST_BUD", 4);
        put("MAGMA_BLOCK", 3);
        put("MEDIUM_AMETHYST_BUD", 2);
        put("BREWING_STAND", 1);
        put("SMALL_AMETHYST_BUD", 1);
        put("DRAGON_EGG", 1);
        startTask();
    }

    private void put(String name, int level) {
        try {
            Material m = Material.matchMaterial(name);
            if (m != null) emission.put(m, level);
        } catch (Exception ignored) {}
    }

    private int heldLevel(Player player) {
        int best = 0;
        try {
            best = Math.max(best, emission.getOrDefault(player.getInventory().getItemInMainHand().getType(), 0));
            best = Math.max(best, emission.getOrDefault(player.getInventory().getItemInOffHand().getType(), 0));
        } catch (Exception ignored) {}
        return Math.min(15, best);
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    try {
                        refresh(player);
                    } catch (Exception ignored) {}
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private void refresh(Player player) {
        UUID id = player.getUniqueId();
        Tracked old = lights.get(id);
        World world = player.getWorld();
        int level = player.isDead() ? 0 : heldLevel(player);

        int bx = player.getLocation().getBlockX();
        int by = player.getLocation().getBlockY();
        int bz = player.getLocation().getBlockZ();

        if (old != null && old.world.equals(world.getUID())
                && old.x == bx && old.y == by && old.z == bz && old.level == level) {
            return; // nothing changed
        }

        clear(id);

        if (level <= 0) return;

        // Dark-room spawners: don't illuminate the farm while player stands in it.
        // Spawners (and natural dark rooms) need low light even if player holds a torch.
        if (hasNearbySpawner(world, bx, by, bz)) return;

        Block at = world.getBlockAt(bx, by, bz);
        if (at.getType() == Material.AIR) {
            place(at, level);
            lights.put(id, new Tracked(world.getUID(), bx, by, bz, level));
            return;
        }

        // Feet occupied (e.g. tall grass zone handled as AIR, but just in case): try eye level
        Block eye = player.getEyeLocation().getBlock();
        if (eye.getWorld().equals(world) && eye.getType() == Material.AIR) {
            if (hasNearbySpawner(world, eye.getX(), eye.getY(), eye.getZ())) return;
            place(eye, level);
            lights.put(id, new Tracked(world.getUID(), eye.getX(), eye.getY(), eye.getZ(), level));
        }
    }

    private boolean hasNearbySpawner(World world, int bx, int by, int bz) {
        int radius = 16;
        try {
            radius = Math.max(0, plugin.getConfig().getInt("dynamic-light.spawner-freeze-radius", 16));
        } catch (Exception ignored) {}
        if (radius <= 0) return false;
        // Scan cube centred on proposed light pos — spawner range is 8, but player moves, so 16 is safe.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    try {
                        if (world.getBlockAt(bx + dx, by + dy, bz + dz).getType() == Material.SPAWNER) return true;
                    } catch (Exception ignored) {}
                }
            }
        }
        return false;
    }

    private void place(Block block, int level) {
        block.setType(Material.LIGHT, false);
        try {
            if (block.getBlockData() instanceof Light light) {
                light.setLevel(Math.max(0, Math.min(15, level)));
                block.setBlockData(light, false);
            }
        } catch (Exception ignored) {}
    }

    private void clear(UUID id) {
        Tracked t = lights.remove(id);
        if (t == null) return;
        World w = Bukkit.getWorld(t.world);
        if (w == null) return;
        Block b = w.getBlockAt(t.x, t.y, t.z);
        if (b.getType() == Material.LIGHT) {
            try {
                b.setType(Material.AIR, false);
            } catch (Exception ignored) {}
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer().getUniqueId());
    }

    /**
     * Light blocks are real blocks: a bucket aimed at one fails to place its
     * liquid, and spreading liquid stops at them. Yield in both cases — the next
     * refresh simply won't re-place light into liquid.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        try {
            if (event.getBlockClicked() == null || event.getBlockFace() == null) {
                return;
            }
            Block target = event.getBlockClicked().getRelative(event.getBlockFace());
            if (target.getType() == Material.LIGHT) {
                target.setType(Material.AIR, false);
            }
        } catch (Exception ignored) {}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        try {
            if (event.getToBlock().getType() == Material.LIGHT) {
                event.getToBlock().setType(Material.AIR, false);
            }
        } catch (Exception ignored) {}
    }

    public void removeAll() {
        for (UUID id : lights.keySet().toArray(new UUID[0])) {
            clear(id);
        }
    }

    private static class Tracked {
        final UUID world;
        final int x;
        final int y;
        final int z;
        final int level;

        Tracked(UUID world, int x, int y, int z, int level) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.level = level;
        }
    }
}
