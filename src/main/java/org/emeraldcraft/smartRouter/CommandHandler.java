package org.emeraldcraft.smartRouter;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import org.emeraldcraft.smartRouter.components.ChildServer;
import org.emeraldcraft.smartRouter.pterodaytcl.Pterodactyl;

public class CommandHandler {

    private final SmartRouter smartRouter;

    public CommandHandler(SmartRouter smartRouter) {
        this.smartRouter = smartRouter;
        ProxyServer server = SmartRouter.getProxyServer();

        CommandMeta meta = server.getCommandManager().metaBuilder("smartrouter")
                .aliases("router")
                .plugin(smartRouter)
                .build();

        LiteralCommandNode<CommandSource> routerCommand = BrigadierCommand.literalArgumentBuilder("router")
                .then(createServerListCommand())
                .then(createReloadConfigCommand())
                .then(createStartServerCommand())
                .then(createStopServerCommand())
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

    private LiteralCommandNode<CommandSource> createStartServerCommand() {
        return BrigadierCommand.literalArgumentBuilder("startserver")
                .then(
                        BrigadierCommand.requiredArgumentBuilder("name", StringArgumentType.word())
                                .executes(
                                        context -> {
                                            String serverName = StringArgumentType.getString(context, "name");
                                            ChildServer server = smartRouter.getConfiguration().childServerFromName(serverName);
                                            Pterodactyl.startServer(server, smartRouter.getConfiguration());
                                            return Command.SINGLE_SUCCESS;
                                        }
                                )
                ).build();
    }
    private LiteralCommandNode<CommandSource> createStopServerCommand() {
        return BrigadierCommand.literalArgumentBuilder("stopserver")
                .then(
                        BrigadierCommand.requiredArgumentBuilder("name", StringArgumentType.word())
                                .executes(
                                        context -> {
                                            String serverName = StringArgumentType.getString(context, "name");
                                            ChildServer server = smartRouter.getConfiguration().childServerFromName(serverName);
                                            Pterodactyl.stopServer(server, smartRouter.getConfiguration());
                                            return Command.SINGLE_SUCCESS;
                                        }
                                )
                ).build();
    }

}
