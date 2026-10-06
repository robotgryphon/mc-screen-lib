package dev.robotgryphon.screenlib.graph.ports;

import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

public sealed interface PortDefinition permits InputPortDefinition, OutputPortDefinition {
    String name();

    Holder<GraphDataType<?>> type();
}
