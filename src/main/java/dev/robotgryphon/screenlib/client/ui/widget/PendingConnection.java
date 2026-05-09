package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.graph.Port;

/**
 * State held during an in-flight port drag (between mouseClicked and mouseReleased).
 */
public record PendingConnection(NodeWidget source, Port sourcePort) {
}
