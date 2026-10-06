package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.ports.Port;

public record Connection(Node source, Port sourcePort,
                         Node target, Port targetPort,
                         int color) {
}
