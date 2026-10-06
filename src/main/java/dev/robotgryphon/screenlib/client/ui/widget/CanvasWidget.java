package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.client.NodeRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import dev.robotgryphon.screenlib.client.ui.render.pip.BezierCurveRenderState;
import dev.robotgryphon.screenlib.client.ui.render.pip.NodeBackgroundRenderState;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import dev.robotgryphon.screenlib.client.ui.widget.property.NumericPropertyEditor;
import dev.robotgryphon.screenlib.geometry.BezierCurve;
import dev.robotgryphon.screenlib.geometry.CurveIndicator;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.ports.Port;
import dev.robotgryphon.screenlib.graph.CanvasViewport;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.ports.PortSide;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import org.joml.Vector2dc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CanvasWidget extends AbstractWidget {
    private static final double CONNECTION_HIT_RADIUS = 4.0;
    private static final float DELETE_BUTTON_RADIUS = 5f;

    private static final float MISMATCHED_TYPE_ALPHA = 0.3f;

    public final Canvas canvas;

    private final CanvasViewport viewport = new CanvasViewport();

    private @Nullable PendingConnection pending;

    private @Nullable NodeWidget focusedNode;

    private boolean panning;

    private final Object2IntOpenHashMap<NodeWidget> layerIndices = new Object2IntOpenHashMap<>();

    private @Nullable ContextMenu activeMenu;

    private final Consumer<Vector2dc> onAddNodeRequested;

    public CanvasWidget(Canvas canvas, int x, int y, int width, int height,
                        Consumer<Vector2dc> onAddNodeRequested) {
        super(x, y, width, height, Component.empty());
        this.canvas = canvas;
        this.onAddNodeRequested = onAddNodeRequested;
    }

    public CanvasViewport viewport() {
        return this.viewport;
    }

    @Override
    public boolean isValidClickButton(MouseButtonInfo info) {
        return info.button() == InputConstants.MOUSE_BUTTON_LEFT || info.button() == InputConstants.MOUSE_BUTTON_RIGHT;
    }

    private MouseButtonEvent convertMouseButtonEvent(MouseButtonEvent screen) {
        final var canvas = this.viewport.screenToCanvas(screen.x(), screen.y());
        return new MouseButtonEvent(canvas.x(), canvas.y(), screen.buttonInfo());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.isActive() || !this.isValidClickButton(event.buttonInfo())) {
            return false;
        }

        if (this.activeMenu != null) {
            ContextMenu menu = this.activeMenu;
            this.activeMenu = null;
            menu.mouseClicked(event.x(), event.y());
            return true;
        }

        NodeWidget popupNode = this.findPopupNode();
        if (popupNode != null) {
            if (popupNode.properties().handleFocusedPropertyPopupClick(this.viewport, event.x(), event.y())) {
                return true;
            }
            popupNode.properties().clearFocusedProperty();
            return true;
        }

        NumericPropertyEditor editingEditor = this.findEditingEditor();
        if (editingEditor != null) {
            final var canvasClickEvent = convertMouseButtonEvent(event);
            if (editingEditor.handleClickWhileEditing(canvasClickEvent, doubleClick)) {
                return true;
            }
        }

        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            final Vector2dc canvasPos = this.viewport.screenToCanvas(event.x(), event.y());
            final NodeWidget hitNode = canvas.findNodesNear(canvasPos).findFirst().orElse(null);

            List<ContextMenu.Item> items;
            if (hitNode != null) {
                items = List.of(new ContextMenu.Item(
                        Component.literal("Remove Node"),
                        () -> {
                            canvas.removeNode(hitNode);
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

        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            Connection hovered = findConnectionUnderCursor(clicked);
            if (hovered != null && isInDeleteButton(hovered, clicked)) {
                canvas.removeConnection(hovered);
                return true;
            }
        }

        var port = canvas.findPortsNear(clicked)
                .filter(p -> p.side().equals(PortSide.RIGHT))
                .findFirst()
                .orElse(null);
        if (port != null) {
            this.pending = new PendingConnection(port.node(), port);
            this.canvas.setActiveDragType(port.type());
            this.focusedNode = null;
            this.panning = false;
            return true;
        }

        var node = canvas.findNodesNear(clicked).findFirst().orElse(null);
        if (node != null && node.mouseClicked(canvasEvent, doubleClick)) {
            this.focusedNode = node;
            this.panning = false;
            return true;
        }

        this.panning = true;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        final var asCanvasEvent = convertMouseButtonEvent(event);

        if (this.panning) {
            this.viewport.pan((float) dx, (float) dy);
            return true;
        }
        if (this.pending != null) {
            return true;
        }
        if (this.focusedNode != null) {
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
            this.canvas.setActiveDragType(null);
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                final var port = canvas.findPortsNear(canvasSpace)
                        .filter(p2 -> !p2.equals(p.sourcePort()))
                        .filter(p2 -> !p2.node().equals(p.source()))
                        .findFirst();

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
        this.viewport.zoomAround(this.viewport.zoom() * factor, mouseX, mouseY);
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        NumericPropertyEditor editor = this.findEditingEditor();
        if (editor != null) {
            return editor.keyPressed(event);
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        NumericPropertyEditor editor = this.findEditingEditor();
        if (editor != null) {
            return editor.charTyped(event);
        }

        return super.charTyped(event);
    }

    private @Nullable NumericPropertyEditor findEditingEditor() {
        for (NodeWidget node : this.canvas.nodes()) {
            NumericPropertyEditor editor = node.properties().activeNumericEditor();
            if (editor != null) {
                return editor;
            }
        }
        return null;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushMatrix();
        this.viewport.transformPose(graphics.pose());

        final var canvasMouse = this.viewport.screenToCanvas(mouseX, mouseY);

        final Connection hovered = this.pending == null
                ? findConnectionUnderCursor(canvasMouse)
                : null;

        extractConnections(graphics, canvasMouse, hovered);

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
            extractPortCircles(graphics, layer, canvasMouse);
        }

        if (!draggedNodes.isEmpty()) {
            graphics.nextStratum();
            extractNodeBackgrounds(graphics, draggedNodes, true);
            for (NodeWidget node : draggedNodes) {
                node.extractRenderState(graphics, (int) canvasMouse.x(), (int) canvasMouse.y(), partialTick);
            }
            extractPortCircles(graphics, draggedNodes, canvasMouse);
        }

        graphics.pose().popMatrix();

        if (this.activeMenu != null) {
            graphics.nextStratum();
            this.activeMenu.render(graphics, mouseX, mouseY);
        }

        NodeWidget popupNode = this.findPopupNode();
        if (popupNode != null) {
            graphics.nextStratum();
            popupNode.properties().renderFocusedPropertyPopup(graphics, this.viewport, mouseX, mouseY);
        }
    }

    private @Nullable NodeWidget findPopupNode() {
        for (NodeWidget node : canvas.nodes()) {
            if (node.properties().hasFocusedPropertyPopup()) {
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
            Vector2fc start = this.portAttachment(c.source(), c.sourcePort());
            Vector2fc end = this.portAttachment(c.target(), c.targetPort());
            CurveIndicator indicator = (c == hovered)
                    ? new CurveIndicator(midpoint(start, end), DELETE_BUTTON_RADIUS,
                            isInDeleteButton(c, mouseCanvas))
                    : null;
            int wireColor = c.color();
            if (dragType != null && c.sourcePort().type().value() != dragType.value()) {
                wireColor = scaleAlpha(wireColor, MISMATCHED_TYPE_ALPHA);
            }
            curves.add(BezierCurve.from(graphics, start, end, wireColor, indicator));
        }

        if (this.pending != null) {
            final var start = this.portAttachment(this.pending.source(), this.pending.sourcePort());
            final var end = new Vector2f(mouseCanvas);

            final var pendingCurve = BezierCurve.from(graphics, start, end, Canvas.IN_FLIGHT_COLOR);
            curves.add(pendingCurve);
        }

        for (var curve : curves) {
            final var state = BezierCurveRenderState.from(graphics, curve);
            graphics.submitPictureInPictureRenderState(state);
        }
    }

    private void extractNodeBackgrounds(GuiGraphicsExtractor graphics,
                                        List<NodeWidget> nodes,
                                        boolean dropShadow) {
        if (nodes.isEmpty()) {
            return;
        }

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
        int basePad = 1;
        int shadowPad = dropShadow
                ? Math.max(2, Math.round(NodeWidget.NODE_CORNER_RADIUS * this.viewport.zoom() * 2f))
                : 0;
        boolean anyGlow = false;
        for (NodeWidget widget : nodes) {
            if (widget.hasVisibleGlow()) {
                anyGlow = true;
                break;
            }
        }
        int glowPad = anyGlow
                ? Math.max(2, Math.round(NodeWidget.NODE_CORNER_RADIUS * this.viewport.zoom()))
                : 0;
        int leftPad = basePad + Math.max(shadowPad, glowPad);
        int topPad = basePad + Math.max(shadowPad, glowPad);
        int rightPad = basePad + Math.max(shadowPad, glowPad);
        int bottomPad = basePad + Math.max(shadowPad, glowPad);
        minX -= leftPad; minY -= topPad;
        maxX += rightPad; maxY += bottomPad;
        ScreenRectangle textureBounds = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY);

        float guiScale = (float) net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        float cornerRadiusScaled = NodeWidget.NODE_CORNER_RADIUS * this.viewport.zoom() * guiScale;
        float featherScaled = 1f;
        float borderThicknessScaled = Math.max(1f,
                0.5f * this.viewport.zoom() * guiScale);

        List<NodeBackgroundUniform.Entry> entries = new ArrayList<>(nodes.size());
        for (NodeWidget widget : nodes) {
            entries.add(widget.buildBackgroundEntry(this.viewport, minX, minY, guiScale, dropShadow));
        }

        var state = new NodeBackgroundRenderState(textureBounds, entries,
                cornerRadiusScaled, featherScaled, borderThicknessScaled);
        graphics.submitPictureInPictureRenderState(state);
    }

    private void extractPortCircles(GuiGraphicsExtractor graphics,
                                    List<NodeWidget> nodes,
                                    Vector2dc canvasMouse) {
        if (nodes.isEmpty()) return;

        float guiScale = (float) net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        float radiusScaled = NodeRenderer.PORT_RADIUS * this.viewport.zoom() * guiScale;
        float hoverRadiusScaled = radiusScaled * RenderedPort.HOVER_RADIUS_FACTOR;
        float featherScaled = 1f;
        float borderThicknessScaled = Math.max(1f, radiusScaled * 0.2f);

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
        int pad = (int) Math.ceil(hoverRadiusScaled / guiScale) + 1;
        minX -= pad; minY -= pad;
        maxX += pad; maxY += pad;
        ScreenRectangle textureBounds = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY);

        List<NodeBackgroundUniform.Entry> entries = new ArrayList<>();
        for (NodeWidget widget : nodes) {
            widget.ports().appendPortEntries(this.viewport, minX, minY, guiScale,
                    canvasMouse.x(), canvasMouse.y(), entries);
        }
        if (entries.isEmpty()) return;

        int max = NodeBackgroundUniform.Storage.MAX_NODES;
        for (int from = 0; from < entries.size(); from += max) {
            int to = Math.min(from + max, entries.size());
            List<NodeBackgroundUniform.Entry> chunk = entries.subList(from, to);
            var state = new NodeBackgroundRenderState(textureBounds, List.copyOf(chunk),
                    radiusScaled, featherScaled, borderThicknessScaled);
            graphics.submitPictureInPictureRenderState(state);
        }
    }

    private static Vector2f midpoint(Vector2fc a, Vector2fc b) {
        return new Vector2f((a.x() + b.x()) / 2f, (a.y() + b.y()) / 2f);
    }

    private int computeStaticLayerIndices(List<NodeWidget> staticNodes) {
        this.layerIndices.clear();
        int maxLayer = 0;
        for (NodeWidget node : staticNodes) {
            int layer = 0;
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

    private static boolean overlaps(NodeWidget a, NodeWidget b) {
        return a.getX() < b.getX() + b.getWidth()
                && b.getX() < a.getX() + a.getWidth()
                && a.getY() < b.getY() + b.getHeight()
                && b.getY() < a.getY() + a.getHeight();
    }
    private Vector2fc portAttachment(Node node, Port port) {
        return this.canvas.widgetFor(node).ports().portAttachment(port);
    }

    private Vector2fc[] connectionEndpoints(Connection c) {
        return new Vector2fc[]{
                this.portAttachment(c.source(), c.sourcePort()),
                this.portAttachment(c.target(), c.targetPort())
        };
    }

    private Vector2f connectionMidpoint(Connection c) {
        Vector2fc[] e = connectionEndpoints(c);
        return new Vector2f((e[0].x() + e[1].x()) / 2f, (e[0].y() + e[1].y()) / 2f);
    }

    private @Nullable Connection findConnectionUnderCursor(Vector2dc canvasMouse) {
        final int samples = 32;
        final double hitR2 = CONNECTION_HIT_RADIUS * CONNECTION_HIT_RADIUS;
        Connection best = null;
        double bestD2 = hitR2;
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

    private boolean isInDeleteButton(Connection c, Vector2dc canvasMouse) {
        Vector2f mid = connectionMidpoint(c);
        double dx = canvasMouse.x() - mid.x();
        double dy = canvasMouse.y() - mid.y();
        return dx * dx + dy * dy <= DELETE_BUTTON_RADIUS * DELETE_BUTTON_RADIUS;
    }

    private static int scaleAlpha(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int scaled = Math.max(0, Math.min(255, Math.round(a * factor)));
        return (scaled << 24) | (argb & 0x00FFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
