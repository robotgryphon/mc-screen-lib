package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.state.CanvasState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CanvasSyncPayload(CanvasState state) implements CustomPacketPayload {
    public static final Type<CanvasSyncPayload> TYPE = new Type<>(ScreenLib.id("canvas_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasSyncPayload> STREAM_CODEC = StreamCodec.composite(
            CanvasState.STREAM_CODEC, CanvasSyncPayload::state,
            CanvasSyncPayload::new
    );

    @Override
    public Type<CanvasSyncPayload> type() {
        return TYPE;
    }
}
