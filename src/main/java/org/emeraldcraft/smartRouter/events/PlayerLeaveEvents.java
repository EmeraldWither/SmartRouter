package org.emeraldcraft.smartRouter.events;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import org.emeraldcraft.smartRouter.SmartRouter;

public class PlayerLeaveEvents {
    private final SmartRouter smartRouter;
    public PlayerLeaveEvents(SmartRouter smartRouter) {
        this.smartRouter = smartRouter;
    }
    @Subscribe
    public void onPlayerLeave(DisconnectEvent event) {
        if(SmartRouter.getProxyServer().getPlayerCount() == 0 && smartRouter.getConfiguration().getSelectedServer().autoStart()) {
            SmartRouter.getLogger().info("Everyone has left the proxy. Starting the stop timer.");
            SmartRouter.getInstance().getServerManager().delayedShutdownServer(smartRouter.getConfiguration().getSelectedServer());
        }
    }
}
