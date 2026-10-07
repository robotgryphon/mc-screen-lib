package dev.robotgryphon.screenlib.client.network;

import dev.robotgryphon.screenlib.client.ui.CanvasScreen;
import dev.robotgryphon.screenlib.network.CanvasEvaluationPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void handleEvaluation(CanvasEvaluationPayload payload, IPayloadContext context) {
        if (Minecraft.getInstance().gui.screen() instanceof CanvasScreen<?> screen) {
            screen.applyEvaluation(payload);
        }
    }
}
