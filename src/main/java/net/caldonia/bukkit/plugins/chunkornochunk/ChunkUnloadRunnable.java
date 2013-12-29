package net.caldonia.bukkit.plugins.chunkornochunk;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.awt.Point;

public class ChunkUnloadRunnable implements Runnable {
    private Plugin chunkOrNoChunk;
    private String worldName;
    private Point point;

    public ChunkUnloadRunnable(Plugin chunkOrNoChunk, String worldName, Point point) {
        this.chunkOrNoChunk = chunkOrNoChunk;
        this.worldName = worldName;
        this.point = point;
    }

    @Override
    public void run() {
        World world = chunkOrNoChunk.getServer().getWorld(worldName);

        if (world == null) {
            chunkOrNoChunk.getServer().getLogger().warning("Delayed unload requested for unknown world '" + worldName + "'.");
            return;
        }

        Chunk chunk = world.getChunkAt(point.x, point.y);

        if (chunk.isLoaded()) {
            chunk.unload(false, false);
        }
    }
}
