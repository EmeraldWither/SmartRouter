package org.emeraldcraft.paperrouter.serverapi.manager;

import org.bukkit.Server;

public enum SimpleServerState {
    OFFLINE,
    STARTING,
    ONLINE,
    STOPPING,
    UNKNOWN;

    public static SimpleServerState fromServerState(ServerState state) {
        if(state == ServerState.INSTANCE_STOPPED) return OFFLINE;
        if(state == ServerState.SERVER_STARTING || state == ServerState.SERVER_OFFLINE || state == ServerState.SERVER_UNREACHABLE) return STARTING;
        if(state == ServerState.INSTANCE_STOPPING || state == ServerState.SERVER_STOPPING) return STOPPING;
        if(state == ServerState.SERVER_ONLINE) return ONLINE;
        return UNKNOWN;
    }
}

