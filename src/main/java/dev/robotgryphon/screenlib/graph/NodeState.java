package dev.robotgryphon.screenlib.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.types.NodeDefinition;
import net.minecraft.core.Holder;

/**
 * Serializable snapshot of one node placed on a {@link Canvas}.
 *
 * <p>Only the pieces a Codec can faithfully reconstruct are kept here: the
 * registry reference for the schema and the canvas-space position. The
 * runtime {@link Node} that gets rebuilt from this state recomputes its
 * own dimensions from the active font, and the host's
 * {@code loadState} callback is responsible for deciding the title (the
 * default — translated from the registry key — is what every spawn path
 * already uses, so most consumers don't need anything custom).
 *
 * @param definition registry holder for the node's schema; resolved via
 *                   {@link NodeDefinition#HOLDER_CODEC}
 * @param x          canvas-space x of the node's top-left corner
 * @param y          canvas-space y of the node's top-left corner
 */
public record NodeState(Holder<NodeDefinition> definition, int x, int y) {

    public static final Codec<NodeState> CODEC = RecordCodecBuilder.create(i -> i.group(
            NodeDefinition.HOLDER_CODEC.fieldOf("definition").forGetter(NodeState::definition),
            Codec.INT.fieldOf("x").forGetter(NodeState::x),
            Codec.INT.fieldOf("y").forGetter(NodeState::y)
    ).apply(i, NodeState::new));
}
