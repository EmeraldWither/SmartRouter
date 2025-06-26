package org.emeraldcraft.smartRouter.manager;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum StartResponse {
    SUCCESS("You have started %s", NamedTextColor.GREEN),
    ALREADY_STOPPING("The server %s is stopping, so you cannot perform actions right now. Please wait a moment.", NamedTextColor.YELLOW),
    ALREADY_STARTING("The server %s is still starting up.", NamedTextColor.YELLOW),
    ERROR_ADMIN("ERROR. Something is not okay-dokie and should probably contact the admin (i think you should).", NamedTextColor.RED),
    LOGIN_ACCEPTED("Login accepted?", NamedTextColor.WHITE);


    private final String response;
    private final TextColor color;

    StartResponse(String response, TextColor color) {
        this.response = response;
        this.color = color;
    }

    public String getResponse() {
        return response;
    }

    public TextColor getColor() {
        return color;
    }
}
