package org.emeraldcraft.smartRouter.manager;

import org.emeraldcraft.smartRouter.SmartRouter;
import org.emeraldcraft.smartRouter.components.ChildServerConfig;
import org.emeraldcraft.smartRouter.components.Configuration;

import java.util.ArrayList;
import java.util.List;

public class ServerManager {

    private final List<ChildServer> childServers = new ArrayList<>();

    public ServerManager(Configuration configuration) {
        for (ChildServerConfig configuredChildServer : configuration.getConfiguredChildServers()) {
            ChildServer childServer = new ChildServer(configuredChildServer);
            childServer.fetchData();
            childServers.add(childServer);
        }
        SmartRouter.getLogger().info("Fetched all of the server states.");
    }

    public StartResponse startServer(ChildServerConfig childServerConfig) {
        ChildServer server = fromConfig(childServerConfig);
        return server.start();
    }

    public void shutdownServerNow(ChildServerConfig childServerConfig) {
        ChildServer server = fromConfig(childServerConfig);
        server.shutdownNow();
    }
    public void delayedShutdownServerNow(ChildServerConfig childServerConfig) {
        ChildServer server = fromConfig(childServerConfig);
        server.delayedShutdown();
    }

    public void cancelStopTimer(ChildServerConfig childServerConfig) {
        ChildServer server = fromConfig(childServerConfig);
        server.cancelStopTimer();
    }

    public ChildServer fromConfig(ChildServerConfig childServerConfig) {
        ChildServer server = null;
        for (ChildServer childServer : childServers) {
            if(childServer.getChildServerConfig().configName().equals(childServerConfig.configName())) {
                server = childServer;
                break;
            }
        }
        if(server == null) throw new IllegalArgumentException("Illegal Child Server Configuration");
        return server;
    }





}
