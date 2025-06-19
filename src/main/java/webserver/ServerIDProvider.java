package webserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.emeraldcraft.smartRouter.SmartRouter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class ServerIDProvider {
    public void init() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(25564), 0);
        server.createContext("/serverid", new PteroServerHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }
    static class PteroServerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = SmartRouter.getInstance().getConfiguration().getSelectedServer().pteroServerID();
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
