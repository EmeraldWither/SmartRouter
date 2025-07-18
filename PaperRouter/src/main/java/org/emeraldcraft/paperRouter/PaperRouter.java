package org.emeraldcraft.paperRouter;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.emeraldcraft.paperRouter.listeners.prevent.PlayerPreventListener;
import org.emeraldcraft.paperRouter.listeners.PlayerServerMenuListener;

public final class PaperRouter extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(new PlayerPreventListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerServerMenuListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
