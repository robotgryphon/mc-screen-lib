package dev.robotgryphon.screenlib.nodes.client.network;

import dev.robotgryphon.screenlib.nodes.client.CanvasSession;
import dev.robotgryphon.screenlib.ui.common.HostedScreen;
import dev.robotgryphon.screenlib.nodes.client.widget.canvas.CanvasPanel;
import dev.robotgryphon.screenlib.nodes.common.network.CanvasEvaluationPayload;
import dev.robotgryphon.screenlib.nodes.common.network.CanvasSyncPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    private static List<CanvasSession> sessions() {
        if (Minecraft.getInstance().gui.screen() instanceof HostedScreen screen) {
            return screen.host().panels(CanvasPanel.class).stream()
                    .map(CanvasPanel::session)
                    .distinct()
                    .toList();
        }
        return List.of();
    }

    public static void handleEvaluation(CanvasEvaluationPayload payload, IPayloadContext context) {
        sessions().forEach(session -> session.applyEvaluation(payload));
    }

    public static void handleSync(CanvasSyncPayload payload, IPayloadContext context) {
        sessions().forEach(session -> session.applySync(payload.state()));
    }
}
