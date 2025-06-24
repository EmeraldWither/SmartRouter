package org.emeraldcraft.serverdaemon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
public class StartServerController {

    private final Environment environment;
    @Autowired
    public StartServerController(Environment environment) {
        this.environment = environment;
    }


    @PostMapping("/start")
    public void startServer(@RequestParam(value = "id") String id) {
        Logger.getGlobal().info("Starting server " + id);


        String apiKey = environment.getProperty("ptero.apikey");
        String panelURL = environment.getProperty("ptero.panelurl");
        Logger.getGlobal().info("API KEY: " + apiKey);
        Logger.getGlobal().info("Panel URL: " + panelURL);

        Pterodactyl.startServer(id,  apiKey, panelURL);
    }
}
