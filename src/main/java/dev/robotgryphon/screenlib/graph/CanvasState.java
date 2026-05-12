package dev.robotgryphon.screenlib.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Serializable snapshot of a {@link Canvas}'s placed nodes and the
 * connections between them. Storage is the modder's call — the
 * {@link #CODEC} produces and consumes a generic Dynamic representation,
 * so the same record round-trips through JSON, NBT, packet buffers, etc.
 *
 * <p>Pan/zoom state is intentionally out of scope: those are view-level
 * concerns owned by the user's session, not part of the document the
 * canvas describes.
 *
 * <p>Use {@link Canvas#toState()} to capture and
 * {@link Canvas#loadState(CanvasState, java.util.function.Function)} to
 * restore. The empty constant {@link #EMPTY} is provided for "no saved
 * data yet" defaults.
 */
public record CanvasState(List<NodeState> nodes, List<ConnectionState> connections) {

    /** Convenience constant for a brand-new canvas. */
    public static final CanvasState EMPTY = new CanvasState(List.of(), List.of());

    /**
     * MapCodec form. NeoForge's {@code AttachmentType.Builder#serialize}
     * specifically takes a {@link MapCodec}, and several other persistence
     * paths in vanilla / NeoForge expect the same shape, so we keep this as
     * the source of truth and derive the regular {@link #CODEC} from it.
     */
    public static final MapCodec<CanvasState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NodeState.CODEC.listOf()
                    .optionalFieldOf("nodes", List.of())
                    .forGetter(CanvasState::nodes),
            ConnectionState.CODEC.listOf()
                    .optionalFieldOf("connections", List.of())
                    .forGetter(CanvasState::connections)
    ).apply(i, CanvasState::new));

    public static final Codec<CanvasState> CODEC = MAP_CODEC.codec();
}
