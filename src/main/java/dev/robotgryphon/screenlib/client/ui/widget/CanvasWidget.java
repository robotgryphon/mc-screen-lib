package dev.robotgryphon.screenlib.client.ui.widget;

import dev.robotgryphon.screenlib.graph.canvas.client.CanvasViewport;
import dev.robotgryphon.screenlib.client.ui.state.DragState;
import dev.robotgryphon.screenlib.client.ui.state.PendingConnection;
import dev.robotgryphon.screenlib.client.GraphColors;
import dev.robotgryphon.screenlib.client.ui.widget.node.NodeBackgrounds;
import dev.robotgryphon.screenlib.client.ui.widget.node.NodeWidget;
import dev.robotgryphon.screenlib.client.ui.widget.canvas.CanvasBounds;
import dev.robotgryphon.screenlib.graph.canvas.client.ViewportState;
import net.minecraft.util.Mth;
import dev.robotgryphon.screenlib.graph.Connection;
import com.mojang.blaze3d.platform.InputConstants;
import dev.robotgryphon.screenlib.client.ui.render.pip.BezierCurveRenderState;
import dev.robotgryphon.screenlib.client.ui.widget.property.NumericPropertyEditor;
import dev.robotgryphon.screenlib.client.geometry.BezierCurve;
import dev.robotgryphon.screenlib.client.geometry.CurveIndicator;
import dev.robotgryphon.screenlib.graph.canvas.client.ClientCanvasState;
import dev.robotgryphon.screenlib.graph.nodes.ports.Port;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.ports.PortSide;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CanvasWidget extends AbstractWidget {
    private static final double CONNECTION_HIT_RADIUS = 4.0;
    private static final float DELETE_BUTTON_RADIUS = 5f;

    private static final float MISMATCHED_TYPE_ALPHA = 0.3f;

    private static final int IN_FLIGHT_COLOR = 0xFFFFD24A;

    private static final double FIT_PADDING = 24;

    public final ClientCanvasState canvas;

    private final DragState dragState = new DragState();

    private final Map<Node, NodeWidget> widgetsByNode = new IdentityHashMap<>();

    private List<NodeWidget> widgets = List.of();

    private final CanvasViewport viewport = new CanvasViewport();

    private @Nullable PendingConnection pending;

    private @Nullable NodeWidget focusedNode;

    private boolean panning;

    private final Object2IntOpenHashMap<NodeWidget> layerIndices = new Object2IntOpenHashMap<>();

    private @Nullable ContextMenu activeMenu;

    private Consumer<ContextMenu> menus = menu -> this.activeMenu = menu;


    public CanvasWidget(ClientCanvasState canvas, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.canvas = canvas;
    }

    public void menus(Consumer<ContextMenu> menus) {
        this.menus = menus;
    }

    @Override
    public void setX(int x) {
        this.viewport.pan(x - this.getX(), 0);
        super.setX(x);
    }

    @Override
    public void setY(int y) {
        this.viewport.pan(0, y - this.getY());
        super.setY(y);
    }

    public List<NodeWidget> nodeWidgets() {
        List<Node> nodes = this.canvas.graph().nodes();
        boolean current = nodes.size() == this.widgets.size();
        for (int i = 0; current && i < nodes.size(); i++) {
            current = this.widgets.get(i).node() == nodes.get(i);
        }
        if (current) {
            return this.widgets;
        }

        List<NodeWidget> rebuilt = new ArrayList<>(nodes.size());
        Map<Node, NodeWidget> kept = new IdentityHashMap<>();
        for (Node node : nodes) {
            NodeWidget widget = this.widgetsByNode.get(node);
            if (widget == null) {
                widget = new NodeWidget(node, this.canvas, this.dragState);
            }
            kept.put(node, widget);
            rebuilt.add(widget);
        }
        this.layerIndices.keySet().removeIf(widget -> !kept.containsValue(widget));
        this.widgetsByNode.clear();
        this.widgetsByNode.putAll(kept);
        this.widgets = List.copyOf(rebuilt);
        return this.widgets;
    }

    private NodeWidget widgetFor(Node node) {
        this.nodeWidgets();
        NodeWidget widget = this.widgetsByNode.get(node);
        if (widget == null) {
            throw new IllegalArgumentException("Node is not on this canvas: " + node);
        }
        return widget;
    }

    private Stream<Port> findPortsNear(Vector2dc point) {
        List<NodeWidget> all = this.nodeWidgets();
        Stream.Builder<Port> found = Stream.builder();
        for (int i = all.size() - 1; i >= 0; i--) {
            Port port = all.get(i).portAt(point.x(), point.y());
            if (port != null) {
                found.add(port);
            }
        }
        return found.build();
    }

    private Stream<NodeWidget> findNodesNear(Vector2dc point) {
        List<NodeWidget> all = this.nodeWidgets();
        Stream.Builder<NodeWidget> found = Stream.builder();
        for (int i = all.size() - 1; i >= 0; i--) {
            NodeWidget widget = all.get(i);
            if (widget.isMouseOver(point.x(), point.y()) && widget.portAt(point.x(), point.y()) == null) {
                found.add(widget);
            }
        }
        return found.build();
    }

    public CanvasViewport viewport() {
        return this.viewport;
    }

    public void openMenu(ContextMenu menu) {
        this.menus.accept(menu);
    }

    public void zoomTo(float zoom) {
        this.viewport.zoomAround(zoom, this.getX() + this.getWidth() / 2.0, this.getY() + this.getHeight() / 2.0);
    }

    public void centerOn(double canvasX, double canvasY, float zoom) {
        this.viewport.apply(new ViewportState(canvasX, canvasY, zoom), this.getRectangle());
    }

    public void fitToContent() {
        Optional<CanvasBounds> content = this.contentBounds();
        if (content.isEmpty()) {
            this.centerOn(0, 0, 1f);
            return;
        }
        CanvasBounds bounds = content.get().grow(FIT_PADDING);
        float zoom = (float) Math.min(this.getWidth() / bounds.width(), this.getHeight() / bounds.height());
        this.centerOn(bounds.centerX(), bounds.centerY(), Mth.clamp(zoom, CanvasViewport.MIN_ZOOM, 1f));
    }

    public Optional<CanvasBounds> contentBounds() {
        CanvasBounds result = null;
        for (NodeWidget widget : this.nodeWidgets()) {
            CanvasBounds node = new CanvasBounds(widget.getX(), widget.getY(),
                    widget.getX() + widget.getWidth(), widget.getY() + widget.getHeight());
            result = result == null ? node : result.union(node);
        }
        return Optional.ofNullable(result);
    }

    public CanvasBounds viewBounds() {
        Vector2dc min = this.viewport.screenToCanvas(this.getX(), this.getY());
        Vector2dc max = this.viewport.screenToCanvas(this.getRight(), this.getBottom());
        return new CanvasBounds(min.x(), min.y(), max.x(), max.y());
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

        NumericPropertyEditor<?, ?> editingEditor = this.findEditingEditor();
        if (editingEditor != null) {
            final var canvasClickEvent = convertMouseButtonEvent(event);
            if (editingEditor.handleClickWhileEditing(canvasClickEvent, doubleClick)) {
                return true;
            }
        }

        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            final Vector2dc canvasPos = this.viewport.screenToCanvas(event.x(), event.y());
            final NodeWidget hitNode = this.findNodesNear(canvasPos).findFirst().orElse(null);
            if (hitNode == null || this.canvas.isRequired(hitNode.node())) {
                return true;
            }

            this.openMenu(new ContextMenu((int) event.x(), (int) event.y(), List.of(new ContextMenu.Item(
                    Component.literal("Remove Node"),
                    () -> {
                        canvas.removeNode(hitNode.node());
                        if (this.focusedNode == hitNode) {
                            this.focusedNode = null;
                        }
                    }))));
            return true;
        }

        final var clicked = this.viewport.screenToCanvas(event.x(), event.y());
        final var canvasEvent = convertMouseButtonEvent(event);

        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            Connection hovered = findConnectionUnderCursor(clicked);
            if (hovered != null && isInDeleteButton(hovered, clicked)) {
                canvas.disconnect(hovered);
                return true;
            }
        }

        var port = this.findPortsNear(clicked)
                .filter(p -> p.side().equals(PortSide.RIGHT))
                .findFirst()
                .orElse(null);
        if (port != null) {
            this.pending = new PendingConnection(port.node(), port);
            this.dragState.setType(port.type());
            this.focusedNode = null;
            this.panning = false;
            return true;
        }

        var node = this.findNodesNear(clicked).findFirst().orElse(null);
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
            this.dragState.setType(null);
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                final var port = this.findPortsNear(canvasSpace)
                        .filter(p2 -> !p2.equals(p.sourcePort()))
                        .filter(p2 -> !p2.node().equals(p.source()))
                        .findFirst();

                port.ifPresent(n -> canvas.connect(p.sourcePort(), n));
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
    public void mouseMoved(double mouseX, double mouseY) {
        var canvasPos = this.viewport.screenToCanvas(mouseX, mouseY);
        for (NodeWidget widget : this.nodeWidgets()) {
            widget.mouseMoved(canvasPos.x(), canvasPos.y());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var canvasPos = this.viewport.screenToCanvas(mouseX, mouseY);
        var hit = this.findNodesNear(canvasPos).findFirst().orElse(null);
        if (hit != null && hit.mouseScrolled(canvasPos.x(), canvasPos.y(), scrollX, scrollY)) {
            return true;
        }
        if (scrollY == 0) {
            return false;
        }
        float factor = scrollY > 0 ? CanvasViewport.ZOOM_STEP : 1f / CanvasViewport.ZOOM_STEP;
        this.viewport.zoomAround(this.viewport.zoom() * factor, mouseX, mouseY);
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        NumericPropertyEditor<?, ?> editor = this.findEditingEditor();
        if (editor != null) {
            return editor.keyPressed(event);
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        NumericPropertyEditor<?, ?> editor = this.findEditingEditor();
        if (editor != null) {
            return editor.charTyped(event);
        }

        return super.charTyped(event);
    }

    private @Nullable NumericPropertyEditor<?, ?> findEditingEditor() {
        for (NodeWidget node : this.nodeWidgets()) {
            NumericPropertyEditor<?, ?> editor = node.properties().activeNumericEditor();
            if (editor != null) {
                return editor;
            }
        }
        return null;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        graphics.pose().pushMatrix();
        this.viewport.transformPose(graphics.pose());

        final var canvasMouse = this.viewport.screenToCanvas(mouseX, mouseY);

        final Connection hovered = this.pending == null
                ? findConnectionUnderCursor(canvasMouse)
                : null;

        extractConnections(graphics, canvasMouse, hovered);

        var allNodes = this.nodeWidgets();
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
            NodeBackgrounds.nodes(graphics, this.viewport, layer, false);
            for (NodeWidget node : layer) {
                node.extractRenderState(graphics, (int) canvasMouse.x(), (int) canvasMouse.y(), partialTick);
            }
            NodeBackgrounds.ports(graphics, this.viewport, layer, canvasMouse);
        }

        if (!draggedNodes.isEmpty()) {
            graphics.nextStratum();
            NodeBackgrounds.nodes(graphics, this.viewport, draggedNodes, true);
            for (NodeWidget node : draggedNodes) {
                node.extractRenderState(graphics, (int) canvasMouse.x(), (int) canvasMouse.y(), partialTick);
            }
            NodeBackgrounds.ports(graphics, this.viewport, draggedNodes, canvasMouse);
        }

        graphics.pose().popMatrix();
        graphics.disableScissor();

        if (this.activeMenu != null) {
            graphics.nextStratum();
            this.activeMenu.render(graphics, mouseX, mouseY);
        }

        NodeWidget popupNode = this.findPopupNode();
        if (popupNode != null) {
            graphics.nextStratum();
            popupNode.properties().renderFocusedPropertyPopup(graphics, this.viewport, mouseX, mouseY);
        } else if (this.isHovered() && this.activeMenu == null && this.pending == null && !this.panning
                && this.canvasHasNoDraggedNode()) {
            this.findNodesNear(canvasMouse).findFirst()
                    .ifPresent(widget -> {
                        if (!widget.properties().tooltip(graphics, canvasMouse.x(), canvasMouse.y(), mouseX, mouseY)) {
                            widget.tooltip(graphics, canvasMouse.x(), canvasMouse.y(), mouseX, mouseY);
                        }
                    });
        }
    }

    private boolean canvasHasNoDraggedNode() {
        for (NodeWidget widget : this.nodeWidgets()) {
            if (widget.isDragging()) {
                return false;
            }
        }
        return true;
    }

    private @Nullable NodeWidget findPopupNode() {
        for (NodeWidget node : this.nodeWidgets()) {
            if (node.properties().hasFocusedPropertyPopup()) {
                return node;
            }
        }
        return null;
    }

    private void extractConnections(GuiGraphicsExtractor graphics, Vector2dc mouseCanvas,
                                    @Nullable Connection hovered) {
        List<BezierCurve> curves = new ArrayList<>();
        var dragType = this.dragState.type();
        for (Connection c : canvas.graph().connections().all()) {
            Vector2fc start = this.portAttachment(c.source(), c.sourcePort());
            Vector2fc end = this.portAttachment(c.target(), c.targetPort());
            CurveIndicator indicator = (c == hovered)
                    ? new CurveIndicator(midpoint(start, end), DELETE_BUTTON_RADIUS,
                            isInDeleteButton(c, mouseCanvas))
                    : null;
            int wireColor = GraphColors.of(c.sourcePort());
            if (dragType != null && c.sourcePort().type().value() != dragType.value()) {
                wireColor = scaleAlpha(wireColor, MISMATCHED_TYPE_ALPHA);
            }
            curves.add(BezierCurve.from(graphics, start, end, wireColor, indicator));
        }

        if (this.pending != null) {
            final var start = this.portAttachment(this.pending.source(), this.pending.sourcePort());
            final var end = new Vector2f(mouseCanvas);

            final var pendingCurve = BezierCurve.from(graphics, start, end, IN_FLIGHT_COLOR);
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
        return this.widgetFor(node).ports().portAttachment(port);
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
        java.util.List<Connection> all = canvas.graph().connections().all();
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
