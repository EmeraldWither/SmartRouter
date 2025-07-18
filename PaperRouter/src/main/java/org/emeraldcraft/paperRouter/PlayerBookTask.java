package org.emeraldcraft.paperRouter;

import net.kyori.adventure.inventory.Book;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerBookTask implements Runnable {
    private static final char[] frames = {'⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'};
    private int frame = 9;

    private final Player player;

    public PlayerBookTask(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        Book book = ItemBuilder.buildBook();
        frame = (frame + 1) % frames.length;
        player.openBook(book);
    }

}
