package org.emeraldcraft.smartRouter.components;

import org.jetbrains.annotations.NotNull;

public record ChildServerConfig(@NotNull String configName, @NotNull String displayName, @NotNull String pteroServerID, @NotNull String awsInstanceID, boolean autoStart) {
    @Override
    public @NotNull String toString() {
        return "ChildServerConfig{" +
                "configName='" + configName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", pteroServerID='" + pteroServerID + '\'' +
                ", awsInstanceID='" + awsInstanceID + '\'' +
                ", autoStart=" + autoStart +
                '}';
    }
}
