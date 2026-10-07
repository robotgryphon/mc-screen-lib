package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import dev.robotgryphon.screenlib.graph.nodes.NodeState;
import dev.robotgryphon.screenlib.graph.ports.Port;
import dev.robotgryphon.screenlib.graph.ports.PortSide;
import dev.robotgryphon.screenlib.graph.ports.PropertyInputPort;
import dev.robotgryphon.screenlib.graph.ports.PropertyOutputPort;
import dev.robotgryphon.screenlib.graph.schema.node.StoredPropertyDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CanvasStateManager {
    private CanvasStateManager() {}

    public static CanvasState toState(Canvas canvas, Optional<ViewportState> viewport, RegistryAccess registryAccess) {
        List<Node> nodes = canvas.nodes();
        List<NodeState> nodeStates = new ArrayList<>(nodes.size());
        Map<Node, Integer> indexOf = new IdentityHashMap<>(nodes.size());

        var nodeDefinitions = registryAccess.lookupOrThrow(NodeDefinition.REGISTRY_KEY);

        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            indexOf.put(node, i);
            nodeStates.add(new NodeState(
                    nodeDefinitions.getOrThrow(node.type()), canvas.x(node), canvas.y(node),
                    encodePropertyValues(node),
                    Optional.ofNullable(canvas.tint(node))));
        }

        List<Connection> connections = canvas.connections();
        List<ConnectionState> connectionStates = new ArrayList<>(connections.size());
        for (Connection c : connections) {
            Integer srcIdx = indexOf.get(c.source());
            Integer tgtIdx = indexOf.get(c.target());
            if (srcIdx == null || tgtIdx == null) {
                continue;
            }
            int srcPortIdx = c.source().portManager().ports().indexOf(c.sourcePort());
            int tgtPortIdx = c.target().portManager().ports().indexOf(c.targetPort());
            if (srcPortIdx < 0 || tgtPortIdx < 0) {
                continue;
            }
            connectionStates.add(new ConnectionState(
                    srcIdx, srcPortIdx, Optional.of(portLocalName(c.sourcePort())),
                    tgtIdx, tgtPortIdx, Optional.of(portLocalName(c.targetPort()))));
        }

        return new CanvasState(nodeStates, connectionStates, viewport);
    }

    private static String portLocalName(Port port) {
        return switch (port) {
            case PropertyInputPort p -> p.propertyName();
            case PropertyOutputPort p -> p.propertyName();
            default -> port.title().getString();
        };
    }

    public static void loadState(Canvas canvas, CanvasState state) {
        canvas.clear();

        for (NodeState ns : state.nodes()) {
            Node node = new Node(canvas, ns.definition(), Node.defaultTitle(ns.definition().key()), ns.x(), ns.y());
            applyPropertyValues(node, ns.propertyValues());
            ns.tintColor().ifPresent(c -> canvas.setTint(node, c));
            canvas.addNode(node);
        }

        List<Node> rebuilt = canvas.nodes();
        for (ConnectionState cs : state.connections()) {
            if (!isValidIndex(cs.sourceNodeIndex(), rebuilt.size())) continue;
            if (!isValidIndex(cs.targetNodeIndex(), rebuilt.size())) continue;
            Node srcNode = rebuilt.get(cs.sourceNodeIndex());
            Node tgtNode = rebuilt.get(cs.targetNodeIndex());

            Port srcPort = resolvePort(srcNode, PortSide.RIGHT, cs.sourcePortName(), cs.sourcePortIndex());
            Port tgtPort = resolvePort(tgtNode, PortSide.LEFT, cs.targetPortName(), cs.targetPortIndex());
            if (srcPort == null || tgtPort == null) continue;
            if (srcPort.type().value() != tgtPort.type().value()) continue;

            canvas.addConnection(new Connection(srcNode, srcPort, tgtNode, tgtPort));
        }
    }

    private static @Nullable Port resolvePort(Node node, PortSide side,
                                              Optional<String> savedName,
                                              int savedIndex) {
        List<Port> ports = node.portManager().ports();
        if (savedName.isPresent()) {
            String name = savedName.get();
            for (Port port : ports) {
                if (port.side() == side && name.equals(portLocalName(port))) {
                    return port;
                }
            }
        }
        if (isValidIndex(savedIndex, ports.size()) && ports.get(savedIndex).side() == side) {
            return ports.get(savedIndex);
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

    private static boolean isValidIndex(int idx, int size) {
        return idx >= 0 && idx < size;
    }
}
