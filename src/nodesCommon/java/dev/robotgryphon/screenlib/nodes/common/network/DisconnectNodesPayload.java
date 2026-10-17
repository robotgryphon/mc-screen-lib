package dev.robotgryphon.screenlib.nodes.common.network;

import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.nodes.api.state.CanvasState;
import dev.robotgryphon.screenlib.nodes.api.state.ConnectionState;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public record DisconnectNodesPayload(ConnectionState connection) implements CanvasEditPayload {
    public static final Type<DisconnectNodesPayload> TYPE = new Type<>(ScreenLib.id("disconnect_nodes"));

    public static final StreamCodec<ByteBuf, DisconnectNodesPayload> STREAM_CODEC = StreamCodec.composite(
            ConnectionState.STREAM_CODEC, DisconnectNodesPayload::connection,
            DisconnectNodesPayload::new
    );

    @Override
    public Optional<CanvasState> apply(CanvasState state, RegistryAccess registries) {
        if (!state.connections().contains(this.connection)) {
            return Optional.empty();
        }
        return Optional.of(state.withConnections(state.connections().stream()
                .filter(existing -> !existing.equals(this.connection))
                .toList()));
    }

    @Override
    public Type<DisconnectNodesPayload> type() {
        return TYPE;
    }
}
