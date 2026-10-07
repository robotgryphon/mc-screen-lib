package dev.robotgryphon.screenlib.graph.evaluation;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.graph.CanvasStateManager;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.PropertyManager;
import dev.robotgryphon.screenlib.graph.schema.AllowedValues;
import dev.robotgryphon.screenlib.graph.schema.node.ComputedPropertyDefinition;
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

public final class CanvasEvaluator {
    private CanvasEvaluator() {}

    public static Session open(CanvasState state, RegistryAccess registries, MinecraftServer server) {
        Canvas canvas = new Canvas(AllowedValues.live(registries));
        canvas.attachServer(server);
        CanvasStateManager.loadState(canvas, state);
        return new Session(canvas, registries);
    }

    public static final class Session {
        private final Canvas canvas;
        private final RegistryAccess registries;

        private Session(Canvas canvas, RegistryAccess registries) {
            this.canvas = canvas;
            this.registries = registries;
        }

        public CanvasEvaluation evaluate() {
            List<NodeComputed> computed = computeAll(this.canvas, this.registries);
            List<NodeClientData> clientData = extractClientData(this.canvas, this.registries);
            return new CanvasEvaluation(GraphValidator.validate(this.canvas), computed, clientData);
        }
    }

    private static List<NodeClientData> extractClientData(Canvas canvas, RegistryAccess registries) {
        List<NodeClientData> results = new ArrayList<>();
        List<Node> nodes = canvas.nodes();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            PropertyManager properties = node.propertyManager();
            for (ComputedPropertyDefinition definition : properties.computedProperties()) {
                var provider = definition.clientData();
                Object value = properties.computedValue(definition.name());
                if (!node.renderManager().displays(definition.name()) || provider.isEmpty() || value == null) {
                    continue;
                }
                Optional<Object> data = provider.get().clientData(value, definition.context(node));
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
    private static List<NodeComputed> computeAll(Canvas canvas, RegistryAccess registries) {
        List<Node> nodes = canvas.nodes();
        for (int pass = 0; pass <= nodes.size(); pass++) {
            boolean changed = false;
            for (Node node : nodes) {
                PropertyManager properties = node.propertyManager();
                for (ComputedPropertyDefinition definition : properties.computedProperties()) {
                    Object next = definition.computation().value()
                            .compute(definition.context(node))
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
