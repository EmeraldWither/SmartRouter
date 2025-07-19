package org.emeraldcraft.paperrouter;

import org.bukkit.Bukkit;
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
