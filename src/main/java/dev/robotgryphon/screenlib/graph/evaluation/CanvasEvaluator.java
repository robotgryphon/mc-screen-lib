package dev.robotgryphon.screenlib.graph.evaluation;

import dev.robotgryphon.screenlib.graph.sync.CanvasEvaluation;
import dev.robotgryphon.screenlib.graph.sync.ClientDataSubscription;
import dev.robotgryphon.screenlib.graph.sync.NodeClientData;
import dev.robotgryphon.screenlib.graph.sync.NodeComputed;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.graph.nodes.NodeReference;
import dev.robotgryphon.screenlib.graph.canvas.CanvasGraph;
import dev.robotgryphon.screenlib.graph.state.CanvasState;
import dev.robotgryphon.screenlib.graph.sync.CanvasStateManager;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.properties.PropertyManager;
import dev.robotgryphon.screenlib.graph.nodes.properties.AllowedValues;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.graph.validation.GraphValidator;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class CanvasEvaluator {
    private CanvasEvaluator() {}

    public static Session open(CanvasState state, RegistryAccess registries, MinecraftServer server) {
        CanvasGraph canvas = new CanvasGraph(AllowedValues.live(registries));
        CanvasStateManager.loadState(canvas, state);
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
                properties.resolve(property.name()).ifPresent(value -> values.put(property.name(), value));
            }
            for (ComputedPropertyDefinition property : properties.computedProperties()) {
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
        List<Node> nodes = canvas.nodes();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            PropertyManager properties = node.propertyManager();
            for (ComputedPropertyDefinition definition : properties.computedProperties()) {
                var provider = definition.clientData();
                Object value = properties.computedValue(definition.name());
                if (provider.isEmpty() || value == null
                        || !subscriptions.contains(new ClientDataSubscription(node.type(), definition.name()))) {
                    continue;
                }
                Optional<Object> data = provider.get().clientData(value, definition.context(node, server));
                if (data.isEmpty()) {
                    continue;
                }
                var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
                provider.get().clientCodec().encode(buffer, data.get());
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes);
                results.add(new NodeClientData(i, definition.name(), bytes));
            }
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private static List<NodeComputed> computeAll(CanvasGraph canvas, RegistryAccess registries, MinecraftServer server) {
        List<Node> nodes = canvas.nodes();
        for (int pass = 0; pass <= nodes.size(); pass++) {
            boolean changed = false;
            for (Node node : nodes) {
                PropertyManager properties = node.propertyManager();
                for (ComputedPropertyDefinition definition : properties.computedProperties()) {
                    Object next = definition.computation().value()
                            .compute(definition.context(node, server))
                            .orElse(null);
                    if (!Objects.equals(properties.computedValue(definition.name()), next)) {
                        properties.setComputedValue(definition.name(), next);
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }

        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        List<NodeComputed> results = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            PropertyManager properties = nodes.get(i).propertyManager();
            Map<String, Tag> tags = new HashMap<>();
            for (ComputedPropertyDefinition definition : properties.computedProperties()) {
                Object value = properties.computedValue(definition.name());
                if (value == null) {
                    continue;
                }
                Codec<Object> codec = (Codec<Object>) definition.type().value().codec();
                codec.encodeStart(ops, value).result().ifPresent(tag -> tags.put(definition.name(), tag));
            }
            if (!tags.isEmpty()) {
                results.add(new NodeComputed(i, tags));
            }
        }
        return results;
    }
}
