package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.menu.TestScreenMenu;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Logical-server side of the network layer. Handlers run on the main
 * thread (NeoForge's {@code PayloadRegistrar} wraps them by default),
 * so it's safe to touch level / menu / attachment state directly from
 * here without extra synchronization.
 */
public final class ServerPayloadHandler {

    private ServerPayloadHandler() {}

    /**
     * Apply a canvas state update from the client. The packet only makes
     * sense while the player has the test screen open; if a forged or
     * delayed packet arrives when the menu has already been closed or
     * was never open in the first place, we drop it silently rather
     * than write through to the level attachment via some out-of-band
     * path. The {@link TestScreenMenu#updateState} method handles the
     * write-through to the level attachment internally.
     */
    public static void handleUpdate(UpdateCanvasStatePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof TestScreenMenu menu) {
            menu.updateState(payload.state());
        }
    }
}
