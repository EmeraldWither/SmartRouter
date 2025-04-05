package org.emeraldcraft.smartRouter.events;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import org.emeraldcraft.smartRouter.SmartRouter;
import org.emeraldcraft.smartRouter.pterodaytcl.Pterodactyl;

public class PlayerLeaveEvents {
    private final SmartRouter smartRouter;
    public PlayerLeaveEvents(SmartRouter smartRouter) {
        this.smartRouter = smartRouter;
    }
    @Subscribe
    public void onPlayerLeave(DisconnectEvent event) {
        if(SmartRouter.getProxyServer().getPlayerCount() == 0) {
            SmartRouter.getLogger().info("Everyone has left the proxy. Starting the stop timer.");
            Pterodactyl.stopServerDelayed(smartRouter.getConfiguration());
        }
    }
}
