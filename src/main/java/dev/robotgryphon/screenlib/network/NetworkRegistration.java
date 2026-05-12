package dev.robotgryphon.screenlib.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Single home for {@link RegisterPayloadHandlersEvent} wiring. The mod
 * constructor adds {@link #register} as a mod-bus listener; everything
 * payload-related is added here so growing the network surface doesn't
 * involve touching the main mod class.
 */
public final class NetworkRegistration {

    private NetworkRegistration() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        // The string is a *protocol version*, not a namespace — bump it if
        // the wire format of any payload here changes incompatibly. Mod id
        // namespacing is carried on each payload's Type id instead.
        PayloadRegistrar registrar = event.registrar("1");

        // The "open test screen" direction is handled by vanilla's menu
        // open protocol now (server.openMenu → ClientboundOpenScreenPacket
        // + TestScreenMenu's extra-data buffer). The only custom payload
        // is the reverse direction: telling the server about edits made
        // on the client side of the menu.
        registrar.playToServer(
                UpdateCanvasStatePayload.TYPE,
                UpdateCanvasStatePayload.STREAM_CODEC,
                ServerPayloadHandler::handleUpdate
        );
    }
}
