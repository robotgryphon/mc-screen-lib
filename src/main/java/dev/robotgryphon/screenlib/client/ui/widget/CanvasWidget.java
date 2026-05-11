package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.client.ui.render.pip.BezierCurveRenderState;
import dev.robotgryphon.screenlib.geometry.BezierCurve;
import dev.robotgryphon.screenlib.geometry.CurveIndicator;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.PortSide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    /** Color of the "x" glyph drawn on top of the indicator circle. */
    private static final int DELETE_BUTTON_GLYPH_COLOR = 0xFFFFFFFF;

    public final Canvas canvas;

    private @Nullable PendingConnection pending;

    /**
     * The node currently capturing body-drag events.
     */
    private @Nullable NodeWidget focusedNode;

    /**
     * True while the user is left-dragging on empty canvas to pan.
     */
    private boolean panning;

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

    /** Allow right-click (button 1) so it can open the context menu. */
    @Override
    public boolean isValidClickButton(MouseButtonInfo info) {
        return info.button() == 0 || info.button() == 1;
    }

    // -- Mouse input --------------------------------------------------------

    private MouseButtonEvent convertMouseButtonEvent(MouseButtonEvent screen) {
        final var canvas = this.canvas.screenToCanvas(screen.x(), screen.y());
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

        // Right-click on the canvas → open context menu in screen space at the
        // cursor. The canvas-space position of the click is captured so the
        // "Add Node" callback can place the new node where the user clicked.
        if (event.button() == 1) {
            final Vector2dc canvasPos = canvas.screenToCanvas(event.x(), event.y());
            this.activeMenu = new ContextMenu(
                    (int) event.x(), (int) event.y(),
                    List.of(new ContextMenu.Item(
                            Component.literal("Add Node"),
                            () -> this.onAddNodeRequested.accept(canvasPos))));
            return true;
        }

        final var clicked = canvas.screenToCanvas(event.x(), event.y());
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
            canvas.pan((float) dx, (float) dy);
            return true;
        }
        if (this.pending != null) {
            // The in-flight curve uses the live mouseX/mouseY passed into
            // extractRenderState, so we just claim the drag here.
            return true;
        }
        if (this.focusedNode != null) {
            // Scale dx/dy into canvas units in case the node uses them.
            return this.focusedNode.mouseDragged(asCanvasEvent, dx / canvas.zoom(), dy / canvas.zoom());
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        final var canvasSpace = this.canvas.screenToCanvas(event.x(), event.y());
        final var asCanvasEvent = convertMouseButtonEvent(event);

        if (this.panning) {
            this.panning = false;
            return true;
        }

        if (this.pending != null) {
            PendingConnection p = this.pending;
            this.pending = null;
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
        float factor = scrollY > 0 ? Canvas.ZOOM_STEP : 1f / Canvas.ZOOM_STEP;
        // Canvas owns the pan/zoom fields, so it also owns the math that keeps
        // the cursor's canvas point stationary across a zoom change.
        canvas.zoomAround(canvas.zoom() * factor, mouseX, mouseY);
        return true;
    }

    // -- Render -------------------------------------------------------------

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushMatrix();
        canvas.transformPose(graphics.pose());

        // Hover / port-detection inside nodes operates in canvas space.
        final var canvasMouse = canvas.screenToCanvas(mouseX, mouseY);

        // Resolve the hovered connection once: the curve render attaches a
        // close-indicator to its bezier (so the indicator rides inside the
        // shader pass), and the screen-space "x" glyph drawn later sits on
        // top of the nodes so the affordance stays visible.
        final Connection hovered = this.pending == null
                ? findConnectionUnderCursor(canvasMouse)
                : null;

        extractConnections(graphics, canvasMouse, hovered);

        // GO up a level so nodes will always render above connections
        graphics.nextStratum();
        for (NodeWidget node : canvas.nodes()) {
            node.extractRenderState(graphics, (int) canvasMouse.x(), (int) canvasMouse.y(), partialTick);
        }

        // The shader-rendered indicator circle is part of the connection PiP
        // and thus sits behind the nodes. Draw just the "x" glyph above the
        // nodes so it stays visible and the affordance remains discoverable
        // even when a connection's midpoint passes under a node's edge.
        if (hovered != null) {
            graphics.nextStratum();
            renderDeleteGlyph(graphics, hovered);
        }

        graphics.pose().popMatrix();

        // Context menu is screen-anchored — drawn outside the canvas pose so
        // it doesn't pan or zoom with the underlying graph.
        if (this.activeMenu != null) {
            graphics.nextStratum();
            this.activeMenu.render(graphics, mouseX, mouseY);
        }
    }

    private void extractConnections(GuiGraphicsExtractor graphics, Vector2dc mouseCanvas,
                                    @Nullable Connection hovered) {
        List<BezierCurve> curves = new ArrayList<>();
        for (Connection c : canvas.connections()) {
            Vector2fc start = c.source().portAttachment(c.sourcePort());
            Vector2fc end = c.target().portAttachment(c.targetPort());
            // Only the hovered connection gets a close indicator. Every other
            // connection passes null and the shader renders just the curve.
            CurveIndicator indicator = (c == hovered)
                    ? new CurveIndicator(midpoint(start, end), DELETE_BUTTON_RADIUS)
                    : null;
            curves.add(BezierCurve.from(graphics, start, end, c.color(), indicator));
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

    private static Vector2f midpoint(Vector2fc a, Vector2fc b) {
        return new Vector2f((a.x() + b.x()) / 2f, (a.y() + b.y()) / 2f);
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
     * Renders just the "x" affordance on top of the curve's
     * shader-rendered indicator circle. The circle itself is now drawn by
     * the bezier fragment shader (see {@link CurveIndicator} and the
     * {@code indicator} uniform on {@code BezierCurveUniform}), so this
     * method's only job is the text glyph.
     */
    private static void renderDeleteGlyph(GuiGraphicsExtractor graphics, Connection c) {
        Vector2f mid = connectionMidpoint(c);
        int cx = Math.round(mid.x());
        int cy = Math.round(mid.y());

        // "x" centered. Lowercase: visually denser at this size and avoids
        // falling through to the multiplication-sign glyph in the font.
        Font font = Minecraft.getInstance().font;
        Component glyph = Component.literal("x");
        int gw = font.width(glyph);
        graphics.text(font, glyph,
                cx - gw / 2 + 1,
                cy - font.lineHeight / 2 + 1,
                DELETE_BUTTON_GLYPH_COLOR, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // Nodes carry their own narration; the canvas itself is silent.
    }
}
