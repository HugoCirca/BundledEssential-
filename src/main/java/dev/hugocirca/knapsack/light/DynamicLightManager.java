package dev.hugocirca.knapsack.light;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Built-in dynamic lighting: held items with light properties
 * (torch, lantern, lava bucket, glowstone, end rod, ...) show a
 * client-side fake Light block at the player's feet via sendBlockChange.
 * Server blocks stay AIR, so crouch-placing, buckets, liquids and mining
 * never break. No extra plugin needed.
 */
public class DynamicLightManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<Material, Integer> emission = new HashMap<>();
    private final Map<UUID, Tracked> fakeLights = new HashMap<>();
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
        Tracked old = fakeLights.get(id);
        World world = player.getWorld();
        int level = player.isDead() ? 0 : heldLevel(player);

        int bx = player.getLocation().getBlockX();
        int by = player.getLocation().getBlockY();
        int bz = player.getLocation().getBlockZ();

        if (old != null && old.world.equals(world.getUID())
                && old.x == bx && old.y == by && old.z == bz && old.level == level) {
            return; // nothing changed
        }

        clear(player);

        if (level <= 0) return;

        // Dark-room spawners: don't illuminate the farm while player stands in it.
        if (hasNearbySpawner(world, bx, by, bz)) return;

        Block at = world.getBlockAt(bx, by, bz);
        if (at.getType().isAir()) {
            sendFake(player, at.getLocation(), level);
            return;
        }

        // Feet occupied: try eye level
        Block eye = player.getEyeLocation().getBlock();
        if (eye.getWorld().equals(world) && eye.getType().isAir()) {
            if (hasNearbySpawner(world, eye.getX(), eye.getY(), eye.getZ())) return;
            sendFake(player, eye.getLocation(), level);
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

    private void sendFake(Player player, Location loc, int level) {
        try {
            BlockData data = Material.LIGHT.createBlockData();
            if (data instanceof Light light) {
                light.setLevel(Math.max(0, Math.min(15, level)));
            }
            player.sendBlockChange(loc, data);
            fakeLights.put(player.getUniqueId(),
                    new Tracked(loc.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), level));
        } catch (Exception ignored) {}
    }

    private void clear(Player player) {
        UUID id = player.getUniqueId();
        Tracked t = fakeLights.remove(id);
        if (t == null) return;
        try {
            World current = player.getWorld();
            // If player changed world, old fake is in another world — client already dropped it.
            if (!current.getUID().equals(t.world)) return;
            Block real = current.getBlockAt(t.x, t.y, t.z);
            // Server block was never changed, so just re-send real state to erase fake.
            player.sendBlockChange(real.getLocation(), real.getBlockData());
        } catch (Exception ignored) {}
    }

    private void clear(UUID id) {
        Player player = Bukkit.getPlayer(id);
        if (player != null) {
            clear(player);
        } else {
            fakeLights.remove(id);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Fake blocks are per-player client-side; nothing server-side to clean.
        fakeLights.remove(event.getPlayer().getUniqueId());
    }

    public void removeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                clear(player);
            } catch (Exception ignored) {}
        }
        fakeLights.clear();
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
