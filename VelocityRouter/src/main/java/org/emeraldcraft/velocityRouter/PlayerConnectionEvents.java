package org.emeraldcraft.velocityRouter;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.emeraldcraft.velocityRouter.serverapi.components.ChildServerConfig;

import java.time.Duration;
import java.util.HashMap;

public class PlayerConnectionEvents {

    private final HashMap<Player, RegisteredServer> lastServer = new HashMap<>();

    @Subscribe
    public void onPlayerLeave(DisconnectEvent event) {
        RegisteredServer server = lastServer.get(event.getPlayer());
        VelocityRouter.getProxyServer().getAllPlayers().forEach(player -> {
            player.sendMessage(Component.text(event.getPlayer().getUsername() + " has disconnected from the network.").color(NamedTextColor.YELLOW).decorate(TextDecoration.ITALIC));
        });
        if(server == null) return;
        System.out.println("not null disconnect");
        if(server.getServerInfo().getName().equals("limbo")) return;
        System.out.println("is not limbo");
        VelocityRouter.getProxyServer().getScheduler().buildTask(VelocityRouter.getInstance(), () -> {
            if(server.getPlayersConnected().isEmpty()) {
                System.out.println("empty player list");
                VelocityRouter.getLogger().info("Everyone has left that server. Starting the stop timer.");
                VelocityRouter.getServerManager().delayedShutdownServer(VelocityRouter.getConfiguration().childServerFromName(server.getServerInfo().getName()));

            }
        }).delay(Duration.ofSeconds(2)).schedule();
    }

    @Subscribe
    public void onPlayerSwitch(ServerConnectedEvent event) {
        lastServer.put(event.getPlayer(), event.getServer());
        if(event.getPreviousServer().isPresent()) {
            RegisteredServer registeredServer = event.getPreviousServer().get();
            if(registeredServer.getServerInfo().getName().equals("limbo")) {
                ChildServerConfig config = VelocityRouter.getConfiguration().childServerFromName(event.getServer().getServerInfo().getName());
                VelocityRouter.getServerManager().cancelStopTimer(config);
                VelocityRouter.getProxyServer().getAllPlayers().forEach(player -> {
                    player.sendMessage(Component.text(event.getPlayer().getUsername() + " has connected to %s.".formatted(config.displayName())).color(NamedTextColor.YELLOW).decorate(TextDecoration.ITALIC));
                });
                return;
            }
            ChildServerConfig config = VelocityRouter.getConfiguration().childServerFromName(registeredServer.getServerInfo().getName());
            if(registeredServer.getPlayersConnected().isEmpty()) {
                VelocityRouter.getLogger().info("Everyone has left " + config.displayName() + ". Starting the delayed shutdown timer");
                VelocityRouter.getServerManager().delayedShutdownServer(config);
            }
            else {
                VelocityRouter.getServerManager().cancelStopTimer(config);
            }
        }
        RegisteredServer server = event.getServer();
        if(server.getServerInfo().getName().equals("limbo")) {
            VelocityRouter.getProxyServer().getAllPlayers().forEach(player -> {
                player.sendMessage(Component.text(event.getPlayer().getUsername() + " has connected to the Hub.").color(NamedTextColor.YELLOW).decorate(TextDecoration.ITALIC));
            });
            return;
        }
    }
}