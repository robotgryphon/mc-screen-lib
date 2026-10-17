package dev.robotgryphon.screenlib.nodes.common.network;

import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.nodes.api.state.CanvasState;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

public record RemoveNodePayload(UUID node) implements CanvasEditPayload {
    public static final Type<RemoveNodePayload> TYPE = new Type<>(ScreenLib.id("remove_node"));

    public static final StreamCodec<ByteBuf, RemoveNodePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RemoveNodePayload::node,
            RemoveNodePayload::new
    );

    @Override
    public Optional<CanvasState> apply(CanvasState state, RegistryAccess registries) {
        if (state.node(this.node).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(state.withoutNode(this.node));
    }

    @Override
    public Type<RemoveNodePayload> type() {
        return TYPE;
    }
}
