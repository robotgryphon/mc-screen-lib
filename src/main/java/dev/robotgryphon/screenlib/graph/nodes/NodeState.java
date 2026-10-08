package dev.robotgryphon.screenlib.graph.nodes;

import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;

import java.util.Map;
import java.util.Optional;

public record NodeState(Holder<NodeDefinition> definition, int x, int y,
                        Map<String, Dynamic<?>> propertyValues,
                        Optional<Integer> tintColor,
                        Optional<String> host) {
    public static final Codec<NodeState> CODEC = RecordCodecBuilder.create(i -> i.group(
            NodeDefinition.HOLDER_CODEC.fieldOf("definition").forGetter(NodeState::definition),
            Codec.INT.fieldOf("x").forGetter(NodeState::x),
            Codec.INT.fieldOf("y").forGetter(NodeState::y),
            Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH)
                    .optionalFieldOf("properties", Map.of())
                    .forGetter(NodeState::propertyValues),
            Codec.INT.optionalFieldOf("tint")
                    .forGetter(NodeState::tintColor),
            Codec.STRING.optionalFieldOf("host")
                    .forGetter(NodeState::host)
    ).apply(i, NodeState::new));
}
