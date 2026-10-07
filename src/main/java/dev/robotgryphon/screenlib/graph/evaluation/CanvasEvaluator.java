package dev.robotgryphon.screenlib.graph.evaluation;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.graph.CanvasStateManager;
import dev.robotgryphon.screenlib.graph.computed.ComputeContext;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.PropertyManager;
import dev.robotgryphon.screenlib.graph.schema.AllowedValues;
import dev.robotgryphon.screenlib.graph.schema.node.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.validation.GraphValidator;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
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
            var transfer = this.canvas.transfer();
            if (transfer != null) {
                transfer.refresh();
            }
            List<NodeComputed> computed = computeAll(this.canvas, this.registries);
            List<NodeRendered> rendered = extractRenderStates(this.canvas, this.registries);
            return new CanvasEvaluation(GraphValidator.validate(this.canvas), computed, rendered);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<NodeRendered> extractRenderStates(Canvas canvas, RegistryAccess registries) {
        List<NodeRendered> results = new ArrayList<>();
        List<Node> nodes = canvas.nodes();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            var providers = node.renderManager().providers();
            for (int j = 0; j < providers.size(); j++) {
                var provider = providers.get(j).value();
                Optional<?> state = provider.extract(new ComputeContext(node));
                if (state.isEmpty()) {
                    continue;
                }
                var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
                ((StreamCodec<RegistryFriendlyByteBuf, Object>) provider.codec()).encode(buffer, state.get());
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                results.add(new NodeRendered(i, j, data));
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
                            .compute(new ComputeContext(node))
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
