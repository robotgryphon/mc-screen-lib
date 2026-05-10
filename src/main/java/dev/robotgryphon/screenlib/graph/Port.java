package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.types.PropertyType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

/**
 * A single connection port on a {@link Node}. Multiple ports can share a
 * side; the node distributes them evenly along that edge.
 *
 * <p>The port carries a back-reference to its owning {@code Node} (rather
 * than to a UI widget) so anything reading the graph — connections,
 * hit-testing, future serialization — can compute geometry directly from
 * the model without going through a view.
 *
 * @param node  the node this port belongs to
 * @param side  which edge of the node this port lives on
 * @param title small label rendered inside the node body next to the port
 * @param type  the data type the port carries — drives the port's color
 *              and is later used to gate which connections are valid
 */
public record Port(Node node, PortSide side, Component title, Holder<PropertyType<?>> type) {

    public static Port of(Node node, PortSide side, Component title, Holder<PropertyType<?>> type) {
        return new Port(node, side, title, type);
    }

    public static Port of(Node node, PortSide side, String title, Holder<PropertyType<?>> type) {
        return of(node, side, Component.literal(title), type);
    }

    /** Convenience: the color this port should render with. */
    public int color() {
        return this.type.value().color();
    }
}
