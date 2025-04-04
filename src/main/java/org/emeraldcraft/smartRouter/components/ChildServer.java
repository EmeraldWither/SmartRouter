package org.emeraldcraft.smartRouter.components;

import org.jetbrains.annotations.NotNull;

public record ChildServer(@NotNull String configName, @NotNull String displayName, @NotNull String pteroServerID, boolean autoStart) {
}
