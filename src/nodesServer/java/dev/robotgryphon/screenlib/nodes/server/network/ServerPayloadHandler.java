package dev.robotgryphon.screenlib.nodes.server.network;

import dev.robotgryphon.screenlib.nodes.common.menu.CanvasContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import dev.robotgryphon.screenlib.nodes.common.network.AddNodePayload;
import dev.robotgryphon.screenlib.nodes.common.network.CanvasEditPayload;
import dev.robotgryphon.screenlib.nodes.common.network.SubscribeClientDataPayload;

public final class ServerPayloadHandler {
    private ServerPayloadHandler() {}

    public static void handleCanvasEdit(CanvasEditPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof CanvasContainerMenu menu) {
            menu.backend().ifPresent(backend -> backend.applyEdit(payload));
        }
    }

    public static void handleSubscribeClientData(SubscribeClientDataPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof CanvasContainerMenu menu) {
            menu.backend().ifPresent(backend -> backend.subscribe(payload.subscriptions()));
        }
    }

    public static void handleAddNode(AddNodePayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof CanvasContainerMenu menu) {
            menu.backend().ifPresent(backend -> backend.addNode(payload.definition(), payload.position()));
        }
    }
}
