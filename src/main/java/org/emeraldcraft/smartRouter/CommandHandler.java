package org.emeraldcraft.smartRouter;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.emeraldcraft.smartRouter.components.ChildServerConfig;
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
                .then(createStartTimerCommand())
                .then(createStopTimerCommand())
                .then(createMaintenanceCommand())
                .then(createMaintenanceMessageCommand())
                .then(helpCommand())
                .build();
        server.getCommandManager().register(meta, new BrigadierCommand(routerCommand));

    }

    private LiteralArgumentBuilder<CommandSource> helpCommand() {
        return BrigadierCommand.literalArgumentBuilder("help")
                .executes(context -> {
                    SmartRouter.getProxyServer().sendMessage(
                            Component.text("list - list servers ; reload - reload config; startserver [name] - starts the server ; stopserver [name] - stops the server; maintenance [true/false] - temporarily sets the maintenance value until the next reboot ; starttimer - starts the stop timers to stop the instances ; canceltimer - stops the stop timers do the server does not shut down")
                    );
                    return Command.SINGLE_SUCCESS;
                });
    }

    private LiteralArgumentBuilder<CommandSource> createServerListCommand() {
        return BrigadierCommand.literalArgumentBuilder("list")
                .executes(
                        context -> {
                            StringBuilder serverList = new StringBuilder("Configured servers: \n");
                            context.getSource().sendPlainMessage("Current Server: " + smartRouter.getConfiguration().getSelectedServer().displayName());
                            for (ChildServerConfig server : smartRouter.getConfiguration().getConfiguredChildServers()) {
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
                                            ChildServerConfig server = smartRouter.getConfiguration().childServerFromName(serverName);
                                            SmartRouter.getInstance().getServerManager().startServer(server);
                                            SmartRouter.getProxyServer().sendMessage(Component.text("Starting server..."));
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
                                            ChildServerConfig server = smartRouter.getConfiguration().childServerFromName(serverName);
                                            smartRouter.getServerManager().shutdownServerNow(server);
                                            return Command.SINGLE_SUCCESS;
                                        }
                                )
                ).build();
    }

    private LiteralCommandNode<CommandSource> createStartTimerCommand() {
        return BrigadierCommand.literalArgumentBuilder("starttimer")
                .then(
                        BrigadierCommand.requiredArgumentBuilder("name", StringArgumentType.word())
                                .executes(
                                        context -> {
                                            String serverName = StringArgumentType.getString(context, "name");
                                            ChildServerConfig server = smartRouter.getConfiguration().childServerFromName(serverName);
                                            smartRouter.getServerManager().delayedShutdownServer(server);
                                            return Command.SINGLE_SUCCESS;
                                        }
                                )
                ).build();
    }

    private LiteralCommandNode<CommandSource> createStopTimerCommand() {
        return BrigadierCommand.literalArgumentBuilder("canceltimer")
                .then(BrigadierCommand.requiredArgumentBuilder("name", StringArgumentType.word()).executes(
                        context -> {
                            String name = StringArgumentType.getString(context, "name");
                            ChildServerConfig childServerConfig = SmartRouter.getInstance().getConfiguration().childServerFromName(name);
                            SmartRouter.getInstance().getServerManager().cancelStopTimer(childServerConfig);
                            return Command.SINGLE_SUCCESS;
                        }
                ))
                .build();
    }

    private LiteralCommandNode<CommandSource> createMaintenanceCommand() {
        return BrigadierCommand.literalArgumentBuilder("maintenance")
                .then(
                        BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.word())
                                .executes(context -> {
                                    boolean value = Boolean.parseBoolean(StringArgumentType.getString(context, "value"));
                                    smartRouter.getConfiguration().setMaintenance(value);
                                    SmartRouter.getLogger().info("Set the temporary maintenance mode to %s".formatted(value));
                                    return Command.SINGLE_SUCCESS;
                                })
                )
                .build();
    }
    private LiteralCommandNode<CommandSource> createMaintenanceMessageCommand() {
        return BrigadierCommand.literalArgumentBuilder("message")
                .then(
                        BrigadierCommand.requiredArgumentBuilder("msg", StringArgumentType.string())
                                .executes(context -> {
                                    String value = StringArgumentType.getString(context, "msg");
                                    smartRouter.getConfiguration().setMaintenanceMessage(value);
                                    SmartRouter.getLogger().info("Set the temporary maintenance message to to %s".formatted(value));
                                    return Command.SINGLE_SUCCESS;
                                })
                )
                .build();
    }

}
