package dev.robotgryphon.screenlib.nodes.server.evaluation;

import dev.robotgryphon.screenlib.graph.schema.properties.computed.ComputeContext;
import dev.robotgryphon.screenlib.graph.schema.properties.computed.ClientDataProvider;
import dev.robotgryphon.screenlib.graph.schema.properties.PropertyValues;
import dev.robotgryphon.screenlib.nodes.common.sync.CanvasEvaluation;
import dev.robotgryphon.screenlib.nodes.common.sync.ClientDataSubscription;
import dev.robotgryphon.screenlib.nodes.common.sync.NodeClientData;
import dev.robotgryphon.screenlib.nodes.common.sync.NodeComputed;
import dev.robotgryphon.screenlib.nodes.api.NodeReference;
import dev.robotgryphon.screenlib.nodes.common.canvas.CanvasGraph;
import dev.robotgryphon.screenlib.nodes.api.state.CanvasState;
import dev.robotgryphon.screenlib.nodes.common.Node;
import dev.robotgryphon.screenlib.nodes.common.properties.PropertyManager;
import dev.robotgryphon.screenlib.nodes.common.properties.AllowedValues;
import dev.robotgryphon.screenlib.graph.schema.properties.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.properties.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.nodes.server.validation.GraphValidator;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CanvasEvaluator {
    private CanvasEvaluator() {}

    public static Session open(CanvasState state, RegistryAccess registries, MinecraftServer server) {
        CanvasGraph canvas = new CanvasGraph(AllowedValues.live(registries));
        canvas.load(state);
        return new Session(canvas, registries, server);
    }

    public static final class Session {
        private final CanvasGraph canvas;
        private final RegistryAccess registries;
        private final MinecraftServer server;

        private Session(CanvasGraph canvas, RegistryAccess registries, MinecraftServer server) {
            this.canvas = canvas;
            this.registries = registries;
            this.server = server;
        }

        public CanvasEvaluation evaluate(Set<ClientDataSubscription> subscriptions) {
            List<NodeComputed> computed = computeAll(this.canvas, this.registries, this.server);
            List<NodeClientData> clientData = extractClientData(this.canvas, this.registries, this.server, subscriptions);
            return new CanvasEvaluation(GraphValidator.validate(this.canvas), computed, clientData);
        }

        public void compute() {
            computeAll(this.canvas, this.registries, this.server);
        }

        public Optional<NodeSnapshot> snapshot(NodeReference reference) {
            return this.canvas.node(reference).map(node -> new NodeSnapshot(GraphValidator.errors(node), values(node)));
        }

        private static Map<String, Object> values(Node node) {
            Map<String, Object> values = new HashMap<>();
            PropertyManager properties = node.propertyManager();
            for (StoredPropertyDefinition<?> property : properties.stored()) {
                properties.resolve(property).ifPresent(value -> values.put(property.name(), value));
            }
            for (ComputedPropertyDefinition<?> property : properties.computedProperties()) {
                Object value = properties.computedValue(property.name());
                if (value != null) {
                    values.put(property.name(), value);
                }
            }
            return values;
        }
    }

    private static List<NodeClientData> extractClientData(CanvasGraph canvas, RegistryAccess registries,
                                                          MinecraftServer server,
                                                          Set<ClientDataSubscription> subscriptions) {
        List<NodeClientData> results = new ArrayList<>();
        for (Node node : canvas.nodes()) {
            for (ComputedPropertyDefinition<?> definition : node.propertyManager().computedProperties()) {
                if (subscriptions.contains(new ClientDataSubscription(node.type(), definition.name()))) {
                    extractClientData(node, definition, registries, server).ifPresent(results::add);
                }
            }
        }
        return results;
    }

    private static <T> Optional<NodeClientData> extractClientData(Node node, ComputedPropertyDefinition<T> definition,
                                                                  RegistryAccess registries, MinecraftServer server) {
        Optional<T> value = node.propertyManager().computed(definition);
        return definition.function().clientDataProvider()
                .flatMap(provider -> value.flatMap(present -> encodeClientData(provider, present,
                        new NodeComputeContext(node, definition, server), registries)))
                .map(bytes -> new NodeClientData(node.id(), definition.name(), bytes));
    }

    private static <T, D> Optional<byte[]> encodeClientData(ClientDataProvider<T, D> provider, T value,
                                                            ComputeContext context, RegistryAccess registries) {
        return provider.clientData(value, context).map(data -> {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            provider.clientCodec().encode(buffer, data);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        });
    }

    private static List<NodeComputed> computeAll(CanvasGraph canvas, RegistryAccess registries, MinecraftServer server) {
        List<Node> nodes = canvas.nodes();
        for (int pass = 0; pass <= nodes.size(); pass++) {
            boolean changed = false;
            for (Node node : nodes) {
                for (ComputedPropertyDefinition<?> definition : node.propertyManager().computedProperties()) {
                    changed |= recompute(node, definition, server);
                }
            }
            if (!changed) {
                break;
            }
        }

        List<NodeComputed> results = new ArrayList<>();
        for (Node node : nodes) {
            PropertyValues values = node.propertyManager().computedValues();
            if (!values.isEmpty()) {
                results.add(new NodeComputed(node.id(), node.definition(), values));
            }
        }
        return results;
    }

    private static <T> boolean recompute(Node node, ComputedPropertyDefinition<T> definition, MinecraftServer server) {
        return node.propertyManager().setComputed(definition, definition.function().compute(new NodeComputeContext(node, definition, server)));
    }
}
