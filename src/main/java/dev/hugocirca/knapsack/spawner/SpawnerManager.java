package dev.hugocirca.knapsack.spawner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hugocirca.knapsack.common.Saveable;
import dev.hugocirca.knapsack.util.JsonStorage;
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
 * Custom-category stackable spawners: zombie ($250) + skeleton ($325).
 * Place for a normal 1x spawner labeled "Zombie 1x" / "Skeleton 1x".
 * Right-click it holding another spawner item of the SAME type to consume
 * it and raise the rate, up to 35x. Mine with iron+ pickaxe to keep it.
 * Rate scales by spawning (mult - 1) bonus mobs per natural spawner cycle.
 */
public class SpawnerManager implements Listener, Saveable {

    private static final long SAVE_INTERVAL_TICKS = 6000L;
    private static final double DEF_ZOMBIE_PRICE = 250.0;
    private static final double DEF_SKELETON_PRICE = 325.0;
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

    public static NamespacedKey typeKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, "spawn-type");
    }

    /** Shop template: 1x marker, no stored multiplier. Legacy = zombie. */
    public static ItemStack template(JavaPlugin plugin) {
        return template(plugin, EntityType.ZOMBIE);
    }

    public static ItemStack template(JavaPlugin plugin, EntityType type) {
        boolean skeleton = type == EntityType.SKELETON;
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(skeleton ? "§f§lSkeleton Spawner" : "§a§lZombie Spawner");
        List<String> lore = new ArrayList<>();
        lore.add("§7Place for a 1x spawner anywhere");
        lore.add(skeleton ? "§7Right-click a wild skeleton" : "§7Right-click a wild zombie");
        lore.add("§7spawner to boost it to 35x");
        lore.add("§7Whole held stack feeds at once");
        lore.add("§7Mine with iron+ pickaxe to keep");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(tagKey(plugin), PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(typeKey(plugin), PersistentDataType.STRING,
                skeleton ? "SKELETON" : "ZOMBIE");
        item.setItemMeta(meta);
        return item;
    }

    public static EntityType typeOf(JavaPlugin plugin, ItemStack item) {
        try {
            String s = item.getItemMeta().getPersistentDataContainer()
                    .get(typeKey(plugin), PersistentDataType.STRING);
            if ("SKELETON".equalsIgnoreCase(s)) return EntityType.SKELETON;
        } catch (Exception ignored) {}
        return EntityType.ZOMBIE;
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
        return tagged(plugin, mult, EntityType.ZOMBIE);
    }

    public static ItemStack tagged(JavaPlugin plugin, int mult, EntityType type) {
        ItemStack item = template(plugin, type);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer()
                .set(multKey(plugin), PersistentDataType.INTEGER, Math.max(1, mult));
        item.setItemMeta(meta);
        return item;
    }

    // ---------- config ----------

    public double spawnerPrice() {
        return zombiePrice();
    }

    public double zombiePrice() {
        try {
            if (plugin.getConfig().contains("spawner.zombie-price")) {
                double p = plugin.getConfig().getDouble("spawner.zombie-price", DEF_ZOMBIE_PRICE);
                return p > 0 ? Math.round(p * 100.0) / 100.0 : DEF_ZOMBIE_PRICE;
            }
            double p = plugin.getConfig().getDouble("spawner.price", DEF_ZOMBIE_PRICE);
            return p > 0 ? Math.round(p * 100.0) / 100.0 : DEF_ZOMBIE_PRICE;
        } catch (Exception e) {
            return DEF_ZOMBIE_PRICE;
        }
    }

    public double skeletonPrice() {
        try {
            double p = plugin.getConfig().getDouble("spawner.skeleton-price", DEF_SKELETON_PRICE);
            return p > 0 ? Math.round(p * 100.0) / 100.0 : DEF_SKELETON_PRICE;
        } catch (Exception e) {
            return DEF_SKELETON_PRICE;
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

    @Override
    public void saveAll() {
        JsonStorage.save(plugin, "spawner.json", data);
    }

    private void loadAll() {
        JsonObject loaded = JsonStorage.load(plugin, "spawner.json");
        if (loaded != null) loaded.entrySet().forEach(e -> data.add(e.getKey(), e.getValue()));
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

    private static String label(int mult, EntityType type) {
        if (type == EntityType.SKELETON) return "§f§lSkeleton " + mult + "x";
        return "§a§lZombie " + mult + "x";
    }

    private static EntityType typeOf(JsonObject e) {
        try {
            String s = e.get("type").getAsString();
            if ("SKELETON".equalsIgnoreCase(s)) return EntityType.SKELETON;
        } catch (Exception ignored) {}
        return EntityType.ZOMBIE;
    }

    private ArmorStand ensureHologram(String key, Block block, int mult, EntityType type) {
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
        stand.setCustomName(label(mult, typeOf(e)));
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
                JsonObject je = en.getValue().getAsJsonObject();
                ensureHologram(en.getKey(), block, multOf(je), typeOf(je));
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
        EntityType type = typeOf(plugin, hand);
        spawner.setSpawnedType(type);
        try {
            spawner.update();
        } catch (Exception ignored) {}
        int mult = Math.min(maxMult(), multOf(plugin, hand));
        String key = locKey(block);
        JsonObject e = new JsonObject();
        e.addProperty("mult", mult);
        e.addProperty("type", type == EntityType.SKELETON ? "SKELETON" : "ZOMBIE");
        spawners().add(key, e);
        ensureHologram(key, block, mult, type);
        saveAll();
        String nice = type == EntityType.SKELETON ? "Skeleton " : "Zombie ";
        event.getPlayer().sendMessage("§aPlaced §l" + nice + mult + "x§a spawner!"
                + " §7(Right-click it with another " + nice.trim().toLowerCase() + " spawner to raise the rate)");
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
        if (!(clicked.getState() instanceof CreatureSpawner spawner)) {
            return; // not a spawner: let vanilla place/interact normally
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isSpawnerItem(plugin, hand)) {
            return;
        }
        EntityType handType = typeOf(plugin, hand);
        EntityType blockType = spawner.getSpawnedType();
        // Only same-type boosting; vanilla types (e.g. wild skeleton vs zombie item) rejected.
        if (blockType != handType) {
            event.setCancelled(true);
            player.sendMessage("§cType mismatch! Use a §e" + blockType.name().toLowerCase()
                    + " §cspawner on this one (you hold " + handType.name().toLowerCase() + ").");
            return;
        }
        if (blockType != EntityType.ZOMBIE && blockType != EntityType.SKELETON) {
            event.setCancelled(true);
            player.sendMessage("§cOnly §azombie §f/ skeleton §cspawners can be boosted!");
            return;
        }
        // Don't let vanilla place the held spawner against this one.
        event.setCancelled(true);
        String key = locKey(clicked);
        JsonObject e = spawners().has(key) ? spawners().getAsJsonObject(key) : null;
        if (e == null) {
            // Wild spawner: every held spawner item is +1x on top of its natural 1x.
            int room = maxMult() - 1;
            int use = Math.min(hand.getAmount(), Math.max(0, room));
            if (use <= 0) {
                player.sendMessage("§eMax rate is §l" + maxMult() + "x§e — spawners can't go higher.");
                return;
            }
            int adopted = 1 + use;
            JsonObject fresh = new JsonObject();
            fresh.addProperty("mult", adopted);
            fresh.addProperty("type", handType == EntityType.SKELETON ? "SKELETON" : "ZOMBIE");
            spawners().add(key, fresh);
            ensureHologram(key, clicked, adopted, handType);
            hand.setAmount(hand.getAmount() - use);
            saveAll();
            String nice = handType == EntityType.SKELETON ? "Skeleton " : "Zombie ";
            player.sendMessage("§aBoosted wild spawner to §l" + nice + adopted + "x§a! (used " + use + ")");
            return;
        }
        int mult = multOf(e);
        EntityType stored = typeOf(e);
        int room = maxMult() - mult;
        if (room <= 0) {
            player.sendMessage("§eThis spawner is already at max §l" + maxMult() + "x§e!");
            return;
        }
        int use = Math.min(hand.getAmount(), room);
        hand.setAmount(hand.getAmount() - use);
        int next = mult + use;
        e.addProperty("mult", next);
        ensureHologram(key, clicked, next, stored);
        saveAll();
        String nice = stored == EntityType.SKELETON ? "Skeleton " : "Zombie ";
        player.sendMessage("§a§l" + nice + next + "x§a! (used " + use + " spawner(s))");
    }

    private static boolean isIronPickaxeOrBetter(org.bukkit.inventory.ItemStack tool) {
        if (tool == null) return false;
        Material t = tool.getType();
        return t == Material.IRON_PICKAXE || t == Material.DIAMOND_PICKAXE || t == Material.NETHERITE_PICKAXE;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        String key = locKey(event.getBlock());
        JsonObject e = spawners().has(key) ? spawners().getAsJsonObject(key) : null;
        if (e == null) {
            return;
        }
        // Iron+ pickaxe required to keep the spawner; otherwise block the break.
        if (event.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE
                && !isIronPickaxeOrBetter(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cMine this spawner with an §firon pickaxe §cor better to keep it!");
            return;
        }
        int mult = multOf(e);
        EntityType stored = typeOf(e);
        removeSpawner(key);
        saveAll();
        if (event.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }
        event.setDropItems(false);
        event.getBlock().getWorld().dropItemNaturally(
                event.getBlock().getLocation().add(0.5, 0.5, 0.5), tagged(plugin, mult, stored));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : new ArrayList<>(event.blockList())) {
            String key = locKey(block);
            if (spawners().has(key)) {
                // Make custom spawners blast-resistant: remove from explosion list, keep data/hologram
                event.blockList().remove(block);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : new ArrayList<>(event.blockList())) {
            String key = locKey(block);
            if (spawners().has(key)) {
                event.blockList().remove(block);
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
        EntityType spawnType = typeOf(e);
        Location base = event.getEntity().getLocation();
        World world = base.getWorld();
        if (world == null) {
            return;
        }
        for (int i = 1; i < mult; i++) {
            Location at = base.clone().add(random.nextDouble() * 4.0 - 2.0, random.nextDouble() * 2.0, random.nextDouble() * 4.0 - 2.0);
            try {
                if (at.getBlock().isPassable()) {
                    world.spawnEntity(at, spawnType);
                }
            } catch (Exception ignored) {}
        }
    }
}
