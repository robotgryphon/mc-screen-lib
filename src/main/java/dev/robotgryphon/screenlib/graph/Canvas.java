package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.client.ui.widget.Connection;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import net.minecraft.util.CommonColors;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2d;
import org.joml.Vector2dc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

    public static final float MIN_ZOOM = 0.25f;
    public static final float MAX_ZOOM = 4.0f;
    /**
     * Multiplicative factor per scroll tick.
     */
    public static final float ZOOM_STEP = 1.1f;

    private final List<NodeWidget> nodes = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();

    /**
     * Translation in screen pixels, applied before {@link #zoom}.
     */
    private float panX = 0f;
    private float panY = 0f;
    /**
     * Uniform scale factor; 1.0 = no zoom.
     */
    private float zoom = 1f;

    /**
     * Add a node to the canvas. Returns the node for chaining.
     */
    public NodeWidget addNode(NodeWidget node) {
        this.nodes.add(node);
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

    /**
     * Reset the view so the canvas origin is at the canvas widget origin and zoom is 1.
     */
    public void resetView() {
        this.panX = 0f;
        this.panY = 0f;
        this.zoom = 1f;
    }

    public void transformPose(Matrix3x2fStack pose) {
        pose.translate(this.panX, this.panY);
        pose.scale(this.zoom, this.zoom);
    }

    public void pan(float x, float y) {
        this.panX += x;
        this.panY += y;
    }

    public float zoom() {
        return this.zoom;
    }

    public void zoom(float zoom) {
        this.zoom += zoom;
    }

    /**
     * Set the zoom level to {@code newZoom} (clamped to the configured range)
     * while keeping the canvas point currently under {@code (screenX, screenY)}
     * stationary in screen space. Compensates {@link #panX}/{@link #panY} so a
     * scroll-to-zoom over the cursor doesn't drift the view.
     *
     * <p>Derivation: the canvas point under the cursor before the zoom change
     * is {@code (screenX - oldPan) / oldZoom}. We want the same canvas point
     * to remain at {@code (screenX, screenY)} afterward, i.e.,
     * {@code newPan = screenX - canvasPoint * newZoom = screenX - (screenX - oldPan) * (newZoom / oldZoom)}.
     */
    public void zoomAround(float newZoom, double screenX, double screenY) {
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        if (newZoom == this.zoom) {
            return;
        }
        float ratio = newZoom / this.zoom;
        this.panX = (float) (screenX - (screenX - this.panX) * ratio);
        this.panY = (float) (screenY - (screenY - this.panY) * ratio);
        this.zoom = newZoom;
    }

    // -- Coordinate conversion ----------------------------------------------

    public Vector2dc screenToCanvas(double screenX, double screenY) {
        return screenToCanvas(new Vector2d(screenX, screenY));
    }

    public Vector2dc screenToCanvas(Vector2dc screen) {
        final var joml = new Vector2d(screen)
                .sub(this.panX, this.panY)
                .div(this.zoom);

        final var inlined = new Vector2d((screen.x() - this.panX) / this.zoom, (screen.y() - this.panY) / this.zoom);

        return inlined;
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
     * connection was added; {@code false} if it was rejected because:
     * the sides aren't a valid output→input pair, the port types differ,
     * or a connection between this exact pair already exists.
     */
    public boolean connect(Port from, Port to, int color) {
        if (from.side() != PortSide.RIGHT || to.side() != PortSide.LEFT) {
            return false;
        }
        // PropertyType instances are registered singletons, so identity on the
        // resolved value is the right notion of "same type" here.
        if (from.type().value() != to.type().value()) {
            return false;
        }
        for (Connection c : this.connections) {
            if (c.sourcePort().equals(from) && c.targetPort().equals(to)) {
                return false;
            }
        }
        this.connections.add(new Connection(from.node(), from, to.node(), to, color));
        return true;
    }

    public void removeConnection(Connection connection) {
        this.connections.remove(connection);
    }

    // -- Persistence --------------------------------------------------------

    /**
     * Captures a serializable snapshot of the canvas's current nodes and
     * connections. The snapshot is a plain {@link CanvasState} record —
     * encode it with {@link CanvasState#CODEC} against whatever storage
     * format you want (JSON via {@code JsonOps}, NBT via {@code NbtOps},
     * etc.).
     *
     * <p>Pan / zoom and any other view-only state are intentionally
     * excluded; only the document — nodes + wires — is captured.
     */
    public CanvasState toState() {
        List<NodeState> nodeStates = new ArrayList<>(this.nodes.size());
        // IdentityHashMap so node ref-equality (not value-equality) drives
        // the index lookup — two structurally identical nodes are still
        // distinct entries on the canvas.
        Map<Node, Integer> indexOf = new IdentityHashMap<>(this.nodes.size());
        for (int i = 0; i < this.nodes.size(); i++) {
            Node node = this.nodes.get(i).node();
            indexOf.put(node, i);
            nodeStates.add(new NodeState(node.definitionHolder(), node.x(), node.y()));
        }

        List<ConnectionState> connectionStates = new ArrayList<>(this.connections.size());
        for (Connection c : this.connections) {
            Integer srcIdx = indexOf.get(c.source());
            Integer tgtIdx = indexOf.get(c.target());
            if (srcIdx == null || tgtIdx == null) {
                // Connection refers to a node not in our list — shouldn't
                // happen with the normal API but skip rather than serialize
                // a dangling reference.
                continue;
            }
            int srcPortIdx = c.source().ports().indexOf(c.sourcePort());
            int tgtPortIdx = c.target().ports().indexOf(c.targetPort());
            if (srcPortIdx < 0 || tgtPortIdx < 0) {
                continue;
            }
            connectionStates.add(new ConnectionState(
                    srcIdx, srcPortIdx, tgtIdx, tgtPortIdx, c.color()));
        }

        return new CanvasState(nodeStates, connectionStates);
    }

    /**
     * Restores a {@link CanvasState}. The current nodes and connections
     * are dropped, then:
     * <ol>
     *   <li>Each {@link NodeState} is turned into a {@link NodeWidget} via
     *       {@code nodeBuilder} (so the host can decide title text,
     *       custom subclasses, etc.) and added in order — preserving the
     *       indices that {@link ConnectionState} entries reference.</li>
     *   <li>Each {@link ConnectionState} is resolved against the rebuilt
     *       node list. References that fall outside the rebuilt graph (a
     *       missing node, a port index that doesn't exist on the
     *       definition anymore) are dropped silently rather than crashing
     *       the load.</li>
     * </ol>
     */
    public void loadState(CanvasState state, Function<NodeState, NodeWidget> nodeBuilder) {
        this.nodes.clear();
        this.connections.clear();

        for (NodeState ns : state.nodes()) {
            this.nodes.add(nodeBuilder.apply(ns));
        }

        for (ConnectionState cs : state.connections()) {
            if (!isValidIndex(cs.sourceNodeIndex(), this.nodes.size())) continue;
            if (!isValidIndex(cs.targetNodeIndex(), this.nodes.size())) continue;
            Node srcNode = this.nodes.get(cs.sourceNodeIndex()).node();
            Node tgtNode = this.nodes.get(cs.targetNodeIndex()).node();
            if (!isValidIndex(cs.sourcePortIndex(), srcNode.ports().size())) continue;
            if (!isValidIndex(cs.targetPortIndex(), tgtNode.ports().size())) continue;
            Port srcPort = srcNode.ports().get(cs.sourcePortIndex());
            Port tgtPort = tgtNode.ports().get(cs.targetPortIndex());
            this.connections.add(new Connection(srcNode, srcPort, tgtNode, tgtPort, cs.color()));
        }
    }

    private static boolean isValidIndex(int idx, int size) {
        return idx >= 0 && idx < size;
    }
}
