package dev.hugocirca.knapsack.tpa;

import dev.hugocirca.knapsack.KnapsackPlugin;
import dev.hugocirca.knapsack.common.Saveable;
import dev.hugocirca.knapsack.util.JsonStorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.hugocirca.knapsack.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TpaManager implements CommandExecutor, TabCompleter, Listener, Saveable {

    private final KnapsackPlugin plugin;
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private final Map<UUID, Boolean> tpaHereRequests = new HashMap<>();
    private final Set<UUID> autoAccept = new HashSet<>();
    private static final long REQUEST_EXPIRE_TICKS = 600L;
    private static final String GUI_TITLE = "§6TPA Requests";
    private final Gson gson = new GsonBuilder().create();
    private final java.nio.file.Path autoFile;

    public TpaManager(KnapsackPlugin plugin) {
        this.plugin = plugin;
        this.autoFile = plugin.getDataFolder().toPath().resolve("tpa.json");
        loadAuto();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadAuto() {
        com.google.gson.JsonObject obj = JsonStorage.load(plugin, "tpa.json");
        try {
            if (obj != null && obj.size() > 0) {
                JsonArray acc = obj.has("autoAccept") && obj.get("autoAccept").isJsonArray() ? obj.getAsJsonArray("autoAccept") : new JsonArray();
                for (JsonElement el : acc) try { autoAccept.add(UUID.fromString(el.getAsString())); } catch (Exception ignored) {}
            } else {
                String raw = new String(java.nio.file.Files.readAllBytes(autoFile));
                com.google.gson.JsonElement root = gson.fromJson(raw, com.google.gson.JsonElement.class);
                if (root != null && root.isJsonArray()) {
                    JsonArray arr = root.getAsJsonArray();
                    for (JsonElement el : arr) try { autoAccept.add(UUID.fromString(el.getAsString())); } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load tpa.json");
        }
    }

    private void saveAuto() {
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        JsonArray acc = new JsonArray();
        for (UUID id : autoAccept) acc.add(id.toString());
        obj.add("autoAccept", acc);
        JsonStorage.save(plugin, "tpa.json", obj);
    }

    @Override
    public void saveAll() { saveAuto(); }

    // Extracted toggle path used by both command (legacy) and GUI
    public boolean toggleAuto(UUID id) {
        boolean enable = !autoAccept.contains(id);
        if (enable) autoAccept.add(id); else autoAccept.remove(id);
        saveAuto();
        return enable;
    }

    private void openRequestsGui(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
        ItemStack filler = GuiUtil.filler(Material.GRAY_STAINED_GLASS_PANE);
        GuiUtil.fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        UUID targetId = viewer.getUniqueId();
        boolean has = pendingRequests.containsKey(targetId);
        if (has) {
            UUID senderId = pendingRequests.get(targetId);
            boolean isHere = tpaHereRequests.getOrDefault(targetId, false);
            Player sender = Bukkit.getPlayer(senderId);
            String name = sender != null ? sender.getName() : senderId.toString().substring(0,8);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(senderId)); } catch (Exception ignored) {}
                meta.setDisplayName("§e" + name);
                List<String> lore = new ArrayList<>();
                lore.add(isHere ? "§7wants you to teleport to them" : "§7wants to teleport to you");
                lore.add("§7Click Accept or Deny");
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(11, head);
            inv.setItem(15, GuiUtil.item(Material.LIME_WOOL, "§aAccept", "§7Teleport " + (isHere ? "them to you" : "you to them")));
            inv.setItem(16, GuiUtil.item(Material.RED_WOOL, "§cDeny", "§7Decline request"));
        } else {
            inv.setItem(13, GuiUtil.item(Material.BARRIER, "§7No pending requests", "§7Someone must /tpa you first"));
        }
        boolean auto = autoAccept.contains(targetId);
        inv.setItem(26, GuiUtil.item(auto ? Material.LIME_DYE : Material.GRAY_DYE, auto ? "§aAuto-Accept: ON" : "§7Auto-Accept: OFF",
                auto ? "§7Click to disable" : "§7Click to enable instant teleport"));
        viewer.openInventory(inv);
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().getTitle().equals(GUI_TITLE)) return;
        e.setCancelled(true);
        if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;
        UUID id = p.getUniqueId();
        int slot = e.getSlot();
        if (slot == 15 && pendingRequests.containsKey(id)) {
            handleTpAccept(p);
            p.closeInventory();
        } else if (slot == 16 && pendingRequests.containsKey(id)) {
            UUID senderId = pendingRequests.get(id);
            pendingRequests.remove(id);
            tpaHereRequests.remove(id);
            p.sendMessage("§cDenied TPA request.");
            Player sender = Bukkit.getPlayer(senderId);
            if (sender != null) sender.sendMessage("§c" + p.getName() + " denied your TPA request.");
            p.closeInventory();
        } else if (slot == 26) {
            boolean now = toggleAuto(id);
            p.sendMessage(now ? "§aTPA auto-accept §lON" : "§eTPA auto-accept §lOFF");
            openRequestsGui(p);
        } else if (slot == 11 && pendingRequests.containsKey(id)) {
            // clicking head does nothing, just info
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("tpa")) {
            if (args.length == 0) {
                openRequestsGui(player);
                return true;
            }
            String sub = args[0].toLowerCase();
            if (sub.equals("here")) {
                if (args.length != 2) {
                    player.sendMessage("§cUsage: /tpa here <player>");
                    return true;
                }
                handleTpaHere(player, args[1]);
            } else if (args.length == 1) {
                handleTpa(player, args[0]);
            } else {
                player.sendMessage("§cUsage: /tpa <player> or /tpa here <player> or /tpa");
            }
            return true;
        }
        if (cmd.equals("tpahere")) {
            if (args.length != 1) {
                player.sendMessage("§cUsage: /tpahere <player>");
                return true;
            }
            handleTpaHere(player, args[0]);
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String name = command.getName().toLowerCase();
        if (name.equals("tpa")) {
            if (args.length == 1) {
                out.add("here");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (sender instanceof Player self && p.equals(self)) continue;
                    out.add(p.getName());
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("here")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (sender instanceof Player self && p.equals(self)) continue;
                    out.add(p.getName());
                }
            }
        } else if (name.equals("tpahere")) {
            if (args.length == 1) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (sender instanceof Player self && p.equals(self)) continue;
                    out.add(p.getName());
                }
            }
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        out.removeIf(s -> !s.toLowerCase().startsWith(last));
        return out;
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
        if (autoAccept.contains(targetId)) {
            sender.sendMessage("§aTeleport request sent to §e" + target.getName() + "§a!");
            doTeleport(sender, target, false, true);
            return;
        }
        pendingRequests.put(targetId, senderId);
        tpaHereRequests.put(targetId, false);
        sender.sendMessage("§aTeleport request sent to §e" + target.getName() + "§a!");
        target.sendMessage("§e" + sender.getName() + " §awants to teleport to you. §6/tpa §7to accept/deny or auto.");
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRequests.containsKey(targetId) && pendingRequests.get(targetId).equals(senderId)) {
                    pendingRequests.remove(targetId);
                    tpaHereRequests.remove(targetId);
                    if (sender.isOnline()) sender.sendMessage("§cYour teleport request to §e" + target.getName() + " §cexpired.");
                    if (target.isOnline()) target.sendMessage("§cThe teleport request from §e" + sender.getName() + " §cexpired.");
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
        if (autoAccept.contains(targetId)) {
            sender.sendMessage("§aRequest sent to §e" + target.getName() + " §a to teleport to you.");
            doTeleport(sender, target, true, true);
            return;
        }
        pendingRequests.put(targetId, senderId);
        tpaHereRequests.put(targetId, true);
        sender.sendMessage("§aRequest sent to §e" + target.getName() + " §a to teleport to you.");
        target.sendMessage("§e" + sender.getName() + " §awants you to teleport to them. §6/tpa §7to accept/deny.");
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRequests.containsKey(targetId) && pendingRequests.get(targetId).equals(senderId)) {
                    pendingRequests.remove(targetId);
                    tpaHereRequests.remove(targetId);
                    if (sender.isOnline()) sender.sendMessage("§cYour teleport request to §e" + target.getName() + " §cexpired.");
                    if (target.isOnline()) target.sendMessage("§cThe teleport request from §e" + sender.getName() + " §cexpired.");
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
        doTeleport(sender, target, isTpaHere, false);
    }

    private void doTeleport(Player sender, Player target, boolean isTpaHere, boolean auto) {
        String tag = auto ? "§b[Auto] " : "";
        if (isTpaHere) {
            target.teleport(sender.getLocation());
            target.sendMessage(tag + "§aYou have been teleported to §e" + sender.getName() + "§a!");
            sender.sendMessage(tag + "§aTeleported §e" + target.getName() + " §ato you!");
        } else {
            sender.teleport(target.getLocation());
            sender.sendMessage(tag + "§aYou have been teleported to §e" + target.getName() + "§a!");
            target.sendMessage(tag + "§e" + sender.getName() + " §ahas accepted your teleport request!");
        }
    }
}
