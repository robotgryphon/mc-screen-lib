package com.example.examplemod.client.widget;

import com.example.examplemod.client.render.BezierCurveRenderer;
import com.example.examplemod.graph.NodeConnection;
import com.example.examplemod.graph.NodeConnection.NodeSide;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A canvas widget that hosts a graph of {@link DraggableWidget} nodes and the
 * bezier connections between their {@link Port}s. Add it to a {@link
 * net.minecraft.client.gui.screens.Screen} via {@code addRenderableWidget(...)}
 * and call {@link #addNode(DraggableWidget)} for each node — the canvas takes
 * over from there.
 *
 * <p>Interaction:
 * <ul>
 *   <li>Click a {@code RIGHT}-side port to start a connection that follows the
 *       cursor; release on any other port to finalize.</li>
 *   <li>Click and drag a node body to move the node.</li>
 *   <li>Click and drag the empty canvas area to <b>pan</b>.</li>
 *   <li>Scroll the mouse wheel to <b>zoom</b>, centered on the cursor.</li>
 * </ul>
 *
 * <p>Internally the canvas keeps a single source of truth — node positions are
 * authored in canvas space, and a translate+scale matrix is applied to
 * {@code graphics.pose()} at render time. Mouse events are converted from
 * screen space to canvas space before being forwarded to nodes.
 */
public class Canvas extends AbstractWidget {

    private static final int[] CONNECTION_PALETTE = {
            CommonColors.GREEN,
            CommonColors.RED,
            CommonColors.YELLOW,
            CommonColors.WHITE,
            CommonColors.LIGHT_GRAY,
    };
    private static final int IN_FLIGHT_COLOR = 0xFFFFD24A;

    private static final float MIN_ZOOM = 0.25f;
    private static final float MAX_ZOOM = 4.0f;
    /** Multiplicative factor per scroll tick. */
    private static final float ZOOM_STEP = 1.1f;

    private final List<DraggableWidget> nodes = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();

    private @Nullable PendingConnection pending;
    /** The node currently capturing body-drag events. */
    private @Nullable DraggableWidget focusedNode;
    /** True while the user is left-dragging on empty canvas to pan. */
    private boolean panning;

    /** Translation in screen pixels, applied before {@link #zoom}. */
    private float panX = 0f;
    private float panY = 0f;
    /** Uniform scale factor; 1.0 = no zoom. */
    private float zoom = 1f;

    public Canvas(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    /** Add a node to the canvas. Returns the node for chaining. */
    public DraggableWidget addNode(DraggableWidget node) {
        this.nodes.add(node);
        return node;
    }

    public List<DraggableWidget> nodes() {
        return Collections.unmodifiableList(this.nodes);
    }

    public List<Connection> connections() {
        return Collections.unmodifiableList(this.connections);
    }

    /** Remove all connections (e.g., for a "clear" action). */
    public void clearConnections() {
        this.connections.clear();
        this.pending = null;
    }

    public float zoom() {
        return this.zoom;
    }

    public float panX() {
        return this.panX;
    }

    public float panY() {
        return this.panY;
    }

    /** Reset the view so the canvas origin is at the canvas widget origin and zoom is 1. */
    public void resetView() {
        this.panX = 0f;
        this.panY = 0f;
        this.zoom = 1f;
    }

    // -- Coordinate conversion ----------------------------------------------

    private double screenToCanvasX(double sx) {
        return (sx - this.panX) / this.zoom;
    }

    private double screenToCanvasY(double sy) {
        return (sy - this.panY) / this.zoom;
    }

    private MouseButtonEvent toCanvasSpace(MouseButtonEvent event) {
        return new MouseButtonEvent(
                screenToCanvasX(event.x()),
                screenToCanvasY(event.y()),
                event.buttonInfo());
    }

    // -- Mouse input --------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.isActive() || !this.isValidClickButton(event.buttonInfo())) {
            return false;
        }

        MouseButtonEvent canvasEvent = toCanvasSpace(event);

        // 1. Topmost-first port hit → start a pending connection.
        for (int i = this.nodes.size() - 1; i >= 0; i--) {
            DraggableWidget node = this.nodes.get(i);
            Port port = node.portAt(canvasEvent.x(), canvasEvent.y());
            if (port != null && port.side() == NodeSide.RIGHT) {
                this.pending = new PendingConnection(node, port);
                this.focusedNode = null;
                this.panning = false;
                return true;
            }
        }

        // 2. Topmost-first body hit → forward click to the node for body drag.
        for (int i = this.nodes.size() - 1; i >= 0; i--) {
            DraggableWidget node = this.nodes.get(i);
            if (node.isMouseOver(canvasEvent.x(), canvasEvent.y()) && node.mouseClicked(canvasEvent, doubleClick)) {
                this.focusedNode = node;
                this.panning = false;
                return true;
            }
        }

        // 3. Empty area → pan.
        this.panning = true;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.panning) {
            // dx/dy come in screen-pixel space — pan is a screen-space translation.
            this.panX += (float) dx;
            this.panY += (float) dy;
            return true;
        }
        if (this.pending != null) {
            // The in-flight curve uses the live mouseX/mouseY passed into
            // extractRenderState, so we just claim the drag here.
            return true;
        }
        if (this.focusedNode != null) {
            MouseButtonEvent canvasEvent = toCanvasSpace(event);
            // Scale dx/dy into canvas units in case the node uses them.
            return this.focusedNode.mouseDragged(canvasEvent, dx / this.zoom, dy / this.zoom);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.panning) {
            this.panning = false;
            return true;
        }
        if (this.pending != null) {
            PendingConnection p = this.pending;
            this.pending = null;
            if (event.button() == 0) {
                MouseButtonEvent canvasEvent = toCanvasSpace(event);
                for (int i = this.nodes.size() - 1; i >= 0; i--) {
                    DraggableWidget node = this.nodes.get(i);
                    if (node == p.source) {
                        continue;
                    }
                    Port port = node.portAt(canvasEvent.x(), canvasEvent.y());
                    if (port != null) {
                        int color = CONNECTION_PALETTE[this.connections.size() % CONNECTION_PALETTE.length];
                        this.connections.add(new Connection(p.source, p.sourcePort, node, port, color));
                        break;
                    }
                }
            }
            return true;
        }
        if (this.focusedNode != null) {
            DraggableWidget f = this.focusedNode;
            this.focusedNode = null;
            return f.mouseReleased(toCanvasSpace(event));
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return false;
        }
        float oldZoom = this.zoom;
        float factor = scrollY > 0 ? ZOOM_STEP : 1f / ZOOM_STEP;
        float newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, oldZoom * factor));
        if (newZoom == oldZoom) {
            return true;
        }
        // Keep the canvas point under the cursor stationary in screen space.
        // newPan = mouse - (mouse - oldPan) * (newZoom / oldZoom)
        float ratio = newZoom / oldZoom;
        this.panX = (float) (mouseX - (mouseX - this.panX) * ratio);
        this.panY = (float) (mouseY - (mouseY - this.panY) * ratio);
        this.zoom = newZoom;
        return true;
    }

    // -- Render -------------------------------------------------------------

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.panX, this.panY);
        graphics.pose().scale(this.zoom, this.zoom);

        // Hover / port-detection inside nodes operates in canvas space.
        int canvasMouseX = (int) screenToCanvasX(mouseX);
        int canvasMouseY = (int) screenToCanvasY(mouseY);

        // Nodes first, in insertion order — later additions paint on top.
        for (DraggableWidget node : this.nodes) {
            node.extractRenderState(graphics, canvasMouseX, canvasMouseY, partialTick);
        }

        // Connections sit one stratum above the nodes so the curves are never
        // hidden behind a panel.
        graphics.nextStratum();
        for (Connection connection : this.connections) {
            NodeConnection curve = connection.toNodeConnection();
            BezierCurveRenderer.render(graphics, curve, curve.bounds());
        }

        if (this.pending != null) {
            Vector2fc start = this.pending.source.portAttachment(this.pending.sourcePort);
            Vector2fc end = new Vector2f((float) canvasMouseX, (float) canvasMouseY);
            NodeConnection curve = NodeConnection.rightToLeft(start, end, IN_FLIGHT_COLOR);
            BezierCurveRenderer.render(graphics, curve, curve.bounds());
        }

        graphics.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // Nodes carry their own narration; the canvas itself is silent.
    }

    // -- Records ------------------------------------------------------------

    /** A finalized connection between two ports. */
    public record Connection(DraggableWidget source, Port sourcePort,
                             DraggableWidget target, Port targetPort,
                             int color) {
        public NodeConnection toNodeConnection() {
            return NodeConnection.rightToLeft(
                    this.source.portAttachment(this.sourcePort),
                    this.target.portAttachment(this.targetPort),
                    this.color);
        }
    }

    /** State held during an in-flight port drag (between mouseClicked and mouseReleased). */
    private record PendingConnection(DraggableWidget source, Port sourcePort) {
    }
}
