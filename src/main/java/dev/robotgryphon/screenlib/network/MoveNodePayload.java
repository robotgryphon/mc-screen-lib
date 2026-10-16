package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.ScreenLibCodecs;
import dev.robotgryphon.screenlib.graph.state.CanvasState;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import org.joml.Vector2ic;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record MoveNodePayload(UUID node, Vector2ic position) implements CanvasEditPayload {
    public static final Type<MoveNodePayload> TYPE = new Type<>(ScreenLib.id("move_node"));

    public static final StreamCodec<ByteBuf, MoveNodePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, MoveNodePayload::node,
            ScreenLibCodecs.VECTOR2I_STREAM, MoveNodePayload::position,
            MoveNodePayload::new
    );

    @Override
    public Optional<CanvasState> apply(CanvasState state, RegistryAccess registries, Set<UUID> required) {
        return state.node(this.node).map(node -> state.withNode(node.withPosition(this.position)));
    }

    @Override
    public Type<MoveNodePayload> type() {
        return TYPE;
    }
}
