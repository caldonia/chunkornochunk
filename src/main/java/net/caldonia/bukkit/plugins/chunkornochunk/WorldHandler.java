package net.caldonia.bukkit.plugins.chunkornochunk;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldHandler {
    private Plugin plugin;
    private String worldName;
    private boolean keepSpawnInMemory;
    private boolean preventNewChunks;
    private boolean preventChunkSaves;
    private Map<String, WorldArea> worldAreas;
    private List<Point> safeUnloadingChunks;

    public WorldHandler(Plugin plugin, String worldName, ConfigurationSection configuration) throws InvalidConfigurationException {
        this.plugin = plugin;
        this.worldName = worldName;

        keepSpawnInMemory = configuration.getBoolean("keepSpawnInMemory", true);
        preventNewChunks = configuration.getBoolean("preventNewChunks", false);
        preventChunkSaves = configuration.getBoolean("preventChunkSaves", false);

        worldAreas = new HashMap<>();
        safeUnloadingChunks = new ArrayList<>();

        if (configuration.contains("keepLoaded")) {
            ConfigurationSection keeps = configuration.getConfigurationSection("keepLoaded");

            for (String area : keeps.getKeys(false)) {
                ConfigurationSection areaConfig = keeps.getConfigurationSection(area);

                WorldArea worldArea = new WorldArea(area, areaConfig);
                worldAreas.put(area, worldArea);
            }
        }
    }

    public void worldLoad(World world) {
        plugin.getLogger().info("World '" + worldName + "' loading, keepSpawnInMemory is " + keepSpawnInMemory + ", has " + world.getLoadedChunks().length + " chunks loaded.");

        // Set if we should keep spawn in memory.
        world.setKeepSpawnInMemory(keepSpawnInMemory);

        // Unload unusued chunks if not needed.
        if (!keepSpawnInMemory) {
            plugin.getLogger().info("World '" + worldName + "' has " + world.getLoadedChunks().length + " chunks loaded after setting keepSpawnInMemory.");

            for (Chunk chunk : world.getLoadedChunks()) {
                boolean unload = true;

                // Make sure the loaded areas aren't meant to be loaded.
                for (WorldArea worldArea : worldAreas.values()) {
                    if (worldArea.includesChunk(chunk)) {
                        unload = false;
                    }
                }

                // If we have to unload, unload it.
                if (unload) {
                    plugin.getLogger().info("World '" + worldName + "' unloading chunk " + chunk.getX() + "," + chunk.getZ() + ".");
                    chunk.unload(!preventChunkSaves);
                }
            }
        }

        for (WorldArea worldArea : worldAreas.values()) {
            int loadCount = worldArea.requestLoadOfArea(world);
            plugin.getLogger().info("World '" + worldName + "' loading required area '" + worldArea.getName() + "' with " + loadCount + " chunk(s).");
        }
    }

    public void chunkLoad(Chunk chunk, boolean isNew) {
        // Unload the chunk if it's new.
        if (isNew && preventNewChunks) {
            plugin.getLogger().info("World '" + worldName + "' preventing new chunk " + chunk.getX() + "," + chunk.getZ() + ".");

            // Add to our list of chunks permitted to unload.
            safeUnloadingChunks.add(chunkToPoint(chunk));
            // Unload the chunk without saving and do it unsafely (ignore players).
            chunk.unload(false, false);
        }
    }

    public boolean chunkUnload(Chunk chunk) {
        // First check to see if the chunk must remain loaded.
        for (WorldArea worldArea : worldAreas.values()) {
            if (worldArea.includesChunk(chunk)) {
                plugin.getLogger().info("World '" + worldName + "' preventing chunk unload due to area '" + worldArea.getName() + "' for " + chunk.getX() + "," + chunk.getZ() + ".");
                // Cancel the unload.
                return true;
            }
        }

        // Find point for chunk.
        Point point = chunkToPoint(chunk);

        // If the chunk isn't in the safe list and we're preventing chunk saves.
        if (!safeUnloadingChunks.contains(point) && preventChunkSaves) {
            plugin.getLogger().info("World '" + worldName + "' canceling unload to prevent save for " + chunk.getX() + "," + chunk.getZ() + ".");

            // Add it save next time.
            safeUnloadingChunks.add(point);

            // Schedule the unload on the next tick when it's safe.
            plugin.getServer().getScheduler().runTask(plugin, new ChunkUnloadRunnable(plugin, worldName, point));

            // Cancel the unload.
            return true;
        }

        // Remove reference from safe list regardless of it being in or not.
        safeUnloadingChunks.remove(point);

        plugin.getLogger().info("World '" + worldName + "' allowing unload for " + chunk.getX() + "," + chunk.getZ() + ".");

        // Don't cancel the unload.
        return false;
    }

    public static Point chunkToPoint(Chunk chunk) {
        return new Point(chunk.getX(), chunk.getZ());
    }
}
