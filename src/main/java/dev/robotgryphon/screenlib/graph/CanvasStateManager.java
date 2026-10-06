package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.NodeState;
import dev.robotgryphon.screenlib.graph.ports.Port;
import dev.robotgryphon.screenlib.graph.ports.PortSide;
import dev.robotgryphon.screenlib.graph.ports.PropertyInputPort;
import dev.robotgryphon.screenlib.graph.properties.NodeProperty;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import dev.robotgryphon.screenlib.client.ui.widget.Connection;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public final class CanvasStateManager {
    private CanvasStateManager() {}

    public static CanvasState toState(Canvas canvas) {
        List<NodeWidget> widgets = canvas.nodes();
        List<NodeState> nodeStates = new ArrayList<>(widgets.size());
        Map<Node, Integer> indexOf = new IdentityHashMap<>(widgets.size());
        for (int i = 0; i < widgets.size(); i++) {
            NodeWidget widget = widgets.get(i);
            Node node = widget.node();
            indexOf.put(node, i);
            nodeStates.add(new NodeState(
                    canvas.definitionHolder(node), canvas.x(node), canvas.y(node),
                    encodePropertyValues(node),
                    Optional.ofNullable(widget.tintColor())));
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
                    tgtIdx, tgtPortIdx, Optional.of(portLocalName(c.targetPort())),
                    c.color()));
        }

        return new CanvasState(nodeStates, connectionStates);
    }

    private static String portLocalName(Port port) {
        return switch (port) {
            case PropertyInputPort p -> p.propertyName();
            default -> port.title().getString();
        };
    }

    public static void loadState(Canvas canvas, CanvasState state,
                                 BiFunction<Canvas, NodeState, NodeWidget> nodeBuilder) {
        canvas.clear();

        for (NodeState ns : state.nodes()) {
            NodeWidget widget = nodeBuilder.apply(canvas, ns);
            applyPropertyValues(widget.node(), ns.propertyValues());
            ns.tintColor().ifPresent(c -> widget.setTintColor(c));
            canvas.addNode(widget);
        }

        List<NodeWidget> rebuilt = canvas.nodes();
        for (ConnectionState cs : state.connections()) {
            if (!isValidIndex(cs.sourceNodeIndex(), rebuilt.size())) continue;
            if (!isValidIndex(cs.targetNodeIndex(), rebuilt.size())) continue;
            Node srcNode = rebuilt.get(cs.sourceNodeIndex()).node();
            Node tgtNode = rebuilt.get(cs.targetNodeIndex()).node();

            Port srcPort = resolvePort(srcNode, cs.sourcePortName(), cs.sourcePortIndex());
            Port tgtPort = resolvePort(tgtNode, cs.targetPortName(), cs.targetPortIndex());
            if (srcPort == null || tgtPort == null) continue;

            if (srcPort.side() != PortSide.RIGHT) continue;
            if (tgtPort.side() != PortSide.LEFT) continue;
            if (srcPort.type().value() != tgtPort.type().value()) continue;

            canvas.addConnection(new Connection(srcNode, srcPort, tgtNode, tgtPort, cs.color()));
        }
    }

    private static @Nullable Port resolvePort(Node node,
                                              Optional<String> savedName,
                                              int savedIndex) {
        if (savedName.isPresent()) {
            String name = savedName.get();
            for (Port port : node.portManager().ports()) {
                String portName = portLocalName(port);
                if (name.equals(portName)) {
                    return port;
                }
            }
        }
        if (isValidIndex(savedIndex, node.portManager().ports().size())) {
            return node.portManager().ports().get(savedIndex);
        }
        return null;
    }

    private static Map<String, Dynamic<?>> encodePropertyValues(Node node) {
        Map<String, Dynamic<?>> encoded = new HashMap<>();
        for (NodeProperty<?> prop : node.propertyManager().properties()) {
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
        Map<String, NodeProperty<?>> byName = new HashMap<>();
        for (NodeProperty<?> prop : node.propertyManager().properties()) {
            byName.put(prop.name(), prop);
        }
        for (Map.Entry<String, Dynamic<?>> entry : stored.entrySet()) {
            NodeProperty<?> prop = byName.get(entry.getKey());
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
