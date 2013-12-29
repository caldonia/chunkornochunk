package net.caldonia.bukkit.plugins.chunkornochunk;

import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class ChunkOrNoChunk extends JavaPlugin implements Listener {
    private Map<String, WorldHandler> worldHandlers = new HashMap<String, WorldHandler>();

    @Override
    public void onEnable() {
        // Save config out if we don't have one.
        saveDefaultConfig();

        // Load the config
        loadConfig();

        // Cycle through already loaded worlds.
        for (World world : getServer().getWorlds()) {
            WorldHandler worldHandler = worldHandlers.get(world.getName());

            if (worldHandler != null) {
                worldHandler.worldLoad(world);
            }
        }

        // Register listener aspect.
        getServer().getPluginManager().registerEvents(this, this);
	}

    @Override
    public void onDisable() {
        // Unregister this plugin.
        HandlerList.unregisterAll((Listener) this);
	}

    public void loadConfig() {
        Map<String, WorldHandler> newHandlers = new HashMap<String, WorldHandler>();

        reloadConfig();

        Configuration root = getConfig();

        if (!root.contains("worlds")) {
            getLogger().warning("Configuration does not contain worlds section.");
            return;
        }

        ConfigurationSection worlds = root.getConfigurationSection("worlds");

        for (String worldName : worlds.getKeys(false)) {
            try {
                WorldHandler worldHandler = new WorldHandler(this, worldName, worlds.getConfigurationSection(worldName));
                newHandlers.put(worldName, worldHandler);
            } catch (InvalidConfigurationException e) {
                getLogger().warning("Configuration error loading configuration for world '" + worldName + "'.");
            }
        }

        getLogger().info("Configuration file loaded.");

        worldHandlers = newHandlers;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent worldLoadEvent) {
        WorldHandler worldHandler = worldHandlers.get(worldLoadEvent.getWorld().getName());

        if (worldHandler != null) {
            worldHandler.worldLoad(worldLoadEvent.getWorld());
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent chunkLoadEvent) {
        WorldHandler worldHandler = worldHandlers.get(chunkLoadEvent.getWorld().getName());

        if (worldHandler != null) {
            worldHandler.chunkLoad(chunkLoadEvent.getChunk(), chunkLoadEvent.isNewChunk());
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent chunkUnloadEvent) {
        WorldHandler worldHandler = worldHandlers.get(chunkUnloadEvent.getWorld().getName());

        if (worldHandler != null) {
            if (worldHandler.chunkUnload(chunkUnloadEvent.getChunk())) {
                chunkUnloadEvent.setCancelled(true);
            }
        }
    }
}