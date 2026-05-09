package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.graph.NodeConnection;
import dev.robotgryphon.screenlib.graph.Port;

/**
 * A finalized connection between two ports.
 */
public record Connection(NodeWidget source, Port sourcePort,
                         NodeWidget target, Port targetPort,
                         int color) {
    public NodeConnection toNodeConnection() {
        return NodeConnection.rightToLeft(
                this.source.portAttachment(this.sourcePort),
                this.target.portAttachment(this.targetPort),
                this.color);
    }
}
