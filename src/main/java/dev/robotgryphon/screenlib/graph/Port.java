package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.types.PropertyType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

/**
 * A single connection port on a {@link NodeWidget}. Multiple ports can
 * share a side; the widget distributes them evenly along that edge.
 *
 * @param node  the node this port belongs to
 * @param side  which edge of the widget this port lives on
 * @param title small label rendered inside the widget body next to the port
 * @param type  the data type the port carries — drives the port's color
 *              and is later used to gate which connections are valid
 */
public record Port(NodeWidget node, PortSide side, Component title, Holder<PropertyType<?>> type) {

    public static Port of(NodeWidget node, PortSide side, Component title, Holder<PropertyType<?>> type) {
        return new Port(node, side, title, type);
    }

    public static Port of(NodeWidget node, PortSide side, String title, Holder<PropertyType<?>> type) {
        return of(node, side, Component.literal(title), type);
    }

    /** Convenience: the color this port should render with. */
    public int color() {
        return this.type.value().color();
    }
}
