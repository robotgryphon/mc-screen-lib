package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.graph.ConnectionState;
import dev.robotgryphon.screenlib.graph.evaluation.CanvasEvaluator;
import dev.robotgryphon.screenlib.graph.nodes.NodeState;
import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class HostedNodes {
    private HostedNodes() {}

    public static CanvasState reconcile(CanvasState state, List<HostedNode> hosted, RegistryAccess registries) {
        var definitions = registries.lookupOrThrow(NodeDefinition.REGISTRY_KEY);
        Map<String, Holder<NodeDefinition>> valid = new LinkedHashMap<>();
        Map<String, HostedNode> byId = new LinkedHashMap<>();
        for (HostedNode node : hosted) {
            var definition = definitions.get(node.type());
            if (definition.isEmpty()) {
                ScreenLib.LOGGER.error("Hosted node '{}' uses unknown node type {}", node.id(), node.type().identifier());
                continue;
            }
            if (!definition.get().value().outputs().isEmpty()) {
                ScreenLib.LOGGER.error("Hosted node '{}' uses {}, which has outputs; hosted nodes must be terminating",
                        node.id(), node.type().identifier());
                continue;
            }
            valid.put(node.id(), definition.get());
            byId.put(node.id(), node);
        }

        List<NodeState> nodes = new ArrayList<>(state.nodes().size() + valid.size());
        int[] remap = new int[state.nodes().size()];
        Set<String> present = new HashSet<>();
        for (int i = 0; i < state.nodes().size(); i++) {
            NodeState node = state.nodes().get(i);
            if (node.host().isPresent()) {
                String id = node.host().get();
                Holder<NodeDefinition> expected = valid.get(id);
                if (expected == null || !expected.is(node.definition()) || !present.add(id)) {
                    remap[i] = -1;
                    continue;
                }
            }
            remap[i] = nodes.size();
            nodes.add(node);
        }

        for (var entry : valid.entrySet()) {
            if (!present.contains(entry.getKey())) {
                HostedNode node = byId.get(entry.getKey());
                nodes.add(new NodeState(entry.getValue(), node.x(), node.y(), Map.of(), Optional.empty(),
                        Optional.of(node.id())));
            }
        }

        List<ConnectionState> connections = new ArrayList<>(state.connections().size());
        for (ConnectionState connection : state.connections()) {
            int source = remapIndex(remap, connection.sourceNodeIndex());
            int target = remapIndex(remap, connection.targetNodeIndex());
            if (source < 0 || target < 0) {
                continue;
            }
            connections.add(new ConnectionState(source, connection.sourcePortIndex(), connection.sourcePortName(),
                    target, connection.targetPortIndex(), connection.targetPortName()));
        }

        return new CanvasState(nodes, connections, state.viewport());
    }

    private static int remapIndex(int[] remap, int index) {
        return index >= 0 && index < remap.length ? remap[index] : -1;
    }

    public static void publish(CanvasSource source) {
        List<HostedNode> hosted = source.hostedNodes();
        var level = source.level();
        var server = level.getServer();
        if (hosted.isEmpty() || server == null) {
            return;
        }
        CanvasState state = reconcile(source.load(), hosted, level.registryAccess());
        Map<String, HostedInputs> inputs = CanvasEvaluator.open(state, level.registryAccess(), server).hostedInputs();
        for (HostedNode node : hosted) {
            node.publish(Optional.ofNullable(inputs.get(node.id())));
        }
    }
}
