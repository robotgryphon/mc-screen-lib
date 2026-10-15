package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.sync.ClientDataSubscription;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record SubscribeClientDataPayload(List<ClientDataSubscription> subscriptions) implements CustomPacketPayload {
    public static final Type<SubscribeClientDataPayload> TYPE =
            new Type<>(ScreenLib.id("subscribe_client_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SubscribeClientDataPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ClientDataSubscription.STREAM_CODEC.apply(ByteBufCodecs.list()), SubscribeClientDataPayload::subscriptions,
                    SubscribeClientDataPayload::new
            );

    @Override
    public Type<SubscribeClientDataPayload> type() {
        return TYPE;
    }
}
