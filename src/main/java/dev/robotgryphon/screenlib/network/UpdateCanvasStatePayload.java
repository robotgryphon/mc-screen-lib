package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.CanvasState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client→server packet carrying the user's freshly-edited canvas state.
 * The client emits this from {@code TestScreen.onClose} right before the
 * vanilla container-close packet so the server can apply the snapshot to
 * the open {@link dev.robotgryphon.screenlib.menu.TestScreenMenu} while
 * it's still alive.
 *
 * <p>The server handler then writes the state through to the level's
 * {@link ScreenLib#TEST_SCREEN_ATTACHMENT}, which is the authoritative
 * persistent location.
 */
public record UpdateCanvasStatePayload(CanvasState state) implements CustomPacketPayload {

    public static final Type<UpdateCanvasStatePayload> TYPE =
            new Type<>(ScreenLib.id("update_canvas_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateCanvasStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodecWithRegistries(CanvasState.CODEC),
                    UpdateCanvasStatePayload::state,
                    UpdateCanvasStatePayload::new
            );

    @Override
    public Type<UpdateCanvasStatePayload> type() {
        return TYPE;
    }
}
