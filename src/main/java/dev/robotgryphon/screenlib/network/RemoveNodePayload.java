package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.state.CanvasState;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record RemoveNodePayload(UUID node) implements CanvasEditPayload {
    public static final Type<RemoveNodePayload> TYPE = new Type<>(ScreenLib.id("remove_node"));

    public static final StreamCodec<ByteBuf, RemoveNodePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RemoveNodePayload::node,
            RemoveNodePayload::new
    );

    @Override
    public Optional<CanvasState> apply(CanvasState state, RegistryAccess registries, Set<UUID> required) {
        if (required.contains(this.node) || state.node(this.node).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(state.withoutNode(this.node));
    }

    @Override
    public Type<RemoveNodePayload> type() {
        return TYPE;
    }
}
