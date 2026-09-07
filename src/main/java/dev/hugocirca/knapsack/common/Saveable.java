package dev.hugocirca.knapsack.common;

/** Common contract for managers that persist data to disk. */
public interface Saveable {
    void saveAll();
}
