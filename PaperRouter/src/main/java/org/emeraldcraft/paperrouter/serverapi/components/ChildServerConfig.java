package org.emeraldcraft.paperrouter.serverapi.components;

import org.jetbrains.annotations.NotNull;

public record ChildServerConfig(@NotNull String configName, @NotNull String displayName, @NotNull String pteroServerID, boolean autoStart) {
    @Override
    public @NotNull String toString() {
        return "ChildServerConfig{" +
                "configName='" + configName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", pteroServerID='" + pteroServerID + '\'' +
                ", autoStart=" + autoStart +
                '}';
    }
}
