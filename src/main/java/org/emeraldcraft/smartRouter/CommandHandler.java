package org.emeraldcraft.smartRouter;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import org.emeraldcraft.smartRouter.components.ChildServer;

public class CommandHandler {

    private final SmartRouter smartRouter;
    private final ProxyServer server;

    public CommandHandler(SmartRouter smartRouter) {
        this.smartRouter = smartRouter;
        this.server = smartRouter.getProxyServer();

        CommandMeta meta = server.getCommandManager().metaBuilder("smartrouter")
                .aliases("router")
                .plugin(smartRouter)
                .build();

        LiteralCommandNode<CommandSource> routerCommand = BrigadierCommand.literalArgumentBuilder("router")
                .then(createServerListCommand())
                .then(createReloadConfigCommand())
                .build();
        server.getCommandManager().register(meta, new BrigadierCommand(routerCommand));

    }
    private LiteralArgumentBuilder<CommandSource> createServerListCommand() {
        return BrigadierCommand.literalArgumentBuilder("list")
                .executes(
                        context -> {
                            StringBuilder serverList = new StringBuilder("Configured servers: \n");
                            context.getSource().sendPlainMessage("Current Server: " + smartRouter.getConfiguration().getSelectedServer().displayName());
                            for (ChildServer server : smartRouter.getConfiguration().getConfiguredChildServers()) {
                                serverList.append("ConfigName: ").append(server.configName())
                                        .append(", DisplayName: ").append(server.displayName())
                                        .append(", PteroServerID: ").append(server.pteroServerID())
                                        .append(", AutoStart: ").append(server.autoStart())
                                        .append("\n");
                            }
                            context.getSource().sendPlainMessage(serverList.toString());
                            return Command.SINGLE_SUCCESS;
                        }
                );
    }

    private LiteralArgumentBuilder<CommandSource> createReloadConfigCommand() {
        return BrigadierCommand.literalArgumentBuilder("reload")
                .executes(
                        context -> {
                            try {
                                smartRouter.reloadConfiguration();
                                context.getSource().sendPlainMessage("Configuration reloaded successfully.");
                            } catch (Exception e) {
                                context.getSource().sendPlainMessage("Failed to reload configuration: " + e.getMessage());
                            }
                            return Command.SINGLE_SUCCESS;
                        }
                );
    }
}
