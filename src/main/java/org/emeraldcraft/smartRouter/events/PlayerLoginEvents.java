package org.emeraldcraft.smartRouter.events;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.emeraldcraft.smartRouter.SmartRouter;
import org.emeraldcraft.smartRouter.components.ChildServer;
import org.emeraldcraft.smartRouter.components.Configuration;
import org.emeraldcraft.smartRouter.pterodaytcl.Pterodactyl;

import java.util.Optional;
import java.util.logging.Logger;

public class PlayerLoginEvents {
    private final SmartRouter smartRouter;

    public PlayerLoginEvents(SmartRouter smartRouter) {
        this.smartRouter = smartRouter;
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        if (smartRouter.getConfiguration().isMaintenance()) {
            PreLoginEvent.PreLoginComponentResult denied = PreLoginEvent.PreLoginComponentResult.denied(Component.text(smartRouter.getConfiguration().getMaintenanceMessage()).color(NamedTextColor.RED));
            event.setResult(denied);
            SmartRouter.getLogger().info("[prelogin] Player %s attempted to join during maintenance mode.".formatted(event.getUsername()));
            return;
        }
        if (!smartRouter.getConfiguration().getAllowList().contains(event.getUniqueId().toString())) {
            PreLoginEvent.PreLoginComponentResult denied = PreLoginEvent.PreLoginComponentResult.denied(Component.text("You are not whitelisted to be part of the network.").color(NamedTextColor.RED));
            event.setResult(denied);
            SmartRouter.getLogger().warn("[prelogin] Player %s (UUID %s) attempted to join but is not on the allowlist.".formatted(event.getUsername(), event.getUniqueId()));
        }
    }
    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
//        if (smartRouter.getConfiguration().isMaintenance()) {
//            ResultedEvent.ComponentResult denied = LoginEvent.ComponentResult.denied(Component.text(smartRouter.getConfiguration().getMaintenanceMessage()));
//            event.getPlayer().disconnect(denied.getReasonComponent().get());
//            event.setResult(denied);
//            smartRouter.getLogger().info("Player %s attempted to join during maintenance mode.".formatted(event.getPlayer().getUsername()));
//            return;
//        }
        //check for allowlist

        if (!smartRouter.getConfiguration().getAllowList().contains(event.getPlayer().getUniqueId().toString())) {
            ResultedEvent.ComponentResult denied = LoginEvent.ComponentResult.denied(Component.text("You are not whitelisted to be part of the network.").color(NamedTextColor.RED));
            event.getPlayer().disconnect(denied.getReasonComponent().get());
            event.setResult(denied);
            SmartRouter.getLogger().warn("[login] Player %s (UUID %s) attempted to join but is not on the allowlist.".formatted(event.getPlayer().getUsername(), event.getPlayer().getUniqueId().toString()));
            return;
        }

        //check for selected server
        event.setResult(ResultedEvent.ComponentResult.allowed());
    }
    @Subscribe
    public void onChooseServer(PlayerChooseInitialServerEvent event) {
        Configuration configuration = smartRouter.getConfiguration();
        ChildServer selectedServer = configuration.getSelectedServer();
        Optional<RegisteredServer> server = SmartRouter.getProxyServer().getServer(selectedServer.configName());
        if(server.isPresent()) {
            //check for instance state, or start server

            String state = Pterodactyl.getServerState(selectedServer, configuration);
            if (state.equalsIgnoreCase("stopped")) {
                Pterodactyl.startServer(selectedServer, configuration);
                SmartRouter.getLogger().info("Starting the server %s!".formatted(selectedServer.displayName()));
                event.getPlayer().disconnect(Component.text("You have started the server %s".formatted(selectedServer.displayName())).color(NamedTextColor.GREEN));
                return;
            }
            if (state.equalsIgnoreCase("starting")) {
                event.getPlayer().disconnect(Component.text("The server %s is still starting.".formatted(selectedServer.displayName())).color(NamedTextColor.GOLD));
                return;
            }

            if (state.equalsIgnoreCase("stopping")) {
                SmartRouter.getLogger().info("Server is stopping. Cannot run any actions.");
                event.getPlayer().disconnect(Component.text("The server %s is stopping, and cannot have actions run on it. Try again in a bit...".formatted(selectedServer.displayName())).color(NamedTextColor.RED));
                return;
            }


            event.setInitialServer(server.get());
            event.getPlayer().sendMessage(Component.text("You have been sent to %s".formatted(selectedServer.displayName())).color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC));
            //stop all instance stop timers
            Pterodactyl.stopAllTimers();
        }
        else {
            event.getPlayer().disconnect(Component.text("An invalid configuration was detected. Please contact an admin."));
            SmartRouter.getLogger().error("Unable to find selected server %s in configuration during PlayerChooseInitalServerEvent.".formatted(selectedServer.configName()));
        }
    }

}
