package dev.hugocirca.knapsack.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** Loads features.yml — one true/false toggle per feature. */
public class Features {

    private final FileConfiguration config;

    public Features(JavaPlugin plugin) {
        plugin.saveResource("features.yml", false);
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "features.yml"));
    }

    public boolean isEnabled(String feature) {
        return config.getBoolean(feature, false);
    }

    public boolean hasAnyEnabled() {
        for (String key : config.getKeys(false)) if (config.getBoolean(key, false)) return true;
        return false;
    }
}
