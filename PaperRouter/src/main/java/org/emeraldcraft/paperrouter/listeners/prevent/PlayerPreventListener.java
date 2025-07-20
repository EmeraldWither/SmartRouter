package org.emeraldcraft.paperrouter.listeners.prevent;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerPreventListener implements Listener {




    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        event.setCancelled(true);
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);
    }
}
