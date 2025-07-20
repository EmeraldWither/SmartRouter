package org.emeraldcraft.velocityRouter.serverapi;

import org.emeraldcraft.velocityRouter.VelocityRouter;
import org.emeraldcraft.velocityRouter.serverapi.instance.Instance;

public class InstanceManager {

    private final Instance instance = new Instance(VelocityRouter.getConfiguration());

    public void stopInstance() {

        instance.attemptSafeInstanceShutdown();

    }


}
