package org.emeraldcraft.smartRouter;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.emeraldcraft.smartRouter.components.Configuration;
import org.emeraldcraft.smartRouter.events.PlayerLoginEvents;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Plugin(id = "smartrouter", name = "SmartRouter", authors = "EmerqldWither", version = BuildConstants.VERSION, description = "Smart Router for EmeraldCraft Servers")
public class SmartRouter {

    @Inject
    private ProxyServer server;

    @Inject
    private Logger logger;

    @DataDirectory
    @Inject
    private Path dataDirectory;

    private Configuration configuration;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws ConfigurateException {
        reloadConfiguration();
        new CommandHandler(this);
        server.getEventManager().register(this, new PlayerLoginEvents(this));
        logger.info("SmartRouter has been initialized!");
    }



    @Subscribe
    public void onPlayerMOTDPing(ProxyPingEvent event) {
        Optional<Favicon> favicon = server.getConfiguration().getFavicon();
        ServerPing.Players players = new ServerPing.Players(1, 1, List.of(new ServerPing.SamplePlayer("EmerqldWither", UUID.randomUUID())));
        ServerPing ping;

        if(configuration.isMaintenance()) ping = new ServerPing(event.getPing().getVersion(), players, buildConfigurationMOTD(configuration.getMaintenanceMessage()), favicon.orElse(null));
        else ping = new ServerPing(event.getPing().getVersion(), players, buildConfigurationMOTD(configuration.getSelectedServer().displayName()), favicon.orElse(null));
        event.setPing(ping);
    }

    private static Component buildConfigurationMOTD(String server) {
        return Component.empty().append(Component.text("Emerald", NamedTextColor.DARK_GREEN).decorate(TextDecoration.BOLD)).append(Component.text("Craft", NamedTextColor.GREEN).append(Component.text(" SmartRouter Proxy", NamedTextColor.GOLD))).decorate().appendNewline()
                .append(Component.text("Current Configuration: ", NamedTextColor.RED)).append(Component.text(server, NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD));
    }
    public ProxyServer getProxyServer() {
        return server;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void reloadConfiguration() throws ConfigurateException {
        this.configuration = new Configuration(dataDirectory, this);
        this.configuration.load();
    }

    public Logger getLogger() {
        return logger;
    }
}
