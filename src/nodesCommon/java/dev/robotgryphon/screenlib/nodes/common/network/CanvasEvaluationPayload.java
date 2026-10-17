package dev.robotgryphon.screenlib.nodes.common.network;

import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.nodes.common.sync.NodeComputed;
import dev.robotgryphon.screenlib.nodes.common.sync.NodeClientData;
import dev.robotgryphon.screenlib.nodes.common.sync.NodeValidity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record CanvasEvaluationPayload(List<NodeValidity> validity,
                                      List<NodeComputed> computed,
                                      List<NodeClientData> clientData) implements CustomPacketPayload {
    public static final Type<CanvasEvaluationPayload> TYPE =
            new Type<>(ScreenLib.id("canvas_evaluation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasEvaluationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    NodeValidity.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    CanvasEvaluationPayload::validity,
                    NodeComputed.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    CanvasEvaluationPayload::computed,
                    NodeClientData.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    CanvasEvaluationPayload::clientData,
                    CanvasEvaluationPayload::new
            );

    @Override
    public Type<CanvasEvaluationPayload> type() {
        return TYPE;
    }
}
