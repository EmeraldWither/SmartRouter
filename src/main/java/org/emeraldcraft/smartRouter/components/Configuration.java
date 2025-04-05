package org.emeraldcraft.smartRouter.components;

import org.emeraldcraft.smartRouter.SmartRouter;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Configuration {
    private final Path path;

    private boolean maintenance = true;
    private String maintenanceMessage = "Wrong Configuration. Contact Admin.";
    private final List<ChildServer> configuredChildServers = new ArrayList<>();
    private ChildServer selectedServer;
    private String pteroPanelURL;
    private String pteroAPIKey;
    private Ec2Client ec2Client;
    private final List<String> allowList = new ArrayList<>();

    public Configuration(Path path) {
        this.path = path;
    }

    public void load() throws ConfigurateException {
        SmartRouter.getLogger().info("Configuration File Path: {}", path.resolve("config.yml").toAbsolutePath());
        createFileIfNotFound();
        final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(path.resolve("config.yml"))
                .build();
        loader.load();


        final ConfigurationNode root;
        root = loader.load();
        maintenance = root.node("maintenance").getBoolean();
        maintenanceMessage = root.node("maintenance-message").getString();
        root.node("servers").childrenMap().forEach((serverName, node) -> {
            String displayName = node.node("display_name").getString();
            String pteroServerId = node.node("ptero_server_id").getString();
            boolean autoStart = node.node("auto_start").getBoolean();
            String awsInstanceId = root.node("aws_instance_id").getString();
            ChildServer childServer = new ChildServer(serverName.toString(), Objects.requireNonNull(displayName), Objects.requireNonNull(pteroServerId), Objects.requireNonNull(awsInstanceId), autoStart);
            configuredChildServers.add(childServer);
        });

        root.node("allowlist").childrenList().forEach(node -> {
            String playerName = node.getString();
            if (playerName != null) {
                allowList.add(playerName);
            }
        });

        //find the selected server
        String selectedServer = root.node("selected_server").getString();
        boolean found = false;
        for (ChildServer childServer : configuredChildServers) {
            if (childServer.configName().equals(selectedServer)) {
                found = true;
                this.selectedServer = childServer;
                break;
            }
        }
        if(!found) {
            SmartRouter.getLogger().error("Unable to find selected server %s in configuration");
            throw new IllegalArgumentException("Selected server %s not found in configuration".formatted(selectedServer));
        }

        //grab the ptero panel url
        pteroPanelURL = root.node("ptero_panel_url").getString();
        pteroAPIKey = root.node("ptero_api_key").getString();

        this.ec2Client = Ec2Client.builder().credentialsProvider(InstanceProfileCredentialsProvider.builder().build()).region(Region.US_EAST_2).build();
        this.ec2Client.describeInstanceStatus();

        SmartRouter.getLogger().info("Successfully loaded configuration");

        SmartRouter.getLogger().info("""
                Configuration Information
                
                Maintenance: %s
                Maintenance Message: %s
                
                Selected Server: %s
                Configured Servers: %s
                Allowlist: %s
                """.formatted(
                        maintenance,
                        maintenanceMessage,
                        getSelectedServer().displayName(),
                        configuredChildServers.stream().map(ChildServer::displayName).toList(),
                        allowList.stream().map(String::toString).toList()
                )
        );
    }

    public ChildServer childServerFromName(String serverName) {
        for (ChildServer childServer : configuredChildServers) {
            if (childServer.configName().equals(serverName)) {
                return childServer;
            }
        }
        throw new IllegalArgumentException("Cannot find server with name %s".formatted(serverName));
    }

    private void createFileIfNotFound() {
        File file = path.resolve("config.yml").toFile();
        if (file.exists()) {
            return;
        }
        SmartRouter.getLogger().warn("Configuration file not found. Creating a new one.");
        //grab the file from our jar file
        InputStream is = SmartRouter.class.getResourceAsStream("/config.yml");
        try {
            if (!file.createNewFile()) {
                SmartRouter.getLogger().warn("A configuration file already exists!!!");
                return;
            }
            Files.copy(Objects.requireNonNull(is), Path.of(file.getAbsolutePath()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SmartRouter.getLogger().warn("Configuration file created. Please edit the file ASAP!!!");
    }

    public boolean isMaintenance() {
        return maintenance;
    }

    public String getMaintenanceMessage() {
        return maintenanceMessage;
    }

    public ChildServer getSelectedServer() {
        return selectedServer;
    }

    public List<ChildServer> getConfiguredChildServers() {
        return configuredChildServers;
    }

    public List<String> getAllowList() {
        return allowList;
    }

    public String getPteroPanelURL() {
        return pteroPanelURL;
    }

    public String getPteroAPIKey() {
        return pteroAPIKey;
    }

    public Ec2Client getEc2Client() {
        return ec2Client;
    }
}
