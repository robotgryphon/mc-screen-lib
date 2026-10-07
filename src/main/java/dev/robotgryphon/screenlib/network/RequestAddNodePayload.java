package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestAddNodePayload(int x, int y) implements CustomPacketPayload {
    public static final Type<RequestAddNodePayload> TYPE =
            new Type<>(ScreenLib.id("request_add_node"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestAddNodePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RequestAddNodePayload::x,
                    ByteBufCodecs.VAR_INT, RequestAddNodePayload::y,
                    RequestAddNodePayload::new
            );

    @Override
    public Type<RequestAddNodePayload> type() {
        return TYPE;
    }
}
