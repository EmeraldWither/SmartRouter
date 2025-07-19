package org.emeraldcraft.paperrouter.serverapi.manager;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.emeraldcraft.paperrouter.Configuration;
import org.emeraldcraft.paperrouter.PaperRouter;
import org.emeraldcraft.paperrouter.serverapi.components.ChildServerConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ServerManager {

    private final List<ChildServer> childServers = new ArrayList<>();

    public ServerManager(Configuration configuration) {
        for (ChildServerConfig configuredChildServer : configuration.getConfiguredChildServers()) {
            ChildServer childServer = new ChildServer(configuredChildServer);
            childServer.fetchNow();
            childServers.add(childServer);
        }
        PaperRouter.logger().info("Fetched all of the server states.");
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


    public void attemptPlayerConnectAndStart(@NotNull Player player, ChildServer server) {
        if(server.getServerState() == ServerState.SERVER_ONLINE) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(server.getChildServerConfig().configName());
            player.sendPluginMessage(JavaPlugin.getProvidingPlugin(PaperRouter.class), "BungeeCord", out.toByteArray());
        }
        else {
            var response = startServer(server.getChildServerConfig());
        }
    }
}
