package org.emeraldcraft.paperrouter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.emeraldcraft.paperrouter.serverapi.components.ChildServerConfig;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Configuration {
    private final FileConfiguration config;

    private boolean maintenance = true;
    private String maintenanceMessage = "Wrong Configuration. Contact Admin.";
    private final List<ChildServerConfig> configuredChildServerConfigs = new ArrayList<>();
    private ChildServerConfig selectedServer;
    private String pteroPanelURL;
    private String pteroAPIKey;
    private Ec2Client ec2Client;
    private final List<String> allowList = new ArrayList<>();

    public Configuration(FileConfiguration config) {
        this.config = config;
    }

    public void load() throws InvalidConfigurationException {
        maintenance = config.getBoolean("maintenance");
        maintenanceMessage = config.getString("maintenance-message");

        ConfigurationSection servers = config.getConfigurationSection("servers");
        if (servers == null) {

            PaperRouter.logger().severe("Illegal configuration, cannot continue");
            throw new InvalidConfigurationException("You do not have a servers section. Please use the default config");
        }

        Map<String, Object> values = servers.getValues(false);

        for (String serverName : values.keySet()) {
            ConfigurationSection configurationSection = servers.getConfigurationSection(serverName);
            assert  configurationSection != null;
            String displayName = configurationSection.getString("display_name");
            String pteroServerId = configurationSection.getString("ptero_server_id");
            boolean autoStart = configurationSection.getBoolean("auto_start");
            String awsInstanceId = config.getString("aws_instance_id");
            ChildServerConfig childServerConfig = new ChildServerConfig(serverName, Objects.requireNonNull(displayName), Objects.requireNonNull(pteroServerId), Objects.requireNonNull(awsInstanceId), autoStart);
            configuredChildServerConfigs.add(childServerConfig);
        }


//        root.node("servers").childrenMap().forEach((serverName, node) -> {
//            String displayName = node.node("display_name").getString();
//            String pteroServerId = node.node("ptero_server_id").getString();
//            boolean autoStart = node.node("auto_start").getBoolean();
//            String awsInstanceId = root.node("aws_instance_id").getString();
//            ChildServerConfig childServerConfig = new ChildServerConfig(serverName.toString(), Objects.requireNonNull(displayName), Objects.requireNonNull(pteroServerId), Objects.requireNonNull(awsInstanceId), autoStart);
//            configuredChildServerConfigs.add(childServerConfig);
//        });

        allowList.addAll(config.getStringList("allowlist"));

        //find the selected server
        String selectedServer = config.getString("selected_server");
        boolean found = false;
        for (ChildServerConfig childServerConfig : configuredChildServerConfigs) {
            if (childServerConfig.configName().equals(selectedServer)) {
                found = true;
                this.selectedServer = childServerConfig;
                break;
            }
        }
        if(!found) {
            PaperRouter.logger().severe("Unable to find selected server %s in configuration");
            throw new IllegalArgumentException("Selected server %s not found in configuration".formatted(selectedServer));
        }

        //grab the ptero panel url
        pteroPanelURL = config.getString("ptero_panel_url");
        pteroAPIKey = config.getString("ptero_api_key");

        this.ec2Client = Ec2Client.builder().credentialsProvider(InstanceProfileCredentialsProvider.builder().build()).region(Region.US_EAST_2).build();
        this.ec2Client.describeInstanceStatus();

        PaperRouter.logger().info("Successfully loaded configuration");

        PaperRouter.logger().info("""
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

    public Ec2Client getEc2Client() {
        return ec2Client;
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
