package org.emeraldcraft.paperRouter;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemBuilder {
    public static final byte COMPASS_KEY = 0x11;
    public static ItemStack createCompassItem() {
        NamespacedKey key = new NamespacedKey(JavaPlugin.getPlugin(PaperRouter.class), "compass");
        ItemStack itemStack = new ItemStack(Material.COMPASS, 1);
        itemStack.editMeta(itemMeta -> {
            itemMeta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, COMPASS_KEY);
        });
        return itemStack;
    }

    public static Book buildBook() {

        Component page = Component.empty()
        .append(
                Component.text("    Emerald").color(NamedTextColor.DARK_GREEN).decorate(TextDecoration.BOLD)
        ).append(
                Component.text("Craft").color(NamedTextColor.GREEN)
        )
        .appendNewline()
        .append(Component.text("==================="));

        return Book.book(Component.empty(), Component.empty(), page);
    }

    public static void createPlayerInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().addItem(createCompassItem());
    }
}
