package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.client.ui.widget.Connection;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.types.PropertyDefinition;
import net.minecraft.core.Holder;
import net.minecraft.util.CommonColors;
import org.joml.Vector2dc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class Canvas {

    private static final int[] CONNECTION_PALETTE = {
            CommonColors.GREEN,
            CommonColors.RED,
            CommonColors.YELLOW,
            CommonColors.WHITE,
            CommonColors.LIGHT_GRAY,
    };
    public static final int IN_FLIGHT_COLOR = 0xFFFFD24A;

    private final List<NodeWidget> nodes = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();

    /**
     * Set while the user is mid-drag from a port. Carries the source
     * port's type so any rendering pass can dim things of other types
     * out of the way — making the legal drop targets visually pop. Null
     * outside of a drag. Set/cleared by the canvas widget when the
     * pending connection comes and goes.
     *
     * <p>Lives on the model rather than the widget because the widgets
     * that consult it (the node widgets, drawing themselves) already
     * have a back-reference to {@link Canvas} but not to the widget
     * that owns the pending state.
     */
    private @Nullable Holder<PropertyDefinition<?>> activeDragType;

    /**
     * Add a node to the canvas. Returns the node for chaining. Also wires
     * the widget's back-reference to {@code this} so the widget can look
     * up its connected ports at render time (needed for property ports,
     * which only become visible once something is wired to them).
     */
    public NodeWidget addNode(NodeWidget node) {
        this.nodes.add(node);
        node.setCanvas(this);
        return node;
    }

    public List<NodeWidget> nodes() {
        return Collections.unmodifiableList(this.nodes);
    }

    public List<Connection> connections() {
        return Collections.unmodifiableList(this.connections);
    }

    /**
     * Remove all connections (e.g., for a "clear" action).
     */
    public void clearConnections() {
        this.connections.clear();
    }

    /** The type currently being dragged from, or {@code null} when no drag is in flight. */
    public @Nullable Holder<PropertyDefinition<?>> activeDragType() {
        return this.activeDragType;
    }

    /**
     * Sets the active drag type. Pass the source port's type when a
     * pending connection starts; pass {@code null} when it ends (release
     * or cancellation). Idempotent — repeated identical sets are a no-op.
     */
    public void setActiveDragType(@Nullable Holder<PropertyDefinition<?>> type) {
        this.activeDragType = type;
    }

    public Stream<Port> findPortsNear(Vector2dc point) {
        var s = Stream.<Port>builder();
        for (int i = this.nodes.size() - 1; i >= 0; i--) {
            NodeWidget node = this.nodes.get(i);
            Port port = node.portAt(point.x(), point.y());
            if (port != null)
                s.add(port);
        }

        return s.build();
    }

    public Stream<NodeWidget> findNodesNear(Vector2dc point) {
        var s = Stream.<NodeWidget>builder();
        for (int i = this.nodes.size() - 1; i >= 0; i--) {
            NodeWidget node = this.nodes.get(i);
            if (node.isMouseOver(point.x(), point.y())) {
                final var maybePort = node.portAt(point.x(), point.y());
                if(maybePort == null)
                    s.add(node);
            }
        }

        return s.build();
    }

    /**
     * Add a connection between two ports. Returns {@code true} if the
     * connection was added; {@code false} if it was rejected because
     * the sides aren't a valid output→input pair or the port types
     * differ.
     *
     * <p>Input ports accept exactly one inbound connection. If
     * {@code to} already has a wire targeting it, that wire is
     * dropped and the new one takes its place — the standard node
     * graph UX where redirecting an input is a single drag, not a
     * disconnect-then-reconnect dance. The replacement covers
     * {@code (from, to)} exactly matching an existing connection too
     * (that connection's removal and immediate re-add is a no-op
     * net change but still reports {@code true} so the caller knows
     * the wire is now live).
     */
    public boolean connect(Port from, Port to, int color) {
        if (from.side() != PortSide.RIGHT || to.side() != PortSide.LEFT) {
            return false;
        }
        // PropertyDefinition instances are registered singletons, so identity
        // on the resolved value is the right notion of "same type" here.
        if (from.type().value() != to.type().value()) {
            return false;
        }
        // Enforce single-inbound on the target input by dropping any
        // existing wire that targets it before the new wire lands.
        this.connections.removeIf(c -> c.targetPort().equals(to));
        this.connections.add(new Connection(from.node(), from, to.node(), to, color));
        return true;
    }

    public void removeConnection(Connection connection) {
        this.connections.remove(connection);
    }

    /**
     * Remove a node from the canvas, along with every connection that
     * referenced it. Also clears the widget's back-reference so a
     * subsequently rendered detached widget (e.g., during animation) no
     * longer reads connection state off this canvas.
     *
     * <p>No-op when the widget isn't on the canvas — callers don't have to
     * pre-check membership.
     */
    public void removeNode(NodeWidget widget) {
        if (!this.nodes.remove(widget)) {
            return;
        }
        Node target = widget.node();
        // Drop any wires that touched this node on either side. Using the
        // model-level Node identity (not the widget) so this works for
        // both source and target sides — Connection records hold Nodes,
        // not NodeWidgets.
        this.connections.removeIf(c -> c.source() == target || c.target() == target);
        widget.setCanvas(null);
    }

    // -- Persistence hooks --------------------------------------------------
    // Save / load themselves live in CanvasStateManager. The canvas only
    // exposes the minimum surface that manager needs to do its job:
    // wipe the live state and append a pre-built connection without going
    // through connect()'s validation (which would re-reject anything the
    // saved data had legitimately produced).

    /**
     * Empties the canvas — drops every node and every connection — and
     * clears each node widget's canvas back-reference so a subsequently-
     * rendered detached widget no longer reads connection state off
     * {@code this}. Used by {@link CanvasStateManager#loadState} to wipe
     * before rebuild.
     */
    public void clear() {
        for (NodeWidget widget : this.nodes) {
            widget.setCanvas(null);
        }
        this.nodes.clear();
        this.connections.clear();
    }

    /**
     * Appends {@code connection} to the canvas without the validation
     * {@link #connect} runs. Used when restoring a saved canvas — the
     * persisted connection has already passed through {@code connect}
     * once, so re-validating against the rebuilt graph (where the same
     * ports may have shifted indices) would falsely reject legitimate
     * wires.
     */
    public void addConnection(Connection connection) {
        this.connections.add(connection);
    }
}
