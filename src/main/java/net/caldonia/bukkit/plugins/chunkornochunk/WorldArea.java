package net.caldonia.bukkit.plugins.chunkornochunk;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

public class WorldArea {
    private String name;
    private int x1;
    private int z1;
    private int x2;
    private int z2;

    public WorldArea(String name, ConfigurationSection configurationSection) throws InvalidConfigurationException {
        this.name = name;

        if (!configurationSection.contains("x1") || !configurationSection.contains("x2") ||
            !configurationSection.contains("z1") || !configurationSection.contains("z2")) {
            throw new InvalidConfigurationException("Missing coordinate from keepLoaded area '" + name + "'.");
        }

        x1 = configurationSection.getInt("x1");
        z1 = configurationSection.getInt("z1");
        x2 = configurationSection.getInt("x2");
        z2 = configurationSection.getInt("z2");

        if (x2 < x1) {
            int xT = x2;
            x2 = x1;
            x1 = xT;
        }

        if (z2 < z1) {
            int zT = z2;
            z2 = z1;
            z1 = zT;
        }
    }

    public String getName() {
        return name;
    }

    public boolean includesChunk(Chunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();

        return (x >= x1 && x <= x2 && z >= z1 && z <= z2);
    }

    public void requestLoadOfArea(World world) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                Chunk chunk = world.getChunkAt(x, z);

                if (!chunk.isLoaded()) {
                    chunk.load();
                }
            }
        }
    }
}
