package com.bundleessential.util;

import com.bundleessential.BundledEssential;

public class DataStorage {

    private final BundledEssential plugin;

    public DataStorage(BundledEssential plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    public void saveAll() {
        plugin.saveConfig();
    }
}
