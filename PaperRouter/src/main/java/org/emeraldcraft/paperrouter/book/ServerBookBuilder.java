package org.emeraldcraft.paperrouter.book;

import net.kyori.adventure.text.Component;
import org.emeraldcraft.paperrouter.serverapi.manager.ChildServer;

public interface ServerBookBuilder {

    Component forServer(ChildServer server);

}
