package org.emeraldcraft.paperrouter.listeners;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.emeraldcraft.paperrouter.book.ItemBuilder;
import org.emeraldcraft.paperrouter.PaperRouter;
import org.emeraldcraft.paperrouter.book.PlayerBookTask;

import java.util.HashMap;

public class PlayerServerMenuListener implements Listener {

    private final HashMap<Player, BukkitTask> tasks = new HashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().setGameMode(GameMode.SPECTATOR);
        event.getPlayer().setFlying(true);
        var task = Bukkit.getScheduler().runTaskTimer(JavaPlugin.getProvidingPlugin(PaperRouter.class), new PlayerBookTask(event.getPlayer()), 0, 2);
        tasks.put(event.getPlayer(), task);
    }
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        if(tasks.get(event.getPlayer()) == null) return;
        tasks.get(event.getPlayer()).cancel();
        tasks.remove(event.getPlayer());
    }


}
