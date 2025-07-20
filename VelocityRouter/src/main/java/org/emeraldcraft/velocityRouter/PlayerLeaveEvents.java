package org.emeraldcraft.velocityRouter;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.emeraldcraft.velocityRouter.serverapi.components.ChildServerConfig;

import java.util.HashMap;

public class PlayerLeaveEvents {

    private final HashMap<Player, RegisteredServer> lastServer = new HashMap<>();

    @Subscribe
    public void onPlayerLeave(DisconnectEvent event) {
        RegisteredServer server = lastServer.get(event.getPlayer());
        if(server.getServerInfo().getName().equals("limbo")) return;
        if(server.getPlayersConnected().isEmpty()) {
            VelocityRouter.getLogger().info("Everyone has left that server. Starting the stop timer.");
            VelocityRouter.getServerManager().delayedShutdownServer(VelocityRouter.getConfiguration().childServerFromName(server.getServerInfo().getName()));

        }
    }

    @Subscribe
    public void onPlayerSwitch(ServerConnectedEvent event) {
        lastServer.put(event.getPlayer(), event.getServer());
        if(event.getPreviousServer().isPresent()) {
            RegisteredServer registeredServer = event.getPreviousServer().get();
            if(registeredServer.getServerInfo().getName().equals("limbo")) return;
            if(registeredServer.getPlayersConnected().isEmpty()) {
                ChildServerConfig config = VelocityRouter.getConfiguration().childServerFromName(registeredServer.getServerInfo().getName());
                VelocityRouter.getLogger().info("Everyone has left " + config.displayName() + ". Starting the delayed shutdown timer");
                VelocityRouter.getServerManager().delayedShutdownServer(config);
            }
        }
        RegisteredServer server = event.getServer();
        if(server.getServerInfo().getName().equals("limbo")) return;
        ChildServerConfig config = VelocityRouter.getConfiguration().childServerFromName(server.getServerInfo().getName());
        VelocityRouter.getServerManager().cancelStopTimer(config);

    }
}