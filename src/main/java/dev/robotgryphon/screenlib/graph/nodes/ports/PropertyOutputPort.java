package dev.robotgryphon.screenlib.graph.nodes.ports;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.PropertyDefinition;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

public record PropertyOutputPort(Node node, Component title, PropertyDefinition<?> property) implements OutputPort {
    @Override
    public Holder<GraphDataType<?>> type() {
        return this.property.anyType();
    }

    @Override
    public String propertyName() {
        return this.property.name();
    }
}
