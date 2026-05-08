package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.client.ui.widget.Connection;
import dev.robotgryphon.screenlib.client.ui.widget.Node;
import net.minecraft.util.CommonColors;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2d;
import org.joml.Vector2dc;

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

    public static final float MIN_ZOOM = 0.25f;
    public static final float MAX_ZOOM = 4.0f;
    /**
     * Multiplicative factor per scroll tick.
     */
    public static final float ZOOM_STEP = 1.1f;

    private final List<Node> nodes = new ArrayList<>();
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
    public Node addNode(Node node) {
        this.nodes.add(node);
        return node;
    }

    public List<Node> nodes() {
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
            Node node = this.nodes.get(i);
            Port port = node.portAt(point.x(), point.y());
            if (port != null)
                s.add(port);
        }

        return s.build();
    }

    public Stream<Node> findNodesNear(Vector2dc point) {
        var s = Stream.<Node>builder();
        for (int i = this.nodes.size() - 1; i >= 0; i--) {
            Node node = this.nodes.get(i);
            if (node.isMouseOver(point.x(), point.y())) {
                final var maybePort = node.portAt(point.x(), point.y());
                if(maybePort == null)
                    s.add(node);
            }
        }

        return s.build();
    }

    public void connect(Port from, Port to, int color) {
        this.connections.add(new Connection(from.node(), from, to.node(), to, color));
    }
}
