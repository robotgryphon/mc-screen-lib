package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import dev.robotgryphon.screenlib.graph.schema.AllowedValuesSnapshot;
import dev.robotgryphon.screenlib.menu.AddNodeData;
import dev.robotgryphon.screenlib.menu.AddNodeMenu;
import dev.robotgryphon.screenlib.menu.AddNodeMenuProvider;
import dev.robotgryphon.screenlib.menu.TestScreenMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerPayloadHandler {
    private ServerPayloadHandler() {}

    public static void handleUpdate(UpdateCanvasStatePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof TestScreenMenu menu) {
            menu.updateState(payload.state(), payload.sequence());
        }
    }

    public static void handleRequestAddNode(RequestAddNodePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof TestScreenMenu) {
            var level = player.level();
            var available = level.registryAccess()
                    .lookupOrThrow(NodeDefinition.REGISTRY_KEY)
                    .listElementIds()
                    .toList();
            player.openMenu(new AddNodeMenuProvider(level, new AddNodeData(available, payload.x(), payload.y(),
                    AllowedValuesSnapshot.capture(level.registryAccess()))));
        }
    }

    public static void handleAddNodeResult(AddNodeResultPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof AddNodeMenu menu) {
            payload.selected().ifPresentOrElse(type -> menu.submit(player, type), () -> menu.cancel(player));
        }
    }
}
