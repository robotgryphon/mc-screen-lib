package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.menu.CanvasMenu;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerPayloadHandler {
    private ServerPayloadHandler() {}

    public static void handleCanvasEdit(CanvasEditPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof CanvasMenu menu) {
            menu.applyEdit(payload);
        }
    }

    public static void handleSubscribeClientData(SubscribeClientDataPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof CanvasMenu menu) {
            menu.subscribe(payload.subscriptions());
        }
    }

    public static void handleAddNode(AddNodePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof CanvasMenu menu) {
            menu.addNode(player, payload.definition(), payload.position());
        }
    }
}
