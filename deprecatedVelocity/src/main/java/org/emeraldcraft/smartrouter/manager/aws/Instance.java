package org.emeraldcraft.smartrouter.manager.aws;

import org.emeraldcraft.smartrouter.components.ChildServerConfig;

import java.util.List;

public record Instance(List<ChildServerConfig> children, String instanceID){}
