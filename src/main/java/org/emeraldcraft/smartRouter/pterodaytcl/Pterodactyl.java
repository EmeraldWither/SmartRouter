package org.emeraldcraft.smartRouter.pterodaytcl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.emeraldcraft.smartRouter.SmartRouter;
import org.emeraldcraft.smartRouter.components.ChildServerConfig;
import org.emeraldcraft.smartRouter.components.Configuration;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@Deprecated
public class Pterodactyl {
    private static final HashMap<ChildServerConfig, ScheduledTask> instanceStopTimers = new HashMap<>();
    private static ScheduledTask pteroStopTimer;
    public static void startServer(ChildServerConfig server, Configuration configuration) {
        if(getServerState(server, configuration).equals("stopping")) {
            SmartRouter.getLogger().warn("Server is stopping. Cannot start server.");
            return;
        }
        stopAllTimers();
        String instanceState = getInstanceState(server, configuration);
        if (instanceState.equalsIgnoreCase("stopped")) {
            Ec2Client ec2Client = configuration.getEc2Client();
            ec2Client.startInstances(StartInstancesRequest.builder().instanceIds(server.awsInstanceID()).build());
        }
    }
    public static void stopServer(ChildServerConfig server, Configuration configuration) {
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
            String powerAction = "{\n  \"signal\": \"stop\"\n}";
            byte[] out = powerAction.getBytes(StandardCharsets.UTF_8);
            OutputStream stream = http.getOutputStream();
            stream.write(out);
            http.getInputStream();
            http.disconnect();
        } catch (IOException var8) {
            var8.printStackTrace();
        }
        if (instanceStopTimers.get(server) != null) {
            instanceStopTimers.get(server).cancel();
        }
        ScheduledTask task = SmartRouter.getProxyServer().getScheduler().buildTask(SmartRouter.getInstance(), () -> {
            SmartRouter.getLogger().info("30 seconds has passed. Stopping instance %s for server %s.".formatted(server.awsInstanceID(), server.displayName()));
            Ec2Client ec2Client = configuration.getEc2Client();
            ec2Client.stopInstances(StopInstancesRequest.builder().instanceIds(server.awsInstanceID()).build());
        }).delay(30L, TimeUnit.SECONDS).schedule();
        instanceStopTimers.put(server, task);
    }

    public static boolean isServerOnline(ChildServerConfig server, Configuration configuration) {
        String serverInfo = getResponse(configuration.getPteroPanelURL(), server.pteroServerID(), configuration.getPteroAPIKey());
        if (serverInfo.contains("\"detail\": \"Could not establish a connection to the machine running this server. Please try again.\"")) {
            SmartRouter.getLogger().info("Pterodactyl cannot communicate with instance, so %s offline.".formatted(server.displayName()));
            return false;
        } else if (serverInfo.startsWith("<!DOCTYPE html>")) {
            SmartRouter.getLogger().info("Weird server info. CHECK API KEY!!!!!!!! " + serverInfo);
            return false;
        } else {
            JsonObject json = (new Gson()).fromJson(serverInfo, JsonObject.class);
            String state = json.getAsJsonObject("attributes").get("current_state").getAsString();
            if (state.equalsIgnoreCase("starting")) {
                SmartRouter.getLogger().info("Pterodactyl says %s is starting.".formatted(server.displayName()));
                return false;
            } else if (state.equalsIgnoreCase("running")) {
                SmartRouter.getLogger().info("Ptero says %s is running.".formatted(server.displayName()));
                return true;
            } else if (state.equalsIgnoreCase("stopping")) {
                SmartRouter.getLogger().info("Ptero says stopping %s.".formatted(server.displayName()));
                return false;
            }
            else if (state.equalsIgnoreCase("offline")) {
                SmartRouter.getLogger().info("Ptero says offline %s.".formatted(server.displayName()));
                return false;
            }
            else {
                SmartRouter.getLogger().warn("Server State Failed: " + serverInfo);
                return false;
            }
        }
    }

    public static String getServerState(ChildServerConfig server, Configuration configuration) {
        String currentStatus = getInstanceState(server, configuration);
        if (!currentStatus.equalsIgnoreCase("running")) {
            SmartRouter.getLogger().info("Current EC2 Instance State: " + currentStatus);
            return currentStatus;
        } else if (!Pterodactyl.isServerOnline(server, configuration)) {
            SmartRouter.getLogger().info("Ptero Says not Online");
            return "starting";
        } else {
            return "online";
        }
    }

    public static String getInstanceState(ChildServerConfig server, Configuration configuration) {
        Ec2Client ec2Client = configuration.getEc2Client();
        DescribeInstanceStatusResponse response = ec2Client.describeInstanceStatus(DescribeInstanceStatusRequest.builder().instanceIds(server.awsInstanceID()).includeAllInstances(true).build());
        return response.instanceStatuses().get(0).instanceState().nameAsString();
    }
    public static String getResponse(String panelURL, String serverID, String apiKey) {
        try {
            URL url = new URL(panelURL + "/api/client/servers/" + serverID + "/resources");
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestMethod("GET");
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/json");
            http.setRequestProperty("Authorization", "Bearer " + apiKey);
            http.setReadTimeout(1500);
            http.connect();
            String serverInfo = parseInputStream(http.getInputStream());
            http.disconnect();
            return serverInfo;
        } catch (SocketTimeoutException e) {
            return "\"detail\": \"Could not establish a connection to the machine running this server. Please try again.\"";
        }
        catch (IOException var6) {
            throw new RuntimeException(var6);
        }
    }
    private static String parseInputStream(InputStream stream) {
        String s = "";

        Scanner scanner;
        for(scanner = new Scanner(stream); scanner.hasNextLine(); s = s.concat(scanner.nextLine() + "\n"));

        scanner.close();
        return s;
    }

    public static void stopServerDelayed(Configuration configuration) {
        if(pteroStopTimer != null) pteroStopTimer.cancel();
        pteroStopTimer = null;
        SmartRouter.getLogger().info("Started the stop timers for instances. The servers will stop in 5 minutes.");
        pteroStopTimer = SmartRouter.getProxyServer().getScheduler().buildTask(SmartRouter.getInstance(), () -> {
            SmartRouter.getLogger().info("Stopping the child servers, then will stop instances.");
            for(ChildServerConfig server : configuration.getConfiguredChildServers()) {
                if(!server.autoStart()) continue;
                stopServer(server, configuration);
            }
        }).delay(5L, TimeUnit.MINUTES).schedule();
    }

    public static void stopAllTimers() {
        List<ChildServerConfig> removedServers = new ArrayList<>();
        for(ChildServerConfig server : instanceStopTimers.keySet()) {
            if(server == null) continue;
            if(instanceStopTimers.get(server) != null) {
                instanceStopTimers.get(server).cancel();
                removedServers.add(server);
            }
        }
        removedServers.forEach(instanceStopTimers::remove);
        if(pteroStopTimer != null) {
            pteroStopTimer.cancel();
        }
    }
}
