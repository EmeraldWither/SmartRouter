package org.emeraldcraft.smartRouter.manager.aws;

import org.emeraldcraft.smartRouter.SmartRouter;
import org.emeraldcraft.smartRouter.components.ChildServerConfig;

import java.util.List;

public record Instance(List<ChildServerConfig> children, String instanceID){}
