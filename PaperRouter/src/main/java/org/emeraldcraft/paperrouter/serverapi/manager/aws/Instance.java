package org.emeraldcraft.paperrouter.serverapi.manager.aws;


import org.emeraldcraft.paperrouter.serverapi.components.ChildServerConfig;

import java.util.List;

public record Instance(List<ChildServerConfig> children, String instanceID){}
