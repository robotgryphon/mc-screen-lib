package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.graph.Node;
import dev.robotgryphon.screenlib.graph.NodeConnection;
import dev.robotgryphon.screenlib.graph.Port;

/**
 * A finalized connection between two ports.
 *
 * <p>Source and target reference {@link Node}s rather than widgets — port
 * geometry comes from the model, so the bezier endpoints are computed
 * straight off the node without going through the UI layer.
 */
public record Connection(Node source, Port sourcePort,
                         Node target, Port targetPort,
                         int color) {
    public NodeConnection toNodeConnection() {
        return NodeConnection.rightToLeft(
                this.source.portAttachment(this.sourcePort),
                this.target.portAttachment(this.targetPort),
                this.color);
    }
}
