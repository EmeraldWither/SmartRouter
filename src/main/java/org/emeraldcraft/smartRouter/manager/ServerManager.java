package org.emeraldcraft.smartRouter.manager;

import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import org.emeraldcraft.smartRouter.SmartRouter;
import org.emeraldcraft.smartRouter.components.ChildServerConfig;
import org.emeraldcraft.smartRouter.components.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

    public void startServer(ChildServerConfig childServerConfig) {
        ChildServer server = null;
        for (ChildServer childServer : childServers) {
            if(childServer.getChildServerConfig().configName().equals(childServerConfig.configName())) {
                server = childServer;
                break;
            }
        }
        if(server == null) {
            throw new IllegalArgumentException("Illegal Child Server Configuration");
        }
        if(server.getServerState() == ServerState.SERVER_ONLINE) {
            return;
        }
        if(server.getServerState() == ServerState.UNKNOWN) {
            return;
        }

        server.start();

    }



}
