package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.client.ui.render.pip.BezierCurveRenderState;
import dev.robotgryphon.screenlib.client.ui.render.pip.NodeBackgroundRenderState;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import dev.robotgryphon.screenlib.geometry.BezierCurve;
import dev.robotgryphon.screenlib.geometry.CurveIndicator;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.CanvasViewport;
import dev.robotgryphon.screenlib.graph.Node;
import dev.robotgryphon.screenlib.graph.PortSide;
import dev.robotgryphon.screenlib.types.PropertyDefinition;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.Holder;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import org.joml.Vector2dc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CanvasWidget extends AbstractWidget {

    /** Distance in canvas pixels within which the mouse counts as "on" a connection. */
    private static final double CONNECTION_HIT_RADIUS = 4.0;
    /** Radius (canvas pixels) of the close-indicator circle at a hovered connection's midpoint. */
    private static final float DELETE_BUTTON_RADIUS = 5f;

    /**
     * Alpha multiplier applied to anything whose type doesn't match the
     * active drag's type. Low enough that the dimmed elements clearly
     * read as "out of play" while still hinting at the underlying graph,
     * so the user keeps their spatial orientation while dragging.
     */
    private static final float MISMATCHED_TYPE_ALPHA = 0.3f;

    public final Canvas canvas;

    /**
     * Per-widget viewport — the pan/zoom/coord-translation state for how
     * <em>this widget</em> is currently showing the canvas. Owned by the
     * widget (not the canvas) because it's a "how is this view configured"
     * concern, not a "what's in the document" concern; if two widgets ever
     * end up showing the same canvas, each gets its own viewport.
     */
    private final CanvasViewport viewport = new CanvasViewport();

    private @Nullable PendingConnection pending;

    /**
     * The node currently capturing body-drag events.
     */
    private @Nullable NodeWidget focusedNode;

    /**
     * True while the user is left-dragging on empty canvas to pan.
     */
    private boolean panning;

    /**
     * Maps each static node to its current layer index — the lowest
     * non-negative integer such that, walking the canvas's nodes list
     * in z-order, no earlier node already on that layer overlaps with
     * this one in canvas space.
     *
     * <p>Nodes on the same layer are guaranteed not to overlap, so the
     * node-background shader's "last writer wins" behavior gives correct
     * pixels within a single batch. Nodes on <em>different</em> layers
     * are submitted as separate PiP batches, each in its own stratum,
     * so normal blending composites them with the already-painted layer
     * underneath (instead of letting the AA edge of the top node bleed
     * the canvas background through where the underlying node should
     * have been visible).
     *
     * <p>The map is reused across frames — {@link #computeStaticLayerIndices}
     * clears and refills it. fastutil avoids the per-frame boxing /
     * allocation cost of a plain {@code Map<NodeWidget, Integer>}.
     */
    private final Object2IntOpenHashMap<NodeWidget> layerIndices = new Object2IntOpenHashMap<>();

    /** Active right-click context menu, if any. Drawn in screen space above the canvas. */
    private @Nullable ContextMenu activeMenu;

    /**
     * Callback invoked when the user picks "Add Node" from the context menu.
     * Receives the canvas-space position of the original right-click so the
     * caller can spawn the new node where the user clicked.
     */
    private final Consumer<Vector2dc> onAddNodeRequested;

    public CanvasWidget(Canvas canvas, int x, int y, int width, int height,
                        Consumer<Vector2dc> onAddNodeRequested) {
        super(x, y, width, height, Component.empty());
        this.canvas = canvas;
        this.onAddNodeRequested = onAddNodeRequested;
    }

    /**
     * The viewport this widget is currently viewing the canvas through.
     * Pan / zoom / screen↔canvas conversions all hang off of it.
     */
    public CanvasViewport viewport() {
        return this.viewport;
    }

    /** Allow right-click (button 1) so it can open the context menu. */
    @Override
    public boolean isValidClickButton(MouseButtonInfo info) {
        return info.button() == 0 || info.button() == 1;
    }

    // -- Mouse input --------------------------------------------------------

    private MouseButtonEvent convertMouseButtonEvent(MouseButtonEvent screen) {
        final var canvas = this.viewport.screenToCanvas(screen.x(), screen.y());
        return new MouseButtonEvent(canvas.x(), canvas.y(), screen.buttonInfo());
    }



    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.isActive() || !this.isValidClickButton(event.buttonInfo())) {
            return false;
        }

        // While a context menu is open, every click is consumed by it: clicks
        // inside trigger an item, clicks outside dismiss. Either way the menu
        // closes and the canvas underneath does not see this click.
        if (this.activeMenu != null) {
            ContextMenu menu = this.activeMenu;
            this.activeMenu = null;
            menu.mouseClicked(event.x(), event.y());
            return true;
        }

        // Same pattern for an open property popup — inside-click selects
        // an option (the popup's onSelect clears the property's focus on
        // the owning node); outside-click just dismisses. Popup state
        // lives on the node, so we ask each node widget whether it has
        // an open popup. There's at most one in practice (the modal-
        // close branch below ensures opening one closes any others), but
        // iterating is robust to that invariant slipping.
        NodeWidget popupNode = this.findPopupNode();
        if (popupNode != null) {
            if (popupNode.handleFocusedPropertyPopupClick(this.viewport, event.x(), event.y())) {
                // Option selected — the popup's onSelect already cleared
                // the node's focused-property marker.
                return true;
            }
            // Click missed the popup. Clear focus on the node that owned
            // it and consume the event so the click doesn't fall through
            // to start a node drag or pan.
            popupNode.clearFocusedProperty();
            return true;
        }

        // Right-click → open a context menu in screen space at the cursor.
        // The menu's items depend on whether the click landed on a node:
        //   - on a node body → node-scoped menu (Remove Node, …)
        //   - elsewhere      → canvas-scoped menu (Add Node, …)
        // findNodesNear already excludes hits that land on the node's
        // ports, so right-clicking a port falls through to the canvas
        // menu — matching the user's likely intent ("the wire-side, not
        // the node-side").
        if (event.button() == 1) {
            final Vector2dc canvasPos = this.viewport.screenToCanvas(event.x(), event.y());
            final NodeWidget hitNode = canvas.findNodesNear(canvasPos).findFirst().orElse(null);

            List<ContextMenu.Item> items;
            if (hitNode != null) {
                items = List.of(new ContextMenu.Item(
                        Component.literal("Remove Node"),
                        () -> {
                            canvas.removeNode(hitNode);
                            // Defensive: if this node was somehow still
                            // the drag target (shouldn't be, since the
                            // right-click that opened the menu cleared
                            // any in-progress drag), drop the reference
                            // so the next frame doesn't try to forward
                            // events to a removed widget.
                            if (this.focusedNode == hitNode) {
                                this.focusedNode = null;
                            }
                        }));
            } else {
                items = List.of(new ContextMenu.Item(
                        Component.literal("Add Node"),
                        () -> this.onAddNodeRequested.accept(canvasPos)));
            }

            this.activeMenu = new ContextMenu((int) event.x(), (int) event.y(), items);
            return true;
        }

        final var clicked = this.viewport.screenToCanvas(event.x(), event.y());
        final var canvasEvent = convertMouseButtonEvent(event);

        // 0. Delete-button hit → remove the connection. Only the bezier-hovered
        //    connection's button is visible, so we only honor a click within
        //    that connection's midpoint button.
        if (event.button() == 0) {
            Connection hovered = findConnectionUnderCursor(clicked);
            if (hovered != null && isInDeleteButton(hovered, clicked)) {
                canvas.removeConnection(hovered);
                return true;
            }
        }

        // 1. Topmost-first port hit → start a pending connection.
        var port = canvas.findPortsNear(clicked)
                .filter(p -> p.side().equals(PortSide.RIGHT))
                .findFirst()
                .orElse(null);
        if (port != null) {
            this.pending = new PendingConnection(port.node(), port);
            // Tell the model what type is being dragged so renderers
            // elsewhere on the canvas can dim mismatched targets.
            this.canvas.setActiveDragType(port.type());
            this.focusedNode = null;
            this.panning = false;
            return true;
        }

        // 2. Topmost-first body hit → forward click to the node so its
        //    onClick fires (which sets `dragging = true` and captures the
        //    grab offset). Without this forwarding the node never enters
        //    drag state and onDrag short-circuits.
        var node = canvas.findNodesNear(clicked).findFirst().orElse(null);
        if (node != null && node.mouseClicked(canvasEvent, doubleClick)) {
            this.focusedNode = node;
            this.panning = false;
            return true;
        }

        // 3. Empty area → pan.
        this.panning = true;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        final var asCanvasEvent = convertMouseButtonEvent(event);

        if (this.panning) {
            // dx/dy come in screen-pixel space — pan is a screen-space translation.
            this.viewport.pan((float) dx, (float) dy);
            return true;
        }
        if (this.pending != null) {
            // The in-flight curve uses the live mouseX/mouseY passed into
            // extractRenderState, so we just claim the drag here.
            return true;
        }
        if (this.focusedNode != null) {
            // Scale dx/dy into canvas units in case the node uses them.
            float zoom = this.viewport.zoom();
            return this.focusedNode.mouseDragged(asCanvasEvent, dx / zoom, dy / zoom);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        final var canvasSpace = this.viewport.screenToCanvas(event.x(), event.y());
        final var asCanvasEvent = convertMouseButtonEvent(event);

        if (this.panning) {
            this.panning = false;
            return true;
        }

        if (this.pending != null) {
            PendingConnection p = this.pending;
            this.pending = null;
            // Always clear the drag-type hint, regardless of whether the
            // release lands on a valid target — leaving it set would dim
            // the canvas indefinitely.
            this.canvas.setActiveDragType(null);
            if (event.button() == 0) {
                // Find ports that are not the starting one and not on the same node
                final var port = canvas.findPortsNear(canvasSpace)
                        .filter(p2 -> !p2.equals(p.sourcePort()))
                        .filter(p2 -> !p2.node().equals(p.source()))
                        .findFirst();

                // If we have a match, create the connection. The line takes the
                // source port's type color so wires read as "this is a Position"
                // / "this is an Item Handler" at a glance, matching the diamond.
                port.ifPresent(n -> canvas.connect(p.sourcePort(), n, p.sourcePort().color()));
            }
            return true;
        }

        if (this.focusedNode != null) {
            NodeWidget f = this.focusedNode;
            this.focusedNode = null;
            return f.mouseReleased(asCanvasEvent);
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return false;
        }
        float factor = scrollY > 0 ? CanvasViewport.ZOOM_STEP : 1f / CanvasViewport.ZOOM_STEP;
        // Viewport owns the pan/zoom fields, so it also owns the math that
        // keeps the cursor's canvas point stationary across a zoom change.
        this.viewport.zoomAround(this.viewport.zoom() * factor, mouseX, mouseY);
        return true;
    }

    // -- Render -------------------------------------------------------------

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushMatrix();
        this.viewport.transformPose(graphics.pose());

        // Hover / port-detection inside nodes operates in canvas space.
        final var canvasMouse = this.viewport.screenToCanvas(mouseX, mouseY);

        // Resolve the hovered connection once: the curve render attaches a
        // close-indicator to its bezier so both the indicator circle and the
        // X glyph inside it ride in the shader pass — no separate Java-side
        // render needed for the glyph.
        final Connection hovered = this.pending == null
                ? findConnectionUnderCursor(canvasMouse)
                : null;

        extractConnections(graphics, canvasMouse, hovered);

        // Partition nodes into "static" (everything anchored to the
        // canvas) and "dragged" (whatever the user is currently moving).
        // Each set gets its own stratum so a dragged node's content
        // can't end up underneath a static node's background — the bug
        // when both backgrounds shared one PiP texture and both contents
        // shared one CPU stratum.
        var allNodes = canvas.nodes();
        List<NodeWidget> staticNodes = new ArrayList<>(allNodes.size());
        List<NodeWidget> draggedNodes = new ArrayList<>();
        for (NodeWidget node : allNodes) {
            if (node.isDragging()) {
                draggedNodes.add(node);
            } else {
                staticNodes.add(node);
            }
        }

        // Static nodes — grouped by layer index so overlapping nodes end
        // up in separate batches. Each layer gets its own stratum;
        // within a layer, nodes are guaranteed non-overlapping (by
        // construction of the layer assignment), so a single batched
        // PiP submission with shader-level last-writer-wins composites
        // them correctly. Between layers, normal blending takes over —
        // a later layer's nodes sit on top of the framebuffer pixels
        // already painted by earlier layers.
        int staticMaxLayer = computeStaticLayerIndices(staticNodes);
        List<List<NodeWidget>> staticByLayer = new ArrayList<>(staticMaxLayer + 1);
        for (int i = 0; i <= staticMaxLayer; i++) {
            staticByLayer.add(new ArrayList<>());
        }
        for (NodeWidget node : staticNodes) {
            staticByLayer.get(this.layerIndices.getInt(node)).add(node);
        }
        for (List<NodeWidget> layer : staticByLayer) {
            if (layer.isEmpty()) continue;
            graphics.nextStratum();
            extractNodeBackgrounds(graphics, layer, false);
            for (NodeWidget node : layer) {
                node.extractRenderState(graphics, (int) canvasMouse.x(), (int) canvasMouse.y(), partialTick);
            }
        }

        // Dragged nodes, if any. Same shape (background then content)
        // but on its own stratum above every static layer so the whole
        // "node being moved" composite floats cleanly above the static
        // graph. Drop shadow is enabled for this batch as the drag-time
        // visual cue.
        if (!draggedNodes.isEmpty()) {
            graphics.nextStratum();
            extractNodeBackgrounds(graphics, draggedNodes, true);
            for (NodeWidget node : draggedNodes) {
                node.extractRenderState(graphics, (int) canvasMouse.x(), (int) canvasMouse.y(), partialTick);
            }
        }

        graphics.pose().popMatrix();

        // Context menu is screen-anchored — drawn outside the canvas pose so
        // it doesn't pan or zoom with the underlying graph.
        if (this.activeMenu != null) {
            graphics.nextStratum();
            this.activeMenu.render(graphics, mouseX, mouseY);
        }

        // Same screen-anchored stratum for an open property popup. Each
        // nextStratum() call only ever raises the floor for subsequent
        // submissions, so it's safe to call it again here — even if the
        // context menu also bumped the stratum a moment ago. The popup's
        // state lives on the node, so we ask the node widget to render
        // its own popup if it has one.
        NodeWidget popupNode = this.findPopupNode();
        if (popupNode != null) {
            graphics.nextStratum();
            popupNode.renderFocusedPropertyPopup(graphics, this.viewport, mouseX, mouseY);
        }
    }

    /**
     * Linear scan to find the node widget (if any) whose underlying node
     * has a focused property with an open popup. Returns the first hit;
     * the click and render paths both expect at most one popup open at a
     * time and the {@code mouseClicked} modal-close branch enforces
     * that. O(N) in node count, fine for typical canvas sizes.
     */
    private @Nullable NodeWidget findPopupNode() {
        for (NodeWidget node : canvas.nodes()) {
            if (node.hasFocusedPropertyPopup()) {
                return node;
            }
        }
        return null;
    }

    private void extractConnections(GuiGraphicsExtractor graphics, Vector2dc mouseCanvas,
                                    @Nullable Connection hovered) {
        List<BezierCurve> curves = new ArrayList<>();
        var dragType = this.canvas.activeDragType();
        for (Connection c : canvas.connections()) {
            Vector2fc start = c.source().portAttachment(c.sourcePort());
            Vector2fc end = c.target().portAttachment(c.targetPort());
            // Only the hovered connection gets a close indicator. Every other
            // connection passes null and the shader renders just the curve.
            // The indicator's hovered flag is set when the cursor is also
            // within the close-button hit area — same test used for click —
            // so the shader can react to cursor proximity, not just the
            // broader "this curve is under the mouse" condition.
            CurveIndicator indicator = (c == hovered)
                    ? new CurveIndicator(midpoint(start, end), DELETE_BUTTON_RADIUS,
                            isInDeleteButton(c, mouseCanvas))
                    : null;
            // While a drag is in flight, dim connections that aren't the
            // same type as what's being dragged. The shader uses
            // vertexColor.a as a per-fragment scaler, so reducing the
            // color's alpha here makes the whole curve render translucent
            // without needing a shader-side change.
            int wireColor = c.color();
            if (dragType != null && c.sourcePort().type().value() != dragType.value()) {
                wireColor = scaleAlpha(wireColor, MISMATCHED_TYPE_ALPHA);
            }
            curves.add(BezierCurve.from(graphics, start, end, wireColor, indicator));
        }

        if (this.pending != null) {
            final var start = this.pending.source().portAttachment(this.pending.sourcePort());
            final var end = new Vector2f(mouseCanvas);

            final var pendingCurve = BezierCurve.from(graphics, start, end, Canvas.IN_FLIGHT_COLOR);
            curves.add(pendingCurve);
        }

        for (var curve : curves) {
            final var state = BezierCurveRenderState.from(graphics, curve);
            graphics.submitPictureInPictureRenderState(state);
        }
    }

    /**
     * Builds and submits a single batched PiP state covering the given
     * node widgets. Texture bounds are the screen-space union of every
     * node's rectangle under the current viewport; per-entry bounds are
     * recorded relative to the texture origin in scaled (window) pixels
     * so the fragment shader can use {@code gl_FragCoord} directly.
     *
     * <p>Skipped when the list is empty — a zero-area PiP would be
     * pointless and the framework treats it as a degenerate texture.
     *
     * <p>Callers split nodes into "static" and "dragged" subsets and
     * submit them in separate strata so a dragged node's content doesn't
     * end up sandwiched between another node's background and content.
     * The {@code dropShadow} parameter forwards the same flag onto each
     * entry — used to draw a soft offset shadow underneath the dragged
     * batch so the moving node visually lifts off the static layer.
     */
    private void extractNodeBackgrounds(GuiGraphicsExtractor graphics,
                                        List<NodeWidget> nodes,
                                        boolean dropShadow) {
        if (nodes.isEmpty()) {
            return;
        }

        // First pass: union the screen-space rects so we know the
        // texture dimensions and origin. The texture must cover every
        // node entirely so the shader has room to paint at the right
        // pixel position; anything outside the bounds gets discarded
        // by the framework.
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (NodeWidget widget : nodes) {
            ScreenRectangle b = widget.screenBounds(this.viewport);
            minX = Math.min(minX, b.left());
            minY = Math.min(minY, b.top());
            maxX = Math.max(maxX, b.right());
            maxY = Math.max(maxY, b.bottom());
        }
        // Pad by 1 screen pixel on each side so the shader's AA falloff
        // band isn't clipped at the texture edge. When the batch carries
        // a drop shadow, extend the padding by roughly the shadow's
        // extent so the shadow has room to fade out beyond each node
        // (offset down-and-right, blur on all sides).
        int basePad = 1;
        int shadowPad = dropShadow
                ? Math.max(2, Math.round(NodeWidget.NODE_CORNER_RADIUS * this.viewport.zoom() * 2f))
                : 0;
        int leftPad = basePad + shadowPad;
        int topPad = basePad + shadowPad;
        int rightPad = basePad + shadowPad;
        int bottomPad = basePad + shadowPad;
        minX -= leftPad; minY -= topPad;
        maxX += rightPad; maxY += bottomPad;
        ScreenRectangle textureBounds = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY);

        // Shared shader parameters in scaled-pixel space. The corner
        // radius is the same canvas-pixel constant the CPU path used to
        // use, here lifted through the viewport's zoom so the on-screen
        // radius scales with the rest of the node.
        float guiScale = (float) net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        float cornerRadiusScaled = NodeWidget.NODE_CORNER_RADIUS * this.viewport.zoom() * guiScale;
        // 1 scaled pixel of AA falloff — enough to soften the edge
        // without bleeding into neighboring rows.
        float featherScaled = 1f;
        // 1 canvas pixel of border thickness, then through the same
        // zoom * guiScale conversion as the radius.
        float borderThicknessScaled = 1f * this.viewport.zoom() * guiScale;

        // Second pass: build per-node entries relative to the texture origin.
        List<NodeBackgroundUniform.Entry> entries = new ArrayList<>(nodes.size());
        for (NodeWidget widget : nodes) {
            entries.add(widget.buildBackgroundEntry(this.viewport, minX, minY, guiScale, dropShadow));
        }

        var state = new NodeBackgroundRenderState(textureBounds, entries,
                cornerRadiusScaled, featherScaled, borderThicknessScaled);
        graphics.submitPictureInPictureRenderState(state);
    }

    private static Vector2f midpoint(Vector2fc a, Vector2fc b) {
        return new Vector2f((a.x() + b.x()) / 2f, (a.y() + b.y()) / 2f);
    }

    // -- Layer assignment ---------------------------------------------------

    /**
     * Walks {@code staticNodes} in z-order (the order they appear in
     * {@link Canvas#nodes()}, where later = visually on top) and assigns
     * each node the lowest layer index such that no earlier-in-z node
     * already on that layer overlaps with it in canvas space.
     *
     * <p>Result is written into {@link #layerIndices}; returned value is
     * the highest layer index actually used so the caller knows how many
     * sub-strata to allocate. The map is cleared first so stale entries
     * from a previous frame (e.g., a node that's since been removed)
     * don't leak through.
     *
     * <p>Greedy and O(N²) in worst case, fine for typical canvas sizes
     * (the shader cap is 64 nodes per batch and that's already generous
     * for an edit session). When no nodes overlap — the common case —
     * the inner conflict loop short-circuits on the first iteration and
     * the cost stays close to O(N).
     *
     * <p>Recomputed every frame rather than lazily-invalidated because
     * any operation that could change the answer (drop, add, remove,
     * canvas reload) already triggers a render pass; doing the work
     * here keeps the bookkeeping in one place and avoids a class of
     * "forgot to invalidate" bugs.
     */
    private int computeStaticLayerIndices(List<NodeWidget> staticNodes) {
        this.layerIndices.clear();
        int maxLayer = 0;
        for (NodeWidget node : staticNodes) {
            int layer = 0;
            // Walk upward through layer indices until we find one with
            // no overlap conflict among the already-assigned earlier
            // nodes. The break-on-self pattern means we only ever
            // consult nodes that have been placed in `layerIndices`,
            // so getInt() returns a meaningful value (not the default).
            search:
            while (true) {
                for (NodeWidget other : staticNodes) {
                    if (other == node) break;
                    if (this.layerIndices.getInt(other) == layer && overlaps(node, other)) {
                        layer++;
                        continue search;
                    }
                }
                break;
            }
            this.layerIndices.put(node, layer);
            if (layer > maxLayer) maxLayer = layer;
        }
        return maxLayer;
    }

    /**
     * AABB overlap test in canvas-space pixels. Half-open intervals so
     * two nodes that share only an edge (e.g., one's right edge meeting
     * another's left edge) don't count as overlapping — they'd render
     * fine in the same batch.
     */
    private static boolean overlaps(NodeWidget a, NodeWidget b) {
        Node na = a.node();
        Node nb = b.node();
        return na.x() < nb.x() + nb.width()
                && nb.x() < na.x() + na.width()
                && na.y() < nb.y() + nb.height()
                && nb.y() < na.y() + na.height();
    }

    // -- Connection hit-testing + delete button -----------------------------

    /**
     * Endpoints of a connection in canvas space — the same two points the
     * bezier curve is drawn between. Returned as a 2-element array so callers
     * can use {@code [0]} for start and {@code [1]} for end without a record.
     */
    private static Vector2fc[] connectionEndpoints(Connection c) {
        return new Vector2fc[]{
                c.source().portAttachment(c.sourcePort()),
                c.target().portAttachment(c.targetPort())
        };
    }

    /** Canvas-space midpoint of a connection's bezier. For the symmetric
     *  cubic produced by {@code calculateRightToLeft}, t=0.5 reduces to the
     *  average of the two endpoints — no need to evaluate the curve. */
    private static Vector2f connectionMidpoint(Connection c) {
        Vector2fc[] e = connectionEndpoints(c);
        return new Vector2f((e[0].x() + e[1].x()) / 2f, (e[0].y() + e[1].y()) / 2f);
    }

    /**
     * Return the topmost connection whose bezier passes within
     * {@link #CONNECTION_HIT_RADIUS} of the given canvas-space point, or
     * {@code null}. Sampling the curve at 32 segments is plenty for the
     * smooth, low-curvature shapes used here.
     */
    private @Nullable Connection findConnectionUnderCursor(Vector2dc canvasMouse) {
        final int samples = 32;
        final double hitR2 = CONNECTION_HIT_RADIUS * CONNECTION_HIT_RADIUS;
        Connection best = null;
        double bestD2 = hitR2;
        // Iterate in reverse so a newer connection on top wins ties.
        java.util.List<Connection> all = canvas.connections();
        for (int idx = all.size() - 1; idx >= 0; idx--) {
            Connection c = all.get(idx);
            Vector2fc[] e = connectionEndpoints(c);
            float midX = (e[0].x() + e[1].x()) / 2f;
            float p0x = e[0].x(), p0y = e[0].y();
            float p1x = midX,    p1y = e[0].y();
            float p2x = midX,    p2y = e[1].y();
            float p3x = e[1].x(), p3y = e[1].y();
            for (int i = 0; i <= samples; i++) {
                float t = i / (float) samples;
                float u = 1f - t;
                float bx = u * u * u * p0x + 3 * u * u * t * p1x + 3 * u * t * t * p2x + t * t * t * p3x;
                float by = u * u * u * p0y + 3 * u * u * t * p1y + 3 * u * t * t * p2y + t * t * t * p3y;
                double dx = bx - canvasMouse.x();
                double dy = by - canvasMouse.y();
                double d2 = dx * dx + dy * dy;
                if (d2 < bestD2) {
                    bestD2 = d2;
                    best = c;
                }
            }
        }
        return best;
    }

    private static boolean isInDeleteButton(Connection c, Vector2dc canvasMouse) {
        Vector2f mid = connectionMidpoint(c);
        double dx = canvasMouse.x() - mid.x();
        double dy = canvasMouse.y() - mid.y();
        return dx * dx + dy * dy <= DELETE_BUTTON_RADIUS * DELETE_BUTTON_RADIUS;
    }

    /**
     * Multiply an ARGB color's alpha channel by {@code factor}, clamped
     * to [0, 255]. RGB channels are left untouched so the color reads as
     * "this is the same wire, just faded" rather than shifting hue.
     */
    private static int scaleAlpha(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int scaled = Math.max(0, Math.min(255, Math.round(a * factor)));
        return (scaled << 24) | (argb & 0x00FFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // Nodes carry their own narration; the canvas itself is silent.
    }
}
