package org.emeraldcraft.velocityRouter;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.emeraldcraft.velocityRouter.serverapi.InstanceManager;
import org.emeraldcraft.velocityRouter.serverapi.components.Configuration;
import org.emeraldcraft.velocityRouter.serverapi.components.ServerManager;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;

import java.nio.file.Path;

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
    private InstanceManager instanceManager;

    @DataDirectory
    @Inject
    private Path dataDirectory;


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws ConfigurateException {
        instance = this;
        configuration = new Configuration(dataDirectory);
        configuration.load();
        manager = new ServerManager(configuration);
        instanceManager = new InstanceManager();
        getProxyServer().getEventManager().register(this, new PlayerLeaveEvents());
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

    public static InstanceManager getInstanceManager() {
        return  instance.instanceManager;
    }

}


