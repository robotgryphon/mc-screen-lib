package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.client.ui.render.pip.BezierCurveRenderState;
import dev.robotgryphon.screenlib.geometry.BezierCurve;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.NodeConnection;
import dev.robotgryphon.screenlib.graph.PortSide;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.joml.Vector2dc;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CanvasWidget extends AbstractWidget {

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

    public CanvasWidget(Canvas canvas, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.canvas = canvas;
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

        final var clicked = canvas.screenToCanvas(event.x(), event.y());
        final var canvasEvent = convertMouseButtonEvent(event);

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

                // If we have a match, create the connection
                port.ifPresent(n -> canvas.connect(p.sourcePort(), n, CommonColors.BLUE));
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

        extractConnections(graphics, canvasMouse);

        // GO up a level so nodes will always render above connections
        graphics.nextStratum();
        for (NodeWidget node : canvas.nodes()) {
            node.extractRenderState(graphics, (int) canvasMouse.x(), (int) canvasMouse.y(), partialTick);
        }

        graphics.pose().popMatrix();
    }

    private void extractConnections(GuiGraphicsExtractor graphics, Vector2dc mouseCanvas) {
        List<BezierCurve> curves = new ArrayList<>();
        canvas.connections().forEach(c -> {
            NodeConnection curve = c.toNodeConnection();
            final var bezCurve = curve.asCurve(graphics);
            curves.add(bezCurve);
        });

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

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // Nodes carry their own narration; the canvas itself is silent.
    }
}
