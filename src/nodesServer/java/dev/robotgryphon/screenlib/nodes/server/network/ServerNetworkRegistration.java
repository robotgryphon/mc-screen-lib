package dev.robotgryphon.screenlib.nodes.server.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import dev.robotgryphon.screenlib.nodes.common.network.SetNodePropertyPayload;
import dev.robotgryphon.screenlib.nodes.common.network.ConnectNodesPayload;
import dev.robotgryphon.screenlib.nodes.common.network.AddNodePayload;
import dev.robotgryphon.screenlib.nodes.common.network.AddProvidedNodePayload;
import dev.robotgryphon.screenlib.nodes.common.network.MoveNodePayload;
import dev.robotgryphon.screenlib.nodes.common.network.NetworkRegistration;
import dev.robotgryphon.screenlib.nodes.common.network.SubscribeClientDataPayload;
import dev.robotgryphon.screenlib.nodes.common.network.RemoveNodePayload;
import dev.robotgryphon.screenlib.nodes.common.network.DisconnectNodesPayload;

public final class ServerNetworkRegistration {
    private ServerNetworkRegistration() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NetworkRegistration.VERSION);
        registrar.playToServer(MoveNodePayload.TYPE, MoveNodePayload.STREAM_CODEC, ServerPayloadHandler::handleCanvasEdit);
        registrar.playToServer(SetNodePropertyPayload.TYPE, SetNodePropertyPayload.STREAM_CODEC, ServerPayloadHandler::handleCanvasEdit);
        registrar.playToServer(ConnectNodesPayload.TYPE, ConnectNodesPayload.STREAM_CODEC, ServerPayloadHandler::handleCanvasEdit);
        registrar.playToServer(DisconnectNodesPayload.TYPE, DisconnectNodesPayload.STREAM_CODEC, ServerPayloadHandler::handleCanvasEdit);
        registrar.playToServer(RemoveNodePayload.TYPE, RemoveNodePayload.STREAM_CODEC, ServerPayloadHandler::handleCanvasEdit);
        registrar.playToServer(SubscribeClientDataPayload.TYPE, SubscribeClientDataPayload.STREAM_CODEC,
                ServerPayloadHandler::handleSubscribeClientData);
        registrar.playToServer(AddNodePayload.TYPE, AddNodePayload.STREAM_CODEC, ServerPayloadHandler::handleAddNode);
        registrar.playToServer(AddProvidedNodePayload.TYPE, AddProvidedNodePayload.STREAM_CODEC, ServerPayloadHandler::handleAddProvidedNode);
    }
}
