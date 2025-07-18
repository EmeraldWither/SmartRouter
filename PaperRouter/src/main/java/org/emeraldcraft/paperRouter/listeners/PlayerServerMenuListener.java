package org.emeraldcraft.paperRouter.listeners;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.emeraldcraft.paperRouter.ItemBuilder;
import org.emeraldcraft.paperRouter.PaperRouter;
import org.emeraldcraft.paperRouter.PlayerBookTask;

public class PlayerServerMenuListener implements Listener {

    private BukkitTask task;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ItemBuilder.createPlayerInventory(event.getPlayer());
    }
    @EventHandler
    public void onPlayerLeave(PlayerConnectionCloseEvent event) {
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        NamespacedKey key = new NamespacedKey(JavaPlugin.getPlugin(PaperRouter.class), "compass");
        if (event.getItem() == null) return;
        if (event.getItem().getPersistentDataContainer().has(key)) {
            Byte b = event.getItem().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
            if(b == null) return;
            if(b == ItemBuilder.COMPASS_KEY) {
                Player player = event.getPlayer();
                task = Bukkit.getScheduler().runTaskTimer(JavaPlugin.getProvidingPlugin(PaperRouter.class), new PlayerBookTask(player), 0, 2);
            }
        }
    }

}
