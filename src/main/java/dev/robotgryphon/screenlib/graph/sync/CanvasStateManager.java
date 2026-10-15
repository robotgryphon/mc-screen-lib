package dev.robotgryphon.screenlib.graph.sync;

import dev.robotgryphon.screenlib.graph.state.CanvasState;
import dev.robotgryphon.screenlib.graph.state.ConnectionState;
import dev.robotgryphon.screenlib.graph.state.NodeState;

import dev.robotgryphon.screenlib.graph.canvas.CanvasGraph;

import dev.robotgryphon.screenlib.graph.Connection;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import dev.robotgryphon.screenlib.graph.nodes.ports.Port;
import dev.robotgryphon.screenlib.graph.nodes.ports.PortSide;
import dev.robotgryphon.screenlib.graph.nodes.ports.PropertyInputPort;
import dev.robotgryphon.screenlib.graph.nodes.ports.PropertyOutputPort;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class CanvasStateManager {
    private CanvasStateManager() {}

    public static CanvasState toState(UUID id, CanvasGraph canvas, Function<Node, Vector2ic> positions,
                                      RegistryAccess registryAccess) {
        var nodeDefinitions = registryAccess.lookupOrThrow(NodeDefinition.REGISTRY_KEY);

        List<NodeState> nodeStates = new ArrayList<>(canvas.nodes().size());
        for (Node node : canvas.nodes()) {
            nodeStates.add(new NodeState(node.id(), nodeDefinitions.getOrThrow(node.type()),
                    new Vector2i(positions.apply(node)), encodePropertyValues(node)));
        }

        List<ConnectionState> connectionStates = new ArrayList<>(canvas.connections().all().size());
        for (Connection c : canvas.connections().all()) {
            connectionStates.add(new ConnectionState(c.source().id(), portName(c.sourcePort()),
                    c.target().id(), portName(c.targetPort())));
        }

        return new CanvasState(id, nodeStates, connectionStates);
    }

    private static String portName(Port port) {
        return switch (port) {
            case PropertyInputPort p -> p.propertyName();
            case PropertyOutputPort p -> p.propertyName();
        };
    }

    public static void loadState(CanvasGraph canvas, CanvasState state) {
        canvas.clear();

        Map<UUID, Node> byId = new HashMap<>();
        for (NodeState ns : state.nodes()) {
            Node node = new Node(canvas, ns.id(), ns.definition(), Node.defaultTitle(ns.definition().key()));
            applyPropertyValues(node, ns.propertyValues());
            if (byId.putIfAbsent(ns.id(), node) == null) {
                canvas.addNode(node);
            }
        }

        for (ConnectionState cs : state.connections()) {
            Node srcNode = byId.get(cs.sourceNode());
            Node tgtNode = byId.get(cs.targetNode());
            if (srcNode == null || tgtNode == null) continue;

            Port srcPort = resolvePort(srcNode, PortSide.RIGHT, cs.sourcePort());
            Port tgtPort = resolvePort(tgtNode, PortSide.LEFT, cs.targetPort());
            if (srcPort == null || tgtPort == null) continue;
            if (srcPort.type().value() != tgtPort.type().value()) continue;

            canvas.connections().add(new Connection(srcNode, srcPort, tgtNode, tgtPort));
        }
    }

    private static @Nullable Port resolvePort(Node node, PortSide side, String name) {
        for (Port port : node.portManager().ports()) {
            if (port.side() == side && name.equals(portName(port))) {
                return port;
            }
        }
        return null;
    }

    private static Map<String, Dynamic<?>> encodePropertyValues(Node node) {
        Map<String, Dynamic<?>> encoded = new HashMap<>();
        for (StoredPropertyDefinition<?> prop : node.propertyManager().stored()) {
            Object value = node.propertyManager().value(prop.name());
            if (value == null) continue;
            @SuppressWarnings({"unchecked", "rawtypes"})
            DataResult<Tag> result = ((Codec) prop.type().value().codec())
                    .encodeStart(NbtOps.INSTANCE, value);
            result.result().ifPresent(tag -> encoded.put(prop.name(), new Dynamic<>(NbtOps.INSTANCE, tag)));
        }
        return encoded;
    }

    private static void applyPropertyValues(Node node, Map<String, Dynamic<?>> stored) {
        if (stored.isEmpty()) return;
        Map<String, StoredPropertyDefinition<?>> byName = new HashMap<>();
        for (StoredPropertyDefinition<?> prop : node.propertyManager().stored()) {
            byName.put(prop.name(), prop);
        }
        for (Map.Entry<String, Dynamic<?>> entry : stored.entrySet()) {
            StoredPropertyDefinition<?> prop = byName.get(entry.getKey());
            if (prop == null) continue;
            @SuppressWarnings({"unchecked", "rawtypes"})
            DataResult<?> decoded = ((Codec) prop.type().value().codec())
                    .parse(entry.getValue());
            decoded.result().ifPresent(value -> node.propertyManager().set(prop.name(), value));
        }
    }

}
