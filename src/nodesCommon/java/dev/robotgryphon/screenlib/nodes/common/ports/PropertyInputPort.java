package dev.robotgryphon.screenlib.nodes.common.ports;

import dev.robotgryphon.screenlib.nodes.common.Node;
import dev.robotgryphon.screenlib.graph.schema.properties.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

public record PropertyInputPort(Node node, StoredPropertyDefinition<?> property) implements Port {
    public String propertyName() {
        return this.property.name();
    }

    @Override
    public Component title() {
        return Node.propertyLabel(this.node.type(), this.property.name());
    }

    @Override
    public Holder<GraphDataType<?>> type() {
        return this.property.anyType();
    }
}
