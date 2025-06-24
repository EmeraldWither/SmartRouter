package org.emeraldcraft.smartRouter.manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.emeraldcraft.smartRouter.SmartRouter;
import org.emeraldcraft.smartRouter.components.ChildServerConfig;
import org.emeraldcraft.smartRouter.components.Configuration;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.emeraldcraft.smartRouter.pterodaytcl.Pterodactyl.getInstanceState;
import static org.emeraldcraft.smartRouter.pterodaytcl.Pterodactyl.getResponse;

public class ChildServer {
    private final ChildServerConfig childServerConfig;

    private ServerState serverState = ServerState.UNKNOWN;
    private ScheduledTask startTask;


    public ChildServer(ChildServerConfig childServerConfig) {
        this.childServerConfig = childServerConfig;
    }

    public ServerState fetchData() {
        Configuration configuration = SmartRouter.getInstance().getConfiguration();
        String currentAWSState = getInstanceState(childServerConfig, SmartRouter.getInstance().getConfiguration());
        if(currentAWSState.equals("stopped")) {
            serverState = ServerState.INSTANCE_STOPPED;
            return serverState;
        }
        else if(currentAWSState.equals("stopping")) {
            serverState = ServerState.INSTANCE_STOPPING;
            return serverState;
        }
        else if(currentAWSState.equals("running")) {

            String serverInfo = getResponse(configuration.getPteroPanelURL(), childServerConfig.pteroServerID(), configuration.getPteroAPIKey());
            if (serverInfo.contains("\"detail\": \"Could not establish a connection to the machine running this server. Please try again.\"")) {
                SmartRouter.getLogger().info("Pterodactyl cannot communicate with instance, so %s offline.".formatted(childServerConfig.displayName()));
                serverState = ServerState.SERVER_UNREACHABLE;
                return serverState;
            } else if (serverInfo.startsWith("<!DOCTYPE html>")) {
                SmartRouter.getLogger().info("Weird server info. CHECK API KEY!!!!!!!! " + serverInfo);
                serverState = ServerState.SERVER_UNREACHABLE;
                return serverState;
            } else {
                JsonObject json = (new Gson()).fromJson(serverInfo, JsonObject.class);
                String state = json.getAsJsonObject("attributes").get("current_state").getAsString();
                if (state.equalsIgnoreCase("starting")) {
                    SmartRouter.getLogger().info("Pterodactyl says %s is starting.".formatted(childServerConfig.displayName()));
                    serverState =  ServerState.SERVER_STARTING;
                    return serverState;
                } else if (state.equalsIgnoreCase("running")) {
                    SmartRouter.getLogger().info("Ptero says %s is running.".formatted(childServerConfig.displayName()));
                    serverState =  ServerState.SERVER_ONLINE;
                    return serverState;
                } else if (state.equalsIgnoreCase("stopping")) {
                    SmartRouter.getLogger().info("Ptero says stopping %s.".formatted(childServerConfig.displayName()));
                    serverState =  ServerState.SERVER_STOPPING;
                    return serverState;
                }
                else if (state.equalsIgnoreCase("offline")) {
                    SmartRouter.getLogger().info("Ptero says offline %s.".formatted(childServerConfig.displayName()));
                    serverState =  ServerState.SERVER_OFFLINE;
                    return serverState;
                }
                else {
                    SmartRouter.getLogger().warn("Server State Failed: " + serverInfo);
                    return ServerState.UNKNOWN;
                }
            }


        }
        else {
            SmartRouter.getLogger().error("Unable to determine instance state for %s (The instance state was %s)".formatted(childServerConfig, currentAWSState));
            serverState = ServerState.UNKNOWN;
            return ServerState.UNKNOWN;
        }



    }

    public ChildServerConfig getChildServerConfig() {
        return childServerConfig;
    }

    public ServerState getServerState() {
        return serverState;
    }


    public void start() {
        //first verify instance state
        fetchData();
        if(serverState == ServerState.UNKNOWN) {
            SmartRouter.getLogger().error("Tried to start server, but failed because we have an unknown state...");
            return;
        }
        if(serverState == ServerState.INSTANCE_STOPPED) {
            //start AWS
            startAWSInstance();
            runPteroStartTask();

        } else if (serverState == ServerState.SERVER_OFFLINE) {
            if(startTask == null) {
                runPteroStartTask();
            }
            //else we just wait
        }


    }

    private void runPteroStartTask() {
        startTask = SmartRouter.getProxyServer().getScheduler().buildTask(SmartRouter.class, () -> {
            //get the new server state
            fetchData();
            if(serverState == ServerState.SERVER_OFFLINE) {
                //trigger a Ptero start
                startPteroServer(childServerConfig, SmartRouter.getInstance().getConfiguration());

            }
            else if(serverState == ServerState.SERVER_ONLINE) {
                startTask.cancel();
                startTask = null;
            }
            else if(serverState == ServerState.SERVER_STOPPING || serverState == ServerState.SERVER_UNREACHABLE) {
                //wait until it goes offline again
            }
            else if(serverState == ServerState.SERVER_STARTING) {
                startTask.cancel();
                startTask = null;
            }


        }).repeat(Duration.ofSeconds(2)).schedule();
    }

    private void startAWSInstance() {
        Ec2Client ec2Client = SmartRouter.getInstance().getConfiguration().getEc2Client();
        ec2Client.startInstances(StartInstancesRequest.builder().instanceIds(childServerConfig.awsInstanceID()).build());
    }

    public static void startPteroServer(ChildServerConfig server, Configuration configuration) {
        try {
            String panelURL = configuration.getPteroPanelURL();
            String serverID = server.pteroServerID();

            URL url;
            try {
                url = new URL(panelURL + "/api/client/servers/" + serverID + "/power");
            } catch (MalformedURLException var7) {
                var7.printStackTrace();
                return;
            }

            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestMethod("POST");
            http.setRequestProperty("Content-Type", "application/json");
            http.setRequestProperty("Accept", "application/json");
            http.setRequestProperty("Authorization", "Bearer " + configuration.getPteroAPIKey());
            http.setDoOutput(true);
            http.setConnectTimeout(5000);
            http.connect();
            String powerAction = "{\n  \"signal\": \"start\"\n}";
            byte[] out = powerAction.getBytes(StandardCharsets.UTF_8);
            OutputStream stream = http.getOutputStream();
            stream.write(out);
            http.getInputStream();
            http.disconnect();
        } catch (IOException var8) {
            var8.printStackTrace();
        }
    }

}

