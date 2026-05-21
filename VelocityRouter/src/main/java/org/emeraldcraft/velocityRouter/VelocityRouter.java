package org.emeraldcraft.velocityRouter;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.emeraldcraft.velocityRouter.serverapi.components.Configuration;
import org.emeraldcraft.velocityRouter.serverapi.components.ServerManager;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Plugin(
    id = "velocityrouter",
    name = "VelocityRouter",
    version = BuildConstants.VERSION
)
public class VelocityRouter {

    @Inject private Logger logger;
    @Inject private ProxyServer server;
    private static VelocityRouter instance;
    private Configuration configuration;
    private ServerManager manager;

    @DataDirectory
    @Inject
    private Path dataDirectory;


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws ConfigurateException {
        instance = this;
        configuration = new Configuration(dataDirectory);
        configuration.load();
        manager = new ServerManager(configuration);
        getProxyServer().getEventManager().register(this, new PlayerConnectionEvents());

        CommandMeta lobby = getProxyServer().getCommandManager().metaBuilder("lobby").plugin(this).build();
        CommandMeta shout = getProxyServer().getCommandManager().metaBuilder("shout").plugin(this).build();
        getProxyServer().getCommandManager().register(lobby, new LobbyCommand());
        getProxyServer().getCommandManager().register(shout, new ShoutCommand());
    }

    @Subscribe
    public void onPlayerMOTDPing(ProxyPingEvent event) {
        Optional<Favicon> favicon = server.getConfiguration().getFavicon();

        ServerPing.Version version = event.getPing().getVersion();
        if(configuration.isMaintenance()) {
            version = new ServerPing.Version(1, "Under Maintenance");
        }
        ServerPing.Players players = new ServerPing.Players(1, 1, List.of(new ServerPing.SamplePlayer("EmerqldWither", UUID.randomUUID())));
        ServerPing ping = new ServerPing(version, players, buildConfigurationMOTD(configuration), favicon.orElse(null));
        event.setPing(ping);
    }

    private static Component buildConfigurationMOTD(Configuration configuration) {
        return Component.empty().append(Component.text("Emerald", NamedTextColor.DARK_GREEN).decorate(TextDecoration.BOLD)).append(Component.text("Craft", NamedTextColor.GREEN).append(Component.text(" SmartRouter Proxy", NamedTextColor.GOLD))).decorate().appendNewline()
                .append(buildCurrentServerMOTD(configuration));
    }
    private static Component buildCurrentServerMOTD(Configuration configuration) {
        if(configuration.isMaintenance()) return Component.text(configuration.getMaintenanceMessage()).color(NamedTextColor.RED).decorate(TextDecoration.BOLD);
        return Component.text("Now with V3 Server Manager!").color(NamedTextColor.DARK_AQUA);
    }

    public static VelocityRouter getInstance() {
        return instance;
    }
    public static ProxyServer getProxyServer() {
        return instance.server;
    }

    public static Logger getLogger() {
        return instance.logger;
    }

    public static Configuration getConfiguration() {
        return instance.configuration;
    }

    public static ServerManager getServerManager() {
        return instance.manager;
    }


    static class LobbyCommand implements SimpleCommand {

        @Override
        public void execute(Invocation invocation) {
            Optional<UUID> uuid = invocation.source().get(Identity.UUID);
            if(uuid.isPresent()) {
                Optional<Player> player = VelocityRouter.getProxyServer().getPlayer(uuid.get());
                player.ifPresent(value -> value.createConnectionRequest(VelocityRouter.getProxyServer().getServer("limbo").get()).connect());
            }
        }
    }
    static class ShoutCommand implements SimpleCommand {

        @Override
        public void execute(Invocation invocation) {
            Optional<UUID> uuid = invocation.source().get(Identity.UUID);
            StringBuilder str = new StringBuilder();
            for (String argument : invocation.arguments()) {
                str.append(argument).append(" ");
            }
            String msg = str.toString();
            if(uuid.isPresent()) {
                Optional<Player> player = VelocityRouter.getProxyServer().getPlayer(uuid.get());
               if(player.isEmpty()) return;
               Player play = player.get();
                String name = play.getGameProfile().getName();
                String serverName =  VelocityRouter.getConfiguration().childServerFromName(play.getCurrentServer().get().getServerInfo().getName()).displayName();

                for(Player player1 : VelocityRouter.getProxyServer().getAllPlayers()) {
                    player1.sendMessage(Component.text("[" + serverName.toUpperCase() + "] ").color(NamedTextColor.GRAY).append(
                            Component.text("<" + name + "> ").color(NamedTextColor.WHITE)
                                    .append(
                                            Component.text(msg).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)
                                    )
                    ));
                }
            }
        }
    }
}


