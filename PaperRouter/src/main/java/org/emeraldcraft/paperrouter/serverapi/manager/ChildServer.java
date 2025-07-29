package org.emeraldcraft.paperrouter.serverapi.manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.emeraldcraft.paperrouter.Configuration;
import org.emeraldcraft.paperrouter.PaperRouter;
import org.emeraldcraft.paperrouter.serverapi.components.ChildServerConfig;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.emeraldcraft.paperrouter.serverapi.pterodaytcl.Pterodactyl.getInstanceState;
import static org.emeraldcraft.paperrouter.serverapi.pterodaytcl.Pterodactyl.getResponse;


public class ChildServer {
    private final ChildServerConfig childServerConfig;

    private ServerState serverState = ServerState.UNKNOWN;
    private BukkitTask startTask;
    private BukkitTask pteroStopTask;
    private BukkitTask instanceStopTask;
    private long lastFetchTime;
    private boolean starting;


    public ChildServer(ChildServerConfig childServerConfig) {
        this.childServerConfig = childServerConfig;
    }

    public void fetchWithDelay(long seconds) {
        if(System.currentTimeMillis() - lastFetchTime > 1000 * seconds) fetchNow();
    }
    public void fetchNow() {
        lastFetchTime = System.currentTimeMillis();
        Configuration configuration = PaperRouter.getConfiguration();
        String currentAWSState = getInstanceState(childServerConfig, PaperRouter.getConfiguration());
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
                PaperRouter.logger().info("Pterodactyl cannot communicate with instance, so %s offline.".formatted(childServerConfig.displayName()));
                serverState = ServerState.SERVER_UNREACHABLE;
            } else if (serverInfo.startsWith("<!DOCTYPE html>")) {
                serverState = ServerState.SERVER_UNREACHABLE;
            } else {
                JsonObject json = (new Gson()).fromJson(serverInfo, JsonObject.class);
                String state = json.getAsJsonObject("attributes").get("current_state").getAsString();
                if (state.equalsIgnoreCase("starting")) {
                    PaperRouter.logger().info("Pterodactyl says %s is starting.".formatted(childServerConfig.displayName()));
                    serverState =  ServerState.SERVER_STARTING;
                } else if (state.equalsIgnoreCase("running")) {
                    PaperRouter.logger().info("Ptero says %s is running.".formatted(childServerConfig.displayName()));
                    starting = false;
                    serverState =  ServerState.SERVER_ONLINE;
                } else if (state.equalsIgnoreCase("stopping")) {
                    PaperRouter.logger().info("Ptero says stopping %s.".formatted(childServerConfig.displayName()));
                    serverState =  ServerState.SERVER_STOPPING;
                }
                else if (state.equalsIgnoreCase("offline")) {
                    PaperRouter.logger().info("Ptero says offline %s.".formatted(childServerConfig.displayName()));
                    serverState =  ServerState.SERVER_OFFLINE;
                }
                else {
                    PaperRouter.logger().warning("Server State Failed: " + serverInfo);
                }
            }


        }
        else {
            PaperRouter.logger().severe("Unable to determine instance state for %s (The instance state was %s)".formatted(childServerConfig, currentAWSState));
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
        fetchNow();
        System.out.println("Current State: " + serverState);
        if(serverState == ServerState.UNKNOWN) {
            PaperRouter.logger().severe("Tried to start server, but failed because we have an unknown state...");
            return StartResponse.ERROR_ADMIN;
        }
        if(serverState == ServerState.INSTANCE_STOPPED) {
            //start AWS
            starting = true;
            startAWSInstance();
            runPteroStartTask();
            return StartResponse.SUCCESS;

        } else if (serverState == ServerState.SERVER_OFFLINE) {
            if(startTask == null) {
                starting = true;
                System.out.println("Running ptero start task");
                runPteroStartTask();
            }
            return StartResponse.ALREADY_STARTING;
            //else we just wait
        }
        if(serverState == ServerState.SERVER_STARTING ||  serverState == ServerState.SERVER_UNREACHABLE) {
            return StartResponse.ALREADY_STARTING;
        }
        if(serverState == ServerState.SERVER_STOPPING) {
            starting = false;
            return StartResponse.ALREADY_STOPPING;
        }
        if(serverState == ServerState.SERVER_ONLINE) {
            starting = false;
            return StartResponse.LOGIN_ACCEPTED;
        }

        PaperRouter.logger().severe("Went through all server states and no fall-bak condition, considering as error...");
        return StartResponse.ERROR_ADMIN;
    }

    public void delayedShutdown() {
        pteroStopTask = Bukkit.getScheduler().runTaskLater(JavaPlugin.getProvidingPlugin(PaperRouter.class), this::runPteroStopTask, 20 * 60 * 5);
    }
    public void shutdownNow() {
        fetchNow();
        if(serverState == ServerState.SERVER_ONLINE) {
            sendPteroPowerCommand(childServerConfig, PaperRouter.getConfiguration(), "stop");
            runPteroStopTask();
        }
        if(serverState == ServerState.SERVER_OFFLINE) {
            stopAWSInstance();
        }
    }


    private void runPteroStartTask() {
        startTask = Bukkit.getScheduler().runTaskTimer(JavaPlugin.getProvidingPlugin(PaperRouter.class), () -> {
            //get the new server state
            fetchNow();
            if(serverState == ServerState.SERVER_OFFLINE) {
                //trigger a Ptero start
                sendPteroPowerCommand(childServerConfig, PaperRouter.getConfiguration(), "start");

            }
            else if(serverState == ServerState.SERVER_ONLINE) {
                starting = false;
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


        }, 0, 20 * 2);
    }
    private void runPteroStopTask() {
        sendPteroPowerCommand(childServerConfig, PaperRouter.getConfiguration(), "stop");
        PaperRouter.logger().info("Sent Pterodactyl stop command to '%s', waiting 20s before sending instance shutdown...".formatted(childServerConfig.displayName()));
        instanceStopTask = Bukkit.getScheduler().runTaskLater(JavaPlugin.getProvidingPlugin(PaperRouter.class), this::stopAWSInstance, 20 * 20);
    }

    private void startAWSInstance() {
        Ec2Client ec2Client = PaperRouter.getConfiguration().getEc2Client();
        ec2Client.startInstances(StartInstancesRequest.builder().instanceIds(childServerConfig.awsInstanceID()).build());
    }
    private void stopAWSInstance() {
        PaperRouter.logger().info("Shutting down instance %s.".formatted(childServerConfig.awsInstanceID()));
        Ec2Client ec2Client = PaperRouter.getConfiguration().getEc2Client();
        ec2Client.stopInstances(StopInstancesRequest.builder().instanceIds(childServerConfig.awsInstanceID()).build());
    }

    public void cancelStopTimer() {
        if(pteroStopTask != null) pteroStopTask.cancel();
        if(instanceStopTask != null) instanceStopTask.cancel();
        pteroStopTask = null;
        instanceStopTask = null;
        PaperRouter.logger().info("Stopped the stop timer for %s".formatted(childServerConfig.displayName()));
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

    public boolean isStarting() {
        return starting;
    }
}

