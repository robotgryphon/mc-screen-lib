package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.client.network.ClientPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkRegistration {
    private NetworkRegistration() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                UpdateCanvasStatePayload.TYPE,
                UpdateCanvasStatePayload.STREAM_CODEC,
                ServerPayloadHandler::handleUpdate
        );

        registrar.playToClient(
                CanvasEvaluationPayload.TYPE,
                CanvasEvaluationPayload.STREAM_CODEC,
                ClientPayloadHandler::handleEvaluation
        );

        registrar.playToServer(
                RequestAddNodePayload.TYPE,
                RequestAddNodePayload.STREAM_CODEC,
                ServerPayloadHandler::handleRequestAddNode
        );

        registrar.playToServer(
                AddNodeResultPayload.TYPE,
                AddNodeResultPayload.STREAM_CODEC,
                ServerPayloadHandler::handleAddNodeResult
        );
    }
}
