package org.emeraldcraft.paperrouter;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;
import org.emeraldcraft.paperrouter.listeners.PlayerServerMenuListener;
import org.emeraldcraft.paperrouter.listeners.prevent.PlayerPreventListener;
import org.emeraldcraft.paperrouter.serverapi.manager.ServerManager;

import java.util.logging.Logger;

public final class PaperRouter extends JavaPlugin {

    private static Logger logger;
    private static Configuration configuration;
    private static ServerManager serverManager;

    @Override
    public void onEnable() {
        logger = super.getLogger();
        saveDefaultConfig();
        reloadConfig();
        configuration = new Configuration(getConfig());
        try {
            configuration.load();
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
        serverManager = new ServerManager(configuration);
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(new PlayerPreventListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerServerMenuListener(), this);
        Bukkit.getServerTickManager().setFrozen(true);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        deleteArea();
    }

    public void deleteArea() {
        World world = Bukkit.getWorld("world");
        for(int x = 0; x <= 40; x++) {
            for(int y = -64; y <= -44; y++) {
                for(int z = 0; z <= 40; z++) {
                    assert world != null;
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                    world.setBiome(x, y, z, Biome.THE_END);
                }
            }
        }
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setTime(0);
        world.setClearWeatherDuration(0);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getServerTickManager().setFrozen(false);
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static ServerManager getServerManager() {
        return serverManager;
    }

    public static Logger logger() {
        return logger;
    }
}
