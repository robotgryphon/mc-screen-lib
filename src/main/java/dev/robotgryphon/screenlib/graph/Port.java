package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import net.minecraft.network.chat.Component;

/**
 * A single connection port on a {@link NodeWidget}. Multiple ports can
 * share a side; the widget distributes them evenly along that edge.
 *
 * @param side  which edge of the widget this port lives on
 * @param title small label rendered inside the widget body next to the port
 */
public record Port(NodeWidget node, PortSide side, Component title) {

    public static Port of(NodeWidget node, PortSide side, Component title) {
        return new Port(node, side, title);
    }

    public static Port of(NodeWidget node, PortSide side, String title) {
        return of(node, side, Component.literal(title));
    }
}
