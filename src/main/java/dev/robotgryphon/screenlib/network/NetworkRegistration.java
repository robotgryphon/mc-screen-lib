package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.client.network.ClientPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkRegistration {
    private NetworkRegistration() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(MoveNodePayload.TYPE, MoveNodePayload.STREAM_CODEC, ServerPayloadHandler::handleCanvasEdit);
        registrar.playToServer(SetNodePropertyPayload.TYPE, SetNodePropertyPayload.STREAM_CODEC, ServerPayloadHandler::handleCanvasEdit);
        registrar.playToServer(ConnectNodesPayload.TYPE, ConnectNodesPayload.STREAM_CODEC, ServerPayloadHandler::handleCanvasEdit);
        registrar.playToServer(DisconnectNodesPayload.TYPE, DisconnectNodesPayload.STREAM_CODEC, ServerPayloadHandler::handleCanvasEdit);
        registrar.playToServer(RemoveNodePayload.TYPE, RemoveNodePayload.STREAM_CODEC, ServerPayloadHandler::handleCanvasEdit);

        registrar.playToClient(CanvasSyncPayload.TYPE, CanvasSyncPayload.STREAM_CODEC, ClientPayloadHandler::handleSync);

        registrar.playToServer(
                SubscribeClientDataPayload.TYPE,
                SubscribeClientDataPayload.STREAM_CODEC,
                ServerPayloadHandler::handleSubscribeClientData
        );

        registrar.playToClient(
                CanvasEvaluationPayload.TYPE,
                CanvasEvaluationPayload.STREAM_CODEC,
                ClientPayloadHandler::handleEvaluation
        );

        registrar.playToServer(AddNodePayload.TYPE, AddNodePayload.STREAM_CODEC, ServerPayloadHandler::handleAddNode);
    }
}
