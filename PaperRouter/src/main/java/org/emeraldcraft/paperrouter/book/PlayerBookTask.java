package org.emeraldcraft.paperrouter.book;

import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.emeraldcraft.paperrouter.PaperRouter;
import org.emeraldcraft.paperrouter.serverapi.manager.ChildServer;
import org.emeraldcraft.paperrouter.serverapi.manager.SimpleServerState;

import java.util.Optional;
import java.util.UUID;

public class PlayerBookTask implements Runnable {
    private static final char[] frames = {'⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'};
    private int frame = 9;

    private final Player player;

    public PlayerBookTask(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        Book book = ItemBuilder.buildBook(new BookBuilder());
        frame = (frame + 1) % frames.length;
        player.openBook(book);
    }
    public static class BookBuilder implements ServerBookBuilder {
        public static final String[] LOADING_ANIM = {"⌛", "⏳"};
        @Override
        public Component forServer(ChildServer server) {
            Component text = Component.text(server.getChildServerConfig().displayName());
            String serverStateText = "Unknown";
            server.fetchWithDelay(2);
            SimpleServerState state = SimpleServerState.fromServerState(server.getServerState());

            if((state == SimpleServerState.OFFLINE && !server.isStarting()) || (state == SimpleServerState.STARTING && !server.isStarting())) {
                text = Component.text("• ").color(NamedTextColor.RED).append(text);
                serverStateText = "Offline";
            }
            else if((state == SimpleServerState.STARTING && server.isStarting()) || (state == SimpleServerState.OFFLINE && server.isStarting())){
                text = Component.text(LOADING_ANIM[(int) (Math.random() * 2)] + " ").color(NamedTextColor.GOLD).append(text.decorate(TextDecoration.ITALIC));
                serverStateText = "Starting";
            }
            else if(state == SimpleServerState.ONLINE) {
                text = Component.text("✔ ").color(NamedTextColor.DARK_GREEN).append(text.decorate(TextDecoration.BOLD));
                serverStateText = "Online";
            }
            else if(state == SimpleServerState.STOPPING) {
                text = Component.text("\uD83D\uDED1").color(NamedTextColor.LIGHT_PURPLE).append(text);
                serverStateText = "Stopping";
            }
            else if (state == SimpleServerState.UNKNOWN) {
                text = Component.text("(?)").color(NamedTextColor.DARK_GRAY).append(text);
                serverStateText = "Cannot fetch server state.";
            }
            text = text.hoverEvent(HoverEvent.showText(Component.text(serverStateText))).clickEvent(ClickEvent.callback(audience -> {
                Optional<UUID> uuid = audience.get(Identity.UUID);
                if(uuid.isEmpty()) return;
                Player bukkPlayer = Bukkit.getPlayer(uuid.get());
                if(bukkPlayer == null) return;
                PaperRouter.getServerManager().attemptPlayerConnectAndStart(bukkPlayer, server);
            }));
            return text;
        }
    }
}


