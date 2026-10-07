package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import dev.robotgryphon.screenlib.graph.schema.AllowedValuesSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

import java.util.List;

public record AddNodeData(List<ResourceKey<NodeDefinition>> available, int x, int y,
                          AllowedValuesSnapshot allowedValues) {
    public static final StreamCodec<RegistryFriendlyByteBuf, AddNodeData> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(NodeDefinition.REGISTRY_KEY).apply(ByteBufCodecs.list()),
            AddNodeData::available,
            ByteBufCodecs.VAR_INT,
            AddNodeData::x,
            ByteBufCodecs.VAR_INT,
            AddNodeData::y,
            AllowedValuesSnapshot.STREAM_CODEC,
            AddNodeData::allowedValues,
            AddNodeData::new
    );
}
