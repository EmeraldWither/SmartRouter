package org.emeraldcraft.paperrouter.serverapi.manager.aws;

import org.emeraldcraft.paperrouter.Configuration;
import org.emeraldcraft.paperrouter.PaperRouter;
import org.emeraldcraft.paperrouter.serverapi.components.ChildServerConfig;
import org.emeraldcraft.paperrouter.serverapi.manager.ChildServer;
import org.emeraldcraft.paperrouter.serverapi.manager.ServerState;

import java.util.ArrayList;
import java.util.List;

public class InstanceManager {

    private final List<Instance> instances;

    public InstanceManager(Configuration configuration) {
        this.instances = new ArrayList<>();
        for (ChildServerConfig configuredChildServer : configuration.getConfiguredChildServers()) {
            String instanceID = configuredChildServer.awsInstanceID();

            if(instances.stream().anyMatch(instance -> instance.instanceID().equals(instanceID)))
            {
                
            }
            else {
                instances.add(new Instance(new ArrayList<>(), instanceID));
            }
        }
    }


    public void shutdownInstance(String instanceID){
        Instance instance = fromID(instanceID);
        for(ChildServerConfig childConfig : instance.children()){
            ChildServer childServer = PaperRouter.getServerManager().fromConfig(childConfig);
            childServer.fetchNow();
            if(childServer.getServerState() != ServerState.SERVER_OFFLINE) {
                PaperRouter.logger().info("Cannot safely shutdown instance '%s' because its child-servers are still online.".formatted(instance.instanceID()));
                return;
            }
        }
    }
    public Instance fromID(String instanceID){
        Instance server = null;
        for (Instance instance : instances) {
            if(instance.instanceID().equals(instanceID)) {
                server = instance;
                break;
            }
        }
        if(server == null) throw new IllegalArgumentException("Illegal Child Server Configuration");
        return server;
    }

}
