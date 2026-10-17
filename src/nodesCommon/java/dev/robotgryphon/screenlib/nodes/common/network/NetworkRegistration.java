package dev.robotgryphon.screenlib.nodes.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkRegistration {
    public static final String VERSION = "1";

    private NetworkRegistration() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(CanvasSyncPayload.TYPE, CanvasSyncPayload.STREAM_CODEC);
        registrar.playToClient(CanvasEvaluationPayload.TYPE, CanvasEvaluationPayload.STREAM_CODEC);
    }
}
