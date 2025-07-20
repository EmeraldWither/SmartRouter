package org.emeraldcraft.velocityRouter;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;

import java.nio.charset.Charset;

public class PluginChannelListener {

    private ProxyServer server;
    public PluginChannelListener(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        System.out.println(event.getIdentifier());
        System.out.println(new String(event.getData(), Charset.defaultCharset()));
        if(event.getIdentifier().getId().equals("router")) {
            String name = new String(event.getData(), Charset.defaultCharset());

        }
    }

}
