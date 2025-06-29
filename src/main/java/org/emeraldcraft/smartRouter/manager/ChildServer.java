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
import java.sql.SQLOutput;
import java.time.Duration;

import static org.emeraldcraft.smartRouter.pterodaytcl.Pterodactyl.getInstanceState;
import static org.emeraldcraft.smartRouter.pterodaytcl.Pterodactyl.getResponse;

public class ChildServer {
    private final ChildServerConfig childServerConfig;

    private ServerState serverState = ServerState.UNKNOWN;
    private ScheduledTask startTask;
    private ScheduledTask pteroStopTask;
    private ScheduledTask instanceStopTask;


    public ChildServer(ChildServerConfig childServerConfig) {
        this.childServerConfig = childServerConfig;
    }

    public void fetchData() {
        Configuration configuration = SmartRouter.getInstance().getConfiguration();
        String currentAWSState = getInstanceState(childServerConfig, SmartRouter.getInstance().getConfiguration());
        if(currentAWSState.equals("stopped")) {
            serverState = ServerState.INSTANCE_STOPPED;
        }
        else if(currentAWSState.equals("pending")) {
            serverState = ServerState.SERVER_UNREACHABLE;
        }
        else if(currentAWSState.equals("stopping")) {
            serverState = ServerState.INSTANCE_STOPPING;
        }
        else if(currentAWSState.equals("running")) {

            String serverInfo = getResponse(configuration.getPteroPanelURL(), childServerConfig.pteroServerID(), configuration.getPteroAPIKey());
            if (serverInfo.contains("\"detail\": \"Could not establish a connection to the machine running this server. Please try again.\"")) {
                SmartRouter.getLogger().info("Pterodactyl cannot communicate with instance, so %s offline.".formatted(childServerConfig.displayName()));
                serverState = ServerState.SERVER_UNREACHABLE;
            } else if (serverInfo.startsWith("<!DOCTYPE html>")) {
                serverState = ServerState.SERVER_UNREACHABLE;
            } else {
                JsonObject json = (new Gson()).fromJson(serverInfo, JsonObject.class);
                String state = json.getAsJsonObject("attributes").get("current_state").getAsString();
                if (state.equalsIgnoreCase("starting")) {
                    SmartRouter.getLogger().info("Pterodactyl says %s is starting.".formatted(childServerConfig.displayName()));
                    serverState =  ServerState.SERVER_STARTING;
                } else if (state.equalsIgnoreCase("running")) {
                    SmartRouter.getLogger().info("Ptero says %s is running.".formatted(childServerConfig.displayName()));
                    serverState =  ServerState.SERVER_ONLINE;
                } else if (state.equalsIgnoreCase("stopping")) {
                    SmartRouter.getLogger().info("Ptero says stopping %s.".formatted(childServerConfig.displayName()));
                    serverState =  ServerState.SERVER_STOPPING;
                }
                else if (state.equalsIgnoreCase("offline")) {
                    SmartRouter.getLogger().info("Ptero says offline %s.".formatted(childServerConfig.displayName()));
                    serverState =  ServerState.SERVER_OFFLINE;
                }
                else {
                    SmartRouter.getLogger().warn("Server State Failed: " + serverInfo);
                }
            }


        }
        else {
            SmartRouter.getLogger().error("Unable to determine instance state for %s (The instance state was %s)".formatted(childServerConfig, currentAWSState));
            serverState = ServerState.UNKNOWN;
        }



    }

    public ChildServerConfig getChildServerConfig() {
        return childServerConfig;
    }

    public ServerState getServerState() {
        return serverState;
    }



    public StartResponse start() {
        //first verify instance state
        if(instanceStopTask != null) {
            instanceStopTask.cancel();
            instanceStopTask = null;
        }
        if(pteroStopTask != null) {
            pteroStopTask.cancel();
            pteroStopTask = null;
        }
        fetchData();
        System.out.println("Current State: " + serverState);
        if(serverState == ServerState.UNKNOWN) {
            SmartRouter.getLogger().error("Tried to start server, but failed because we have an unknown state...");
            return StartResponse.ERROR_ADMIN;
        }
        if(serverState == ServerState.INSTANCE_STOPPED) {
            //start AWS
            startAWSInstance();
            runPteroStartTask();
            return StartResponse.SUCCESS;

        } else if (serverState == ServerState.SERVER_OFFLINE) {
            if(startTask == null) {
                runPteroStartTask();
            }
            return StartResponse.ALREADY_STARTING;
            //else we just wait
        }
        if(serverState == ServerState.SERVER_STARTING ||  serverState == ServerState.SERVER_UNREACHABLE) {
            return StartResponse.ALREADY_STARTING;
        }
        if(serverState == ServerState.SERVER_STOPPING) {
            return StartResponse.ALREADY_STOPPING;
        }
        if(serverState == ServerState.SERVER_ONLINE) {
            return StartResponse.LOGIN_ACCEPTED;
        }

        SmartRouter.getLogger().error("Went through all server states and no fall-bak condition, considering as error...");
        return StartResponse.ERROR_ADMIN;
    }

    public void delayedShutdown() {
        pteroStopTask = SmartRouter.getProxyServer().getScheduler().buildTask(SmartRouter.getInstance(), this::runPteroStopTask).delay(Duration.ofMinutes(5)).schedule();
    }
    public void shutdownNow() {
        fetchData();
        if(serverState == ServerState.SERVER_ONLINE) {
            sendPteroPowerCommand(childServerConfig, SmartRouter.getInstance().getConfiguration(), "stop");
            runPteroStopTask();
        }
        if(serverState == ServerState.SERVER_OFFLINE) {
            stopAWSInstance();
        }
    }


    private void runPteroStartTask() {
        startTask = SmartRouter.getProxyServer().getScheduler().buildTask(SmartRouter.getInstance(), () -> {
            //get the new server state
            fetchData();
            if(serverState == ServerState.SERVER_OFFLINE) {
                //trigger a Ptero start
                sendPteroPowerCommand(childServerConfig, SmartRouter.getInstance().getConfiguration(), "start");

            }
            else if(serverState == ServerState.SERVER_ONLINE) {
                startTask.cancel();
                startTask = null;
            }
            else if(serverState == ServerState.SERVER_STOPPING || serverState == ServerState.SERVER_UNREACHABLE) {
                //wait until it goes offline again
            }
            else if(serverState == ServerState.SERVER_STARTING) {
                if(startTask != null) {
                    startTask.cancel();
                    startTask = null;
                }
            }


        }).repeat(Duration.ofSeconds(2)).schedule();
    }
    private void runPteroStopTask() {
        sendPteroPowerCommand(childServerConfig, SmartRouter.getInstance().getConfiguration(), "stop");
        SmartRouter.getLogger().info("Sent Pterodactyl stop command to '%s', waiting 20s before sending instance shutdown...".formatted(childServerConfig.displayName()));
        instanceStopTask = SmartRouter.getProxyServer().getScheduler().buildTask(SmartRouter.getInstance(), this::stopAWSInstance).delay(Duration.ofSeconds(20)).schedule();
    }

    private void startAWSInstance() {
        Ec2Client ec2Client = SmartRouter.getInstance().getConfiguration().getEc2Client();
        ec2Client.startInstances(StartInstancesRequest.builder().instanceIds(childServerConfig.awsInstanceID()).build());
    }
    private void stopAWSInstance() {
        SmartRouter.getLogger().info("Shutting down instance %s.".formatted(childServerConfig.awsInstanceID()));
        Ec2Client ec2Client = SmartRouter.getInstance().getConfiguration().getEc2Client();
        ec2Client.stopInstances(StopInstancesRequest.builder().instanceIds(childServerConfig.awsInstanceID()).build());
    }

    public void cancelStopTimer() {
        if(pteroStopTask != null) pteroStopTask.cancel();
        if(instanceStopTask != null) instanceStopTask.cancel();
        pteroStopTask = null;
        instanceStopTask = null;
        SmartRouter.getLogger().info("Stopped the stop timer for %s".formatted(childServerConfig.displayName()));
    }

    public static void sendPteroPowerCommand(ChildServerConfig server, Configuration configuration, String powerCommand) {
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
            String powerAction = "{\n  \"signal\": \"%s\"\n}".formatted(powerCommand);
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

