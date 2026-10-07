package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.ports.Port;

public record Connection(Node source, Port sourcePort,
                         Node target, Port targetPort) {
}
