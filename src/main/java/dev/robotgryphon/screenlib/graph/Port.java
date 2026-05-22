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
 * {@code propertyName}). A "property port" is the input-side handle that
 * exists for every declared property on the node: a single LEFT-side port
 * that lets a wire override the property's local value with whatever
 * value flows in. Properties don't expose a right-side "output" port —
 * values flow OUT of a node only through regular output ports — so the
 * property port count is exactly one per property, on the left.
 *
 * <p>{@code linkedPropertyName} is the converse bridge: a regular output
 * port (right side, non-property) can declare that the value it carries
 * downstream is the current value of one of the same node's properties.
 * The widget layer reads through this link when computing what a wire
 * from that output reports to its target. Inputs leave it null. Property
 * ports leave it null too — they are themselves the property's handle,
 * there's nothing further to link.
 *
 * @param node                  the node this port belongs to
 * @param side                  which edge of the node this port lives on
 * @param title                 small label rendered inside the node body next to the port
 * @param type                  the data type the port carries — drives the port's color
 *                              and is later used to gate which connections are valid
 * @param propertyName          the name of the property this port is bound to, or
 *                              {@code null} for a regular (always-visible) port
 * @param linkedPropertyName    for a regular output port that relays a property's
 *                              value, the name of that property; {@code null} on every
 *                              other port (inputs, property ports, and outputs that
 *                              don't shadow a property)
 */
public record Port(Node node, PortSide side, Component title, Holder<PropertyDefinition<?>> type,
                   @Nullable String propertyName,
                   @Nullable String linkedPropertyName) {

    /** Regular-port constructor — no property binding, no link to a property. */
    public Port(Node node, PortSide side, Component title, Holder<PropertyDefinition<?>> type) {
        this(node, side, title, type, null, null);
    }

    public static Port of(Node node, PortSide side, Component title, Holder<PropertyDefinition<?>> type) {
        return new Port(node, side, title, type, null, null);
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
        return new Port(node, side, Component.literal(propertyName), type, propertyName, null);
    }

    /** {@code true} when this port is the input handle for a property. */
    public boolean isProperty() {
        return this.propertyName != null;
    }

    /** Convenience: the color this port should render with. */
    public int color() {
        return this.type.value().color();
    }
}
