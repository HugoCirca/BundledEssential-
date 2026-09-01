package com.bundleessential.updater;

import com.bundleessential.BundledEssential;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class UpdateManager {

    private final BundledEssential plugin;
    private static final String GITHUB_REPO = "HugoCirca/BundledEssential-";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    public UpdateManager(BundledEssential plugin) {
        this.plugin = plugin;
    }

    public void startup() {
        cleanupOldJar();
        checkForUpdates();
    }

    private void cleanupOldJar() {
        Path pluginsDir = plugin.getDataFolder().getParentFile().toPath();
        Path updateDir = pluginsDir.resolve("update");
        Path pendingUpdate = updateDir.resolve("BundledEssential.jar");

        if (Files.exists(pendingUpdate)) {
            Path currentJar = pluginsDir.resolve("BundledEssential.jar");
            try {
                Files.deleteIfExists(currentJar);
                Files.move(pendingUpdate, currentJar);
                plugin.getLogger().info("Updated to new version successfully!");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to apply update", e);
            }
        }

        try {
            if (Files.exists(updateDir)) {
                Files.walk(updateDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
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
                    String latestTag = getLatestTag();
                    String latestVersion = latestTag.replace("v", "");

                    if (latestVersion == null || latestVersion.equals(currentVersion)) {
                        return;
                    }

                    if (isNewerVersion(latestVersion, currentVersion)) {
                        plugin.getLogger().info("New update found: v" + latestVersion + " (current: " + currentVersion + ")");
                        downloadUpdate(latestTag);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to check for updates", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private String getLatestTag() throws IOException {
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

    private void downloadUpdate(String tag) {
        try {
            Path updateDir = plugin.getDataFolder().getParentFile().toPath().resolve("update");
            Files.createDirectories(updateDir);
            Path updateFile = updateDir.resolve("BundledEssential.jar");

            String downloadUrl = "https://github.com/" + GITHUB_REPO + "/releases/download/" + tag + "/BundledEssential-" + tag.replace("v", "") + ".jar";
            HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
            conn.setRequestProperty("User-Agent", "BundledEssential-Updater");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            if (conn.getResponseCode() != 200) {
                plugin.getLogger().warning("Failed to download update: HTTP " + conn.getResponseCode());
                return;
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

            plugin.getLogger().info("Update downloaded. It will be applied on next restart.");

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to download update", e);
        }
    }
}
