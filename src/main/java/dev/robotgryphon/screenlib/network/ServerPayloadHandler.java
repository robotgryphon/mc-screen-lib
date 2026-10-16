package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import dev.robotgryphon.screenlib.graph.sync.AllowedValuesSnapshot;
import dev.robotgryphon.screenlib.menu.AddNodeData;
import dev.robotgryphon.screenlib.menu.AddNodeMenu;
import dev.robotgryphon.screenlib.menu.AddNodeMenuProvider;
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

    public static void handleRequestAddNode(RequestAddNodePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof CanvasMenu menu)
                || menu.source() == null) {
            return;
        }
        var registries = player.registryAccess();
        var available = registries.lookupOrThrow(NodeDefinition.REGISTRY_KEY).listElements()
                .filter(definition -> !definition.value().hidden())
                .map(definition -> definition.key())
                .toList();
        player.openMenu(new AddNodeMenuProvider(menu, new AddNodeData(available, payload.x(), payload.y(),
                AllowedValuesSnapshot.capture(registries))));
    }

    public static void handleAddNodeResult(AddNodeResultPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof AddNodeMenu menu) {
            payload.selected().ifPresentOrElse(type -> menu.submit(player, type), () -> menu.cancel(player));
        }
    }
}
