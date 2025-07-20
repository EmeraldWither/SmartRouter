package org.emeraldcraft.velocityRouter.serverapi;

import org.emeraldcraft.velocityRouter.serverapi.components.ChildServer;
import org.emeraldcraft.velocityRouter.VelocityRouter;
import org.emeraldcraft.velocityRouter.serverapi.components.ChildServerConfig;
import org.emeraldcraft.velocityRouter.serverapi.components.Configuration;
import org.emeraldcraft.velocityRouter.serverapi.components.StartResponse;

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
        VelocityRouter.getLogger().info("Fetched all of the server states.");
    }

    public StartResponse startServer(ChildServerConfig childServerConfig) {
        ChildServer server = fromConfig(childServerConfig);
        return server.start();
    }

    public void shutdownServerNow(ChildServerConfig childServerConfig) {
        ChildServer server = fromConfig(childServerConfig);
        server.shutdownNow();
    }
    public void delayedShutdownServer(ChildServerConfig childServerConfig) {
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
