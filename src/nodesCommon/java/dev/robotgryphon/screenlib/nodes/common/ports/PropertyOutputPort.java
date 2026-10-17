package dev.robotgryphon.screenlib.nodes.common.ports;

import dev.robotgryphon.screenlib.nodes.common.Node;
import dev.robotgryphon.screenlib.graph.schema.properties.PropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

public record PropertyOutputPort(Node node, Component title, PropertyDefinition<?> property) implements Port {
    @Override
    public Holder<GraphDataType<?>> type() {
        return this.property.anyType();
    }

    @Override
    public String propertyName() {
        return this.property.name();
    }
}
