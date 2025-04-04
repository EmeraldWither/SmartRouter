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

import java.util.Optional;

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
            smartRouter.getLogger().info("[prelogin] Player %s attempted to join during maintenance mode.".formatted(event.getUsername()));
            return;
        }
        if (!smartRouter.getConfiguration().getAllowList().contains(event.getUniqueId().toString())) {
            PreLoginEvent.PreLoginComponentResult denied = PreLoginEvent.PreLoginComponentResult.denied(Component.text("You are not whitelisted to be part of the network.").color(NamedTextColor.RED));
            event.setResult(denied);
            smartRouter.getLogger().warn("[prelogin] Player %s (UUID %s) attempted to join but is not on the allowlist.".formatted(event.getUsername(), event.getUniqueId()));
            return;
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
            smartRouter.getLogger().warn("[login] Player %s (UUID %s) attempted to join but is not on the allowlist.".formatted(event.getPlayer().getUsername(), event.getPlayer().getUniqueId().toString()));
            return;
        }

        //check for selected server
        event.setResult(ResultedEvent.ComponentResult.allowed());
    }
    @Subscribe
    public void onChooseServer(PlayerChooseInitialServerEvent event) {
        Optional<RegisteredServer> server = smartRouter.getProxyServer().getServer(smartRouter.getConfiguration().getSelectedServer().configName());
        if(server.isPresent()) {
            event.setInitialServer(server.get());
            event.getPlayer().sendMessage(Component.text("You have been sent to %s".formatted(smartRouter.getConfiguration().getSelectedServer().displayName())).color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC));
        }
        else {
            event.getPlayer().disconnect(Component.text("An invalid configuration was detected. Please contact an admin."));
            smartRouter.getLogger().error("Unable to find selected server %s in configuration during PlayerChooseInitalServerEvent.".formatted(smartRouter.getConfiguration().getSelectedServer().configName()));
        }
    }

}
