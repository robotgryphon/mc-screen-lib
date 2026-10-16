package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.nodes.properties.AllowedValues;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import dev.robotgryphon.screenlib.graph.state.CanvasState;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record SetNodePropertyPayload<T>(UUID node, Holder<NodeDefinition> definition,
                                        StoredPropertyDefinition<T> property, Optional<T> value)
        implements CanvasEditPayload {
    public static final Type<SetNodePropertyPayload<?>> TYPE = new Type<>(ScreenLib.id("set_node_property"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetNodePropertyPayload<?>> STREAM_CODEC =
            ByteBufCodecs.holderRegistry(NodeDefinition.REGISTRY_KEY)
                    .dispatch(SetNodePropertyPayload::definition, SetNodePropertyPayload::contentStreamCodec);

    private static StreamCodec<RegistryFriendlyByteBuf, SetNodePropertyPayload<?>> contentStreamCodec(Holder<NodeDefinition> definition) {
        List<StoredPropertyDefinition<?>> properties = definition.value().stored();
        return StreamCodec.of(
                (buffer, payload) -> {
                    ByteBufCodecs.VAR_INT.encode(buffer, properties.indexOf(payload.property()));
                    payload.writeContent(buffer);
                },
                buffer -> {
                    int index = ByteBufCodecs.VAR_INT.decode(buffer);
                    if (index < 0 || index >= properties.size()) {
                        throw new DecoderException("Unknown property index " + index);
                    }
                    return read(definition, properties.get(index), buffer);
                });
    }

    private static <T> SetNodePropertyPayload<T> read(Holder<NodeDefinition> definition, StoredPropertyDefinition<T> property,
                                                      RegistryFriendlyByteBuf buffer) {
        UUID node = UUIDUtil.STREAM_CODEC.decode(buffer);
        Optional<T> value = buffer.readBoolean()
                ? Optional.of(property.type().streamCodec().decode(buffer))
                : Optional.empty();
        return new SetNodePropertyPayload<>(node, definition, property, value);
    }

    private void writeContent(RegistryFriendlyByteBuf buffer) {
        UUIDUtil.STREAM_CODEC.encode(buffer, this.node);
        buffer.writeBoolean(this.value.isPresent());
        this.value.ifPresent(present -> this.property.type().streamCodec().encode(buffer, present));
    }

    @Override
    public Optional<CanvasState> apply(CanvasState state, RegistryAccess registries, Set<UUID> required) {
        var node = state.node(this.node).filter(found -> found.definition().value() == this.definition.value());
        if (node.isEmpty()) {
            return Optional.empty();
        }
        if (this.value.isPresent()) {
            var allowed = AllowedValues.live(registries).of(this.property.anyType());
            if (allowed.isPresent() && !allowed.get().contains(this.value.get())) {
                return Optional.empty();
            }
        }
        return Optional.of(state.withNode(node.get().withProperty(this.property, this.value)));
    }

    @Override
    public Type<SetNodePropertyPayload<?>> type() {
        return TYPE;
    }
}
