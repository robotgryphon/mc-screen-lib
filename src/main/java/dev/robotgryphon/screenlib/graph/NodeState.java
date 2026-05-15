package dev.robotgryphon.screenlib.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import net.minecraft.core.Holder;

import java.util.Map;

/**
 * Serializable snapshot of one node placed on a {@link Canvas}.
 *
 * <p>Carries everything a Codec can faithfully reconstruct: the registry
 * reference for the schema, the canvas-space position, and the current
 * values of any configurable properties the user has set (or that
 * defaulted from the schema). The runtime {@link Node} that gets rebuilt
 * from this state recomputes its own dimensions from the active font;
 * the host's {@code loadState} callback decides the title (the default —
 * translated from the registry key — is what every spawn path already
 * uses, so most consumers don't need anything custom).
 *
 * <p>Property values ride as a {@code Map<String, Dynamic<?>>} — the
 * codec-agnostic raw form — because the codec for {@code NodeState} can't
 * know what concrete value type to deserialize each entry into until the
 * matching {@link dev.robotgryphon.screenlib.types.PropertyDefinition}
 * has been resolved. {@link Canvas#loadState} walks the definition and
 * decodes each Dynamic via the property type's own codec when applying
 * the state to a freshly built {@link Node}.
 *
 * @param definition       registry holder for the node's schema; resolved via
 *                         {@link NodeDefinition#HOLDER_CODEC}
 * @param x                canvas-space x of the node's top-left corner
 * @param y                canvas-space y of the node's top-left corner
 * @param propertyValues   per-property serialized current values, keyed by
 *                         property name; empty when nothing has been set
 */
public record NodeState(Holder<NodeDefinition> definition, int x, int y,
                        Map<String, Dynamic<?>> propertyValues) {

    /** Convenience for the no-properties case — keeps test-only call sites unchanged. */
    public NodeState(Holder<NodeDefinition> definition, int x, int y) {
        this(definition, x, y, Map.of());
    }

    public static final Codec<NodeState> CODEC = RecordCodecBuilder.create(i -> i.group(
            NodeDefinition.HOLDER_CODEC.fieldOf("definition").forGetter(NodeState::definition),
            Codec.INT.fieldOf("x").forGetter(NodeState::x),
            Codec.INT.fieldOf("y").forGetter(NodeState::y),
            // Unbounded map keyed by property name. The values use
            // PASSTHROUGH so each Dynamic flows through whichever ops
            // the canvas codec happens to be running on (NbtOps for the
            // attachment path, JsonOps for any future export) without
            // needing to know the property's value type at this layer.
            Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH)
                    .optionalFieldOf("properties", Map.of())
                    .forGetter(NodeState::propertyValues)
    ).apply(i, NodeState::new));
}
