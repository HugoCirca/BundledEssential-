package com.bundleessential.updater;

import com.bundleessential.BundledEssential;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.logging.Level;

public class UpdateManager implements CommandExecutor {

    private final BundledEssential plugin;
    private static final String GITHUB_REPO = "HugoCirca/BundledEssential-";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private static final long MIN_JAR_BYTES = 50_000L; // sanity floor: real jar is ~135KB
    private static final int MAX_BACKUPS = 3;
    private volatile String pendingTag = null;

    public UpdateManager(BundledEssential plugin) {
        this.plugin = plugin;
    }

    public void startup() {
        cleanupOldJar();
        checkForUpdates();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bundleessential.update")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        sender.sendMessage("§eChecking for updates...");
        try {
            if (Files.exists(getPendingFile())) {
                sender.sendMessage("§7Pending update on disk"
                        + (pendingTag != null ? " (" + pendingTag + ")" : "")
                        + " — applies on next restart.");
            }
        } catch (Exception ignored) {}
        checkForUpdateManual(sender);
        return true;
    }

    private void checkForUpdateManual(CommandSender sender) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String currentVersion = plugin.getDescription().getVersion();
                    String[] info = getReleaseInfo();
                    String latestTag = info[0];
                    String digest = info[1];
                    if (latestTag == null) {
                        sender.sendMessage("§cFailed to check for updates.");
                        return;
                    }

                    String latestVersion = latestTag.replace("v", "");

                    if (latestVersion.equals(currentVersion)) {
                        sender.sendMessage("§aYou are already on the latest version (v" + currentVersion + ")");
                        return;
                    }

                    if (isNewerVersion(latestVersion, currentVersion)) {
                        sender.sendMessage("§eNew update found: v" + latestVersion + " (current: " + currentVersion + ")");
                        sender.sendMessage("§eDownloading...");
                        boolean success = downloadUpdate(latestTag, digest);
                        if (success) {
                            sender.sendMessage("§aUpdate downloaded! It will be applied on next restart.");
                        } else {
                            sender.sendMessage("§cFailed to download update.");
                        }
                    } else {
                        sender.sendMessage("§cYou are running a newer version than the latest release.");
                    }
                } catch (Exception e) {
                    sender.sendMessage("§cFailed to check for updates: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private Path getUpdateDir() {
        return plugin.getDataFolder().getParentFile().toPath().resolve("update");
    }

    private Path getPendingFile() {
        return getUpdateDir().resolve("BundledEssential.jar");
    }

    private Path getBackupDir() {
        return plugin.getDataFolder().getParentFile().toPath().resolve("BundledEssential-backups");
    }

    private void cleanupOldJar() {
        Path pendingUpdate = getPendingFile();
        if (!Files.exists(pendingUpdate)) {
            deleteQuietly(getUpdateDir());
            return;
        }
        try {
            if (Files.size(pendingUpdate) < MIN_JAR_BYTES) {
                plugin.getLogger().warning("Pending update looks corrupt (too small), deleting.");
                Files.deleteIfExists(pendingUpdate);
                return;
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not verify pending update", e);
            return;
        }

        Path currentJar = plugin.getDataFolder().getParentFile().toPath().resolve("BundledEssential.jar");
        try {
            if (Files.exists(currentJar)) {
                backupCurrentJar(currentJar);
                Files.deleteIfExists(currentJar);
            }
            Files.move(pendingUpdate, currentJar);
            plugin.getLogger().info("Updated to new version successfully! Previous jar kept in BundledEssential-backups/.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to apply update, restoring backup", e);
            restoreBackup(currentJar);
        } finally {
            deleteQuietly(getUpdateDir());
        }
    }

    private void backupCurrentJar(Path currentJar) {
        try {
            Path backupDir = getBackupDir();
            Files.createDirectories(backupDir);
            Path backup = backupDir.resolve("BundledEssential-" + plugin.getDescription().getVersion() + ".jar");
            Files.copy(currentJar, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            pruneBackups(backupDir);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not back up current jar", e);
        }
    }

    private void pruneBackups(Path backupDir) {
        try (java.util.stream.Stream<Path> files = Files.list(backupDir)) {
            List<Path> jars = files.filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .sorted((a, b) -> {
                        try {
                            return Long.compare(Files.getLastModifiedTime(a).toMillis(), Files.getLastModifiedTime(b).toMillis());
                        } catch (IOException e) {
                            return 0;
                        }
                    }).toList();
            while (jars.size() > MAX_BACKUPS) {
                Files.deleteIfExists(jars.remove(0));
            }
        } catch (IOException ignored) {}
    }

    private void restoreBackup(Path currentJar) {
        try {
            Path backupDir = getBackupDir();
            if (!Files.exists(backupDir)) return;
            try (java.util.stream.Stream<Path> files = Files.list(backupDir)) {
                Path newest = files.filter(p -> p.getFileName().toString().endsWith(".jar")).max((a, b) -> {
                    try {
                        return Long.compare(Files.getLastModifiedTime(a).toMillis(), Files.getLastModifiedTime(b).toMillis());
                    } catch (IOException e) {
                        return 0;
                    }
                }).orElse(null);
                if (newest != null && !Files.exists(currentJar)) {
                    Files.copy(newest, currentJar);
                    plugin.getLogger().info("Restored " + newest.getFileName());
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to restore backup", e);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
            }
        } catch (IOException ignored) {}
    }

    private void checkForUpdates() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String currentVersion = plugin.getDescription().getVersion();
                    String[] info = getReleaseInfo();
                    String latestTag = info[0];
                    if (latestTag == null) return;
                    String latestVersion = latestTag.replace("v", "");

                    if (latestVersion == null || latestVersion.equals(currentVersion)) {
                        return;
                    }

                    if (isNewerVersion(latestVersion, currentVersion)) {
                        plugin.getLogger().info("New update found: v" + latestVersion + " (current: " + currentVersion + ")");
                        downloadUpdate(latestTag, info[1]);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to check for updates", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /** Returns {latestTag or null, sha256 digest or null} from one release-API call. */
    private String[] getReleaseInfo() throws IOException {
        String json = fetchReleaseJson();
        return new String[]{parseTag(json), parseDigest(json)};
    }

    private String getLatestTag() throws IOException {
        return parseTag(fetchReleaseJson());
    }

    /** First asset SHA-256 digest published by the API (ours is the only asset). */
    private String parseDigest(String json) {
        for (String prefix : new String[]{"\"digest\":\"sha256:", "\"digest\": \"sha256:"}) {
            int idx = json.indexOf(prefix);
            if (idx != -1) {
                int start = idx + prefix.length();
                int end = json.indexOf("\"", start);
                if (end != -1) return json.substring(start, end).toLowerCase();
            }
        }
        return null;
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    md.update(buffer, 0, len);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }

    private String fetchReleaseJson() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setRequestProperty("User-Agent", "BundledEssential-Updater");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (conn.getResponseCode() != 200) {
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();

        String json = sb.toString();
        reader.close();
        return json;
    }

    private String parseTag(String json) {
        int tagIdx = json.indexOf("\"tag_name\":\"");
        if (tagIdx == -1) return null;

        int start = tagIdx + "\"tag_name\":\"".length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");

        int maxLen = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < maxLen; i++) {
            int l = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            int c = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            if (l > c) return true;
            if (l < c) return false;
        }
        return false;
    }

    private boolean downloadUpdate(String tag, String expectedSha256) {
        try {
            Path updateDir = getUpdateDir();
            Files.createDirectories(updateDir);
            Path updateFile = getPendingFile();

            String downloadUrl = "https://github.com/" + GITHUB_REPO + "/releases/download/" + tag + "/BundledEssential-" + tag.replace("v", "") + ".jar";
            HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
            conn.setRequestProperty("User-Agent", "BundledEssential-Updater");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            if (conn.getResponseCode() != 200) {
                plugin.getLogger().warning("Failed to download update: HTTP " + conn.getResponseCode());
                return false;
            }

            InputStream in = conn.getInputStream();
            OutputStream out = Files.newOutputStream(updateFile);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.close();
            in.close();

            long size = Files.size(updateFile);
            if (expectedSha256 != null && !expectedSha256.isEmpty()) {
                String actual = sha256(updateFile);
                String want = expectedSha256.toLowerCase().replace("sha256:", "");
                if (!actual.equals(want)) {
                    Files.deleteIfExists(updateFile);
                    plugin.getLogger().warning("Download checksum mismatch (want " + want + ", got " + actual + "), deleted.");
                    return false;
                }
            } else if (size < MIN_JAR_BYTES) {
                Files.deleteIfExists(updateFile);
                plugin.getLogger().warning("Downloaded update too small (" + size + " bytes), deleted.");
                return false;
            }
            pendingTag = tag;
            plugin.getLogger().info("Update downloaded (" + size + " bytes). It will be applied on next restart.");
            return true;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to download update", e);
            try {
                Files.deleteIfExists(getPendingFile());
            } catch (IOException ignored) {}
            return false;
        }
    }
}
