package com.bundleessential.spawner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Second Custom-category item: stackable zombie spawner ($500).
 * Place it for a normal 1x zombie spawner labeled "Zombie 1x". Right-click it
 * holding another spawner item to consume it and raise the rate, up to 35x.
 * Rate scales by spawning (mult - 1) bonus zombies per natural spawner cycle.
 */
public class SpawnerManager implements Listener {

    private static final long SAVE_INTERVAL_TICKS = 6000L;
    private static final double DEF_PRICE = 500.0;
    private static final int DEF_MAX_MULT = 35;

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final JsonObject data = new JsonObject();

    public SpawnerManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("spawner.json");
        loadAll();
        startSaveTask();
    }

    // ---------- static item helpers ----------

    public static NamespacedKey tagKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, "spawnboost");
    }

    public static NamespacedKey multKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, "spawn-mult");
    }

    /** Shop template: 1x marker, no stored multiplier. */
    public static ItemStack template(JavaPlugin plugin) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§lZombie Spawner");
        List<String> lore = new ArrayList<>();
        lore.add("§7Place for a 1x zombie spawner");
        lore.add("§7Right-click one placed to 2x, 3x...");
        lore.add("§7Max 35x rate");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(tagKey(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isSpawnerItem(JavaPlugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER || !item.hasItemMeta()) {
            return false;
        }
        try {
            return item.getItemMeta().getPersistentDataContainer()
                    .has(tagKey(plugin), PersistentDataType.BYTE);
        } catch (Exception e) {
            return false;
        }
    }

    /** Stored multiplier on a break-returned item, 1 if absent/invalid. */
    public static int multOf(JavaPlugin plugin, ItemStack item) {
        try {
            Integer mult = item.getItemMeta().getPersistentDataContainer()
                    .get(multKey(plugin), PersistentDataType.INTEGER);
            return mult == null ? 1 : Math.max(1, mult);
        } catch (Exception e) {
            return 1;
        }
    }

    /** Break return: keeps its multiplier so relocating never loses progress. */
    public static ItemStack tagged(JavaPlugin plugin, int mult) {
        ItemStack item = template(plugin);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer()
                .set(multKey(plugin), PersistentDataType.INTEGER, Math.max(1, mult));
        item.setItemMeta(meta);
        return item;
    }

    // ---------- config ----------

    public double spawnerPrice() {
        try {
            double p = plugin.getConfig().getDouble("spawner.price", DEF_PRICE);
            return p > 0 ? Math.round(p * 100.0) / 100.0 : DEF_PRICE;
        } catch (Exception e) {
            return DEF_PRICE;
        }
    }

    private int maxMult() {
        try {
            return Math.max(1, plugin.getConfig().getInt("spawner.max-multiplier", DEF_MAX_MULT));
        } catch (Exception e) {
            return DEF_MAX_MULT;
        }
    }

    // ---------- data ----------

    private JsonObject spawners() {
        if (!data.has("spawners") || !data.get("spawners").isJsonObject()) {
            data.add("spawners", new JsonObject());
        }
        return data.getAsJsonObject("spawners");
    }

    private static String locKey(Block block) {
        return block.getWorld().getUID() + "|" + block.getX() + "|" + block.getY() + "|" + block.getZ();
    }

    private static String locKey(World world, int x, int y, int z) {
        return world.getUID() + "|" + x + "|" + y + "|" + z;
    }

    private int multOf(JsonObject e) {
        try {
            return Math.max(1, e.get("mult").getAsInt());
        } catch (Exception ex) {
            return 1;
        }
    }

    public void saveAll() {
        try {
            Files.write(file, gson.toJson(data).getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save spawner.json");
        }
    }

    private void loadAll() {
        plugin.getDataFolder().mkdirs();
        try {
            if (Files.exists(file)) {
                JsonObject loaded = gson.fromJson(new String(Files.readAllBytes(file)), JsonObject.class);
                if (loaded != null) {
                    loaded.entrySet().forEach(e -> data.add(e.getKey(), e.getValue()));
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load spawner.json");
        }
    }

    private void startSaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAll();
            }
        }.runTaskTimer(plugin, SAVE_INTERVAL_TICKS, SAVE_INTERVAL_TICKS);
    }

    // ---------- hologram label ----------

    private static String label(int mult) {
        return "§a§lZombie " + mult + "x";
    }

    private ArmorStand ensureHologram(String key, Block block, int mult) {
        JsonObject e = spawners().getAsJsonObject(key);
        ArmorStand stand = null;
        try {
            String standId = e.has("stand") ? e.get("stand").getAsString() : null;
            if (standId != null) {
                Entity entity = Bukkit.getEntity(UUID.fromString(standId));
                if (entity instanceof ArmorStand armorStand && !armorStand.isDead()) {
                    stand = armorStand;
                }
            }
        } catch (Exception ignored) {}
        if (stand == null) {
            Location at = block.getLocation().add(0.5, 2.1, 0.5);
            try {
                stand = (ArmorStand) block.getWorld().spawnEntity(at, EntityType.ARMOR_STAND);
            } catch (Exception ex) {
                return null;
            }
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setRemoveWhenFarAway(false);
            stand.setInvulnerable(true);
            stand.setCustomNameVisible(true);
            e.addProperty("stand", stand.getUniqueId().toString());
        }
        stand.setCustomName(label(mult));
        return stand;
    }

    private void killHologram(String key) {
        try {
            JsonObject e = spawners().getAsJsonObject(key);
            if (e != null && e.has("stand")) {
                Entity entity = Bukkit.getEntity(UUID.fromString(e.get("stand").getAsString()));
                if (entity != null) {
                    entity.remove();
                }
            }
        } catch (Exception ignored) {}
    }

    private void removeSpawner(String key) {
        killHologram(key);
        spawners().remove(key);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        String worldId = world.getUID().toString();
        int cx = event.getChunk().getX();
        int cz = event.getChunk().getZ();
        for (Map.Entry<String, JsonElement> en : new ArrayList<>(spawners().entrySet())) {
            String[] parts = en.getKey().split("\\|");
            if (parts.length != 4 || !parts[0].equals(worldId)) {
                continue;
            }
            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                if ((x >> 4) != cx || (z >> 4) != cz) {
                    continue;
                }
                Block block = world.getBlockAt(x, y, z);
                if (!(block.getState() instanceof CreatureSpawner)) {
                    continue;
                }
                ensureHologram(en.getKey(), block, multOf(en.getValue().getAsJsonObject()));
            } catch (Exception ignored) {}
        }
    }

    // ---------- place / upgrade / break ----------

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack hand = event.getItemInHand();
        if (!isSpawnerItem(plugin, hand)) {
            return;
        }
        Block block = event.getBlockPlaced();
        BlockState state = block.getState();
        if (!(state instanceof CreatureSpawner spawner)) {
            return;
        }
        spawner.setSpawnedType(EntityType.ZOMBIE);
        try {
            spawner.update();
        } catch (Exception ignored) {}
        int mult = Math.min(maxMult(), multOf(plugin, hand));
        String key = locKey(block);
        JsonObject e = new JsonObject();
        e.addProperty("mult", mult);
        spawners().add(key, e);
        ensureHologram(key, block, mult);
        saveAll();
        event.getPlayer().sendMessage("§aPlaced §lZombie " + mult + "x§a spawner!"
                + " §7(Right-click it with another spawner to raise the rate)");
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        String key = locKey(clicked);
        JsonObject e = spawners().has(key) ? spawners().getAsJsonObject(key) : null;
        if (e == null) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isSpawnerItem(plugin, hand)) {
            return;
        }
        // Don't let vanilla place the held spawner against this one.
        event.setCancelled(true);
        int mult = multOf(e);
        if (mult >= maxMult()) {
            player.sendMessage("§eThis spawner is already at max §l" + maxMult() + "x§e!");
            return;
        }
        hand.setAmount(hand.getAmount() - 1);
        int next = mult + 1;
        e.addProperty("mult", next);
        ensureHologram(key, clicked, next);
        saveAll();
        player.sendMessage("§a§lZombie " + next + "x§a! Rate increased.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        String key = locKey(event.getBlock());
        JsonObject e = spawners().has(key) ? spawners().getAsJsonObject(key) : null;
        if (e == null) {
            return;
        }
        int mult = multOf(e);
        removeSpawner(key);
        saveAll();
        if (event.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }
        event.setDropItems(false);
        event.getBlock().getWorld().dropItemNaturally(
                event.getBlock().getLocation().add(0.5, 0.5, 0.5), tagged(plugin, mult));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : new ArrayList<>(event.blockList())) {
            String key = locKey(block);
            if (spawners().has(key)) {
                removeSpawner(key);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : new ArrayList<>(event.blockList())) {
            String key = locKey(block);
            if (spawners().has(key)) {
                removeSpawner(key);
            }
        }
    }

    // ---------- rate boost ----------

    @EventHandler(ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        Block spawnerBlock = event.getSpawner() != null ? event.getSpawner().getBlock() : null;
        if (spawnerBlock == null) {
            return;
        }
        String key = locKey(spawnerBlock);
        JsonObject e = spawners().has(key) ? spawners().getAsJsonObject(key) : null;
        if (e == null) {
            return;
        }
        int mult = multOf(e);
        if (mult <= 1) {
            return;
        }
        Location base = event.getEntity().getLocation();
        World world = base.getWorld();
        if (world == null) {
            return;
        }
        for (int i = 1; i < mult; i++) {
            Location at = base.clone().add(random.nextDouble() * 4.0 - 2.0, random.nextDouble() * 2.0, random.nextDouble() * 4.0 - 2.0);
            try {
                if (at.getBlock().isPassable()) {
                    world.spawnEntity(at, EntityType.ZOMBIE);
                }
            } catch (Exception ignored) {}
        }
    }
}
