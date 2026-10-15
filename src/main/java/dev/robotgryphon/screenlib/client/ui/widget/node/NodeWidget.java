package dev.robotgryphon.screenlib.client.ui.widget.node;

import dev.robotgryphon.screenlib.graph.canvas.client.ClientCanvasState;

import dev.robotgryphon.screenlib.graph.canvas.client.CanvasViewport;
import dev.robotgryphon.screenlib.client.ui.state.DragState;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.client.appearance.NodeAppearances;
import dev.robotgryphon.screenlib.client.appearance.WidgetAppearance;
import dev.robotgryphon.screenlib.client.ui.widget.property.DropdownEditor;
import dev.robotgryphon.screenlib.client.NodeRenderer;
import dev.robotgryphon.screenlib.client.render.NodeStateRenderer;
import dev.robotgryphon.screenlib.client.render.NodeWidgets;
import dev.robotgryphon.screenlib.client.render.PropertyValueRenderer;
import dev.robotgryphon.screenlib.client.PortLayout;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.ports.Port;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.Holder;
import org.joml.Vector2dc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public class NodeWidget extends AbstractWidget {
    private static final int BACKGROUND_COLOR = 0xCC1F1F23;
    private static final int BACKGROUND_HOVER_COLOR = 0xCC2A2A33;
    private static final int BORDER_COLOR = 0xFF7F7F8C;
    private static final int BORDER_DRAG_COLOR = 0xFFFFD24A;
    private static final int TITLE_BAR_COLOR = 0xFF3A3A45;

    public static final int INVALID_GLOW_COLOR = 0x99FF4D4D;

    private static final int INVALID_BORDER_COLOR = 0xFFA13838;

    public static final int NODE_CORNER_RADIUS = 4;

    private static final float MISMATCHED_TYPE_ALPHA = 0.3f;

    private final Node node;
    private final ClientCanvasState state;
    private final PortLayout layout;
    private final List<Integer> valueWidths;
    private final DragState dragState;
    private final NodePortsRenderer ports;
    private final NodePropertiesRenderer properties;
    private final List<WidgetAppearance> widgetDefinitions;

    private boolean dragging;
    private double grabOffsetX;
    private double grabOffsetY;

    private @Nullable Integer glowColor;


    public NodeWidget(Node node, ClientCanvasState state, DragState dragState) {
        this(node, state, dragState, PortLayout.compile(node.portManager().ports(), Minecraft.getInstance().font),
                valueWidths(node));
    }

    private NodeWidget(Node node, ClientCanvasState state, DragState dragState, PortLayout layout, List<Integer> valueWidths) {
        super(state.x(node), state.y(node), width(node, layout, valueWidths), height(node, layout), node.title);
        this.node = node;
        this.state = state;
        this.valueWidths = valueWidths;
        this.dragState = dragState;
        this.layout = layout;
        this.ports = new NodePortsRenderer(this, layout);
        this.properties = new NodePropertiesRenderer(this);
        this.widgetDefinitions = NodeAppearances.widgets(node.definition());
    }

    private static List<Integer> valueWidths(Node node) {
        Font font = Minecraft.getInstance().font;
        List<Integer> widths = new ArrayList<>();
        for (StoredPropertyDefinition<?> prop : node.propertyManager().stored()) {
            int width = NodeRenderer.PROPERTY_VALUE_MIN_WIDTH;
            var allowed = node.canvas.allowedValues(prop.anyType());
            if (allowed.isPresent()) {
                width = Math.max(width, DropdownEditor.widthFor(font, allowed.get()));
            }
            widths.add(NodePropertiesRenderer.valueRenderer(node, prop).map(PropertyValueRenderer::width).orElse(width));
        }
        for (var definition : NodePropertiesRenderer.visibleComputed(node)) {
            widths.add(NodePropertiesRenderer.valueRenderer(node, definition).map(PropertyValueRenderer::width)
                    .orElse(NodeRenderer.COMPUTED_VALUE_WIDTH));
        }
        return List.copyOf(widths);
    }

    private static int width(Node node, PortLayout layout, List<Integer> valueWidths) {
        List<String> rowNames = new ArrayList<>();
        node.propertyManager().stored().forEach(prop -> rowNames.add(NodeAppearances.get(node.type()).property(prop.name()).hideLabel() ? "" : NodeRenderer.propertyLabel(node, prop.name()).getString()));
        NodePropertiesRenderer.visibleComputed(node).forEach(def -> rowNames.add(NodeRenderer.propertyLabel(node, def.name()).getString()));
        int base = NodeRenderer.computeWidth(Minecraft.getInstance().font, node.title,
                layout, rowNames, valueWidths);
        int renderWidth = maxStateWidth(NodeAppearances.widgets(node.definition()));
        return renderWidth == 0 ? base : Math.max(base, renderWidth + 2 * NodeRenderer.PROPERTY_PADDING_X);
    }

    int valueWidth(int rowIndex) {
        return this.valueWidths.get(rowIndex);
    }

    private static int height(Node node, PortLayout layout) {
        return NodeRenderer.TITLE_BAR_HEIGHT + layout.bandHeight()
                + NodeRenderer.propertyRegionHeight(NodePropertiesRenderer.rowCount(node))
                + NodeRenderer.renderRegionHeight(stateHeights(NodeAppearances.widgets(node.definition())));
    }

    @Override
    public int getX() {
        return this.state.x(this.node);
    }

    @Override
    public int getY() {
        return this.state.y(this.node);
    }

    @Override
    public void setX(int x) {
        this.state.place(this.node, x, this.getY());
    }

    @Override
    public void setY(int y) {
        this.state.place(this.node, this.getX(), y);
    }

    PortLayout layout() {
        return this.layout;
    }

    public Node node() {
        return this.node;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public @Nullable Integer glowColor() {
        return this.glowColor;
    }

    public void setGlowColor(@Nullable Integer color) {
        this.glowColor = color;
    }

    public @Nullable Integer effectiveGlowColor() {
        if (this.glowColor != null) return this.glowColor;
        if (this.state.isInvalid(this.node)) {
            return INVALID_GLOW_COLOR;
        }
        return null;
    }


    public ScreenRectangle screenBounds(CanvasViewport viewport) {
        Vector2dc tl = viewport.canvasToScreen(this.getX(), this.getY());
        Vector2dc br = viewport.canvasToScreen(
                this.getX() + this.getWidth(),
                this.getY() + this.getHeight());
        int sx0 = (int) Math.floor(tl.x());
        int sy0 = (int) Math.floor(tl.y());
        int sx1 = (int) Math.ceil(br.x());
        int sy1 = (int) Math.ceil(br.y());
        return new ScreenRectangle(sx0, sy0, sx1 - sx0, sy1 - sy0);
    }

    public NodeBackgroundUniform.Entry buildBackgroundEntry(CanvasViewport viewport,
                                                            double textureOriginX,
                                                            double textureOriginY,
                                                            float guiScale,
                                                            boolean dropShadow) {
        Vector2dc tl = viewport.canvasToScreen(this.getX(), this.getY());
        Vector2dc br = viewport.canvasToScreen(
                this.getX() + this.getWidth(),
                this.getY() + this.getHeight());

        float relX = (float) ((tl.x() - textureOriginX) * guiScale);
        float relY = (float) ((tl.y() - textureOriginY) * guiScale);
        float w = (float) ((br.x() - tl.x()) * guiScale);
        float h = (float) ((br.y() - tl.y()) * guiScale);

        boolean hovered = this.isHovered() || this.dragging;
        int bodyArgb = hovered ? BACKGROUND_HOVER_COLOR : BACKGROUND_COLOR;
        int titleArgb = TITLE_BAR_COLOR;
        int borderArgb;
        if (this.dragging) {
            borderArgb = BORDER_DRAG_COLOR;
        } else if (this.state.isInvalid(this.node)) {
            borderArgb = INVALID_BORDER_COLOR;
        } else {
            borderArgb = BORDER_COLOR;
        }

        float titleHeight = NodeRenderer.TITLE_BAR_HEIGHT * viewport.zoom() * guiScale;

        Integer effectiveGlow = this.effectiveGlowColor();
        Vector4fc glowVec = effectiveGlow != null
                ? argbToVec(effectiveGlow)
                : new Vector4f(0f, 0f, 0f, 0f);

        return new NodeBackgroundUniform.Entry(
                new Vector4f(relX, relY, w, h),
                argbToVec(bodyArgb),
                argbToVec(titleArgb),
                argbToVec(borderArgb),
                titleHeight,
                dropShadow,
                0f, 0f,
                glowVec);
    }

    public boolean hasVisibleGlow() {
        Integer color = this.effectiveGlowColor();
        return color != null && ((color >>> 24) & 0xFF) > 0;
    }

    private static Vector4fc argbToVec(int argb) {
        return new Vector4f(
                ARGB.redFloat(argb),
                ARGB.greenFloat(argb),
                ARGB.blueFloat(argb),
                ARGB.alphaFloat(argb));
    }

    public @Nullable Port portAt(double mouseX, double mouseY) {
        return this.ports.portAt(mouseX, mouseY);
    }

    public NodePropertiesRenderer properties() {
        return this.properties;
    }

    public NodePortsRenderer ports() {
        return this.ports;
    }

    ClientCanvasState state() {
        return this.state;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.isValidClickButton(event.buttonInfo())) {
            if (this.portAt(event.x(), event.y()) != null) {
                return false;
            }
            if (this.properties.handleEditorClick(event, doubleClick)) {
                return true;
            }
            if (this.stateClicked(event, doubleClick)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        super.onClick(event, doubleClick);
        this.dragging = true;
        this.grabOffsetX = event.x() - this.getX();
        this.grabOffsetY = event.y() - this.getY();
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        if (!this.dragging) {
            return;
        }
        int newX = (int) Math.round(event.x() - this.grabOffsetX);
        int newY = (int) Math.round(event.y() - this.grabOffsetY);
        this.state.place(this.node, newX, newY);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        super.onRelease(event);
        this.dragging = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = this.getX();
        int top = this.getY();

        Font font = Minecraft.getInstance().font;
        int titleColor = ARGB.color((int) (255 * this.getAlpha()), 0xFF, 0xFF, 0xFF);
        graphics.centeredText(font, this.node.title, left + this.getWidth() / 2, top + 2, titleColor);

        this.properties.extractRenderState(graphics, mouseX, mouseY, partialTick);

        for (int i = 0; i < this.widgetDefinitions.size(); i++) {
            WidgetAppearance widget = this.widgetDefinitions.get(i);
            Object state = this.state(widget);
            NodeStateRenderer<Object> renderer = renderer(widget);
            if (state != null && renderer != null) {
                renderer.render(graphics, state, this.stateArea(i), mouseX, mouseY, this.getAlpha());
            }
        }

        this.ports.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static @Nullable NodeStateRenderer<Object> renderer(WidgetAppearance widget) {
        return NodeWidgets.get(widget.type());
    }

    private static List<Integer> stateHeights(List<WidgetAppearance> widgets) {
        return widgets.stream()
                .map(NodeWidget::renderer)
                .filter(Objects::nonNull)
                .map(NodeStateRenderer::height)
                .toList();
    }

    private static int maxStateWidth(List<WidgetAppearance> widgets) {
        return widgets.stream()
                .map(NodeWidget::renderer)
                .filter(Objects::nonNull)
                .mapToInt(NodeStateRenderer::width)
                .max().orElse(0);
    }

    private @Nullable Object state(WidgetAppearance widget) {
        return this.state.clientData(this.node, widget.property());
    }

    private ScreenRectangle stateArea(int entryIndex) {
        int y = this.properties.top() + this.properties.height() + NodeRenderer.RENDER_REGION_PADDING;
        for (int i = 0; i < entryIndex; i++) {
            var previous = renderer(this.widgetDefinitions.get(i));
            if (previous != null) {
                y += previous.height() + NodeRenderer.RENDER_REGION_PADDING;
            }
        }
        var renderer = renderer(this.widgetDefinitions.get(entryIndex));
        int width = renderer == null ? 0 : renderer.width();
        int height = renderer == null ? 0 : renderer.height();
        return new ScreenRectangle(this.getX() + (this.getWidth() - width) / 2, y, width, height);
    }

    @FunctionalInterface
    private interface StateVisitor<R> {
        R visit(NodeStateRenderer<Object> renderer, Object state, ScreenRectangle area);
    }

    private <R> Optional<R> firstStateResult(StateVisitor<Optional<R>> visitor) {
        for (int i = 0; i < this.widgetDefinitions.size(); i++) {
            WidgetAppearance widget = this.widgetDefinitions.get(i);
            Object state = this.state(widget);
            NodeStateRenderer<Object> renderer = renderer(widget);
            if (state == null || renderer == null) {
                continue;
            }
            Optional<R> result = visitor.visit(renderer, state, this.stateArea(i));
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    private boolean stateClicked(MouseButtonEvent event, boolean doubleClick) {
        return this.firstStateResult((renderer, state, area) ->
                contains(area, event.x(), event.y()) && renderer.mouseClicked(state, area, event, doubleClick)
                        ? Optional.of(true) : Optional.<Boolean>empty()).isPresent();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.firstStateResult((renderer, state, area) -> {
            renderer.mouseMoved(state, area, mouseX, mouseY);
            return Optional.<Boolean>empty();
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.firstStateResult((renderer, state, area) ->
                contains(area, mouseX, mouseY) && renderer.mouseScrolled(state, area, mouseX, mouseY, scrollX, scrollY)
                        ? Optional.of(true) : Optional.<Boolean>empty()).isPresent();
    }

    public void tooltip(GuiGraphicsExtractor graphics, double mouseX, double mouseY, int screenX, int screenY) {
        this.firstStateResult((renderer, state, area) -> {
            if (!contains(area, mouseX, mouseY)) {
                return Optional.<Boolean>empty();
            }
            renderer.tooltip(graphics, state, area, Mth.floor(mouseX), Mth.floor(mouseY), screenX, screenY);
            return Optional.of(true);
        });
    }

    private static boolean contains(ScreenRectangle area, double x, double y) {
        return area.containsPoint(Mth.floor(x), Mth.floor(y));
    }

    float effectiveAlpha(Holder<GraphDataType<?>> type) {
        float base = this.getAlpha();
        Holder<GraphDataType<?>> active = this.dragState.type();
        if (active == null) {
            return base;
        }
        return type.value() == active.value() ? base : base * MISMATCHED_TYPE_ALPHA;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
