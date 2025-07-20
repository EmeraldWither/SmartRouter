package org.emeraldcraft.velocityRouter.serverapi.components;

public enum ServerState {
    UNKNOWN,
    INSTANCE_STOPPED,
    SERVER_UNREACHABLE,
    SERVER_STARTING,
    SERVER_ONLINE,
    SERVER_STOPPING,
    SERVER_OFFLINE,
    INSTANCE_STOPPING
}
