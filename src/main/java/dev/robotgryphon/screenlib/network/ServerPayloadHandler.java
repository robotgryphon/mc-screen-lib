package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import dev.robotgryphon.screenlib.graph.schema.AllowedValuesSnapshot;
import dev.robotgryphon.screenlib.menu.AddNodeData;
import dev.robotgryphon.screenlib.menu.AddNodeMenu;
import dev.robotgryphon.screenlib.menu.AddNodeMenuProvider;
import dev.robotgryphon.screenlib.menu.CanvasMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerPayloadHandler {
    private ServerPayloadHandler() {}

    public static void handleUpdate(UpdateCanvasStatePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof CanvasMenu menu) {
            menu.updateState(payload.state(), payload.sequence());
        }
    }

    public static void handleRequestAddNode(RequestAddNodePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof CanvasMenu menu)
                || menu.source() == null) {
            return;
        }
        var source = menu.source();
        var registries = source.level().registryAccess();
        var available = registries.lookupOrThrow(NodeDefinition.REGISTRY_KEY).listElements()
                .filter(definition -> !definition.value().hidden())
                .map(definition -> definition.key())
                .toList();
        player.openMenu(new AddNodeMenuProvider(source, new AddNodeData(available, payload.x(), payload.y(),
                AllowedValuesSnapshot.capture(registries))));
    }

    public static void handleAddNodeResult(AddNodeResultPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof AddNodeMenu menu) {
            payload.selected().ifPresentOrElse(type -> menu.submit(player, type), () -> menu.cancel(player));
        }
    }
}
