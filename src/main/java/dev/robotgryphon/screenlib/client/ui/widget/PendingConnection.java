package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.graph.Node;
import dev.robotgryphon.screenlib.graph.Port;

/**
 * State held during an in-flight port drag (between mouseClicked and mouseReleased).
 * Tracks the source as a {@link Node} so the in-flight curve can be drawn
 * straight from the model — the widget layer doesn't need to be involved.
 */
public record PendingConnection(Node source, Port sourcePort) {
}
