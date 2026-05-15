package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.types.PropertyDefinition;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * A single connection port on a {@link Node}. Multiple ports can share a
 * side; the node distributes them evenly along that edge.
 *
 * <p>The port carries a back-reference to its owning {@code Node} (rather
 * than to a UI widget) so anything reading the graph — connections,
 * hit-testing, future serialization — can compute geometry directly from
 * the model without going through a view.
 *
 * <p>A port may optionally be bound to a property name (see
 * {@code propertyName}). These "property ports" exist for every declared
 * property on the node — one LEFT (input-side) and one RIGHT (output-side)
 * — and let the user expose a property's value as either an incoming
 * connection point (override the local property value) or an outgoing
 * connection point (consume the property's value downstream). They differ
 * from regular ports in two ways:
 *
 * <ul>
 *   <li>They anchor to the property's row inside the node body, not to
 *       the side's port band.</li>
 *   <li>Their visible state is conditional — a property port shows only
 *       while the user is hovering its row or when something is actually
 *       connected to it. This is purely a rendering concern; the port
 *       itself is always a real {@code Port} for hit-testing and graph
 *       semantics.</li>
 * </ul>
 *
 * @param node          the node this port belongs to
 * @param side          which edge of the node this port lives on
 * @param title         small label rendered inside the node body next to the port
 * @param type          the data type the port carries — drives the port's color
 *                      and is later used to gate which connections are valid
 * @param propertyName  the name of the property this port is bound to, or
 *                      {@code null} for a regular (always-visible) port
 */
public record Port(Node node, PortSide side, Component title, Holder<PropertyDefinition<?>> type,
                   @Nullable String propertyName) {

    /** Regular-port constructor — the property name slot is left empty. */
    public Port(Node node, PortSide side, Component title, Holder<PropertyDefinition<?>> type) {
        this(node, side, title, type, null);
    }

    public static Port of(Node node, PortSide side, Component title, Holder<PropertyDefinition<?>> type) {
        return new Port(node, side, title, type, null);
    }

    public static Port of(Node node, PortSide side, String title, Holder<PropertyDefinition<?>> type) {
        return of(node, side, Component.literal(title), type);
    }

    /**
     * Builds a port that's bound to one of the node's properties. The
     * label uses the property name verbatim — when the port renders, the
     * label is suppressed anyway (the property row already shows it), but
     * keeping it here preserves the invariant that every port has a
     * non-null title.
     */
    public static Port property(Node node, PortSide side, String propertyName,
                                Holder<PropertyDefinition<?>> type) {
        return new Port(node, side, Component.literal(propertyName), type, propertyName);
    }

    /** {@code true} when this port is the input/output handle for a property. */
    public boolean isProperty() {
        return this.propertyName != null;
    }

    /** Convenience: the color this port should render with. */
    public int color() {
        return this.type.value().color();
    }
}
