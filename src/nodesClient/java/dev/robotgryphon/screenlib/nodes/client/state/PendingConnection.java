package dev.robotgryphon.screenlib.nodes.client.state;

import dev.robotgryphon.screenlib.nodes.common.Node;
import dev.robotgryphon.screenlib.nodes.common.ports.PropertyOutputPort;

public record PendingConnection(Node source, PropertyOutputPort sourcePort) {
}
