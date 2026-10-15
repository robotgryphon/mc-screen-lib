package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.state.CanvasState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UpdateCanvasStatePayload(CanvasState state, int sequence) implements CustomPacketPayload {
    public static final Type<UpdateCanvasStatePayload> TYPE =
            new Type<>(ScreenLib.id("update_canvas_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateCanvasStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    CanvasState.STREAM_CODEC,
                    UpdateCanvasStatePayload::state,
                    ByteBufCodecs.VAR_INT,
                    UpdateCanvasStatePayload::sequence,
                    UpdateCanvasStatePayload::new
            );

    @Override
    public Type<UpdateCanvasStatePayload> type() {
        return TYPE;
    }
}
