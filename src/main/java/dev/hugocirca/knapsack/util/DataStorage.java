package dev.hugocirca.knapsack.util;

import dev.hugocirca.knapsack.KnapsackPlugin;

public class DataStorage {

    private final KnapsackPlugin plugin;

    public DataStorage(KnapsackPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    public void saveAll() {
        plugin.saveConfig();
    }
}
