package dev.robotgryphon.screenlib.nodes.common;

import dev.robotgryphon.screenlib.nodes.common.ports.PropertyInputPort;
import dev.robotgryphon.screenlib.nodes.common.ports.PropertyOutputPort;

public record Connection(Node source, PropertyOutputPort sourcePort,
                         Node target, PropertyInputPort targetPort) {
}
