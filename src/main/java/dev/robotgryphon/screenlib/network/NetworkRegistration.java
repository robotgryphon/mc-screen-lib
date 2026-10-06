package dev.robotgryphon.screenlib.network;

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
    }
}
