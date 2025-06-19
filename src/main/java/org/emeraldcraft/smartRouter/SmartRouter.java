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
import org.emeraldcraft.smartRouter.events.PlayerLeaveEvents;
import org.emeraldcraft.smartRouter.events.PlayerLoginEvents;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;
import webserver.ServerIDProvider;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Plugin(id = "smartrouter", name = "SmartRouter", authors = "EmerqldWither", version = BuildConstants.VERSION, description = "Smart Router for EmeraldCraft Servers")
public class SmartRouter {

    @Inject
    private ProxyServer server;
    private static ProxyServer staticServer;
    private static SmartRouter instance;

    @Inject
    private Logger logger;

    private static Logger staticLogger;

    @DataDirectory
    @Inject
    private Path dataDirectory;

    private Configuration configuration;

    private ServerIDProvider serverIDProvider;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws ConfigurateException {
        try{
            instance = this;
            staticServer = server;
            staticLogger = logger;
            logger.info("SmartRouter is initializing...");
            logger.info("Data Directory: {}", dataDirectory.toAbsolutePath());
            logger.info("Loading configuration...");
            reloadConfiguration();
            logger.info("Registering commands...");
            new CommandHandler(this);
            logger.info("Registering events...");
            server.getEventManager().register(this, new PlayerLoginEvents(this));
            server.getEventManager().register(this, new PlayerLeaveEvents(this));
            logger.info("Creating the webserver...");
            serverIDProvider = new ServerIDProvider();
            serverIDProvider.init();

            logger.info("SmartRouter has been initialized!");
        } catch (Exception e) {
            logger.error("SmartRouter could not be initialized!", e);
        }
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
                .append(Component.text("Current Server: ", NamedTextColor.RED)).append(Component.text(server, NamedTextColor.DARK_AQUA).decorate(TextDecoration.BOLD));
    }
    public static ProxyServer getProxyServer() {
        return staticServer;
    }
    public static SmartRouter getInstance() {
        return instance;
    }
    public Configuration getConfiguration() {
        return configuration;
    }

    public void reloadConfiguration() throws ConfigurateException {
        this.configuration = new Configuration(dataDirectory);
        this.configuration.load();
    }

    public static Logger getLogger() {
        return staticLogger;
    }
}
