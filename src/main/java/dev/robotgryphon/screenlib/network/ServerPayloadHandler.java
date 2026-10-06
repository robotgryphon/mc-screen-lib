package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.menu.TestScreenMenu;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerPayloadHandler {
    private ServerPayloadHandler() {}

    public static void handleUpdate(UpdateCanvasStatePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof TestScreenMenu menu) {
            menu.updateState(payload.state());
        }
    }
}
