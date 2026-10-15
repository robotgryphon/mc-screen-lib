package dev.robotgryphon.screenlib.graph.nodes.ports;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

public record PropertyInputPort(Node node, StoredPropertyDefinition<?> property) implements InputPort {
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

    @Override
    public boolean optional() {
        return true;
    }
}
