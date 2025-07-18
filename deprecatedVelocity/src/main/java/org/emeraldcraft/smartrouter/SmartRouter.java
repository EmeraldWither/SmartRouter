package org.emeraldcraft.smartrouter;

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
import org.emeraldcraft.smartrouter.components.Configuration;
import org.emeraldcraft.smartrouter.events.PlayerLeaveEvents;
import org.emeraldcraft.smartrouter.events.PlayerLoginEvents;
import org.emeraldcraft.smartrouter.manager.ServerManager;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Plugin(id = "smartrouter", name = "SmartRouter", authors = "EmerqldWither", version = org.emeraldcraft.smartrouter.BuildConstants.VERSION, description = "Smart Router for EmeraldCraft Servers")
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

    private ServerManager serverManager;

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

            logger.info("Initializating server manager...");
            serverManager = new ServerManager(configuration);

            logger.info("Registering commands...");
            new CommandHandler(this);
            logger.info("Registering events...");
            server.getEventManager().register(this, new PlayerLoginEvents(this));
            server.getEventManager().register(this, new PlayerLeaveEvents(this));

            logger.info("SmartRouter has been initialized!");
        } catch (Exception e) {
            logger.error("SmartRouter could not be initialized!", e);
        }
    }



    public ServerManager getServerManager() {
        return serverManager;
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
        return Component.text("Current Server: ").color(NamedTextColor.DARK_AQUA).append(Component.text(configuration.getSelectedServer().displayName(), NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
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
