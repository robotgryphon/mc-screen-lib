package dev.robotgryphon.screenlib.graph.nodes.ports;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

public sealed interface Port permits InputPort, OutputPort {
    Node node();

    Component title();

    Holder<GraphDataType<?>> type();

    PortSide side();

    String propertyName();
}
