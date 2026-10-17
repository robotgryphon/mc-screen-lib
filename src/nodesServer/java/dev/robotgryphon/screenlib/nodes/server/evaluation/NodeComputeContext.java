package dev.robotgryphon.screenlib.nodes.server.evaluation;

import dev.robotgryphon.screenlib.nodes.common.Node;
import dev.robotgryphon.screenlib.graph.schema.properties.DataKey;
import dev.robotgryphon.screenlib.graph.schema.properties.computed.ComputeContext;
import dev.robotgryphon.screenlib.graph.schema.properties.ComputedPropertyDefinition;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;

record NodeComputeContext(Node node, ComputedPropertyDefinition<?> definition, MinecraftServer server)
        implements ComputeContext {
    @Override
    public <T> Optional<T> get(DataKey<T> key) {
        String property = this.definition.data().get(key.name());
        if (property == null) {
            return Optional.empty();
        }
        Class<T> type = key.type().get().dataType();
        return this.node.propertyManager().resolve(property).filter(type::isInstance).map(type::cast);
    }
}
