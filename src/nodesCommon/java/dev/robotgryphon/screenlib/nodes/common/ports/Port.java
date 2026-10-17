package dev.robotgryphon.screenlib.nodes.common.ports;

import dev.robotgryphon.screenlib.nodes.common.Node;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

public sealed interface Port permits PropertyInputPort, PropertyOutputPort {
    Node node();

    Component title();

    Holder<GraphDataType<?>> type();

    String propertyName();
}
