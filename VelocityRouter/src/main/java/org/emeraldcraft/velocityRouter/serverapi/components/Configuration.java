package org.emeraldcraft.velocityRouter.serverapi.components;

import org.emeraldcraft.velocityRouter.VelocityRouter;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Configuration {
    private final Path path;

    private boolean maintenance = true;
    private String maintenanceMessage = "Wrong Configuration. Contact Admin.";
    private final List<ChildServerConfig> configuredChildServerConfigs = new ArrayList<>();
    private ChildServerConfig selectedServer;
    private String pteroPanelURL;
    private String pteroAPIKey;
    private final List<String> allowList = new ArrayList<>();

    public Configuration(Path path) {
        this.path = path;
    }

    public void load() throws ConfigurateException {
        VelocityRouter.getLogger().info("Configuration File Path: {}", path.resolve("config.yml").toAbsolutePath());
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
            ChildServerConfig childServerConfig = new ChildServerConfig(serverName.toString(), Objects.requireNonNull(displayName), Objects.requireNonNull(pteroServerId), autoStart);
            configuredChildServerConfigs.add(childServerConfig);
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
        for (ChildServerConfig childServerConfig : configuredChildServerConfigs) {
            if (childServerConfig.configName().equals(selectedServer)) {
                found = true;
                this.selectedServer = childServerConfig;
                break;
            }
        }
        if(!found) {
            VelocityRouter.getLogger().error("Unable to find selected server %s in configuration");
            throw new IllegalArgumentException("Selected server %s not found in configuration".formatted(selectedServer));
        }

        //grab the ptero panel url
        pteroPanelURL = root.node("ptero_panel_url").getString();
        pteroAPIKey = root.node("ptero_api_key").getString();

        VelocityRouter.getLogger().info("Successfully loaded configuration");

        VelocityRouter.getLogger().info("""
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
                        configuredChildServerConfigs.stream().map(ChildServerConfig::displayName).toList(),
                        allowList.stream().map(String::toString).toList()
                )
        );
    }

    public ChildServerConfig childServerFromName(String serverName) {
        for (ChildServerConfig childServerConfig : configuredChildServerConfigs) {
            if (childServerConfig.configName().equals(serverName)) {
                return childServerConfig;
            }
        }
        throw new IllegalArgumentException("Cannot find server with name %s".formatted(serverName));
    }

    private void createFileIfNotFound() {
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }
        File file = path.resolve("config.yml").toFile();
        if (file.exists()) {
            return;
        }
        VelocityRouter.getLogger().warn("Configuration file not found. Creating a new one.");
        //grab the file from our jar file
        InputStream is = VelocityRouter.class.getResourceAsStream("/config.yml");
        try {
            if (!file.createNewFile()) {
                VelocityRouter.getLogger().warn("A configuration file already exists!!!");
                return;
            }
            Files.copy(Objects.requireNonNull(is), Path.of(file.getAbsolutePath()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        VelocityRouter.getLogger().warn("Configuration file created. Please edit the file ASAP!!!");
    }

    public boolean isMaintenance() {
        return maintenance;
    }

    public String getMaintenanceMessage() {
        return maintenanceMessage;
    }

    public ChildServerConfig getSelectedServer() {
        return selectedServer;
    }

    public List<ChildServerConfig> getConfiguredChildServers() {
        return configuredChildServerConfigs;
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


    public void setMaintenance(boolean value) {
        this.maintenance = value;
    }

    public void setMaintenanceMessage(String value) {
        this.maintenanceMessage = value;
    }

    public void setServer(ChildServerConfig childServerConfig) {
        selectedServer = childServerConfig;
    }
}
