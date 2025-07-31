package org.emeraldcraft.velocityRouter;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
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
    public void onPreLogin(PreLoginEvent event) {
        if (VelocityRouter.getConfiguration().isMaintenance()) {
            PreLoginEvent.PreLoginComponentResult denied = PreLoginEvent.PreLoginComponentResult.denied(Component.text(VelocityRouter.getConfiguration().getMaintenanceMessage()).color(NamedTextColor.RED));
            event.setResult(denied);
            VelocityRouter.getLogger().info("[prelogin] Player %s attempted to join during maintenance mode.".formatted(event.getUsername()));
            return;
        }
        PreLoginEvent.PreLoginComponentResult denied = PreLoginEvent.PreLoginComponentResult.denied(Component.text("You are not whitelisted to be part of the network.").color(NamedTextColor.RED));
        if(event.getUniqueId() == null) {
            event.setResult(denied);
            VelocityRouter.getLogger().warn("[prelogin] Unable to find a UUID for the ip '%s'; has been kicked.".formatted(event.getConnection().getRemoteAddress().getHostString()));
            return;
        }
        if (!VelocityRouter.getConfiguration().getAllowList().contains(event.getUniqueId().toString())) {
            event.setResult(denied);
            VelocityRouter.getLogger().warn("[prelogin] Player %s (UUID %s) attempted to join but is not on the allowlist.".formatted(event.getUsername(), event.getUniqueId()));
        }
    }
    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
        if (VelocityRouter.getConfiguration().isMaintenance()) {
            ResultedEvent.ComponentResult denied = LoginEvent.ComponentResult.denied(Component.text(VelocityRouter.getConfiguration().getMaintenanceMessage()));
            event.getPlayer().disconnect(denied.getReasonComponent().get());
            event.setResult(denied);
            VelocityRouter.getLogger().info("Player %s attempted to join during maintenance mode.".formatted(event.getPlayer().getUsername()));
            return;
        }
        //check for allowlist

        if (!VelocityRouter.getConfiguration().getAllowList().contains(event.getPlayer().getUniqueId().toString())) {
            ResultedEvent.ComponentResult denied = LoginEvent.ComponentResult.denied(Component.text("You are not whitelisted to be part of the network.").color(NamedTextColor.RED));
            event.getPlayer().disconnect(denied.getReasonComponent().get());
            event.setResult(denied);
            VelocityRouter.getLogger().warn("[login] Player %s (UUID %s) attempted to join but is not on the allowlist.".formatted(event.getPlayer().getUsername(), event.getPlayer().getUniqueId().toString()));
            return;
        }

        //check for selected server
        event.setResult(ResultedEvent.ComponentResult.allowed());
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