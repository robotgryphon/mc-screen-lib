package dev.robotgryphon.screenlib.graph.ports;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.schema.node.NodeProperty;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

public record PropertyInputPort(Node node, NodeProperty<?> property) implements InputPort {
    public String propertyName() {
        return this.property.name();
    }

    @Override
    public Component title() {
        return Component.literal(this.property.name());
    }

    @Override
    public Holder<GraphDataType<?>> type() {
        return this.property.anyType();
    }

    @Override
    public boolean optional() {
        return true;
    }
}
