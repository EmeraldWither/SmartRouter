package org.emeraldcraft.velocityRouter.serverapi.instance;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.emeraldcraft.velocityRouter.VelocityRouter;
import org.emeraldcraft.velocityRouter.serverapi.components.*;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Instance {
    private final List<ChildServer> servers = new ArrayList<>();
    private final String instanceID;
    public Instance(Configuration configuration) {
        instanceID = configuration.getSelectedServer().awsInstanceID();
        for (ChildServerConfig configuredChildServer : configuration.getConfiguredChildServers()) {
            if(configuredChildServer.awsInstanceID().equals(instanceID)) {
                servers.add(VelocityRouter.getServerManager().fromConfig(configuredChildServer));
            }
        }
    }

    public void attemptSafeInstanceShutdown() {
        for (ChildServer server : servers) {

            Optional<RegisteredServer> velServer = VelocityRouter.getProxyServer().getServer(server.getChildServerConfig().configName());
            if(velServer.isPresent()) {
                if(velServer.get().getServerInfo().getName().equals("limbo")) continue;

                Collection<Player> playersConnected = velServer.get().getPlayersConnected();
                if(!playersConnected.isEmpty()) return;
                server.fetchData();
                if(server.getServerState() != ServerState.SERVER_OFFLINE) return;
            }
        }

        VelocityRouter.getConfiguration().getEc2Client().stopInstances(StopInstancesRequest.builder().instanceIds(instanceID).build());
    }
}
