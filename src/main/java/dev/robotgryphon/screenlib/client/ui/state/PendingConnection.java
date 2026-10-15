package dev.robotgryphon.screenlib.client.ui.state;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.ports.Port;

public record PendingConnection(Node source, Port sourcePort) {
}
