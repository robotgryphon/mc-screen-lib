package dev.robotgryphon.screenlib.graph.ports;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

public record StandardInputPort(Node node, Component title, Holder<GraphDataType<?>> type,
                                boolean optional) implements InputPort {
}
