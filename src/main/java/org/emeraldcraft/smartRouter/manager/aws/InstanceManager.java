package org.emeraldcraft.smartRouter.manager.aws;

import org.emeraldcraft.smartRouter.SmartRouter;
import org.emeraldcraft.smartRouter.components.ChildServerConfig;
import org.emeraldcraft.smartRouter.components.Configuration;
import org.emeraldcraft.smartRouter.manager.ChildServer;
import org.emeraldcraft.smartRouter.manager.ServerState;

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
            ChildServer childServer = SmartRouter.getInstance().getServerManager().fromConfig(childConfig);
            childServer.fetchData();
            if(childServer.getServerState() != ServerState.SERVER_OFFLINE) {
                SmartRouter.getLogger().info("Cannot safely shutdown instance '%s' because its child-servers are still online.".formatted(instance.instanceID()));
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
