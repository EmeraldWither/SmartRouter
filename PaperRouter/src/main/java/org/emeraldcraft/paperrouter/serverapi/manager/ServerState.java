package org.emeraldcraft.paperrouter.serverapi.manager;

public enum ServerState {
    UNKNOWN,
    SERVER_UNREACHABLE,
    SERVER_STARTING,
    SERVER_ONLINE,
    SERVER_STOPPING,
    SERVER_OFFLINE,
    INSTANCE_STOPPING
}
