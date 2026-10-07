package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.evaluation.NodeComputed;
import dev.robotgryphon.screenlib.graph.evaluation.NodeClientData;
import dev.robotgryphon.screenlib.graph.validation.NodeValidity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record CanvasEvaluationPayload(int sequence, List<NodeValidity> validity,
                                      List<NodeComputed> computed,
                                      List<NodeClientData> clientData) implements CustomPacketPayload {
    public static final Type<CanvasEvaluationPayload> TYPE =
            new Type<>(ScreenLib.id("canvas_evaluation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasEvaluationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    CanvasEvaluationPayload::sequence,
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
